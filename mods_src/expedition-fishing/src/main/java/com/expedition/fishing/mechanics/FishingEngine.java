package com.expedition.fishing.mechanics;

import com.expedition.fishing.api.ExpeditionFishingApi;
import com.expedition.fishing.biome.BiomeResolver;
import com.expedition.fishing.config.FishingDataRegistry;
import com.expedition.fishing.data.FishingPlayerData;
import com.expedition.fishing.data.PlayerDataStorage;
import com.expedition.fishing.item.FishingItemFactory;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.*;

public class FishingEngine {

    public static class PlayerFishingStats {
        public double fishingSpeed = 0.0;
        public double seaCreatureChance = 15.0; // Base 15% SCC
        public double rareCreatureBonus = 0.0;
        public double treasureChance = 5.0;     // Base 5% Treasure
        public double doubleCatchChance = 0.0;
        public double xpMultiplier = 1.0;
        public double weightMultiplier = 1.0;
        public double heatReduction = 0.0;
        public double baitSaveChance = 0.0;
        public double scavBonus = 0.0;
        public double bossWeightBonus = 0.0;
        public double biomeWeightBonus = 0.0;
        public double artifactBonusChance = 0.0;
        public double mirrorCastChance = 0.0;
        public double chronosBypassChance = 0.0;
        public double dimensionalDriftChance = 0.0;
        public boolean autoSmeltFish = false;
        public int frostbiteLevel = 0;
        public int stunningYankLevel = 0;
        public int stormCallerLevel = 0;
        public int feastLevel = 0;
        public int chainCatchLevel = 0;
        public int luckyCloverLevel = 0;
        public boolean poseidonsWrath = false;
        public boolean eternalAngler = false;
        public String baitOverrideBiome = null;
    }

    public static Map<String, Integer> getRodEnchants(ItemStack rod) {
        Map<String, Integer> map = new HashMap<>();
        if (rod == null || rod.isEmpty()) return map;
        ItemEnchantments enchs = rod.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        for (Map.Entry<Holder<Enchantment>, Integer> entry : enchs.entrySet()) {
            String id = entry.getKey().unwrapKey().map(k -> k.identifier().toString()).orElse("");
            if (!id.isEmpty()) {
                map.put(id, entry.getValue());
            }
        }
        return map;
    }

