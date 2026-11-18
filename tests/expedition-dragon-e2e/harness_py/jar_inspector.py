"""Opaque-box JAR, Fabric metadata, Mixin, Java 25 constant-pool & javap inspector."""

from __future__ import annotations

import hashlib
import json
import os
import re
import shutil
import struct
import subprocess
import zipfile
from dataclasses import dataclass
from pathlib import Path
from typing import Any

WORKSPACE_MOD_JAR = Path(r"d:\Minecraft-One-More-Expedition\mods\expedition-dragon-1.0.0.jar")
ALLAYOPS_MOD_JAR = Path(r"D:\AllayOps\mods\expedition-dragon-1.0.0.jar")

SELECTOR_POLL_RE = re.compile(r"""(?:^|[\s"'`/;\x01\x02])@e(?:\[|\s|$|["'`;\x01\x02])""")
COMMAND_POLL_METHODS = (
    "performPrefixedCommand",
    "performCommand",
)
FORBIDDEN_JUNK_PREFIXES = ("org/junit/", "junit/")
FORBIDDEN_RESOURCE_NAMES = {"AGENTS.md", "GEMINI.md"}


@dataclass(frozen=True)
class ClassScanResult:
    class_name: str
    major_version: int
    minor_version: int
    utf8_constants: tuple[str, ...]
    string_literals: tuple[str, ...]
    class_refs: tuple[str, ...]


def sha256_file(path: Path) -> str:
    h = hashlib.sha256()
    with path.open("rb") as f:
        for chunk in iter(lambda: f.read(65536), b""):
            h.update(chunk)
    return h.hexdigest()


def check_zip_crc(path: Path) -> list[str]:
    errors: list[str] = []
    if not path.is_file():
        return [f"JAR file not found: {path}"]
    try:
        with zipfile.ZipFile(path, "r") as zf:
            bad = zf.testzip()
            if bad is not None:
                errors.append(f"Corrupt ZIP entry CRC in {path}: {bad}")
    except zipfile.BadZipFile as exc:
        errors.append(f"Invalid ZIP/JAR {path}: {exc}")
    return errors


def check_dual_deploy_parity(
    workspace_jar: Path = WORKSPACE_MOD_JAR,
    allayops_jar: Path = ALLAYOPS_MOD_JAR,
) -> dict[str, Any]:
    errors: list[str] = []
    for label, p in (("workspace", workspace_jar), ("allayops", allayops_jar)):
        if not p.is_file():
            errors.append(f"Missing {label} JAR: {p}")
        else:
            size = p.stat().st_size
            if size < 10_240 or size > 20 * 1024 * 1024:
                errors.append(f"JAR size out of bounds (10KB..20MB) for {p}: {size} bytes")
            errors.extend(check_zip_crc(p))
            siblings = [
                c.name for c in p.parent.glob("expedition-dragon-*.jar")
                if c.name != p.name
            ]
            if siblings:
                errors.append(f"Stale expedition-dragon JARs in {p.parent}: {siblings}")

    ws_hash = sha256_file(workspace_jar) if workspace_jar.is_file() else None
    ao_hash = sha256_file(allayops_jar) if allayops_jar.is_file() else None
    if ws_hash and ao_hash and ws_hash != ao_hash:
        errors.append(f"Dual-deploy SHA-256 mismatch: workspace={ws_hash} vs allayops={ao_hash}")

    return {
        "ok": len(errors) == 0,
        "workspace_jar": str(workspace_jar),
        "allayops_jar": str(allayops_jar),
        "workspace_sha256": ws_hash,
        "allayops_sha256": ao_hash,
        "errors": errors,
    }


def read_fabric_mod_json(jar_path: Path) -> dict[str, Any]:
    with zipfile.ZipFile(jar_path, "r") as zf:
        if "fabric.mod.json" not in zf.namelist():
            raise AssertionError(f"Missing fabric.mod.json in {jar_path}")
        return json.loads(zf.read("fabric.mod.json").decode("utf-8"))


def validate_fabric_mod_json(mod_json: dict[str, Any]) -> list[str]:
    if not isinstance(mod_json, dict):
        return ["fabric.mod.json root must be a JSON object"]
    errors: list[str] = []
    if mod_json.get("schemaVersion") != 1:
        errors.append(f"Expected schemaVersion=1, got {mod_json.get('schemaVersion')}")
    if mod_json.get("id") != "expedition-dragon":
        errors.append(f"Expected mod id 'expedition-dragon', got {mod_json.get('id')!r}")
    if not mod_json.get("version"):
        errors.append("Missing mod version in fabric.mod.json")

    depends = mod_json.get("depends") or {}
    if not isinstance(depends, dict):
        errors.append("fabric.mod.json 'depends' must be an object")
    else:
        if "economycraft" in depends:
            errors.append("Hard dependency on 'economycraft' forbidden in fabric.mod.json depends")
        mc_dep = str(depends.get("minecraft", ""))
        if "26.3" not in mc_dep:
            errors.append(f"Expected minecraft ~26.3 dependency, got {mc_dep!r}")
        java_dep = str(depends.get("java", ""))
        if "25" not in java_dep:
            errors.append(f"Expected java >=25 dependency, got {java_dep!r}")
        if "fabricloader" not in depends:
            errors.append("Missing 'fabricloader' in depends")
        if "fabric-api" not in depends:
            errors.append("Missing 'fabric-api' in depends")

    breaks = mod_json.get("breaks") or {}
    if isinstance(breaks, dict) and "economycraft" in breaks:
        errors.append("'economycraft' must not appear in fabric.mod.json 'breaks'")

    entrypoints = mod_json.get("entrypoints") or {}
    if not isinstance(entrypoints, dict):
        errors.append("fabric.mod.json 'entrypoints' must be an object")
    elif not any(entrypoints.get(k) for k in ("main", "server")):
        errors.append("Missing 'main' or 'server' entrypoint in fabric.mod.json")
    return errors


