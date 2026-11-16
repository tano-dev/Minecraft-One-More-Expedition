package net.onemoreexpedition.sidebar.integration;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.level.ServerPlayer;

import java.lang.reflect.Method;
import java.util.UUID;

public class FishingIntegration {
    private static boolean isPresent = false;
    private static Method getLevelMethod;
    private static Method getXpProgressMethod;
    private static Method getCurrentXpMethod;
    private static Method getRequiredXpMethod;
    private static Method getHeatMethod;
    private static Method isEnabledMethod;
    private static Method shouldDisplayHeatMethod;

    public static void init() {
        isPresent = FabricLoader.getInstance().isModLoaded("expedition-fishing");
        if (isPresent) {
            try {
                Class<?> api = Class.forName("com.expedition.fishing.api.ExpeditionFishingApi");
                getLevelMethod = api.getMethod("getLevel", UUID.class);
                getXpProgressMethod = api.getMethod("getXpProgressPercent", UUID.class);
                getCurrentXpMethod = api.getMethod("getCurrentXp", UUID.class);
                getRequiredXpMethod = api.getMethod("getRequiredXp", UUID.class);
                getHeatMethod = api.getMethod("getHeat", UUID.class);
                isEnabledMethod = api.getMethod("isEnabledForPlayer", ServerPlayer.class);
                shouldDisplayHeatMethod = api.getMethod("shouldDisplayHeatOnSidebar", ServerPlayer.class);
            } catch (Throwable t) {
                isPresent = false;
            }
        }
    }

    public static boolean isAvailableFor(ServerPlayer player) {
        if (!isPresent || player == null) return false;
        try {
            return (Boolean) isEnabledMethod.invoke(null, player);
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static int getLevel(UUID uuid) {
        if (!isPresent) return 1;
        try {
            return (Integer) getLevelMethod.invoke(null, uuid);
        } catch (Throwable ignored) {
            return 1;
        }
    }

    public static double getXpProgressPercent(UUID uuid) {
        if (!isPresent) return 0.0;
        try {
            return (Double) getXpProgressMethod.invoke(null, uuid);
        } catch (Throwable ignored) {
            return 0.0;
        }
    }

    public static long getCurrentXp(UUID uuid) {
        if (!isPresent || getCurrentXpMethod == null) return 0L;
        try {
            return (Long) getCurrentXpMethod.invoke(null, uuid);
        } catch (Throwable ignored) {
            return 0L;
        }
    }

    public static long getRequiredXp(UUID uuid) {
        if (!isPresent || getRequiredXpMethod == null) return 100L;
        try {
            return (Long) getRequiredXpMethod.invoke(null, uuid);
        } catch (Throwable ignored) {
            return 100L;
        }
    }

    public static int getHeat(UUID uuid) {
        if (!isPresent) return 0;
        try {
            return (Integer) getHeatMethod.invoke(null, uuid);
        } catch (Throwable ignored) {
            return 0;
        }
    }

    public static boolean shouldDisplayHeat(ServerPlayer player) {
        if (!isPresent || player == null) return false;
        try {
            return (Boolean) shouldDisplayHeatMethod.invoke(null, player);
        } catch (Throwable ignored) {
            return false;
        }
    }
}
