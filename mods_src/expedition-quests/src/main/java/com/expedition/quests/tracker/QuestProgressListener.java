package com.expedition.quests.tracker;

import com.expedition.quests.api.ExpeditionQuestsApi;
import com.expedition.quests.config.QuestsConfig;
import com.expedition.quests.data.QuestDataStorage;
import com.expedition.quests.data.QuestPlayerData;
import com.expedition.quests.exploration.ClaimedLocationsRegistry;
import com.expedition.quests.model.QuestEntry;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityCombatEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.camel.Camel;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.monster.Strider;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class QuestProgressListener {

    // Anti-Place-Break: track blocks placed by players (dimension:x:y:z)
    private static final Set<String> PLACED_BLOCKS = ConcurrentHashMap.newKeySet();

    // Anti-AFK Grinder: track last kill cell per player
    private static final Map<UUID, String> LAST_KILL_CELL = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> SAME_CELL_KILL_STREAK = new ConcurrentHashMap<>();

    // Anti-Circle Travel Waypoints: last checkpoint position per player
    private static final Map<UUID, Vec3> LAST_TRAVEL_CHECKPOINT = new ConcurrentHashMap<>();

    public static void registerEvents() {
        // 1. Track block placements to prevent Place-and-Break exploits
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (player instanceof ServerPlayer sp && !world.isClientSide()) {
                ItemStack stack = sp.getItemInHand(hand);
                if (stack.getItem() instanceof BlockItem) {
                    BlockPos placed = hitResult.getBlockPos().relative(hitResult.getDirection());
                    String key = world.dimension().identifier() + ":" + placed.getX() + ":" + placed.getY() + ":" + placed.getZ();
                    if (PLACED_BLOCKS.size() > 50000) PLACED_BLOCKS.clear();
                    PLACED_BLOCKS.add(key);
                }
            }
            return InteractionResult.PASS;
        });

        // 2. Mining & Logging (with Veinminer compatibility & Anti-Place-Break)
        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
            if (!(player instanceof ServerPlayer sp) || world.isClientSide()) return;
            if (!ExpeditionQuestsApi.isEnabledForPlayer(sp)) return;

            String key = world.dimension().identifier() + ":" + pos.getX() + ":" + pos.getY() + ":" + pos.getZ();
            if (PLACED_BLOCKS.remove(key)) {
                return; // Player-placed block: ignore!
            }

            String blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
            // Normalize deepslate ore variants to match base ore quest IDs
            String normalized = blockId.replace("minecraft:deepslate_", "minecraft:");

            ExpeditionQuestsApi.advanceMatchingQuests(sp, "MINING",
                    q -> q.subType.equals(blockId) || q.subType.equals(normalized), 1);
            ExpeditionQuestsApi.advanceMatchingQuests(sp, "LOGGING",
                    q -> q.subType.equals(blockId), 1);
        });

        // 3. Slayer (with Anti-AFK Grinder Filter)
        ServerEntityCombatEvents.AFTER_KILLED_OTHER_ENTITY.register((world, entity, killedEntity, damageSource) -> {
            if (!(entity instanceof ServerPlayer sp)) return;
            if (!ExpeditionQuestsApi.isEnabledForPlayer(sp)) return;

            String mobId = BuiltInRegistries.ENTITY_TYPE.getKey(killedEntity.getType()).toString();
            UUID uuid = sp.getUUID();
            String cell = world.dimension().identifier() + ":" + (killedEntity.getBlockX() / 6) + ":" + (killedEntity.getBlockZ() / 6);

            if (cell.equals(LAST_KILL_CELL.get(uuid))) {
                int streak = SAME_CELL_KILL_STREAK.getOrDefault(uuid, 0) + 1;
                SAME_CELL_KILL_STREAK.put(uuid, streak);
                if (streak > 8) {
                    if (streak == 9) {
                        sp.sendSystemMessage(Component.literal("§e⚠ Please move away from stationary mob grinders to continue Slayer Quest progress."));
                    }
                    return;
                }
            } else {
                LAST_KILL_CELL.put(uuid, cell);
                SAME_CELL_KILL_STREAK.put(uuid, 1);
            }

            ExpeditionQuestsApi.advanceMatchingQuests(sp, "SLAYER", q -> q.subType.equals(mobId), 1);
        });
    }

    public static void tickServer(MinecraftServer server, int tickCounter) {
        // Every 20 ticks (1 second): check Exploration arrival, Pathfinder Speed, Travel Waypoints, Visited Grid
        if (tickCounter % 20 != 0) return;

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!ExpeditionQuestsApi.isEnabledForPlayer(player) || player.isSpectator()) continue;

            // Record visited 64x64 grid cell
            ClaimedLocationsRegistry.recordPlayerPosition(player);

            QuestPlayerData data = QuestDataStorage.get(player.getUUID());
            String playerDim = player.level().dimension().identifier().toString();
            boolean hasActiveExplore = false;

            int pathfinderLv = ExpeditionQuestsApi.getItemEnchantLevel(player.getItemBySlot(EquipmentSlot.FEET), "expedition_quests:pathfinder");
            int extraRadius = pathfinderLv * (int) QuestsConfig.getEnchantVal("pathfinder_capture_radius_bonus_per_level", 16);

            // Check Exploration Quests
            for (QuestEntry q : data.activeSlots.values()) {
                if (q == null || q.completed || !q.isExploration() || !q.located) continue;
                hasActiveExplore = true;
                if (!playerDim.equals(q.dimension)) continue;

                int baseRadius = QuestsConfig.getExplorationCaptureRadius(q.category);
                int totalRadius = baseRadius + extraRadius;
                double distSq = Math.pow(player.getX() - q.targetX, 2) + Math.pow(player.getZ() - q.targetZ, 2);
                if (distSq <= (double) totalRadius * totalRadius) {
                    ExpeditionQuestsApi.completeQuest(player, data, q);
                }
            }

            // Pathfinder Boots speed buff while exploring
            if (hasActiveExplore && pathfinderLv > 0) {
                player.addEffect(new MobEffectInstance(MobEffects.SPEED, 45, pathfinderLv - 1, true, false));
            }

            // Travel Waypoint Tracking (Boat, Land Mount, Lava Strider, Elytra)
            tickTravelProgress(player);
        }

        if (tickCounter % 200 == 0) {
            ClaimedLocationsRegistry.saveIfDirty();
        }
    }

    private static void tickTravelProgress(ServerPlayer player) {
        UUID uuid = player.getUUID();
        Vec3 cur = player.position();
        Vec3 prev = LAST_TRAVEL_CHECKPOINT.get(uuid);
        if (prev == null) {
            LAST_TRAVEL_CHECKPOINT.put(uuid, cur);
            return;
        }

        double dist = Math.hypot(cur.x - prev.x, cur.z - prev.z);
        int minInterval = QuestsConfig.getWaypointCheckpointBlocks();
        if (dist < minInterval) return;

        // Cap unrealistic teleport jumps (> 120 blocks/sec)
        LAST_TRAVEL_CHECKPOINT.put(uuid, cur);
        if (dist > 120.0) return;

        int blocksMoved = (int) Math.round(dist);
        Entity vehicle = player.getVehicle();

        if (player.isFallFlying()) {
            ExpeditionQuestsApi.advanceMatchingQuests(player, "ACTION", q -> "ELYTRA".equals(q.subType), blocksMoved);
        } else if (vehicle instanceof AbstractBoat) {
            ExpeditionQuestsApi.advanceMatchingQuests(player, "ACTION", q -> "BOAT".equals(q.subType), blocksMoved);
        } else if (vehicle instanceof Strider) {
            ExpeditionQuestsApi.advanceMatchingQuests(player, "ACTION", q -> "LAVA_STRIDER".equals(q.subType), blocksMoved);
        } else if (vehicle instanceof AbstractHorse || vehicle instanceof Camel) {
            ExpeditionQuestsApi.advanceMatchingQuests(player, "ACTION", q -> "LAND_MOUNT".equals(q.subType), blocksMoved);
        }
    }
}
