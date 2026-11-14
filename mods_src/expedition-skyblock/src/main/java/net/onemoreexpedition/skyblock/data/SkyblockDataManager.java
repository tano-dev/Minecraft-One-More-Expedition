package net.onemoreexpedition.skyblock.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import net.onemoreexpedition.skyblock.ExpeditionSkyblockMod;
import net.onemoreexpedition.skyblock.island.IslandGenerator;
import net.onemoreexpedition.skyblock.island.IslandMath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class SkyblockDataManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(SkyblockDataManager.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "expedition_skyblock.json";

    private final MinecraftServer server;
    private final Path dataFile;

    private int version = 1;
    private int nextIslandIndex = 0;
    private final Map<UUID, SkyblockIsland> islands = new ConcurrentHashMap<>();
    private final Map<UUID, PlayerReturnLocation> playerReturns = new ConcurrentHashMap<>();

    private static SkyblockDataManager instance;

    public SkyblockDataManager(MinecraftServer server) {
        this.server = server;
        Path dataDir = server.getWorldPath(LevelResource.ROOT).resolve("data");
        try {
            Files.createDirectories(dataDir);
        } catch (IOException e) {
            LOGGER.error("Failed to create data directory", e);
        }
        this.dataFile = dataDir.resolve(FILE_NAME);
    }

    public static void initialize(MinecraftServer server) {
        instance = new SkyblockDataManager(server);
        instance.load();
    }

    public static SkyblockDataManager getInstance() {
        return instance;
    }

    public synchronized SkyblockIsland getOrCreateIsland(ServerPlayer player) {
        UUID uuid = player.getUUID();
        SkyblockIsland existing = islands.get(uuid);
        if (existing != null) {
            // Update owner name in case player renamed
            existing.setOwnerName(player.getGameProfile().name());
            return existing;
        }

        int index = nextIslandIndex++;
        IslandMath.GridPos grid = IslandMath.indexToGridPos(index);
        IslandMath.WorldPos center = IslandMath.indexToCenterPos(index);

        SkyblockIsland newIsland = new SkyblockIsland(
                uuid,
                player.getGameProfile().name(),
                index,
                grid.u(),
                grid.v(),
                center.x(),
                center.z()
        );

        ServerLevel skyblockLevel = server.getLevel(ExpeditionSkyblockMod.SKYBLOCK_DIMENSION);
        if (skyblockLevel != null) {
            IslandGenerator.generateStarterIsland(skyblockLevel, center.x(), center.z());
        } else {
            LOGGER.warn("Skyblock dimension not available during island generation for player {}", player.getName().getString());
        }

        islands.put(uuid, newIsland);
        save();
        return newIsland;
    }

    public SkyblockIsland getIslandByOwner(UUID ownerUUID) {
        return islands.get(ownerUUID);
    }

    public Optional<SkyblockIsland> findIslandByOwnerName(String name) {
        return islands.values().stream()
                .filter(island -> island.getOwnerName() != null && island.getOwnerName().equalsIgnoreCase(name))
                .findFirst();
    }

    public SkyblockIsland getIslandByPosition(int blockX, int blockZ) {
        IslandMath.WorldPos center = IslandMath.getNearestIslandCenter(blockX, blockZ);
        for (SkyblockIsland island : islands.values()) {
            if (island.getCenterX() == center.x() && island.getCenterZ() == center.z()) {
                return island;
            }
        }
        return null;
    }

    public Collection<SkyblockIsland> getAllIslands() {
        return Collections.unmodifiableCollection(islands.values());
    }

    public void saveReturnLocation(ServerPlayer player) {
        if (player.level().dimension().equals(ExpeditionSkyblockMod.SKYBLOCK_DIMENSION)) {
            // Never overwrite return location with Skyblock coordinates!
            return;
        }

        PlayerReturnLocation loc = new PlayerReturnLocation(
                player.level().dimension().identifier().toString(),
                player.getX(),
                player.getY(),
                player.getZ(),
                player.getYRot(),
                player.getXRot()
        );
        playerReturns.put(player.getUUID(), loc);
        save();
    }

    public PlayerReturnLocation getReturnLocation(UUID playerUUID) {
        return playerReturns.get(playerUUID);
    }

    public void clearReturnLocation(UUID playerUUID) {
        playerReturns.remove(playerUUID);
        save();
    }

    public synchronized void save() {
        try {
            DataPayload payload = new DataPayload();
            payload.version = this.version;
            payload.nextIslandIndex = this.nextIslandIndex;
            payload.islands = new HashMap<>(this.islands);
            payload.playerReturns = new HashMap<>(this.playerReturns);

            Path tempFile = dataFile.resolveSibling(FILE_NAME + ".tmp");
            try (BufferedWriter writer = Files.newBufferedWriter(tempFile, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
                GSON.toJson(payload, writer);
            }

            try {
                Files.move(tempFile, dataFile, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tempFile, dataFile, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            LOGGER.error("Failed to save Skyblock data to {}", dataFile, e);
        }
    }

    public synchronized void load() {
        if (!Files.exists(dataFile)) {
            LOGGER.info("No existing Skyblock data file found at {}, starting fresh.", dataFile);
            return;
        }

        try (BufferedReader reader = Files.newBufferedReader(dataFile)) {
            DataPayload payload = GSON.fromJson(reader, DataPayload.class);
            if (payload != null) {
                this.version = payload.version;
                this.nextIslandIndex = payload.nextIslandIndex;
                this.islands.clear();
                if (payload.islands != null) {
                    this.islands.putAll(payload.islands);
                }
                this.playerReturns.clear();
                if (payload.playerReturns != null) {
                    this.playerReturns.putAll(payload.playerReturns);
                }
                LOGGER.info("Loaded {} Skyblock islands and {} return locations from {}",
                        this.islands.size(), this.playerReturns.size(), dataFile);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to load Skyblock data from {}", dataFile, e);
        }
    }

    private static class DataPayload {
        int version;
        int nextIslandIndex;
        Map<UUID, SkyblockIsland> islands;
        Map<UUID, PlayerReturnLocation> playerReturns;
    }
}
