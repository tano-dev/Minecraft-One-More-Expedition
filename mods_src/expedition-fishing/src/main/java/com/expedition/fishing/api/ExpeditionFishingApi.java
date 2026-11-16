package com.expedition.fishing.api;

import com.expedition.fishing.biome.BiomeResolver;
import com.expedition.fishing.data.FishingPlayerData;
import com.expedition.fishing.data.PlayerDataStorage;
import com.expedition.fishing.item.FishingItemFactory;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Public API for cross-mod integration (expedition-sidebar, expedition-sim, economycraft, and future Quest Mod).
 */
public class ExpeditionFishingApi {

    @FunctionalInterface
    public interface CatchListener {
        void onCatch(ServerPlayer player, String catchId, String category, String rarity, double weightKg, double xpEarned);
    }

    @FunctionalInterface
    public interface LevelUpListener {
        void onLevelUp(ServerPlayer player, int oldLevel, int newLevel);
    }

    private static final List<CatchListener> CATCH_LISTENERS = new CopyOnWriteArrayList<>();
    private static final List<LevelUpListener> LEVEL_UP_LISTENERS = new CopyOnWriteArrayList<>();

    public static void registerCatchListener(CatchListener listener) {
        if (listener != null) CATCH_LISTENERS.add(listener);
    }

    public static void registerLevelUpListener(LevelUpListener listener) {
        if (listener != null) LEVEL_UP_LISTENERS.add(listener);
    }

    public static void fireCatch(ServerPlayer player, String catchId, String category, String rarity, double weightKg, double xpEarned) {
        for (CatchListener l : CATCH_LISTENERS) {
            try {
                l.onCatch(player, catchId, category, rarity, weightKg, xpEarned);
            } catch (Throwable ignored) {}
        }
    }

    public static void fireLevelUp(ServerPlayer player, int oldLevel, int newLevel) {
        for (LevelUpListener l : LEVEL_UP_LISTENERS) {
            try {
                l.onLevelUp(player, oldLevel, newLevel);
            } catch (Throwable ignored) {}
        }
    }

    public static int getLevel(UUID uuid) {
        return PlayerDataStorage.get(uuid).level;
    }

    public static double getXpProgressPercent(UUID uuid) {
        return PlayerDataStorage.get(uuid).getXpProgressPercent();
    }

    public static long getCurrentXp(UUID uuid) {
        return Math.round(PlayerDataStorage.get(uuid).xp);
    }

    public static long getRequiredXp(UUID uuid) {
        FishingPlayerData d = PlayerDataStorage.get(uuid);
        if (d.level >= 50) return 0L;
        return Math.round(FishingPlayerData.getXpForNextLevel(d.level));
    }

    public static int getHeat(UUID uuid) {
        return (int) Math.round(PlayerDataStorage.get(uuid).heat);
    }

    public static int getTotalCatches(UUID uuid) {
        return PlayerDataStorage.get(uuid).totalCatches;
    }

    public static int getMobCatchCount(UUID uuid, String mobId) {
        return PlayerDataStorage.get(uuid).mobCatches.getOrDefault(mobId, 0);
    }

    public static double getBestWeight(UUID uuid, String mobId) {
        return PlayerDataStorage.get(uuid).bestWeights.getOrDefault(mobId, 0.0);
    }

    public static boolean isEnabledForPlayer(ServerPlayer player) {
        if (player == null) return false;
        return !BiomeResolver.isSkyblockWorld(player.level());
    }

    public static boolean shouldDisplayHeatOnSidebar(ServerPlayer player) {
        if (!isEnabledForPlayer(player)) return false;
        FishingPlayerData data = PlayerDataStorage.get(player.getUUID());
        if (data.heat >= 1.0) return true;
        return FishingItemFactory.isLavaCapableRod(player.getMainHandItem())
                || FishingItemFactory.isLavaCapableRod(player.getOffhandItem());
    }
}