    public static PlayerFishingStats computeStats(ServerPlayer player, ItemStack rod, boolean inLava, boolean inVoid) {
        PlayerFishingStats st = new PlayerFishingStats();
        FishingPlayerData data = PlayerDataStorage.get(player.getUUID());
        ServerLevel level = player.level();
        BlockPos pos = player.blockPosition();

        // 1. Level 1-50 Scaling
        st.fishingSpeed += data.level * 1.0;
        st.treasureChance += data.level * 0.3;
        st.doubleCatchChance += data.level * 0.4;
        st.xpMultiplier += data.level * 0.01;

        // 2. Rod Custom Stats
        String rodId = FishingItemFactory.getCustomItemId(rod);
        if (rodId != null) {
            JsonObject rCfg = FishingDataRegistry.ITEMS.get(rodId);
            if (rCfg != null) {
                int reqLv = FishingDataRegistry.getInt(rCfg, "required_level", 0);
                if (data.level >= reqLv) {
                    st.fishingSpeed += FishingDataRegistry.getDouble(rCfg, "fishing_speed", 0.0);
                    st.seaCreatureChance += FishingDataRegistry.getDouble(rCfg, "scc", FishingDataRegistry.getDouble(rCfg, "rare_creature_chance", 0.0));
                    st.doubleCatchChance += FishingDataRegistry.getDouble(rCfg, "double_catch", 0.0);
                    st.treasureChance += FishingDataRegistry.getDouble(rCfg, "treasure_bonus", 0.0);
                }
            }
        }

        // 3. Armor Set Bonuses
        EquipmentSlot[] armorSlots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
        for (EquipmentSlot slot : armorSlots) {
            ItemStack piece = player.getItemBySlot(slot);
            String pid = FishingItemFactory.getCustomItemId(piece);
            if (pid != null) {
                JsonObject aCfg = FishingDataRegistry.ITEMS.get(pid);
                if (aCfg != null && data.level >= FishingDataRegistry.getInt(aCfg, "required_level", 0)) {
                    st.seaCreatureChance += FishingDataRegistry.getDouble(aCfg, "scc", 0.0);
                    st.fishingSpeed += FishingDataRegistry.getDouble(aCfg, "fishing_speed", 0.0);
                }
            }
        }

        // 4. Artifact Bag Passives
        for (String artId : data.artifactBag) {
            JsonObject artCfg = FishingDataRegistry.ARTIFACTS.get(artId);
            if (artCfg != null && artCfg.has("passive_effects") && artCfg.get("passive_effects").isJsonArray()) {
                for (JsonElement el : artCfg.getAsJsonArray("passive_effects")) {
                    if (!el.isJsonObject()) continue;
                    JsonObject eff = el.getAsJsonObject();
                    String stat = FishingDataRegistry.getStr(eff, "stat", "");
                    double val = FishingDataRegistry.getDouble(eff, "value", 0.0);
                    switch (stat) {
                        case "fishing_speed" -> st.fishingSpeed += val;
                        case "rare_creature_chance" -> {
                            st.seaCreatureChance += val;
                            st.rareCreatureBonus += val;
                        }
                        case "treasure_chance" -> st.treasureChance += val;
                        case "double_catch_chance" -> st.doubleCatchChance += val;
                        case "fishing_xp_bonus" -> st.xpMultiplier += (val / 100.0);
                    }
                }
            }
        }

        // 5. Off-hand Bait
        ItemStack offhand = player.getOffhandItem();
        String baitId = FishingItemFactory.getCustomItemId(offhand);
        if (baitId != null) {
            JsonObject bCfg = FishingDataRegistry.ITEMS.get(baitId);
            if (bCfg != null && "BAIT".equalsIgnoreCase(FishingDataRegistry.getStr(bCfg, "category", ""))) {
                st.fishingSpeed += FishingDataRegistry.getDouble(bCfg, "fishing_speed_bonus", 10.0);
                st.seaCreatureChance += FishingDataRegistry.getDouble(bCfg, "scc_bonus", 3.0);
                String targetBiome = FishingDataRegistry.getStr(bCfg, "target_biome", "");
                if (!targetBiome.isEmpty()) {
                    st.baitOverrideBiome = targetBiome;
                }
                if (baitId.contains("ice") || baitId.contains("cool") || baitId.contains("frost")) {
                    st.heatReduction += 0.35;
                }
            }
        }

        // 6. All 45 Fishing Enchantments (5 Existing + 40 New expedition_fishing:*)
        Map<String, Integer> enchs = getRodEnchants(rod);
        long dayTime = level.getDefaultClockTime() % 24000L;
        boolean isNight = dayTime >= 13000L && dayTime <= 23000L;
        boolean isRaining = level.isRaining();
        boolean isThundering = level.isThundering();

        int lure = enchs.getOrDefault("minecraft:lure", 0);
        st.fishingSpeed += lure * 10.0;

        int lots = enchs.getOrDefault("minecraft:luck_of_the_sea", 0);
        st.treasureChance += lots * 2.5;
        st.rareCreatureBonus += lots * 1.0;

        int scholar = enchs.getOrDefault("enchantencore:scholar_of_fishing", 0);
        st.xpMultiplier += scholar * 0.15;

        if (enchs.getOrDefault("enchantencore:harpoon", 0) > 0) {
            st.seaCreatureChance += 4.0;
        }
        if (enchs.getOrDefault("enchantencore:yoink", 0) > 0) {
            st.doubleCatchChance += 6.0;
        }

        // 40 New Fishing Enchantments
        st.fishingSpeed += enchs.getOrDefault("expedition_fishing:quick_cast", 0) * 5.0;
        st.weightMultiplier += enchs.getOrDefault("expedition_fishing:heavy_sinker", 0) * 0.03;
        st.baitSaveChance += enchs.getOrDefault("expedition_fishing:bait_saver", 0) * 0.15;
        st.seaCreatureChance += enchs.getOrDefault("expedition_fishing:angler_instinct", 0) * 3.0;
        st.scavBonus += enchs.getOrDefault("expedition_fishing:tide_scavenger", 0) * 0.10;
        if (isRaining) {
            st.fishingSpeed += enchs.getOrDefault("expedition_fishing:steady_hand", 0) * 4.0;
        }
        if (isNight) {
            int nf = enchs.getOrDefault("expedition_fishing:night_fisher", 0);
            st.fishingSpeed += nf * 5.0;
            st.seaCreatureChance += nf * 3.0;
        } else {
            int sc = enchs.getOrDefault("expedition_fishing:sunlit_cast", 0);
            st.fishingSpeed += sc * 5.0;
            st.treasureChance += sc * 3.0;
        }
        if (pos.getY() < 40) {
            int dw = enchs.getOrDefault("expedition_fishing:deep_water", 0);
            st.fishingSpeed += dw * 5.0;
            st.rareCreatureBonus += dw * 3.0;
        } else if (pos.getY() >= 62) {
            st.fishingSpeed += enchs.getOrDefault("expedition_fishing:surface_cast", 0) * 5.0;
        }
        st.stormCallerLevel = enchs.getOrDefault("expedition_fishing:storm_caller", 0);
        if (isThundering && st.stormCallerLevel > 0) {
            st.seaCreatureChance += st.stormCallerLevel * 12.0;
        }
        st.feastLevel = enchs.getOrDefault("expedition_fishing:fishermans_feast", 0);

        // Rare Enchants
        int sccLv = enchs.getOrDefault("expedition_fishing:sea_creature_chance", 0);
        st.seaCreatureChance += sccLv * 4.0;
        st.rareCreatureBonus += sccLv * 4.0;

        st.doubleCatchChance += enchs.getOrDefault("expedition_fishing:double_hook", 0) * 4.0;
        int thLv = enchs.getOrDefault("expedition_fishing:treasure_hunter", 0);
        st.treasureChance += thLv * 3.0;
        st.artifactBonusChance += thLv * 1.5;

        st.heatReduction += enchs.getOrDefault("expedition_fishing:thermal_cooling", 0) * 0.20;
        int hcLv = enchs.getOrDefault("expedition_fishing:heat_channeling", 0);
        if (inLava && hcLv > 0) {
            st.seaCreatureChance += (int) (data.heat / 20.0) * (hcLv * 3.0);
        }
        int slayerLv = enchs.getOrDefault("expedition_fishing:slayer_hook", 0);
        if (slayerLv > 0) {
            data.slayerBuffLevel = slayerLv;
        }
        st.biomeWeightBonus += enchs.getOrDefault("expedition_fishing:biome_attunement", 0) * 0.05;
        int trophyLv = enchs.getOrDefault("expedition_fishing:trophy_hunter", 0);
        st.weightMultiplier += trophyLv * 0.08;
        st.frostbiteLevel = enchs.getOrDefault("expedition_fishing:frostbite_hook", 0);
        if (enchs.getOrDefault("expedition_fishing:blazing_line", 0) > 0) {
            st.autoSmeltFish = true;
            if (inLava) st.seaCreatureChance += 5.0;
        }
        st.stunningYankLevel = enchs.getOrDefault("expedition_fishing:stunning_yank", 0);
        st.luckyCloverLevel = enchs.getOrDefault("expedition_fishing:lucky_clover", 0);
        st.chainCatchLevel = enchs.getOrDefault("expedition_fishing:chain_catch", 0);
        st.xpMultiplier += enchs.getOrDefault("expedition_fishing:culling_line", 0) * 0.15;
        if (inLava) {
            st.rareCreatureBonus += enchs.getOrDefault("expedition_fishing:magma_tether", 0) * 4.0;
        }

        // Epic Enchants
        int voidAnchor = enchs.getOrDefault("expedition_fishing:void_anchor", 0);
        if (inVoid || level.dimension() == Level.END) {
            st.seaCreatureChance += voidAnchor * 6.0;
            st.rareCreatureBonus += voidAnchor * 6.0;
        }
        if (enchs.getOrDefault("expedition_fishing:abyssal_blessing", 0) > 0) {
            st.artifactBonusChance += 5.0;
        }
        st.bossWeightBonus += enchs.getOrDefault("expedition_fishing:boss_lure", 0) * 0.15;
        if (enchs.getOrDefault("expedition_fishing:frenzy_reel", 0) > 0) {
            int stacks = Math.min(5, data.frenzyStacks);
            st.fishingSpeed += stacks * 3.0;
            st.seaCreatureChance += stacks * 1.5;
        }
        if (enchs.getOrDefault("expedition_fishing:mirror_cast", 0) > 0) {
            st.mirrorCastChance = 0.12;
        }
        st.chronosBypassChance = enchs.getOrDefault("expedition_fishing:chronos_line", 0) * 0.35;
        if (enchs.getOrDefault("expedition_fishing:dimensional_drift", 0) > 0) {
            st.dimensionalDriftChance = 0.10;
        }
        if (level.getGameTime() < data.bloodBuffUntilTick) {
            int bloodLv = Math.max(1, enchs.getOrDefault("expedition_fishing:blood_in_the_water", 1));
            st.fishingSpeed += bloodLv * 6.0;
            st.seaCreatureChance += bloodLv * 4.0;
        }

        // Ultimate Enchants
        if (enchs.getOrDefault("expedition_fishing:poseidons_wrath", 0) > 0) {
            st.poseidonsWrath = true;
            st.seaCreatureChance += 10.0;
            st.fishingSpeed += 15.0;
            st.doubleCatchChance += 10.0;
        }
        if (enchs.getOrDefault("expedition_fishing:eternal_angler", 0) > 0) {
            st.eternalAngler = true;
            st.xpMultiplier += 0.25;
            st.baitSaveChance += 0.25;
            st.treasureChance += 10.0;
        }

        // Heat Bonus in Lava
        if (inLava && data.heat >= 50.0) {
            st.seaCreatureChance += (data.heat - 40.0) * 0.25;
            st.rareCreatureBonus += (data.heat - 40.0) * 0.20;
        }

        return st;
    }

