package net.onemoreexpedition.dragon;

import net.onemoreexpedition.dragon.core.FightState;
import net.onemoreexpedition.dragon.variants.DragonVariant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class FightStateTest {

    @Test
    @DisplayName("Damage and crystal breaks accumulate per player and ignore invalid inputs")
    void damageAndCrystalTrackingAccumulates() {
        FightState state = new FightState(UUID.randomUUID(), DragonVariant.REVENANT, true, 1600.0);
        UUID p1 = UUID.randomUUID();
        UUID p2 = UUID.randomUUID();

        state.recordDamage(p1, 100.0);
        state.recordDamage(p1, 50.5);
        state.recordDamage(p2, 25.0);
        state.recordDamage(p2, -10.0);
        state.recordDamage(p2, Double.NaN);
        state.recordDamage(null, 50.0);

        state.recordCrystalOrGuardianBreak(p1);
        state.recordCrystalOrGuardianBreak(p1);
        state.recordCrystalOrGuardianBreak(null);

        assertEquals(150.5, state.getDamage(p1), 1e-6);
        assertEquals(25.0, state.getDamage(p2), 1e-6);
        assertEquals(175.5, state.getTotalDamage(), 1e-6);
        assertEquals(2, state.getCrystalBreaks(p1));
        assertEquals(0, state.getCrystalBreaks(p2));
    }

    @Test
    @DisplayName("Qualification rule requires >=1% total damage OR >=20.0 flat damage")
    void qualificationRuleRequiresPercentOrFlatThreshold() {
        FightState state = new FightState(UUID.randomUUID(), DragonVariant.INFERNAL, false, 1500.0);
        UUID topDamager = UUID.randomUUID();
        UUID flatQualifier = UUID.randomUUID();
        UUID pctQualifier = UUID.randomUUID();
        UUID underThreshold = UUID.randomUUID();
        UUID zeroDamager = UUID.randomUUID();

        // Total damage will be 5000 + 25 + 15 + 5 = 5045
        // 25 >= 20 flat -> qualified even though 25/5045 < 1%
        // Let's test pctQualifier on smaller total first or separate fight
        state.recordDamage(topDamager, 5000.0);
        state.recordDamage(flatQualifier, 25.0);
        state.recordDamage(underThreshold, 5.0);

        assertTrue(state.isPlayerQualified(topDamager, 0.01, 20.0));
        assertTrue(state.isPlayerQualified(flatQualifier, 0.01, 20.0));
        assertFalse(state.isPlayerQualified(underThreshold, 0.01, 20.0));
        assertFalse(state.isPlayerQualified(zeroDamager, 0.01, 20.0));
        assertFalse(state.isPlayerQualified(null, 0.01, 20.0));

        // Percent-only qualification: 10.0 out of 1000.0 = 1.0% (>= 0.01), even though < 20.0 flat
        FightState smallFight = new FightState(UUID.randomUUID(), DragonVariant.VOIDGLOOM, false, 1400.0);
        smallFight.recordDamage(topDamager, 990.0);
        smallFight.recordDamage(pctQualifier, 10.0);
        assertTrue(smallFight.isPlayerQualified(pctQualifier, 0.01, 20.0));

        // IEEE 754 exact 1% boundary: 0.29 / 29.0 yields 0.009999999999999998 in float division,
        // but dmg >= total * minDamagePercent (0.29 >= 0.29) must qualify accurately.
        FightState ieeeFight = new FightState(UUID.randomUUID(), DragonVariant.CHRONOS, false, 2000.0);
        UUID exactOnePct = UUID.randomUUID();
        UUID justUnderOnePct = UUID.randomUUID();
        ieeeFight.recordDamage(topDamager, 28.71);
        ieeeFight.recordDamage(exactOnePct, 0.29);
        assertTrue(ieeeFight.isPlayerQualified(exactOnePct, 0.01, 20.0));

        FightState underFight = new FightState(UUID.randomUUID(), DragonVariant.CHRONOS, false, 2000.0);
        underFight.recordDamage(topDamager, 28.71);
        underFight.recordDamage(justUnderOnePct, 0.2899);
        assertFalse(underFight.isPlayerQualified(justUnderOnePct, 0.01, 20.0));
    }

    @Test
    @DisplayName("Post-end state becomes immutable for damage, anchors, and minions")
    void postEndStateRejectsMutations() {
        FightState state = new FightState(UUID.randomUUID(), DragonVariant.ENLIGHTENED, true, 1800.0);
        UUID player = UUID.randomUUID();
        UUID anchor = UUID.randomUUID();
        UUID minion = UUID.randomUUID();

        state.recordDamage(player, 100.0);
        state.addAnchor(anchor);
        state.trackMinion(minion);

        assertTrue(state.hasActiveAnchors());
        assertEquals(1, state.getActiveAnchorCount());
        assertTrue(state.isTrackedMinion(minion));

        state.markEnded();
        assertTrue(state.isEnded());

        state.recordDamage(player, 200.0);
        state.recordCrystalOrGuardianBreak(player);
        state.addAnchor(UUID.randomUUID());
        state.trackMinion(UUID.randomUUID());

        assertEquals(100.0, state.getDamage(player), 1e-6);
        assertEquals(0, state.getCrystalBreaks(player));
        assertEquals(1, state.getActiveAnchorCount());
        assertEquals(1, state.getTrackedMinionCount());

        state.removeAnchor(anchor);
        state.untrackMinion(minion);
        assertFalse(state.hasActiveAnchors());
        assertEquals(0, state.getTrackedMinionCount());
    }
}
