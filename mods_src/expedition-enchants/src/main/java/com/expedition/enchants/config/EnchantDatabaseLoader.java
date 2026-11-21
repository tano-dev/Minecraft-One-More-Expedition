package com.expedition.enchants.config;

import com.expedition.enchants.ExpeditionEnchantsMod;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Loads tier information from enchantments_config.json (the master enchant database).
 * This is the source of truth for enchant tiers (common/rare/epic/ultimate/curse).
 *
 * File path (relative to server root): enchantments_config.json
 */
public class EnchantDatabaseLoader {

    private static final Gson GSON = new Gson();
    private static final Path DB_PATH = Path.of("enchantments_config.json");
    private static final Path ALT_DB_PATH = Path.of("config", "enchantments_config.json");

    private static final AtomicReference<Map<String, String>> TIER_MAP =
        new AtomicReference<>(Collections.emptyMap());
    private static final AtomicReference<java.util.Set<String>> DISABLED_SET =
        new AtomicReference<>(Collections.emptySet());

    /**
     * Load (or reload) the enchant database. Call this during server start and on /enchantsreload.
     */
    public static void load() {
        Reader reader = null;
        try {
            if (Files.exists(DB_PATH)) {
                reader = Files.newBufferedReader(DB_PATH, StandardCharsets.UTF_8);
            } else if (Files.exists(ALT_DB_PATH)) {
                reader = Files.newBufferedReader(ALT_DB_PATH, StandardCharsets.UTF_8);
            } else {
                java.io.InputStream stream = EnchantDatabaseLoader.class.getResourceAsStream("/enchantments_config.json");
                if (stream != null) {
                    reader = new java.io.InputStreamReader(stream, StandardCharsets.UTF_8);
                    ExpeditionEnchantsMod.LOGGER.info("[ExpeditionEnchants] Loading enchantments_config.json from bundled JAR resource.");
                }
            }

            if (reader == null) {
                ExpeditionEnchantsMod.LOGGER.warn("[ExpeditionEnchants] enchantments_config.json not found — tier detection will fall back to description color.");
                TIER_MAP.set(Collections.emptyMap());
                DISABLED_SET.set(Collections.emptySet());
                return;
            }

            try (Reader r = reader) {
                JsonObject root = GSON.fromJson(r, JsonObject.class);
                JsonObject enchantments = root.getAsJsonObject("enchantments");
                if (enchantments == null) {
                    ExpeditionEnchantsMod.LOGGER.warn("[ExpeditionEnchants] enchantments_config.json has no 'enchantments' key.");
                    TIER_MAP.set(Collections.emptyMap());
                    DISABLED_SET.set(Collections.emptySet());
                    return;
                }

                Map<String, String> map = new HashMap<>(enchantments.size());
                java.util.Set<String> disabled = new java.util.HashSet<>();

                for (Map.Entry<String, JsonElement> entry : enchantments.entrySet()) {
                    String id = entry.getKey();
                    JsonObject obj = entry.getValue().getAsJsonObject();

                    JsonElement enabledEl = obj.get("enabled");
                    boolean isEnabled = enabledEl == null || !enabledEl.isJsonPrimitive() || enabledEl.getAsBoolean();
                    if (!isEnabled) {
                        disabled.add(id);
                    }

                    JsonElement tierEl = obj.get("tier");
                    if (tierEl == null || !tierEl.isJsonPrimitive()) continue;
                    String tier = tierEl.getAsString().toLowerCase(java.util.Locale.ROOT);
                    if (tier.equals("disabled_duplicate")) {
                        disabled.add(id);
                        continue;
                    }

                    JsonElement curseEl = obj.get("curse");
                    if (curseEl != null && curseEl.isJsonPrimitive() && curseEl.getAsBoolean()) {
                        tier = "curse";
                    }

                    map.put(id, tier);
                }
                TIER_MAP.set(Collections.unmodifiableMap(map));
                DISABLED_SET.set(Collections.unmodifiableSet(disabled));
                ExpeditionEnchantsMod.LOGGER.info("[ExpeditionEnchants] Loaded {} enchant tiers ({} disabled) from enchantments_config.json", map.size(), disabled.size());
            }
        } catch (Exception e) {
            ExpeditionEnchantsMod.LOGGER.error("[ExpeditionEnchants] Failed to load enchantments_config.json: {}", e.getMessage(), e);
            TIER_MAP.set(Collections.emptyMap());
            DISABLED_SET.set(Collections.emptySet());
        }
    }

    /**
     * Look up the tier for an enchantment ID (e.g. "minecraft:sharpness").
     * Returns null if not found in database.
     */
    public static String getTier(String enchantmentId) {
        return TIER_MAP.get().get(enchantmentId);
    }

    /**
     * Check if an enchantment ID is enabled in the master database.
     */
    public static boolean isEnabled(String enchantmentId) {
        return !DISABLED_SET.get().contains(enchantmentId);
    }

    /**
     * @return set of all disabled enchantment IDs
     */
    public static java.util.Set<String> getDisabledIds() {
        return DISABLED_SET.get();
    }

    /**
     * @return true if database was loaded with at least one entry.
     */
    public static boolean isLoaded() {
        return !TIER_MAP.get().isEmpty();
    }
}
