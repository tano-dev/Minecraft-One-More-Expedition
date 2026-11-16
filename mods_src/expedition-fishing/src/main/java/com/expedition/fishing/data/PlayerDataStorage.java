package com.expedition.fishing.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerDataStorage {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path DATA_DIR = FabricLoader.getInstance().getGameDir().resolve("config").resolve("expedition-fishing").resolve("playerdata");
    private static final Map<UUID, FishingPlayerData> CACHE = new ConcurrentHashMap<>();

    public static FishingPlayerData get(UUID uuid) {
        return CACHE.computeIfAbsent(uuid, PlayerDataStorage::loadFromDisk);
    }

    private static FishingPlayerData loadFromDisk(UUID uuid) {
        try {
            Files.createDirectories(DATA_DIR);
            Path file = DATA_DIR.resolve(uuid.toString() + ".json");
            if (Files.exists(file)) {
                try (Reader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                    FishingPlayerData data = GSON.fromJson(r, FishingPlayerData.class);
                    if (data != null) {
                        if (data.mobCatches == null) data.mobCatches = new ConcurrentHashMap<>();
                        if (data.bestWeights == null) data.bestWeights = new ConcurrentHashMap<>();
                        if (data.discoveredArtifacts == null) data.discoveredArtifacts = ConcurrentHashMap.newKeySet();
                        if (data.artifactBag == null) data.artifactBag = new java.util.ArrayList<>();
                        return data;
                    }
                }
            }
        } catch (Exception ignored) {}
        return new FishingPlayerData();
    }

    public static void saveAsync(UUID uuid) {
        FishingPlayerData data = CACHE.get(uuid);
        if (data == null || !data.dirty) return;
        data.dirty = false;
        CompletableFuture.runAsync(() -> writeSync(uuid, data));
    }

    public static void saveAllSync() {
        for (Map.Entry<UUID, FishingPlayerData> entry : CACHE.entrySet()) {
            if (entry.getValue().dirty) {
                entry.getValue().dirty = false;
                writeSync(entry.getKey(), entry.getValue());
            }
        }
    }

    private static void writeSync(UUID uuid, FishingPlayerData data) {
        try {
            Files.createDirectories(DATA_DIR);
            Path file = DATA_DIR.resolve(uuid.toString() + ".json");
            try (Writer w = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                GSON.toJson(data, w);
            }
        } catch (Exception ignored) {}
    }
}