def validate_mixins(jar_path: Path, mod_json: dict[str, Any]) -> list[str]:
    errors: list[str] = []
    mixin_entries = mod_json.get("mixins") or []
    if not isinstance(mixin_entries, list) or not mixin_entries:
        return ["No mixin config declared in fabric.mod.json"]

    with zipfile.ZipFile(jar_path, "r") as zf:
        names = set(zf.namelist())
        for item in mixin_entries:
            cfg_name = item["config"] if isinstance(item, dict) and "config" in item else str(item)
            if cfg_name not in names:
                errors.append(f"Declared mixin config missing in JAR: {cfg_name}")
                continue
            cfg = json.loads(zf.read(cfg_name).decode("utf-8"))
            if not isinstance(cfg, dict):
                errors.append(f"{cfg_name}: mixin config root must be a JSON object")
                continue
            compat = cfg.get("compatibilityLevel")
            if compat != "JAVA_25":
                errors.append(f"{cfg_name}: expected compatibilityLevel JAVA_25, got {compat!r}")
            pkg = str(cfg.get("package", "")).strip(".")
            if not pkg:
                errors.append(f"{cfg_name}: missing 'package'")
                continue
            pkg_path = pkg.replace(".", "/")
            declared: list[str] = []
            for key in ("mixins", "server", "client"):
                vals = cfg.get(key) or []
                if isinstance(vals, list):
                    declared.extend(str(v) for v in vals)
            if not declared:
                errors.append(f"{cfg_name}: no mixin classes listed")
            for m in declared:
                cls_entry = f"{pkg_path}/{m.replace('.', '/')}.class"
                if cls_entry not in names:
                    errors.append(f"{cfg_name}: declared mixin class missing in JAR: {cls_entry}")
    return errors


def parse_class_constant_pool(class_bytes: bytes, class_name: str = "<memory>") -> ClassScanResult:
    if len(class_bytes) < 10:
        raise ValueError(f"Truncated classfile {class_name}")
    magic, minor, major, cp_count = struct.unpack_from(">IHHH", class_bytes, 0)
    if magic != 0xCAFEBABE:
        raise ValueError(f"Invalid classfile magic 0x{magic:08x} in {class_name}")

    pos = 10
    cp_entries: dict[int, tuple[int, Any]] = {}
    i = 1
    while i < cp_count:
        tag = class_bytes[pos]
        pos += 1
        if tag == 1:  # CONSTANT_Utf8
            (length,) = struct.unpack_from(">H", class_bytes, pos)
            pos += 2
            raw = class_bytes[pos : pos + length]
            pos += length
            cp_entries[i] = (tag, raw.decode("utf-8", errors="replace"))
        elif tag in (3, 4):  # Integer / Float
            val = struct.unpack_from(">I", class_bytes, pos)[0]
            pos += 4
            cp_entries[i] = (tag, val)
        elif tag in (5, 6):  # Long / Double (2 slots)
            val = struct.unpack_from(">Q", class_bytes, pos)[0]
            pos += 8
            cp_entries[i] = (tag, val)
            i += 1
        elif tag in (7, 8, 16, 19, 20):  # Class, String, MethodType, Module, Package
            (idx,) = struct.unpack_from(">H", class_bytes, pos)
            pos += 2
            cp_entries[i] = (tag, idx)
        elif tag in (9, 10, 11, 12, 17, 18):  # Fieldref, Methodref, InterfaceMethodref, NameAndType, Dynamic, InvokeDynamic
            a, b = struct.unpack_from(">HH", class_bytes, pos)
            pos += 4
            cp_entries[i] = (tag, (a, b))
        elif tag == 15:  # MethodHandle
            kind, idx = struct.unpack_from(">BH", class_bytes, pos)
            pos += 3
            cp_entries[i] = (tag, (kind, idx))
        else:
            raise ValueError(f"Unknown constant pool tag {tag} at index {i} in {class_name}")
        i += 1

    utf8s: list[str] = []
    strings: list[str] = []
    class_refs: list[str] = []
    for tag, val in cp_entries.values():
        if tag == 1:
            utf8s.append(val)
        elif tag == 8:
            target = cp_entries.get(val)
            if target and target[0] == 1:
                strings.append(target[1])
        elif tag == 7:
            target = cp_entries.get(val)
            if target and target[0] == 1:
                class_refs.append(target[1])

    return ClassScanResult(
        class_name=class_name,
        major_version=major,
        minor_version=minor,
        utf8_constants=tuple(utf8s),
        string_literals=tuple(strings),
        class_refs=tuple(class_refs),
    )


