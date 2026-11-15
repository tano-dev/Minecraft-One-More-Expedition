"""
AllayOps & OneMoreExpedition - Global Surface Structure Terrain Naturalizer
1. Leaves all underground / buried structures completely untouched.
2. On surface land structures (`step == "surface_structures"`), replaces
   `beard_box` and `none` with `beard_thin` for smooth natural foundations.
3. Performs 3D BFS flood-fill on ALL surface `.nbt` templates across all
   datapacks to convert exterior bounding-box `minecraft:air` into
   `minecraft:structure_void`, preventing rectangular terrain/tree slicing
   while preserving interior rooms.
"""

import gzip
import json
import os
import struct
from collections import deque

REPO_ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DATAPACKS_DIR = os.path.join(REPO_ROOT, "datapacks")

UNDERGROUND_OR_BURIED_KEYWORDS = (
    "underground", "mineshaft", "dungeon", "cave", "cavern", "crypt",
    "tomb", "catacomb", "ancient_city", "stronghold", "buried", "bunker",
    "sewer", "cellar", "undercroft", "fossil"
)

SKIP_NON_LAND_KEYWORDS = (
    "ship", "vessel", "boat", "raft", "lighthouse", "island",
    "aqua", "ocean", "sea", "underwater", "sunken", "sky", "cloud",
    "floating", "airship", "balloon", "end_", "void", "nether"
)

SKIP_PACK_KEYWORDS = (
    "mineshaft", "underwater", "u_sea", "sunken", "medieval_end",
    "medieval_nether", "nullscape", "incendium", "stellarity", "ancient city"
)

# --- Lossless Pure-Python NBT Reader/Writer ---
TAG_END = 0
TAG_BYTE = 1
TAG_SHORT = 2
TAG_INT = 3
TAG_LONG = 4
TAG_FLOAT = 5
TAG_DOUBLE = 6
TAG_BYTE_ARRAY = 7
TAG_STRING = 8
TAG_LIST = 9
TAG_COMPOUND = 10
TAG_INT_ARRAY = 11
TAG_LONG_ARRAY = 12


class NBTReader:
    def __init__(self, data: bytes):
        self.data = data
        self.pos = 0

    def read(self, n: int) -> bytes:
        chunk = self.data[self.pos : self.pos + n]
        self.pos += n
        return chunk

    def read_fmt(self, fmt: str):
        size = struct.calcsize(fmt)
        return struct.unpack(fmt, self.read(size))[0]

    def read_str(self) -> str:
        length = self.read_fmt(">H")
        return self.read(length).decode("utf-8")

    def read_payload(self, tag_type: int):
        if tag_type == TAG_BYTE:
            return ("byte", self.read_fmt(">b"))
        if tag_type == TAG_SHORT:
            return ("short", self.read_fmt(">h"))
        if tag_type == TAG_INT:
            return ("int", self.read_fmt(">i"))
        if tag_type == TAG_LONG:
            return ("long", self.read_fmt(">q"))
        if tag_type == TAG_FLOAT:
            return ("float", self.read_fmt(">f"))
        if tag_type == TAG_DOUBLE:
            return ("double", self.read_fmt(">d"))
        if tag_type == TAG_BYTE_ARRAY:
            n = self.read_fmt(">i")
            return ("byte_array", list(struct.unpack(f">{n}b", self.read(n))))
        if tag_type == TAG_STRING:
            return ("string", self.read_str())
        if tag_type == TAG_LIST:
            elem_type = self.read_fmt(">B")
            n = self.read_fmt(">i")
            items = [self.read_payload(elem_type) for _ in range(n)]
            return ("list", (elem_type, items))
        if tag_type == TAG_COMPOUND:
            items = []
            while True:
                t = self.read_fmt(">B")
                if t == TAG_END:
                    break
                name = self.read_str()
                val = self.read_payload(t)
                items.append((name, t, val))
            return ("compound", items)
        if tag_type == TAG_INT_ARRAY:
            n = self.read_fmt(">i")
            return ("int_array", list(struct.unpack(f">{n}i", self.read(n * 4))))
        if tag_type == TAG_LONG_ARRAY:
            n = self.read_fmt(">i")
            return ("long_array", list(struct.unpack(f">{n}q", self.read(n * 8))))
        raise ValueError(f"Unknown NBT tag type: {tag_type}")

    def read_root(self):
        t = self.read_fmt(">B")
        name = self.read_str()
        val = self.read_payload(t)
        return name, t, val


