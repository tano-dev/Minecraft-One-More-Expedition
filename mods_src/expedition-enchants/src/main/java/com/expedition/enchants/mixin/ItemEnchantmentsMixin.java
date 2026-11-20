package com.expedition.enchants.mixin;

import com.expedition.enchants.config.EnchantRollConfig;
import com.expedition.enchants.tooltip.EnchantTooltipFormatter;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

@Mixin(ItemEnchantments.class)
public class ItemEnchantmentsMixin {
    @Inject(method = "addToTooltip", at = @At("HEAD"), cancellable = true)
    private void expedition$formatEnchantTooltip(
            Item.TooltipContext context,
            Consumer<Component> tooltip,
            TooltipFlag tooltipFlag,
            DataComponentGetter dataComponentGetter,
            CallbackInfo ci
    ) {
        if (!EnchantRollConfig.get().tooltip.enabled) {
            return;
        }
        ItemEnchantments self = (ItemEnchantments) (Object) this;
        if (self.isEmpty()) {
            return;
        }
        boolean isBook = (dataComponentGetter != null && dataComponentGetter.get(net.minecraft.core.component.DataComponents.STORED_ENCHANTMENTS) != null);
        if (EnchantTooltipFormatter.formatTooltip(self, context, tooltip, isBook)) {
            ci.cancel();
        }
    }
}
