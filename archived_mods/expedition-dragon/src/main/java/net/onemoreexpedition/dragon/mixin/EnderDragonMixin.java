package net.onemoreexpedition.dragon.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.EnderDragonPart;
import net.minecraft.world.entity.boss.enderdragon.phases.DragonPhaseInstance;
import net.minecraft.world.phys.Vec3;
import net.onemoreexpedition.dragon.combat.DragonShield;
import net.onemoreexpedition.dragon.combat.MovesetController;
import net.onemoreexpedition.dragon.core.DragonFightManager;
import net.onemoreexpedition.dragon.core.FightState;
import net.onemoreexpedition.dragon.core.VariantBehavior;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EnderDragon.class)
public class EnderDragonMixin {

    @Shadow
    public EndCrystal nearestCrystal;

    @Shadow
    public int dragonDeathTime;

    @Unique
    private float expeditionDragon$preHurtHealth;

    @Inject(
            method = "hurt(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/boss/enderdragon/EnderDragonPart;Lnet/minecraft/world/damagesource/DamageSource;F)Z",
            at = @At("HEAD"),
            cancellable = true
    )
    private void expeditionDragon$guardShield(
            ServerLevel level,
            EnderDragonPart part,
            DamageSource source,
            float amount,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (DragonShield.shouldCancelDamage(level, (EnderDragon) (Object) this, source)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(
            method = "hurt(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/boss/enderdragon/EnderDragonPart;Lnet/minecraft/world/damagesource/DamageSource;F)Z",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/boss/enderdragon/EnderDragon;reallyHurt(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;F)V",
                    shift = At.Shift.BEFORE
            )
    )
    private void expeditionDragon$capturePreHurtHealth(
            ServerLevel level,
            EnderDragonPart part,
            DamageSource source,
            float amount,
            CallbackInfoReturnable<Boolean> cir
    ) {
        this.expeditionDragon$preHurtHealth = ((EnderDragon) (Object) this).getHealth();
    }

    @Inject(
            method = "hurt(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/boss/enderdragon/EnderDragonPart;Lnet/minecraft/world/damagesource/DamageSource;F)Z",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/boss/enderdragon/EnderDragon;reallyHurt(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;F)V",
                    shift = At.Shift.AFTER
            )
    )
    private void expeditionDragon$recordPostHurtDamage(
            ServerLevel level,
            EnderDragonPart part,
            DamageSource source,
            float amount,
            CallbackInfoReturnable<Boolean> cir
    ) {
        DragonShield.recordPostReductionDamage(level, (EnderDragon) (Object) this, source, this.expeditionDragon$preHurtHealth);
    }

    @Inject(method = "checkCrystals()V", at = @At("HEAD"), cancellable = true)
    private void expeditionDragon$guardCheckCrystalsHead(CallbackInfo ci) {
        EnderDragon self = (EnderDragon) (Object) this;
        if (!(self.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        DragonFightManager manager = DragonFightManager.get(serverLevel.getServer());
        if (!manager.isCustomDragon(self)) {
            return;
        }
        VariantBehavior behavior = manager.getActiveBehavior();
        if (behavior != null && behavior.suppressCrystalSpawns()) {
            this.nearestCrystal = null;
            ci.cancel();
            return;
        }
        FightState state = manager.getActiveFight();
        if (state != null && this.nearestCrystal != null
                && !state.getActiveAnchors().contains(this.nearestCrystal.getUUID())) {
            this.nearestCrystal = null;
        }
    }

    @Inject(method = "checkCrystals()V", at = @At("RETURN"))
    private void expeditionDragon$guardCheckCrystalsReturn(CallbackInfo ci) {
        EnderDragon self = (EnderDragon) (Object) this;
        if (!(self.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        DragonFightManager manager = DragonFightManager.get(serverLevel.getServer());
        if (!manager.isCustomDragon(self)) {
            return;
        }
        FightState state = manager.getActiveFight();
        if (state != null && this.nearestCrystal != null
                && !state.getActiveAnchors().contains(this.nearestCrystal.getUUID())) {
            this.nearestCrystal = null;
        }
    }

    @Redirect(
            method = "aiStep()V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/boss/enderdragon/phases/DragonPhaseInstance;getFlyTargetLocation()Lnet/minecraft/world/phys/Vec3;"
            )
    )
    private Vec3 expeditionDragon$overrideFlyTarget(DragonPhaseInstance instance) {
        EnderDragon self = (EnderDragon) (Object) this;
        if (self.level() instanceof ServerLevel serverLevel) {
            MovesetController moveset = DragonFightManager.get(serverLevel.getServer()).getMovesetController();
            if (moveset != null && moveset.isMovementOverridden(self)) {
                moveset.applyMovementStep(self, serverLevel);
                return null;
            }
        }
        return instance.getFlyTargetLocation();
    }

    @Inject(method = "tickDeath()V", at = @At("HEAD"))
    private void expeditionDragon$onTickDeathHead(CallbackInfo ci) {
        EnderDragon self = (EnderDragon) (Object) this;
        if (!(self.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        DragonFightManager manager = DragonFightManager.get(serverLevel.getServer());
        if (!manager.isCustomDragon(self)) {
            return;
        }
        if (this.dragonDeathTime == 0) {
            manager.onDragonDyingStarted(serverLevel, self);
        }
        if (this.dragonDeathTime >= 199) {
            manager.onDragonKilled(serverLevel, self);
        }
    }

    @Inject(method = "tickDeath()V", at = @At("TAIL"))
    private void expeditionDragon$onTickDeathTail(CallbackInfo ci) {
        EnderDragon self = (EnderDragon) (Object) this;
        if (self.level() instanceof ServerLevel serverLevel) {
            DragonFightManager manager = DragonFightManager.get(serverLevel.getServer());
            if (manager.isCustomOrDyingDragon(self)) {
                manager.onDragonDeathTick(serverLevel, self, this.dragonDeathTime);
            }
        }
    }
}
