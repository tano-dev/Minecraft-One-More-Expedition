package com.expedition.quests.exploration;

import com.expedition.quests.config.QuestsConfig;
import com.expedition.quests.model.QuestEntry;
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.Structure;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class UnexploredStructureLocator {

    // Indexed pools by Dimension -> School -> List of Registry IDs
    public static final Map<String, List<String>> BIOMES_BY_DIM = new ConcurrentHashMap<>();
    public static final Map<String, List<String>> STRUCTURES_BY_DIM = new ConcurrentHashMap<>();
    public static final Map<String, List<String>> DUNGEONS_BY_DIM = new ConcurrentHashMap<>();

    public static void scanRegistries(MinecraftServer server) {
        BIOMES_BY_DIM.clear();
        STRUCTURES_BY_DIM.clear();
        DUNGEONS_BY_DIM.clear();

        try {
            Registry<Structure> structReg = server.registryAccess().lookupOrThrow(Registries.STRUCTURE);
            for (ResourceKey<Structure> key : structReg.registryKeySet()) {
                String id = key.identifier().toString();
                StructureClassifier.School school = StructureClassifier.classifyStructure(id);
                if (school == StructureClassifier.School.IGNORED) continue;
                String dim = StructureClassifier.inferDimension(id);
                if (school == StructureClassifier.School.EXPLORE_DUNGEON) {
                    DUNGEONS_BY_DIM.computeIfAbsent(dim, k -> new ArrayList<>()).add(id);
                } else if (school == StructureClassifier.School.EXPLORE_STRUCTURE) {
                    STRUCTURES_BY_DIM.computeIfAbsent(dim, k -> new ArrayList<>()).add(id);
                }
            }

            Registry<Biome> biomeReg = server.registryAccess().lookupOrThrow(Registries.BIOME);
            for (ResourceKey<Biome> key : biomeReg.registryKeySet()) {
                String id = key.identifier().toString();
                if (id.contains("the_void")) continue;
                String dim = StructureClassifier.inferDimension(id);
                BIOMES_BY_DIM.computeIfAbsent(dim, k -> new ArrayList<>()).add(id);
            }
        } catch (Exception ignored) {}
    }

    public static List<String> getPool(String category, String dimension) {
        Map<String, List<String>> map = switch (category) {
            case "EXPLORE_BIOME" -> BIOMES_BY_DIM;
            case "EXPLORE_DUNGEON" -> DUNGEONS_BY_DIM;
            default -> STRUCTURES_BY_DIM;
        };
        return map.getOrDefault(dimension, Collections.emptyList());
    }

    /**
     * Lazy-Locate when the player clicks to accept an Exploration Quest in the Draft GUI.
     * Guarantees an unexplored & unclaimed coordinate within the configured distance band.
     */
    public static void locateAndLockTarget(MinecraftServer server, ServerPlayer player, QuestEntry quest) {
        if (quest.located) return;

        ServerLevel targetLevel = getLevelByDimensionId(server, quest.dimension);
        if (targetLevel == null) targetLevel = server.overworld();

        BlockPos origin = ClaimedLocationsRegistry.getSearchOrigin(player);
        // If quest is in another dimension than player, scale or center near (0,0) if extreme
        int ox = player.level() == targetLevel ? origin.getX() : 0;
        int oz = player.level() == targetLevel ? origin.getZ() : 0;

        int baseMin = quest.minExpectedDist;
        int baseMax = quest.maxExpectedDist;
        int expandStep = QuestsConfig.getAutoExpandStepBlocks();
        int maxAttempts = QuestsConfig.getMaxAutoExpandAttempts();

        Random rng = new Random();
        List<String> backupCandidates = new ArrayList<>(getPool(quest.category, quest.dimension));
        Collections.shuffle(backupCandidates, rng);

        // Try the primary subType first, then up to 4 backup candidates of the same school & dimension
        List<String> tryIds = new ArrayList<>();
        tryIds.add(quest.subType);
        for (String b : backupCandidates) {
            if (!b.equals(quest.subType) && tryIds.size() < 5) {
                tryIds.add(b);
            }
        }

        for (int expand = 0; expand < maxAttempts; expand++) {
            int curMin = baseMin + expand * expandStep;
            int curMax = baseMax + expand * expandStep;

            for (String candidateId : tryIds) {
                double angle = rng.nextDouble() * Math.PI * 2.0;
                double dist = curMin + rng.nextDouble() * Math.max(100, curMax - curMin);
                int probeX = ox + (int) Math.round(Math.cos(angle) * dist);
                int probeZ = oz + (int) Math.round(Math.sin(angle) * dist);

                if (!ClaimedLocationsRegistry.isLocationUnexploredAndUnclaimed(quest.dimension, probeX, probeZ)) {
                    continue;
                }

                BlockPos probePos = new BlockPos(probeX, 64, probeZ);
                BlockPos found = "EXPLORE_BIOME".equals(quest.category)
                        ? locateBiomeNear(targetLevel, probePos, candidateId)
                        : locateStructureNear(targetLevel, probePos, candidateId);

                if (found != null && ClaimedLocationsRegistry.isLocationUnexploredAndUnclaimed(quest.dimension, found.getX(), found.getZ())) {
                    if (isChunkAlreadyInhabited(targetLevel, found)) {
                        continue;
                    }
                    // Found a valid unexplored target!
                    quest.subType = candidateId;
                    String schoolPrefix = switch (quest.category) {
                        case "EXPLORE_BIOME" -> "🌿 Khám phá Quần xã: ";
                        case "EXPLORE_DUNGEON" -> "🗝️ Thám hiểm Dungeon: ";
                        default -> "🏛️ Khám phá Công trình: ";
                    };
                    quest.displayName = schoolPrefix + StructureClassifier.formatDisplayName(candidateId);
                    quest.targetX = found.getX();
                    quest.targetZ = found.getZ();
                    quest.located = true;
                    quest.actualDistance = (int) Math.hypot(found.getX() - ox, found.getZ() - oz);
                    applyDistanceBonus(quest);
                    ClaimedLocationsRegistry.claimLocation(quest.dimension, quest.targetX, quest.targetZ);
                    return;
                }
            }
        }

        // Guaranteed mathematical fallback in an unexplored ring if registry locator timed out
        for (int i = 0; i < 16; i++) {
            double angle = rng.nextDouble() * Math.PI * 2.0;
            double dist = baseMin + rng.nextDouble() * Math.max(200, baseMax - baseMin);
            int fx = ox + (int) Math.round(Math.cos(angle) * dist);
            int fz = oz + (int) Math.round(Math.sin(angle) * dist);
            if (ClaimedLocationsRegistry.isLocationUnexploredAndUnclaimed(quest.dimension, fx, fz)) {
                quest.targetX = fx;
                quest.targetZ = fz;
                quest.located = true;
                quest.actualDistance = (int) Math.hypot(fx - ox, fz - oz);
                applyDistanceBonus(quest);
                ClaimedLocationsRegistry.claimLocation(quest.dimension, fx, fz);
                return;
            }
        }
    }

    private static void applyDistanceBonus(QuestEntry quest) {
        double bonusPer1000 = QuestsConfig.getDistanceRewardBonusPer1000m();
        if (bonusPer1000 > 0 && quest.actualDistance > 0) {
            double mult = 1.0 + (quest.actualDistance / 1000.0) * bonusPer1000;
            quest.rewardCoins = Math.max(1L, Math.round(quest.rewardCoins * mult));
            quest.rewardXp = (int) Math.max(5, Math.round(quest.rewardXp * mult));
        }
    }

    private static boolean isChunkAlreadyInhabited(ServerLevel level, BlockPos pos) {
        try {
            int cx = pos.getX() >> 4;
            int cz = pos.getZ() >> 4;
            ChunkAccess chunk = level.getChunkSource().getChunkNow(cx, cz);
            if (chunk != null && chunk.getInhabitedTime() > 40L) {
                return true;
            }
        } catch (Exception ignored) {}
        return false;
    }

    private static BlockPos locateStructureNear(ServerLevel level, BlockPos probePos, String structureId) {
        try {
            Registry<Structure> reg = level.registryAccess().lookupOrThrow(Registries.STRUCTURE);
            ResourceKey<Structure> key = ResourceKey.create(Registries.STRUCTURE, Identifier.parse(structureId));
            Optional<Holder.Reference<Structure>> opt = reg.get(key);
            if (opt.isEmpty()) return null;

            HolderSet<Structure> set = HolderSet.direct(opt.get());
            Pair<BlockPos, Holder<Structure>> res = level.getChunkSource().getGenerator()
                    .findNearestMapStructure(level, set, probePos, 24, false);
            return res != null ? res.getFirst() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private static BlockPos locateBiomeNear(ServerLevel level, BlockPos probePos, String biomeId) {
        try {
            ResourceKey<Biome> key = ResourceKey.create(Registries.BIOME, Identifier.parse(biomeId));
            Pair<BlockPos, Holder<Biome>> res = level.findClosestBiome3d(
                    h -> h.is(key),
                    probePos,
                    960,
                    48,
                    64
            );
            return res != null ? res.getFirst() : null;
        } catch (Exception e) {
            return null;
        }
    }

    public static ServerLevel getLevelByDimensionId(MinecraftServer server, String dimId) {
        if ("minecraft:the_nether".equals(dimId)) return server.getLevel(Level.NETHER);
        if ("minecraft:the_end".equals(dimId)) return server.getLevel(Level.END);
        return server.overworld();
    }

    public static String getCompassArrow(ServerPlayer player, int targetX, int targetZ) {
        double dx = targetX - player.getX();
        double dz = targetZ - player.getZ();
        double targetAngle = Math.toDegrees(Math.atan2(-dx, dz));
        double playerYaw = Mth.wrapDegrees(player.getYRot());
        double diff = Mth.wrapDegrees(targetAngle - playerYaw);

        if (diff >= -22.5 && diff < 22.5) return "⬆";
        if (diff >= 22.5 && diff < 67.5) return "↗";
        if (diff >= 67.5 && diff < 112.5) return "➡";
        if (diff >= 112.5 && diff < 157.5) return "↘";
        if (diff >= -67.5 && diff < -22.5) return "↖";
        if (diff >= -112.5 && diff < -67.5) return "⬅";
        if (diff >= -157.5 && diff < -112.5) return "↙";
        return "⬇";
    }
}
