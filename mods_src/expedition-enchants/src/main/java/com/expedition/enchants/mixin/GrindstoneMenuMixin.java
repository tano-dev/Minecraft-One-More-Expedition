package com.expedition.enchants.mixin;

import com.expedition.enchants.tooltip.EnchantTooltipFormatter;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.GrindstoneMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GrindstoneMenu.class)
public abstract class GrindstoneMenuMixin {
    @Shadow
    @Final
    private Container resultSlots;

    @Inject(method = "createResult", at = @At("RETURN"))
    private void expedition$updateGrindstoneResult(CallbackInfo ci) {
        ItemStack result = this.resultSlots.getItem(0);
        if (result.isEmpty()) return;
        ItemEnchantments enchants = result.get(DataComponents.ENCHANTMENTS);
        if (enchants == null || enchants.isEmpty()) {
            TooltipDisplay display = result.get(DataComponents.TOOLTIP_DISPLAY);
            if (display != null) {
                result.set(DataComponents.TOOLTIP_DISPLAY, display.withHidden(DataComponents.ENCHANTMENTS, false).withHidden(DataComponents.STORED_ENCHANTMENTS, false));
            }
            result.remove(DataComponents.LORE);
        } else {
            EnchantTooltipFormatter.applyToItemStack(result);
        }
    }
}
