"""Opaque-box datapack deconfliction, Junction, level.dat NBT & Stellarity preservation inspector."""

from __future__ import annotations

import gzip
import json
import os
import re
import struct
import zlib
from pathlib import Path
from typing import Any

WORKSPACE_DATAPACKS = Path(r"d:\Minecraft-One-More-Expedition\datapacks")
ALLAYOPS_WORLD_DATAPACKS = Path(r"D:\AllayOps\world\datapacks")
ALLAYOPS_LEVEL_DAT = Path(r"D:\AllayOps\world\level.dat")


class _NbtReader:
    """Minimal pure-Python big-endian NBT parser for level.dat DataPacks inspection."""

    def __init__(self, data: bytes) -> None:
        self.data = data
        self.pos = 0

    def u8(self) -> int:
        val = self.data[self.pos]
        self.pos += 1
        return val

    def i16(self) -> int:
        (val,) = struct.unpack_from(">h", self.data, self.pos)
        self.pos += 2
        return val

    def u16(self) -> int:
        (val,) = struct.unpack_from(">H", self.data, self.pos)
        self.pos += 2
        return val

    def i32(self) -> int:
        (val,) = struct.unpack_from(">i", self.data, self.pos)
        self.pos += 4
        return val

    def i64(self) -> int:
        (val,) = struct.unpack_from(">q", self.data, self.pos)
        self.pos += 8
        return val

    def f32(self) -> float:
        (val,) = struct.unpack_from(">f", self.data, self.pos)
        self.pos += 4
        return val

    def f64(self) -> float:
        (val,) = struct.unpack_from(">d", self.data, self.pos)
        self.pos += 8
        return val

    def string(self) -> str:
        length = self.u16()
        raw = self.data[self.pos : self.pos + length]
        self.pos += length
        return raw.decode("utf-8", errors="replace")

    def read_payload(self, tag_id: int) -> Any:
        if tag_id == 1:
            return struct.unpack_from(">b", self.data, self.pos - 1 + 1)[0] if False else self._i8()
        if tag_id == 2:
            return self.i16()
        if tag_id == 3:
            return self.i32()
        if tag_id == 4:
            return self.i64()
        if tag_id == 5:
            return self.f32()
        if tag_id == 6:
            return self.f64()
        if tag_id == 7:  # ByteArray
            n = self.i32()
            b = self.data[self.pos : self.pos + n]
            self.pos += n
            return b
        if tag_id == 8:  # String
            return self.string()
        if tag_id == 9:  # List
            elem_tag = self.u8()
            n = self.i32()
            return [self.read_payload(elem_tag) for _ in range(n)]
        if tag_id == 10:  # Compound
            out: dict[str, Any] = {}
            while True:
                t = self.u8()
                if t == 0:
                    return out
                name = self.string()
                out[name] = self.read_payload(t)
        if tag_id == 11:  # IntArray
            n = self.i32()
            vals = list(struct.unpack_from(f">{n}i", self.data, self.pos))
            self.pos += 4 * n
            return vals
        if tag_id == 12:  # LongArray
            n = self.i32()
            vals = list(struct.unpack_from(f">{n}q", self.data, self.pos))
            self.pos += 8 * n
            return vals
        raise ValueError(f"Unknown NBT tag id: {tag_id}")

    def _i8(self) -> int:
        (val,) = struct.unpack_from(">b", self.data, self.pos)
        self.pos += 1
        return val


def parse_level_dat_datapacks(level_dat_path: Path) -> dict[str, list[str]]:
    raw = gzip.decompress(level_dat_path.read_bytes())
    reader = _NbtReader(raw)
    root_tag = reader.u8()
    if root_tag != 10:
        raise ValueError(f"Expected root TAG_Compound (10) in {level_dat_path}, got {root_tag}")
    _ = reader.string()
    root = reader.read_payload(10)
    if not isinstance(root, dict):
        raise ValueError(f"Expected root NBT Compound dict in {level_dat_path}")
    data_tag = root.get("Data")
    if not isinstance(data_tag, dict):
        raise ValueError(f"Expected NBT Compound 'Data' in {level_dat_path}, got {type(data_tag).__name__}")
    dp = data_tag.get("DataPacks")
    if dp is None:
        dp = {}
    elif not isinstance(dp, dict):
        raise ValueError(f"Expected NBT Compound 'Data.DataPacks' in {level_dat_path}, got {type(dp).__name__}")
    enabled_raw = dp.get("Enabled") or []
    disabled_raw = dp.get("Disabled") or []
    if not isinstance(enabled_raw, list) or not isinstance(disabled_raw, list):
        raise ValueError(f"Expected NBT list for DataPacks Enabled/Disabled in {level_dat_path}")
    return {
        "enabled": [str(x) for x in enabled_raw],
        "disabled": [str(x) for x in disabled_raw],
    }


