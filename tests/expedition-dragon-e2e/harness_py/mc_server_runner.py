"""Isolated sandbox MC 26.3 / Fabric server runner + pure-Python RCON client.

Never mutates D:\\AllayOps\\world; reuses D:\\AllayOps\\libraries and D:\\AllayOps\\versions
via NTFS Junctions, allocates ephemeral ports, and tears down cleanly.
"""

from __future__ import annotations

import os
import select
import shutil
import socket
import struct
import subprocess
import tempfile
import threading
import time
from pathlib import Path
from typing import Any

ALLAYOPS_ROOT = Path(r"D:\AllayOps")
WORKSPACE_MODS = Path(r"d:\Minecraft-One-More-Expedition\mods")
WORKSPACE_DATAPACKS = Path(r"d:\Minecraft-One-More-Expedition\datapacks")

SERVER_LAUNCHER_NAME = "fabric-server-mc.26.3-loader.0.20.1-launcher.1.1.2.jar"

PROFILE_BASE_MODS = (
    "fabric-api-0.166.2+26.3.jar",
    "fabric-carpet-26.3+v260915.jar",
    "spark-1.10.187-fabric.jar",
)
ECONOMYCRAFT_MOD = "economycraft-fabric-1.11.0_26.3.jar"
DRAGON_MOD = "expedition-dragon-1.0.0.jar"

RCON_TYPE_COMMAND = 2
RCON_TYPE_AUTH = 3
RCON_TYPE_RESPONSE = 0
RCON_TYPE_AUTH_RESPONSE = 2


def find_free_port() -> int:
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
        s.bind(("127.0.0.1", 0))
        return int(s.getsockname()[1])


def find_java25_exe() -> str:
    candidates: list[Path] = []
    for env_key in ("MC_JAVA_EXE",):
        v = os.environ.get(env_key)
        if v:
            candidates.append(Path(v))
    java_home = os.environ.get("JAVA_HOME")
    if java_home:
        candidates.append(Path(java_home) / "bin" / "java.exe")
    adoptium = Path(r"C:\Program Files\Eclipse Adoptium")
    if adoptium.is_dir():
        for d in sorted(adoptium.glob("jdk-25*"), reverse=True):
            candidates.append(d / "bin" / "java.exe")
    which_java = shutil.which("java")
    if which_java:
        candidates.append(Path(which_java))

    for exe in candidates:
        if not exe.is_file():
            continue
        proc = subprocess.run(
            [str(exe), "-version"],
            capture_output=True,
            text=True,
            encoding="utf-8",
            errors="replace",
            check=False,
        )
        out = proc.stdout + proc.stderr
        if 'version "25' in out or "openjdk 25" in out.lower():
            return str(exe)
    raise RuntimeError("Java 25 executable not found for MC 26.3 sandbox server")


def create_junction(link_path: Path, target_path: Path) -> None:
    if not target_path.exists():
        raise FileNotFoundError(f"Junction target missing: {target_path}")
    if link_path.exists() or link_path.is_symlink():
        remove_junction(link_path)
    if os.name == "nt":
        proc = subprocess.run(
            ["cmd", "/c", "mklink", "/J", str(link_path), str(target_path)],
            capture_output=True,
            text=True,
            check=False,
        )
        if proc.returncode != 0:
            raise OSError(f"mklink /J failed: {proc.stdout} {proc.stderr}")
    else:
        os.symlink(target_path, link_path, target_is_directory=True)


def remove_junction(link_path: Path) -> None:
    if not link_path.exists() and not link_path.is_symlink():
        return
    if os.name == "nt":
        # rmdir removes an NTFS junction without deleting the target directory contents!
        subprocess.run(["cmd", "/c", "rmdir", str(link_path)], capture_output=True, check=False)
    else:
        link_path.unlink(missing_ok=True)


def encode_rcon_packet(req_id: int, pkt_type: int, payload: str) -> bytes:
    body = payload.encode("utf-8") + b"\x00\x00"
    length = 4 + 4 + len(body)
    return struct.pack("<iii", length, req_id, pkt_type) + body


def decode_rcon_packet(raw: bytes) -> tuple[int, int, str]:
    if len(raw) < 14:
        raise ValueError(f"RCON packet too short: {len(raw)}")
    (length,) = struct.unpack_from("<i", raw, 0)
    req_id, pkt_type = struct.unpack_from("<ii", raw, 4)
    payload_bytes = raw[12 : 4 + length - 2]
    return req_id, pkt_type, payload_bytes.decode("utf-8", errors="replace")


