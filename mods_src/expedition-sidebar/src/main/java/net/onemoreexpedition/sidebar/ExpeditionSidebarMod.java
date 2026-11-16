package net.onemoreexpedition.sidebar;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.onemoreexpedition.sidebar.command.SidebarCommands;
import net.onemoreexpedition.sidebar.config.SidebarConfig;
import net.onemoreexpedition.sidebar.integration.EconomyIntegration;
import net.onemoreexpedition.sidebar.integration.FishingIntegration;
import net.onemoreexpedition.sidebar.integration.SkyblockIntegration;
import net.onemoreexpedition.sidebar.renderer.SidebarRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExpeditionSidebarMod implements ModInitializer {
    public static final String MOD_ID = "expedition-sidebar";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private int tickCounter = 0;

    @Override
    public void onInitialize() {
        LOGGER.info("[ExpeditionSidebar] Initializing Expedition Sidebar Mod for 26.3...");

        SidebarConfig.load();
        EconomyIntegration.init();
        SkyblockIntegration.init();
        FishingIntegration.init();

        CommandRegistrationCallback.EVENT.register(SidebarCommands::register);

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            SidebarRenderer.onPlayerDisconnect(handler.getPlayer().getUUID());
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            tickCounter++;
            int interval = Math.max(1, SidebarConfig.get().updateIntervalTicks);
            if (tickCounter % interval == 0) {
                SidebarRenderer.tick(server);
            }
        });

        LOGGER.info("[ExpeditionSidebar] Initialized successfully. Economy integration: {}, Skyblock integration: {}",
                EconomyIntegration.isAvailable(), SkyblockIntegration.isAvailable());
    }
}
