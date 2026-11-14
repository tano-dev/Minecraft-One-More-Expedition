package net.onemoreexpedition.skyblock.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class SkyblockConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger("expedition-skyblock");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String CONFIG_FILE = "expedition-skyblock.json";

    private static SkyblockConfig instance = new SkyblockConfig();

    public int pvpCombatCooldownSeconds = 60;
    public int mobDamageCooldownSeconds = 5;

    public static SkyblockConfig get() {
        return instance;
    }

    public static void load() {
        Path configPath = FabricLoader.getInstance().getConfigDir().resolve(CONFIG_FILE);
        if (Files.exists(configPath)) {
            try (BufferedReader reader = Files.newBufferedReader(configPath)) {
                SkyblockConfig loaded = GSON.fromJson(reader, SkyblockConfig.class);
                if (loaded != null) {
                    instance = loaded;
                    LOGGER.info("[ExpeditionSkyblock] Loaded configuration: pvpCombatCooldownSeconds={}", instance.pvpCombatCooldownSeconds);
                    return;
                }
            } catch (Exception e) {
                LOGGER.error("[ExpeditionSkyblock] Failed to read {}, using defaults", CONFIG_FILE, e);
            }
        }
        save();
    }

    public static void save() {
        Path configPath = FabricLoader.getInstance().getConfigDir().resolve(CONFIG_FILE);
        try {
            Files.createDirectories(configPath.getParent());
            try (BufferedWriter writer = Files.newBufferedWriter(configPath)) {
                GSON.toJson(instance, writer);
            }
        } catch (IOException e) {
            LOGGER.error("[ExpeditionSkyblock] Failed to write {}", CONFIG_FILE, e);
        }
    }
}