def verify_datapacks_junction(
    allayops_datapacks: Path = ALLAYOPS_WORLD_DATAPACKS,
    workspace_datapacks: Path = WORKSPACE_DATAPACKS,
) -> dict[str, Any]:
    errors: list[str] = []
    if not allayops_datapacks.exists():
        errors.append(f"Missing AllayOps datapacks path: {allayops_datapacks}")
    if not workspace_datapacks.exists():
        errors.append(f"Missing workspace datapacks path: {workspace_datapacks}")
    resolved_ao = os.path.normcase(os.path.realpath(allayops_datapacks))
    resolved_ws = os.path.normcase(os.path.realpath(workspace_datapacks))
    if not errors and resolved_ao != resolved_ws:
        errors.append(
            f"Junction parity mismatch: {allayops_datapacks} -> {resolved_ao} != {resolved_ws}"
        )
    return {
        "ok": len(errors) == 0,
        "allayops_resolved": resolved_ao,
        "workspace_resolved": resolved_ws,
        "errors": errors,
    }


def _active_mcfunction_lines(path: Path) -> list[str]:
    if not path.is_file():
        return []
    out: list[str] = []
    buf: list[str] = []
    for raw_line in path.read_text(encoding="utf-8", errors="replace").splitlines():
        s = raw_line.strip()
        if s.startswith("#"):
            continue
        if not buf and not s:
            continue
        if s.endswith("\\"):
            part = s[:-1].rstrip()
            if part:
                buf.append(part)
        else:
            if s:
                buf.append(s)
            if buf:
                out.append(" ".join(buf))
                buf.clear()
    if buf:
        out.append(" ".join(buf))
    return out


def _tag_json_values(path: Path, errors: list[str] | None = None) -> list[str]:
    if not path.is_file():
        return []
    try:
        data = json.loads(path.read_text(encoding="utf-8"))
    except (json.JSONDecodeError, UnicodeDecodeError, OSError) as exc:
        if errors is not None:
            errors.append(f"Corrupt tag JSON file {path}: {exc}")
        return []
    if not isinstance(data, dict):
        if errors is not None:
            errors.append(f"Expected JSON object in tag file {path}")
        return []
    vals = data.get("values") or []
    if not isinstance(vals, list):
        if errors is not None:
            errors.append(f"Expected list for 'values' in tag file {path}")
        return []
    return [v["id"] if isinstance(v, dict) and "id" in v else str(v) for v in vals]


def check_true_ending_disabled(
    datapacks_dir: Path = WORKSPACE_DATAPACKS,
    level_dat_path: Path | None = ALLAYOPS_LEVEL_DAT,
) -> dict[str, Any]:
    errors: list[str] = []
    pack_dir = datapacks_dir / "tru.e-ending-1.1.4d"
    pack_mcmeta = pack_dir / "pack.mcmeta"

    pack_present_and_valid = pack_dir.is_dir() and pack_mcmeta.is_file()
    level_enabled: list[str] = []
    if level_dat_path and level_dat_path.is_file():
        try:
            dp_state = parse_level_dat_datapacks(level_dat_path)
            level_enabled = dp_state["enabled"]
        except (gzip.BadGzipFile, zlib.error, EOFError, ValueError, OSError, struct.error, IndexError) as exc:
            errors.append(f"Corrupt or unreadable level.dat ({level_dat_path}): {exc}")

    enabled_in_level = any("tru.e-ending" in e for e in level_enabled)

    # Check function tag hooks inside tru.e-ending-1.1.4d (including overlays)
    active_tag_hooks: list[str] = []
    if pack_dir.is_dir():
        for tag_name in ("load.json", "tick.json"):
            for tag_file in pack_dir.rglob(f"data/minecraft/tags/function/{tag_name}"):
                vals = _tag_json_values(tag_file, errors=errors)
                te_vals = [v for v in vals if "true_ending" in v]
                if te_vals:
                    active_tag_hooks.append(f"{tag_file}: {te_vals}")

    # Check active commands in true_ending load/tick mcfunctions if tag hooks remain
    load_cmds = _active_mcfunction_lines(pack_dir / "data/true_ending/function/load.mcfunction")
    tick_cmds = _active_mcfunction_lines(pack_dir / "data/true_ending/function/tick.mcfunction")
    active_fn_hooks = [
        cmd for cmd in (load_cmds + tick_cmds)
        if re.search(r"\b(schedule|scoreboard|execute|function)\b", cmd)
    ]

    if pack_present_and_valid:
        # Either disabled in level.dat OR tag/function hooks neutralized
        if level_dat_path and level_dat_path.is_file():
            if enabled_in_level and (active_tag_hooks or active_fn_hooks):
                errors.append(
                    "tru.e-ending-1.1.4d is still enabled in level.dat AND has active load/tick hooks: "
                    f"tags={active_tag_hooks}, active_cmds={len(active_fn_hooks)}"
                )
        else:
            if active_tag_hooks:
                errors.append(f"tru.e-ending-1.1.4d still registers active minecraft tags: {active_tag_hooks}")

    return {
        "ok": len(errors) == 0,
        "pack_present": pack_present_and_valid,
        "enabled_in_level_dat": enabled_in_level,
        "active_tag_hooks": active_tag_hooks,
        "active_fn_hook_count": len(active_fn_hooks),
        "errors": errors,
    }


