package com.expedition.quests.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

public class QuestsConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static final Path CONFIG_DIR = FabricLoader.getInstance().getConfigDir().resolve("expedition-quests");
    private static final Path CONFIG_FILE = CONFIG_DIR.resolve("quests_config.json");

    private static JsonObject ROOT = new JsonObject();

    public static synchronized void load() {
        try {
            Files.createDirectories(CONFIG_DIR);
            if (!Files.exists(CONFIG_FILE)) {
                try (InputStream in = QuestsConfig.class.getResourceAsStream("/data/expedition_quests/quests_config.json")) {
                    if (in != null) {
                        byte[] bytes = in.readAllBytes();
                        Files.write(CONFIG_FILE, bytes);
                        ROOT = JsonParser.parseString(new String(bytes, StandardCharsets.UTF_8)).getAsJsonObject();
                        return;
                    }
                }
            } else {
                try (Reader r = Files.newBufferedReader(CONFIG_FILE, StandardCharsets.UTF_8)) {
                    ROOT = JsonParser.parseReader(r).getAsJsonObject();
                    return;
                }
            }
        } catch (Exception e) {
            try (InputStream in = QuestsConfig.class.getResourceAsStream("/data/expedition_quests/quests_config.json")) {
                if (in != null) {
                    ROOT = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
                }
            } catch (Exception ignored) {}
        }
    }

    public static JsonObject section(String name) {
        if (ROOT != null && ROOT.has(name) && ROOT.get(name).isJsonObject()) {
            return ROOT.getAsJsonObject(name);
        }
        return new JsonObject();
    }

    public static int getMaxLevel() {
        return getInt(section("progression"), "max_level", 50);
    }

    public static int getNetherUnlockLevel() {
        return getInt(section("progression"), "nether_unlock_level", 15);
    }

    public static int getEndUnlockLevel() {
        return getInt(section("progression"), "end_unlock_level", 30);
    }

    public static boolean requireEndUnlockForElytra() {
        return getBool(section("progression"), "require_end_unlock_for_elytra_quests", true);
    }

    public static double getCoinBonusPerLevel() {
        return getDouble(section("progression"), "coin_bonus_per_level", 0.01);
    }

    public static double getDimensionRewardMultiplier(String dimension) {
        if ("minecraft:the_nether".equals(dimension)) {
            return getDouble(section("progression"), "nether_quest_reward_multiplier", 1.20);
        }
        if ("minecraft:the_end".equals(dimension)) {
            return getDouble(section("progression"), "end_quest_reward_multiplier", 1.40);
        }
        return 1.0;
    }

    public static int getBaseDraftChoices() {
        return getInt(section("daily_slots"), "draft_choices_per_slot", 4);
    }

    public static int getBaseFreeRerolls() {
        return getInt(section("daily_slots"), "base_free_rerolls_per_day", 1);
    }

    public static String getOriginMode() {
        return getStr(section("exploration_distances"), "origin_mode", "PLAYER_POS");
    }

    public static int getVisitedGridCellSize() {
        return Math.max(16, getInt(section("exploration_distances"), "visited_grid_cell_size_blocks", 64));
    }

    public static int getMinIsolationBlocks() {
        return Math.max(64, getInt(section("exploration_distances"), "min_isolation_between_claimed_targets_blocks", 256));
    }

    public static int getAutoExpandStepBlocks() {
        return Math.max(200, getInt(section("exploration_distances"), "auto_expand_if_explored_step_blocks", 600));
    }

    public static int getMaxAutoExpandAttempts() {
        return Math.max(1, getInt(section("exploration_distances"), "max_auto_expand_attempts", 5));
    }

    public static double getDistanceRewardBonusPer1000m() {
        return getDouble(section("exploration_distances"), "distance_reward_bonus_per_1000_blocks", 0.10);
    }

    public static double getDimensionDistanceMultiplier(String dimension) {
        JsonObject exp = section("exploration_distances");
        if (exp.has("dimension_distance_multipliers") && exp.get("dimension_distance_multipliers").isJsonObject()) {
            JsonObject dm = exp.getAsJsonObject("dimension_distance_multipliers");
            return getDouble(dm, dimension, 1.0);
        }
        return 1.0;
    }

    public static JsonObject getExploreSchoolCfg(String category) {
        JsonObject exp = section("exploration_distances");
        String key = switch (category) {
            case "EXPLORE_BIOME" -> "biome";
            case "EXPLORE_DUNGEON" -> "dungeon";
            default -> "structure";
        };
        if (exp.has(key) && exp.get(key).isJsonObject()) {
            return exp.getAsJsonObject(key);
        }
        return new JsonObject();
    }

    public static int getExplorationMinBlocks(String category, String difficulty, String dimension) {
        JsonObject sc = getExploreSchoolCfg(category);
        String diffKey = difficulty.toLowerCase(Locale.ROOT) + "_min_blocks";
        int base = getInt(sc, diffKey, 800);
        return (int) Math.max(250, Math.round(base * getDimensionDistanceMultiplier(dimension)));
    }

    public static int getExplorationMaxBlocks(String category, String difficulty, String dimension) {
        JsonObject sc = getExploreSchoolCfg(category);
        String diffKey = difficulty.toLowerCase(Locale.ROOT) + "_max_blocks";
        int base = getInt(sc, diffKey, 2000);
        return (int) Math.max(500, Math.round(base * getDimensionDistanceMultiplier(dimension)));
    }

    public static int getExplorationCaptureRadius(String category) {
        JsonObject sc = getExploreSchoolCfg(category);
        return getInt(sc, "capture_radius_blocks", 48);
    }

    public static double getExplorationSchoolRewardMultiplier(String category) {
        JsonObject sc = getExploreSchoolCfg(category);
        double def = switch (category) {
            case "EXPLORE_BIOME" -> 1.35;
            case "EXPLORE_DUNGEON" -> 1.75;
            default -> 1.50;
        };
        return getDouble(sc, "reward_multiplier", def);
    }

    public static int getTravelDistance(String subType, String difficulty) {
        JsonObject tr = section("travel_action_distances");
        String key = switch (subType) {
            case "BOAT" -> "boat";
            case "LAVA_STRIDER" -> "lava_strider";
            case "ELYTRA" -> "elytra_flight";
            default -> "land_mount";
        };
        int def = switch (difficulty) {
            case "EASY" -> 500;
            case "MEDIUM" -> 1200;
            default -> 2500;
        };
        if (tr.has(key) && tr.get(key).isJsonObject()) {
            return getInt(tr.getAsJsonObject(key), difficulty.toLowerCase(Locale.ROOT) + "_blocks", def);
        }
        return def;
    }

    public static int getWaypointCheckpointBlocks() {
        return Math.max(6, getInt(section("travel_action_distances"), "waypoint_checkpoint_interval_blocks", 12));
    }

    public static double getEconomyDailyReward() {
        try {
            Path econPath = FabricLoader.getInstance().getConfigDir().resolve("economycraft").resolve("config.json");
            if (Files.exists(econPath)) {
                try (Reader reader = Files.newBufferedReader(econPath, StandardCharsets.UTF_8)) {
                    JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                    if (root.has("economy") && root.get("economy").isJsonObject()) {
                        return Math.max(1.0, getDouble(root.getAsJsonObject("economy"), "daily_reward", 100.0));
                    }
                }
            }
        } catch (Exception ignored) {}
        return 100.0;
    }

    public static long getBaseTierCoins(String difficulty) {
        JsonObject rw = section("rewards_daily_multiplier");
        double daily = getBool(rw, "scale_with_economycraft_daily", true) ? getEconomyDailyReward() : 100.0;
        double mult = switch (difficulty) {
            case "EASY" -> getDouble(rw, "easy_daily_mult", 0.15);
            case "MEDIUM" -> getDouble(rw, "medium_daily_mult", 0.35);
            default -> getDouble(rw, "hard_daily_mult", 0.75);
        };
        return Math.max(1L, Math.round(daily * mult));
    }

    public static long getAll10BonusCoins() {
        JsonObject rw = section("rewards_daily_multiplier");
        double daily = getBool(rw, "scale_with_economycraft_daily", true) ? getEconomyDailyReward() : 100.0;
        double mult = getDouble(rw, "all_10_completed_bonus_daily_mult", 1.20);
        return Math.max(5L, Math.round(daily * mult));
    }

    public static int getBaseTierXp(String difficulty) {
        JsonObject rw = section("rewards_daily_multiplier");
        return switch (difficulty) {
            case "EASY" -> getInt(rw, "easy_adventure_xp", 50);
            case "MEDIUM" -> getInt(rw, "medium_adventure_xp", 130);
            default -> getInt(rw, "hard_adventure_xp", 320);
        };
    }

    public static int getAll10BonusXp() {
        return getInt(section("rewards_daily_multiplier"), "all_10_completed_bonus_xp", 300);
    }

    public static double getEnchantVal(String key, double def) {
        return getDouble(section("enchantments"), key, def);
    }

    public static String getStr(JsonObject obj, String key, String def) {
        if (obj == null || !obj.has(key) || obj.get(key).isJsonNull()) return def;
        try { return obj.get(key).getAsString(); } catch (Exception e) { return def; }
    }

    public static double getDouble(JsonObject obj, String key, double def) {
        if (obj == null || !obj.has(key) || obj.get(key).isJsonNull()) return def;
        try { return obj.get(key).getAsDouble(); } catch (Exception e) { return def; }
    }

    public static int getInt(JsonObject obj, String key, int def) {
        if (obj == null || !obj.has(key) || obj.get(key).isJsonNull()) return def;
        try { return obj.get(key).getAsInt(); } catch (Exception e) { return def; }
    }

    public static boolean getBool(JsonObject obj, String key, boolean def) {
        if (obj == null || !obj.has(key) || obj.get(key).isJsonNull()) return def;
        try { return obj.get(key).getAsBoolean(); } catch (Exception e) { return def; }
    }
}
