package com.expedition.fishing.mixin;

import com.expedition.fishing.biome.BiomeResolver;
import com.expedition.fishing.data.FishingPlayerData;
import com.expedition.fishing.data.PlayerDataStorage;
import com.expedition.fishing.item.FishingItemFactory;
import com.expedition.fishing.mechanics.FishingEngine;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FishingHook.class)
public abstract class FishingHookMixin extends Entity {

    @Shadow
    private int nibble;

    @Shadow
    private int timeUntilLured;

    @Unique
    private boolean expSpeedApplied = false;

    @Unique
    private int expCustomMediumWait = -1;

    @Unique
    private boolean expInLava = false;

    @Unique
    private boolean expInVoid = false;

    public FishingHookMixin(EntityType<?> type, Level level) {
        super(type, level);
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void expeditionFishing$onTick(CallbackInfo ci) {
        FishingHook self = (FishingHook) (Object) this;
        if (!(self.getOwner() instanceof ServerPlayer player)) return;
        ServerLevel sLevel = player.level();

        // Skyblock Island check: 100% vanilla fishing
        if (BiomeResolver.isSkyblockWorld(sLevel)) return;

        ItemStack rod = player.getMainHandItem();
        if (!rod.is(net.minecraft.world.item.Items.FISHING_ROD)) {
            rod = player.getOffhandItem();
        }

        // Luminous bobber glow
        if (FishingEngine.getRodEnchants(rod).getOrDefault("expedition_fishing:luminous_bobber", 0) > 0) {
            self.setGlowingTag(true);
        }

        boolean lavaFluid = sLevel.getFluidState(self.blockPosition()).is(FluidTags.LAVA) || self.isInLava();
        boolean endVoidMedium = sLevel.dimension() == Level.END && !sLevel.getFluidState(self.blockPosition()).is(FluidTags.WATER) && self.tickCount >= 22;

        if (lavaFluid && FishingItemFactory.isLavaCapableRod(rod)) {
            this.expInLava = true;
            self.clearFire();
            Vec3 m = self.getDeltaMovement();
            self.setDeltaMovement(m.x * 0.7, Math.max(0.0, m.y * 0.2 + 0.03), m.z * 0.7);
            tickCustomMediumBite(player, sLevel, rod, true, false);
        } else if (endVoidMedium && FishingItemFactory.isVoidCapableRod(rod)) {
            this.expInVoid = true;
            self.setNoGravity(true);
            Vec3 m = self.getDeltaMovement();
            self.setDeltaMovement(m.x * 0.6, m.y * 0.3, m.z * 0.6);
            tickCustomMediumBite(player, sLevel, rod, false, true);
        } else if (this.timeUntilLured > 20 && !this.expSpeedApplied) {
            // Apply custom Fishing Speed reduction to vanilla water wait time
            this.expSpeedApplied = true;
            FishingEngine.PlayerFishingStats st = FishingEngine.computeStats(player, rod, false, false);
            double factor = 100.0 / (100.0 + Math.max(0.0, st.fishingSpeed));
            FishingPlayerData pData = PlayerDataStorage.get(player.getUUID());
            if (pData.nextCastFast) {
                pData.nextCastFast = false;
                factor *= 0.5;
            }
            this.timeUntilLured = Math.max(20, (int) Math.round(this.timeUntilLured * factor));
        }
    }

    @Unique
    private void tickCustomMediumBite(ServerPlayer player, ServerLevel sLevel, ItemStack rod, boolean lava, boolean endVoid) {
        if (this.nibble > 0) {
            this.nibble--;
            if (lava) {
                sLevel.sendParticles(ParticleTypes.LAVA, selfX(), selfY() + 0.2, selfZ(), 2, 0.15, 0.05, 0.15, 0.01);
            } else if (endVoid) {
                sLevel.sendParticles(ParticleTypes.PORTAL, selfX(), selfY() + 0.2, selfZ(), 4, 0.2, 0.1, 0.2, 0.05);
            }
            return;
        }

        if (this.expCustomMediumWait < 0) {
            FishingEngine.PlayerFishingStats st = FishingEngine.computeStats(player, rod, lava, endVoid);
            double factor = 100.0 / (100.0 + Math.max(0.0, st.fishingSpeed));
            FishingPlayerData pData = PlayerDataStorage.get(player.getUUID());
            if (pData.nextCastFast) {
                pData.nextCastFast = false;
                factor *= 0.5;
            }
            int baseTicks = 90 + sLevel.getRandom().nextInt(140);
            this.expCustomMediumWait = Math.max(25, (int) Math.round(baseTicks * factor));
        } else if (this.expCustomMediumWait > 0) {
            this.expCustomMediumWait--;
            if (this.expCustomMediumWait % 10 == 0) {
                sLevel.sendParticles(lava ? ParticleTypes.FLAME : ParticleTypes.REVERSE_PORTAL,
                        selfX(), selfY() + 0.15, selfZ(), 2, 0.2, 0.05, 0.2, 0.01);
            }
            if (this.expCustomMediumWait == 0) {
                this.nibble = 40;
                this.expCustomMediumWait = -1;
                sLevel.playSound(null, this.blockPosition(), SoundEvents.FISHING_BOBBER_SPLASH, SoundSource.NEUTRAL, 0.8f, lava ? 0.7f : 1.3f);
            }
        }
    }

    @Unique
    private double selfX() {
        return ((FishingHook) (Object) this).getX();
    }

    @Unique
    private double selfY() {
        return ((FishingHook) (Object) this).getY();
    }

    @Unique
    private double selfZ() {
        return ((FishingHook) (Object) this).getZ();
    }

    @Inject(method = "retrieve", at = @At("HEAD"), cancellable = true)
    private void expeditionFishing$onRetrieve(ItemStack rod, CallbackInfoReturnable<Integer> cir) {
        FishingHook self = (FishingHook) (Object) this;
        if (!(self.getOwner() instanceof ServerPlayer player)) return;
        ServerLevel sLevel = player.level();

        // Skyblock Island check: 100% vanilla fishing
        if (BiomeResolver.isSkyblockWorld(sLevel)) return;

        if (this.nibble > 0) {
            FishingEngine.handleCatch(player, self, rod, this.expInLava, this.expInVoid);
            self.discard();
            int dmg = FishingEngine.getRodEnchants(rod).getOrDefault("expedition_fishing:gentle_reel", 0) > 0 ? 0 : 1;
            cir.setReturnValue(dmg);
        } else {
            // Reset frenzy stack on missed reel
            FishingPlayerData pData = PlayerDataStorage.get(player.getUUID());
            pData.frenzyStacks = 0;
        }
    }
}
