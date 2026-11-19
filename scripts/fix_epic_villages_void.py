"""
Fix Epic Villages 2.0.0 - Flood-fill exterior minecraft:air -> minecraft:structure_void
Chỉ chạy với datapack "Epic Villages 2.0.0".

Bug fix so với script gốc: keyword matching dùng path SEGMENTS (thư mục/file name),
KHÔNG phải toàn bộ chuỗi path → tránh "raft" ⊂ "minecraft" false positive.
"""

import gzip
import os
import struct
from collections import deque

REPO_ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DATAPACKS_DIR = os.path.join(REPO_ROOT, "datapacks")
TARGET_DATAPACK = "Epic Villages 2.0.0"

# Check từng PATH SEGMENT, không phải toàn bộ chuỗi
UNDERGROUND_SEGMENTS = {
    "underground", "mineshaft", "dungeon", "cave", "cavern", "crypt",
    "tomb", "catacomb", "ancient_city", "stronghold", "buried", "bunker",
    "sewer", "cellar", "undercroft", "fossil"
}
SKIP_NON_LAND_SEGMENTS = {
    "ship", "vessel", "boat", "raft", "lighthouse", "island",
    "aqua", "ocean", "sea", "underwater", "sunken", "sky", "cloud",
    "floating", "airship", "balloon", "void", "nether"
}
# Prefix match cho "end_xxx"
SKIP_NON_LAND_PREFIXES = ("end_",)


def _seg_is_skip(seg: str) -> bool:
    """True nếu segment (thư mục hoặc tên file không có ext) nằm trong skip list."""
    s = seg.lower()
    if s in UNDERGROUND_SEGMENTS or s in SKIP_NON_LAND_SEGMENTS:
        return True
    if any(s.startswith(p) for p in SKIP_NON_LAND_PREFIXES):
        return True
    return False


def _path_should_skip(fp: str) -> bool:
    """Kiểm tra từng segment của path (không tính datapack root và namespace)."""
    rel = os.path.relpath(fp, DATAPACKS_DIR)
    segments = rel.replace("\\", "/").split("/")
    # segments[0] = datapack name, segments[1] = "data", segments[2] = namespace
    # Chỉ check từ segments[3] trở đi (tránh "minecraft" → "craft" false positive)
    for seg in segments[3:]:
        name_no_ext = os.path.splitext(seg)[0]
        if _seg_is_skip(name_no_ext):
            return True
    return False


# --- Lossless Pure-Python NBT Reader/Writer ---
TAG_END = 0; TAG_BYTE = 1; TAG_SHORT = 2; TAG_INT = 3; TAG_LONG = 4
TAG_FLOAT = 5; TAG_DOUBLE = 6; TAG_BYTE_ARRAY = 7; TAG_STRING = 8
TAG_LIST = 9; TAG_COMPOUND = 10; TAG_INT_ARRAY = 11; TAG_LONG_ARRAY = 12


class NBTReader:
    def __init__(self, data):
        self.data = data; self.pos = 0

    def read(self, n):
        chunk = self.data[self.pos:self.pos + n]; self.pos += n; return chunk

    def read_fmt(self, fmt):
        size = struct.calcsize(fmt)
        return struct.unpack(fmt, self.read(size))[0]

    def read_str(self):
        length = self.read_fmt(">H"); return self.read(length).decode("utf-8")

    def read_payload(self, tag_type):
        if tag_type == TAG_BYTE:   return ("byte",   self.read_fmt(">b"))
        if tag_type == TAG_SHORT:  return ("short",  self.read_fmt(">h"))
        if tag_type == TAG_INT:    return ("int",    self.read_fmt(">i"))
        if tag_type == TAG_LONG:   return ("long",   self.read_fmt(">q"))
        if tag_type == TAG_FLOAT:  return ("float",  self.read_fmt(">f"))
        if tag_type == TAG_DOUBLE: return ("double", self.read_fmt(">d"))
        if tag_type == TAG_BYTE_ARRAY:
            n = self.read_fmt(">i")
            return ("byte_array", list(struct.unpack(f">{n}b", self.read(n))))
        if tag_type == TAG_STRING: return ("string", self.read_str())
        if tag_type == TAG_LIST:
            elem_type = self.read_fmt(">B"); n = self.read_fmt(">i")
            items = [self.read_payload(elem_type) for _ in range(n)]
            return ("list", (elem_type, items))
        if tag_type == TAG_COMPOUND:
            items = []
            while True:
                t = self.read_fmt(">B")
                if t == TAG_END: break
                name = self.read_str()
                items.append((name, t, self.read_payload(t)))
            return ("compound", items)
        if tag_type == TAG_INT_ARRAY:
            n = self.read_fmt(">i")
            return ("int_array", list(struct.unpack(f">{n}i", self.read(n * 4))))
        if tag_type == TAG_LONG_ARRAY:
            n = self.read_fmt(">i")
            return ("long_array", list(struct.unpack(f">{n}q", self.read(n * 8))))
        raise ValueError(f"Unknown tag type {tag_type}")

    def read_root(self):
        tag_type = self.read_fmt(">B"); name = self.read_str()
        return name, tag_type, self.read_payload(tag_type)


