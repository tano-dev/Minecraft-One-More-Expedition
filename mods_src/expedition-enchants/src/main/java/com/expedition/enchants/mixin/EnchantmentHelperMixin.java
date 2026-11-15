package com.expedition.enchants.mixin;

import com.expedition.enchants.config.EnchantRollConfig;
import com.expedition.enchants.roll.CustomEnchantRoller;
import net.minecraft.core.Holder;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.stream.Stream;

@Mixin(EnchantmentHelper.class)
public class EnchantmentHelperMixin {
    @Inject(
        method = "selectEnchantment",
        at = @At("HEAD"),
        cancellable = true
    )
    private static void expedition$customSelectEnchantment(
        RandomSource random,
        ItemStack stack,
        int level,
        Stream<Holder<Enchantment>> possibleEnchantments,
        CallbackInfoReturnable<List<EnchantmentInstance>> cir
    ) {
        if (!EnchantRollConfig.get().enabled) return;
        cir.setReturnValue(CustomEnchantRoller.roll(random, stack, level, possibleEnchantments));
    }
}
