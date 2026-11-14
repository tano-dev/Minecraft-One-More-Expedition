package net.onemoreexpedition.skyblock.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.portal.PortalShape;
import net.onemoreexpedition.skyblock.ExpeditionSkyblockMod;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(PortalShape.class)
public abstract class PortalShapeMixin {

    @Inject(method = "findEmptyPortalShape", at = @At("HEAD"), cancellable = true)
    private static void expedition$suppressPortalIgnition(LevelAccessor level, BlockPos pos, Direction.Axis axis, CallbackInfoReturnable<Optional<PortalShape>> cir) {
        if (level instanceof ServerLevel serverLevel && serverLevel.dimension().equals(ExpeditionSkyblockMod.SKYBLOCK_DIMENSION)) {
            cir.setReturnValue(Optional.empty());
        }
    }
}
