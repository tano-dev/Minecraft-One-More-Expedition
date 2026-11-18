package net.onemoreexpedition.dragon.variants;

import net.minecraft.world.BossEvent;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public enum DragonVariant {
    INFERNAL(
            "infernal",
            "Infernal",
            "§c§lInfernal Dragon",
            BossEvent.BossBarColor.RED,
            0.20,
            1500.0
    ),
    VOIDGLOOM(
            "voidgloom",
            "Voidgloom",
            "§5§lVoidgloom Dragon",
            BossEvent.BossBarColor.PURPLE,
            0.20,
            1400.0
    ),
    REVENANT(
            "revenant",
            "Revenant",
            "§a§lRevenant Dragon",
            BossEvent.BossBarColor.GREEN,
            0.20,
            1600.0
    ),
    ENLIGHTENED(
            "enlightened",
            "Enlightened",
            "§e§lEnlightened Dragon",
            BossEvent.BossBarColor.YELLOW,
            0.20,
            1800.0
    ),
    CHRONOS(
            "chronos",
            "Chronos",
            "§9§lChronos Dragon",
            BossEvent.BossBarColor.BLUE,
            0.20,
            2000.0
    );

    private final String id;
    private final String displayName;
    private final String defaultBossBarTitle;
    private final BossEvent.BossBarColor defaultBossBarColor;
    private final double defaultWeight;
    private final double defaultBaseHp;

    DragonVariant(
            String id,
            String displayName,
            String defaultBossBarTitle,
            BossEvent.BossBarColor defaultBossBarColor,
            double defaultWeight,
            double defaultBaseHp
    ) {
        this.id = id;
        this.displayName = displayName;
        this.defaultBossBarTitle = defaultBossBarTitle;
        this.defaultBossBarColor = defaultBossBarColor;
        this.defaultWeight = defaultWeight;
        this.defaultBaseHp = defaultBaseHp;
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    public String defaultBossBarTitle() {
        return defaultBossBarTitle;
    }

    public BossEvent.BossBarColor defaultBossBarColor() {
        return defaultBossBarColor;
    }

    public double defaultWeight() {
        return defaultWeight;
    }

    public double defaultBaseHp() {
        return defaultBaseHp;
    }

    public static DragonVariant fromString(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String normalized = raw.trim().toLowerCase(Locale.ROOT);
        for (DragonVariant variant : values()) {
            if (variant.id.equals(normalized) || variant.name().toLowerCase(Locale.ROOT).equals(normalized)) {
                return variant;
            }
        }
        return null;
    }

    public static BossEvent.BossBarColor parseBossBarColor(String raw, BossEvent.BossBarColor fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        String normalized = raw.trim().toUpperCase(Locale.ROOT);
        for (BossEvent.BossBarColor color : BossEvent.BossBarColor.values()) {
            if (color.name().equals(normalized)) {
                return color;
            }
        }
        return fallback;
    }

    public static List<String> ids() {
        return Arrays.stream(values()).map(DragonVariant::id).toList();
    }
}
