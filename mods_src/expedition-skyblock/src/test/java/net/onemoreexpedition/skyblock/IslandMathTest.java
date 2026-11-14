package net.onemoreexpedition.skyblock;

import net.onemoreexpedition.skyblock.island.IslandMath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class IslandMathTest {

    @Test
    @DisplayName("Index 0 maps to (0, 0) center")
    void testIndexZeroCenter() {
        IslandMath.GridPos grid = IslandMath.indexToGridPos(0);
        assertEquals(0, grid.u());
        assertEquals(0, grid.v());

        IslandMath.WorldPos center = IslandMath.indexToCenterPos(0);
        assertEquals(0, center.x());
        assertEquals(0, center.z());
    }

    @Test
    @DisplayName("Index 1 maps to (1024, 0) center")
    void testIndexOneCenter() {
        IslandMath.GridPos grid = IslandMath.indexToGridPos(1);
        assertEquals(1, grid.u());
        assertEquals(0, grid.v());

        IslandMath.WorldPos center = IslandMath.indexToCenterPos(1);
        assertEquals(1024, center.x());
        assertEquals(0, center.z());
    }

    @Test
    @DisplayName("First ring of 8 islands surrounds origin with 1024-block spacing")
    void testFirstRingCoordinates() {
        // Spiral ring 1: indices 1 to 8
        int[][] expected = {
                {1024, 0},      // Index 1: (1, 0)
                {1024, 1024},   // Index 2: (1, 1)
                {0, 1024},      // Index 3: (0, 1)
                {-1024, 1024},  // Index 4: (-1, 1)
                {-1024, 0},     // Index 5: (-1, 0)
                {-1024, -1024}, // Index 6: (-1, -1)
                {0, -1024},     // Index 7: (0, -1)
                {1024, -1024}   // Index 8: (1, -1)
        };

        for (int i = 1; i <= 8; i++) {
            IslandMath.WorldPos pos = IslandMath.indexToCenterPos(i);
            assertEquals(expected[i - 1][0], pos.x(), "Mismatch in X for index " + i);
            assertEquals(expected[i - 1][1], pos.z(), "Mismatch in Z for index " + i);
        }
    }

    @Test
    @DisplayName("Verify 0 collisions across 10,000 indices")
    void testZeroCollisionsLargeScale() {
        Set<String> visited = new HashSet<>();
        for (int i = 0; i < 10_000; i++) {
            IslandMath.WorldPos pos = IslandMath.indexToCenterPos(i);
            String key = pos.x() + "," + pos.z();
            assertFalse(visited.contains(key), "Collision detected at index " + i + ": " + key);
            visited.add(key);
        }
        assertEquals(10_000, visited.size());
    }

    @Test
    @DisplayName("Buildable island bounds checks at 128-block radius")
    void testIslandBounds() {
        int cx = 1024;
        int cz = 1024;

        // Inside bounds
        assertTrue(IslandMath.isWithinIslandBounds(cx, cz, cx, cz));
        assertTrue(IslandMath.isWithinIslandBounds(cx + 128, cz, cx, cz));
        assertTrue(IslandMath.isWithinIslandBounds(cx - 128, cz, cx, cz));
        assertTrue(IslandMath.isWithinIslandBounds(cx, cz + 128, cx, cz));
        assertTrue(IslandMath.isWithinIslandBounds(cx, cz - 128, cx, cz));
        assertTrue(IslandMath.isWithinIslandBounds(cx + 128, cz + 128, cx, cz));

        // Outside bounds
        assertFalse(IslandMath.isWithinIslandBounds(cx + 129, cz, cx, cz));
        assertFalse(IslandMath.isWithinIslandBounds(cx - 129, cz, cx, cz));
        assertFalse(IslandMath.isWithinIslandBounds(cx, cz + 129, cx, cz));
        assertFalse(IslandMath.isWithinIslandBounds(cx, cz - 129, cx, cz));
    }

    @Test
    @DisplayName("Buffer zone identification for 768-block void buffer")
    void testBufferZone() {
        // At center of island 0: not buffer
        assertFalse(IslandMath.isBufferZone(0, 0));
        assertFalse(IslandMath.isBufferZone(128, 128));

        // In 768m buffer between (0, 0) and (1024, 0)
        // Midpoint is 512
        assertTrue(IslandMath.isBufferZone(512, 0));
        assertTrue(IslandMath.isBufferZone(200, 0));
        assertTrue(IslandMath.isBufferZone(0, 200));

        // At center of island 1: not buffer
        assertFalse(IslandMath.isBufferZone(1024, 0));
        assertFalse(IslandMath.isBufferZone(1024 + 128, 0));
        assertTrue(IslandMath.isBufferZone(1024 + 129, 0));
    }
}
