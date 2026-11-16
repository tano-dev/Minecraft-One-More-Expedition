package com.expedition.quests.data;

import com.expedition.quests.config.QuestsConfig;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class QuestDataStorage {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PLAYERS_DIR = QuestsConfig.CONFIG_DIR.resolve("players");
    private static final Map<UUID, QuestPlayerData> CACHE = new ConcurrentHashMap<>();

    public static QuestPlayerData get(UUID uuid) {
        QuestPlayerData data = CACHE.computeIfAbsent(uuid, QuestDataStorage::loadFromDisk);
        data.checkDailyReset();
        return data;
    }

    private static QuestPlayerData loadFromDisk(UUID uuid) {
        try {
            Files.createDirectories(PLAYERS_DIR);
            Path file = PLAYERS_DIR.resolve(uuid.toString() + ".json");
            if (Files.exists(file)) {
                try (Reader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                    QuestPlayerData loaded = GSON.fromJson(r, QuestPlayerData.class);
                    if (loaded != null) {
                        loaded.uuid = uuid;
                        if (loaded.activeSlots == null) loaded.activeSlots = new ConcurrentHashMap<>();
                        if (loaded.draftOffers == null) loaded.draftOffers = new ConcurrentHashMap<>();
                        return loaded;
                    }
                }
            }
        } catch (Exception ignored) {}
        return new QuestPlayerData(uuid);
    }

    public static void saveAsync(UUID uuid) {
        QuestPlayerData data = CACHE.get(uuid);
        if (data == null) return;
        CompletableFuture.runAsync(() -> saveSync(data));
    }

    public static void saveSync(QuestPlayerData data) {
        try {
            Files.createDirectories(PLAYERS_DIR);
            Path file = PLAYERS_DIR.resolve(data.uuid.toString() + ".json");
            try (Writer w = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                GSON.toJson(data, w);
            }
            data.dirty = false;
        } catch (Exception ignored) {}
    }

    public static void saveAllSync() {
        for (QuestPlayerData data : CACHE.values()) {
            if (data.dirty) {
                saveSync(data);
            }
        }
    }
}