def check_stellarity_deconflicted(
    datapacks_dir: Path = WORKSPACE_DATAPACKS,
    level_dat_path: Path | None = ALLAYOPS_LEVEL_DAT,
) -> dict[str, Any]:
    errors: list[str] = []
    pack_dir = datapacks_dir / "Stellarity-6.0.1"
    if not pack_dir.is_dir():
        return {"ok": False, "errors": [f"Missing Stellarity-6.0.1 directory in {datapacks_dir}"]}

    if level_dat_path and level_dat_path.is_file():
        try:
            dp_state = parse_level_dat_datapacks(level_dat_path)
            if not any("Stellarity" in e for e in dp_state["enabled"]):
                errors.append("file/Stellarity-6.0.1 is NOT enabled in level.dat DataPacks.Enabled")
        except (gzip.BadGzipFile, zlib.error, EOFError, ValueError, OSError, struct.error, IndexError) as exc:
            errors.append(f"Corrupt or unreadable level.dat ({level_dat_path}): {exc}")

    init_mc = pack_dir / "data/stellarity/function/config/init.mcfunction"
    init_text = init_mc.read_text(encoding="utf-8", errors="replace") if init_mc.is_file() else ""
    enable_dragon_match = re.search(
        r"^\s*execute\s+unless\s+score\s+#stellarity\.config\s+stellarity\.config\.enable_ender_dragon\s+.*?run\s+scoreboard\s+players\s+set\s+#stellarity\.config\s+stellarity\.config\.enable_ender_dragon\s+(\d+)",
        init_text,
        re.MULTILINE,
    )
    direct_zero = re.search(
        r"^\s*scoreboard\s+players\s+set\s+#stellarity\.config\s+stellarity\.config\.enable_ender_dragon\s+0\b",
        init_text,
        re.MULTILINE,
    )
    static_enable_dragon = 0 if direct_zero else (int(enable_dragon_match.group(1)) if enable_dragon_match else None)
    if static_enable_dragon is None:
        errors.append("Could not locate #stellarity.config.enable_ender_dragon default in config/init.mcfunction")
    elif static_enable_dragon != 0:
        errors.append(
            f"Stellarity config/init.mcfunction still defaults #stellarity.config.enable_ender_dragon to {static_enable_dragon} (expected 0)"
        )

    main_mc = pack_dir / "data/stellarity/function/main.mcfunction"
    main_lines = _active_mcfunction_lines(main_mc)
    ungated_butcher = [
        line for line in main_lines
        if "stellarity:entity/dragon/butcher" in line
        and "enable_ender_dragon matches 1" not in line
    ]
    if ungated_butcher:
        errors.append(
            "Stellarity main.mcfunction has ungated dragon/butcher hook that would kill custom dragons: "
            f"{ungated_butcher}"
        )

    ec_main_mc = pack_dir / "data/stellarity/function/mechanic/end_crystal/main.mcfunction"
    ec_main_lines = _active_mcfunction_lines(ec_main_mc)
    altar_crystal_guarded = False
    for line in ec_main_lines:
        if "action/left_click" in line:
            break
        if (
            "in_the_end" in line
            and "positioned 0 ~ 0" in line
            and "distance=..6" in line
            and "return" in line
            and "kill @s" in line
        ):
            altar_crystal_guarded = True
            break

    left_click_mc = pack_dir / "data/stellarity/function/mechanic/end_crystal/action/left_click.mcfunction"
    left_click_lines = _active_mcfunction_lines(left_click_mc)
    ungated_drop = (
        []
        if altar_crystal_guarded
        else [
            line for line in left_click_lines
            if "stellarity:mechanic/end_crystal/drop" in line
            and "enable_ender_dragon matches 1" not in line
            and "altar" not in line.lower()
        ]
    )
    if ungated_drop:
        errors.append(
            "Stellarity left_click.mcfunction has ungated end_crystal/drop hook that could steal altar crystals: "
            f"{ungated_drop}"
        )

    return {
        "ok": len(errors) == 0,
        "static_enable_ender_dragon": static_enable_dragon,
        "ungated_butcher_lines": ungated_butcher,
        "ungated_crystal_drop_lines": ungated_drop,
        "errors": errors,
    }


