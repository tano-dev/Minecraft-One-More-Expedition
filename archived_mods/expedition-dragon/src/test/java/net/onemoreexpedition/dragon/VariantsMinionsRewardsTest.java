package net.onemoreexpedition.dragon;

import net.minecraft.world.phys.Vec3;
import net.onemoreexpedition.dragon.config.DragonConfig;
import net.onemoreexpedition.dragon.core.FightState;
import net.onemoreexpedition.dragon.core.VariantBehavior;
import net.onemoreexpedition.dragon.minions.DragonMinionManager;
import net.onemoreexpedition.dragon.rewards.DragonRewardManager;
import net.onemoreexpedition.dragon.variants.DragonVariant;
import net.onemoreexpedition.dragon.variants.VariantBehaviors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class VariantsMinionsRewardsTest {

    @AfterEach
    void tearDown() {
        VariantBehavior.REGISTRY.clear();
        DragonMinionManager.get().clearLocalTracking();
    }

    @Test
    @DisplayName("M3: All 5 VariantBehaviors register and expose exact variant mechanics")
    void allFiveVariantBehaviorsWork() {
        VariantBehaviors.registerAll();

        // 1. Infernal
        VariantBehavior infernal = VariantBehavior.create(DragonVariant.INFERNAL);
        assertEquals(DragonVariant.INFERNAL, infernal.variant());
        assertEquals(7, infernal.modifyChainFireballCount(4));
        assertTrue(infernal.modifyMovesetCooldown(200.0) < 200.0);

        // 2. Voidgloom
        VariantBehavior voidgloom = VariantBehavior.create(DragonVariant.VOIDGLOOM);
        assertEquals(DragonVariant.VOIDGLOOM, voidgloom.variant());
        assertEquals(8, voidgloom.modifyChainFireballCount(4));
        Vec3 steered = VariantBehaviors.VoidgloomBehavior.computeHomingVelocity(
                new Vec3(1.0, 0.0, 0.0),
                new Vec3(0.0, 80.0, 0.0),
                new Vec3(0.0, 80.0, 20.0)
        );
        assertTrue(steered.z > 0.2D, "Homing velocity should curve toward target Z");
        assertEquals(1.10D, steered.length(), 1.0E-3D);

        // 3. Revenant
        VariantBehavior revenant = VariantBehavior.create(DragonVariant.REVENANT);
        assertEquals(DragonVariant.REVENANT, revenant.variant());
        assertEquals(9, revenant.getEndermanCallThresholds().length);
        assertEquals(5, revenant.getShadowPhantomsPerCrystal(new DragonConfig.MinionConfig()));

        // 4. Enlightened
        VariantBehavior enlightened = VariantBehavior.create(DragonVariant.ENLIGHTENED);
        assertEquals(DragonVariant.ENLIGHTENED, enlightened.variant());
        assertTrue(enlightened.hasCustomShieldAnchors());
        assertTrue(enlightened.suppressCrystalSpawns());

        // 5. Chronos
        VariantBehavior chronos = VariantBehavior.create(DragonVariant.CHRONOS);
        assertEquals(DragonVariant.CHRONOS, chronos.variant());
        Vec3 playerPos = new Vec3(10.0, 70.0, 10.0);
        Vec3 wardenPos = new Vec3(40.0, 70.0, 10.0);
        Vec3 offsetPos = VariantBehaviors.ChronosBehavior.computeSafeTeleportOffset(playerPos, wardenPos, 5.5D);
        double dist = offsetPos.distanceTo(playerPos);
        assertTrue(dist >= 5.0D && dist <= 6.0D, "Chronos Warden must teleport 5-6 blocks away, got " + dist);
    }

    @Test
    @DisplayName("M4: DragonMinionManager tracks Enderman thresholds, airborne ticks, enclosure, and knockback")
    void minionManagerMechanics() {
        VariantBehaviors.registerAll();
        DragonMinionManager mgr = DragonMinionManager.get();

        // Standard 75/50/25% thresholds
        FightState infernalFight = new FightState(UUID.randomUUID(), DragonVariant.INFERNAL, false, 1500.0);
        mgr.onHealthThreshold(infernalFight, null, null, 0.80, 0.74);
        assertTrue(mgr.getTriggeredThresholdPercents().contains(75));
        assertFalse(mgr.getTriggeredThresholdPercents().contains(50));

        // Revenant 10% thresholds
        mgr.clearLocalTracking();
        FightState revenantFight = new FightState(UUID.randomUUID(), DragonVariant.REVENANT, false, 1600.0);
        mgr.onHealthThreshold(revenantFight, null, null, 0.95, 0.78);
        assertTrue(mgr.getTriggeredThresholdPercents().contains(90));
        assertTrue(mgr.getTriggeredThresholdPercents().contains(80));

        // Airborne tracking (>60 ticks = 3s)
        UUID pid = UUID.randomUUID();
        for (int i = 0; i < 65; i++) {
            mgr.recordAirborneState(pid, true);
        }
        assertEquals(65, mgr.getAirborneTicks(pid));
        mgr.recordAirborneState(pid, false);
        assertEquals(0, mgr.getAirborneTicks(pid));

        // Enclosure check (roof + >=3 sides)
        assertTrue(DragonMinionManager.isEnclosureConditionMet(true, 3));
        assertTrue(DragonMinionManager.isEnclosureConditionMet(true, 4));
        assertFalse(DragonMinionManager.isEnclosureConditionMet(false, 4));
        assertFalse(DragonMinionManager.isEnclosureConditionMet(true, 2));

        // 3x Shadow Phantom knockback impulse
        Vec3 kb = DragonMinionManager.computeKnockbackImpulse(new Vec3(0, 70, 0), new Vec3(2, 70, 0), 3.0D);
        assertTrue(kb.x > 1.5D);
        assertTrue(kb.y > 0.5D);
    }

    @Test
    @DisplayName("M5: DragonRewardManager computes damage-weighted rolls, coins, and safe EconomyCraft fallback")
    void rewardManagerCalculationsAndOverflowFallback() {
        DragonConfig.RewardConfig cfg = new DragonConfig.RewardConfig();
        cfg.normalize();

        assertEquals(2, DragonRewardManager.computeTotalRolls(0.02, cfg.baseRolls, cfg.maxDamageBonusRolls));
        assertEquals(5, DragonRewardManager.computeTotalRolls(0.50, cfg.baseRolls, cfg.maxDamageBonusRolls));
        assertEquals(8, DragonRewardManager.computeTotalRolls(1.00, cfg.baseRolls, cfg.maxDamageBonusRolls));

        long coins = DragonRewardManager.computeCoinsReward(0.50, 2, cfg);
        // 500 base + (3 bonus rolls + 2 crystal breaks) * 250 = 1750
        assertEquals(1750L, coins);

        // Safe false return when EconomyCraft is absent in test runtime
        assertFalse(DragonRewardManager.routeOverflowToOrders(null, UUID.randomUUID(), null));
    }

    @Test
    @DisplayName("StellarityPillars defines all 10 custom Stellarity pillars and 4 iron-bar caged pillars")
    void stellarityPillarsMatchExactCoordinates() {
        assertEquals(10, net.onemoreexpedition.dragon.altar.StellarityPillars.PILLARS.size());
        long cagedCount = net.onemoreexpedition.dragon.altar.StellarityPillars.PILLARS.stream()
                .filter(net.onemoreexpedition.dragon.altar.StellarityPillars.Pillar::caged)
                .count();
        assertEquals(4L, cagedCount);
    }
}