class NBTWriter:
    def __init__(self):
        self.buf = bytearray()

    def write_fmt(self, fmt: str, *args):
        self.buf.extend(struct.pack(fmt, *args))

    def write_str(self, s: str):
        b = s.encode("utf-8")
        self.write_fmt(">H", len(b))
        self.buf.extend(b)

    def write_payload(self, tag_type: int, node):
        _, val = node
        if tag_type == TAG_BYTE:
            self.write_fmt(">b", val)
        elif tag_type == TAG_SHORT:
            self.write_fmt(">h", val)
        elif tag_type == TAG_INT:
            self.write_fmt(">i", val)
        elif tag_type == TAG_LONG:
            self.write_fmt(">q", val)
        elif tag_type == TAG_FLOAT:
            self.write_fmt(">f", val)
        elif tag_type == TAG_DOUBLE:
            self.write_fmt(">d", val)
        elif tag_type == TAG_BYTE_ARRAY:
            self.write_fmt(">i", len(val))
            self.buf.extend(struct.pack(f">{len(val)}b", *val))
        elif tag_type == TAG_STRING:
            self.write_str(val)
        elif tag_type == TAG_LIST:
            elem_type, items = val
            self.write_fmt(">B", elem_type)
            self.write_fmt(">i", len(items))
            for item in items:
                self.write_payload(elem_type, item)
        elif tag_type == TAG_COMPOUND:
            for name, t, child in val:
                self.write_fmt(">B", t)
                self.write_str(name)
                self.write_payload(t, child)
            self.write_fmt(">B", TAG_END)
        elif tag_type == TAG_INT_ARRAY:
            self.write_fmt(">i", len(val))
            self.buf.extend(struct.pack(f">{len(val)}i", *val))
        elif tag_type == TAG_LONG_ARRAY:
            self.write_fmt(">i", len(val))
            self.buf.extend(struct.pack(f">{len(val)}q", *val))
        else:
            raise ValueError(f"Unknown NBT tag type: {tag_type}")

    def write_root(self, name: str, tag_type: int, node) -> bytes:
        self.write_fmt(">B", tag_type)
        self.write_str(name)
        self.write_payload(tag_type, node)
        return bytes(self.buf)


def _compound_get(compound_node, key):
    for name, _, val in compound_node[1]:
        if name == key:
            return val
    return None


def _compound_set_field(compound_node, key, tag_type, new_val):
    items = compound_node[1]
    for i, (name, _, _) in enumerate(items):
        if name == key:
            items[i] = (key, tag_type, new_val)
            return
    items.append((key, tag_type, new_val))


def flood_fill_nbt_exterior_air(nbt_path: str) -> tuple[bool, int]:
    """Convert exterior bounding-box minecraft:air -> minecraft:structure_void in an .nbt file."""
    with gzip.open(nbt_path, "rb") as f:
        raw = f.read()

    if b"minecraft:air" not in raw:
        return False, 0

    name, tag_type, root = NBTReader(raw).read_root()
    size_node = _compound_get(root, "size")
    palette_node = _compound_get(root, "palette")
    blocks_node = _compound_get(root, "blocks")

    if not size_node or not palette_node or not blocks_node:
        return False, 0

    w, h, d = [x[1] for x in size_node[1][1]]
    palette_items = palette_node[1][1]

    air_idx = None
    void_idx = None
    for idx, p_entry in enumerate(palette_items):
        pname_node = _compound_get(p_entry, "Name")
        if pname_node:
            if pname_node[1] == "minecraft:air":
                air_idx = idx
            elif pname_node[1] == "minecraft:structure_void":
                void_idx = idx

    if air_idx is None:
        return False, 0

    grid_state = {}
    grid_block_idx = {}
    block_entries = blocks_node[1][1]
    for b_idx, b_entry in enumerate(block_entries):
        pos_node = _compound_get(b_entry, "pos")
        state_node = _compound_get(b_entry, "state")
        if pos_node and state_node:
            bx, by, bz = [x[1] for x in pos_node[1][1]]
            grid_state[(bx, by, bz)] = state_node[1]
            grid_block_idx[(bx, by, bz)] = b_idx

    visited = set()
    q = deque()
    for x in range(w):
        for y in range(h):
            for z in (0, d - 1):
                if grid_state.get((x, y, z)) == air_idx and (x, y, z) not in visited:
                    visited.add((x, y, z))
                    q.append((x, y, z))
    for x in range(w):
        for z in range(d):
            for y in (0, h - 1):
                if grid_state.get((x, y, z)) == air_idx and (x, y, z) not in visited:
                    visited.add((x, y, z))
                    q.append((x, y, z))
    for y in range(h):
        for z in range(d):
            for x in (0, w - 1):
                if grid_state.get((x, y, z)) == air_idx and (x, y, z) not in visited:
                    visited.add((x, y, z))
                    q.append((x, y, z))

    while q:
        cx, cy, cz = q.popleft()
        for dx, dy, dz in ((1, 0, 0), (-1, 0, 0), (0, 1, 0), (0, -1, 0), (0, 0, 1), (0, 0, -1)):
            nx, ny, nz = cx + dx, cy + dy, cz + dz
            if 0 <= nx < w and 0 <= ny < h and 0 <= nz < d:
                if (nx, ny, nz) not in visited and grid_state.get((nx, ny, nz)) == air_idx:
                    visited.add((nx, ny, nz))
                    q.append((nx, ny, nz))

    if not visited:
        return False, 0

    if void_idx is None:
        void_idx = len(palette_items)
        palette_items.append(("compound", [("Name", TAG_STRING, ("string", "minecraft:structure_void"))]))

    for coord in visited:
        b_idx = grid_block_idx[coord]
        _compound_set_field(block_entries[b_idx], "state", TAG_INT, ("int", void_idx))

    out_bytes = NBTWriter().write_root(name, tag_type, root)
    with gzip.open(nbt_path, "wb") as f:
        f.write(out_bytes)

    return True, len(visited)