def check_stellarity_preserved(datapacks_dir: Path = WORKSPACE_DATAPACKS) -> dict[str, Any]:
    errors: list[str] = []
    pack_dir = datapacks_dir / "Stellarity-6.0.1"
    if not pack_dir.is_dir():
        return {"ok": False, "errors": [f"Missing Stellarity-6.0.1 directory in {datapacks_dir}"]}

    def check_json(path: Path) -> Any:
        if not path.is_file():
            errors.append(f"Missing JSON file: {path}")
            return None
        try:
            return json.loads(path.read_text(encoding="utf-8"))
        except json.JSONDecodeError as exc:
            errors.append(f"Corrupt JSON file {path}: {exc}")
            return None

    mcmeta = check_json(pack_dir / "pack.mcmeta")
    if isinstance(mcmeta, dict):
        pack_obj = mcmeta.get("pack") if isinstance(mcmeta.get("pack"), dict) else {}
        pack_id = mcmeta.get("id") or pack_obj.get("id")
        if pack_id != "stellarity":
            errors.append(f"Unexpected Stellarity pack.mcmeta id: {pack_id!r}")

    load_vals = _tag_json_values(pack_dir / "data/minecraft/tags/function/load.json", errors=errors)
    tick_vals = _tag_json_values(pack_dir / "data/minecraft/tags/function/tick.json", errors=errors)
    for req in ("stellarity:init", "kohara:load"):
        if req not in load_vals:
            errors.append(f"Stellarity load.json missing {req}")
    for req in ("stellarity:main", "kohara:main"):
        if req not in tick_vals:
            errors.append(f"Stellarity tick.json missing {req}")

    # End Altar
    for rel in (
        "data/stellarity/structure/exit_portal/activated.nbt",
        "data/stellarity/structure/exit_portal/deactivated.nbt",
        "data/stellarity/function/structure/exit_portal/main.mcfunction",
        "data/stellarity/function/structure/exit_portal/replace.mcfunction",
        "data/stellarity/function/structure/exit_portal/activated/generate.mcfunction",
        "data/stellarity/function/structure/exit_portal/deactivated/generate.mcfunction",
        "data/stellarity/function/structure/exit_portal/gateway/choose_location.mcfunction",
    ):
        p = pack_dir / rel
        if not p.is_file() or p.stat().st_size == 0:
            errors.append(f"Missing or empty End Altar asset: {rel}")

    main_text = (pack_dir / "data/stellarity/function/main.mcfunction").read_text(encoding="utf-8", errors="replace") if (pack_dir / "data/stellarity/function/main.mcfunction").is_file() else ""
    if "stellarity:structure/exit_portal/replace" not in main_text:
        errors.append("Stellarity main.mcfunction missing stellarity:structure/exit_portal/replace")
    if "stellarity:entity/end_city/crystal/main" not in main_text and "end_city/crystal/main" not in main_text:
        errors.append("Stellarity main.mcfunction missing end_city/crystal/main")
    if "end_city/marker_init" not in main_text:
        errors.append("Stellarity main.mcfunction missing end_city/marker_init")

    ep_main = (pack_dir / "data/stellarity/function/structure/exit_portal/main.mcfunction").read_text(encoding="utf-8", errors="replace") if (pack_dir / "data/stellarity/function/structure/exit_portal/main.mcfunction").is_file() else ""
    if "stellarity:post_gen/initialize" not in ep_main:
        errors.append("Stellarity exit_portal/main.mcfunction missing stellarity:post_gen/initialize")

    # Custom Items
    item_fns = list((pack_dir / "data/stellarity/function/item").rglob("*.mcfunction"))
    if len(item_fns) < 491:
        errors.append(f"Expected >= 491 Stellarity item functions, found {len(item_fns)}")
    item_loot = list((pack_dir / "data/stellarity/loot_table/item").rglob("*.json"))
    if len(item_loot) < 156:
        errors.append(f"Expected >= 156 Stellarity item loot tables, found {len(item_loot)}")
    for f in item_loot:
        check_json(f)
    dragon_loot = check_json(pack_dir / "data/stellarity/loot_table/entity/ender_dragon.json")
    if isinstance(dragon_loot, dict) and len(dragon_loot.get("pools") or []) < 4:
        errors.append("Stellarity entity/ender_dragon.json expected >= 4 loot pools")

    # 24 End Biomes
    biomes = list((pack_dir / "data/stellarity/worldgen/biome").glob("*.json"))
    if len(biomes) != 24:
        errors.append(f"Expected 24 Stellarity End biomes, found {len(biomes)}")
    for b in biomes:
        check_json(b)

    # End Cities
    ec_loot = list((pack_dir / "data/stellarity/loot_table/end_city").rglob("*.json"))
    if len(ec_loot) != 39:
        errors.append(f"Expected 39 Stellarity End City loot tables, found {len(ec_loot)}")
    for f in ec_loot:
        check_json(f)
    ec_st_nbt = list((pack_dir / "data/stellarity/structure/end_city").rglob("*.nbt"))
    ec_mc_nbt = list((pack_dir / "data/minecraft/structure/end_city").rglob("*.nbt"))
    if len(ec_st_nbt) != 58:
        errors.append(f"Expected 58 Stellarity end_city NBT structures, found {len(ec_st_nbt)}")
    if len(ec_mc_nbt) != 20:
        errors.append(f"Expected 20 minecraft end_city NBT structures, found {len(ec_mc_nbt)}")

    # Altar of the Accursed
    altar_nbt = pack_dir / "data/stellarity/structure/altar_of_the_accursed.nbt"
    if not altar_nbt.is_file() or altar_nbt.stat().st_size == 0:
        errors.append("Missing or empty altar_of_the_accursed.nbt")
    post_gen = pack_dir / "data/stellarity/function/post_gen/initialize.mcfunction"
    if not post_gen.is_file() or "stellarity:altar_of_the_accursed 42 67 -48" not in post_gen.read_text(encoding="utf-8", errors="replace"):
        errors.append("post_gen/initialize.mcfunction missing altar_of_the_accursed placement")
    for rel in (
        "data/stellarity/function/mechanic/altar_of_the_accursed/main.mcfunction",
        "data/stellarity/function/mechanic/altar_of_the_accursed/main_activated.mcfunction",
        "data/stellarity/function/mechanic/altar_of_the_accursed/place.mcfunction",
        "data/stellarity/function/mechanic/altar_of_the_accursed/crafting/checks.mcfunction",
    ):
        if not (pack_dir / rel).is_file():
            errors.append(f"Missing Altar of the Accursed function: {rel}")
    altar_advs = list((pack_dir / "data/stellarity/advancement/altar_of_the_accursed").glob("*.json"))
    place_adv = pack_dir / "data/stellarity/advancement/event/place_altar_of_the_accursed.json"
    if len(altar_advs) != 6 or not place_adv.is_file():
        errors.append(f"Expected 6+1 Altar of the Accursed advancements, found {len(altar_advs)} + place={place_adv.is_file()}")
    for a in altar_advs + ([place_adv] if place_adv.is_file() else []):
        check_json(a)

    return {
        "ok": len(errors) == 0,
        "item_functions": len(item_fns),
        "item_loot_tables": len(item_loot),
        "biomes": len(biomes),
        "end_city_loot": len(ec_loot),
        "end_city_structures": len(ec_st_nbt) + len(ec_mc_nbt),
        "errors": errors,
    }


def inspect_datapacks_full(
    datapacks_dir: Path = WORKSPACE_DATAPACKS,
    allayops_datapacks: Path = ALLAYOPS_WORLD_DATAPACKS,
    level_dat_path: Path | None = ALLAYOPS_LEVEL_DAT,
) -> dict[str, Any]:
    junction = verify_datapacks_junction(allayops_datapacks, datapacks_dir)
    te = check_true_ending_disabled(datapacks_dir, level_dat_path)
    st_deconf = check_stellarity_deconflicted(datapacks_dir, level_dat_path)
    st_pres = check_stellarity_preserved(datapacks_dir)
    errors = junction["errors"] + te["errors"] + st_deconf["errors"] + st_pres["errors"]
    return {
        "ok": len(errors) == 0,
        "junction": junction,
        "true_ending": te,
        "stellarity_deconfliction": st_deconf,
        "stellarity_preservation": st_pres,
        "errors": errors,
    }