    public static void handleCatch(ServerPlayer player, FishingHook hook, ItemStack rod, boolean inLava, boolean inVoid) {
        ServerLevel level = player.level();
        if (BiomeResolver.isSkyblockWorld(level)) return;

        FishingPlayerData data = PlayerDataStorage.get(player.getUUID());
        if (inLava && data.level < 27) {
            player.sendSystemMessage(Component.literal("§c🔥 Lava Fishing requires Fishing Level 27! (Current: Lv." + data.level + ")"));
            return;
        }
        if (inVoid && data.level < 35) {
            player.sendSystemMessage(Component.literal("§d✦ End / Void Fishing requires Fishing Level 35! (Current: Lv." + data.level + ")"));
            return;
        }

        PlayerFishingStats stats = computeStats(player, rod, inLava, inVoid);
        RandomSource rng = level.getRandom();

        // Consume off-hand bait unless saved
        ItemStack offhand = player.getOffhandItem();
        String baitId = FishingItemFactory.getCustomItemId(offhand);
        if (baitId != null && "BAIT".equalsIgnoreCase(FishingDataRegistry.getStr(FishingDataRegistry.ITEMS.get(baitId), "category", ""))) {
            if (rng.nextDouble() >= Math.min(0.85, stats.baitSaveChance)) {
                offhand.shrink(1);
            }
        }

        // Heat accumulation / Eternal Angler venting
        if (inLava) {
            double heatGain = 4.0 * Math.max(0.15, 1.0 - stats.heatReduction);
            if (stats.eternalAngler) heatGain -= 5.0;
            data.addHeat(heatGain);
        } else if (stats.eternalAngler && data.heat > 0) {
            data.addHeat(-5.0);
        }

        // Frenzy stacks & Chain catch
        data.frenzyStacks = Math.min(5, data.frenzyStacks + 1);
        if (stats.chainCatchLevel > 0 && rng.nextDouble() < (stats.chainCatchLevel * 0.08)) {
            data.nextCastFast = true;
            player.sendSystemMessage(Component.literal("§b⚡ Echoing Hook triggered! Next cast wait time halved!"));
        }
        if (stats.feastLevel > 0 && rng.nextDouble() < (stats.feastLevel * 0.20)) {
            player.getFoodData().eat(2, 0.4f);
        }

        int rolls = 1;
        if (rng.nextDouble() * 100.0 < stats.doubleCatchChance) {
            rolls = 2;
            player.sendSystemMessage(Component.literal("§a§l✦ DOUBLE CATCH! §7Your hook pulled up two catches at once!"));
        }

        String biomeGroup = stats.baitOverrideBiome != null
                ? stats.baitOverrideBiome
                : BiomeResolver.resolveBiomeGroup(level, hook.blockPosition(), inLava, inVoid);

        for (int i = 0; i < rolls; i++) {
            executeSingleRoll(player, hook, biomeGroup, stats, rng);
        }

        PlayerDataStorage.saveAsync(player.getUUID());
    }