class NBTWriter:
    def __init__(self): self.buf = bytearray()

    def write(self, b): self.buf.extend(b)

    def write_fmt(self, fmt, *v): self.write(struct.pack(fmt, *v))

    def write_str(self, s):
        b = s.encode("utf-8"); self.write_fmt(">H", len(b)); self.write(b)

    def write_payload(self, tag_type, payload):
        """payload is the VALUE part of a (type_name, value) tuple from read_payload."""
        if tag_type == TAG_BYTE:       self.write_fmt(">b", payload)
        elif tag_type == TAG_SHORT:    self.write_fmt(">h", payload)
        elif tag_type == TAG_INT:      self.write_fmt(">i", payload)
        elif tag_type == TAG_LONG:     self.write_fmt(">q", payload)
        elif tag_type == TAG_FLOAT:    self.write_fmt(">f", payload)
        elif tag_type == TAG_DOUBLE:   self.write_fmt(">d", payload)
        elif tag_type == TAG_BYTE_ARRAY:
            self.write_fmt(">i", len(payload))
            self.write(struct.pack(f">{len(payload)}b", *payload))
        elif tag_type == TAG_STRING:   self.write_str(payload)
        elif tag_type == TAG_LIST:
            # payload = (elem_type, [item, ...]) where item = (type_name, value)
            elem_type, items = payload
            self.write_fmt(">B", elem_type); self.write_fmt(">i", len(items))
            for item in items:
                self.write_payload(elem_type, item[1])
        elif tag_type == TAG_COMPOUND:
            # payload = [(name, tag_type, (type_name, value)), ...]  — fields list
            for name, t, (type_name, v) in payload:
                self.write_fmt(">B", t); self.write_str(name); self.write_payload(t, v)
            self.write_fmt(">B", TAG_END)
        elif tag_type == TAG_INT_ARRAY:
            self.write_fmt(">i", len(payload))
            self.write(struct.pack(f">{len(payload)}i", *payload))
        elif tag_type == TAG_LONG_ARRAY:
            self.write_fmt(">i", len(payload))
            self.write(struct.pack(f">{len(payload)}q", *payload))

    def write_root(self, name, tag_type, root_tuple):
        """root_tuple = ('compound', fields_list) from read_payload."""
        self.write_fmt(">B", tag_type); self.write_str(name)
        # root_tuple[1] = fields_list for a compound
        self.write_payload(tag_type, root_tuple[1])
        return bytes(self.buf)


def _compound_get(fields, key):
    """fields: list of (name, tag_type, payload) — the inner list of a compound."""
    for name, t, v in fields:
        if name == key: return (t, v)
    return None


def _compound_get_val(fields, key):
    """Shortcut: returns just the payload value, or None."""
    result = _compound_get(fields, key)
    return result[1] if result else None


def _compound_set_field(fields, key, new_type, new_val):
    """fields: list of (name, tag_type, payload)."""
    for i, (name, t, v) in enumerate(fields):
        if name == key:
            fields[i] = (name, new_type, new_val); return
    fields.append((key, new_type, new_val))


