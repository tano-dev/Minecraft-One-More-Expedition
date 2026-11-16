package com.expedition.fishing.mechanics;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import com.mojang.math.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class BroodmotherCosmeticsManager {

    private record StrandSpec(
            float anchorX, float anchorY, float anchorZ,
            float dirX, float dirY, float dirZ,
            float bendX, float bendY, float bendZ,
            int segments, float phase
    ) {}

    private static final StrandSpec[] STRANDS = {
            new StrandSpec(-0.96f, 0.36f, 0.08f, -1.00f, 0.12f, 0.14f, 0.00f, 0.25f, -0.62f, 5, 0.20f),
            new StrandSpec(-0.96f, -0.18f, -0.34f, -0.92f, -0.38f, -0.14f, 0.00f, -0.16f, -0.54f, 5, 1.15f),
            new StrandSpec(0.96f, 0.30f, -0.12f, 0.98f, 0.08f, -0.18f, 0.00f, 0.20f, 0.58f, 5, 2.20f),
            new StrandSpec(0.96f, -0.08f, 0.36f, 0.93f, -0.32f, 0.18f, 0.00f, -0.12f, 0.60f, 5, 3.05f),
            new StrandSpec(-0.35f, 0.96f, -0.18f, -0.28f, 0.72f, -0.30f, 0.58f, 0.10f, 0.18f, 4, 3.80f),
            new StrandSpec(0.38f, 0.96f, 0.12f, 0.34f, 0.64f, 0.18f, -0.50f, 0.16f, 0.34f, 4, 4.55f),
            new StrandSpec(-0.68f, 0.10f, -0.96f, -0.40f, 0.02f, -0.92f, 0.45f, -0.10f, 0.00f, 4, 5.45f),
            new StrandSpec(0.42f, -0.22f, -0.96f, 0.16f, -0.24f, -0.98f, -0.50f, -0.08f, 0.00f, 4, 6.20f),
            new StrandSpec(-0.12f, -0.34f, -0.96f, -0.06f, -0.34f, -0.94f, 0.36f, -0.16f, 0.00f, 4, 7.05f),
    };

    private record SegmentNode(UUID displayUuid, int strandIdx, int segIdx) {}

    private record BroodmotherInstance(net.minecraft.resources.ResourceKey<Level> dimension, List<SegmentNode> nodes) {}

    private static final Map<UUID, BroodmotherInstance> ACTIVE_BROODMOTHERS = new ConcurrentHashMap<>();
    private static long tickCounter = 0L;

    public static void attachBroodmotherTentacles(ServerLevel level, LivingEntity broodmother) {
        cleanup(level.getServer(), broodmother.getUUID());
        EntityType<?> bdType = BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.withDefaultNamespace("block_display"));
        if (bdType == null) return;

        List<SegmentNode> nodes = new ArrayList<>();
        for (int sIdx = 0; sIdx < STRANDS.length; sIdx++) {
            StrandSpec spec = STRANDS[sIdx];
            for (int seg = 0; seg < spec.segments(); seg++) {
                Entity raw = bdType.create(level, EntitySpawnReason.EVENT);
                if (raw instanceof Display.BlockDisplay bd) {
                    bd.setPos(broodmother.getX(), broodmother.getY(), broodmother.getZ());
                    bd.setNoGravity(true);
                    bd.setBlockState(Blocks.NETHER_WART_BLOCK.defaultBlockState());
                    level.addFreshEntity(bd);
                    nodes.add(new SegmentNode(bd.getUUID(), sIdx, seg));
                }
            }
        }
        if (!nodes.isEmpty()) {
            ACTIVE_BROODMOTHERS.put(broodmother.getUUID(), new BroodmotherInstance(level.dimension(), nodes));
        }
    }

    public static void cleanup(MinecraftServer server, UUID parentUuid) {
        BroodmotherInstance inst = ACTIVE_BROODMOTHERS.remove(parentUuid);
        if (inst == null || server == null) return;
        ServerLevel level = server.getLevel(inst.dimension());
        if (level == null) return;
        for (SegmentNode node : inst.nodes()) {
            Entity e = level.getEntity(node.displayUuid());
            if (e != null) e.discard();
        }
    }

    public static void tick(MinecraftServer server) {
        if (ACTIVE_BROODMOTHERS.isEmpty()) return;
        tickCounter++;

        Iterator<Map.Entry<UUID, BroodmotherInstance>> it = ACTIVE_BROODMOTHERS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, BroodmotherInstance> entry = it.next();
            UUID parentId = entry.getKey();
            BroodmotherInstance inst = entry.getValue();
            ServerLevel level = server.getLevel(inst.dimension());
            if (level == null) {
                it.remove();
                continue;
            }
            Entity parentEnt = level.getEntity(parentId);
            if (!(parentEnt instanceof LivingEntity brood) || !brood.isAlive()) {
                for (SegmentNode node : inst.nodes()) {
                    Entity e = level.getEntity(node.displayUuid());
                    if (e != null) e.discard();
                }
                it.remove();
                continue;
            }

            // Smoothly face nearest player
            ServerPlayer nearest = null;
            double bestDist = 60.0 * 60.0;
            for (ServerPlayer sp : level.players()) {
                if (!sp.isAlive() || sp.isSpectator()) continue;
                double d = sp.distanceToSqr(brood);
                if (d < bestDist) {
                    bestDist = d;
                    nearest = sp;
                }
            }
            if (nearest != null) {
                double dx = nearest.getX() - brood.getX();
                double dz = nearest.getZ() - brood.getZ();
                float desiredYaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
                brood.setYRot(desiredYaw);
                brood.setYHeadRot(desiredYaw);
                brood.setYBodyRot(desiredYaw);
            }

            AABB box = brood.getBoundingBox();
            double cx = (box.minX + box.maxX) * 0.5;
            double cy = (box.minY + box.maxY) * 0.5;
            double cz = (box.minZ + box.maxZ) * 0.5;
            float halfX = Math.max(0.8f, (float) (box.getXsize() * 0.5));
            float halfY = Math.max(0.8f, (float) (box.getYsize() * 0.5));
            float halfZ = Math.max(0.8f, (float) (box.getZsize() * 0.5));
            float bodySpan = Math.max(halfX, halfZ) * 2.0f;
            float blockSize = Math.max(0.45f, bodySpan * 0.18f);
            float spacing = blockSize * 0.72f;

            double rad = Math.toRadians(brood.getYRot());
            float cosYaw = (float) Math.cos(rad);
            float sinYaw = (float) Math.sin(rad);

            for (SegmentNode node : inst.nodes()) {
                Entity child = level.getEntity(node.displayUuid());
                if (!(child instanceof Display.BlockDisplay bd)) continue;

                StrandSpec strand = STRANDS[node.strandIdx()];
                int segment = node.segIdx();
                float depth = strand.segments() <= 1 ? 0.0f : segment / (float) (strand.segments() - 1);

                float distance = spacing * (segment + 0.35f);
                float bend = depth * depth * spacing * 1.55f;
                float wave = (float) Math.sin(tickCounter * 0.08 + strand.phase() + segment * 0.55f) * spacing * 0.18f * depth;
                float bob = (float) Math.cos(tickCounter * 0.07 + strand.phase() + segment * 0.35f) * spacing * 0.10f * depth;

                float localX = strand.anchorX() * halfX + strand.dirX() * distance + strand.bendX() * bend + strand.bendZ() * wave;
                float localY = strand.anchorY() * halfY + strand.dirY() * distance + strand.bendY() * bend + bob;
                float localZ = strand.anchorZ() * halfZ + strand.dirZ() * distance + strand.bendZ() * bend - strand.bendX() * wave;

                double worldX = cx + localX * cosYaw - localZ * sinYaw;
                double worldZ = cz + localX * sinYaw + localZ * cosYaw;

                float taper = Math.max(0.62f, 1.0f - depth * 0.30f);
                float rootBoost = segment == 0 ? 1.18f : 1.0f;
                float sz = blockSize * taper * rootBoost;

                bd.setPos(worldX, cy + localY, worldZ);
                bd.setYRot(brood.getYRot());
                bd.setTransformation(new Transformation(
                        new Vector3f(-sz * 0.5f, -sz * 0.5f, -sz * 0.5f),
                        new Quaternionf(),
                        new Vector3f(sz, sz, sz),
                        new Quaternionf()
                ));
            }
        }
    }

    public static void applyGlowColor(ServerLevel level, Entity entity, String colorName) {
        if (colorName == null || colorName.isEmpty() || level.getServer() == null) return;
        entity.setGlowingTag(true);
        net.minecraft.world.scores.TeamColor tc = switch (colorName.toUpperCase(Locale.ROOT)) {
            case "DARK_RED" -> net.minecraft.world.scores.TeamColor.DARK_RED;
            case "RED" -> net.minecraft.world.scores.TeamColor.RED;
            case "ORANGE", "GOLD" -> net.minecraft.world.scores.TeamColor.GOLD;
            case "YELLOW" -> net.minecraft.world.scores.TeamColor.YELLOW;
            case "DARK_GREEN" -> net.minecraft.world.scores.TeamColor.DARK_GREEN;
            case "GREEN" -> net.minecraft.world.scores.TeamColor.GREEN;
            case "AQUA" -> net.minecraft.world.scores.TeamColor.AQUA;
            case "DARK_PURPLE" -> net.minecraft.world.scores.TeamColor.DARK_PURPLE;
            case "LIGHT_PURPLE", "PURPLE" -> net.minecraft.world.scores.TeamColor.LIGHT_PURPLE;
            case "DARK_GRAY", "BLACK" -> net.minecraft.world.scores.TeamColor.DARK_GRAY;
            default -> net.minecraft.world.scores.TeamColor.WHITE;
        };
        try {
            Scoreboard sb = level.getServer().getScoreboard();
            String teamName = "ef_" + tc.name().toLowerCase(Locale.ROOT);
            if (teamName.length() > 16) teamName = teamName.substring(0, 16);
            PlayerTeam team = sb.getPlayerTeam(teamName);
            if (team == null) {
                team = sb.addPlayerTeam(teamName);
                team.setColor(Optional.of(tc));
            }
            sb.addPlayerToTeam(entity.getScoreboardName(), team);
        } catch (Throwable ignored) {}
    }

    public static void equipSulfurCubeVariant(LivingEntity entity) {
        Block[] blocks = {Blocks.GOLD_BLOCK, Blocks.MAGMA_BLOCK, Blocks.RED_NETHER_BRICKS, Blocks.SHROOMLIGHT};
        Block pick = blocks[entity.getRandom().nextInt(blocks.length)];
        entity.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, new net.minecraft.world.item.ItemStack(pick));
    }
}
