"""
Pure reference implementation of IslandMath for expedition-skyblock.
Authoritative source: PROJECT.md § Interface Contracts & Explorer 2 handoff.
"""
import math
from typing import Tuple, NamedTuple


class GridPos(NamedTuple):
    u: int
    v: int


class WorldPos(NamedTuple):
    x: int
    z: int


class IslandMath:
    ISLAND_SPACING: int = 1024
    ISLAND_RADIUS: int = 128  # 256x256 buildable bounds: [-128, 128] from center
    VOID_BUFFER: int = 768    # 1024 - 256 = 768 blocks void between adjacent islands
    EMERGENCY_BUFFER_RADIUS: int = 135  # Threshold where soft clamp turns into hard rescue
    VOID_RESCUE_THRESHOLD_Y: float = -64.0  # Void auto-rescue trigger
    COMBAT_COOLDOWN_SECONDS: float = 5.0    # Combat cooldown gating /is from main world

    @staticmethod
    def index_to_grid_pos(index: int) -> GridPos:
        """
        Maps a non-negative island index (0, 1, 2, ...) to square spiral grid coordinates (u, v).
        Guarantees 0 collisions, 100% density, expanding in concentric rings.
        """
        if index <= 0:
            return GridPos(0, 0)

        # Ring number r >= 1
        r = int(math.floor((math.isqrt(index) + 1) / 2))
        prev_max = (2 * r - 1) * (2 * r - 1) - 1
        offset = index - prev_max - 1
        d = 2 * r
        side = offset // d
        step = offset % d

        u, v = 0, 0
        if side == 0:
            u = r
            v = -(r - 1) + step
        elif side == 1:
            u = (r - 1) - step
            v = r
        elif side == 2:
            u = -r
            v = (r - 1) - step
        elif side == 3:
            u = -(r - 1) + step
            v = -r

        return GridPos(u, v)

    @classmethod
    def index_to_center_pos(cls, index: int) -> WorldPos:
        """
        Converts island index to world coordinates (cx, cz).
        Each grid step is spaced by ISLAND_SPACING (1,024 blocks).
        """
        grid = cls.index_to_grid_pos(index)
        return WorldPos(grid.u * cls.ISLAND_SPACING, grid.v * cls.ISLAND_SPACING)

    @classmethod
    def is_within_island_bounds(cls, x: float, z: float, cx: float, cz: float) -> bool:
        """
        Returns True if (x, z) is within the 256x256 buildable boundary of island center (cx, cz).
        Boundary condition: |x - cx| <= 128 and |z - cz| <= 128.
        """
        return abs(x - cx) <= cls.ISLAND_RADIUS and abs(z - cz) <= cls.ISLAND_RADIUS

    @classmethod
    def get_closest_island_center(cls, x: float, z: float) -> WorldPos:
        """
        Finds the theoretical center coordinates of the nearest island grid cell.
        """
        u = round(x / cls.ISLAND_SPACING)
        v = round(z / cls.ISLAND_SPACING)
        return WorldPos(int(u * cls.ISLAND_SPACING), int(v * cls.ISLAND_SPACING))

    @classmethod
    def is_buffer_zone(cls, x: float, z: float) -> bool:
        """
        Returns True if (x, z) is in the void buffer (outside the 256x256 buildable area of all islands).
        """
        nearest_center = cls.get_closest_island_center(x, z)
        return not cls.is_within_island_bounds(x, z, nearest_center.x, nearest_center.z)

    @classmethod
    def clamp_position(cls, x: float, z: float, cx: float, cz: float) -> Tuple[float, float, bool]:
        """
        Soft clamp player position within the island boundary [-127.5, 127.5] relative to center.
        Returns (new_x, new_z, was_clamped).
        """
        dx = x - cx
        dz = z - cz
        clamped = False

        max_offset = cls.ISLAND_RADIUS - 0.5  # 127.5
        min_offset = -max_offset             # -127.5

        new_x = x
        new_z = z

        if dx > max_offset:
            new_x = cx + max_offset
            clamped = True
        elif dx < min_offset:
            new_x = cx + min_offset
            clamped = True

        if dz > max_offset:
            new_z = cz + max_offset
            clamped = True
        elif dz < min_offset:
            new_z = cz + min_offset
            clamped = True

        return (new_x, new_z, clamped)

    @classmethod
    def requires_emergency_rescue(cls, x: float, z: float, cx: float, cz: float) -> bool:
        """
        Checks if player has penetrated deep into the void buffer (|dx| > 135 or |dz| > 135).
        """
        return abs(x - cx) > cls.EMERGENCY_BUFFER_RADIUS or abs(z - cz) > cls.EMERGENCY_BUFFER_RADIUS
