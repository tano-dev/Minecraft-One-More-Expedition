package com.expedition.enchants.config;

import com.expedition.enchants.ExpeditionEnchantsMod;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class EnchantRollConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = Path.of("config", "expedition-enchants.json");
    private static EnchantRollConfig INSTANCE = new EnchantRollConfig();

    public boolean enabled = true;
    public boolean enableRevealUi = true;

    public static class TooltipConfig {
        public boolean enabled = true;
        public boolean useTargetIcons = true;
        public boolean boldMaxIcon = true;
        public boolean targetIconsOnBooksOnly = true;
        public String maxLevelSymbol = "✦";
        public boolean symbolBeforeName = true;
        public boolean sortMaxFirst = true;
        public boolean singleLinePerEnchantOnBooks = true;
        public int maxEnchantsPerLine = 3;
        public int maxLineLength = 38;
        public boolean showCurseAtBottom = true;
        public boolean enableServerLoreSync = true;
        public Map<String, String> targetIcons = new LinkedHashMap<>(Map.ofEntries(
            Map.entry("all", "✦"),
            Map.entry("armor", "🛡"),
            Map.entry("helmet", "🪖"),
            Map.entry("chestplate", "🦺"),
            Map.entry("leggings", "👖"),
            Map.entry("boots", "🥾"),
            Map.entry("sword", "⚔"),
            Map.entry("axe", "🪓"),
            Map.entry("spear", "🗡"),
            Map.entry("melee", "⚔🪓"),
            Map.entry("tools", "⛏🪓"),
            Map.entry("mining", "⛏"),
            Map.entry("ranged", "🏹🎯"),
            Map.entry("bow", "🏹"),
            Map.entry("crossbow", "🎯"),
            Map.entry("trident", "🔱"),
            Map.entry("mace", "🔨"),
            Map.entry("fishing_rod", "🎣"),
            Map.entry("elytra", "🪽"),
            Map.entry("curse", "☠")
        ));
        public Map<String, String> tierColors = new LinkedHashMap<>(Map.of(
            "ultimate", "gold",
            "epic", "light_purple",
            "rare", "aqua",
            "common", "gray",
            "curse", "red"
        ));
    }

    public TooltipConfig tooltip = new TooltipConfig();

    // Chance per enchanting roll at Lv 30 (scaled linearly for lower table levels)
    public Map<String, Double> tierChances = new LinkedHashMap<>(Map.of(
        "ultimate", 0.00,
        "epic", 0.005,
        "rare", 0.05
    ));

    // Godroll settings (at Lv 30, disabled below Lv 20)
    public double godrollChance = 0.008;
    public double godrollEpicChance = 0.15;
    public double godrollRareChance = 0.35;
    public int godrollMinEnchants = 6;
    public int godrollMaxEnchants = 6;

    // Target weights for how many enchantments are rolled on an item at Lv 30 (2-5 enchants)
    public Map<String, Integer> enchantCountWeights = new LinkedHashMap<>(Map.of(
        "2", 20,
        "3", 50,
        "4", 22,
        "5", 8
    ));

    // Level distribution weights keyed by max_level (at Lv 30)
    public Map<String, Map<String, Integer>> levelCurves = defaultLevelCurves();

    // Optional manual overrides: enchantment_id -> "common" | "rare" | "epic" | "ultimate" | "curse"
    public Map<String, String> tierOverrides = new LinkedHashMap<>();

    // Structure Chest Enchanted Gear -> Enchanted Book replacement
    public boolean enableStructureChestBookReplacement = true;
    public double chestBookGodrollChance = 0.008;
    public Map<String, Integer> chestBookCountWeights = new LinkedHashMap<>(Map.of(
        "1", 30,
        "2", 25,
        "3", 20,
        "4", 15,
        "5", 10
    ));
    public double chestBookCommonChance = 0.60;
    public double chestBookRareChance = 0.25;
    public double chestBookEpicChance = 0.05;
    // Curse slot chance per enchant (Common = remainder). Godroll books use the godroll value instead.
    public double chestBookCurseChance = 0.10;
    public double chestBookGodrollCurseChance = 0.0;
    // Only curses from these namespaces may appear on chest books
    public List<String> chestBookCurseNamespaces = new ArrayList<>(List.of("minecraft", "enchantencore"));
    public double chestBookGodrollRareMultiplier = 3.0;
    public double chestBookGodrollEpicMultiplier = 3.0;
    public List<String> chestBookExcludedNamespaces = new ArrayList<>(List.of(
        "mt", "wither", "stellarity", "player_motion", "kattersstructures", "incendium", "bound"
    ));
    public List<String> chestBookWhitelistedEnchants = new ArrayList<>(List.of(
        "stellarity:ambush",
        "stellarity:plated",
        "stellarity:levitation_shot"
    ));

    private static Map<String, Map<String, Integer>> defaultLevelCurves() {
        Map<String, Map<String, Integer>> m = new LinkedHashMap<>();
        m.put("1", new LinkedHashMap<>(Map.of("1", 100)));
        m.put("2", new LinkedHashMap<>(Map.of("1", 70, "2", 30)));
        m.put("3", new LinkedHashMap<>(Map.of("1", 50, "2", 35, "3", 15)));
        m.put("4", new LinkedHashMap<>(Map.of("2", 50, "3", 35, "4", 15)));
        m.put("5", new LinkedHashMap<>(Map.of("2", 35, "3", 35, "4", 20, "5", 10)));
        return m;
    }

    public static EnchantRollConfig get() {
        return INSTANCE;
    }

    public static void load() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            if (Files.exists(CONFIG_PATH)) {
                try (Reader r = Files.newBufferedReader(CONFIG_PATH, StandardCharsets.UTF_8)) {
                    EnchantRollConfig loaded = GSON.fromJson(r, EnchantRollConfig.class);
                    if (loaded != null) {
                        INSTANCE = loaded;
                    }
                }
            } else {
                save();
            }
        } catch (Exception e) {
            ExpeditionEnchantsMod.LOGGER.error("[ExpeditionEnchants] Failed to load config, using defaults.", e);
        }
    }

    public static void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            try (Writer w = Files.newBufferedWriter(CONFIG_PATH, StandardCharsets.UTF_8)) {
                GSON.toJson(INSTANCE, w);
            }
        } catch (Exception e) {
            ExpeditionEnchantsMod.LOGGER.error("[ExpeditionEnchants] Failed to save config.", e);
        }
    }
}
