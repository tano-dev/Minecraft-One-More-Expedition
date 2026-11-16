package net.onemoreexpedition.sidebar.integration;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.level.ServerPlayer;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class QuestIntegration {
    private static boolean isPresent = false;
    private static Method getLevelMethod;
    private static Method getXpProgressMethod;
    private static Method getCurrentXpMethod;
    private static Method getRequiredXpMethod;
    private static Method isEnabledMethod;
    private static Method getSidebarQuestLinesMethod;
    private static Method getCompletedTodayMethod;

    public static void init() {
        isPresent = FabricLoader.getInstance().isModLoaded("expedition-quests");
        if (isPresent) {
            try {
                Class<?> api = Class.forName("com.expedition.quests.api.ExpeditionQuestsApi");
                getLevelMethod = api.getMethod("getAdventureLevel", UUID.class);
                getXpProgressMethod = api.getMethod("getXpProgressPercent", UUID.class);
                getCurrentXpMethod = api.getMethod("getCurrentXp", UUID.class);
                getRequiredXpMethod = api.getMethod("getRequiredXp", UUID.class);
                isEnabledMethod = api.getMethod("isEnabledForPlayer", ServerPlayer.class);
                getSidebarQuestLinesMethod = api.getMethod("getSidebarQuestLines", ServerPlayer.class, int.class);
                getCompletedTodayMethod = api.getMethod("getCompletedTodayCount", UUID.class);
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

    public static int getAdventureLevel(UUID uuid) {
        if (!isPresent || getLevelMethod == null) return 1;
        try { return (Integer) getLevelMethod.invoke(null, uuid); } catch (Throwable ignored) { return 1; }
    }

    public static double getXpProgressPercent(UUID uuid) {
        if (!isPresent || getXpProgressMethod == null) return 0.0;
        try { return (Double) getXpProgressMethod.invoke(null, uuid); } catch (Throwable ignored) { return 0.0; }
    }

    public static long getCurrentXp(UUID uuid) {
        if (!isPresent || getCurrentXpMethod == null) return 0L;
        try { return (Long) getCurrentXpMethod.invoke(null, uuid); } catch (Throwable ignored) { return 0L; }
    }

    public static long getRequiredXp(UUID uuid) {
        if (!isPresent || getRequiredXpMethod == null) return 100L;
        try { return (Long) getRequiredXpMethod.invoke(null, uuid); } catch (Throwable ignored) { return 100L; }
    }

    public static int getCompletedToday(UUID uuid) {
        if (!isPresent || getCompletedTodayMethod == null) return 0;
        try { return (Integer) getCompletedTodayMethod.invoke(null, uuid); } catch (Throwable ignored) { return 0; }
    }

    @SuppressWarnings("unchecked")
    public static List<String> getActiveQuestLines(ServerPlayer player, int maxCount) {
        if (!isPresent || getSidebarQuestLinesMethod == null || player == null) return Collections.emptyList();
        try {
            return (List<String>) getSidebarQuestLinesMethod.invoke(null, player, maxCount);
        } catch (Throwable ignored) {
            return Collections.emptyList();
        }
    }
}
