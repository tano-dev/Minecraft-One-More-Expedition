package com.expedition.quests;

import com.expedition.quests.command.QuestCommand;
import com.expedition.quests.config.QuestsConfig;
import com.expedition.quests.data.QuestDataStorage;
import com.expedition.quests.exploration.ClaimedLocationsRegistry;
import com.expedition.quests.exploration.UnexploredStructureLocator;
import com.expedition.quests.tracker.QuestProgressListener;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExpeditionQuestsMod implements ModInitializer {
    public static final String MOD_ID = "expedition-quests";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private int tickCounter = 0;

    @Override
    public void onInitialize() {
        QuestsConfig.load();
        ClaimedLocationsRegistry.load();
        QuestProgressListener.registerEvents();

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            QuestCommand.register(dispatcher);
        });

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            UnexploredStructureLocator.scanRegistries(server);
            LOGGER.info("[Expedition Quests] Indexed Biomes: {}, Structures: {}, Dungeons: {}",
                    UnexploredStructureLocator.BIOMES_BY_DIM.values().stream().mapToInt(java.util.List::size).sum(),
                    UnexploredStructureLocator.STRUCTURES_BY_DIM.values().stream().mapToInt(java.util.List::size).sum(),
                    UnexploredStructureLocator.DUNGEONS_BY_DIM.values().stream().mapToInt(java.util.List::size).sum());
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            tickCounter++;
            QuestProgressListener.tickServer(server, tickCounter);
        });

        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            QuestDataStorage.saveAllSync();
            ClaimedLocationsRegistry.saveIfDirty();
        });
    }
}
