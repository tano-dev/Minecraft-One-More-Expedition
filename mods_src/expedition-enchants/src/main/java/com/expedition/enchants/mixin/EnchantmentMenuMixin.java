package com.expedition.enchants.mixin;

import com.expedition.enchants.command.EnchantRigManager;
import com.expedition.enchants.config.EnchantRollConfig;
import com.expedition.enchants.roll.CustomEnchantRoller;
import com.expedition.enchants.ui.RevealEnchantsMenu;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EnchantmentMenu.class)
public class EnchantmentMenuMixin {
    @Shadow
    @Final
    private Container enchantSlots;

    @Inject(method = "clickMenuButton", at = @At("HEAD"))
    private void expedition$beforeClickMenuButton(Player player, int id, CallbackInfoReturnable<Boolean> cir) {
        if (player instanceof ServerPlayer sp) {
            EnchantRigManager.setCurrentPlayer(sp);
        }
    }

    @Inject(method = "clickMenuButton", at = @At("RETURN"))
    private void expedition$openRevealUi(Player player, int id, CallbackInfoReturnable<Boolean> cir) {
        try {
            if (!cir.getReturnValue()) return;
            boolean isGodroll = CustomEnchantRoller.consumeLastRollGodroll();
            if (!EnchantRollConfig.get().enableRevealUi) return;
            if (!(player instanceof ServerPlayer sp)) return;

            ItemStack enchanted = this.enchantSlots.getItem(0);
            if (!enchanted.isEmpty()) {
                com.expedition.enchants.tooltip.EnchantTooltipFormatter.applyToItemStack(enchanted);
                RevealEnchantsMenu.open(sp, enchanted, isGodroll);
                try {
                    Class<?> qApi = Class.forName("com.expedition.quests.api.ExpeditionQuestsApi");
                    qApi.getMethod("onItemEnchanted", ServerPlayer.class).invoke(null, sp);
                } catch (Throwable ignored) {}
            }
        } finally {
            EnchantRigManager.setCurrentPlayer(null);
        }
    }
}
