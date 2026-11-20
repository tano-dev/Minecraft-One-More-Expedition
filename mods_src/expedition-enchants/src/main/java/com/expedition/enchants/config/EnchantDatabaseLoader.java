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
    // Relative to server working directory (where the server jar runs from)
    private static final Path DB_PATH = Path.of("enchantments_config.json");

    private static final AtomicReference<Map<String, String>> TIER_MAP =
        new AtomicReference<>(Collections.emptyMap());

    /**
     * Load (or reload) the enchant database. Call this during server start and on /enchantsreload.
     */
    public static void load() {
        if (!Files.exists(DB_PATH)) {
            ExpeditionEnchantsMod.LOGGER.warn("[ExpeditionEnchants] enchantments_config.json not found at '{}' — tier detection will fall back to description color.", DB_PATH.toAbsolutePath());
            TIER_MAP.set(Collections.emptyMap());
            return;
        }
        try (Reader reader = Files.newBufferedReader(DB_PATH, StandardCharsets.UTF_8)) {
            JsonObject root = GSON.fromJson(reader, JsonObject.class);
            JsonObject enchantments = root.getAsJsonObject("enchantments");
            if (enchantments == null) {
                ExpeditionEnchantsMod.LOGGER.warn("[ExpeditionEnchants] enchantments_config.json has no 'enchantments' key.");
                TIER_MAP.set(Collections.emptyMap());
                return;
            }

            Map<String, String> map = new HashMap<>(enchantments.size());
            for (Map.Entry<String, JsonElement> entry : enchantments.entrySet()) {
                String id = entry.getKey(); // e.g. "enchantencore:sharpness" or "minecraft:sharpness"
                JsonObject obj = entry.getValue().getAsJsonObject();

                // Skip disabled_duplicate entries
                JsonElement tierEl = obj.get("tier");
                if (tierEl == null || !tierEl.isJsonPrimitive()) continue;
                String tier = tierEl.getAsString().toLowerCase(java.util.Locale.ROOT);
                if (tier.equals("disabled_duplicate")) continue;

                // If marked as curse, override tier
                JsonElement curseEl = obj.get("curse");
                if (curseEl != null && curseEl.isJsonPrimitive() && curseEl.getAsBoolean()) {
                    tier = "curse";
                }

                map.put(id, tier);
            }
            TIER_MAP.set(Collections.unmodifiableMap(map));
            ExpeditionEnchantsMod.LOGGER.info("[ExpeditionEnchants] Loaded {} enchant tiers from enchantments_config.json", map.size());
        } catch (Exception e) {
            ExpeditionEnchantsMod.LOGGER.error("[ExpeditionEnchants] Failed to load enchantments_config.json: {}", e.getMessage(), e);
            TIER_MAP.set(Collections.emptyMap());
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
     * @return true if database was loaded with at least one entry.
     */
    public static boolean isLoaded() {
        return !TIER_MAP.get().isEmpty();
    }
}
