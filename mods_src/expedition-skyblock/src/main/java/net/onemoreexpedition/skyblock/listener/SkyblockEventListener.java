package net.onemoreexpedition.skyblock.listener;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.onemoreexpedition.skyblock.ExpeditionSkyblockMod;
import net.onemoreexpedition.skyblock.config.SkyblockConfig;
import net.onemoreexpedition.skyblock.data.PlayerReturnLocation;
import net.onemoreexpedition.skyblock.data.SkyblockDataManager;
import net.onemoreexpedition.skyblock.data.SkyblockIsland;
import net.onemoreexpedition.skyblock.island.IslandMath;

public final class SkyblockEventListener {
    private static final Map<UUID, Long> LAST_DAMAGE_TIMESTAMPS = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> LAST_PVP_TIMESTAMPS = new ConcurrentHashMap<>();

    private SkyblockEventListener() {}

    public static long getLastDamageTimestamp(UUID uuid) {
        return LAST_DAMAGE_TIMESTAMPS.getOrDefault(uuid, 0L);
    }

    public static long getLastPvpTimestamp(UUID uuid) {
        return LAST_PVP_TIMESTAMPS.getOrDefault(uuid, 0L);
    }

    public static int getRemainingPvpSeconds(UUID uuid) {
        long last = getLastPvpTimestamp(uuid);
        if (last <= 0L) return 0;
        long elapsedMillis = System.currentTimeMillis() - last;
        long totalMillis = SkyblockConfig.get().pvpCombatCooldownSeconds * 1000L;
        if (elapsedMillis >= totalMillis) return 0;
        return (int) Math.ceil((totalMillis - elapsedMillis) / 1000.0);
    }

    public static boolean isInPvpCombat(UUID uuid) {
        return getRemainingPvpSeconds(uuid) > 0;
    }

