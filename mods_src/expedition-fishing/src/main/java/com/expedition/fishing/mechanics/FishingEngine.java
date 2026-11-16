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
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
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

    public record TrackedCreature(
            UUID entityUuid,
            UUID ownerUuid,
            net.minecraft.resources.ResourceKey<Level> dimension,
            long expireGameTime,
            boolean isBoss,
            String mobId,
            String displayName,
            String rarity,
            double weightKg,
            double pendingXp,
            double scavBonus,
            boolean autoSmeltFish
    ) {}

    private static final List<TrackedCreature> TRACKED_CREATURES = new java.util.concurrent.CopyOnWriteArrayList<>();

    public static boolean isTrackedCreature(UUID uuid) {
        for (TrackedCreature tc : TRACKED_CREATURES) {
            if (tc.entityUuid().equals(uuid)) return true;
        }
        return false;
    }

    public static boolean isTrackedPassiveCreature(UUID uuid) {
        for (TrackedCreature tc : TRACKED_CREATURES) {
            if (tc.entityUuid().equals(uuid) && tc.pendingXp() <= 0.0 && !tc.isBoss()) return true;
        }
        return false;
    }

    public static void onSeaCreatureKilled(ServerLevel level, Entity killedEntity, ServerPlayer killer) {
        UUID entId = killedEntity.getUUID();
        BossAbilityEngine.removeController(entId);
        BroodmotherCosmeticsManager.cleanup(level.getServer(), entId);
        TrackedCreature matched = null;
        for (TrackedCreature tc : TRACKED_CREATURES) {
            if (tc.entityUuid().equals(entId)) {
                matched = tc;
                break;
            }
        }
        if (matched == null) return;
        TRACKED_CREATURES.remove(matched);

        // If this was a Boss Rider that still had a live Mount, discard the orphaned Mount
        if (killedEntity.getVehicle() != null && killedEntity.getVehicle().isAlive()) {
            Entity v = killedEntity.getVehicle();
            BossAbilityEngine.removeController(v.getUUID());
            v.discard();
        }

        if (matched.pendingXp() <= 0) return;

        ServerPlayer recipient = level.getServer() != null
                ? level.getServer().getPlayerList().getPlayer(matched.ownerUuid())
                : null;
        if (recipient == null) recipient = killer;
        if (recipient == null) return;

        FishingPlayerData data = PlayerDataStorage.get(recipient.getUUID());
        int oldLv = data.level;
        int gained = data.addXp(matched.pendingXp());

        JsonObject mobCfg = FishingDataRegistry.MOBS.get(matched.mobId());
        if (mobCfg != null) {
            PlayerFishingStats dropStats = new PlayerFishingStats();
            dropStats.scavBonus = matched.scavBonus();
            dropStats.autoSmeltFish = matched.autoSmeltFish();
            grantMobDrops(recipient, mobCfg, dropStats, level.getRandom());
        }

        String color = FishingItemFactory.getRarityColor(matched.rarity());
        recipient.sendSystemMessage(Component.literal(
                "§b⚔ Slain " + color + matched.displayName() + "§7! §3+" + Math.round(matched.pendingXp()) + " Fishing XP"
        ));

        if (gained > 0) {
            onLevelUp(recipient, oldLv, data.level);
        }
        PlayerDataStorage.saveAsync(recipient.getUUID());
        ExpeditionFishingApi.fireCatch(recipient, matched.mobId(), "HOSTILE", matched.rarity(), matched.weightKg(), matched.pendingXp());
    }

    public static void tickDespawnManager(net.minecraft.server.MinecraftServer server) {
        if (TRACKED_CREATURES.isEmpty()) return;
        for (TrackedCreature tc : TRACKED_CREATURES) {
            ServerLevel lvl = server.getLevel(tc.dimension());
            if (lvl == null) {
                TRACKED_CREATURES.remove(tc);
                continue;
            }
            Entity ent = lvl.getEntity(tc.entityUuid());
            if (ent == null || !ent.isAlive()) {
                TRACKED_CREATURES.remove(tc);
                continue;
            }
            if (lvl.getGameTime() >= tc.expireGameTime()) {
                BossAbilityEngine.removeController(tc.entityUuid());
                BroodmotherCosmeticsManager.cleanup(server, tc.entityUuid());
                ent.discard();
                TRACKED_CREATURES.remove(tc);
            }
        }
    }

    private static void registerSpawnedCreature(
            ServerPlayer owner,
            ServerLevel level,
            Entity entity,
            boolean isBoss,
            String mobId,
            String displayName,
            String rarity,
            double weightKg,
            double pendingXp,
            double scavBonus,
            boolean autoSmeltFish
    ) {
        int maxPerPlayer = FishingDataRegistry.getAntiLagInt("max_active_creatures_per_player", 12);
        List<TrackedCreature> ownedNonBoss = new ArrayList<>();
        for (TrackedCreature tc : TRACKED_CREATURES) {
            if (tc.ownerUuid().equals(owner.getUUID()) && !tc.isBoss()) {
                Entity e = level.getEntity(tc.entityUuid());
                if (e != null && e.isAlive()) {
                    ownedNonBoss.add(tc);
                } else {
                    TRACKED_CREATURES.remove(tc);
                }
            }
        }
        while (ownedNonBoss.size() >= maxPerPlayer) {
            TrackedCreature oldest = ownedNonBoss.remove(0);
            Entity oldEnt = level.getEntity(oldest.entityUuid());
            if (oldEnt != null) oldEnt.discard();
            TRACKED_CREATURES.remove(oldest);
        }

        int ttlSeconds = isBoss
                ? FishingDataRegistry.getAntiLagInt("boss_despawn_seconds", 600)
                : FishingDataRegistry.getAntiLagInt("normal_despawn_seconds", 240);
        long expireAt = level.getGameTime() + (ttlSeconds * 20L);
        TRACKED_CREATURES.add(new TrackedCreature(
                entity.getUUID(), owner.getUUID(), level.dimension(), expireAt, isBoss,
                mobId, displayName, rarity, weightKg, pendingXp, scavBonus, autoSmeltFish
        ));
    }

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

        // 4.5 Totem Bonus
        st.treasureChance += FishingCombatAndUtilityHandler.getNearbyTreasureTotemBonus(player, level);
        if (FishingCombatAndUtilityHandler.hasMagmaFilterBuff(player.getUUID(), level.getGameTime())) {
            st.heatReduction += 0.50;
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
        st.fishingSpeed += lure * FishingDataRegistry.getEnchantBalance("lure_speed_per_level", 10.0);

        int lots = enchs.getOrDefault("minecraft:luck_of_the_sea", 0);
        st.treasureChance += lots * FishingDataRegistry.getEnchantBalance("luck_of_the_sea_treasure_per_level", 2.5);
        st.rareCreatureBonus += lots * FishingDataRegistry.getEnchantBalance("luck_of_the_sea_rare_per_level", 1.0);

        int scholar = enchs.getOrDefault("enchantencore:scholar_of_fishing", 0);
        st.xpMultiplier += scholar * FishingDataRegistry.getEnchantBalance("scholar_of_fishing_xp_per_level", 0.15);

        if (enchs.getOrDefault("enchantencore:harpoon", 0) > 0) {
            st.seaCreatureChance += FishingDataRegistry.getEnchantBalance("harpoon_scc_bonus", 4.0);
        }
        if (enchs.getOrDefault("enchantencore:yoink", 0) > 0) {
            st.doubleCatchChance += FishingDataRegistry.getEnchantBalance("yoink_double_catch_bonus", 6.0);
        }

        // 40 New Fishing Enchantments (Configurable via enchants_balance.json)
        st.fishingSpeed += enchs.getOrDefault("expedition_fishing:quick_cast", 0) * FishingDataRegistry.getEnchantBalance("quick_cast_speed_per_level", 5.0);
        st.weightMultiplier += enchs.getOrDefault("expedition_fishing:heavy_sinker", 0) * FishingDataRegistry.getEnchantBalance("heavy_sinker_weight_per_level", 0.03);
        st.baitSaveChance += enchs.getOrDefault("expedition_fishing:bait_saver", 0) * FishingDataRegistry.getEnchantBalance("bait_saver_chance_per_level", 0.15);
        st.seaCreatureChance += enchs.getOrDefault("expedition_fishing:angler_instinct", 0) * FishingDataRegistry.getEnchantBalance("angler_instinct_scc_per_level", 3.0);
        st.scavBonus += enchs.getOrDefault("expedition_fishing:tide_scavenger", 0) * FishingDataRegistry.getEnchantBalance("tide_scavenger_drop_per_level", 0.10);
        if (isRaining) {
            st.fishingSpeed += enchs.getOrDefault("expedition_fishing:steady_hand", 0) * FishingDataRegistry.getEnchantBalance("steady_hand_rain_speed_per_level", 4.0);
        }
        if (isNight) {
            int nf = enchs.getOrDefault("expedition_fishing:night_fisher", 0);
            st.fishingSpeed += nf * FishingDataRegistry.getEnchantBalance("night_fisher_speed_per_level", 5.0);
            st.seaCreatureChance += nf * FishingDataRegistry.getEnchantBalance("night_fisher_scc_per_level", 3.0);
        } else {
            int sc = enchs.getOrDefault("expedition_fishing:sunlit_cast", 0);
            st.fishingSpeed += sc * FishingDataRegistry.getEnchantBalance("sunlit_cast_speed_per_level", 5.0);
            st.treasureChance += sc * FishingDataRegistry.getEnchantBalance("sunlit_cast_treasure_per_level", 3.0);
        }
        if (pos.getY() < 40) {
            int dw = enchs.getOrDefault("expedition_fishing:deep_water", 0);
            st.fishingSpeed += dw * FishingDataRegistry.getEnchantBalance("deep_water_speed_per_level", 5.0);
            st.rareCreatureBonus += dw * FishingDataRegistry.getEnchantBalance("deep_water_rare_per_level", 3.0);
        } else if (pos.getY() >= 62) {
            st.fishingSpeed += enchs.getOrDefault("expedition_fishing:surface_cast", 0) * FishingDataRegistry.getEnchantBalance("surface_cast_speed_per_level", 5.0);
        }
        st.stormCallerLevel = enchs.getOrDefault("expedition_fishing:storm_caller", 0);
        if (isThundering && st.stormCallerLevel > 0) {
            st.seaCreatureChance += st.stormCallerLevel * FishingDataRegistry.getEnchantBalance("storm_caller_scc_per_level", 12.0);
        }
        st.feastLevel = enchs.getOrDefault("expedition_fishing:fishermans_feast", 0);

        // Rare Enchants
        int sccLv = enchs.getOrDefault("expedition_fishing:sea_creature_chance", 0);
        double sccPerLv = FishingDataRegistry.getEnchantBalance("sea_creature_chance_per_level", 4.0);
        st.seaCreatureChance += sccLv * sccPerLv;
        st.rareCreatureBonus += sccLv * sccPerLv;

        st.doubleCatchChance += enchs.getOrDefault("expedition_fishing:double_hook", 0) * FishingDataRegistry.getEnchantBalance("double_hook_per_level", 4.0);
        int thLv = enchs.getOrDefault("expedition_fishing:treasure_hunter", 0);
        st.treasureChance += thLv * FishingDataRegistry.getEnchantBalance("treasure_hunter_treasure_per_level", 3.0);
        st.artifactBonusChance += thLv * FishingDataRegistry.getEnchantBalance("treasure_hunter_artifact_per_level", 1.5);

        st.heatReduction += enchs.getOrDefault("expedition_fishing:thermal_cooling", 0) * FishingDataRegistry.getEnchantBalance("thermal_cooling_reduction_per_level", 0.20);
        int hcLv = enchs.getOrDefault("expedition_fishing:heat_channeling", 0);
        if (inLava && hcLv > 0) {
            st.seaCreatureChance += (int) (data.heat / 20.0) * (hcLv * FishingDataRegistry.getEnchantBalance("heat_channeling_scc_per_20_heat_per_level", 3.0));
        }
        int slayerLv = enchs.getOrDefault("expedition_fishing:slayer_hook", 0);
        if (slayerLv > 0) {
            data.slayerBuffLevel = slayerLv;
        }
        st.biomeWeightBonus += enchs.getOrDefault("expedition_fishing:biome_attunement", 0) * FishingDataRegistry.getEnchantBalance("biome_attunement_weight_per_level", 0.05);
        int trophyLv = enchs.getOrDefault("expedition_fishing:trophy_hunter", 0);
        st.weightMultiplier += trophyLv * FishingDataRegistry.getEnchantBalance("trophy_hunter_weight_per_level", 0.08);
        st.frostbiteLevel = enchs.getOrDefault("expedition_fishing:frostbite_hook", 0);
        if (enchs.getOrDefault("expedition_fishing:blazing_line", 0) > 0) {
            st.autoSmeltFish = true;
            if (inLava) st.seaCreatureChance += FishingDataRegistry.getEnchantBalance("blazing_line_lava_scc_bonus", 5.0);
        }
        st.stunningYankLevel = enchs.getOrDefault("expedition_fishing:stunning_yank", 0);
        st.luckyCloverLevel = enchs.getOrDefault("expedition_fishing:lucky_clover", 0);
        st.chainCatchLevel = enchs.getOrDefault("expedition_fishing:chain_catch", 0);
        st.xpMultiplier += enchs.getOrDefault("expedition_fishing:culling_line", 0) * FishingDataRegistry.getEnchantBalance("culling_line_xp_per_level", 0.15);
        if (inLava) {
            st.rareCreatureBonus += enchs.getOrDefault("expedition_fishing:magma_tether", 0) * FishingDataRegistry.getEnchantBalance("magma_tether_rare_per_level", 4.0);
        }

        // Epic Enchants
        int voidAnchor = enchs.getOrDefault("expedition_fishing:void_anchor", 0);
        if (inVoid || level.dimension() == Level.END) {
            double vaVal = voidAnchor * FishingDataRegistry.getEnchantBalance("void_anchor_scc_per_level", 6.0);
            st.seaCreatureChance += vaVal;
            st.rareCreatureBonus += vaVal;
        }
        if (enchs.getOrDefault("expedition_fishing:abyssal_blessing", 0) > 0) {
            st.artifactBonusChance += FishingDataRegistry.getEnchantBalance("abyssal_blessing_artifact_bonus", 5.0);
        }
        st.bossWeightBonus += enchs.getOrDefault("expedition_fishing:boss_lure", 0) * FishingDataRegistry.getEnchantBalance("boss_lure_weight_per_level", 0.15);
        if (enchs.getOrDefault("expedition_fishing:frenzy_reel", 0) > 0) {
            int stacks = Math.min(5, data.frenzyStacks);
            st.fishingSpeed += stacks * FishingDataRegistry.getEnchantBalance("frenzy_reel_speed_per_stack", 3.0);
            st.seaCreatureChance += stacks * FishingDataRegistry.getEnchantBalance("frenzy_reel_scc_per_stack", 1.5);
        }
        if (enchs.getOrDefault("expedition_fishing:mirror_cast", 0) > 0) {
            st.mirrorCastChance = FishingDataRegistry.getEnchantBalance("mirror_cast_duplicate_chance", 0.12);
        }
        st.chronosBypassChance = enchs.getOrDefault("expedition_fishing:chronos_line", 0) * FishingDataRegistry.getEnchantBalance("chronos_line_bypass_per_level", 0.35);
        if (enchs.getOrDefault("expedition_fishing:dimensional_drift", 0) > 0) {
            st.dimensionalDriftChance = FishingDataRegistry.getEnchantBalance("dimensional_drift_chance", 0.10);
        }
        if (level.getGameTime() < data.bloodBuffUntilTick) {
            int bloodLv = Math.max(1, enchs.getOrDefault("expedition_fishing:blood_in_the_water", 1));
            st.fishingSpeed += bloodLv * FishingDataRegistry.getEnchantBalance("blood_in_the_water_speed_per_level", 6.0);
            st.seaCreatureChance += bloodLv * FishingDataRegistry.getEnchantBalance("blood_in_the_water_scc_per_level", 4.0);
        }

        // Ultimate Enchants
        if (enchs.getOrDefault("expedition_fishing:poseidons_wrath", 0) > 0) {
            st.poseidonsWrath = true;
            st.seaCreatureChance += FishingDataRegistry.getEnchantBalance("poseidons_wrath_scc", 10.0);
            st.fishingSpeed += FishingDataRegistry.getEnchantBalance("poseidons_wrath_speed", 15.0);
            st.doubleCatchChance += FishingDataRegistry.getEnchantBalance("poseidons_wrath_double_catch", 10.0);
        }
        if (enchs.getOrDefault("expedition_fishing:eternal_angler", 0) > 0) {
            st.eternalAngler = true;
            st.xpMultiplier += FishingDataRegistry.getEnchantBalance("eternal_angler_xp_bonus", 0.25);
            st.baitSaveChance += FishingDataRegistry.getEnchantBalance("eternal_angler_bait_save", 0.25);
            st.treasureChance += FishingDataRegistry.getEnchantBalance("eternal_angler_treasure_bonus", 10.0);
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
        boolean isHostileCreature = "HOSTILE".equalsIgnoreCase(category);
        String color = FishingItemFactory.getRarityColor(rarity);
        String recordTag = isRecord ? " §6§l[NEW RECORD!]" : (isHeavyweight ? " §e[Heavyweight]" : "");

        if (isHostileCreature) {
            // Hostile Sea Creatures grant Fishing XP & drops when SLAIN!
            spawnAndLaunchCreature(player, hook, mobId, mobCfg, displayName, rarity, weightKg, xpEarned, stats);
            player.sendSystemMessage(Component.literal(
                    "§3🎣 Hooked " + color + displayName + " §7(" + weightKg + " kg)" + recordTag + " §e— Slay it for Fishing XP & Loot!"
            ));
        } else {
            // Passive fish grant Fishing XP & drops immediately upon catch
            int oldLevel = data.level;
            int gainedLevels = data.addXp(xpEarned);
            spawnAndLaunchCreature(player, hook, mobId, mobCfg, displayName, rarity, weightKg, 0.0, stats);
            grantMobDrops(player, mobCfg, stats, rng);
            player.sendSystemMessage(Component.literal(
                    "§3🎣 Caught " + color + displayName + " §7(" + weightKg + " kg)" + recordTag + " §b+" + Math.round(xpEarned) + " Fishing XP"
            ));
            if (gainedLevels > 0) {
                onLevelUp(player, oldLevel, data.level);
            }
            ExpeditionFishingApi.fireCatch(player, mobId, category, rarity, weightKg, xpEarned);
        }

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

    private static void spawnAndLaunchCreature(ServerPlayer player, FishingHook hook, String mobId, JsonObject mobCfg, String displayName, String rarity, double weightKg, double pendingXp, PlayerFishingStats stats) {
        ServerLevel level = player.level();
        Vec3 hookPos = hook.position().add(0, 0.4, 0);

        // If fishing in The End / over the Void without solid ground beneath the bobber, pull the creature through a Void Rift directly onto safe ground near the player!
        if (level.dimension() == Level.END || !hasSolidGroundBelow(level, hook.blockPosition(), 6)) {
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.REVERSE_PORTAL, hookPos.x, hookPos.y, hookPos.z, 35, 0.4, 0.4, 0.4, 0.15);
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.WITCH, hookPos.x, hookPos.y, hookPos.z, 20, 0.3, 0.3, 0.3, 0.05);
            Vec3 safePos = findSafeLandingPos(level, player);
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.REVERSE_PORTAL, safePos.x, safePos.y + 0.8, safePos.z, 30, 0.4, 0.5, 0.4, 0.1);
            spawnComplexCreature(player, level, safePos, new Vec3(0, 0.18, 0), mobId, mobCfg, displayName, rarity, weightKg, pendingXp, stats);
            return;
        }

        Vec3 dir = player.position().add(0, 1.0, 0).subtract(hookPos);
        Vec3 vel = new Vec3(dir.x * 0.14, Math.min(0.65, Math.max(0.28, dir.y * 0.15 + 0.32)), dir.z * 0.14);
        spawnComplexCreature(player, level, hookPos, vel, mobId, mobCfg, displayName, rarity, weightKg, pendingXp, stats);
    }

    public static boolean hasSolidGroundBelow(ServerLevel level, BlockPos start, int maxDepth) {
        for (int dy = 0; dy <= maxDepth; dy++) {
            BlockPos check = start.below(dy);
            if (!level.getBlockState(check).isAir() && level.getBlockState(check).isSolid()) {
                return true;
            }
        }
        return false;
    }

    public static Vec3 findSafeLandingPos(ServerLevel level, ServerPlayer player) {
        BlockPos pPos = player.blockPosition();
        Vec3 look = player.getLookAngle();
        BlockPos front = BlockPos.containing(player.getX() + look.x * 2.0, player.getY(), player.getZ() + look.z * 2.0);
        if (hasSolidGroundBelow(level, front, 3) && level.getBlockState(front).isAir()) {
            return new Vec3(front.getX() + 0.5, front.getY() + 0.5, front.getZ() + 0.5);
        }
        for (int r = 1; r <= 3; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    BlockPos cand = pPos.offset(dx, 0, dz);
                    if (level.getBlockState(cand.below()).isSolid() && level.getBlockState(cand).isAir()) {
                        return new Vec3(cand.getX() + 0.5, cand.getY() + 0.5, cand.getZ() + 0.5);
                    }
                }
            }
        }
        return player.position().add(0, 0.5, 0);
    }

    private static void spawnComplexCreature(
            ServerPlayer player,
            ServerLevel level,
            Vec3 spawnPos,
            Vec3 vel,
            String mobId,
            JsonObject mobCfg,
            String displayName,
            String rarity,
            double weightKg,
            double pendingXp,
            PlayerFishingStats stats
    ) {
        JsonObject spCfg = mobCfg.has("spawn") && mobCfg.get("spawn").isJsonObject()
                ? mobCfg.getAsJsonObject("spawn")
                : new JsonObject();
        String spType = FishingDataRegistry.getStr(spCfg, "type", "SINGLE").toUpperCase(Locale.ROOT);
        boolean isBoss = "EPIC".equalsIgnoreCase(rarity) || "LEGENDARY".equalsIgnoreCase(rarity);
        double scav = stats != null ? stats.scavBonus : 0.0;
        boolean smelt = stats != null && stats.autoSmeltFish;

        // 1. Handle GROUP spawns (e.g. Pillager Trio/Quartet/Quintet, Wailing Ghast Duo, Piglin Squad)
        if ("GROUP".equals(spType) && spCfg.has("members") && spCfg.get("members").isJsonArray()) {
            double spread = FishingDataRegistry.getDouble(spCfg, "spread", 2.0);
            boolean firstMember = true;
            for (JsonElement el : spCfg.getAsJsonArray("members")) {
                if (!el.isJsonObject()) continue;
                JsonObject mem = el.getAsJsonObject();
                String memTypeStr = FishingDataRegistry.getStr(mem, "entity_type", "PILLAGER");
                int count = Math.max(1, FishingDataRegistry.getInt(mem, "count", 1));
                for (int i = 0; i < count; i++) {
                    double ox = (level.getRandom().nextDouble() - 0.5) * spread;
                    double oz = (level.getRandom().nextDouble() - 0.5) * spread;
                    Vec3 mPos = spawnPos.add(ox, 0, oz);
                    Entity memberEnt = createConfiguredEntity(level, mPos, vel, memTypeStr, displayName, rarity, mem, stats);
                    if (memberEnt != null) {
                        level.addFreshEntity(memberEnt);
                        double xpForThis = firstMember ? pendingXp : 0.0;
                        registerSpawnedCreature(player, level, memberEnt, isBoss, mobId, displayName, rarity, weightKg, xpForThis, scav, smelt);
                        if (memberEnt instanceof LivingEntity mLiv) {
                            BossAbilityEngine.registerController(player, level, mLiv, mobId, displayName, rarity, spCfg, firstMember);
                        }
                        if (mem.has("rider") && mem.get("rider").isJsonObject()) {
                            JsonObject riderCfg = mem.getAsJsonObject("rider");
                            String rType = FishingDataRegistry.getStr(riderCfg, "entity_type", "PILLAGER");
                            Entity riderEnt = createConfiguredEntity(level, mPos, vel, rType, displayName, rarity, riderCfg, stats);
                            if (riderEnt != null) {
                                level.addFreshEntity(riderEnt);
                                registerSpawnedCreature(player, level, riderEnt, isBoss, mobId, displayName, rarity, weightKg, 0.0, 0.0, false);
                                riderEnt.startRiding(memberEnt, true, true);
                            }
                        }
                        firstMember = false;
                    }
                }
            }
            return;
        }

        // 2. Handle SINGLE, SCALED, or MOUNTED spawns (80% HP for Boss Rider, 20% HP for Mount!)
        boolean isMountedSpawn = spCfg.has("mount") && spCfg.get("mount").isJsonObject();
        String mainTypeStr = spCfg.has("rider_entity_type")
                ? FishingDataRegistry.getStr(spCfg, "rider_entity_type", "ZOMBIFIED_PIGLIN")
                : FishingDataRegistry.getStr(mobCfg, "entity_type", "COD");

        Entity entity = createConfiguredEntity(level, spawnPos, vel, mainTypeStr, displayName, rarity, spCfg, stats);
        if (entity == null) return;

        double totalBossHp = 100.0;
        if (entity instanceof LivingEntity living) {
            totalBossHp = living.getMaxHealth();
            if (isMountedSpawn && totalBossHp > 10.0) {
                // 80% of total HP goes to the Boss Rider
                double riderHp = Math.max(10.0, totalBossHp * 0.80);
                AttributeInstance hpAttr = living.getAttribute(Attributes.MAX_HEALTH);
                if (hpAttr != null) {
                    hpAttr.setBaseValue(riderHp);
                    living.setHealth((float) riderHp);
                }
            }
        }

        String glowColor = FishingDataRegistry.getStr(spCfg, "glow_color", FishingDataRegistry.getStr(spCfg, "name_color", ""));
        if (!glowColor.isEmpty()) {
            BroodmotherCosmeticsManager.applyGlowColor(level, entity, glowColor);
        }

        level.addFreshEntity(entity);
        registerSpawnedCreature(player, level, entity, isBoss, mobId, displayName, rarity, weightKg, pendingXp, scav, smelt);
        if (entity instanceof LivingEntity living) {
            BossAbilityEngine.registerController(player, level, living, mobId, displayName, rarity, spCfg, true);
            if ("ghast_broodmother".equals(mobId)) {
                BroodmotherCosmeticsManager.attachBroodmotherTentacles(level, living);
            } else if ("sulfur_cube".equals(mobId)) {
                BroodmotherCosmeticsManager.equipSulfurCubeVariant(living);
            }
        }

        if (isMountedSpawn) {
            JsonObject mountCfg = spCfg.getAsJsonObject("mount");
            String mTypeStr = FishingDataRegistry.getStr(mountCfg, "entity_type", "DOLPHIN");
            String mName = FishingDataRegistry.getStr(mountCfg, "display_name", displayName + "'s Steed");
            Entity mount = createConfiguredEntity(level, spawnPos, vel, mTypeStr, mName, rarity, mountCfg, stats);
            if (mount != null) {
                if (mount instanceof LivingEntity mLiving) {
                    // 20% of total HP goes to the Mount!
                    double mountHp = Math.max(8.0, totalBossHp * 0.20);
                    AttributeInstance mHpAttr = mLiving.getAttribute(Attributes.MAX_HEALTH);
                    if (mHpAttr != null) {
                        mHpAttr.setBaseValue(mountHp);
                        mLiving.setHealth((float) mountHp);
                    }
                }
                if (!FishingDataRegistry.getBool(mountCfg, "show_name", false)) {
                    mount.setCustomNameVisible(false);
                }
                if (!glowColor.isEmpty()) {
                    BroodmotherCosmeticsManager.applyGlowColor(level, mount, glowColor);
                }
                level.addFreshEntity(mount);
                registerSpawnedCreature(player, level, mount, isBoss, mobId, mName, rarity, weightKg, 0.0, 0.0, false);
                if (mount instanceof LivingEntity mLiving) {
                    BossAbilityEngine.registerController(player, level, mLiving, mobId, mName, rarity, mountCfg, false);
                }
                entity.startRiding(mount, true, true);
            }
        }
    }

    private static Entity createConfiguredEntity(
            ServerLevel level,
            Vec3 spawnPos,
            Vec3 vel,
            String entityTypeStr,
            String displayName,
            String rarity,
            JsonObject spCfg,
            PlayerFishingStats stats
    ) {
        String cleanType = entityTypeStr.toLowerCase(Locale.ROOT);
        if ("parched".equals(cleanType)) cleanType = "husk";
        if ("camel_husk".equals(cleanType)) cleanType = "camel";
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.withDefaultNamespace(cleanType));
        if (type == null) type = BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.withDefaultNamespace("cod"));

        Entity entity = type.create(level, EntitySpawnReason.EVENT);
        if (entity == null) return null;

        entity.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
        entity.setCustomName(Component.literal(FishingItemFactory.getRarityColor(rarity) + displayName));
        entity.setCustomNameVisible(true);
        // Prevent external datapacks (Stellarity phantom enlarge, More Mobs, etc.) from overwriting our custom HP/stats!
        entity.addTag("stellarity.phantom.aware");
        entity.addTag("stellarity.aware");
        entity.addTag("mob_manager.settings.exclude");
        entity.addTag("expedition_fishing.mob");
        if (vel != null) {
            entity.setDeltaMovement(vel);
        }

        if (entity instanceof LivingEntity living && spCfg != null) {
            double scale = FishingDataRegistry.getDouble(spCfg, "scale", 1.0);
            if (spCfg.has("slime_size")) {
                scale = Math.max(scale, FishingDataRegistry.getInt(spCfg, "slime_size", 4) * 0.55);
            }
            if (scale != 1.0 && scale > 0.1) {
                AttributeInstance scaleAttr = living.getAttribute(Attributes.SCALE);
                if (scaleAttr != null) scaleAttr.setBaseValue(scale);
            }

            double hpScale = FishingDataRegistry.getMobBalance("global_hp_scale", 1.0);
            double maxBossHp = FishingDataRegistry.getMobBalance("max_boss_hp", 2000.0);
            double explicitHp = FishingDataRegistry.getDouble(spCfg, "health", 0.0);
            double hpMult = FishingDataRegistry.getDouble(spCfg, "health_multiplier", 1.0);
            AttributeInstance hpAttr = living.getAttribute(Attributes.MAX_HEALTH);
            if (hpAttr != null) {
                double baseHp = explicitHp > 0.0 ? explicitHp : (hpAttr.getBaseValue() * hpMult);
                double finalHp = Math.min(maxBossHp, Math.max(4.0, baseHp * hpScale));
                hpAttr.setBaseValue(finalHp);
                living.setHealth((float) finalHp);
            }

            double dmgScale = FishingDataRegistry.getMobBalance("global_damage_scale", 1.0);
            double maxBossDmg = FishingDataRegistry.getMobBalance("max_boss_damage", 50.0);
            double explicitDmg = FishingDataRegistry.getDouble(spCfg, "damage", 0.0);
            double dmgMult = FishingDataRegistry.getDouble(spCfg, "damage_multiplier", 1.0);
            AttributeInstance dmgAttr = living.getAttribute(Attributes.ATTACK_DAMAGE);
            if (dmgAttr != null && (explicitDmg > 0.0 || dmgMult != 1.0)) {
                double baseDmg = explicitDmg > 0.0 ? explicitDmg : (dmgAttr.getBaseValue() * dmgMult);
                dmgAttr.setBaseValue(Math.min(maxBossDmg, Math.max(1.0, baseDmg * dmgScale)));
            }

            if (spCfg.has("equipment") && spCfg.get("equipment").isJsonObject()) {
                JsonObject eq = spCfg.getAsJsonObject("equipment");
                equipSlot(living, EquipmentSlot.HEAD, FishingDataRegistry.getStr(eq, "helmet", ""));
                equipSlot(living, EquipmentSlot.CHEST, FishingDataRegistry.getStr(eq, "chestplate", ""));
                equipSlot(living, EquipmentSlot.LEGS, FishingDataRegistry.getStr(eq, "leggings", ""));
                equipSlot(living, EquipmentSlot.FEET, FishingDataRegistry.getStr(eq, "boots", ""));
                equipSlot(living, EquipmentSlot.MAINHAND, FishingDataRegistry.getStr(eq, "mainhand", ""));
            }

            if (spCfg.has("potion_effects") && spCfg.get("potion_effects").isJsonArray()) {
                for (JsonElement pe : spCfg.getAsJsonArray("potion_effects")) {
                    if (!pe.isJsonObject()) continue;
                    JsonObject pObj = pe.getAsJsonObject();
                    String effType = FishingDataRegistry.getStr(pObj, "type", "").toUpperCase(Locale.ROOT);
                    int amp = FishingDataRegistry.getInt(pObj, "amplifier", 0);
                    switch (effType) {
                        case "GLOWING" -> entity.setGlowingTag(true);
                        case "RESISTANCE" -> living.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 24000, amp, true, false));
                        case "SPEED" -> living.addEffect(new MobEffectInstance(MobEffects.SPEED, 24000, amp, true, false));
                        case "SLOWNESS" -> living.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 24000, amp, true, false));
                        case "FIRE_RESISTANCE" -> living.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 24000, amp, true, false));
                    }
                }
            }

            if (stats != null) {
                if (stats.frostbiteLevel > 0) {
                    living.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, stats.frostbiteLevel * 80, 1));
                }
                if (stats.stunningYankLevel > 0) {
                    living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, stats.stunningYankLevel * 60, 0));
                    living.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, stats.stunningYankLevel * 60, 1));
                }
            }
        }
        return entity;
    }

    private static void equipSlot(LivingEntity entity, EquipmentSlot slot, String spec) {
        if (spec == null || spec.isEmpty()) return;
        ItemStack custom = FishingItemFactory.createCustomItem(spec, 1);
        if (!custom.isEmpty()) {
            entity.setItemSlot(slot, custom);
            return;
        }
        if (spec.startsWith("skull:")) {
            ItemStack head = new ItemStack(Items.PLAYER_HEAD);
            FishingItemFactory.applyHeadTexture(head, spec.substring("skull:".length()));
            entity.setItemSlot(slot, head);
            return;
        }
        String clean = spec;
        Integer dyeRgb = null;
        if (clean.startsWith("dyed:") || clean.startsWith("dyed_trimmed:")) {
            String[] parts = clean.split(":");
            if (parts.length >= 2) clean = parts[1];
            if (parts.length >= 3 && parts[2].startsWith("#")) {
                try {
                    dyeRgb = Integer.parseInt(parts[2].substring(1), 16);
                } catch (Exception ignored) {}
            }
        } else if (clean.startsWith("enchanted:")) {
            String[] parts = clean.split(":");
            if (parts.length >= 2) clean = parts[1];
        }
        Item van = BuiltInRegistries.ITEM.getValue(Identifier.withDefaultNamespace(clean.toLowerCase(Locale.ROOT)));
        if (van != null && van != Items.AIR) {
            ItemStack st = new ItemStack(van);
            if (dyeRgb != null) {
                st.set(DataComponents.DYED_COLOR, new net.minecraft.world.item.component.DyedItemColor(dyeRgb));
            }
            if (spec.startsWith("enchanted:")) {
                st.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
            }
            entity.setItemSlot(slot, st);
        }
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

    private static boolean addEconomyCoins(ServerPlayer player, long amount) {
        if (player == null || amount <= 0 || player.level().getServer() == null) return false;
        try {
            Class<?> ecoCraftClass = Class.forName("com.reazip.economycraft.EconomyCraft");
            java.lang.reflect.Method getMgr = ecoCraftClass.getMethod("getManager", net.minecraft.server.MinecraftServer.class);
            Object manager = getMgr.invoke(null, player.level().getServer());
            if (manager == null) return false;

            Class<?> mutSrcClass = Class.forName("com.reazip.economycraft.api.v1.MutationSource");
            java.lang.reflect.Method ofMethod = mutSrcClass.getMethod("of", String.class);
            Object source = ofMethod.invoke(null, "expedition_fishing:treasure");

            java.lang.reflect.Method addMoney = manager.getClass().getMethod("addMoney", UUID.class, long.class, mutSrcClass, String.class);
            addMoney.invoke(manager, player.getUUID(), amount, source, "Fishing Treasure Catch");
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static void rollTreasureAndArtifact(ServerPlayer player, PlayerFishingStats stats, RandomSource rng) {
        FishingPlayerData data = PlayerDataStorage.get(player.getUUID());
        data.totalCatches++;
        data.treasuresCaught++;

        // Roll Treasure Chest Rarity (boosted by Treasure Hunter enchant)
        double r = rng.nextDouble() * 100.0 - (stats.artifactBonusChance * 1.5);
        String tierName;
        String tierColor;
        int minCoins;
        int maxCoins;
        JsonObject coinCfg = FishingDataRegistry.BALANCE.get("treasure_purse_coins");
        boolean scaleDaily = FishingDataRegistry.getBool(coinCfg, "scale_with_economy_daily", true);
        double daily = scaleDaily ? FishingDataRegistry.getEconomyDailyReward() : 100.0;

        if (r < 2.0) {
            tierName = "LEGENDARY";
            tierColor = "§6§l";
            minCoins = scaleDaily
                    ? (int) Math.max(1, Math.round(daily * FishingDataRegistry.getDouble(coinCfg, "legendary_daily_mult_min", 3.50)))
                    : FishingDataRegistry.getInt(coinCfg, "legendary_min", 350);
            maxCoins = scaleDaily
                    ? (int) Math.max(minCoins, Math.round(daily * FishingDataRegistry.getDouble(coinCfg, "legendary_daily_mult_max", 6.00)))
                    : FishingDataRegistry.getInt(coinCfg, "legendary_max", 600);
        } else if (r < 9.0) {
            tierName = "EPIC";
            tierColor = "§d";
            minCoins = scaleDaily
                    ? (int) Math.max(1, Math.round(daily * FishingDataRegistry.getDouble(coinCfg, "epic_daily_mult_min", 1.50)))
                    : FishingDataRegistry.getInt(coinCfg, "epic_min", 150);
            maxCoins = scaleDaily
                    ? (int) Math.max(minCoins, Math.round(daily * FishingDataRegistry.getDouble(coinCfg, "epic_daily_mult_max", 3.00)))
                    : FishingDataRegistry.getInt(coinCfg, "epic_max", 300);
        } else if (r < 25.0) {
            tierName = "RARE";
            tierColor = "§b";
            minCoins = scaleDaily
                    ? (int) Math.max(1, Math.round(daily * FishingDataRegistry.getDouble(coinCfg, "rare_daily_mult_min", 0.60)))
                    : FishingDataRegistry.getInt(coinCfg, "rare_min", 60);
            maxCoins = scaleDaily
                    ? (int) Math.max(minCoins, Math.round(daily * FishingDataRegistry.getDouble(coinCfg, "rare_daily_mult_max", 1.25)))
                    : FishingDataRegistry.getInt(coinCfg, "rare_max", 125);
        } else if (r < 55.0) {
            tierName = "UNCOMMON";
            tierColor = "§a";
            minCoins = scaleDaily
                    ? (int) Math.max(1, Math.round(daily * FishingDataRegistry.getDouble(coinCfg, "uncommon_daily_mult_min", 0.25)))
                    : FishingDataRegistry.getInt(coinCfg, "uncommon_min", 25);
            maxCoins = scaleDaily
                    ? (int) Math.max(minCoins, Math.round(daily * FishingDataRegistry.getDouble(coinCfg, "uncommon_daily_mult_max", 0.60)))
                    : FishingDataRegistry.getInt(coinCfg, "uncommon_max", 60);
        } else {
            tierName = "COMMON";
            tierColor = "§f";
            minCoins = scaleDaily
                    ? (int) Math.max(1, Math.round(daily * FishingDataRegistry.getDouble(coinCfg, "common_daily_mult_min", 0.10)))
                    : FishingDataRegistry.getInt(coinCfg, "common_min", 10);
            maxCoins = scaleDaily
                    ? (int) Math.max(minCoins, Math.round(daily * FishingDataRegistry.getDouble(coinCfg, "common_daily_mult_max", 0.25)))
                    : FishingDataRegistry.getInt(coinCfg, "common_max", 25);
        }

        double lvScale = 1.0 + (data.level * FishingDataRegistry.getDouble(coinCfg, "level_scaling_per_level", 0.01));
        long baseRoll = minCoins + (maxCoins > minCoins ? rng.nextInt(maxCoins - minCoins + 1) : 0);
        long coins = Math.max(1L, Math.round(baseRoll * lvScale));
        if (stats.luckyCloverLevel > 0 && rng.nextDouble() < (stats.luckyCloverLevel * 0.02)) {
            coins *= 2L;
        }

        addEconomyCoins(player, coins);
        player.sendSystemMessage(Component.literal(
                "§6🎁 Treasure Catch! §8[" + tierColor + tierName + "§8] §7Added §6+" + String.format(Locale.US, "%,d", coins) + " Coins §7to your Purse!"
        ));
        player.level().playSound(null, player.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.9f, 1.25f);

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
                Component artName = artStack.getHoverName().copy();
                giveOrDrop(player, artStack);
                player.sendSystemMessage(Component.literal("§d§l✦ ARTIFACT DISCOVERED! §7You unearthed ").append(artName).append(Component.literal("§7!")));
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

    public static boolean spawnMobAtPlayer(ServerPlayer player, String mobId) {
        JsonObject mobCfg = FishingDataRegistry.MOBS.get(mobId);
        if (mobCfg == null) return false;
        String displayName = FishingDataRegistry.getStr(mobCfg, "display_name", FishingItemFactory.formatId(mobId));
        String rarity = FishingDataRegistry.getStr(mobCfg, "rarity", "COMMON").toUpperCase(Locale.ROOT);
        String category = FishingDataRegistry.getStr(mobCfg, "category", "HOSTILE").toUpperCase(Locale.ROOT);
        double baseXp = FishingDataRegistry.getDouble(mobCfg, "xp", 25.0);
        double baseWeight = Math.max(0.5, FishingDataRegistry.getDouble(mobCfg, "base_weight", 5.0));

        Vec3 look = player.getLookAngle();
        Vec3 pos = player.position().add(look.x * 2.8, 0.5, look.z * 2.8);
        double pendingXp = "HOSTILE".equalsIgnoreCase(category) ? baseXp : 0.0;
        spawnComplexCreature(player, player.level(), pos, Vec3.ZERO, mobId, mobCfg, displayName, rarity, baseWeight, pendingXp, null);
        return true;
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
