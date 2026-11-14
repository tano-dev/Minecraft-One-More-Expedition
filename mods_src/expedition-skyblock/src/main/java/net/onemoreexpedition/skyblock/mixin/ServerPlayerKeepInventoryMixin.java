package net.onemoreexpedition.skyblock.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.gamerules.GameRules;
import net.onemoreexpedition.skyblock.ExpeditionSkyblockMod;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerKeepInventoryMixin {

    @Inject(method = "restoreFrom", at = @At("TAIL"))
    private void expedition$restoreInventoryAndXp(ServerPlayer oldPlayer, boolean alive, CallbackInfo ci) {
        if (!alive && oldPlayer.level().dimension().equals(ExpeditionSkyblockMod.SKYBLOCK_DIMENSION)) {
            ServerPlayer self = (ServerPlayer) (Object) this;
            // If keepInventory was false globally, vanilla skipped transferring inventory & XP.
            // Restore inventory and XP from oldPlayer:
            if (!self.level().getGameRules().get(GameRules.KEEP_INVENTORY)) {
                self.getInventory().replaceWith(oldPlayer.getInventory());
                self.experienceLevel = oldPlayer.experienceLevel;
                self.totalExperience = oldPlayer.totalExperience;
                self.experienceProgress = oldPlayer.experienceProgress;
                self.setScore(oldPlayer.getScore());
            }
        }
    }
}
