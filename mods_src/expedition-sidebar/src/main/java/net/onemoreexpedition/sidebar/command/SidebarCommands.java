package net.onemoreexpedition.sidebar.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.onemoreexpedition.sidebar.config.SidebarConfig;
import net.onemoreexpedition.sidebar.renderer.SidebarRenderer;

public class SidebarCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher,
                                 CommandBuildContext buildContext,
                                 Commands.CommandSelection environment) {
        dispatcher.register(Commands.literal("sidebar")
                .then(Commands.literal("toggle")
                        .executes(SidebarCommands::toggleSidebar))
                .then(Commands.literal("reload")
                        .requires(Commands.hasPermission(Commands.LEVEL_ADMINS))
                        .executes(SidebarCommands::reloadConfig))
        );
    }

    private static int toggleSidebar(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        if (!source.isPlayer()) {
            source.sendFailure(Component.literal("Only players can toggle sidebar."));
            return 0;
        }

        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;

        SidebarRenderer.toggle(player.getUUID());
        boolean isHidden = SidebarRenderer.isHidden(player.getUUID());

        if (isHidden) {
            source.sendSuccess(() -> Component.literal("Sidebar has been hidden. Type /sidebar toggle to show it again.")
                    .withStyle(ChatFormatting.YELLOW), false);
        } else {
            source.sendSuccess(() -> Component.literal("Sidebar is now visible.")
                    .withStyle(ChatFormatting.GREEN), false);
        }
        return 1;
    }

    private static int reloadConfig(CommandContext<CommandSourceStack> context) {
        SidebarConfig.load();
        context.getSource().sendSuccess(() -> Component.literal("[ExpeditionSidebar] Configuration reloaded successfully.")
                .withStyle(ChatFormatting.GREEN), true);
        return 1;
    }
}