class RconClient:
    """Pure-Python stdlib Source RCON client."""

    def __init__(self, host: str, port: int, password: str, timeout: float = 10.0) -> None:
        self.host = host
        self.port = port
        self.password = password
        self.timeout = timeout
        self._sock: socket.socket | None = None
        self._next_id = 1

    def connect(self) -> None:
        sock = socket.create_connection((self.host, self.port), timeout=self.timeout)
        self._sock = sock
        try:
            req_id = self._next_id
            self._next_id += 1
            sock.sendall(encode_rcon_packet(req_id, RCON_TYPE_AUTH, self.password))
            resp_id, _, _ = self._recv_packet()
            if resp_id == -1:
                raise PermissionError("RCON authentication failed (bad password)")
        except BaseException:
            self.close()
            raise

    def _recv_exact(self, n: int) -> bytes:
        assert self._sock is not None
        buf = bytearray()
        while len(buf) < n:
            chunk = self._sock.recv(n - len(buf))
            if not chunk:
                raise ConnectionError("RCON socket closed unexpectedly")
            buf.extend(chunk)
        return bytes(buf)

    def _recv_packet(self) -> tuple[int, int, str]:
        hdr = self._recv_exact(4)
        (length,) = struct.unpack("<i", hdr)
        body = self._recv_exact(length)
        return decode_rcon_packet(hdr + body)

    def command(self, cmd: str) -> str:
        if self._sock is None:
            raise RuntimeError("RconClient not connected")
        req_id = self._next_id
        self._next_id += 1
        self._sock.sendall(encode_rcon_packet(req_id, RCON_TYPE_COMMAND, cmd))
        _, _, first_text = self._recv_packet()
        chunks = [first_text]
        last_len = len(first_text.encode("utf-8"))
        while True:
            wait_sec = 0.15 if last_len >= 3800 else 0.0
            readable, _, _ = select.select([self._sock], [], [], wait_sec)
            if not readable:
                break
            try:
                peek = self._sock.recv(1, socket.MSG_PEEK)
            except OSError:
                break
            if not peek:
                break
            try:
                pkt_id, pkt_type, more_text = self._recv_packet()
            except ConnectionError:
                break
            if pkt_type == RCON_TYPE_RESPONSE and pkt_id == req_id:
                chunks.append(more_text)
                last_len = len(more_text.encode("utf-8"))
            else:
                break
        return "".join(chunks)

    def close(self) -> None:
        if self._sock is not None:
            try:
                self._sock.close()
            finally:
                self._sock = None

    def __enter__(self) -> "RconClient":
        self.connect()
        return self

    def __exit__(self, exc_type: Any, exc: Any, tb: Any) -> None:
        self.close()


