package dev.silverandro.itemflexer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;

public class ItemFlexerConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File FILE = new File("config/itemflexer.json");

    public int cooldown = 0; // cooldown in ticks (20 ticks = 1 second)
    public String commandName = "flex";
    public String chatMessage = "%player% is flexing their %item%";
    public String chatMessageWithCount = "%player% is flexing their %count% %item%";
    public String failureNoItem = "Can't flex empty item";
    public String failureOnCooldown = "On Cooldown: %cooldown% seconds left";

    private static ItemFlexerConfig INSTANCE = new ItemFlexerConfig();

    public static ItemFlexerConfig get() {
        return INSTANCE;
    }

    public static void load() {
        if (!FILE.exists()) {
            save();
            return;
        }
        try (FileReader reader = new FileReader(FILE, StandardCharsets.UTF_8)) {
            ItemFlexerConfig loaded = GSON.fromJson(reader, ItemFlexerConfig.class);
            if (loaded != null) {
                INSTANCE = loaded;
            }
        } catch (Exception e) {
            System.err.println("[ItemFlexer] Failed to load config: " + e.getMessage());
        }
    }

    public static void save() {
        try {
            if (FILE.getParentFile() != null && !FILE.getParentFile().exists()) {
                FILE.getParentFile().mkdirs();
            }
            try (FileWriter writer = new FileWriter(FILE, StandardCharsets.UTF_8)) {
                GSON.toJson(INSTANCE, writer);
            }
        } catch (Exception e) {
            System.err.println("[ItemFlexer] Failed to save config: " + e.getMessage());
        }
    }
}
