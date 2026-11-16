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
import net.minecraft.world.level.levelgen.structure.Structure;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class UnexploredStructureLocator {

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
     * Instant (< 0.1ms) target lock using Precomputed ExplorationTargetCache first,
     * with a strictly bounded fallback so Easy quests NEVER exceed maxExpectedDist.
     */
    public static void locateAndLockTarget(MinecraftServer server, ServerPlayer player, QuestEntry quest) {
        if (quest.located) return;

        BlockPos origin = ClaimedLocationsRegistry.getSearchOrigin(player);
        ServerLevel targetLevel = getLevelByDimensionId(server, quest.dimension);
        if (targetLevel == null) targetLevel = server.overworld();

        int ox = player.level() == targetLevel ? origin.getX() : 0;
        int oz = player.level() == targetLevel ? origin.getZ() : 0;

        // 1. Try Pre-Computed Cache (Zero server lag!)
        ExplorationTargetCache.CachedTarget cached = ExplorationTargetCache.popBestTarget(player, quest);
        if (cached != null) {
            quest.subType = cached.targetId;
            quest.displayName = getSchoolPrefix(quest.category) + StructureClassifier.formatDisplayName(cached.targetId);
            quest.targetX = cached.x;
            quest.targetZ = cached.z;
            quest.located = true;
            quest.actualDistance = (int) Math.hypot(cached.x - ox, cached.z - oz);
            applyDistanceBonus(quest);
            ClaimedLocationsRegistry.claimLocation(quest.dimension, quest.targetX, quest.targetZ);
            return;
        }

        // 2. Fast bounded fallback: try up to 4 real structure/biome lookups, never invent fake structure coords
        int minD = quest.minExpectedDist;
        int maxD = quest.maxExpectedDist;
        Random rng = new Random();
        List<String> pool = new ArrayList<>(getPool(quest.category, quest.dimension));
        Collections.shuffle(pool, rng);
        List<String> tryIds = new ArrayList<>();
        tryIds.add(quest.subType);
        for (String pId : pool) {
            if (!pId.equals(quest.subType) && tryIds.size() < 5) tryIds.add(pId);
        }

        for (String candidateId : tryIds) {
            double angle = rng.nextDouble() * Math.PI * 2.0;
            double dist = minD + rng.nextDouble() * Math.max(100, maxD - minD);
            int probeX = ox + (int) Math.round(Math.cos(angle) * dist);
            int probeZ = oz + (int) Math.round(Math.sin(angle) * dist);

            if (!ClaimedLocationsRegistry.isLocationUnexploredAndUnclaimed(quest.dimension, probeX, probeZ)) {
                continue;
            }

            BlockPos probePos = new BlockPos(probeX, 64, probeZ);
            BlockPos found = "EXPLORE_BIOME".equals(quest.category)
                    ? locateBiomeFast(targetLevel, probePos, candidateId)
                    : locateStructureFast(targetLevel, probePos, candidateId);

            if (found != null && ClaimedLocationsRegistry.isLocationUnexploredAndUnclaimed(quest.dimension, found.getX(), found.getZ())) {
                int actualDist = (int) Math.hypot(found.getX() - ox, found.getZ() - oz);
                if (actualDist >= Math.max(200, minD - 250) && actualDist <= maxD + 400 && !isChunkAlreadyInhabited(targetLevel, found)) {
                    quest.subType = candidateId;
                    quest.displayName = getSchoolPrefix(quest.category) + StructureClassifier.formatDisplayName(candidateId);
                    quest.targetX = found.getX();
                    quest.targetZ = found.getZ();
                    quest.located = true;
                    quest.actualDistance = actualDist;
                    applyDistanceBonus(quest);
                    ClaimedLocationsRegistry.claimLocation(quest.dimension, quest.targetX, quest.targetZ);
                    return;
                }
            }
        }

        // 3. If no structure matched in band yet, convert cleanly to a real nearby Unexplored Biome (guaranteed real!)
        List<String> biomePool = new ArrayList<>(getPool("EXPLORE_BIOME", quest.dimension));
        Collections.shuffle(biomePool, rng);
        for (int i = 0; i < Math.min(4, biomePool.size()); i++) {
            String bId = biomePool.get(i);
            double angle = rng.nextDouble() * Math.PI * 2.0;
            double dist = minD + rng.nextDouble() * Math.max(150, maxD - minD);
            int fx = ox + (int) Math.round(Math.cos(angle) * dist);
            int fz = oz + (int) Math.round(Math.sin(angle) * dist);
            BlockPos foundBiome = locateBiomeFast(targetLevel, new BlockPos(fx, 64, fz), bId);
            if (foundBiome != null && ClaimedLocationsRegistry.isLocationUnexploredAndUnclaimed(quest.dimension, foundBiome.getX(), foundBiome.getZ())) {
                quest.category = "EXPLORE_BIOME";
                quest.subType = bId;
                quest.displayName = getSchoolPrefix("EXPLORE_BIOME") + StructureClassifier.formatDisplayName(bId);
                quest.targetX = foundBiome.getX();
                quest.targetZ = foundBiome.getZ();
                quest.located = true;
                quest.actualDistance = (int) Math.hypot(foundBiome.getX() - ox, foundBiome.getZ() - oz);
                applyDistanceBonus(quest);
                ClaimedLocationsRegistry.claimLocation(quest.dimension, quest.targetX, quest.targetZ);
                return;
            }
        }
    }

    public static String getSchoolPrefix(String category) {
        return switch (category) {
            case "EXPLORE_BIOME" -> "Explore Biome: ";
            case "EXPLORE_DUNGEON" -> "Explore Dungeon: ";
            default -> "Explore Structure: ";
        };
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

    public static BlockPos locateStructureFast(ServerLevel level, BlockPos probePos, String structureId) {
        try {
            Registry<Structure> reg = level.registryAccess().lookupOrThrow(Registries.STRUCTURE);
            ResourceKey<Structure> key = ResourceKey.create(Registries.STRUCTURE, Identifier.parse(structureId));
            Optional<Holder.Reference<Structure>> opt = reg.get(key);
            if (opt.isEmpty()) return null;

            HolderSet<Structure> set = HolderSet.direct(opt.get());
            // Radius 10 chunks (160 blocks) keeps lookup fast and strictly near probePos
            Pair<BlockPos, Holder<Structure>> res = level.getChunkSource().getGenerator()
                    .findNearestMapStructure(level, set, probePos, 10, false);
            return res != null ? res.getFirst() : null;
        } catch (Exception e) {
            return null;
        }
    }

    public static BlockPos locateBiomeFast(ServerLevel level, BlockPos probePos, String biomeId) {
        try {
            ResourceKey<Biome> key = ResourceKey.create(Registries.BIOME, Identifier.parse(biomeId));
            Pair<BlockPos, Holder<Biome>> res = level.findClosestBiome3d(
                    h -> h.is(key),
                    probePos,
                    480,
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

        if (diff >= -22.5 && diff < 22.5) return "↑";
        if (diff >= 22.5 && diff < 67.5) return "↗";
        if (diff >= 67.5 && diff < 112.5) return "→";
        if (diff >= 112.5 && diff < 157.5) return "↘";
        if (diff >= -67.5 && diff < -22.5) return "↖";
        if (diff >= -112.5 && diff < -67.5) return "←";
        if (diff >= -157.5 && diff < -112.5) return "↙";
        return "↓";
    }
}