class SandboxServer:
    """Context-managed isolated MC 26.3 Fabric server."""

    def __init__(
        self,
        profile: str = "minimal",
        allayops_root: Path = ALLAYOPS_ROOT,
        workspace_mods: Path = WORKSPACE_MODS,
        workspace_datapacks: Path = WORKSPACE_DATAPACKS,
        custom_config: Path | None = None,
        require_dragon_mod: bool = True,
    ) -> None:
        if profile not in ("minimal", "no_economy", "datapack"):
            raise ValueError(f"Unknown sandbox profile: {profile}")
        self.profile = profile
        self.allayops_root = allayops_root
        self.workspace_mods = workspace_mods
        self.workspace_datapacks = workspace_datapacks
        self.custom_config = custom_config
        self.require_dragon_mod = require_dragon_mod
        self.sandbox_dir = Path(tempfile.mkdtemp(prefix=f"e2e_mc263_{profile}_"))
        self.server_port = find_free_port()
        self.rcon_port = find_free_port()
        self.rcon_password = "e2e_rcon_secret"
        self.process: subprocess.Popen[str] | None = None
        self.logs: list[str] = []
        self._log_thread: threading.Thread | None = None
        self._ready_event = threading.Event()
        self._crash_reason: str | None = None

    def provision(self, require_dragon_mod: bool | None = None) -> None:
        require_mod = self.require_dragon_mod if require_dragon_mod is None else require_dragon_mod
        launcher_src = self.allayops_root / SERVER_LAUNCHER_NAME
        if not launcher_src.is_file():
            raise FileNotFoundError(f"Server launcher missing: {launcher_src}")
        shutil.copy2(launcher_src, self.sandbox_dir / SERVER_LAUNCHER_NAME)

        create_junction(self.sandbox_dir / "libraries", self.allayops_root / "libraries")
        create_junction(self.sandbox_dir / "versions", self.allayops_root / "versions")

        mods_dir = self.sandbox_dir / "mods"
        mods_dir.mkdir(parents=True, exist_ok=True)
        needed = list(PROFILE_BASE_MODS)
        if self.profile != "no_economy":
            needed.append(ECONOMYCRAFT_MOD)
        needed.append(DRAGON_MOD)

        for mod_name in needed:
            src = self.workspace_mods / mod_name
            if not src.is_file():
                src = self.allayops_root / "mods" / mod_name
            if src.is_file():
                shutil.copy2(src, mods_dir / mod_name)
            elif mod_name == DRAGON_MOD and require_mod:
                raise FileNotFoundError(
                    f"Required mod JAR missing from {self.workspace_mods} and {self.allayops_root / 'mods'}: {mod_name}"
                )

        if self.profile == "datapack":
            world_dp = self.sandbox_dir / "world" / "datapacks"
            world_dp.parent.mkdir(parents=True, exist_ok=True)
            create_junction(world_dp, self.workspace_datapacks)

        if self.custom_config and self.custom_config.is_file():
            cfg_dir = self.sandbox_dir / "config"
            cfg_dir.mkdir(parents=True, exist_ok=True)
            shutil.copy2(self.custom_config, cfg_dir / "expedition-dragon.json")

        (self.sandbox_dir / "eula.txt").write_text("eula=true\n", encoding="utf-8")
        (self.sandbox_dir / "server.properties").write_text(
            "\n".join([
                f"server-port={self.server_port}",
                "enable-rcon=true",
                f"rcon.port={self.rcon_port}",
                f"rcon.password={self.rcon_password}",
                "online-mode=false",
                "op-permission-level=4",
                "function-permission-level=2",
                "level-name=world",
                "level-type=minecraft:flat",
                "difficulty=hard",
                "max-tick-time=-1",
                "pause-when-empty-seconds=0",
                "view-distance=4",
                "simulation-distance=4",
            ]) + "\n",
            encoding="utf-8",
        )

    def start(self, timeout_seconds: float = 90.0) -> None:
        self.provision()
        java_exe = find_java25_exe()
        self.process = subprocess.Popen(
            [java_exe, "-Xms512M", "-Xmx2G", "-jar", SERVER_LAUNCHER_NAME, "nogui"],
            cwd=str(self.sandbox_dir),
            stdin=subprocess.PIPE,
            stdout=subprocess.PIPE,
            stderr=subprocess.STDOUT,
            text=True,
            encoding="utf-8",
            errors="replace",
            bufsize=1,
        )

        def _pump() -> None:
            assert self.process is not None and self.process.stdout is not None
            for line in self.process.stdout:
                stripped = line.rstrip("\r\n")
                self.logs.append(stripped)
                if "Done (" in stripped and "For help" in stripped:
                    self._ready_event.set()
                if "MixinApplyError" in stripped or "Failed to start the minecraft server" in stripped:
                    self._crash_reason = stripped

        self._log_thread = threading.Thread(target=_pump, daemon=True)
        self._log_thread.start()

        deadline = time.monotonic() + timeout_seconds
        while time.monotonic() < deadline:
            if self._ready_event.is_set():
                return
            if self.process.poll() is not None:
                tail = "\n".join(self.logs[-30:])
                raise RuntimeError(
                    f"Sandbox server exited prematurely ({self.process.returncode}): {self._crash_reason or tail}"
                )
            time.sleep(0.2)

        raise TimeoutError(f"Sandbox server failed to reach Ready within {timeout_seconds}s")

    def connect_rcon(self) -> RconClient:
        client = RconClient("127.0.0.1", self.rcon_port, self.rcon_password)
        client.connect()
        return client

    def stop(self, timeout_seconds: float = 20.0) -> None:
        if self.process is not None and self.process.poll() is None:
            try:
                if self.process.stdin:
                    self.process.stdin.write("stop\n")
                    self.process.stdin.flush()
                self.process.wait(timeout=timeout_seconds)
            except Exception:
                self.process.kill()
                self.process.wait(timeout=5)
        if self._log_thread is not None:
            self._log_thread.join(timeout=2)

    def cleanup(self) -> None:
        self.stop()
        remove_junction(self.sandbox_dir / "libraries")
        remove_junction(self.sandbox_dir / "versions")
        remove_junction(self.sandbox_dir / "world" / "datapacks")
        shutil.rmtree(self.sandbox_dir, ignore_errors=True)

    def __enter__(self) -> "SandboxServer":
        try:
            self.start()
        except BaseException:
            self.cleanup()
            raise
        return self

    def __exit__(self, exc_type: Any, exc: Any, tb: Any) -> None:
        self.cleanup()
