package com.expedition.quests.exploration;

import com.expedition.quests.config.QuestsConfig;
import com.expedition.quests.model.QuestEntry;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Pre-computes and caches unexplored Biome, Structure, and Dungeon coordinates
 * in the background (1 lightweight probe per second while cache < target size)
 * so accepting an Exploration Quest in /quest takes < 0.1ms with ZERO server lag.
 */
public class ExplorationTargetCache {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CACHE_FILE = QuestsConfig.CONFIG_DIR.resolve("precomputed_targets.json");
    private static final int TARGET_CACHE_PER_BUCKET = 20;

    public static class CachedTarget {
        public String category;   // EXPLORE_BIOME, EXPLORE_STRUCTURE, EXPLORE_DUNGEON
        public String dimension;  // minecraft:overworld, minecraft:the_nether, minecraft:the_end
        public String targetId;
        public int x;
        public int z;
        public int distFromOrigin;

        public CachedTarget() {}

        public CachedTarget(String category, String dimension, String targetId, int x, int z, int distFromOrigin) {
            this.category = category;
            this.dimension = dimension;
            this.targetId = targetId;
            this.x = x;
            this.z = z;
            this.distFromOrigin = distFromOrigin;
        }
    }

    // Key: "category|dimension" -> list of pre-located unexplored targets
    private static final Map<String, List<CachedTarget>> CACHE = new ConcurrentHashMap<>();
    private static final Random RNG = new Random();
    private static volatile boolean dirty = false;
    private static int warmupCursor = 0;

    private static final String[] CATEGORIES = {"EXPLORE_BIOME", "EXPLORE_STRUCTURE", "EXPLORE_DUNGEON"};
    private static final String[] DIMENSIONS = {"minecraft:overworld", "minecraft:the_nether", "minecraft:the_end"};

