package net.onemoreexpedition.skyblock.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.EndPortalBlock;
import net.minecraft.world.level.portal.TeleportTransition;
import net.onemoreexpedition.skyblock.ExpeditionSkyblockMod;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EndPortalBlock.class)
public abstract class EndPortalMixin {

    @Inject(method = "getPortalDestination", at = @At("HEAD"), cancellable = true)
    private void expedition$suppressEndPortalTraversal(ServerLevel level, Entity entity, BlockPos pos, CallbackInfoReturnable<TeleportTransition> cir) {
        if (level.dimension().equals(ExpeditionSkyblockMod.SKYBLOCK_DIMENSION)) {
            cir.setReturnValue(null);
        }
    }
}
