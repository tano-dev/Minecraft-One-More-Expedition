package net.onemoreexpedition.sidebar.integration;

import com.reazip.economycraft.EconomyCraft;
import com.reazip.economycraft.EconomyManager;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;

import java.util.UUID;

public class EconomyIntegration {
    private static boolean isPresent = false;

    public static void init() {
        isPresent = FabricLoader.getInstance().isModLoaded("economycraft");
    }

    public static boolean isAvailable() {
        return isPresent;
    }

    public static long getBalance(MinecraftServer server, UUID uuid) {
        if (!isPresent || server == null) return 0;
        try {
            EconomyManager manager = EconomyCraft.getManager(server);
            if (manager != null) {
                return manager.peekBalance(uuid);
            }
        } catch (Throwable ignored) {}
        return 0;
    }

    public static boolean hasClaimedDaily(MinecraftServer server, UUID uuid) {
        if (!isPresent || server == null) return false;
        try {
            EconomyManager manager = EconomyCraft.getManager(server);
            if (manager != null) {
                return manager.hasClaimedDailyToday(uuid);
            }
        } catch (Throwable ignored) {}
        return false;
    }
}