    private static void executeSingleRoll(ServerPlayer player, FishingHook hook, String biomeGroup, PlayerFishingStats stats, RandomSource rng) {
        ServerLevel level = player.level();
        FishingPlayerData data = PlayerDataStorage.get(player.getUUID());

        // 1. Check Treasure Chest roll
        if (rng.nextDouble() * 100.0 < stats.treasureChance) {
            rollTreasureAndArtifact(player, stats, rng);
            if (stats.mirrorCastChance > 0 && rng.nextDouble() < stats.mirrorCastChance) {
                player.sendSystemMessage(Component.literal("§d✦ Mirror Cast duplicated your Treasure reward!"));
                rollTreasureAndArtifact(player, stats, rng);
            }
            return;
        }

        // 2. Pick Passive vs Hostile Sea Creature based on SCC
        boolean rollHostile = (rng.nextDouble() * 100.0) < stats.seaCreatureChance;
        String mobId = selectMobFromBiomePool(biomeGroup, data.level, rollHostile, stats, rng);
        if (mobId == null) {
            mobId = "cod";
        }

        JsonObject mobCfg = FishingDataRegistry.MOBS.get(mobId);
        if (mobCfg == null) return;

        String displayName = FishingDataRegistry.getStr(mobCfg, "display_name", FishingItemFactory.formatId(mobId));
        String rarity = FishingDataRegistry.getStr(mobCfg, "rarity", "COMMON").toUpperCase(Locale.ROOT);
        String category = FishingDataRegistry.getStr(mobCfg, "category", "PASSIVE").toUpperCase(Locale.ROOT);
        double baseXp = FishingDataRegistry.getDouble(mobCfg, "xp", 15.0);
        double baseWeight = Math.max(0.5, FishingDataRegistry.getDouble(mobCfg, "base_weight", 2.0));

        // Roll weight (kg) with log-normal / right-skewed distribution
        double variance = 0.65 + (rng.nextDouble() * 0.70) + (rng.nextDouble() < 0.15 ? rng.nextDouble() * 0.85 : 0.0);
        double weightKg = Math.round(baseWeight * variance * stats.weightMultiplier * 100.0) / 100.0;
        boolean isHeavyweight = weightKg >= baseWeight * 1.5;

        double xpEarned = baseXp * stats.xpMultiplier * (isHeavyweight ? 1.15 : 1.0);
        if ("LEGENDARY".equals(rarity) && stats.artifactBonusChance >= 5.0) {
            xpEarned *= 1.25;
        }

        boolean isRecord = data.recordMobCatch(mobId, weightKg);
        int oldLevel = data.level;
        int gainedLevels = data.addXp(xpEarned);

        // Spawn Sea Creature at bobber & fling toward player
        spawnAndLaunchCreature(player, hook, mobId, mobCfg, displayName, rarity, stats);

        // Grant direct drops (so passive & hostile catches always reward materials cleanly)
        grantMobDrops(player, mobCfg, stats, rng);

        String color = FishingItemFactory.getRarityColor(rarity);
        String recordTag = isRecord ? " §6§l[NEW RECORD!]" : (isHeavyweight ? " §e[Heavyweight]" : "");
        player.sendSystemMessage(Component.literal(
                "§3🎣 Caught " + color + displayName + " §7(" + weightKg + " kg)" + recordTag + " §b+" + Math.round(xpEarned) + " Fishing XP"
        ));

        if ("EPIC".equals(rarity) || "LEGENDARY".equals(rarity)) {
            level.playSound(null, player.blockPosition(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.PLAYERS, 0.7f, 1.2f);
            if ("LEGENDARY".equals(rarity) && level.getServer() != null) {
                level.getServer().getPlayerList().broadcastSystemMessage(
                        Component.literal("§6§l✦ SEA BOSS! §e" + player.getName().getString() + " §7just hooked a " + color + displayName + " §7(" + weightKg + " kg)!"),
                        false
                );
            }
        } else {
            level.playSound(null, player.blockPosition(), SoundEvents.FISHING_BOBBER_RETRIEVE, SoundSource.PLAYERS, 1.0f, 1.0f);
        }

        if (gainedLevels > 0) {
            onLevelUp(player, oldLevel, data.level);
        }

        ExpeditionFishingApi.fireCatch(player, mobId, category, rarity, weightKg, xpEarned);
    }

    private static String selectMobFromBiomePool(String biomeGroup, int playerLevel, boolean wantHostile, PlayerFishingStats stats, RandomSource rng) {
        JsonObject profile = FishingDataRegistry.BIOMES.get(biomeGroup);
        if (profile == null) {
            profile = FishingDataRegistry.BIOMES.get("NORMAL_OCEAN");
        }
        if (profile == null) return "cod";

        String weightKey = wantHostile ? "hostile_weights" : "passive_weights";
        JsonObject weightsObj = profile.has(weightKey) && profile.get(weightKey).isJsonObject()
                ? profile.getAsJsonObject(weightKey)
                : null;

        if (weightsObj == null || weightsObj.entrySet().isEmpty()) {
            weightsObj = profile.has("passive_weights") && profile.get("passive_weights").isJsonObject()
                    ? profile.getAsJsonObject("passive_weights")
                    : null;
        }
        if (weightsObj == null) return "cod";

        List<Map.Entry<String, Double>> candidates = new ArrayList<>();
        double totalWeight = 0.0;

        for (Map.Entry<String, JsonElement> entry : weightsObj.entrySet()) {
            String mobId = entry.getKey();
            double w = entry.getValue().getAsDouble();
            if (w <= 0) continue;

            JsonObject mCfg = FishingDataRegistry.MOBS.get(mobId);
            if (mCfg == null || !FishingDataRegistry.getBool(mCfg, "enabled", true)) continue;
            int reqLv = FishingDataRegistry.getInt(mCfg, "required_level", 0);
            if (playerLevel < reqLv) continue;

            String rarity = FishingDataRegistry.getStr(mCfg, "rarity", "COMMON").toUpperCase(Locale.ROOT);
            if ("RARE".equals(rarity) || "EPIC".equals(rarity) || "LEGENDARY".equals(rarity)) {
                w *= (1.0 + (stats.rareCreatureBonus / 100.0));
            }
            if ("EPIC".equals(rarity) || "LEGENDARY".equals(rarity)) {
                w *= (1.0 + stats.bossWeightBonus);
            }
            totalWeight += w;
            candidates.add(Map.entry(mobId, w));
        }

        if (candidates.isEmpty() || totalWeight <= 0) {
            return "cod";
        }

        double roll = rng.nextDouble() * totalWeight;
        double acc = 0.0;
        for (Map.Entry<String, Double> c : candidates) {
            acc += c.getValue();
            if (roll <= acc) return c.getKey();
        }
        return candidates.get(candidates.size() - 1).getKey();
    }

    private static void spawnAndLaunchCreature(ServerPlayer player, FishingHook hook, String mobId, JsonObject mobCfg, String displayName, String rarity, PlayerFishingStats stats) {
        ServerLevel level = player.level();
        String entityTypeStr = FishingDataRegistry.getStr(mobCfg, "entity_type", "COD").toLowerCase(Locale.ROOT);
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.withDefaultNamespace(entityTypeStr));
        if (type == null) type = BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.withDefaultNamespace("cod"));

