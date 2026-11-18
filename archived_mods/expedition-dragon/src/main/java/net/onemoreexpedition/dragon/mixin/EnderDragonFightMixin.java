package net.onemoreexpedition.dragon.mixin;

import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.end.DragonRespawnStage;
import net.minecraft.world.level.dimension.end.EnderDragonFight;
import net.onemoreexpedition.dragon.altar.AltarManager;
import net.onemoreexpedition.dragon.altar.EnderDragonFightBridge;
import net.onemoreexpedition.dragon.core.DragonFightManager;
import net.onemoreexpedition.dragon.core.FightState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.UUID;

@Mixin(EnderDragonFight.class)
public abstract class EnderDragonFightMixin implements EnderDragonFightBridge {

    @Shadow
    private ServerBossEvent dragonEvent;

    @Shadow
    private ServerLevel level;

    @Shadow
    private boolean dragonKilled;

    @Shadow
    private UUID dragonUUID;

    @Shadow
    private DragonRespawnStage respawnStage;

    @Shadow
    private int respawnTime;

    @Shadow
    private List<EntityReference<EndCrystal>> respawnCrystals;

    @Shadow
    private void updatePlayers() {
        throw new AssertionError();
    }

    @Inject(method = "tryRespawn()V", at = @At("HEAD"), cancellable = true)
    private void expeditionDragon$suppressVanillaTryRespawn(CallbackInfo ci) {
        if (this.level != null && this.level.dimension() == Level.END) {
            ci.cancel();
        }
    }

    @Inject(method = "spawnExitPortal(Z)V", at = @At("HEAD"), cancellable = true)
    private void expeditionDragon$preserveStellarityAltar(boolean active, CallbackInfo ci) {
        if (this.level != null && this.level.dimension() == Level.END) {
            // Only cancel if Stellarity portal marker exists to avoid vanilla portal spawning
            if (AltarManager.get(this.level.getServer()).hasStellarityExitPortal(this.level)) {
                ci.cancel();
            }
        }
    }

    @Inject(
            method = "setRespawnStage(Lnet/minecraft/world/level/dimension/end/DragonRespawnStage;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void expeditionDragon$onRespawnStageChanged(DragonRespawnStage stage, CallbackInfo ci) {
        if ((stage == DragonRespawnStage.SUMMONING_PILLARS || stage == DragonRespawnStage.END) && this.level != null) {
            for (EntityReference<EndCrystal> ref : this.respawnCrystals) {
                EndCrystal crystal = ref.getEntity(this.level, EndCrystal.class);
                if (crystal != null && !crystal.isRemoved()) {
                    crystal.setBeamTarget(null);
                    crystal.discard();
                }
            }
            this.respawnStage = null;
            this.respawnTime = 0;
            this.dragonKilled = true;
            this.dragonUUID = null;
            this.respawnCrystals = List.of();
            if (this.dragonEvent != null) {
                this.dragonEvent.setVisible(false);
            }
            ((EnderDragonFight) (Object) this).setDirty();
            AltarManager.get(this.level.getServer()).onRespawnSequenceComplete(this.level);
            ci.cancel();
        }
    }

    @Inject(method = "abortRespawnSequence()V", at = @At("TAIL"))
    private void expeditionDragon$onAbortRespawnSequence(CallbackInfo ci) {
        if (this.level != null) {
            AltarManager.get(this.level.getServer()).onRespawnAborted(this.level);
        }
    }

    @Inject(method = "tick()V", at = @At("HEAD"), cancellable = true)
    private void expeditionDragon$pauseVanillaTickForCommandFight(CallbackInfo ci) {
        if (this.level == null) {
            return;
        }
        FightState active = DragonFightManager.get(this.level.getServer()).getActiveFight();
        if (active != null && !active.isEnded() && !active.isFromAltar()) {
            if (this.dragonEvent != null) {
                this.dragonEvent.setVisible(false);
            }
            ci.cancel();
        }
    }

    @Redirect(
            method = "tick()V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/level/ServerBossEvent;setVisible(Z)V"
            )
    )
    private void expeditionDragon$suppressVanillaBossBarVisibility(ServerBossEvent bossEvent, boolean visible) {
        if (this.level != null) {
            FightState active = DragonFightManager.get(this.level.getServer()).getActiveFight();
            if (active != null && !active.isEnded()) {
                bossEvent.setVisible(false);
                return;
            }
        }
        bossEvent.setVisible(visible);
    }

    @Inject(method = "findOrCreateDragon()V", at = @At("HEAD"), cancellable = true)
    private void expeditionDragon$suppressVanillaDragonSpawn(CallbackInfo ci) {
        if (this.level == null) {
            return;
        }
        // Always prevent vanilla from auto-spawning an initial Ender Dragon on world load (Stellarity style: starts dead)
        FightState active = DragonFightManager.get(this.level.getServer()).getActiveFight();
        if (active == null || active.isEnded()) {
            this.dragonKilled = true;
            this.dragonUUID = null;
            if (this.dragonEvent != null) {
                this.dragonEvent.setVisible(false);
            }
            ((EnderDragonFight) (Object) this).setDirty();
        }
        ci.cancel();
    }

    @Override
    public void expeditionDragon$startAltarRespawn(List<EndCrystal> crystals) {
        this.dragonKilled = true;
        this.dragonUUID = null;
        this.respawnStage = DragonRespawnStage.START;
        this.respawnTime = 0;
        this.respawnCrystals = crystals.stream().map(EntityReference::of).toList();
        if (this.dragonEvent != null) {
            this.dragonEvent.setVisible(false);
            this.updatePlayers();
        }
        ((EnderDragonFight) (Object) this).setDirty();
    }

    @Override
    public void expeditionDragon$bindCustomDragon(UUID customDragonUuid) {
        this.dragonKilled = false;
        this.dragonUUID = customDragonUuid;
        if (this.dragonEvent != null) {
            this.dragonEvent.setVisible(false);
        }
        ((EnderDragonFight) (Object) this).setDirty();
    }

    @Override
    public void expeditionDragon$clearCustomDragon(UUID customDragonUuid) {
        if (customDragonUuid != null && customDragonUuid.equals(this.dragonUUID)) {
            this.dragonUUID = null;
            this.dragonKilled = true;
            if (this.dragonEvent != null) {
                this.dragonEvent.setVisible(false);
            }
            ((EnderDragonFight) (Object) this).setDirty();
        }
    }

    @Override
    public boolean expeditionDragon$isRespawnInProgress() {
        return this.respawnStage != null;
    }
}
