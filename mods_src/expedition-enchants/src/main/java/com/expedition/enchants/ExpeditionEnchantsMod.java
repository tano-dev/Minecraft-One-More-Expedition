package com.expedition.enchants;

import com.expedition.enchants.config.EnchantRollConfig;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExpeditionEnchantsMod implements ModInitializer {
    public static final String MOD_ID = "expedition-enchants";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        EnchantRollConfig.load();
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(
                Commands.literal("enchantsreload")
                    .requires(Commands.hasPermission(Commands.LEVEL_ADMINS))
                    .executes(ctx -> {
                        EnchantRollConfig.load();
                        ctx.getSource().sendSuccess(
                            () -> Component.literal("§a[ExpeditionEnchants] Reloaded config/expedition-enchants.json"),
                            false
                        );
                        return 1;
                    })
            );
        });
        LOGGER.info("[ExpeditionEnchants] Initialized custom enchant roll engine & Reveal Enchants UI.");
    }
}
