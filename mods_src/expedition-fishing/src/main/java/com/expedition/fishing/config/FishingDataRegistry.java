package com.expedition.fishing.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class FishingDataRegistry {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_DIR = FabricLoader.getInstance().getConfigDir().resolve("expedition-fishing");

    public static final Map<String, JsonObject> MOBS = new ConcurrentHashMap<>();
    public static final Map<String, JsonObject> ITEMS = new ConcurrentHashMap<>();
    public static final Map<String, JsonObject> ARTIFACTS = new ConcurrentHashMap<>();
    public static final Map<String, JsonObject> RECIPES = new ConcurrentHashMap<>();
    public static final Map<String, JsonObject> BIOMES = new ConcurrentHashMap<>();
    public static final Map<String, JsonObject> BALANCE = new ConcurrentHashMap<>();

    public static synchronized void loadAll() {
        try {
            Files.createDirectories(CONFIG_DIR);
        } catch (Exception ignored) {}

        loadFile("mobs.json", MOBS);
        loadFile("items.json", ITEMS);
        loadFile("artifacts.json", ARTIFACTS);
        loadFile("recipes.json", RECIPES);
        loadFile("biomes.json", BIOMES);
        loadFile("enchants_balance.json", BALANCE);
    }

    public static double getEnchantBalance(String key, double def) {
        JsonObject enchObj = BALANCE.get("enchantments");
        return getDouble(enchObj, key, def);
    }

    public static int getAntiLagInt(String key, int def) {
        JsonObject lagObj = BALANCE.get("anti_lag");
        return getInt(lagObj, key, def);
    }

    private static void loadFile(String fileName, Map<String, JsonObject> target) {
        target.clear();
        Path externalPath = CONFIG_DIR.resolve(fileName);
        JsonObject root = null;

        try {
            if (!Files.exists(externalPath)) {
                try (InputStream in = FishingDataRegistry.class.getResourceAsStream("/data/expedition_fishing/" + fileName)) {
                    if (in != null) {
                        byte[] bytes = in.readAllBytes();
                        Files.write(externalPath, bytes);
                        root = JsonParser.parseString(new String(bytes, StandardCharsets.UTF_8)).getAsJsonObject();
                    }
                }
            } else {
                try (Reader reader = Files.newBufferedReader(externalPath, StandardCharsets.UTF_8)) {
                    root = JsonParser.parseReader(reader).getAsJsonObject();
                }
            }
        } catch (Exception e) {
            try (InputStream in = FishingDataRegistry.class.getResourceAsStream("/data/expedition_fishing/" + fileName)) {
                if (in != null) {
                    root = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
                }
            } catch (Exception ignored) {}
        }

        if (root != null) {
            for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
                if (entry.getValue().isJsonObject()) {
                    target.put(entry.getKey(), entry.getValue().getAsJsonObject());
                }
            }
        }
    }

    public static String getStr(JsonObject obj, String key, String def) {
        if (obj == null || !obj.has(key) || obj.get(key).isJsonNull()) return def;
        try {
            return obj.get(key).getAsString();
        } catch (Exception e) {
            return def;
        }
    }

    public static double getDouble(JsonObject obj, String key, double def) {
        if (obj == null || !obj.has(key) || obj.get(key).isJsonNull()) return def;
        try {
            return obj.get(key).getAsDouble();
        } catch (Exception e) {
            return def;
        }
    }

    public static int getInt(JsonObject obj, String key, int def) {
        if (obj == null || !obj.has(key) || obj.get(key).isJsonNull()) return def;
        try {
            return obj.get(key).getAsInt();
        } catch (Exception e) {
            return def;
        }
    }

    public static boolean getBool(JsonObject obj, String key, boolean def) {
        if (obj == null || !obj.has(key) || obj.get(key).isJsonNull()) return def;
        try {
            return obj.get(key).getAsBoolean();
        } catch (Exception e) {
            return def;
        }
    }
}