def flood_fill_nbt_exterior_air(nbt_path):
    with gzip.open(nbt_path, "rb") as f:
        raw = f.read()

    reader = NBTReader(raw)
    name, tag_type, root = reader.read_root()

    # root là ('compound', fields) → fields là list of (name, tag, val)
    root_fields = root[1]

    # Lấy size, palette, blocks
    size_val    = _compound_get_val(root_fields, "size")     # ('list', (3, [int_items]))
    palette_val = _compound_get_val(root_fields, "palette")  # ('list', (10, [compound_items]))
    blocks_val  = _compound_get_val(root_fields, "blocks")   # ('list', (10, [compound_items]))

    if not size_val or not palette_val or not blocks_val:
        return False, 0

    # size_val = ('list', (TAG_INT, [('int',w), ('int',h), ('int',d)]))
    _size_elem_type, size_items = size_val[1]
    dims = [v[1] for v in size_items]
    if len(dims) < 3: return False, 0
    w, h, d = dims[0], dims[1], dims[2]

    # palette_val = ('list', (TAG_COMPOUND, [('compound', fields), ...]))
    _pal_elem_type, palette_items = palette_val[1]
    # blocks_val  = ('list', (TAG_COMPOUND, [('compound', fields), ...]))
    _blk_elem_type, block_entries = blocks_val[1]

    # Tìm index air và structure_void trong palette
    # Mỗi palette entry: ('compound', fields) → fields chứa ('Name', TAG_STRING, ('string', 'minecraft:xxx'))
    air_idx = void_idx = None
    for i, pal_entry in enumerate(palette_items):
        # pal_entry = ('compound', fields)
        fields = pal_entry[1]
        name_val = _compound_get_val(fields, "Name")  # ('string', 'minecraft:...')
        if name_val and name_val[1] == "minecraft:air":
            air_idx = i
        if name_val and name_val[1] == "minecraft:structure_void":
            void_idx = i

    if air_idx is None:
        return False, 0  # Không có air → không cần xử lý

    # Build grid: coord → palette_state_index
    # Mỗi block entry: ('compound', fields) → fields chứa 'state' (int) và 'pos' (list of 3 ints)
    grid_state = {}
    grid_block_idx = {}
    for b_idx, blk_entry in enumerate(block_entries):
        # blk_entry = ('compound', fields)
        fields = blk_entry[1]
        pos_val   = _compound_get_val(fields, "pos")    # ('list', (TAG_INT, [int_items]))
        state_val = _compound_get_val(fields, "state")  # ('int', idx)
        if pos_val and state_val is not None:
            _pos_elem, pos_items = pos_val[1]
            bx, by, bz = pos_items[0][1], pos_items[1][1], pos_items[2][1]
            grid_state[(bx, by, bz)] = state_val[1]
            grid_block_idx[(bx, by, bz)] = b_idx

    # BFS flood-fill từ 6 mặt bounding box
    visited = set()
    q = deque()

    def seed_face(coords):
        for coord in coords:
            if grid_state.get(coord) == air_idx and coord not in visited:
                visited.add(coord); q.append(coord)

    seed_face([(x, y, z) for x in range(w) for y in range(h) for z in (0, d-1)])
    seed_face([(x, y, z) for x in range(w) for z in range(d) for y in (0, h-1)])
    seed_face([(x, y, z) for y in range(h) for z in range(d) for x in (0, w-1)])

    while q:
        cx, cy, cz = q.popleft()
        for dx, dy, dz in ((1,0,0),(-1,0,0),(0,1,0),(0,-1,0),(0,0,1),(0,0,-1)):
            nx, ny, nz = cx+dx, cy+dy, cz+dz
            if 0 <= nx < w and 0 <= ny < h and 0 <= nz < d:
                if (nx, ny, nz) not in visited and grid_state.get((nx, ny, nz)) == air_idx:
                    visited.add((nx, ny, nz)); q.append((nx, ny, nz))

    if not visited:
        return False, 0

    # Thêm structure_void vào palette nếu chưa có
    # palette_items là list of ('compound', fields)
    if void_idx is None:
        void_idx = len(palette_items)
        palette_items.append(("compound", [("Name", TAG_STRING, ("string", "minecraft:structure_void"))]))

    # Replace exterior air → structure_void trong block entries
    # block_entries[b_idx] = ('compound', fields) → fields chứa ('state', TAG_INT, ('int', idx))
    for coord in visited:
        b_idx = grid_block_idx[coord]
        blk_fields = block_entries[b_idx][1]
        _compound_set_field(blk_fields, "state", TAG_INT, ("int", void_idx))

    out_bytes = NBTWriter().write_root(name, tag_type, root)
    with gzip.open(nbt_path, "wb") as f:
        f.write(out_bytes)

    return True, len(visited)


def run():
    dp_path = os.path.join(DATAPACKS_DIR, TARGET_DATAPACK)
    if not os.path.isdir(dp_path):
        print(f"[ERROR] Datapack not found: {dp_path}")
        return

    fixed_files = 0
    total_blocks = 0
    skipped = []

    for root, _, files in os.walk(dp_path):
        parts = os.path.normpath(root).split(os.sep)
        if "structure" not in parts or "worldgen" in parts:
            continue

        for f in sorted(files):
            if not f.endswith(".nbt"):
                continue
            fp = os.path.join(root, f)

            if _path_should_skip(fp):
                skipped.append(os.path.relpath(fp, dp_path))
                continue

            try:
                changed, cnt = flood_fill_nbt_exterior_air(fp)
                if changed:
                    fixed_files += 1
                    total_blocks += cnt
                    rel = os.path.relpath(fp, dp_path)
                    print(f"  [fixed] {rel}  ({cnt:,} exterior air → void)")
                else:
                    rel = os.path.relpath(fp, dp_path)
                    print(f"  [skip-no-air] {rel}")
            except Exception as e:
                rel = os.path.relpath(fp, dp_path)
                print(f"  [WARN] {rel}: {e}")

    print()
    print(f"=== Done ===")
    print(f"Fixed files  : {fixed_files}")
    print(f"Total blocks : {total_blocks:,} exterior air → structure_void")
    print(f"Skipped (non-land segments): {len(skipped)}")
    if skipped:
        for s in skipped[:5]:
            print(f"  {s}")


if __name__ == "__main__":
    run()
