package net.onemoreexpedition.e2e.harness;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Map;

/**
 * Shared opaque-box constants and test fixtures derived strictly from
 * ORIGINAL_REQUEST.md and PROJECT.md specifications.
 */
public final class E2EFixtures {
    public static final List<String> VARIANTS = List.of(
            "Infernal", "Voidgloom", "Revenant", "Enlightened", "Chronos"
    );

    public static final Map<String, String> VARIANT_COLORS = Map.of(
            "Infernal", "RED",
            "Voidgloom", "PURPLE",
            "Revenant", "GREEN",
            "Enlightened", "YELLOW",
            "Chronos", "BLUE"
    );

    public static final double DEFAULT_WEIGHT = 0.20;
    public static final double BASE_HP_MIN = 1000.0;
    public static final double BASE_HP_MAX = 2000.0;

    public static final BlockPos EXIT_PORTAL_CENTER = new BlockPos(0, 64, 0);
    public static final Vec3 ISLAND_TRACKING_ORIGIN = new Vec3(0.0, 80.0, 0.0);

    public static final double ALTAR_CRYSTAL_RADIUS = 6.0;
    public static final double ENDERMAN_AGGRO_RADIUS = 150.0;
    public static final double NO_ELYTRA_PULL_RADIUS = 250.0;
    public static final double NO_ELYTRA_ZONE_RADIUS = 180.0;

    public static final double CHRONOS_TELEPORT_DIST_THRESHOLD = 20.0;
    public static final double CHRONOS_TELEPORT_DY_THRESHOLD = 5.0;
    public static final double CHRONOS_TELEPORT_MIN_DIST = 5.0;
    public static final double CHRONOS_TELEPORT_MAX_DIST = 6.0;

    public static final double REWARD_MIN_DAMAGE_PERCENT = 0.01;
    public static final double REWARD_MIN_DAMAGE_FLAT = 20.0;

    public static final String MSG_CRYSTAL_BROADCAST_PREFIX = "§5☬ §e";
    public static final String MSG_SHIELD_ACTIONBAR = "Destroy all End Crystals first!";
    public static final String MSG_SHADOW_PHANTOM_ACTIONBAR = "§cThis Shadow Phantom cannot be killed!";
    public static final String MSG_SPAWN_PULL_CHAT = "§eThe Dragon has awakened! You were pulled safely back to the main island.";
    public static final String MSG_NO_ELYTRA_ACTIONBAR = "§cElytra gliding is disabled during the Dragon fight!";
    public static final String MSG_ORDERS_OVERFLOW_CHAT = "§eInventory full! Overflow items were sent to /orders.";

    private E2EFixtures() {
    }

    public static String expectedCrystalBroadcast(String playerName, int count) {
        return "§5☬ §e" + playerName + " §dplaced an End Crystal! §7(" + count + "/4)";
    }

    public static List<BlockPos> altarCrystalRimOffsets() {
        return List.of(
                new BlockPos(3, 64, 0),
                new BlockPos(-3, 64, 0),
                new BlockPos(0, 64, 3),
                new BlockPos(0, 64, -3)
        );
    }
}
