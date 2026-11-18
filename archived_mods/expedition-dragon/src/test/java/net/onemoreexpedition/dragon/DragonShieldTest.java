package net.onemoreexpedition.dragon;

import net.onemoreexpedition.dragon.combat.DragonShield;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class DragonShieldTest {

    @Test
    @DisplayName("isShieldActive blocks normal damage only when fight active, dragon matches, anchors > 0, and alive")
    void isShieldActiveTruthTable() {
        assertTrue(DragonShield.isShieldActive(true, true, true, false, true));
        assertFalse(DragonShield.isShieldActive(true, true, true, true, true), "BYPASSES_INVULNERABILITY must bypass shield");
        assertFalse(DragonShield.isShieldActive(true, true, false, false, true), "0 anchors must disable shield");
        assertFalse(DragonShield.isShieldActive(true, false, true, false, true), "Non-fight dragon must not be shielded");
        assertFalse(DragonShield.isShieldActive(false, true, true, false, true), "Ended/inactive fight must not shield");
        assertFalse(DragonShield.isShieldActive(true, true, true, false, false), "Dead dragon must not shield");
    }

    @Test
    @DisplayName("ActionBar cooldown throttles per player UUID cleanly")
    void actionBarCooldownThrottlesPerPlayer() {
        Map<UUID, Long> map = new HashMap<>();
        UUID p1 = UUID.randomUUID();
        UUID p2 = UUID.randomUUID();

        assertTrue(DragonShield.tryConsumeActionBarCooldown(map, p1, 1000L, 1000L));
        assertFalse(DragonShield.tryConsumeActionBarCooldown(map, p1, 1500L, 1000L));
        assertTrue(DragonShield.tryConsumeActionBarCooldown(map, p2, 1500L, 1000L));
        assertTrue(DragonShield.tryConsumeActionBarCooldown(map, p1, 2000L, 1000L));
        assertFalse(DragonShield.tryConsumeActionBarCooldown(map, null, 3000L, 1000L));
        assertFalse(DragonShield.tryConsumeActionBarCooldown(null, p1, 3000L, 1000L));

        // Signed long overflow guard when lastMs < 0 and nowMs = Long.MAX_VALUE
        UUID p3 = UUID.randomUUID();
        assertTrue(DragonShield.tryConsumeActionBarCooldown(map, p3, -10L, 1000L));
        assertTrue(DragonShield.tryConsumeActionBarCooldown(map, p3, Long.MAX_VALUE, 1000L));

        // Clock step backward guard
        assertTrue(DragonShield.tryConsumeActionBarCooldown(map, p3, 500L, 1000L));
    }

    @Test
    @DisplayName("computePostReductionDamage clamps overkill and rejects negative/NaN values")
    void computePostReductionDamageClampsProperly() {
        assertEquals(25.0F, DragonShield.computePostReductionDamage(100.0F, 75.0F), 1e-6F);
        assertEquals(12.0F, DragonShield.computePostReductionDamage(12.0F, 0.0F), 1e-6F);
        assertEquals(12.5F, DragonShield.computePostReductionDamage(12.5F, -50.0F), 1e-6F);
        assertEquals(0.0F, DragonShield.computePostReductionDamage(-5.0F, -10.0F), 1e-6F);
        assertEquals(0.0F, DragonShield.computePostReductionDamage(50.0F, 60.0F), 1e-6F);
        assertEquals(0.0F, DragonShield.computePostReductionDamage(Float.NaN, 10.0F), 1e-6F);
        assertEquals(0.0F, DragonShield.computePostReductionDamage(10.0F, Float.POSITIVE_INFINITY), 1e-6F);
        assertEquals("§cDestroy all End Crystals first!", DragonShield.SHIELD_ACTIONBAR_MESSAGE);
    }
}
