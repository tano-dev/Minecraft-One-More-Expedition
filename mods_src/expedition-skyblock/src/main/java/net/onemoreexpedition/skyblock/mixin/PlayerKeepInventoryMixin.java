package net.onemoreexpedition.skyblock.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.onemoreexpedition.skyblock.ExpeditionSkyblockMod;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PlayerKeepInventoryMixin {

    @Inject(method = "dropEquipment", at = @At("HEAD"), cancellable = true)
    private void expedition$keepInventoryOnDeath(ServerLevel level, CallbackInfo ci) {
        if (level.dimension().equals(ExpeditionSkyblockMod.SKYBLOCK_DIMENSION)) {
            ci.cancel(); // Cancel dropping inventory items on death in Skyblock
        }
    }

    @Inject(method = "getBaseExperienceReward", at = @At("HEAD"), cancellable = true)
    private void expedition$keepXpOnDeath(ServerLevel level, CallbackInfoReturnable<Integer> cir) {
        if (level.dimension().equals(ExpeditionSkyblockMod.SKYBLOCK_DIMENSION)) {
            cir.setReturnValue(0); // Prevent XP orbs from spawning on death in Skyblock
        }
    }
}
