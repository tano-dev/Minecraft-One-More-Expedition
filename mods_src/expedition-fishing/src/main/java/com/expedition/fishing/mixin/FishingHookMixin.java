package com.expedition.fishing.mixin;

import com.expedition.fishing.biome.BiomeResolver;
import com.expedition.fishing.data.FishingPlayerData;
import com.expedition.fishing.data.PlayerDataStorage;
import com.expedition.fishing.item.FishingItemFactory;
import com.expedition.fishing.mechanics.FishingEngine;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
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
    private int expCustomBiteWindow = 0;

    @Unique
    private boolean expInLava = false;

    @Unique
    private boolean expInVoid = false;

    public FishingHookMixin(EntityType<?> type, Level level) {
        super(type, level);
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void expeditionFishing$onTickTail(CallbackInfo ci) {
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

        FishingPlayerData pData = PlayerDataStorage.get(player.getUUID());
        boolean lavaFluid = sLevel.getFluidState(self.blockPosition()).is(FluidTags.LAVA) || self.isInLava();
        boolean endVoidMedium = sLevel.dimension() == Level.END
                && !sLevel.getFluidState(self.blockPosition()).is(FluidTags.WATER)
                && self.tickCount >= 14;

        // Auto-enable Magma (Lv.27+) and Void (Lv.35+) fishing for any Fishing Rod once player reaches the required level
        if (lavaFluid && pData.level >= 27) {
            this.expInLava = true;
            self.clearFire();
            self.setGlowingTag(true);
            Vec3 m = self.getDeltaMovement();
            self.setDeltaMovement(m.x * 0.65, Math.max(0.0, m.y * 0.15 + 0.025), m.z * 0.65);
            tickCustomMediumBite(player, sLevel, rod, true, false);
        } else if (endVoidMedium && pData.level >= 35) {
            this.expInVoid = true;
            self.setNoGravity(true);
            self.setGlowingTag(true);
            Vec3 m = self.getDeltaMovement();
            self.setDeltaMovement(m.x * 0.45, m.y * 0.2, m.z * 0.45);
            tickCustomMediumBite(player, sLevel, rod, false, true);
        } else if (this.timeUntilLured > 20 && !this.expSpeedApplied) {
            this.expSpeedApplied = true;
            FishingEngine.PlayerFishingStats st = FishingEngine.computeStats(player, rod, false, false);
            double factor = 100.0 / (100.0 + Math.max(0.0, st.fishingSpeed));
            if (pData.nextCastFast) {
                pData.nextCastFast = false;
                factor *= 0.5;
            }
            this.timeUntilLured = Math.max(20, (int) Math.round(this.timeUntilLured * factor));
        }
    }

    @Unique
    private void tickCustomMediumBite(ServerPlayer player, ServerLevel sLevel, ItemStack rod, boolean lava, boolean endVoid) {
        FishingHook self = (FishingHook) (Object) this;

        // Stage 3: Active BITE Window!
        if (this.expCustomBiteWindow > 0) {
            this.expCustomBiteWindow--;
            this.nibble = this.expCustomBiteWindow;

            boolean flash = (this.expCustomBiteWindow / 4) % 2 == 0;
            String tagText = endVoid
                    ? (flash ? "§d§l✦ §6§l>>> REEL IN NOW! <<< §d§l✦" : "§5§l✦ §e§l>>> VOID BITE! <<< §5§l✦")
                    : (flash ? "§6§l🔥 §e§l>>> REEL IN NOW! <<< §6§l🔥" : "§c§l🔥 §6§l>>> LAVA BITE! <<< §c§l🔥");
            self.setCustomName(Component.literal(tagText));
            self.setCustomNameVisible(true);

            // High-visibility vertical beacon particles
            for (int h = 0; h < 3; h++) {
                sLevel.sendParticles(endVoid ? ParticleTypes.END_ROD : ParticleTypes.LAVA,
                        selfX(), selfY() + 0.2 + h * 0.6, selfZ(), 3, 0.15, 0.2, 0.15, 0.02);
            }
            if (endVoid) {
                sLevel.sendParticles(ParticleTypes.WITCH, selfX(), selfY() + 0.3, selfZ(), 5, 0.25, 0.15, 0.25, 0.02);
            }

            if (this.expCustomBiteWindow == 0) {
                self.setCustomName(Component.literal(endVoid ? "§5✦ §dVoid Rift §7[§8Missed§7]" : "§6🔥 §eMagma Line §7[§8Missed§7]"));
            }
            return;
        }

        // Initialize Wait Timer
        if (this.expCustomMediumWait < 0) {
            FishingEngine.PlayerFishingStats st = FishingEngine.computeStats(player, rod, lava, endVoid);
            double factor = 100.0 / (100.0 + Math.max(0.0, st.fishingSpeed));
            FishingPlayerData pData = PlayerDataStorage.get(player.getUUID());
            if (pData.nextCastFast) {
                pData.nextCastFast = false;
                factor *= 0.5;
            }
            int baseTicks = 80 + sLevel.getRandom().nextInt(110);
            this.expCustomMediumWait = Math.max(25, (int) Math.round(baseTicks * factor));
            return;
        }

        if (this.expCustomMediumWait > 0) {
            this.expCustomMediumWait--;

            // Stage 2: Approaching (last 30 ticks before bite)
            if (this.expCustomMediumWait <= 30) {
                self.setCustomName(Component.literal(endVoid
                        ? "§e§l⚡ §dSomething stirs in the Void..."
                        : "§e§l⚡ §6Something stirs in the Magma..."));
                self.setCustomNameVisible(true);
                if (this.expCustomMediumWait % 6 == 0) {
                    sLevel.sendParticles(endVoid ? ParticleTypes.END_ROD : ParticleTypes.FLAME,
                            selfX(), selfY() + 0.25, selfZ(), 5, 0.25, 0.1, 0.25, 0.03);
                    float pitch = 1.0f + ((30 - this.expCustomMediumWait) / 30.0f) * 0.6f;
                    sLevel.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.7f, pitch);
                }
            } else {
                // Stage 1: Hovering / Waiting
                int dots = (int) ((sLevel.getGameTime() / 10) % 3) + 1;
                String dotStr = ".".repeat(dots);
                self.setCustomName(Component.literal(endVoid
                        ? "§5✦ §dVoid Rift §7[§d" + dotStr + "§7]"
                        : "§6🔥 §eMagma Line §7[§6" + dotStr + "§7]"));
                self.setCustomNameVisible(true);
                if (this.expCustomMediumWait % 8 == 0) {
                    sLevel.sendParticles(endVoid ? ParticleTypes.PORTAL : ParticleTypes.FLAME,
                            selfX(), selfY() + 0.15, selfZ(), 4, 0.3, 0.08, 0.3, 0.02);
                }
            }

            // Trigger BITE!
            if (this.expCustomMediumWait == 0) {
                this.expCustomBiteWindow = 55; // 2.75 seconds to reel in!
                this.nibble = 55;
                this.expCustomMediumWait = -1;

                // Play clear bite alert both at bobber AND directly at the angler's position
                sLevel.playSound(null, this.blockPosition(), SoundEvents.FISHING_BOBBER_SPLASH, SoundSource.NEUTRAL, 1.0f, lava ? 0.7f : 1.3f);
                sLevel.playSound(null, player.blockPosition(), SoundEvents.FISHING_BOBBER_SPLASH, SoundSource.PLAYERS, 0.85f, lava ? 0.75f : 1.25f);
                sLevel.playSound(null, player.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.9f, 1.5f);

                player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket(Component.literal(endVoid
                        ? "§d§l✦ VOID BITE! §e§lRight-Click to Reel In! §d§l✦"
                        : "§6§l🔥 LAVA BITE! §e§lRight-Click to Reel In! §6§l🔥")));
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

        if (this.nibble > 0 || this.expCustomBiteWindow > 0) {
            this.expCustomBiteWindow = 0;
            this.nibble = 0;
            FishingEngine.handleCatch(player, self, rod, this.expInLava, this.expInVoid);
            self.discard();
            int dmg = FishingEngine.getRodEnchants(rod).getOrDefault("expedition_fishing:gentle_reel", 0) > 0 ? 0 : 1;
            cir.setReturnValue(dmg);
        } else {
            FishingPlayerData pData = PlayerDataStorage.get(player.getUUID());
            pData.frenzyStacks = 0;
        }
    }
}
