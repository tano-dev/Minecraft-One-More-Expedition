package com.expedition.fishing;

import com.expedition.fishing.biome.BiomeResolver;
import com.expedition.fishing.command.FishingCommand;
import com.expedition.fishing.config.FishingDataRegistry;
import com.expedition.fishing.data.FishingPlayerData;
import com.expedition.fishing.data.PlayerDataStorage;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityCombatEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExpeditionFishingMod implements ModInitializer {
    public static final String MOD_ID = "expedition-fishing";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private int tickCounter = 0;

    @Override
    public void onInitialize() {
        FishingDataRegistry.loadAll();
        LOGGER.info("[Expedition Fishing] Loaded {} Sea Creatures, {} Items, {} Artifacts, {} Recipes, {} Biomes.",
                FishingDataRegistry.MOBS.size(),
                FishingDataRegistry.ITEMS.size(),
                FishingDataRegistry.ARTIFACTS.size(),
                FishingDataRegistry.RECIPES.size(),
                FishingDataRegistry.BIOMES.size());

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            FishingCommand.register(dispatcher);
        });

        // Blood in the Water enchant trigger on Sea Creature kill
        ServerEntityCombatEvents.AFTER_KILLED_OTHER_ENTITY.register((world, entity, killedEntity, damageSource) -> {
            if (entity instanceof ServerPlayer sp && !BiomeResolver.isSkyblockWorld(world)) {
                if (killedEntity.hasCustomName()) {
                    FishingPlayerData data = PlayerDataStorage.get(sp.getUUID());
                    data.bloodBuffUntilTick = world.getGameTime() + 300L; // 15 seconds
                }
            }
        });

        // Heat natural cooling, Overheat penalty & Sea Creature Auto-Despawn (every 2 seconds = 40 ticks)
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            tickCounter++;
            if (tickCounter % 40 != 0) return;

            com.expedition.fishing.mechanics.FishingEngine.tickDespawnManager(server);

            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                if (BiomeResolver.isSkyblockWorld(player.level())) continue;
                FishingPlayerData data = PlayerDataStorage.get(player.getUUID());
                if (data.heat > 0) {
                    if (data.heat >= 90.0) {
                        player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 0, true, false));
                        player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0, true, false));
                    }
                    // Passive cooling when not actively gaining heat
                    data.addHeat(-0.5);
                }
            }
        });

        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            PlayerDataStorage.saveAllSync();
        });
    }
}
