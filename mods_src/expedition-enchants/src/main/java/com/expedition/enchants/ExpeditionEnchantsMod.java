package com.expedition.enchants;

import com.expedition.enchants.command.EnchantRigManager;
import com.expedition.enchants.config.EnchantDatabaseLoader;
import com.expedition.enchants.config.EnchantRollConfig;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;

public class ExpeditionEnchantsMod implements ModInitializer {
    public static final String MOD_ID = "expedition-enchants";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        EnchantRollConfig.load();
        EnchantDatabaseLoader.load();
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(
                Commands.literal("enchantsreload")
                    .requires(Commands.hasPermission(Commands.LEVEL_ADMINS))
                    .executes(ctx -> {
                        EnchantRollConfig.load();
                        EnchantDatabaseLoader.load();
                        ctx.getSource().sendSuccess(
                            () -> Component.literal("§a[ExpeditionEnchants] Reloaded config/expedition-enchants.json"),
                            false
                        );
                        return 1;
                    })
            );

            dispatcher.register(
                Commands.literal("enchantsformat")
                    .requires(Commands.hasPermission(Commands.LEVEL_ADMINS))
                    .executes(ctx -> {
                        ServerPlayer sp = ctx.getSource().getPlayerOrException();
                        net.minecraft.world.item.ItemStack mainHand = sp.getMainHandItem();
                        if (mainHand.isEmpty()) {
                            ctx.getSource().sendFailure(Component.literal("§cHold an enchanted item or book in your main hand."));
                            return 0;
                        }
                        boolean ok = com.expedition.enchants.tooltip.EnchantTooltipFormatter.applyToItemStack(mainHand);
                        if (ok) {
                            ctx.getSource().sendSuccess(() -> Component.literal("§a[ExpeditionEnchants] Formatted enchantments on held item!"), false);
                            return 1;
                        } else {
                            ctx.getSource().sendFailure(Component.literal("§cItem has no enchantments to format or lore sync is disabled."));
                            return 0;
                        }
                    })
            );

            dispatcher.register(
                Commands.literal("enchantrig")
                    .requires(Commands.hasPermission(Commands.LEVEL_ADMINS))
                    .then(Commands.argument("target", EntityArgument.player())
                        .then(Commands.argument("type", StringArgumentType.word())
                            .suggests((ctx, builder) -> {
                                builder.suggest("godroll");
                                builder.suggest("epic");
                                builder.suggest("rare");
                                builder.suggest("clear");
                                return builder.buildFuture();
                            })
                            .executes(ctx -> executeRig(
                                ctx.getSource(),
                                EntityArgument.getPlayer(ctx, "target"),
                                StringArgumentType.getString(ctx, "type"),
                                1
                            ))
                            .then(Commands.argument("uses", IntegerArgumentType.integer(1, 100))
                                .executes(ctx -> executeRig(
                                    ctx.getSource(),
                                    EntityArgument.getPlayer(ctx, "target"),
                                    StringArgumentType.getString(ctx, "type"),
                                    IntegerArgumentType.getInteger(ctx, "uses")
                                ))
                            )
                        )
                    )
            );
        });
        LOGGER.info("[ExpeditionEnchants] Initialized custom enchant roll engine, Reveal Enchants UI & /enchantrig.");
    }

    private static int executeRig(net.minecraft.commands.CommandSourceStack source, ServerPlayer target, String typeStr, int uses) {
        EnchantRigManager.RigType rig = switch (typeStr.toLowerCase(Locale.ROOT)) {
            case "godroll" -> EnchantRigManager.RigType.GODROLL;
            case "epic" -> EnchantRigManager.RigType.FORCE_EPIC;
            case "rare" -> EnchantRigManager.RigType.FORCE_RARE;
            case "clear" -> EnchantRigManager.RigType.NONE;
            default -> null;
        };
        if (rig == null) {
            source.sendFailure(Component.literal("§cInvalid rig type. Use: godroll | epic | rare | clear"));
            return 0;
        }
        EnchantRigManager.setRig(target.getUUID(), rig, uses);
        if (rig == EnchantRigManager.RigType.NONE) {
            source.sendSuccess(() -> Component.literal("§e[Rig] Cleared enchant rig for §f" + target.getName().getString()), false);
        } else {
            source.sendSuccess(() -> Component.literal(String.format(
                "§a[Rig] Set §6%s §afor §f%s §7(%d use(s))", rig.name(), target.getName().getString(), uses
            )), false);
        }
        return 1;
    }
}