    public static void register() {
        // Track damage timestamps for combat cooldown check
        ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDamage, damageTaken, blocked) -> {
            if (entity instanceof ServerPlayer player) {
                long now = System.currentTimeMillis();
                LAST_DAMAGE_TIMESTAMPS.put(player.getUUID(), now);

                Entity attacker = source.getEntity();
                Entity direct = source.getDirectEntity();
                boolean isPvp = (attacker instanceof Player && !attacker.equals(player))
                             || (direct instanceof Player && !direct.equals(player));

                if (isPvp) {
                    LAST_PVP_TIMESTAMPS.put(player.getUUID(), now);
                    if (attacker instanceof ServerPlayer serverAttacker && !serverAttacker.equals(player)) {
                        LAST_PVP_TIMESTAMPS.put(serverAttacker.getUUID(), now);
                    }
                }
            }
        });

        // 1. Anti-Grief: Block Breaking
        PlayerBlockBreakEvents.BEFORE.register((level, player, pos, state, blockEntity) -> {
            if (!(player instanceof ServerPlayer serverPlayer)) return true;
            if (!canPlayerInteract(serverPlayer, pos)) {
                serverPlayer.sendSystemMessage(Component.literal("§cYou cannot break blocks on this island!"));
                return false;
            }
            return true;
        });

        // 2. Anti-Grief: Block Placing & Container/Block Use
        UseBlockCallback.EVENT.register((player, level, hand, hitResult) -> {
            if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.PASS;
            if (!level.dimension().equals(ExpeditionSkyblockMod.SKYBLOCK_DIMENSION)) return InteractionResult.PASS;

            BlockPos pos = hitResult.getBlockPos();

            // End portal eye insertion suppression
            ItemStack held = player.getItemInHand(hand);
            if (held.is(Items.ENDER_EYE) && level.getBlockState(pos).is(net.minecraft.world.level.block.Blocks.END_PORTAL_FRAME)) {
                serverPlayer.sendSystemMessage(Component.literal("§cEnd portals cannot be activated in Skyblock!"));
                return InteractionResult.FAIL;
            }

            if (!canPlayerInteract(serverPlayer, pos)) {
                serverPlayer.sendSystemMessage(Component.literal("§cYou cannot interact with blocks or containers on this island!"));
                return InteractionResult.FAIL;
            }

            // Also check placement target offset
            BlockPos placePos = pos.relative(hitResult.getDirection());
            if (!canPlayerInteract(serverPlayer, placePos)) {
                serverPlayer.sendSystemMessage(Component.literal("§cYou cannot place blocks outside island bounds!"));
                return InteractionResult.FAIL;
            }

            return InteractionResult.PASS;
        });

        // 3. Anti-Grief: Entity Interaction (item frames, armor stands, minecarts, animals)
        UseEntityCallback.EVENT.register((player, level, hand, entity, hitResult) -> {
            if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.PASS;
            if (!level.dimension().equals(ExpeditionSkyblockMod.SKYBLOCK_DIMENSION)) return InteractionResult.PASS;

            if (!canPlayerInteract(serverPlayer, entity.blockPosition())) {
                serverPlayer.sendSystemMessage(Component.literal("§cYou cannot interact with entities on this island!"));
                return InteractionResult.FAIL;
            }
            return InteractionResult.PASS;
        });

        // 4. Anti-Grief & PvP: Direct Attack
        AttackEntityCallback.EVENT.register((player, level, hand, entity, hitResult) -> {
            if (!(player instanceof ServerPlayer attacker)) return InteractionResult.PASS;
            if (!level.dimension().equals(ExpeditionSkyblockMod.SKYBLOCK_DIMENSION)) return InteractionResult.PASS;

            // Strict PvP cancellation in Skyblock
            if (entity instanceof Player) {
                attacker.sendSystemMessage(Component.literal("§cPvP is disabled in Skyblock!"));
                return InteractionResult.FAIL;
            }

            // Anti-grief: cannot harm non-player entities on visited island
            if (!canPlayerInteract(attacker, entity.blockPosition())) {
                attacker.sendSystemMessage(Component.literal("§cYou cannot attack entities on this island!"));
                return InteractionResult.FAIL;
            }

            return InteractionResult.PASS;
        });

        // 5. Indirect Damage & Void Damage Immunity Failsafe
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (!entity.level().dimension().equals(ExpeditionSkyblockMod.SKYBLOCK_DIMENSION)) return true;

            // Failsafe void damage cancellation
            if (source.is(DamageTypes.FELL_OUT_OF_WORLD) && entity instanceof ServerPlayer) {
                return false;
            }

            // PvP damage cancellation (projectiles, TNT, splash potions)
            if (entity instanceof Player) {
                Entity attacker = source.getEntity();
                Entity direct = source.getDirectEntity();
                if (attacker instanceof Player || direct instanceof Player) {
                    return false;
                }
            } else {
                // Non-player entity harmed by player on visited island
                Entity attacker = source.getEntity();
                if (attacker instanceof ServerPlayer serverPlayer) {
                    if (!canPlayerInteract(serverPlayer, entity.blockPosition())) {
                        return false;
                    }
                }
            }

            return true;
        });

        // 6. Per-tick Void Fall Rescue & Horizontal Boundary Clamping
        ServerTickEvents.END_LEVEL_TICK.register(level -> {
            if (!level.dimension().equals(ExpeditionSkyblockMod.SKYBLOCK_DIMENSION)) return;

            SkyblockDataManager dataManager = SkyblockDataManager.getInstance();
            if (dataManager == null) return;

            for (ServerPlayer player : level.players()) {
                // Void Rescue (< -64.0)
                if (player.getY() < -64.0) {
                    rescueVoidFall(player, level, dataManager);
                    continue;
                }

                // Horizontal boundary containment
                handleBoundaryContainment(player, level, dataManager);
            }
        });

        // Lifecycle: Save on server stop
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            SkyblockDataManager dm = SkyblockDataManager.getInstance();
            if (dm != null) {
                dm.save();
            }
        });
    }

    public static boolean canPlayerInteract(ServerPlayer player, BlockPos pos) {
        if (!player.level().dimension().equals(ExpeditionSkyblockMod.SKYBLOCK_DIMENSION)) {
            return true;
        }

        MinecraftServer server = player.level().getServer();
        if (server != null && server.getPlayerList().isOp(new net.minecraft.server.players.NameAndId(player.getGameProfile()))) {
            return true; // Server Operators have bypass
        }

        IslandMath.WorldPos center = IslandMath.getNearestIslandCenter(pos.getX(), pos.getZ());

        // Check if position is within buildable 256x256 island bounds
        if (!IslandMath.isWithinIslandBounds(pos.getX(), pos.getZ(), center.x(), center.z())) {
            return false; // Void buffer zone is strictly unmodifiable
        }

        SkyblockDataManager dataManager = SkyblockDataManager.getInstance();
        if (dataManager == null) return false;

        SkyblockIsland island = dataManager.getIslandByPosition(pos.getX(), pos.getZ());
        if (island == null) {
            return false;
        }

        return island.isOwner(player.getUUID());
    }

    private static void rescueVoidFall(ServerPlayer player, ServerLevel level, SkyblockDataManager dataManager) {
        SkyblockIsland island = dataManager.getIslandByOwner(player.getUUID());

        double targetX, targetY, targetZ;
        float yaw = 0f, pitch = 0f;

        if (island != null) {
            targetX = island.getHomeX();
            targetY = island.getHomeY();
            targetZ = island.getHomeZ();
            yaw = island.getHomeYaw();
            pitch = island.getHomePitch();
        } else {
            // Player is a visitor who fell into the void without owning an island
            PlayerReturnLocation returnLoc = dataManager.getReturnLocation(player.getUUID());
            if (returnLoc != null) {
                targetX = returnLoc.getX();
                targetY = returnLoc.getY();
                targetZ = returnLoc.getZ();
                yaw = returnLoc.getYaw();
                pitch = returnLoc.getPitch();
            } else {
                targetX = 0.5;
                targetY = 65.0;
                targetZ = 0.5;
            }
        }

        player.setDeltaMovement(Vec3.ZERO);
        player.resetFallDistance();
        player.teleportTo(
                level,
                targetX,
                targetY,
                targetZ,
                Collections.emptySet(),
                yaw,
                pitch,
                true // resets fall distance
        );
        player.setDeltaMovement(Vec3.ZERO);
        player.resetFallDistance();
        player.sendSystemMessage(Component.literal("§6[Skyblock] §eSaved from the void! Teleported back safely."));
    }

    private static void handleBoundaryContainment(ServerPlayer player, ServerLevel level, SkyblockDataManager dataManager) {
        IslandMath.WorldPos center = IslandMath.getNearestIslandCenter(player.getBlockX(), player.getBlockZ());
        double dx = Math.abs(player.getX() - center.x());
        double dz = Math.abs(player.getZ() - center.z());

        double maxRadius = IslandMath.ISLAND_RADIUS; // 128.0 blocks

        if (dx > maxRadius || dz > maxRadius) {
            // Hard penetration rescue (> 135.0 blocks)
            if (dx > maxRadius + 7.0 || dz > maxRadius + 7.0) {
                rescueVoidFall(player, level, dataManager);
                player.sendSystemMessage(Component.literal("§cYou strayed too far into the void buffer and were returned to your home!"));
                return;
            }

            // Soft clamp at boundary (127.5 blocks)
            double clampedX = player.getX();
            double clampedZ = player.getZ();

            if (dx > maxRadius) {
                clampedX = center.x() + (player.getX() > center.x() ? (maxRadius - 0.5) : -(maxRadius - 0.5));
            }
            if (dz > maxRadius) {
                clampedZ = center.z() + (player.getZ() > center.z() ? (maxRadius - 0.5) : -(maxRadius - 0.5));
            }

            player.setDeltaMovement(0, player.getDeltaMovement().y, 0);
            player.teleportTo(
                    level,
                    clampedX,
                    player.getY(),
                    clampedZ,
                    Collections.emptySet(),
                    player.getYRot(),
                    player.getXRot(),
                    false
            );
            player.sendSystemMessage(Component.literal("§cYou have reached the island boundary!"));
        }
    }
}
