package net.onemoreexpedition.dragon.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.level.Level;
import net.onemoreexpedition.dragon.ExpeditionDragonMod;
import net.onemoreexpedition.dragon.config.DragonConfig;
import net.onemoreexpedition.dragon.core.DragonFightManager;
import net.onemoreexpedition.dragon.core.FightState;
import net.onemoreexpedition.dragon.variants.DragonVariant;

public final class DragonCommand {
    private DragonCommand() {
    }

    public static void register(
            CommandDispatcher<CommandSourceStack> dispatcher,
            CommandBuildContext registryAccess,
            Commands.CommandSelection environment
    ) {
        dispatcher.register(Commands.literal("dragon")
                .requires(src -> src.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
                .then(Commands.literal("spawn")
                        .then(Commands.argument("variant", StringArgumentType.word())
                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(DragonVariant.ids(), builder))
                                .executes(DragonCommand::executeSpawn)))
                .then(Commands.literal("reload")
                        .executes(DragonCommand::executeReload))
                .then(Commands.literal("status")
                        .executes(DragonCommand::executeStatus)));
    }

    private static int executeSpawn(CommandContext<CommandSourceStack> ctx) {
        String rawVariant = StringArgumentType.getString(ctx, "variant");
        DragonVariant variant = DragonVariant.fromString(rawVariant);
        if (variant == null) {
            ctx.getSource().sendFailure(Component.literal(
                    "Unknown dragon variant '" + rawVariant + "'. Valid variants: " + String.join(", ", DragonVariant.ids())
            ));
            return 0;
        }

        MinecraftServer server = ctx.getSource().getServer();
        ServerLevel endLevel = server.getLevel(Level.END);
        if (endLevel == null) {
            ctx.getSource().sendFailure(Component.literal("The End dimension (minecraft:the_end) is not loaded."));
            return 0;
        }

        FightState state = DragonFightManager.get(server).startCustomFight(endLevel, variant, false);
        if (state == null) {
            ctx.getSource().sendFailure(Component.literal("Failed to spawn " + variant.displayName() + " Dragon."));
            return 0;
        }

        double reportedMaxHp = state.getMaxHealth();
        Entity spawned = endLevel.getEntity(state.getDragonUuid());
        if (spawned instanceof EnderDragon dragon) {
            reportedMaxHp = dragon.getMaxHealth();
        }
        final double finalMaxHp = reportedMaxHp;
        ctx.getSource().sendSuccess(() -> Component.literal(
                "§aSpawned §e" + variant.displayName() + " Dragon §7(HP: " + (int) Math.round(finalMaxHp) + ", UUID: " + state.getDragonUuid() + ")"
        ), true);
        return 1;
    }

    private static int executeReload(CommandContext<CommandSourceStack> ctx) {
        DragonConfig.reload();
        MinecraftServer server = ctx.getSource().getServer();
        ExpeditionDragonMod.enforceStellarityConfig(server);
        ctx.getSource().sendSuccess(() -> Component.literal(
                "§aReloaded expedition-dragon configuration and enforced Stellarity config."
        ), true);
        return 1;
    }

    private static int executeStatus(CommandContext<CommandSourceStack> ctx) {
        MinecraftServer server = ctx.getSource().getServer();
        FightState state = DragonFightManager.get(server).getActiveFight();
        Integer stellarityFlag = ExpeditionDragonMod.readStellarityDragonScore(server);
        String stellarityText = stellarityFlag != null ? String.valueOf(stellarityFlag) : "objective-not-present";

        if (state == null || state.isEnded()) {
            ctx.getSource().sendSuccess(() -> Component.literal(
                    "§7[Expedition Dragon] §fNo active fight. §7Stellarity enable_ender_dragon=" + stellarityText
            ), false);
            return 1;
        }

        String hpText = "unknown";
        ServerLevel endLevel = server.getLevel(Level.END);
        if (endLevel != null) {
            Entity entity = endLevel.getEntity(state.getDragonUuid());
            if (entity instanceof EnderDragon dragon) {
                hpText = String.format("%.1f/%.1f", dragon.getHealth(), dragon.getMaxHealth());
            }
        }

        String summary = String.format(
                "§7[Expedition Dragon] §e%s §f| HP: §c%s §f| Anchors: §b%d §f| Minions: §d%d §f| Stellarity enable_ender_dragon=%s",
                state.getVariant().displayName(),
                hpText,
                state.getActiveAnchorCount(),
                state.getTrackedMinionCount(),
                stellarityText
        );
        ctx.getSource().sendSuccess(() -> Component.literal(summary), false);
        return 1;
    }
}
