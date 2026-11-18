package net.onemoreexpedition.dragon.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import net.fabricmc.loader.api.FabricLoader;
import net.onemoreexpedition.dragon.variants.DragonVariant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class DragonConfig {
    public static final double MAX_VARIANT_BASE_HP = 1_000_000.0D;
    private static final Logger LOGGER = LoggerFactory.getLogger("expedition-dragon");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static volatile DragonConfig INSTANCE;

    public boolean enforceStellarityDragonDisabled = true;
    public Map<String, VariantConfig> variants = new LinkedHashMap<>();
    public MovesetConfig movesets = new MovesetConfig();
    public MinionConfig minions = new MinionConfig();
    public RewardConfig rewards = new RewardConfig();

    public static DragonConfig get() {
        if (INSTANCE == null) {
            return load();
        }
        return INSTANCE;
    }

    public static synchronized DragonConfig load() {
        Path configPath = FabricLoader.getInstance().getConfigDir().resolve("expedition-dragon.json");
        INSTANCE = loadFromPath(configPath);
        return INSTANCE;
    }

    public static synchronized DragonConfig reload() {
        return load();
    }

    public static DragonConfig loadFromPath(Path configPath) {
        DragonConfig config = null;
        if (Files.exists(configPath)) {
            try (Reader reader = Files.newBufferedReader(configPath, StandardCharsets.UTF_8)) {
                config = GSON.fromJson(reader, DragonConfig.class);
                if (config == null) {
                    LOGGER.warn("Empty or null JSON in config {}, preserving .bak and falling back to defaults", configPath);
                    backupCorruptConfig(configPath);
                }
            } catch (JsonParseException e) {
                LOGGER.error("Malformed JSON in config {}, preserving .bak and falling back to defaults", configPath, e);
                backupCorruptConfig(configPath);
            } catch (Exception e) {
                LOGGER.error("Failed to read config from {}, preserving .bak and falling back to defaults", configPath, e);
                backupCorruptConfig(configPath);
            }
        }
        if (config == null) {
            config = createDefault();
        }
        config.normalize();
        config.saveToPath(configPath);
        return config;
    }

    private static void backupCorruptConfig(Path configPath) {
        try {
            if (Files.exists(configPath) && configPath.getFileName() != null) {
                Path backupPath = configPath.resolveSibling(configPath.getFileName().toString() + ".bak");
                Files.copy(configPath, backupPath, StandardCopyOption.REPLACE_EXISTING);
                LOGGER.warn("Preserved corrupt config backup at {}", backupPath);
            }
        } catch (IOException | RuntimeException e) {
            LOGGER.error("Failed to back up corrupt config {}", configPath, e);
        }
    }

    public static DragonConfig createDefault() {
        DragonConfig config = new DragonConfig();
        config.normalize();
        return config;
    }

    public void saveToPath(Path configPath) {
        try {
            Path parent = configPath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(configPath, GSON.toJson(this), StandardCharsets.UTF_8);
        } catch (IOException | RuntimeException e) {
            LOGGER.error("Failed to save config to {}", configPath, e);
        }
    }

    public void normalize() {
        if (variants == null) {
            variants = new LinkedHashMap<>();
        }
        for (DragonVariant variant : DragonVariant.values()) {
            VariantConfig vc = variants.get(variant.id());
            if (vc == null) {
                vc = VariantConfig.defaultFor(variant);
                variants.put(variant.id(), vc);
            } else {
                vc.normalize(variant);
            }
        }
        if (movesets == null) {
            movesets = new MovesetConfig();
        }
        movesets.normalize();
        if (minions == null) {
            minions = new MinionConfig();
        }
        minions.normalize();
        if (rewards == null) {
            rewards = new RewardConfig();
        }
        rewards.normalize();
    }

    public VariantConfig getVariantConfig(DragonVariant variant) {
        if (variant == null) {
            variant = DragonVariant.INFERNAL;
        }
        VariantConfig vc = variants != null ? variants.get(variant.id()) : null;
        return vc != null ? vc : VariantConfig.defaultFor(variant);
    }

    public DragonVariant rollVariant(Random random) {
        Random rng = random != null ? random : new Random();
        List<DragonVariant> enabled = new ArrayList<>();
        double totalWeight = 0.0;
        for (DragonVariant variant : DragonVariant.values()) {
            VariantConfig vc = getVariantConfig(variant);
            if (vc.enabled) {
                enabled.add(variant);
                if (Double.isFinite(vc.weight) && vc.weight > 0.0) {
                    totalWeight += vc.weight;
                }
            }
        }
        if (enabled.isEmpty()) {
            return DragonVariant.INFERNAL;
        }
        if (!Double.isFinite(totalWeight) || totalWeight <= 0.0) {
            return enabled.get(rng.nextInt(enabled.size()));
        }
        double roll = rng.nextDouble() * totalWeight;
        double cumulative = 0.0;
        DragonVariant lastPositive = enabled.get(0);
        for (DragonVariant variant : enabled) {
            double w = getVariantConfig(variant).weight;
            if (!Double.isFinite(w) || w <= 0.0) {
                continue;
            }
            lastPositive = variant;
            cumulative += w;
            if (roll < cumulative) {
                return variant;
            }
        }
        return lastPositive;
    }

    public static class VariantConfig {
        public boolean enabled = true;
        public double weight = 0.20;
        public double baseHp = 1500.0;
        public double damageMultiplier = 1.0;
        public double cooldownMultiplier = 1.0;
        public String bossBarTitle = "";
        public String bossBarColor = "RED";

        public static VariantConfig defaultFor(DragonVariant variant) {
            VariantConfig vc = new VariantConfig();
            vc.enabled = true;
            vc.weight = variant.defaultWeight();
            vc.baseHp = variant.defaultBaseHp();
            vc.damageMultiplier = 1.0;
            vc.cooldownMultiplier = 1.0;
            vc.bossBarTitle = variant.defaultBossBarTitle();
            vc.bossBarColor = variant.defaultBossBarColor().name();
            return vc;
        }

        public void normalize(DragonVariant variant) {
            if (!Double.isFinite(weight) || weight < 0.0) {
                weight = variant.defaultWeight();
            }
            if (!Double.isFinite(baseHp) || baseHp <= 0.0) {
                baseHp = variant.defaultBaseHp();
            } else if (baseHp < 1.0) {
                baseHp = 1.0;
            } else if (baseHp > MAX_VARIANT_BASE_HP) {
                baseHp = MAX_VARIANT_BASE_HP;
            }
            if (!Double.isFinite(damageMultiplier) || damageMultiplier <= 0.0) {
                damageMultiplier = 1.0;
            }
            if (!Double.isFinite(cooldownMultiplier) || cooldownMultiplier <= 0.0) {
                cooldownMultiplier = 1.0;
            }
            if (bossBarTitle == null || bossBarTitle.isBlank()) {
                bossBarTitle = variant.defaultBossBarTitle();
            }
            if (bossBarColor == null || bossBarColor.isBlank()) {
                bossBarColor = variant.defaultBossBarColor().name();
            }
        }
    }

    public static class MovesetConfig {
        public int diveCooldownTicks = 260;
        public double diveImpactDamage = 18.0;
        public double diveShockwaveRadius = 12.0;
        public double tripleDiveHpThreshold = 0.75;
        public int tripleDiveCooldownTicks = 360;
        public int laserCooldownTicks = 320;
        public double laserDamage = 14.0;
        public int chainFireballCooldownTicks = 180;
        public int chainFireballCount = 4;

        public void normalize() {
            if (diveCooldownTicks < 20) diveCooldownTicks = 260;
            if (!Double.isFinite(diveImpactDamage) || diveImpactDamage < 0.0) diveImpactDamage = 18.0;
            if (!Double.isFinite(diveShockwaveRadius) || diveShockwaveRadius < 1.0) diveShockwaveRadius = 12.0;
            if (!Double.isFinite(tripleDiveHpThreshold) || tripleDiveHpThreshold <= 0.0 || tripleDiveHpThreshold > 1.0) tripleDiveHpThreshold = 0.75;
            if (tripleDiveCooldownTicks < 20) tripleDiveCooldownTicks = 360;
            if (laserCooldownTicks < 20) laserCooldownTicks = 320;
            if (!Double.isFinite(laserDamage) || laserDamage < 0.0) laserDamage = 14.0;
            if (chainFireballCooldownTicks < 20) chainFireballCooldownTicks = 180;
            if (chainFireballCount < 1) chainFireballCount = 4;
        }
    }

    public static class MinionConfig {
        public double endermanCallRadius = 150.0;
        public int shadowPhantomsPerCrystal = 2;
        public int revenantShadowPhantomsPerCrystal = 5;
        public double shadowPhantomDamage = 15.0;
        public double shadowPhantomKnockbackMultiplier = 3.0;
        public int antiTunnelCheckIntervalTicks = 40;
        public int altarAntiFlightVexCount = 5;
        public int altarAntiFlightAirborneTicks = 60;
        public double noElytraZoneRadius = 180.0;
        public double spawnPullRadius = 250.0;

        public void normalize() {
            if (!Double.isFinite(endermanCallRadius) || endermanCallRadius < 1.0) endermanCallRadius = 150.0;
            if (shadowPhantomsPerCrystal < 0) shadowPhantomsPerCrystal = 2;
            if (revenantShadowPhantomsPerCrystal < 0) revenantShadowPhantomsPerCrystal = 5;
            if (!Double.isFinite(shadowPhantomDamage) || shadowPhantomDamage < 0.0) shadowPhantomDamage = 15.0;
            if (!Double.isFinite(shadowPhantomKnockbackMultiplier) || shadowPhantomKnockbackMultiplier < 0.0) shadowPhantomKnockbackMultiplier = 3.0;
            if (antiTunnelCheckIntervalTicks < 1) antiTunnelCheckIntervalTicks = 40;
            if (altarAntiFlightVexCount < 0) altarAntiFlightVexCount = 5;
            if (altarAntiFlightAirborneTicks < 1) altarAntiFlightAirborneTicks = 60;
            if (!Double.isFinite(noElytraZoneRadius) || noElytraZoneRadius < 1.0) noElytraZoneRadius = 180.0;
            if (!Double.isFinite(spawnPullRadius) || spawnPullRadius < 1.0) spawnPullRadius = 250.0;
        }
    }

    public static class RewardItemEntry {
        public String itemId = "minecraft:dragon_breath";
        public int minCount = 1;
        public int maxCount = 1;
        public double chance = 1.0;

        public RewardItemEntry() {
        }

        public RewardItemEntry(String itemId, int minCount, int maxCount, double chance) {
            this.itemId = itemId;
            this.minCount = minCount;
            this.maxCount = maxCount;
            this.chance = chance;
        }

        public void normalize() {
            if (itemId == null || itemId.isBlank()) {
                itemId = "minecraft:dragon_breath";
            }
            if (minCount < 0) {
                minCount = 1;
            }
            if (maxCount < minCount) {
                maxCount = minCount;
            }
            if (!Double.isFinite(chance)) {
                chance = 1.0;
            } else if (chance < 0.0) {
                chance = 0.0;
            } else if (chance > 1.0) {
                chance = 1.0;
            }
        }
    }

    public static class RewardConfig {
        public double minDamagePercent = 0.01;
        public double minDamageFlat = 20.0;
        public int baseRolls = 2;
        public int maxDamageBonusRolls = 6;
        public long guaranteedCoins = 500L;
        public long bonusCoinsPerRoll = 250L;
        public List<RewardItemEntry> baseLoot = defaultBaseLoot();
        public Map<String, List<RewardItemEntry>> variantBonusLoot = defaultVariantBonusLoot();

        public void normalize() {
            if (!Double.isFinite(minDamagePercent) || minDamagePercent < 0.0) {
                minDamagePercent = 0.01;
            }
            if (!Double.isFinite(minDamageFlat) || minDamageFlat < 0.0) {
                minDamageFlat = 20.0;
            }
            if (baseRolls < 0) baseRolls = 2;
            if (maxDamageBonusRolls < 0) maxDamageBonusRolls = 6;
            if (guaranteedCoins < 0) guaranteedCoins = 0;
            if (bonusCoinsPerRoll < 0) bonusCoinsPerRoll = 0;
            if (baseLoot == null) {
                baseLoot = defaultBaseLoot();
            } else {
                baseLoot = sanitizeEntries(baseLoot);
            }
            if (variantBonusLoot == null) {
                variantBonusLoot = new LinkedHashMap<>();
            }
            Map<String, List<RewardItemEntry>> defaults = defaultVariantBonusLoot();
            for (DragonVariant variant : DragonVariant.values()) {
                variantBonusLoot.computeIfAbsent(variant.id(), key -> new ArrayList<>(defaults.get(key)));
            }
            for (Map.Entry<String, List<RewardItemEntry>> entry : variantBonusLoot.entrySet()) {
                List<RewardItemEntry> raw = entry.getValue();
                if (raw == null) {
                    List<RewardItemEntry> fallback = defaults.get(entry.getKey());
                    raw = fallback != null ? new ArrayList<>(fallback) : new ArrayList<>();
                }
                entry.setValue(sanitizeEntries(raw));
            }
        }

        private static List<RewardItemEntry> sanitizeEntries(List<RewardItemEntry> entries) {
            List<RewardItemEntry> clean = new ArrayList<>();
            for (RewardItemEntry entry : entries) {
                if (entry != null) {
                    entry.normalize();
                    clean.add(entry);
                }
            }
            return clean;
        }

        private static List<RewardItemEntry> defaultBaseLoot() {
            List<RewardItemEntry> list = new ArrayList<>();
            list.add(new RewardItemEntry("minecraft:dragon_breath", 2, 6, 1.0));
            list.add(new RewardItemEntry("minecraft:experience_bottle", 8, 16, 1.0));
            list.add(new RewardItemEntry("minecraft:ender_pearl", 4, 12, 0.75));
            return list;
        }

        private static Map<String, List<RewardItemEntry>> defaultVariantBonusLoot() {
            Map<String, List<RewardItemEntry>> map = new LinkedHashMap<>();
            map.put(DragonVariant.INFERNAL.id(), List.of(
                    new RewardItemEntry("minecraft:blaze_rod", 3, 6, 0.85),
                    new RewardItemEntry("minecraft:fire_charge", 4, 8, 1.0)
            ));
            map.put(DragonVariant.VOIDGLOOM.id(), List.of(
                    new RewardItemEntry("minecraft:chorus_fruit", 6, 12, 1.0),
                    new RewardItemEntry("minecraft:shulker_shell", 1, 2, 0.65)
            ));
            map.put(DragonVariant.REVENANT.id(), List.of(
                    new RewardItemEntry("minecraft:phantom_membrane", 3, 6, 1.0),
                    new RewardItemEntry("minecraft:ender_eye", 2, 4, 0.75)
            ));
            map.put(DragonVariant.ENLIGHTENED.id(), List.of(
                    new RewardItemEntry("minecraft:prismarine_crystals", 6, 12, 1.0),
                    new RewardItemEntry("minecraft:heart_of_the_sea", 1, 1, 0.35)
            ));
            map.put(DragonVariant.CHRONOS.id(), List.of(
                    new RewardItemEntry("minecraft:echo_shard", 2, 5, 0.85),
                    new RewardItemEntry("minecraft:nether_star", 1, 1, 0.25)
            ));
            return map;
        }
    }
}
