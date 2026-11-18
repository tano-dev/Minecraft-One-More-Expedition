package net.onemoreexpedition.dragon;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.onemoreexpedition.dragon.altar.AltarManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AltarManagerTest {

    @Test
    @DisplayName("isAltarPosition accepts Stellarity pedestals and vanilla podium faces within radius 6")
    void isAltarPositionCoversStellarityAndVanillaCoordinates() {
        // Stellarity pedestals: (4.5,62,0.5), (-3.5,62,0.5), (0.5,62,-3.5), (0.5,62,4.5)
        assertTrue(AltarManager.isAltarPosition(new BlockPos(4, 62, 0)));
        assertTrue(AltarManager.isAltarPosition(new BlockPos(-4, 62, 0)));
        assertTrue(AltarManager.isAltarPosition(new BlockPos(0, 62, -4)));
        assertTrue(AltarManager.isAltarPosition(new BlockPos(0, 62, 4)));

        // Vanilla exit portal faces: distance 3
        assertTrue(AltarManager.isAltarPosition(new BlockPos(3, 64, 0)));
        assertTrue(AltarManager.isAltarPosition(new BlockPos(-3, 64, 0)));
        assertTrue(AltarManager.isAltarPosition(new BlockPos(0, 64, 3)));
        assertTrue(AltarManager.isAltarPosition(new BlockPos(0, 64, -3)));

        // Exact radius 6 boundary
        assertTrue(AltarManager.isAltarPosition(new BlockPos(6, 62, 0)));
        assertFalse(AltarManager.isAltarPosition(new BlockPos(6, 62, 1)));
        assertFalse(AltarManager.isAltarPosition(new BlockPos(7, 62, 0)));
        assertFalse(AltarManager.isAltarPosition(new BlockPos(0, 44, 0)));
        assertFalse(AltarManager.isAltarPosition(new BlockPos(0, 111, 0)));
        assertFalse(AltarManager.isAltarPosition(null));
    }

    @Test
    @DisplayName("formatCrystalPlacedMessage produces exact 1/4..4/4 broadcast strings")
    void formatCrystalPlacedMessageFormatsExactly() {
        assertEquals("§5☬ §eSteve §dplaced an End Crystal! §7(1/4)",
                AltarManager.formatCrystalPlacedMessage("Steve", 1));
        assertEquals("§5☬ §eAlex §dplaced an End Crystal! §7(2/4)",
                AltarManager.formatCrystalPlacedMessage("Alex", 2));
        assertEquals("§5☬ §eHerobrine §dplaced an End Crystal! §7(3/4)",
                AltarManager.formatCrystalPlacedMessage("Herobrine", 3));
        assertEquals("§5☬ §eNotch §dplaced an End Crystal! §7(4/4)",
                AltarManager.formatCrystalPlacedMessage("Notch", 4));
        assertEquals("§5☬ §eUnknown §dplaced an End Crystal! §7(1/4)",
                AltarManager.formatCrystalPlacedMessage(" ", 0));
        assertEquals("§5☬ §ePlayer §dplaced an End Crystal! §7(4/4)",
                AltarManager.formatCrystalPlacedMessage("Player", 9));
    }

    @Test
    @DisplayName("ALTAR_SCAN_AABB intersects crystals at max valid altar Y=110 and min Y=45")
    void altarScanAabbCoversBoundaryYLevels() {
        assertTrue(AltarManager.ALTAR_SCAN_AABB.intersects(new AABB(new BlockPos(0, AltarManager.MAX_ALTAR_Y, 0))));
        assertTrue(AltarManager.ALTAR_SCAN_AABB.intersects(new AABB(new BlockPos(0, AltarManager.MIN_ALTAR_Y, 0))));
        assertTrue(AltarManager.ALTAR_SCAN_AABB.intersects(new AABB(new BlockPos(4, 62, 0))));
    }
}
