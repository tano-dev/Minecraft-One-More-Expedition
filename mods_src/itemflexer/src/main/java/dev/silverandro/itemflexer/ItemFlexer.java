package dev.silverandro.itemflexer;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ItemFlexer implements ModInitializer {
    public static final String MOD_ID = "itemflexer";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static final Map<UUID, Integer> COOLDOWNS = new ConcurrentHashMap<>();

    @Override
    public void onInitialize() {
        LOGGER.info("[ItemFlexer] Initializing ItemFlexer for Minecraft 26.3...");
        ItemFlexerConfig.load();

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (Map.Entry<UUID, Integer> entry : COOLDOWNS.entrySet()) {
                int cd = entry.getValue();
                if (cd > 0) {
                    entry.setValue(cd - 1);
                } else {
                    COOLDOWNS.remove(entry.getKey());
                }
            }
        });

        CommandRegistrationCallback.EVENT.register(this::registerCommands);
        LOGGER.info("[ItemFlexer] Initialized successfully. Command: /{}", ItemFlexerConfig.get().commandName);
    }

    private void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher,
                                  CommandBuildContext buildContext,
                                  Commands.CommandSelection environment) {
        String cmd = ItemFlexerConfig.get().commandName;
        if (cmd == null || cmd.isBlank()) cmd = "flex";

        dispatcher.register(Commands.literal(cmd)
                .executes(ctx -> flexItem(ctx, false))
                .then(Commands.argument("includeCount", BoolArgumentType.bool())
                        .executes(ctx -> flexItem(ctx, BoolArgumentType.getBool(ctx, "includeCount"))))
        );
    }

    private int flexItem(CommandContext<CommandSourceStack> ctx, boolean includeCount) {
        CommandSourceStack source = ctx.getSource();
        if (!source.isPlayer()) {
            source.sendFailure(Component.literal("Only players can flex items."));
            return 0;
        }

        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;

        ItemFlexerConfig cfg = ItemFlexerConfig.get();
        UUID uuid = player.getUUID();

        // Check cooldown
        int currentCooldown = COOLDOWNS.getOrDefault(uuid, 0);
        if (currentCooldown > 0) {
            float secLeft = currentCooldown / 20.0f;
            String msg = cfg.failureOnCooldown.replace("%cooldown%", String.format("%.1f", secLeft));
            source.sendFailure(Component.literal(msg).withStyle(ChatFormatting.RED));
            return 0;
        }

        ItemStack mainHand = player.getMainHandItem();
        if (mainHand.isEmpty()) {
            source.sendFailure(Component.literal(cfg.failureNoItem).withStyle(ChatFormatting.RED));
            return 0;
        }

        // Apply cooldown
        if (cfg.cooldown > 0) {
            COOLDOWNS.put(uuid, cfg.cooldown);
        }

        // Build item hover component
        Component itemHoverComponent = createItemHoverComponent(mainHand);

        // Format message
        String template = includeCount ? cfg.chatMessageWithCount : cfg.chatMessage;
        Component broadcastMessage = buildFormattedMessage(template, player, mainHand, itemHoverComponent, includeCount);

        // Broadcast to all players
        source.getServer().getPlayerList().broadcastSystemMessage(broadcastMessage, false);
        return 1;
    }

    private Component createItemHoverComponent(ItemStack stack) {
        // [ItemName] styled with rarity/custom name and ShowItem hover event
        MutableComponent itemText = Component.literal("[")
                .append(stack.getDisplayName())
                .append(Component.literal("]"));

        // Attach ShowItem hover event using ItemStackTemplate (26.3 standard)
        try {
            ItemStackTemplate template = ItemStackTemplate.fromNonEmptyStack(stack);
            HoverEvent hoverEvent = new HoverEvent.ShowItem(template);
            return itemText.setStyle(itemText.getStyle().withHoverEvent(hoverEvent));
        } catch (Throwable e) {
            // Fallback if template creation fails
            return itemText;
        }
    }

    private Component buildFormattedMessage(String template, ServerPlayer player, ItemStack stack,
                                           Component itemComponent, boolean includeCount) {
        // Parse placeholders: %player%, %item%, %count%
        MutableComponent result = Component.empty();

        // Default: "%player% is flexing their %count% %item%" or "%player% is flexing their %item%"
        String[] parts = template.split("(?=%player%)|(?<=%player%)|(?=%item%)|(?<=%item%)|(?=%count%)|(?<=%count%)");

        for (String part : parts) {
            if ("%player%".equals(part)) {
                result.append(player.getDisplayName());
            } else if ("%item%".equals(part)) {
                result.append(itemComponent);
            } else if ("%count%".equals(part)) {
                result.append(Component.literal(String.valueOf(stack.getCount())));
            } else {
                result.append(Component.literal(part));
            }
        }

        return result;
    }
}
