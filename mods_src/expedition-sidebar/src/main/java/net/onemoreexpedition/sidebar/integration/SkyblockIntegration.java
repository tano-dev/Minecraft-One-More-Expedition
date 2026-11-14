package net.onemoreexpedition.sidebar.integration;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.onemoreexpedition.skyblock.data.SkyblockDataManager;
import net.onemoreexpedition.skyblock.data.SkyblockIsland;
import net.onemoreexpedition.skyblock.listener.SkyblockEventListener;

import java.util.UUID;

public class SkyblockIntegration {
    private static boolean isPresent = false;

    public static void init() {
        isPresent = FabricLoader.getInstance().isModLoaded("expedition-skyblock");
    }

    public static boolean isAvailable() {
        return isPresent;
    }

    public static int getRemainingPvpSeconds(UUID uuid) {
        if (!isPresent) return 0;
        try {
            return SkyblockEventListener.getRemainingPvpSeconds(uuid);
        } catch (Throwable ignored) {}
        return 0;
    }

    public static boolean isInPvpCombat(UUID uuid) {
        if (!isPresent) return false;
        try {
            return SkyblockEventListener.isInPvpCombat(uuid);
        } catch (Throwable ignored) {}
        return false;
    }

    public static String getIslandStatus(ServerPlayer player) {
        if (!isPresent || player == null) return null;
        try {
            ServerLevel level = player.level();
            if (level.dimension().identifier().toString().equals("expedition-skyblock:skyblock_world")) {
                SkyblockDataManager manager = SkyblockDataManager.getInstance();
                if (manager != null) {
                    BlockPos pos = player.blockPosition();
                    SkyblockIsland island = manager.getIslandByPosition(pos.getX(), pos.getZ());
                    if (island != null) {
                        if (island.isOwner(player.getUUID())) {
                            return "Your Island";
                        } else {
                            return island.getOwnerName();
                        }
                    }
                }
            }
        } catch (Throwable ignored) {}
        return null;
    }
}
