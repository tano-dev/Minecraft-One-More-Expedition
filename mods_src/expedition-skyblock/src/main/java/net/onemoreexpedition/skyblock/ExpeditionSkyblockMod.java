package net.onemoreexpedition.skyblock;

import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import net.onemoreexpedition.skyblock.command.SkyblockCommands;
import net.onemoreexpedition.skyblock.config.SkyblockConfig;
import net.onemoreexpedition.skyblock.data.SkyblockDataManager;
import net.onemoreexpedition.skyblock.listener.SkyblockEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExpeditionSkyblockMod implements DedicatedServerModInitializer {
    public static final String MOD_ID = "expedition-skyblock";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static final ResourceKey<Level> SKYBLOCK_DIMENSION = ResourceKey.create(
            Registries.DIMENSION,
            Identifier.fromNamespaceAndPath("expedition", "skyblock")
    );

    public static final ResourceKey<DimensionType> SKYBLOCK_DIMENSION_TYPE = ResourceKey.create(
            Registries.DIMENSION_TYPE,
            Identifier.fromNamespaceAndPath("expedition", "skyblock")
    );

    @Override
    public void onInitializeServer() {
        LOGGER.info("[ExpeditionSkyblock] Initializing Expedition Skyblock Fabric mod for Minecraft 26.3...");

        // Load configuration
        SkyblockConfig.load();

        // Register Skyblock commands (/is, /return, /is return, /is sethome, /visit, /invite, /is lock, /is unlock, /is kick)
        SkyblockCommands.register();

        // Register Skyblock anti-grief, PvP cancellation, boundary containment, and void rescue listeners
        SkyblockEventListener.register();

        // Register ServerLifecycleEvents to load island persistence on server start
        ServerLifecycleEvents.SERVER_STARTED.register(SkyblockDataManager::initialize);

        LOGGER.info("[ExpeditionSkyblock] Expedition Skyblock initialized successfully.");
    }
}
