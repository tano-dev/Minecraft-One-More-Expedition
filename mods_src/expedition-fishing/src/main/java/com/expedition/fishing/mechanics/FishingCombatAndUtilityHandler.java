package com.expedition.fishing.mechanics;

import com.expedition.fishing.biome.BiomeResolver;
import com.expedition.fishing.config.FishingDataRegistry;
import com.expedition.fishing.data.FishingPlayerData;
import com.expedition.fishing.data.PlayerDataStorage;
import com.expedition.fishing.gui.FishingGui;
import com.expedition.fishing.item.FishingItemFactory;
import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class FishingCombatAndUtilityHandler {

    public record ActiveTotem(
            net.minecraft.resources.ResourceKey<Level> dimension,
            Vec3 pos,
            String totemId,
            long expireTick
    ) {}

    private static final List<ActiveTotem> ACTIVE_TOTEMS = new java.util.concurrent.CopyOnWriteArrayList<>();
    private static final Map<UUID, Long> MAGMA_FILTER_BUFF_UNTIL = new ConcurrentHashMap<>();

    public static void registerEvents() {
        // 1. Right-Click Utility Items (Frost Salve, Magma Filter, Fishing Journal, Totems)
        UseItemCallback.EVENT.register((player, world, hand) -> {
            if (!(player instanceof ServerPlayer sp) || !(world instanceof ServerLevel sLevel)) {
                return InteractionResult.PASS;
            }
            if (BiomeResolver.isSkyblockWorld(sLevel)) return InteractionResult.PASS;

            ItemStack stack = sp.getItemInHand(hand);
            String customId = FishingItemFactory.getCustomItemId(stack);
            if (customId == null) return InteractionResult.PASS;

            // Block any accidental interaction with passive Artifacts
            if (FishingDataRegistry.ARTIFACTS.containsKey(customId)) {
                return InteractionResult.FAIL;
            }

            FishingPlayerData data = PlayerDataStorage.get(sp.getUUID());

            switch (customId) {
                case "frost_salve" -> {
                    data.addHeat(-25.0);
                    stack.shrink(1);
                    sLevel.sendParticles(ParticleTypes.SNOWFLAKE, sp.getX(), sp.getY() + 1.0, sp.getZ(), 25, 0.5, 0.5, 0.5, 0.05);
                    sLevel.playSound(null, sp.blockPosition(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.9f, 1.2f);
                    sp.sendSystemMessage(Component.literal("§b❄ Used Frost Salve! §7Heat reduced by §a-25 §7(Current: §6" + Math.round(data.heat) + "/100§7)."));
                    PlayerDataStorage.saveAsync(sp.getUUID());
                    return InteractionResult.SUCCESS;
                }
                case "magma_filter" -> {
                    data.addHeat(-50.0);
                    MAGMA_FILTER_BUFF_UNTIL.put(sp.getUUID(), sLevel.getGameTime() + 6000L); // 300s
                    stack.shrink(1);
                    sLevel.sendParticles(ParticleTypes.SNOWFLAKE, sp.getX(), sp.getY() + 1.0, sp.getZ(), 35, 0.6, 0.6, 0.6, 0.08);
                    sLevel.playSound(null, sp.blockPosition(), SoundEvents.BREWING_STAND_BREW, SoundSource.PLAYERS, 1.0f, 1.1f);
                    sp.sendSystemMessage(Component.literal("§6🧪 Used Magma Filter! §7Heat reduced by §a-50 §7& granted §e300s Heat Resistance§7!"));
                    PlayerDataStorage.saveAsync(sp.getUUID());
                    return InteractionResult.SUCCESS;
                }
                case "fishing_journal" -> {
                    FishingGui.open(sp, FishingGui.PageType.MAIN, 0);
                    return InteractionResult.SUCCESS;
                }
                case "treasure_totem", "battle_totem_1", "battle_totem_2", "battle_totem_3" -> {
                    ACTIVE_TOTEMS.add(new ActiveTotem(sLevel.dimension(), sp.position(), customId, sLevel.getGameTime() + 3600L)); // 3 minutes
                    stack.shrink(1);
                    sLevel.sendParticles(ParticleTypes.END_ROD, sp.getX(), sp.getY() + 1.2, sp.getZ(), 35, 0.6, 0.8, 0.6, 0.1);
                    sLevel.playSound(null, sp.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0f, 1.2f);
                    sp.sendSystemMessage(Component.literal("§6§l✦ TOTEM DEPLOYED! §e" + FishingItemFactory.formatId(customId) + " §7is active for 3 minutes within 10 blocks!"));
                    return InteractionResult.SUCCESS;
                }
            }
            return InteractionResult.PASS;
        });

        // 2. Right-Click Entity with Fish Bucket
        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (!(player instanceof ServerPlayer sp) || !(world instanceof ServerLevel sLevel)) {
                return InteractionResult.PASS;
            }
            if (BiomeResolver.isSkyblockWorld(sLevel)) return InteractionResult.PASS;

            ItemStack stack = sp.getItemInHand(hand);
            if ("fish_bucket".equals(FishingItemFactory.getCustomItemId(stack)) && entity instanceof LivingEntity liv) {
                if (FishingEngine.isTrackedPassiveCreature(liv.getUUID())) {
                    liv.discard();
                    sLevel.playSound(null, sp.blockPosition(), SoundEvents.BUCKET_FILL_FISH, SoundSource.PLAYERS, 1.0f, 1.0f);
                    FishingEngine.giveOrDrop(sp, new ItemStack(Items.TROPICAL_FISH_BUCKET));
                    sp.sendSystemMessage(Component.literal("§b🪣 Scooped up passive catch into a Fish Bucket!"));
                    return InteractionResult.SUCCESS;
                }
            }
            return InteractionResult.PASS;
        });

        // 3. Sea Creature Attack (SCA) Bonus & Special Trident/Weapon AoE VFX
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (!(player instanceof ServerPlayer sp) || !(world instanceof ServerLevel sLevel) || !(entity instanceof LivingEntity target)) {
                return InteractionResult.PASS;
            }
            if (BiomeResolver.isSkyblockWorld(sLevel)) return InteractionResult.PASS;
            if (!FishingEngine.isTrackedCreature(target.getUUID())) return InteractionResult.PASS;

            double scaPercent = computePlayerScaBonus(sp, sLevel);
            ItemStack mainHand = sp.getMainHandItem();
            String weaponId = FishingItemFactory.getCustomItemId(mainHand);

            // Apply bonus Sea Creature Damage
            if (scaPercent > 0.0) {
                float bonusDmg = (float) Math.min(35.0, 6.0 * (scaPercent / 100.0));
                if (bonusDmg >= 0.5f) {
                    target.hurtServer(sLevel, sLevel.damageSources().playerAttack(sp), bonusDmg);
                }
            }

            // Trigger Trident / Weapon AoE Specials (Tidesplitter, Dreadspear, Poseidon's Trident, Hephaestean Trident)
            if (weaponId != null) {
                triggerWeaponSpecial(sp, sLevel, target, weaponId);
            }

            return InteractionResult.PASS;
        });

        // 4. Prevent placing Artifacts on blocks
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            ItemStack stack = player.getItemInHand(hand);
            String customId = FishingItemFactory.getCustomItemId(stack);
            if (customId != null && FishingDataRegistry.ARTIFACTS.containsKey(customId)) {
                return InteractionResult.FAIL;
            }
            return InteractionResult.PASS;
        });

        // 5. Abyssal Protection: reduce damage taken from Sea Creatures
        ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDamageTaken, damageTaken, blocked) -> {
            if (!(entity instanceof ServerPlayer sp) || !sp.isAlive() || damageTaken <= 0.0f) return;
            Entity attacker = source.getEntity();
            if (attacker == null || !FishingEngine.isTrackedCreature(attacker.getUUID())) return;

            int totalProtLv = 0;
            EquipmentSlot[] armorSlots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
            for (EquipmentSlot slot : armorSlots) {
                totalProtLv += FishingEngine.getRodEnchants(sp.getItemBySlot(slot))
                        .getOrDefault("expedition_fishing:abyssal_protection", 0);
            }
            if (totalProtLv > 0) {
                double perLv = FishingDataRegistry.getEnchantBalance("abyssal_protection_reduction_per_level", 0.08);
                double reduction = Math.min(0.60, totalProtLv * perLv);
                float healBack = (float) (damageTaken * reduction);
                if (healBack > 0.05f) {
                    sp.heal(healBack);
                }
            }
        });
    }

    private static double computePlayerScaBonus(ServerPlayer player, ServerLevel level) {
        double sca = 0.0;
        ItemStack main = player.getMainHandItem();
        sca += getItemSca(main);

        EquipmentSlot[] armor = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
        for (EquipmentSlot slot : armor) {
            sca += getItemSca(player.getItemBySlot(slot));
        }

        Map<String, Integer> enchs = FishingEngine.getRodEnchants(main);
        if (enchs.getOrDefault("enchantencore:harpoon", 0) > 0) sca += 15.0;
        sca += enchs.getOrDefault("expedition_fishing:slayer_hook", 0) * 15.0;
        int abyssalSlayerLv = enchs.getOrDefault("expedition_fishing:abyssal_slayer", 0);
        if (abyssalSlayerLv > 0) {
            double dmgPerLv = FishingDataRegistry.getEnchantBalance("abyssal_slayer_damage_per_level", 1.5);
            sca += abyssalSlayerLv * dmgPerLv * 15.0;
        }

        // Nearby Battle Totems
        for (ActiveTotem t : ACTIVE_TOTEMS) {
            if (t.dimension().equals(level.dimension()) && t.pos().distanceToSqr(player.position()) <= 100.0) {
                switch (t.totemId()) {
                    case "battle_totem_1" -> sca += 15.0;
                    case "battle_totem_2" -> sca += 30.0;
                    case "battle_totem_3" -> sca += 45.0;
                }
            }
        }
        return sca;
    }

    private static double getItemSca(ItemStack stack) {
        String id = FishingItemFactory.getCustomItemId(stack);
        if (id == null) return 0.0;
        JsonObject cfg = FishingDataRegistry.ITEMS.get(id);
        if (cfg == null) return 0.0;
        double direct = FishingDataRegistry.getDouble(cfg, "sea_creature_attack", 0.0);
        if (direct > 0) return direct;
        if (cfg.has("pdc") && cfg.get("pdc").isJsonObject()) {
            return FishingDataRegistry.getDouble(cfg.getAsJsonObject("pdc"), "sea_creature_attack", 0.0);
        }
        if (cfg.has("extra_pdc") && cfg.get("extra_pdc").isJsonObject()) {
            return FishingDataRegistry.getDouble(cfg.getAsJsonObject("extra_pdc"), "sea_creature_attack", 0.0);
        }
        return 0.0;
    }

    private static void triggerWeaponSpecial(ServerPlayer player, ServerLevel level, LivingEntity target, String weaponId) {
        Vec3 tPos = target.position();
        switch (weaponId) {
            case "trident_2" -> {
                // Tidesplitter Spiral
                level.sendParticles(ParticleTypes.SPLASH, tPos.x, tPos.y + 1.0, tPos.z, 25, 0.8, 0.8, 0.8, 0.15);
                target.hurtServer(level, level.damageSources().playerAttack(player), 4.0f);
            }
            case "trident_3" -> {
                // Dreadspear Vortex
                level.sendParticles(ParticleTypes.SCULK_SOUL, tPos.x, tPos.y + 1.0, tPos.z, 20, 0.7, 0.7, 0.7, 0.1);
                target.hurtServer(level, level.damageSources().playerAttack(player), 5.0f);
            }
            case "poseidons_trident", "hephaestean_trident" -> {
                // Encapsulate & Implode AoE
                level.sendParticles( "hephaestean_trident".equals(weaponId) ? ParticleTypes.FLAME : ParticleTypes.BUBBLE_POP,
                        tPos.x, tPos.y + 1.0, tPos.z, 35, 1.1, 1.0, 1.1, 0.12);
                level.playSound(null, target.blockPosition(), SoundEvents.TRIDENT_RETURN, SoundSource.PLAYERS, 0.9f, 1.2f);
                target.hurtServer(level, level.damageSources().playerAttack(player), "hephaestean_trident".equals(weaponId) ? 8.0f : 6.0f);
            }
        }
    }

    public static boolean hasMagmaFilterBuff(UUID playerUuid, long gameTime) {
        return gameTime < MAGMA_FILTER_BUFF_UNTIL.getOrDefault(playerUuid, 0L);
    }

    public static double getNearbyTreasureTotemBonus(ServerPlayer player, ServerLevel level) {
        double bonus = 0.0;
        for (ActiveTotem t : ACTIVE_TOTEMS) {
            if ("treasure_totem".equals(t.totemId()) && t.dimension().equals(level.dimension())
                    && t.pos().distanceToSqr(player.position()) <= 100.0) {
                bonus += 5.0;
            }
        }
        return bonus;
    }

    public static void tickTotemsAndAccessories(MinecraftServer server) {
        // 1. Tick active totems
        for (ActiveTotem t : ACTIVE_TOTEMS) {
            ServerLevel lvl = server.getLevel(t.dimension());
            if (lvl == null || lvl.getGameTime() >= t.expireTick()) {
                ACTIVE_TOTEMS.remove(t);
                continue;
            }
            if (lvl.getGameTime() % 10 == 0) {
                lvl.sendParticles("treasure_totem".equals(t.totemId()) ? ParticleTypes.HAPPY_VILLAGER : ParticleTypes.FLAME,
                        t.pos().x, t.pos().y + 0.8, t.pos().z, 6, 0.5, 0.5, 0.5, 0.02);
            }
        }

        // 2. Check Lava Ring / Eruption Ring & Full Set Armor Footstep Trails every 10 ticks
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            ServerLevel lvl = player.level();
            if (BiomeResolver.isSkyblockWorld(lvl) || lvl.getGameTime() % 10 != 0) continue;

            if (hasRingInInventory(player)) {
                player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 80, 0, true, false));
            }

            String chestId = FishingItemFactory.getCustomItemId(player.getItemBySlot(EquipmentSlot.CHEST));
            if (chestId != null && player.getDeltaMovement().horizontalDistanceSqr() > 0.003) {
                if (chestId.startsWith("dreadplate_")) {
                    lvl.sendParticles(ParticleTypes.CRIMSON_SPORE, player.getX(), player.getY() + 0.1, player.getZ(), 2, 0.2, 0.05, 0.2, 0.01);
                } else if (chestId.startsWith("volcanic_")) {
                    lvl.sendParticles(ParticleTypes.FLAME, player.getX(), player.getY() + 0.1, player.getZ(), 2, 0.2, 0.05, 0.2, 0.01);
                } else if (chestId.startsWith("voidweaver_")) {
                    lvl.sendParticles(ParticleTypes.REVERSE_PORTAL, player.getX(), player.getY() + 0.1, player.getZ(), 2, 0.2, 0.05, 0.2, 0.01);
                }
            }
        }
    }

    private static boolean hasRingInInventory(ServerPlayer player) {
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            String id = FishingItemFactory.getCustomItemId(inv.getItem(i));
            if ("lava_ring".equals(id) || "eruption_ring".equals(id)) {
                return true;
            }
        }
        return false;
    }
}