        Entity entity = type.create(level, EntitySpawnReason.EVENT);
        if (entity == null) return;

        Vec3 spawnPos = hook.position().add(0, 0.4, 0);
        entity.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
        entity.setCustomName(Component.literal(FishingItemFactory.getRarityColor(rarity) + displayName));
        entity.setCustomNameVisible(true);

        Vec3 dir = player.position().add(0, 1.0, 0).subtract(spawnPos);
        Vec3 vel = new Vec3(dir.x * 0.14, Math.min(0.65, Math.max(0.28, dir.y * 0.15 + 0.32)), dir.z * 0.14);
        entity.setDeltaMovement(vel);

        if (entity instanceof LivingEntity living) {
            if (stats.frostbiteLevel > 0) {
                living.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, stats.frostbiteLevel * 80, 1));
            }
            if (stats.stunningYankLevel > 0) {
                living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, stats.stunningYankLevel * 60, 0));
                living.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, stats.stunningYankLevel * 60, 1));
            }
        }

        level.addFreshEntity(entity);
    }

    private static void grantMobDrops(ServerPlayer player, JsonObject mobCfg, PlayerFishingStats stats, RandomSource rng) {
        if (!mobCfg.has("drops") || !mobCfg.get("drops").isJsonArray()) return;
        String rarity = FishingDataRegistry.getStr(mobCfg, "rarity", "COMMON").toUpperCase(Locale.ROOT);
        boolean isCommonTier = "COMMON".equals(rarity) || "UNCOMMON".equals(rarity);

        for (JsonElement el : mobCfg.getAsJsonArray("drops")) {
            if (!el.isJsonObject()) continue;
            JsonObject d = el.getAsJsonObject();
            double chance = FishingDataRegistry.getDouble(d, "chance", 1.0);
            if (rng.nextDouble() > chance) continue;

            int min = FishingDataRegistry.getInt(d, "min", 1);
            int max = Math.max(min, FishingDataRegistry.getInt(d, "max", min));
            int count = min + (max > min ? rng.nextInt(max - min + 1) : 0);
            if (isCommonTier && stats.scavBonus > 0 && rng.nextDouble() < stats.scavBonus) {
                count += 1;
            }

            ItemStack dropStack = ItemStack.EMPTY;
            if (d.has("item")) {
                String customId = d.get("item").getAsString();
                dropStack = FishingItemFactory.createCustomItem(customId, count);
            } else if (d.has("vanilla")) {
                String vanName = d.get("vanilla").getAsString().toLowerCase(Locale.ROOT);
                if (stats.autoSmeltFish) {
                    if ("cod".equals(vanName)) vanName = "cooked_cod";
                    else if ("salmon".equals(vanName)) vanName = "cooked_salmon";
                }
                Item vanItem = BuiltInRegistries.ITEM.getValue(Identifier.withDefaultNamespace(vanName));
                if (vanItem != null && vanItem != Items.AIR) {
                    dropStack = new ItemStack(vanItem, count);
                }
            }

            if (!dropStack.isEmpty()) {
                giveOrDrop(player, dropStack);
            }
        }
    }

    private static void rollTreasureAndArtifact(ServerPlayer player, PlayerFishingStats stats, RandomSource rng) {
        FishingPlayerData data = PlayerDataStorage.get(player.getUUID());
        data.totalCatches++;
        data.treasuresCaught++;

        // Pick a material reward from custom items
        List<String> matKeys = new ArrayList<>(FishingDataRegistry.ITEMS.keySet());
        if (!matKeys.isEmpty()) {
            String pick = matKeys.get(rng.nextInt(matKeys.size()));
            int qty = 1 + (stats.luckyCloverLevel > 0 && rng.nextDouble() < (stats.luckyCloverLevel * 0.02) ? 1 : 0);
            ItemStack reward = FishingItemFactory.createCustomItem(pick, qty);
            if (!reward.isEmpty()) {
                giveOrDrop(player, reward);
                player.sendSystemMessage(Component.literal("§6🎁 Treasure Catch! §7Found " + reward.getCount() + "x ").append(reward.getHoverName()));
            }
        }

        // Check Artifact discovery (base 6% + bonus)
        double artChance = 6.0 + stats.artifactBonusChance;
        if (rng.nextDouble() * 100.0 < artChance && !FishingDataRegistry.ARTIFACTS.isEmpty()) {
            List<String> artIds = new ArrayList<>(FishingDataRegistry.ARTIFACTS.keySet());
            String artId = artIds.get(rng.nextInt(artIds.size()));
            data.discoveredArtifacts.add(artId);
            if (data.artifactBag.size() < 9 && !data.artifactBag.contains(artId)) {
                data.artifactBag.add(artId);
            }
            ItemStack artStack = FishingItemFactory.createArtifactItem(artId);
            if (!artStack.isEmpty()) {
                giveOrDrop(player, artStack);
                player.sendSystemMessage(Component.literal("§d§l✦ ARTIFACT DISCOVERED! §7You unearthed ").append(artStack.getHoverName()).append(Component.literal("§7!")));
                player.level().playSound(null, player.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 0.9f, 1.1f);
            }
        }

        int oldLv = data.level;
        int gained = data.addXp(45.0 * stats.xpMultiplier);
        if (gained > 0) {
            onLevelUp(player, oldLv, data.level);
        }
        data.dirty = true;
    }

    public static void onLevelUp(ServerPlayer player, int oldLevel, int newLevel) {
        player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0f, 1.0f);
        player.sendSystemMessage(Component.literal("§6§l✦ FISHING LEVEL UP! §eLv." + oldLevel + " §7→ §a§lLv." + newLevel + "§7!"));
        if (newLevel >= 27 && oldLevel < 27) {
            player.sendSystemMessage(Component.literal("§6🔥 Unlocked: §eLava Fishing & Nether/Incendium Pools!"));
        }
        if (newLevel >= 35 && oldLevel < 35) {
            player.sendSystemMessage(Component.literal("§d✦ Unlocked: §5End / Void Fishing (Stellarity & Nullscape Pools)!"));
        }
        ExpeditionFishingApi.fireLevelUp(player, oldLevel, newLevel);
    }

    public static void giveOrDrop(ServerPlayer player, ItemStack stack) {
        if (!player.getInventory().add(stack)) {
            player.spawnAtLocation(player.level(), stack);
        }
    }

    public static Map<String, Integer> simulateRolls(String biomeGroup, int playerLevel, int iterations) {
        Map<String, Integer> results = new LinkedHashMap<>();
        PlayerFishingStats dummyStats = new PlayerFishingStats();
        dummyStats.seaCreatureChance = 25.0;
        RandomSource rng = RandomSource.create();
        for (int i = 0; i < iterations; i++) {
            boolean hostile = (rng.nextDouble() * 100.0) < dummyStats.seaCreatureChance;
            String mob = selectMobFromBiomePool(biomeGroup, playerLevel, hostile, dummyStats, rng);
            results.merge(mob, 1, Integer::sum);
        }
        return results;
    }
}
