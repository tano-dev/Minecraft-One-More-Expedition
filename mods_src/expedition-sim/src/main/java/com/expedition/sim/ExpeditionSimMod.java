package com.expedition.sim;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.commands.arguments.item.ItemInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExpeditionSimMod implements ModInitializer {
    public static final String MOD_ID = "expedition-sim";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(
                Commands.literal("sim")
                    .requires(Commands.hasPermission(Commands.LEVEL_ADMINS))
                    .then(Commands.literal("enchant")
                        .then(Commands.argument("item", ItemArgument.item(registryAccess))
                            .then(Commands.argument("count", IntegerArgumentType.integer(1, 1_000_000))
                                .then(Commands.argument("level", IntegerArgumentType.integer(1, 50))
                                    .executes(ctx -> {
                                        ItemInput item = ItemArgument.getItem(ctx, "item");
                                        int count = IntegerArgumentType.getInteger(ctx, "count");
                                        int level = IntegerArgumentType.getInteger(ctx, "level");
                                        return EnchantSimulator.run(ctx.getSource(), item, count, level);
                                    })
                                )
                            )
                        )
                    )
                    .then(Commands.literal("enchant_all")
                        .then(Commands.argument("count", IntegerArgumentType.integer(1, 1_000_000))
                            .then(Commands.argument("level", IntegerArgumentType.integer(1, 50))
                                .executes(ctx -> {
                                    int count = IntegerArgumentType.getInteger(ctx, "count");
                                    int level = IntegerArgumentType.getInteger(ctx, "level");
                                    return EnchantSimulator.runAll(ctx.getSource(), count, level);
                                })
                            )
                        )
                    )
            );
        });
        LOGGER.info("[ExpeditionSim] Initialized OP simulator commands (/sim enchant, /sim enchant_all).");
    }
}