def scan_jar_bytecode(jar_path: Path) -> dict[str, Any]:
    classes: list[ClassScanResult] = []
    selector_hits: list[str] = []
    command_poll_hits: list[str] = []
    version_errors: list[str] = []
    junk_entries: list[str] = []

    with zipfile.ZipFile(jar_path, "r") as zf:
        for entry in zf.namelist():
            base = entry.rsplit("/", 1)[-1]
            if base in FORBIDDEN_RESOURCE_NAMES or entry.endswith(".tmp"):
                junk_entries.append(entry)
            if any(entry.startswith(p) for p in FORBIDDEN_JUNK_PREFIXES):
                junk_entries.append(entry)
            if not entry.endswith(".class") or entry.startswith("META-INF/"):
                continue

            raw = zf.read(entry)
            scan = parse_class_constant_pool(raw, entry)
            classes.append(scan)
            if scan.major_version != 69:
                version_errors.append(f"{entry}: expected Java 25 major=69, got {scan.major_version}")

            for lit in scan.string_literals:
                if SELECTOR_POLL_RE.search(lit):
                    selector_hits.append(f"{entry}: string literal {lit!r}")
                for cmd_m in COMMAND_POLL_METHODS:
                    if cmd_m in lit:
                        command_poll_hits.append(f"{entry}: string literal {lit!r}")

            for u in scan.utf8_constants:
                for cmd_m in COMMAND_POLL_METHODS:
                    if u == cmd_m:
                        command_poll_hits.append(f"{entry}: method/utf8 ref {u!r}")

    return {
        "class_count": len(classes),
        "classes": classes,
        "version_errors": version_errors,
        "selector_hits": selector_hits,
        "command_poll_hits": command_poll_hits,
        "junk_entries": junk_entries,
        "ok": not (version_errors or selector_hits or command_poll_hits or junk_entries or len(classes) == 0),
    }


def inspect_jar_full(jar_path: Path) -> dict[str, Any]:
    errors = check_zip_crc(jar_path)
    if errors:
        return {"ok": False, "errors": errors}

    mod_json: dict[str, Any] | None = None
    bytecode: dict[str, Any] | None = None
    try:
        mod_json = read_fabric_mod_json(jar_path)
        errors.extend(validate_fabric_mod_json(mod_json))
        if isinstance(mod_json, dict):
            errors.extend(validate_mixins(jar_path, mod_json))
    except (
        AssertionError,
        AttributeError,
        TypeError,
        json.JSONDecodeError,
        ValueError,
        struct.error,
        IndexError,
        UnicodeDecodeError,
        OSError,
        KeyError,
        zipfile.BadZipFile,
    ) as exc:
        errors.append(f"Invalid Fabric/Mixin metadata in {jar_path}: {exc}")

    try:
        bytecode = scan_jar_bytecode(jar_path)
        errors.extend(bytecode["version_errors"])
        errors.extend(f"Forbidden @e selector polling: {h}" for h in bytecode["selector_hits"])
        errors.extend(f"Forbidden command polling: {h}" for h in bytecode["command_poll_hits"])
        errors.extend(f"Forbidden junk/test entry in JAR: {j}" for j in bytecode["junk_entries"])
        if bytecode["class_count"] == 0:
            errors.append("JAR contains zero .class files")
    except (
        AssertionError,
        AttributeError,
        TypeError,
        json.JSONDecodeError,
        ValueError,
        struct.error,
        IndexError,
        UnicodeDecodeError,
        OSError,
        KeyError,
        zipfile.BadZipFile,
    ) as exc:
        errors.append(f"Corrupt classfile or JAR bytecode in {jar_path}: {exc}")

    return {
        "ok": len(errors) == 0,
        "mod_json": mod_json,
        "bytecode": bytecode,
        "errors": errors,
    }


def find_javap_exe() -> str:
    java_home = os.environ.get("JAVA_HOME", "")
    if java_home:
        candidate = Path(java_home) / "bin" / ("javap.exe" if os.name == "nt" else "javap")
        if candidate.is_file():
            return str(candidate)
    found = shutil.which("javap")
    if found:
        return found
    raise FileNotFoundError("javap executable not found on JAVA_HOME or PATH")


def run_javap(jar_path: Path, fqcn: str) -> str:
    javap = find_javap_exe()
    proc = subprocess.run(
        [javap, "-cp", str(jar_path), "-p", "-v", fqcn],
        capture_output=True,
        text=True,
        encoding="utf-8",
        errors="replace",
        check=False,
    )
    if proc.returncode != 0:
        raise RuntimeError(f"javap failed ({proc.returncode}) for {fqcn}: {proc.stderr.strip()}")
    return proc.stdout
