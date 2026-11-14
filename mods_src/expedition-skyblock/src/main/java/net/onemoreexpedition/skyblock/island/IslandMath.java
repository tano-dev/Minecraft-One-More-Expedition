package net.onemoreexpedition.skyblock.island;

public final class IslandMath {
    public static final int ISLAND_SPACING = 1024;
    public static final int ISLAND_RADIUS = 128; // 256x256 buildable bounds

    private IslandMath() {}

    /**
     * Maps sequential island index (0, 1, 2...) to unique world grid coordinates (u, v).
     */
    public static GridPos indexToGridPos(int index) {
        if (index <= 0) {
            return new GridPos(0, 0);
        }
        int r = (int) Math.floor((Math.sqrt(index) + 1.0) / 2.0);
        int prevMax = (2 * r - 1) * (2 * r - 1) - 1;
        int offset = index - prevMax - 1;
        int d = 2 * r;
        int side = offset / d;
        int step = offset % d;

        int u = 0;
        int v = 0;
        switch (side) {
            case 0 -> { u = r; v = -(r - 1) + step; }
            case 1 -> { u = (r - 1) - step; v = r; }
            case 2 -> { u = -r; v = (r - 1) - step; }
            case 3 -> { u = -(r - 1) + step; v = -r; }
            default -> {}
        }
        return new GridPos(u, v);
    }

    public static WorldPos indexToCenterPos(int index) {
        GridPos grid = indexToGridPos(index);
        return new WorldPos(grid.u() * ISLAND_SPACING, grid.v() * ISLAND_SPACING);
    }

    public static boolean isWithinIslandBounds(int blockX, int blockZ, int centerX, int centerZ) {
        return Math.abs(blockX - centerX) <= ISLAND_RADIUS && Math.abs(blockZ - centerZ) <= ISLAND_RADIUS;
    }

    public static WorldPos getNearestIslandCenter(int blockX, int blockZ) {
        int gridX = (int) Math.round((double) blockX / ISLAND_SPACING);
        int gridZ = (int) Math.round((double) blockZ / ISLAND_SPACING);
        return new WorldPos(gridX * ISLAND_SPACING, gridZ * ISLAND_SPACING);
    }

    public static boolean isBufferZone(int blockX, int blockZ) {
        WorldPos nearest = getNearestIslandCenter(blockX, blockZ);
        return !isWithinIslandBounds(blockX, blockZ, nearest.x(), nearest.z());
    }

    public record GridPos(int u, int v) {}
    public record WorldPos(int x, int z) {}
}
