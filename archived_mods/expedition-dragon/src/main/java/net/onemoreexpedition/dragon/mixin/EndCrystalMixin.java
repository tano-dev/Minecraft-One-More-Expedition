package net.onemoreexpedition.dragon.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.onemoreexpedition.dragon.altar.AltarManager;
import net.onemoreexpedition.dragon.core.DragonFightManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EndCrystal.class)
public class EndCrystalMixin {

    @Inject(
            method = "hurtServer(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;F)Z",
            at = @At("HEAD"),
            cancellable = true
    )
    private void expeditionDragon$lockAltarCrystal(
            ServerLevel level,
            DamageSource source,
            float amount,
            CallbackInfoReturnable<Boolean> cir
    ) {
        EndCrystal self = (EndCrystal) (Object) this;
        if (AltarManager.isLockedCrystal(level, self)
                && (source == null || !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY))) {
            cir.setReturnValue(false);
        }
    }

    @Inject(
            method = "onDestroyedBy(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;)V",
            at = @At("HEAD")
    )
    private void expeditionDragon$onCrystalDestroyed(
            ServerLevel level,
            DamageSource source,
            CallbackInfo ci
    ) {
        EndCrystal self = (EndCrystal) (Object) this;
        if (!AltarManager.isLockedCrystal(level, self)) {
            DragonFightManager.get(level.getServer())
                    .onAnchorDestroyed(level, self.getUUID(), self.blockPosition(), source);
        }
    }
}
