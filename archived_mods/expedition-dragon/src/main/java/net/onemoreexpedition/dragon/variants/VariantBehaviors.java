package net.onemoreexpedition.dragon.variants;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.PowerParticleOption;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.monster.ElderGuardian;
import net.minecraft.world.entity.monster.Enderman;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.projectile.hurtingprojectile.DragonFireball;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.onemoreexpedition.dragon.altar.StellarityPillars;
import net.onemoreexpedition.dragon.config.DragonConfig;
import net.onemoreexpedition.dragon.core.DragonFightManager;
import net.onemoreexpedition.dragon.core.FightState;
import net.onemoreexpedition.dragon.core.VariantBehavior;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class VariantBehaviors {

    private VariantBehaviors() {
    }

    public static void registerAll() {
        VariantBehavior.register(DragonVariant.INFERNAL, InfernalBehavior::new);
        VariantBehavior.register(DragonVariant.VOIDGLOOM, VoidgloomBehavior::new);
        VariantBehavior.register(DragonVariant.REVENANT, RevenantBehavior::new);
        VariantBehavior.register(DragonVariant.ENLIGHTENED, EnlightenedBehavior::new);
        VariantBehavior.register(DragonVariant.CHRONOS, ChronosBehavior::new);
    }

    public static class InfernalBehavior implements VariantBehavior {
        private final Map<UUID, UUID> crystalBreathClouds = new HashMap<>();

        @Override
        public DragonVariant variant() { return DragonVariant.INFERNAL; }

        @Override
        public ChatFormatting getOutlineColor() { return ChatFormatting.RED; }

        @Override
        public double modifyMovesetCooldown(double baseTicks) {
            return Math.max(40.0D, baseTicks * 0.65D);
        }

        @Override
        public int modifyChainFireballCount(int baseCount) {
            return Math.max(1, baseCount) + 3;
        }

        @Override
        public void modifyFireballCloud(AreaEffectCloud cloud) {
            if (cloud == null) return;
            cloud.setRadius(Math.max(3.0F, cloud.getRadius()) * 3.0F);
            cloud.setDuration(Math.max(100, cloud.getDuration()) * 2);
            cloud.setRadiusPerTick(0.0F);
        }

        @Override
        public void onFightStart(FightState state, ServerLevel level, EnderDragon dragon) {
            if (state == null || level == null) return;
            refreshCrystalBreathClouds(state, level, dragon);
        }

        @Override
        public void onTick(FightState state, ServerLevel level, EnderDragon dragon) {
            if (state == null || level == null || state.getTickCount() % 40L != 0L) return;
            refreshCrystalBreathClouds(state, level, dragon);
        }

        private void refreshCrystalBreathClouds(FightState state, ServerLevel level, EnderDragon dragon) {
            Iterator<Map.Entry<UUID, UUID>> it = crystalBreathClouds.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<UUID, UUID> entry = it.next();
                UUID crystalId = entry.getKey();
                UUID cloudId = entry.getValue();
                if (!state.getActiveAnchors().contains(crystalId)) {
                    Entity cloud = level.getEntity(cloudId);
                    if (cloud != null && !cloud.isRemoved()) cloud.discard();
                    state.untrackMinion(cloudId);
                    it.remove();
                }
            }

            for (UUID anchorId : state.getActiveAnchors()) {
                Entity anchor = level.getEntity(anchorId);
                if (!(anchor instanceof EndCrystal crystal) || crystal.isRemoved()) continue;
                UUID existingCloudId = crystalBreathClouds.get(anchorId);
                Entity existingCloud = existingCloudId != null ? level.getEntity(existingCloudId) : null;
                if (existingCloud instanceof AreaEffectCloud aec && !aec.isRemoved()) {
                    aec.setDuration(200);
                    aec.setRadius(4.5F);
                    continue;
                }
                AreaEffectCloud cloud = new AreaEffectCloud(level, crystal.getX(), crystal.getY(), crystal.getZ());
                if (dragon != null) cloud.setOwner(dragon);
                cloud.setCustomParticle(PowerParticleOption.create(ParticleTypes.DRAGON_BREATH, 1.0F));
                cloud.setRadius(4.5F);
                cloud.setDuration(200);
                cloud.setRadiusPerTick(0.0F);
                cloud.addEffect(new MobEffectInstance(MobEffects.INSTANT_DAMAGE, 1, 1));
                if (level.addFreshEntity(cloud)) {
                    crystalBreathClouds.put(anchorId, cloud.getUUID());
                    state.trackMinion(cloud.getUUID());
                }
            }
        }

        @Override
        public void cleanup(FightState state, ServerLevel level) {
            if (level != null) {
                for (UUID cloudId : crystalBreathClouds.values()) {
                    Entity cloud = level.getEntity(cloudId);
                    if (cloud != null && !cloud.isRemoved()) cloud.discard();
                }
            }
            crystalBreathClouds.clear();
        }
    }

    public static class VoidgloomBehavior implements VariantBehavior {
        private static final double HOMING_SPEED = 1.10D;
        private static final double HOMING_TURN_RATE = 0.35D;

        @Override
        public DragonVariant variant() { return DragonVariant.VOIDGLOOM; }

        @Override
        public ChatFormatting getOutlineColor() { return ChatFormatting.LIGHT_PURPLE; }

        @Override
        public double modifyMovesetCooldown(double baseTicks) {
            return Math.max(40.0D, baseTicks * 0.60D);
        }

        @Override
        public int modifyChainFireballCount(int baseCount) {
            return Math.max(1, baseCount) + 4;
        }

        @Override
        public Vec3 modifyFireballVelocity(DragonFireball fireball, Vec3 baseVelocity, ServerLevel level) {
            if (baseVelocity == null) return Vec3.ZERO;
            double len = baseVelocity.length();
            if (len < 1.0E-4D) return baseVelocity;
            return baseVelocity.scale(HOMING_SPEED / len);
        }

        public static Vec3 computeHomingVelocity(Vec3 currentVel, Vec3 fireballPos, Vec3 targetPos) {
            if (currentVel == null || fireballPos == null || targetPos == null) return currentVel != null ? currentVel : Vec3.ZERO;
            Vec3 toTarget = targetPos.subtract(fireballPos);
            double dist = toTarget.length();
            if (dist < 1.0E-4D) return currentVel;
            Vec3 desiredDir = toTarget.scale(1.0D / dist);
            Vec3 curDir = currentVel.lengthSqr() > 1.0E-6D ? currentVel.normalize() : desiredDir;
            Vec3 blended = curDir.scale(1.0D - HOMING_TURN_RATE).add(desiredDir.scale(HOMING_TURN_RATE));
            double bLen = blended.length();
            if (bLen < 1.0E-4D) return desiredDir.scale(HOMING_SPEED);
            return blended.scale(HOMING_SPEED / bLen);
        }

        @Override
        public void onTick(FightState state, ServerLevel level, EnderDragon dragon) {
            if (state == null || level == null || dragon == null) return;
            long tick = state.getTickCount();
            if (tick % 2L == 0L) {
                AABB searchBox = new AABB(-180.0D, 0.0D, -180.0D, 180.0D, 256.0D, 180.0D);
                List<DragonFireball> fireballs = level.getEntitiesOfClass(DragonFireball.class, searchBox);
                for (DragonFireball fb : fireballs) {
                    if (fb.isRemoved()) continue;
                    ServerPlayer nearest = findNearestTargetPlayer(level, fb.position(), 160.0D);
                    if (nearest != null) {
                        Vec3 steered = computeHomingVelocity(fb.getDeltaMovement(), fb.position(), nearest.position().add(0.0D, 1.0D, 0.0D));
                        fb.setDeltaMovement(steered);
                    }
                }
            }
            if (tick % 70L == 0L) {
                ServerPlayer target = findNearestTargetPlayer(level, dragon.position(), 160.0D);
                if (target != null) {
                    Vec3 origin = dragon.position().add(0.0D, 2.0D, 0.0D);
                    Vec3 dir = target.position().add(0.0D, 1.0D, 0.0D).subtract(origin);
                    if (dir.lengthSqr() > 1.0E-4D) {
                        Vec3 vel = dir.normalize().scale(HOMING_SPEED);
                        DragonFireball fb = new DragonFireball(level, dragon, vel);
                        fb.snapTo(origin.x, origin.y, origin.z, dragon.getYRot(), 0.0F);
                        fb.setDeltaMovement(vel);
                        if (level.addFreshEntity(fb)) state.trackMinion(fb.getUUID());
                    }
                }
            }
        }
    }

    public static class RevenantBehavior implements VariantBehavior {
        private static final double[] THRESHOLDS = new double[]{
                0.90D, 0.80D, 0.70D, 0.60D, 0.50D, 0.40D, 0.30D, 0.20D, 0.10D
        };

        @Override
        public DragonVariant variant() { return DragonVariant.REVENANT; }

        @Override
        public ChatFormatting getOutlineColor() { return ChatFormatting.GREEN; }

        @Override
        public double[] getEndermanCallThresholds() { return THRESHOLDS.clone(); }

        @Override
        public void applyCalledEndermanBuff(Enderman enderman) {
            if (enderman != null && enderman.isAlive()) enderman.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 1200, 0, false, true));
        }

        @Override
        public int getShadowPhantomsPerCrystal(DragonConfig.MinionConfig config) {
            return config != null ? config.revenantShadowPhantomsPerCrystal : 5;
        }
    }

    public static class EnlightenedBehavior implements VariantBehavior {
        private final Map<UUID, Vec3> guardianAnchorPositions = new HashMap<>();

        @Override
        public DragonVariant variant() { return DragonVariant.ENLIGHTENED; }

        @Override
        public ChatFormatting getOutlineColor() { return ChatFormatting.YELLOW; }

        @Override
        public boolean hasCustomShieldAnchors() { return true; }

        @Override
        public boolean suppressCrystalSpawns() { return true; }

        @Override
        public void onFightStart(FightState state, ServerLevel level, EnderDragon dragon) {
            if (state == null || level == null) return;
            guardianAnchorPositions.clear();
            for (StellarityPillars.Pillar pillar : StellarityPillars.PILLARS) {
                level.getChunkAt(pillar.crystalBlockPos());
                ElderGuardian guardian = EntityTypes.ELDER_GUARDIAN.create(level, EntitySpawnReason.EVENT);
                if (guardian == null) continue;
                Vec3 anchorPos = new Vec3(pillar.crystalX() + 0.5D, pillar.crystalY() + 0.5D, pillar.crystalZ() + 0.5D);
                guardian.snapTo(anchorPos.x, anchorPos.y, anchorPos.z, 0.0F, 0.0F);
                guardian.setCustomName(Component.literal("\u00a76\u00a7lEnlightened Pillar Guardian"));
                guardian.setCustomNameVisible(true);
                guardian.setPersistenceRequired();
                guardian.setNoGravity(true);
                AttributeInstance kb = guardian.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
                if (kb != null) kb.setBaseValue(1.0D);
                guardian.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, Integer.MAX_VALUE, 0, false, false));
                guardian.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, Integer.MAX_VALUE, 0, false, false));
                if (level.addFreshEntity(guardian)) {
                    UUID id = guardian.getUUID();
                    guardianAnchorPositions.put(id, anchorPos);
                    state.addAnchor(id);
                    state.trackMinion(id);
                }
            }
        }

        @Override
        public void onTick(FightState state, ServerLevel level, EnderDragon dragon) {
            if (state == null || level == null) return;
            
            // Outline/Glow Effect
            if (!dragon.hasEffect(MobEffects.GLOWING)) {
                dragon.addEffect(new MobEffectInstance(MobEffects.GLOWING, Integer.MAX_VALUE, 0, false, false));
            }
            // Scoreboard team setting could be complex, for now ensure glow is on.
            
            Iterator<Map.Entry<UUID, Vec3>> it = guardianAnchorPositions.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<UUID, Vec3> entry = it.next();
                UUID id = entry.getKey();
                Vec3 pos = entry.getValue();
                Entity entity = level.getEntity(id);
                if (!(entity instanceof ElderGuardian guardian) || guardian.isRemoved() || !guardian.isAlive()) {
                    DamageSource lastSrc = (entity instanceof ElderGuardian eg) ? eg.getLastDamageSource() : null;
                    BlockPos bp = BlockPos.containing(pos);
                    it.remove();
                    DragonFightManager.get(level.getServer()).onAnchorDestroyed(level, id, bp, lastSrc);
                    continue;
                }
                
                // Force NoGravity and tethered position
                guardian.setNoGravity(true);
                guardian.setAirSupply(300);
                guardian.fallDistance = 0.0F;
                guardian.setDeltaMovement(Vec3.ZERO);
                if (guardian.position().distanceToSqr(pos) > 0.25D) {
                    guardian.snapTo(pos.x, pos.y, pos.z, guardian.getYRot(), guardian.getXRot());
                }
                
                // Trigger phantom spawns if needed
                if (state.getTickCount() % 400 == 0) {
                    spawnShadowPhantoms(state, level, guardian);
                }
                
                if (dragon != null && state.getTickCount() % 20L == 0L) {
                    level.sendParticles(ParticleTypes.ENCHANT, pos.x, pos.y + 1.0D, pos.z, 8, 0.4D, 0.4D, 0.4D, 0.1D);
                    if (dragon.getHealth() < dragon.getMaxHealth()) {
                        dragon.setHealth(Math.min(dragon.getMaxHealth(), dragon.getHealth() + 1.0F));
                    }
                }
            }
        }

        private void spawnShadowPhantoms(FightState state, ServerLevel level, ElderGuardian guardian) {
            net.minecraft.world.entity.monster.Phantom phantom = EntityTypes.PHANTOM.create(level, EntitySpawnReason.EVENT);
            if (phantom == null) return;
            phantom.snapTo(guardian.getX(), guardian.getY(), guardian.getZ(), 0.0F, 0.0F);
            phantom.setCustomName(Component.literal("\u00a78\u00a7lShadow Phantom"));
            phantom.setCustomNameVisible(true);
            phantom.setPersistenceRequired();
            if (level.addFreshEntity(phantom)) state.trackMinion(phantom.getUUID());
        }

        @Override
        public void cleanup(FightState state, ServerLevel level) {
            if (level != null) {
                for (UUID id : guardianAnchorPositions.keySet()) {
                    Entity e = level.getEntity(id);
                    if (e != null && !e.isRemoved()) e.discard();
                }
            }
            guardianAnchorPositions.clear();
        }
    }

    public static class ChronosBehavior implements VariantBehavior {
        private boolean wardensSummoned = false;
        private final List<UUID> wardenUuids = new ArrayList<>();

        @Override
        public DragonVariant variant() { return DragonVariant.CHRONOS; }

        @Override
        public ChatFormatting getOutlineColor() { return ChatFormatting.BLUE; }

        public boolean isWardensSummoned() { return wardensSummoned; }

        public static Vec3 computeSafeTeleportOffset(Vec3 playerPos, Vec3 wardenPos, double desiredDistance) {
            double clampedDist = Math.max(5.0D, Math.min(6.0D, desiredDistance));
            Vec3 delta = wardenPos != null && playerPos != null
                    ? new Vec3(wardenPos.x - playerPos.x, 0.0D, wardenPos.z - playerPos.z)
                    : Vec3.ZERO;
            double horizontalLen = delta.length();
            Vec3 dir = horizontalLen > 1.0E-4D ? delta.scale(1.0D / horizontalLen) : new Vec3(1.0D, 0.0D, 0.0D);
            return playerPos.add(dir.x * clampedDist, 0.0D, dir.z * clampedDist);
        }

        @Override
        public void onHealthThreshold(FightState state, ServerLevel level, EnderDragon dragon, double prevRatio, double newRatio) {
            if (wardensSummoned || state == null || newRatio > 0.50D) return;
            wardensSummoned = true;
            if (level == null) return;
            double[][] offsets = new double[][]{{6.0D, 0.0D}, {-6.0D, 5.0D}, {-6.0D, -5.0D}};
            for (double[] off : offsets) {
                Warden warden = EntityTypes.WARDEN.create(level, EntitySpawnReason.EVENT);
                if (warden == null) continue;
                int x = (int) Math.round(off[0]);
                int z = (int) Math.round(off[1]);
                int y = Math.max(58, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z));
                warden.snapTo(x + 0.5D, y, z + 0.5D, 0.0F, 0.0F);
                warden.setCustomName(Component.literal("\u00a7b\u00a7lChronos Warden"));
                warden.setCustomNameVisible(true);
                warden.setPersistenceRequired();
                warden.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, Integer.MAX_VALUE, 0, false, false)); 
                warden.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, Integer.MAX_VALUE, 4, false, false));
                if (level.addFreshEntity(warden)) {
                    wardenUuids.add(warden.getUUID());
                    state.trackMinion(warden.getUUID());
                    ServerPlayer target = findNearestTargetPlayer(level, warden.position(), 180.0D);
                    if (target != null) {
                        warden.increaseAngerAt(target, 150, true);
                        warden.setAttackTarget(target);
                    }
                }
            }
            level.playSound(null, BlockPos.ZERO, SoundEvents.WARDEN_ROAR, SoundSource.HOSTILE, 4.0F, 0.85F);
        }

        @Override
        public void onTick(FightState state, ServerLevel level, EnderDragon dragon) {
            if (!wardensSummoned || state == null || level == null || state.getTickCount() % 30L != 0L) return;
            Iterator<UUID> it = wardenUuids.iterator();
            while (it.hasNext()) {
                UUID id = it.next();
                Entity entity = level.getEntity(id);
                if (!(entity instanceof Warden warden) || warden.isRemoved() || !warden.isAlive()) {
                    it.remove();
                    continue;
                }
                ServerPlayer target = findNearestTargetPlayer(level, warden.position(), 180.0D);
                if (target == null) continue;
                warden.increaseAngerAt(target, 100, false);
                warden.setAttackTarget(target);
                double dist = warden.position().distanceTo(target.position());
                double deltaY = target.getY() - warden.getY();
                if (dist > 20.0D || deltaY > 5.0D) {
                    Vec3 dest = computeSafeTeleportOffset(target.position(), warden.position(), 5.5D);
                    int blockX = (int) Math.floor(dest.x);
                    int blockZ = (int) Math.floor(dest.z);
                    int groundY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, blockX, blockZ);
                    double targetY = groundY > 40 ? groundY : target.getY();
                    warden.snapTo(dest.x, targetY, dest.z, warden.getYRot(), warden.getXRot());
                    warden.fallDistance = 0.0F;
                    level.sendParticles(ParticleTypes.PORTAL, dest.x, targetY + 1.0D, dest.z, 25, 0.5D, 1.0D, 0.5D, 0.2D);
                    level.playSound(null, BlockPos.containing(dest.x, targetY, dest.z), SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 2.0F, 0.7F);
                }
            }
        }

        @Override
        public void cleanup(FightState state, ServerLevel level) {
            if (level != null) {
                for (UUID id : wardenUuids) {
                    Entity e = level.getEntity(id);
                    if (e != null && !e.isRemoved()) e.discard();
                }
            }
            wardenUuids.clear();
        }
    }

    public static ServerPlayer findNearestTargetPlayer(ServerLevel level, Vec3 fromPos, double maxRadius) {
        if (level == null || fromPos == null) return null;
        double bestDistSq = maxRadius * maxRadius;
        ServerPlayer best = null;
        for (ServerPlayer player : level.players()) {
            if (player.isSpectator() || player.isCreative() || !player.isAlive()) continue;
            double dSq = player.position().distanceToSqr(fromPos);
            if (dSq <= bestDistSq) {
                bestDistSq = dSq;
                best = player;
            }
        }
        return best;
    }
}