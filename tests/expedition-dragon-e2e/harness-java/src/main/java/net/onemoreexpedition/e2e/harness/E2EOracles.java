package net.onemoreexpedition.e2e.harness;

import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.phys.Vec3;

import java.util.Map;

/**
 * Reusable opaque-box assertion oracles for Tier 1..4 tests.
 */
public final class E2EOracles {
    private E2EOracles() {
    }

    public static void assertWithinRange(double actual, double min, double max, String label) {
        if (Double.isNaN(actual) || actual < min || actual > max) {
            throw new AssertionError(label + " expected in [" + min + ", " + max + "] but was " + actual);
        }
    }

    public static void assertWeightDistributionValid(Map<String, Double> weights) {
        if (weights == null || weights.size() != 5) {
            throw new AssertionError("Expected 5 variant weights, got: " + weights);
        }
        double sum = 0.0;
        for (Map.Entry<String, Double> e : weights.entrySet()) {
            Double val = e.getValue();
            if (val == null || Double.isNaN(val) || Double.isInfinite(val) || val < 0.0) {
                throw new AssertionError("Invalid weight for " + e.getKey() + ": " + val);
            }
            sum += val;
        }
        if (Double.isNaN(sum) || Math.abs(sum - 1.0) > 1e-6) {
            throw new AssertionError("Variant weights must sum to 1.0, actual sum=" + sum);
        }
    }

    public static void assertCrystalBroadcastMatches(String actualMessage, String playerName, int count) {
        String expected = E2EFixtures.expectedCrystalBroadcast(playerName, count);
        if (!expected.equals(actualMessage)) {
            throw new AssertionError("Expected crystal broadcast '" + expected + "' but got '" + actualMessage + "'");
        }
    }

    public static void assertDragonKnockbackImmune(EnderDragon dragon) {
        AttributeInstance attr = dragon.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
        double value = attr != null ? attr.getValue() : 0.0;
        if (Math.abs(value - 1.0) > 1e-6) {
            throw new AssertionError("Expected KNOCKBACK_RESISTANCE == 1.0, actual=" + value);
        }
    }

    /**
     * Validates Chronos Warden teleport placement using horizontal XZ distance (5-6 blocks
     * with 0.5-block tolerance) rather than 3D Euclidean distance so pillared players (DeltaY > 5)
     * do not fail when the Warden teleports 5-6 blocks horizontally onto solid ground.
     */
    public static void assertWardenTeleportValid(Vec3 playerPos, Vec3 wardenPos) {
        double horizDist = Math.hypot(wardenPos.x - playerPos.x, wardenPos.z - playerPos.z);
        if (Double.isNaN(horizDist) || horizDist < 4.0) {
            throw new AssertionError("Warden teleported too close / on top of player: horizDist=" + horizDist);
        }
        if (horizDist < E2EFixtures.CHRONOS_TELEPORT_MIN_DIST - 0.5 || horizDist > E2EFixtures.CHRONOS_TELEPORT_MAX_DIST + 0.5) {
            throw new AssertionError("Warden horizontal teleport distance expected in [4.5, 6.5], actual=" + horizDist);
        }
    }

    public static boolean qualifiesForBaseReward(double damageDealt, double totalDragonHp) {
        if (totalDragonHp <= 0.0) {
            return damageDealt >= E2EFixtures.REWARD_MIN_DAMAGE_FLAT;
        }
        return (damageDealt / totalDragonHp) >= E2EFixtures.REWARD_MIN_DAMAGE_PERCENT
                || damageDealt >= E2EFixtures.REWARD_MIN_DAMAGE_FLAT;
    }
}