def fix_surface_terrain_adaptation():
    """Upgrade surface land structures from `none` or `beard_box` -> `beard_thin` (skip underground/buried)."""
    changed = 0

    for dp in sorted(os.listdir(DATAPACKS_DIR)):
        dp_path = os.path.join(DATAPACKS_DIR, dp)
        if not os.path.isdir(dp_path) or dp.startswith("_") or dp == "expedition_enchantments_control":
            continue
        if any(k in dp.lower() for k in SKIP_PACK_KEYWORDS):
            continue

        for root, _, files in os.walk(dp_path):
            if "overlay_" in root:
                continue
            parts = os.path.normpath(root).split(os.sep)
            if "worldgen" in parts and "structure" in parts and "structure_set" not in parts:
                for f in files:
                    if not f.endswith(".json"):
                        continue
                    fp = os.path.join(root, f)
                    rel_lower = os.path.relpath(fp, DATAPACKS_DIR).lower()
                    if any(k in rel_lower for k in UNDERGROUND_OR_BURIED_KEYWORDS):
                        continue
                    if any(k in rel_lower for k in SKIP_NON_LAND_KEYWORDS):
                        continue

                    try:
                        with open(fp, "r", encoding="utf-8") as jf:
                            d = json.load(jf)
                    except Exception:
                        continue

                    if d.get("step") != "surface_structures":
                        continue

                    ta = d.get("terrain_adaptation", "none")
                    if ta in ("none", "beard_box"):
                        d["terrain_adaptation"] = "beard_thin"
                        with open(fp, "w", encoding="utf-8") as jf:
                            json.dump(d, jf, indent=4, ensure_ascii=False)
                        changed += 1

    print(f"[terrain_adaptation] Surface land structures upgraded to beard_thin: {changed}")


def fix_all_surface_nbt_templates():
    """Flood-fill exterior air -> structure_void on ALL surface .nbt files across all datapacks."""
    fixed_files = 0
    total_blocks_converted = 0

    for dp in sorted(os.listdir(DATAPACKS_DIR)):
        dp_path = os.path.join(DATAPACKS_DIR, dp)
        if not os.path.isdir(dp_path) or dp.startswith("_") or dp == "expedition_enchantments_control":
            continue
        if any(k in dp.lower() for k in SKIP_PACK_KEYWORDS):
            continue

        for root, _, files in os.walk(dp_path):
            if "overlay_" in root:
                continue
            parts = os.path.normpath(root).split(os.sep)
            if "structure" in parts and "worldgen" not in parts:
                for f in sorted(files):
                    if not f.endswith(".nbt"):
                        continue
                    fp = os.path.join(root, f)
                    rel_lower = os.path.relpath(fp, DATAPACKS_DIR).lower()
                    if any(k in rel_lower for k in UNDERGROUND_OR_BURIED_KEYWORDS):
                        continue
                    if any(k in rel_lower for k in SKIP_NON_LAND_KEYWORDS):
                        continue
                    try:
                        changed, cnt = flood_fill_nbt_exterior_air(fp)
                        if changed:
                            fixed_files += 1
                            total_blocks_converted += cnt
                    except Exception as e:
                        print(f"[warn] Failed {rel_lower}: {e}")

    print(f"[nbt_flood_fill] Fixed {fixed_files} surface .nbt files ({total_blocks_converted:,} exterior air blocks -> structure_void)")


if __name__ == "__main__":
    fix_surface_terrain_adaptation()
    fix_all_surface_nbt_templates()
