package net.onemoreexpedition.e2e.harness;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Opaque-box bridge for inspecting and dynamically loading the compiled
 * expedition-dragon JAR without any compile-time coupling to mods_src/expedition-dragon.
 */
public final class ModJarBridge {
    private final Path jarPath;

    public ModJarBridge(Path jarPath) {
        this.jarPath = jarPath;
    }

    public static ModJarBridge defaultWorkspaceJar() {
        String configured = System.getProperty("e2e.modJar");
        if (configured != null && !configured.isBlank()) {
            return new ModJarBridge(Path.of(configured));
        }
        Path cwd = Path.of("").toAbsolutePath().normalize();
        Path candidate1 = cwd.resolve("../../mods/expedition-dragon-1.0.0.jar").normalize();
        if (Files.exists(candidate1)) {
            return new ModJarBridge(candidate1);
        }
        return new ModJarBridge(Path.of("d:/Minecraft-One-More-Expedition/mods/expedition-dragon-1.0.0.jar"));
    }

    public Path jarPath() {
        return jarPath;
    }

    public boolean exists() {
        return Files.isRegularFile(jarPath);
    }

    public Path requireJar() {
        if (!exists()) {
            throw new AssertionError("Target mod JAR missing at: " + jarPath.toAbsolutePath());
        }
        return jarPath;
    }

    public URLClassLoader openClassLoader(ClassLoader parent) throws IOException {
        requireJar();
        return new URLClassLoader(new URL[]{jarPath.toUri().toURL()}, parent);
    }

    public Optional<JsonObject> readJsonEntry(String entryName) throws IOException {
        requireJar();
        try (ZipFile zip = new ZipFile(jarPath.toFile())) {
            ZipEntry entry = zip.getEntry(entryName);
            if (entry == null) {
                return Optional.empty();
            }
            try (InputStreamReader reader = new InputStreamReader(zip.getInputStream(entry), StandardCharsets.UTF_8)) {
                JsonElement el = JsonParser.parseReader(reader);
                return el.isJsonObject() ? Optional.of(el.getAsJsonObject()) : Optional.empty();
            }
        }
    }

    public List<String> listClassEntries() throws IOException {
        requireJar();
        List<String> classes = new ArrayList<>();
        try (ZipFile zip = new ZipFile(jarPath.toFile())) {
            var entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry e = entries.nextElement();
                if (!e.isDirectory() && e.getName().endsWith(".class") && !e.getName().startsWith("META-INF/")) {
                    classes.add(e.getName());
                }
            }
        }
        return classes;
    }

    public Map<String, List<String>> extractUtf8Constants() throws IOException {
        requireJar();
        Map<String, List<String>> result = new LinkedHashMap<>();
        try (ZipFile zip = new ZipFile(jarPath.toFile())) {
            var entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry e = entries.nextElement();
                if (!e.isDirectory() && e.getName().endsWith(".class") && !e.getName().startsWith("META-INF/")) {
                    byte[] bytes = zip.getInputStream(e).readAllBytes();
                    result.put(e.getName(), parseClassUtf8Constants(bytes));
                }
            }
        }
        return result;
    }

    /**
     * Pure-JVM constant pool UTF-8 extractor supporting classfile versions up to Java 25 (major 69).
     */
    public static List<String> parseClassUtf8Constants(byte[] classBytes) throws IOException {
        List<String> utf8s = new ArrayList<>();
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(classBytes))) {
            int magic = in.readInt();
            if (magic != 0xCAFEBABE) {
                throw new IOException("Invalid class magic: 0x" + Integer.toHexString(magic));
            }
            in.readUnsignedShort(); // minor
            in.readUnsignedShort(); // major
            int cpCount = in.readUnsignedShort();
            for (int i = 1; i < cpCount; i++) {
                int tag = in.readUnsignedByte();
                switch (tag) {
                    case 1 -> utf8s.add(in.readUTF());
                    case 3, 4 -> in.readInt();
                    case 5, 6 -> {
                        in.readLong();
                        i++; // Long & Double occupy two constant pool entries
                    }
                    case 7, 8, 16, 19, 20 -> in.readUnsignedShort();
                    case 9, 10, 11, 12, 17, 18 -> {
                        in.readUnsignedShort();
                        in.readUnsignedShort();
                    }
                    case 15 -> {
                        in.readUnsignedByte();
                        in.readUnsignedShort();
                    }
                    default -> throw new IOException("Unsupported constant pool tag " + tag + " at index " + i);
                }
            }
        }
        return utf8s;
    }
}
