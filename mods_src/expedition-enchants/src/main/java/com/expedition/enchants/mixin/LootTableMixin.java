package com.expedition.enchants.mixin;

import com.expedition.enchants.config.EnchantRollConfig;
import com.expedition.enchants.roll.StructureChestBookRoller;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LootTable.class)
public abstract class LootTableMixin {

    @Inject(method = "fill", at = @At("TAIL"))
    private void expeditionEnchants$replaceChestEnchantedGear(Container container, LootParams params, long seed, CallbackInfo ci) {
        EnchantRollConfig cfg = EnchantRollConfig.get();
        if (!cfg.enabled || !cfg.enableStructureChestBookReplacement) return;
        if (container == null || params == null) return;

        ServerLevel level = params.getLevel();
        if (level == null) return;
        RandomSource random = level.getRandom();

        Entity opener = params.contextMap().get(LootContextParams.THIS_ENTITY);
        ServerPlayer player = (opener instanceof ServerPlayer sp) ? sp : null;
        int godrollCount = 0;

        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (StructureChestBookRoller.shouldReplaceInStructureChest(stack)) {
                StructureChestBookRoller.RollResult res = StructureChestBookRoller.rollChestEnchantedBook(level, random);
                if (!res.book().isEmpty()) {
                    container.setItem(i, res.book());
                    if (res.godroll()) {
                        godrollCount++;
                    }
                }
            }
        }

        if (godrollCount > 0 && player != null) {
            level.playSound(null, player.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 0.9f, 1.1f);
            player.sendSystemMessage(Component.literal(
                    "§6§l✦ GODROLL BOOK! §eYou found " + (godrollCount > 1 ? "§d§l" + godrollCount + "x " : "a ") +
                    "§d§l6-Enchant Godroll Book §ein this chest! §6§l✦"
            ));
        }
    }
}
