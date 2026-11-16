package com.expedition.quests.exploration;

import com.expedition.quests.config.QuestsConfig;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;

import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ClaimedLocationsRegistry {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = QuestsConfig.CONFIG_DIR.resolve("explored_locations.json");

    public static class SavedState {
        public Set<String> visitedGridCells = ConcurrentHashMap.newKeySet();
        public List<String> claimedCoordinates = Collections.synchronizedList(new ArrayList<>());
    }

    private static SavedState STATE = new SavedState();
    private static volatile boolean dirty = false;

    public static synchronized void load() {
        try {
            Files.createDirectories(QuestsConfig.CONFIG_DIR);
            if (Files.exists(FILE)) {
                try (Reader r = Files.newBufferedReader(FILE, StandardCharsets.UTF_8)) {
                    SavedState loaded = GSON.fromJson(r, SavedState.class);
                    if (loaded != null) {
                        if (loaded.visitedGridCells != null) {
                            STATE.visitedGridCells.addAll(loaded.visitedGridCells);
                        }
                        if (loaded.claimedCoordinates != null) {
                            STATE.claimedCoordinates.addAll(loaded.claimedCoordinates);
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
            try (Writer w = Files.newBufferedWriter(FILE, StandardCharsets.UTF_8)) {
                GSON.toJson(STATE, w);
            }
            dirty = false;
        } catch (Exception ignored) {}
    }

    public static void recordPlayerPosition(ServerPlayer player) {
        String dim = player.level().dimension().identifier().toString();
        if (dim.contains("skyblock")) return;
        int cell = QuestsConfig.getVisitedGridCellSize();
        int gx = Math.floorDiv(player.getBlockX(), cell);
        int gz = Math.floorDiv(player.getBlockZ(), cell);
        // Mark current cell and immediate 3x3 neighbor cells as visited
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                String key = dim + ":" + (gx + dx) + ":" + (gz + dz);
                if (STATE.visitedGridCells.add(key)) {
                    dirty = true;
                }
            }
        }
    }

    public static boolean isLocationUnexploredAndUnclaimed(String dimension, int x, int z) {
        int cell = QuestsConfig.getVisitedGridCellSize();
        int gx = Math.floorDiv(x, cell);
        int gz = Math.floorDiv(z, cell);
        String gridKey = dimension + ":" + gx + ":" + gz;
        if (STATE.visitedGridCells.contains(gridKey)) {
            return false;
        }

        int minIso = QuestsConfig.getMinIsolationBlocks();
        long minIsoSq = (long) minIso * minIso;

        synchronized (STATE.claimedCoordinates) {
            for (String entry : STATE.claimedCoordinates) {
                String[] parts = entry.split(":");
                if (parts.length < 4) continue;
                String eDim = parts[0] + ":" + parts[1];
                if (!eDim.equals(dimension)) continue;
                try {
                    int cx = Integer.parseInt(parts[2]);
                    int cz = Integer.parseInt(parts[3]);
                    long dx = x - cx;
                    long dz = z - cz;
                    if (dx * dx + dz * dz < minIsoSq) {
                        return false;
                    }
                } catch (Exception ignored) {}
            }
        }
        return true;
    }

    public static void claimLocation(String dimension, int x, int z) {
        String record = dimension + ":" + x + ":" + z;
        STATE.claimedCoordinates.add(record);
        dirty = true;
    }

    public static BlockPos getSearchOrigin(ServerPlayer player) {
        if ("WORLD_SPAWN".equalsIgnoreCase(QuestsConfig.getOriginMode())) {
            return BlockPos.ZERO;
        }
        return player.blockPosition();
    }
}