    public static synchronized void load() {
        try {
            Files.createDirectories(QuestsConfig.CONFIG_DIR);
            if (Files.exists(CACHE_FILE)) {
                try (Reader r = Files.newBufferedReader(CACHE_FILE, StandardCharsets.UTF_8)) {
                    Type t = new TypeToken<Map<String, List<CachedTarget>>>(){}.getType();
                    Map<String, List<CachedTarget>> loaded = GSON.fromJson(r, t);
                    if (loaded != null) {
                        CACHE.clear();
                        for (Map.Entry<String, List<CachedTarget>> e : loaded.entrySet()) {
                            CACHE.put(e.getKey(), Collections.synchronizedList(new ArrayList<>(e.getValue())));
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
    }

    public static synchronized void saveIfDirty() {
        if (!dirty) return;
        try {
            Files.createDirectories(QuestsConfig.CONFIG_DIR);
            try (Writer w = Files.newBufferedWriter(CACHE_FILE, StandardCharsets.UTF_8)) {
                GSON.toJson(CACHE, w);
            }
            dirty = false;
        } catch (Exception ignored) {}
    }

    /**
     * Called once per second on server tick. Only performs 1 tiny probe if a bucket
     * has fewer than TARGET_CACHE_PER_BUCKET entries, keeping TPS at a steady 20.0.
     */
    public static void tickBackgroundWarmup(MinecraftServer server, int tickCounter) {
        if (tickCounter % 20 != 0) return;

        int totalBuckets = CATEGORIES.length * DIMENSIONS.length;
        for (int attempt = 0; attempt < totalBuckets; attempt++) {
            int idx = (warmupCursor++) % totalBuckets;
            String cat = CATEGORIES[idx / DIMENSIONS.length];
            String dim = DIMENSIONS[idx % DIMENSIONS.length];
            String key = cat + "|" + dim;

            List<CachedTarget> list = CACHE.computeIfAbsent(key, k -> Collections.synchronizedList(new ArrayList<>()));
            if (list.size() >= TARGET_CACHE_PER_BUCKET) continue;

            List<String> pool = UnexploredStructureLocator.getPool(cat, dim);
            if (pool.isEmpty()) continue;

            ServerLevel level = UnexploredStructureLocator.getLevelByDimensionId(server, dim);
            if (level == null) continue;

            // Pick a distance band across EASY / MEDIUM / HARD ranges
            String[] diffs = {"EASY", "MEDIUM", "HARD"};
            String diff = diffs[RNG.nextInt(diffs.length)];
            int minD = QuestsConfig.getExplorationMinBlocks(cat, diff, dim);
            int maxD = QuestsConfig.getExplorationMaxBlocks(cat, diff, dim);

            String candidateId = pool.get(RNG.nextInt(pool.size()));
            double angle = RNG.nextDouble() * Math.PI * 2.0;
            double dist = minD + RNG.nextDouble() * Math.max(150, maxD - minD);
            int probeX = (int) Math.round(Math.cos(angle) * dist);
            int probeZ = (int) Math.round(Math.sin(angle) * dist);

            if (!ClaimedLocationsRegistry.isLocationUnexploredAndUnclaimed(dim, probeX, probeZ)) {
                return;
            }

            BlockPos probePos = new BlockPos(probeX, 64, probeZ);
            BlockPos found = "EXPLORE_BIOME".equals(cat)
                    ? UnexploredStructureLocator.locateBiomeFast(level, probePos, candidateId)
                    : UnexploredStructureLocator.locateStructureFast(level, probePos, candidateId);

            if (found != null && ClaimedLocationsRegistry.isLocationUnexploredAndUnclaimed(dim, found.getX(), found.getZ())) {
                int actualDist = (int) Math.hypot(found.getX(), found.getZ());
                // Strictly reject outliers (e.g. 10,000m away for an Easy quest!)
                if (actualDist >= Math.max(200, minD - 250) && actualDist <= maxD + 400) {
                    list.add(new CachedTarget(cat, dim, candidateId, found.getX(), found.getZ(), actualDist));
                    dirty = true;
                }
            }
            return; // Max 1 probe per second
        }

        if (tickCounter % 400 == 0) {
            saveIfDirty();
        }
    }

    /**
     * Instant (< 0.1ms) target retrieval from the pre-computed cache when a player accepts a quest.
     * Enforces [minExpectedDist, maxExpectedDist] relative to the player's origin.
     */
    public static CachedTarget popBestTarget(ServerPlayer player, QuestEntry quest) {
        String key = quest.category + "|" + quest.dimension;
        List<CachedTarget> list = CACHE.get(key);
        if (list == null || list.isEmpty()) return null;

        BlockPos origin = ClaimedLocationsRegistry.getSearchOrigin(player);
        int ox = player.level().dimension().identifier().toString().equals(quest.dimension) ? origin.getX() : 0;
        int oz = player.level().dimension().identifier().toString().equals(quest.dimension) ? origin.getZ() : 0;

        synchronized (list) {
            Iterator<CachedTarget> it = list.iterator();
            CachedTarget bestMatch = null;
            int bestDiff = Integer.MAX_VALUE;

            while (it.hasNext()) {
                CachedTarget ct = it.next();
                if (!ClaimedLocationsRegistry.isLocationUnexploredAndUnclaimed(quest.dimension, ct.x, ct.z)) {
                    it.remove();
                    dirty = true;
                    continue;
                }
                int dist = (int) Math.hypot(ct.x - ox, ct.z - oz);
                if (dist >= quest.minExpectedDist && dist <= quest.maxExpectedDist) {
                    // Prefer matching subType if available, otherwise any in band
                    if (ct.targetId.equals(quest.subType)) {
                        it.remove();
                        dirty = true;
                        return ct;
                    }
                    int mid = (quest.minExpectedDist + quest.maxExpectedDist) / 2;
                    int d = Math.abs(dist - mid);
                    if (d < bestDiff) {
                        bestDiff = d;
                        bestMatch = ct;
                    }
                }
            }

            if (bestMatch != null) {
                list.remove(bestMatch);
                dirty = true;
                return bestMatch;
            }
        }
        return null;
    }
}
