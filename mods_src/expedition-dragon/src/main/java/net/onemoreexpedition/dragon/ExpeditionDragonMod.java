package net.onemoreexpedition.dragon;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityLevelChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerScoreboard;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.ReadOnlyScoreInfo;
import net.minecraft.world.scores.ScoreAccess;
import net.minecraft.world.scores.ScoreHolder;
import net.onemoreexpedition.dragon.altar.AltarManager;
import net.onemoreexpedition.dragon.command.DragonCommand;
import net.onemoreexpedition.dragon.config.DragonConfig;
import net.onemoreexpedition.dragon.core.DragonFightManager;
import net.onemoreexpedition.dragon.mixin.RangedAttributeAccessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExpeditionDragonMod implements ModInitializer {
    public static final String MOD_ID = "expedition-dragon";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static final double WIDENED_MAX_HEALTH = 1_000_000.0D;
    private static final String STELLARITY_DRAGON_OBJECTIVE = "stellarity.config.enable_ender_dragon";
    private static final String STELLARITY_CONFIG_HOLDER = "#stellarity.config";

    @Override
    public void onInitialize() {
        widenMaxHealthLimit();
        DragonConfig.load();

        CommandRegistrationCallback.EVENT.register(DragonCommand::register);

        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            MinecraftServer server = world.getServer();
            if (server == null) {
                return net.minecraft.world.InteractionResult.PASS;
            }
            return AltarManager.get(server).handleAttackEntity(player, world, entity);
        });

        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            MinecraftServer server = world.getServer();
            if (server == null) {
                return net.minecraft.world.InteractionResult.PASS;
            }
            return AltarManager.get(server).handleUseEntity(player, world, entity);
        });

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            DragonFightManager.get(server);
            AltarManager.get(server).onServerStarted(server);
            enforceStellarityConfig(server);
        });

        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, resourceManager, success) -> {
            if (success) {
                enforceStellarityConfig(server);
            }
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            AltarManager.get(server).onServerTick();
            DragonFightManager.get(server).onServerTick();
        });

        ServerEntityLevelChangeEvents.AFTER_PLAYER_CHANGE_LEVEL.register((player, origin, destination) ->
                DragonFightManager.get(destination.getServer()).syncBossBarPlayer(player));

        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            DragonFightManager manager = DragonFightManager.get(newPlayer.level().getServer());
            manager.removeBossBarPlayer(oldPlayer);
            manager.syncBossBarPlayer(newPlayer);
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                DragonFightManager.get(server).syncBossBarPlayer(handler.getPlayer()));

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                DragonFightManager.get(server).removeBossBarPlayer(handler.getPlayer()));

        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            DragonFightManager.get(server).shutdown();
            DragonFightManager.remove(server);
            AltarManager.remove(server);
        });

        LOGGER.info("Expedition Dragon initialized");
    }

    public static void widenMaxHealthLimit() {
        if (Attributes.MAX_HEALTH.value() instanceof RangedAttribute ranged
                && ranged.getMaxValue() < WIDENED_MAX_HEALTH) {
            ((RangedAttributeAccessor) ranged).expeditionDragon$setMaxValue(WIDENED_MAX_HEALTH);
            LOGGER.info("Widened Attributes.MAX_HEALTH cap to {}", ranged.getMaxValue());
        }
    }

    public static void enforceStellarityConfig(MinecraftServer server) {
        if (server == null || !DragonConfig.get().enforceStellarityDragonDisabled) {
            return;
        }
        ServerScoreboard scoreboard = server.getScoreboard();
        Objective objective = scoreboard.getObjective(STELLARITY_DRAGON_OBJECTIVE);
        if (objective == null) {
            return;
        }
        ScoreAccess score = scoreboard.getOrCreatePlayerScore(
                ScoreHolder.forNameOnly(STELLARITY_CONFIG_HOLDER),
                objective
        );
        score.set(0);
        LOGGER.info("Enforced {} {} = 0", STELLARITY_CONFIG_HOLDER, STELLARITY_DRAGON_OBJECTIVE);
    }

    public static Integer readStellarityDragonScore(MinecraftServer server) {
        if (server == null) {
            return null;
        }
        ServerScoreboard scoreboard = server.getScoreboard();
        Objective objective = scoreboard.getObjective(STELLARITY_DRAGON_OBJECTIVE);
        if (objective == null) {
            return null;
        }
        ReadOnlyScoreInfo scoreInfo = scoreboard.getPlayerScoreInfo(
                ScoreHolder.forNameOnly(STELLARITY_CONFIG_HOLDER),
                objective
        );
        return scoreInfo != null ? scoreInfo.value() : null;
    }
}
