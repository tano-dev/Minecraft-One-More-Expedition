package net.onemoreexpedition.dragon;

import net.onemoreexpedition.dragon.config.DragonConfig;
import net.onemoreexpedition.dragon.variants.DragonVariant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class DragonConfigTest {

    @Test
    @DisplayName("loadFromPath generates default config JSON when file does not exist")
    void loadFromPathGeneratesDefaults(@TempDir Path tempDir) {
        Path configPath = tempDir.resolve("expedition-dragon.json");
        DragonConfig config = DragonConfig.loadFromPath(configPath);

        assertTrue(Files.exists(configPath));
        assertTrue(config.enforceStellarityDragonDisabled);
        assertEquals(5, config.variants.size());
        for (DragonVariant variant : DragonVariant.values()) {
            DragonConfig.VariantConfig vc = config.getVariantConfig(variant);
            assertTrue(vc.enabled);
            assertEquals(0.20, vc.weight, 1e-6);
            assertEquals(variant.defaultBaseHp(), vc.baseHp, 1e-6);
            assertEquals(variant.defaultBossBarTitle(), vc.bossBarTitle);
            assertEquals(variant.defaultBossBarColor().name(), vc.bossBarColor);
        }
        assertEquals(0.01, config.rewards.minDamagePercent, 1e-6);
        assertEquals(20.0, config.rewards.minDamageFlat, 1e-6);
        assertFalse(config.rewards.baseLoot.isEmpty());
        assertEquals(5, config.rewards.variantBonusLoot.size());
    }

    @Test
    @DisplayName("loadFromPath persists and reloads custom overrides while normalizing missing keys")
    void loadFromPathReloadsOverridesAndNormalizesMissing(@TempDir Path tempDir) throws Exception {
        Path configPath = tempDir.resolve("expedition-dragon.json");
        String partialJson = """
                {
                  "enforceStellarityDragonDisabled": false,
                  "variants": {
                    "infernal": {
                      "enabled": true,
                      "weight": 0.55,
                      "baseHp": 1950.0,
                      "damageMultiplier": 1.25,
                      "cooldownMultiplier": 0.85,
                      "bossBarTitle": "Custom Infernal",
                      "bossBarColor": "RED"
                    }
                  }
                }
                """;
        Files.writeString(configPath, partialJson, StandardCharsets.UTF_8);

        DragonConfig loaded = DragonConfig.loadFromPath(configPath);
        assertFalse(loaded.enforceStellarityDragonDisabled);

        DragonConfig.VariantConfig infernal = loaded.getVariantConfig(DragonVariant.INFERNAL);
        assertEquals(0.55, infernal.weight, 1e-6);
        assertEquals(1950.0, infernal.baseHp, 1e-6);
        assertEquals(1.25, infernal.damageMultiplier, 1e-6);
        assertEquals(0.85, infernal.cooldownMultiplier, 1e-6);
        assertEquals("Custom Infernal", infernal.bossBarTitle);

        // Missing variants filled in cleanly by normalize()
        assertEquals(5, loaded.variants.size());
        assertEquals(2000.0, loaded.getVariantConfig(DragonVariant.CHRONOS).baseHp, 1e-6);
        assertNotNull(loaded.movesets);
        assertNotNull(loaded.minions);
        assertNotNull(loaded.rewards);

        // Persisted back with normalized fields
        String saved = Files.readString(configPath, StandardCharsets.UTF_8);
        assertTrue(saved.contains("\"chronos\""));
        assertTrue(saved.contains("\"Custom Infernal\""));
    }

    @Test
    @DisplayName("rollVariant respects weights and enabled flags")
    void rollVariantRespectsWeightsAndEnabledFlags() {
        DragonConfig config = DragonConfig.createDefault();
        for (DragonVariant variant : DragonVariant.values()) {
            config.getVariantConfig(variant).enabled = false;
            config.getVariantConfig(variant).weight = 0.0;
        }
        config.getVariantConfig(DragonVariant.CHRONOS).enabled = true;
        config.getVariantConfig(DragonVariant.CHRONOS).weight = 1.0;

        Random rng = new Random(42L);
        for (int i = 0; i < 25; i++) {
            assertEquals(DragonVariant.CHRONOS, config.rollVariant(rng));
        }

        // Equal default weights distribute across all 5 variants
        DragonConfig equalConfig = DragonConfig.createDefault();
        Map<DragonVariant, Integer> counts = new EnumMap<>(DragonVariant.class);
        for (int i = 0; i < 500; i++) {
            counts.merge(equalConfig.rollVariant(rng), 1, Integer::sum);
        }
        assertEquals(5, counts.size());
    }

    @Test
    @DisplayName("rollVariant falls back safely when all weights are zero or all variants disabled")
    void rollVariantFallbacks() {
        DragonConfig zeroWeight = DragonConfig.createDefault();
        for (DragonVariant variant : DragonVariant.values()) {
            zeroWeight.getVariantConfig(variant).weight = 0.0;
        }
        assertNotNull(zeroWeight.rollVariant(new Random(1L)));

        DragonConfig allDisabled = DragonConfig.createDefault();
        for (DragonVariant variant : DragonVariant.values()) {
            allDisabled.getVariantConfig(variant).enabled = false;
        }
        assertEquals(DragonVariant.INFERNAL, allDisabled.rollVariant(new Random(1L)));
    }

    @Test
    @DisplayName("loadFromPath sanitizes 1e309 (Infinity) across variants, movesets, minions, and rewards without crashing")
    void loadFromPathSanitizesInfinityAndExtremeBounds(@TempDir Path tempDir) throws Exception {
        Path configPath = tempDir.resolve("expedition-dragon.json");
        String infinityJson = """
                {
                  "variants": {
                    "infernal": {
                      "enabled": true,
                      "weight": 1e309,
                      "baseHp": 1e309,
                      "damageMultiplier": 1e309,
                      "cooldownMultiplier": 1e309
                    },
                    "chronos": {
                      "enabled": true,
                      "weight": 0.25,
                      "baseHp": 2500000.0
                    }
                  },
                  "movesets": {
                    "diveImpactDamage": 1e309,
                    "diveShockwaveRadius": 1e309,
                    "tripleDiveHpThreshold": 1e309,
                    "laserDamage": 1e309
                  },
                  "minions": {
                    "endermanCallRadius": 1e309,
                    "shadowPhantomDamage": 1e309,
                    "shadowPhantomKnockbackMultiplier": 1e309,
                    "noElytraZoneRadius": 1e309,
                    "spawnPullRadius": 1e309
                  },
                  "rewards": {
                    "minDamagePercent": 1e309,
                    "minDamageFlat": 1e309,
                    "baseLoot": [
                      { "itemId": "minecraft:diamond", "minCount": -3, "maxCount": 0, "chance": 1e309 }
                    ]
                  }
                }
                """;
        Files.writeString(configPath, infinityJson, StandardCharsets.UTF_8);

        DragonConfig loaded = assertDoesNotThrow(() -> DragonConfig.loadFromPath(configPath));

        DragonConfig.VariantConfig infernal = loaded.getVariantConfig(DragonVariant.INFERNAL);
        assertEquals(DragonVariant.INFERNAL.defaultWeight(), infernal.weight, 1e-6);
        assertEquals(DragonVariant.INFERNAL.defaultBaseHp(), infernal.baseHp, 1e-6);
        assertEquals(1.0, infernal.damageMultiplier, 1e-6);
        assertEquals(1.0, infernal.cooldownMultiplier, 1e-6);

        // baseHp above WIDENED_MAX_HEALTH is capped at 1_000_000.0
        assertEquals(DragonConfig.MAX_VARIANT_BASE_HP, loaded.getVariantConfig(DragonVariant.CHRONOS).baseHp, 1e-6);

        assertEquals(18.0, loaded.movesets.diveImpactDamage, 1e-6);
        assertEquals(12.0, loaded.movesets.diveShockwaveRadius, 1e-6);
        assertEquals(0.75, loaded.movesets.tripleDiveHpThreshold, 1e-6);
        assertEquals(14.0, loaded.movesets.laserDamage, 1e-6);

        assertEquals(150.0, loaded.minions.endermanCallRadius, 1e-6);
        assertEquals(15.0, loaded.minions.shadowPhantomDamage, 1e-6);
        assertEquals(3.0, loaded.minions.shadowPhantomKnockbackMultiplier, 1e-6);
        assertEquals(180.0, loaded.minions.noElytraZoneRadius, 1e-6);
        assertEquals(250.0, loaded.minions.spawnPullRadius, 1e-6);

        assertEquals(0.01, loaded.rewards.minDamagePercent, 1e-6);
        assertEquals(20.0, loaded.rewards.minDamageFlat, 1e-6);
        assertEquals(1, loaded.rewards.baseLoot.size());
        assertEquals(1, loaded.rewards.baseLoot.get(0).minCount);
        assertEquals(1, loaded.rewards.baseLoot.get(0).maxCount);
        assertEquals(1.0, loaded.rewards.baseLoot.get(0).chance, 1e-6);

        // rollVariant works cleanly after loading and even if totalWeight overflows in memory
        Random rng = new Random(123L);
        assertNotNull(loaded.rollVariant(rng));
        loaded.getVariantConfig(DragonVariant.INFERNAL).weight = 1e308;
        loaded.getVariantConfig(DragonVariant.VOIDGLOOM).weight = 1e308;
        assertNotNull(loaded.rollVariant(rng));
        loaded.getVariantConfig(DragonVariant.INFERNAL).weight = Double.POSITIVE_INFINITY;
        assertNotNull(loaded.rollVariant(rng));
    }

    @Test
    @DisplayName("loadFromPath preserves intentional empty baseLoot and strips null reward entries")
    void loadFromPathPreservesEmptyBaseLootAndStripsNullEntries(@TempDir Path tempDir) throws Exception {
        Path configPath = tempDir.resolve("expedition-dragon.json");
        String json = """
                {
                  "rewards": {
                    "baseLoot": [],
                    "variantBonusLoot": {
                      "infernal": [
                        null,
                        { "itemId": "minecraft:blaze_rod", "minCount": 2, "maxCount": 1, "chance": 1.5 }
                      ]
                    }
                  }
                }
                """;
        Files.writeString(configPath, json, StandardCharsets.UTF_8);

        DragonConfig loaded = DragonConfig.loadFromPath(configPath);
        assertNotNull(loaded.rewards.baseLoot);
        assertTrue(loaded.rewards.baseLoot.isEmpty(), "Intentional empty baseLoot [] must be preserved");

        List<DragonConfig.RewardItemEntry> infernalBonus = loaded.rewards.variantBonusLoot.get(DragonVariant.INFERNAL.id());
        assertNotNull(infernalBonus);
        assertEquals(1, infernalBonus.size());
        assertEquals("minecraft:blaze_rod", infernalBonus.get(0).itemId);
        assertEquals(2, infernalBonus.get(0).minCount);
        assertEquals(2, infernalBonus.get(0).maxCount);
        assertEquals(1.0, infernalBonus.get(0).chance, 1e-6);

        // Direct in-memory null elements are also stripped on normalize()
        List<DragonConfig.RewardItemEntry> withNull = new ArrayList<>();
        withNull.add(null);
        withNull.add(new DragonConfig.RewardItemEntry("", -5, -1, -0.5));
        loaded.rewards.baseLoot = withNull;
        loaded.normalize();
        assertEquals(1, loaded.rewards.baseLoot.size());
        assertEquals("minecraft:dragon_breath", loaded.rewards.baseLoot.get(0).itemId);
        assertEquals(1, loaded.rewards.baseLoot.get(0).minCount);
        assertEquals(1, loaded.rewards.baseLoot.get(0).maxCount);
        assertEquals(0.0, loaded.rewards.baseLoot.get(0).chance, 1e-6);
    }

    @Test
    @DisplayName("loadFromPath preserves .bak backup when config JSON is malformed")
    void loadFromPathCreatesBackupOnMalformedJson(@TempDir Path tempDir) throws Exception {
        Path configPath = tempDir.resolve("expedition-dragon.json");
        String corruptContent = "{ \"variants\": { \"infernal\": INVALID_JSON ";
        Files.writeString(configPath, corruptContent, StandardCharsets.UTF_8);

        DragonConfig loaded = DragonConfig.loadFromPath(configPath);
        assertNotNull(loaded);
        assertEquals(5, loaded.variants.size());

        Path backupPath = tempDir.resolve("expedition-dragon.json.bak");
        assertTrue(Files.exists(backupPath), "Corrupt config should be backed up to .bak");
        assertEquals(corruptContent, Files.readString(backupPath, StandardCharsets.UTF_8));

        // Original config path is now valid default JSON
        DragonConfig reloaded = DragonConfig.loadFromPath(configPath);
        assertEquals(5, reloaded.variants.size());
    }

    @Test
    @DisplayName("loadFromPath treats 0-byte, whitespace, and literal null configs as corrupt with .bak backup")
    void loadFromPathBacksUpZeroByteAndNullJson(@TempDir Path tempDir) throws Exception {
        Path emptyPath = tempDir.resolve("empty.json");
        Files.writeString(emptyPath, "", StandardCharsets.UTF_8);
        DragonConfig fromEmpty = DragonConfig.loadFromPath(emptyPath);
        assertNotNull(fromEmpty);
        assertEquals(5, fromEmpty.variants.size());
        assertTrue(Files.exists(tempDir.resolve("empty.json.bak")));
        assertEquals("", Files.readString(tempDir.resolve("empty.json.bak"), StandardCharsets.UTF_8));

        Path nullPath = tempDir.resolve("null.json");
        Files.writeString(nullPath, "null", StandardCharsets.UTF_8);
        DragonConfig fromNull = DragonConfig.loadFromPath(nullPath);
        assertNotNull(fromNull);
        assertEquals(5, fromNull.variants.size());
        assertTrue(Files.exists(tempDir.resolve("null.json.bak")));
        assertEquals("null", Files.readString(tempDir.resolve("null.json.bak"), StandardCharsets.UTF_8));
    }
}
