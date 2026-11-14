package net.onemoreexpedition.sidebar.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SidebarConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File CONFIG_FILE = new File("config/expedition-sidebar.json");

    public boolean enabled = true;
    public int updateIntervalTicks = 20; // 1s
    public int tipRotationSeconds = 8;
    public String serverAddress = "mckhtn.redirectme.net:2026";
    public List<TipEntry> tips = new ArrayList<>(Arrays.asList(
            new TipEntry("/is", "Return to your Skyblock island"),
            new TipEntry("/is visit <player>", "Visit another player's island"),
            new TipEntry("/return", "Return from visiting an island"),
            new TipEntry("/daily", "Claim your daily coin rewards"),
            new TipEntry("/shop", "Buy and sell items anytime"),
            new TipEntry("/ah", "Browse player auction house"),
            new TipEntry("/eco balance", "Check your current coins balance")
    ));

    public static class TipEntry {
        public String command;
        public String description;

        public TipEntry() {}

        public TipEntry(String command, String description) {
            this.command = command;
            this.description = description;
        }
    }

    private static SidebarConfig INSTANCE = new SidebarConfig();

    public static SidebarConfig get() {
        return INSTANCE;
    }

    public static void load() {
        if (!CONFIG_FILE.exists()) {
            save();
            return;
        }
        try (FileReader reader = new FileReader(CONFIG_FILE, StandardCharsets.UTF_8)) {
            SidebarConfig loaded = GSON.fromJson(reader, SidebarConfig.class);
            if (loaded != null) {
                INSTANCE = loaded;
            }
        } catch (Exception e) {
            System.err.println("[ExpeditionSidebar] Failed to load config: " + e.getMessage());
        }
    }

    public static void save() {
        try {
            if (CONFIG_FILE.getParentFile() != null && !CONFIG_FILE.getParentFile().exists()) {
                CONFIG_FILE.getParentFile().mkdirs();
            }
            try (FileWriter writer = new FileWriter(CONFIG_FILE, StandardCharsets.UTF_8)) {
                GSON.toJson(INSTANCE, writer);
            }
        } catch (Exception e) {
            System.err.println("[ExpeditionSidebar] Failed to save config: " + e.getMessage());
        }
    }
}
