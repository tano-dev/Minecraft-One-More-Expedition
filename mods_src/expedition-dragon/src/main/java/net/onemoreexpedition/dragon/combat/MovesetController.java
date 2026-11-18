package net.onemoreexpedition.dragon.combat;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.PowerParticleOption;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase;
import net.minecraft.world.entity.projectile.hurtingprojectile.DragonFireball;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.onemoreexpedition.dragon.config.DragonConfig;
import net.onemoreexpedition.dragon.core.FightState;
import net.onemoreexpedition.dragon.core.VariantBehavior;
import net.onemoreexpedition.dragon.variants.DragonVariant;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class MovesetController {
    public static final double ARENA_RANGE_SQ = 160.0D * 160.0D;
    private static final double LASER_RANGE = 110.0D;
    private static final int LASER_TELEGRAPH_TICKS = 40;
    private static final int LASER_FIRING_TICKS = 80;
    private static final int DIVE_ASCEND_TICKS = 25;
    private static final int TRIPLE_DIVE_PASS_ASCEND_TICKS = 15;
    private static final int DIVE_MAX_TICKS = 50;
    private static final int FIREBALL_TELEGRAPH_TICKS = 12;
    private static final int FIREBALL_INTERVAL_TICKS = 6;

    public enum ActiveMove {
        NONE,
        DIVE,
        TRIPLE_DIVE,
        LASER,
        CHAIN_FIREBALL
    }

    private enum DiveSubPhase {
        ASCEND,
        DIVE
    }

    private enum LaserSubPhase {
        TELEGRAPH,
        FIRING
    }

    private UUID activeDragonUuid;
    private ActiveMove activeMove = ActiveMove.NONE;
    private DiveSubPhase diveSubPhase = DiveSubPhase.ASCEND;
    private LaserSubPhase laserSubPhase = LaserSubPhase.TELEGRAPH;
    private int moveTick;
    private int passesRemaining;
    private int fireballsRemaining;
    private int fireballIndex;

    private int diveCooldown;
    private int tripleDiveCooldown;
    private int laserCooldown;
    private int chainFireballCooldown;

    private Vec3 moveTarget = new Vec3(0.5D, 64.0D, 0.5D);
    private Vec3 pendingStepVelocity = Vec3.ZERO;
    private float laserYaw;
    private float laserPitch;

    private final Set<UUID> trackedFireballUuids = ConcurrentHashMap.newKeySet();
    private final Set<UUID> trackedCloudUuids = ConcurrentHashMap.newKeySet();

    public static int scaleCooldownTicks(int baseTicks, double cooldownMultiplier, int minTicks) {
        int clampedMin = Math.max(1, minTicks);
        if (!Double.isFinite(cooldownMultiplier) || cooldownMultiplier <= 0.0D) {
            return Math.max(clampedMin, baseTicks);
        }
        long scaled = Math.round(baseTicks * cooldownMultiplier);
        if (scaled < clampedMin) {
            return clampedMin;
        }
        if (scaled > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return (int) scaled;
    }

    public static double scaleDamage(double baseDamage, double damageMultiplier) {
        if (!Double.isFinite(baseDamage) || baseDamage <= 0.0D) {
            return 0.0D;
        }
        if (!Double.isFinite(damageMultiplier) || damageMultiplier <= 0.0D) {
            return baseDamage;
        }
        return baseDamage * damageMultiplier;
    }

    public static boolean shouldTriggerTripleDive(double healthRatio, double threshold) {
        if (!Double.isFinite(healthRatio) || !Double.isFinite(threshold)) {
            return false;
        }
        return healthRatio > 0.0D && healthRatio <= threshold;
    }

    public static double computeShockwaveDamage(double baseDamage, double distance, double radius) {
        if (!Double.isFinite(baseDamage) || baseDamage <= 0.0D
                || !Double.isFinite(distance) || distance < 0.0D
                || !Double.isFinite(radius) || radius <= 0.0D
                || distance > radius) {
            return 0.0D;
        }
        double inner = radius * 0.5D;
        if (distance <= inner) {
            return baseDamage;
        }
        double t = (distance - inner) / Math.max(1e-6D, radius - inner);
        return Math.max(0.0D, baseDamage * (1.0D - 0.5D * t));
    }

    private static boolean isFiniteVec(Vec3 v) {
        return v != null && Double.isFinite(v.x) && Double.isFinite(v.y) && Double.isFinite(v.z);
    }

    public static float computeLaserDragonYaw(float laserYaw) {
        if (!Float.isFinite(laserYaw)) {
            return 0.0F;
        }
        return Mth.wrapDegrees(laserYaw + 180.0F);
    }

    public static boolean rayHitsAabb(Vec3 start, Vec3 end, AABB box, double inflateRadius) {
        if (!isFiniteVec(start) || !isFiniteVec(end) || box == null) {
            return false;
        }
        double inflate = Double.isFinite(inflateRadius) && inflateRadius > 0.0D ? inflateRadius : 0.0D;
        AABB expanded = box.inflate(inflate);
        if (start.equals(end)) {
            return start.x >= expanded.minX && start.x <= expanded.maxX
                    && start.y >= expanded.minY && start.y <= expanded.maxY
                    && start.z >= expanded.minZ && start.z <= expanded.maxZ;
        }
        if (expanded.contains(start) || expanded.contains(end)) {
            return true;
        }
        return expanded.clip(start, end).isPresent();
    }

    public static List<Vec3> computeRingPoints(double cx, double y, double cz, double radius, int count) {
        int safeCount = Math.max(1, count);
        double safeRadius = (Double.isFinite(radius) && radius >= 0.0D) ? radius : 0.0D;
        List<Vec3> points = new ArrayList<>(safeCount);
        double step = (Math.PI * 2.0D) / safeCount;
        for (int i = 0; i < safeCount; i++) {
            double angle = i * step;
            points.add(new Vec3(
                    cx + Math.cos(angle) * safeRadius,
                    y,
                    cz + Math.sin(angle) * safeRadius
            ));
        }
        return points;
    }

    public static Vec3 computeDiveVelocity(Vec3 from, Vec3 to, double speed) {
        if (!isFiniteVec(from) || !isFiniteVec(to) || !Double.isFinite(speed) || speed <= 0.0D) {
            return Vec3.ZERO;
        }
        Vec3 delta = to.subtract(from);
        if (!isFiniteVec(delta)) {
            return Vec3.ZERO;
        }
        double lenSq = delta.lengthSqr();
        if (!Double.isFinite(lenSq)) {
            double maxAbs = Math.max(Math.abs(delta.x), Math.max(Math.abs(delta.y), Math.abs(delta.z)));
            if (!Double.isFinite(maxAbs) || maxAbs <= 0.0D) {
                return Vec3.ZERO;
            }
            return new Vec3(delta.x / maxAbs, delta.y / maxAbs, delta.z / maxAbs).normalize().scale(speed);
        }
        if (lenSq < 1e-8D) {
            return Vec3.ZERO;
        }
        Vec3 result = delta.scale(speed / Math.sqrt(lenSq));
        return isFiniteVec(result) ? result : Vec3.ZERO;
    }

    public static List<Vec3> computeFireballSalvoDirections(Vec3 forward, int count, double spreadRadians) {
        int safeCount = Math.max(1, count);
        Vec3 fallback = new Vec3(0.0D, -0.2D, 1.0D).normalize();
        Vec3 base;
        if (!isFiniteVec(forward)) {
            base = fallback;
        } else {
            double lenSq = forward.lengthSqr();
            if (!Double.isFinite(lenSq) || lenSq < 1e-8D) {
                base = fallback;
            } else {
                Vec3 normalized = forward.normalize();
                base = isFiniteVec(normalized) ? normalized : fallback;
            }
        }
        double safeSpread = Double.isFinite(spreadRadians) ? spreadRadians : 0.0D;
        List<Vec3> dirs = new ArrayList<>(safeCount);
        if (safeCount == 1 || safeSpread == 0.0D) {
            for (int i = 0; i < safeCount; i++) {
                dirs.add(base);
            }
            return dirs;
        }
        double half = (safeCount - 1) / 2.0D;
        for (int i = 0; i < safeCount; i++) {
            double yawOffset = (i - half) * safeSpread;
            double cos = Math.cos(yawOffset);
            double sin = Math.sin(yawOffset);
            double rx = base.x * cos - base.z * sin;
            double rz = base.x * sin + base.z * cos;
            dirs.add(new Vec3(rx, base.y, rz).normalize());
        }
        return dirs;
    }

    public void onFightStart(ServerLevel level, EnderDragon dragon, DragonVariant variant) {
        resetInternal();
        if (dragon == null) {
            return;
        }
        this.activeDragonUuid = dragon.getUUID();

        DragonConfig.VariantConfig vc = DragonConfig.get().getVariantConfig(variant);
        DragonConfig.MovesetConfig mc = DragonConfig.get().movesets;
        this.diveCooldown = scaleCooldownTicks(mc.diveCooldownTicks / 2, vc.cooldownMultiplier, 40);
        this.tripleDiveCooldown = scaleCooldownTicks(mc.tripleDiveCooldownTicks / 2, vc.cooldownMultiplier, 60);
        this.laserCooldown = scaleCooldownTicks(mc.laserCooldownTicks / 2, vc.cooldownMultiplier, 60);
        this.chainFireballCooldown = scaleCooldownTicks(mc.chainFireballCooldownTicks / 2, vc.cooldownMultiplier, 30);

        if (level != null) {
            emitSpawnEffects(level, dragon);
        }
    }

    private static void emitSpawnEffects(ServerLevel level, EnderDragon dragon) {
        double x = dragon.getX();
        double y = dragon.getY();
        double z = dragon.getZ();
        level.playSound(null, x, y, z, SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 6.0F, 0.85F);
        level.playSound(null, x, y, z, SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 4.0F, 0.9F);
        level.playSound(null, x, y, z, SoundEvents.END_PORTAL_SPAWN, SoundSource.HOSTILE, 3.0F, 1.0F);

        level.sendParticles(ParticleTypes.CLOUD, x, y + 2.0D, z, 60, 4.0D, 2.5D, 4.0D, 0.08D);
        level.sendParticles(PowerParticleOption.create(ParticleTypes.DRAGON_BREATH, 1.0F),
                x, y + 1.5D, z, 80, 4.5D, 2.5D, 4.5D, 0.06D);
        level.sendParticles(ParticleTypes.END_ROD, x, y + 2.0D, z, 55, 3.5D, 3.0D, 3.5D, 0.12D);
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, x, y + 1.0D, z, 2, 1.0D, 0.5D, 1.0D, 0.0D);
    }

    public void onTick(FightState state, ServerLevel level, EnderDragon dragon, VariantBehavior behavior) {
        if (state == null || state.isEnded() || level == null || dragon == null || !dragon.isAlive()) {
            return;
        }
        this.activeDragonUuid = dragon.getUUID();
        VariantBehavior resolvedBehavior = behavior != null ? behavior : VariantBehavior.defaultFor(state.getVariant());

        updateTrackedFireballs(level, resolvedBehavior);

        DragonConfig.VariantConfig vc = DragonConfig.get().getVariantConfig(state.getVariant());
        DragonConfig.MovesetConfig mc = DragonConfig.get().movesets;

        if (activeMove == ActiveMove.NONE) {
            if (diveCooldown > 0) diveCooldown--;
            if (tripleDiveCooldown > 0) tripleDiveCooldown--;
            if (laserCooldown > 0) laserCooldown--;
            if (chainFireballCooldown > 0) chainFireballCooldown--;

            List<ServerPlayer> targets = getArenaPlayers(level);
            if (targets.isEmpty()) {
                return;
            }

            double healthRatio = dragon.getMaxHealth() > 0.0F ? (dragon.getHealth() / dragon.getMaxHealth()) : 1.0D;
            if (shouldTriggerTripleDive(healthRatio, mc.tripleDiveHpThreshold) && tripleDiveCooldown <= 0) {
                startDive(level, dragon, targets, true, vc, mc, resolvedBehavior);
            } else if (laserCooldown <= 0) {
                startLaser(level, dragon, targets, vc, mc, resolvedBehavior);
            } else if (diveCooldown <= 0) {
                startDive(level, dragon, targets, false, vc, mc, resolvedBehavior);
            } else if (chainFireballCooldown <= 0) {
                startChainFireball(level, dragon, vc, mc, resolvedBehavior);
            }
            return;
        }

        moveTick++;
        switch (activeMove) {
            case DIVE, TRIPLE_DIVE -> tickDive(level, dragon, vc, mc);
            case LASER -> tickLaser(level, dragon, vc, mc);
            case CHAIN_FIREBALL -> tickChainFireball(level, dragon, resolvedBehavior);
            case NONE -> {
            }
        }
    }

    private void updateTrackedFireballs(ServerLevel level, VariantBehavior behavior) {
        if (trackedFireballUuids.isEmpty()) {
            return;
        }
        Iterator<UUID> it = trackedFireballUuids.iterator();
        while (it.hasNext()) {
            UUID uuid = it.next();
            Entity entity = level.getEntity(uuid);
            if (!(entity instanceof DragonFireball fireball) || fireball.isRemoved() || !fireball.isAlive()) {
                it.remove();
                continue;
            }
            Vec3 currentVel = fireball.getDeltaMovement();
            Vec3 modified = behavior.modifyFireballVelocity(fireball, currentVel, level);
            if (modified != null && !modified.equals(currentVel)) {
                fireball.setDeltaMovement(modified);
                fireball.needsSync = true;
            }
        }
    }

    private void startDive(
            ServerLevel level,
            EnderDragon dragon,
            List<ServerPlayer> targets,
            boolean triple,
            DragonConfig.VariantConfig vc,
            DragonConfig.MovesetConfig mc,
            VariantBehavior behavior
    ) {
        ServerPlayer target = targets.get(level.getRandom().nextInt(targets.size()));
        int groundY = level.getHeight(Heightmap.Types.MOTION_BLOCKING, target.getBlockX(), target.getBlockZ());
        this.moveTarget = new Vec3(target.getX(), Math.max(34.0D, groundY), target.getZ());
        this.activeMove = triple ? ActiveMove.TRIPLE_DIVE : ActiveMove.DIVE;
        this.diveSubPhase = DiveSubPhase.ASCEND;
        this.moveTick = 0;
        this.passesRemaining = triple ? 3 : 1;

        double baseCd = triple ? mc.tripleDiveCooldownTicks : mc.diveCooldownTicks;
        int scaledCd = scaleCooldownTicks(
                (int) Math.round(behavior.modifyMovesetCooldown(baseCd)),
                vc.cooldownMultiplier,
                40
        );
        if (triple) {
            this.tripleDiveCooldown = scaledCd;
        } else {
            this.diveCooldown = scaledCd;
        }

        dragon.getPhaseManager().setPhase(EnderDragonPhase.HOVERING);
        level.playSound(null, dragon.getX(), dragon.getY(), dragon.getZ(),
                SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 5.0F, triple ? 1.15F : 0.9F);
    }

    private void tickDive(
            ServerLevel level,
            EnderDragon dragon,
            DragonConfig.VariantConfig vc,
            DragonConfig.MovesetConfig mc
    ) {
        boolean isTriple = activeMove == ActiveMove.TRIPLE_DIVE;
        int ascendDuration = (isTriple && passesRemaining < 3) ? TRIPLE_DIVE_PASS_ASCEND_TICKS : DIVE_ASCEND_TICKS;

        if (diveSubPhase == DiveSubPhase.ASCEND) {
            // Re-track nearest active player during ascend
            List<ServerPlayer> targets = getArenaPlayers(level);
            if (!targets.isEmpty()) {
                ServerPlayer target = targets.get(0);
                int groundY = level.getHeight(Heightmap.Types.MOTION_BLOCKING, target.getBlockX(), target.getBlockZ());
                this.moveTarget = new Vec3(target.getX(), Math.max(34.0D, groundY), target.getZ());
            }

            Vec3 ascendGoal = new Vec3(moveTarget.x, Math.max(moveTarget.y + 28.0D, 92.0D), moveTarget.z);
            this.pendingStepVelocity = computeDiveVelocity(dragon.position(), ascendGoal, 1.35D);

            if (moveTick % 5 == 0) {
                emitGroundWarningRing(level, moveTarget.x, moveTarget.y + 0.35D, moveTarget.z, 5.0D, 16);
                level.playSound(null, dragon.getX(), dragon.getY(), dragon.getZ(),
                        SoundEvents.ENDER_DRAGON_FLAP, SoundSource.HOSTILE, 3.5F, 1.1F);
            }

            if (moveTick >= ascendDuration) {
                this.diveSubPhase = DiveSubPhase.DIVE;
                this.moveTick = 0;
                level.playSound(null, dragon.getX(), dragon.getY(), dragon.getZ(),
                        SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 5.0F, 1.25F);
            }
            return;
        }

        // DiveSubPhase.DIVE
        Vec3 impactGoal = new Vec3(moveTarget.x, moveTarget.y + 1.5D, moveTarget.z);
        double diveSpeed = isTriple ? 2.2D : 1.9D;
        this.pendingStepVelocity = computeDiveVelocity(dragon.position(), impactGoal, diveSpeed);

        if (isTriple) {
            level.sendParticles(PowerParticleOption.create(ParticleTypes.DRAGON_BREATH, 1.0F),
                    dragon.getX(), dragon.getY() + 1.0D, dragon.getZ(), 10, 1.2D, 0.8D, 1.2D, 0.03D);
            level.sendParticles(ParticleTypes.END_ROD,
                    dragon.getX(), dragon.getY() + 1.0D, dragon.getZ(), 6, 1.0D, 0.6D, 1.0D, 0.04D);
            level.sendParticles(ParticleTypes.WITCH,
                    dragon.getX(), dragon.getY() + 1.0D, dragon.getZ(), 6, 1.0D, 0.6D, 1.0D, 0.03D);
        } else if (moveTick % 2 == 0) {
            level.sendParticles(ParticleTypes.CLOUD,
                    dragon.getX(), dragon.getY() + 1.0D, dragon.getZ(), 5, 0.8D, 0.4D, 0.8D, 0.02D);
        }

        double distSq = dragon.position().distanceToSqr(impactGoal);
        boolean impacted = distSq <= 16.0D || dragon.getY() <= moveTarget.y + 3.5D || dragon.verticalCollision || moveTick >= DIVE_MAX_TICKS;
        if (!impacted) {
            return;
        }

        triggerDiveShockwave(level, dragon, vc, mc);
        passesRemaining--;
        if (passesRemaining > 0) {
            List<ServerPlayer> nextTargets = getArenaPlayers(level);
            if (!nextTargets.isEmpty()) {
                ServerPlayer next = nextTargets.get(level.getRandom().nextInt(nextTargets.size()));
                int gy = level.getHeight(Heightmap.Types.MOTION_BLOCKING, next.getBlockX(), next.getBlockZ());
                this.moveTarget = new Vec3(next.getX(), Math.max(34.0D, gy), next.getZ());
            }
            this.diveSubPhase = DiveSubPhase.ASCEND;
            this.moveTick = 0;
        } else {
            finishMove(dragon);
        }
    }

    private void triggerDiveShockwave(
            ServerLevel level,
            EnderDragon dragon,
            DragonConfig.VariantConfig vc,
            DragonConfig.MovesetConfig mc
    ) {
        double ix = dragon.getX();
        double iy = Math.max(moveTarget.y, dragon.getY());
        double iz = dragon.getZ();
        double radius = mc.diveShockwaveRadius;
        double scaledBaseDamage = scaleDamage(mc.diveImpactDamage, vc.damageMultiplier);

        level.playSound(null, ix, iy, iz, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 4.0F, 0.85F);
        level.playSound(null, ix, iy, iz, SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 3.5F, 1.0F);
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, ix, iy + 0.5D, iz, 2, 0.5D, 0.3D, 0.5D, 0.0D);
        level.sendParticles(ParticleTypes.SONIC_BOOM, ix, iy + 1.0D, iz, 2, 1.0D, 0.5D, 1.0D, 0.0D);

        for (Vec3 pt : computeRingPoints(ix, iy + 0.5D, iz, Math.min(radius, 8.0D), 24)) {
            level.sendParticles(PowerParticleOption.create(ParticleTypes.DRAGON_BREATH, 1.0F),
                    pt.x, pt.y, pt.z, 2, 0.2D, 0.1D, 0.2D, 0.02D);
        }

        DamageSource mobAttack = level.damageSources().mobAttack(dragon);
        double radiusSq = radius * radius;
        for (ServerPlayer player : getArenaPlayers(level)) {
            double dSq = player.position().distanceToSqr(ix, iy, iz);
            if (dSq <= radiusSq) {
                double dist = Math.sqrt(dSq);
                float dmg = (float) computeShockwaveDamage(scaledBaseDamage, dist, radius);
                if (dmg > 0.0F) {
                    player.hurtServer(level, mobAttack, dmg);
                }
                double dx = player.getX() - ix;
                double dz = player.getZ() - iz;
                if (dx * dx + dz * dz < 1e-4D) {
                    dx = 0.5D;
                    dz = 0.0D;
                }
                player.knockback(1.6D, -dx, -dz, mobAttack, dmg);
                player.push(0.0D, 0.42D, 0.0D);
                player.needsSync = true;
            }
        }
    }

    private void startLaser(
            ServerLevel level,
            EnderDragon dragon,
            List<ServerPlayer> targets,
            DragonConfig.VariantConfig vc,
            DragonConfig.MovesetConfig mc,
            VariantBehavior behavior
    ) {
        ServerPlayer target = targets.get(level.getRandom().nextInt(targets.size()));
        Vec3 headPos = dragon.head.position();
        Vec3 dir = target.getEyePosition().subtract(headPos);
        this.laserYaw = (float) (Mth.atan2(dir.z, dir.x) * (180.0D / Math.PI)) - 90.0F - 35.0F;
        double horiz = Math.sqrt(dir.x * dir.x + dir.z * dir.z);
        this.laserPitch = (float) -(Mth.atan2(dir.y, Math.max(0.001D, horiz)) * (180.0D / Math.PI));

        this.activeMove = ActiveMove.LASER;
        this.laserSubPhase = LaserSubPhase.TELEGRAPH;
        this.moveTick = 0;
        this.laserCooldown = scaleCooldownTicks(
                (int) Math.round(behavior.modifyMovesetCooldown(mc.laserCooldownTicks)),
                vc.cooldownMultiplier,
                60
        );

        dragon.getPhaseManager().setPhase(EnderDragonPhase.HOVERING);
        level.playSound(null, headPos.x, headPos.y, headPos.z,
                SoundEvents.BELL_RESONATE, SoundSource.HOSTILE, 5.0F, 0.7F);
        level.playSound(null, headPos.x, headPos.y, headPos.z,
                SoundEvents.WARDEN_SONIC_CHARGE, SoundSource.HOSTILE, 4.0F, 1.0F);
    }

    private void tickLaser(
            ServerLevel level,
            EnderDragon dragon,
            DragonConfig.VariantConfig vc,
            DragonConfig.MovesetConfig mc
    ) {
        Vec3 hoverAnchor = new Vec3(0.5D, 86.0D, 0.5D);
        Vec3 toAnchor = hoverAnchor.subtract(dragon.position());
        this.pendingStepVelocity = toAnchor.lengthSqr() > 4.0D
                ? computeDiveVelocity(dragon.position(), hoverAnchor, 0.65D)
                : new Vec3(0.0D, Mth.clamp((86.0D - dragon.getY()) * 0.08D, -0.25D, 0.25D), 0.0D);

        Vec3 headPos = dragon.head.position();
        if (laserSubPhase == LaserSubPhase.TELEGRAPH) {
            level.sendParticles(PowerParticleOption.create(ParticleTypes.DRAGON_BREATH, 1.0F),
                    headPos.x, headPos.y, headPos.z, 8, 0.8D, 0.8D, 0.8D, 0.05D);
            level.sendParticles(ParticleTypes.END_ROD,
                    headPos.x, headPos.y, headPos.z, 4, 0.5D, 0.5D, 0.5D, 0.03D);

            if (moveTick >= LASER_TELEGRAPH_TICKS) {
                this.laserSubPhase = LaserSubPhase.FIRING;
                this.moveTick = 0;
                level.playSound(null, headPos.x, headPos.y, headPos.z,
                        SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 5.0F, 0.8F);
            }
            return;
        }

        // LaserSubPhase.FIRING
        this.laserYaw += 2.2F;
        double yawRad = Math.toRadians(this.laserYaw + 90.0F);
        double pitchRad = Math.toRadians(-this.laserPitch);
        double cosPitch = Math.cos(pitchRad);
        Vec3 rayDir = new Vec3(
                Math.cos(yawRad) * cosPitch,
                Math.sin(pitchRad),
                Math.sin(yawRad) * cosPitch
        ).normalize();
        Vec3 rawEnd = headPos.add(rayDir.scale(LASER_RANGE));
        BlockHitResult blockHit = level.clip(new ClipContext(
                headPos,
                rawEnd,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                dragon
        ));
        Vec3 hitEnd = blockHit.getType() != HitResult.Type.MISS ? blockHit.getLocation() : rawEnd;

        double len = headPos.distanceTo(hitEnd);
        int steps = Math.max(1, (int) (len / 2.2D));
        for (int i = 1; i <= steps; i++) {
            double t = (double) i / steps;
            Vec3 pt = headPos.lerp(hitEnd, t);
            level.sendParticles(ParticleTypes.END_ROD, pt.x, pt.y, pt.z, 1, 0.05D, 0.05D, 0.05D, 0.0D);
            if (i % 2 == 0) {
                level.sendParticles(PowerParticleOption.create(ParticleTypes.DRAGON_BREATH, 1.0F),
                        pt.x, pt.y, pt.z, 1, 0.1D, 0.1D, 0.1D, 0.0D);
            }
        }

        if (moveTick % 5 == 0) {
            float dmg = (float) scaleDamage(mc.laserDamage, vc.damageMultiplier);
            DamageSource laserSource = level.damageSources().indirectMagic(dragon, dragon);
            for (ServerPlayer player : getArenaPlayers(level)) {
                if (rayHitsAabb(headPos, hitEnd, player.getBoundingBox(), 1.15D)) {
                    if (dmg > 0.0F) {
                        player.hurtServer(level, laserSource, dmg);
                    }
                    level.sendParticles(ParticleTypes.EXPLOSION,
                            player.getX(), player.getY() + 1.0D, player.getZ(), 2, 0.2D, 0.2D, 0.2D, 0.0D);
                }
            }
        }

        if (moveTick >= LASER_FIRING_TICKS) {
            finishMove(dragon);
        }
    }

    private void startChainFireball(
            ServerLevel level,
            EnderDragon dragon,
            DragonConfig.VariantConfig vc,
            DragonConfig.MovesetConfig mc,
            VariantBehavior behavior
    ) {
        this.activeMove = ActiveMove.CHAIN_FIREBALL;
        this.moveTick = 0;
        this.fireballIndex = 0;
        this.fireballsRemaining = Math.max(1, behavior.modifyChainFireballCount(mc.chainFireballCount));
        this.chainFireballCooldown = scaleCooldownTicks(
                (int) Math.round(behavior.modifyMovesetCooldown(mc.chainFireballCooldownTicks)),
                vc.cooldownMultiplier,
                30
        );
        Vec3 headPos = dragon.head.position();
        level.playSound(null, headPos.x, headPos.y, headPos.z,
                SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 4.0F, 1.1F);
    }

    private void tickChainFireball(ServerLevel level, EnderDragon dragon, VariantBehavior behavior) {
        Vec3 headPos = dragon.head.position();
        if (moveTick < FIREBALL_TELEGRAPH_TICKS) {
            if (moveTick % 3 == 0) {
                level.sendParticles(PowerParticleOption.create(ParticleTypes.DRAGON_BREATH, 1.0F),
                        headPos.x, headPos.y, headPos.z, 5, 0.5D, 0.5D, 0.5D, 0.04D);
            }
            return;
        }

        int fireElapsed = moveTick - FIREBALL_TELEGRAPH_TICKS;
        if (fireElapsed % FIREBALL_INTERVAL_TICKS == 0 && fireballsRemaining > 0) {
            List<ServerPlayer> targets = getArenaPlayers(level);
            if (targets.isEmpty()) {
                finishMove(dragon);
                return;
            }
            ServerPlayer target = targets.get(fireballIndex % targets.size());
            Vec3 aim = target.getEyePosition().subtract(headPos).normalize();
            List<Vec3> spreadDirs = computeFireballSalvoDirections(aim, 3, 0.12D);
            Vec3 shotDir = spreadDirs.get(fireballIndex % spreadDirs.size());

            DragonFireball fireball = new DragonFireball(level, dragon, shotDir);
            fireball.setPos(headPos.x, headPos.y, headPos.z);
            fireball.accelerationPower = 0.16D;
            Vec3 initialVel = shotDir.scale(0.85D);
            Vec3 modifiedVel = behavior.modifyFireballVelocity(fireball, initialVel, level);
            fireball.setDeltaMovement(modifiedVel != null ? modifiedVel : initialVel);
            fireball.needsSync = true;

            if (level.addFreshEntity(fireball)) {
                trackedFireballUuids.add(fireball.getUUID());
            }
            level.playSound(null, headPos.x, headPos.y, headPos.z,
                    SoundEvents.ENDER_DRAGON_SHOOT, SoundSource.HOSTILE, 4.0F, 1.0F);

            fireballIndex++;
            fireballsRemaining--;
            if (fireballsRemaining <= 0) {
                finishMove(dragon);
            }
        }
    }

    private void finishMove(EnderDragon dragon) {
        this.activeMove = ActiveMove.NONE;
        this.moveTick = 0;
        this.pendingStepVelocity = Vec3.ZERO;
        if (dragon != null && dragon.isAlive()
                && dragon.getPhaseManager().getCurrentPhase().getPhase() != EnderDragonPhase.DYING) {
            dragon.getPhaseManager().setPhase(EnderDragonPhase.HOLDING_PATTERN);
        }
    }

    public void abortActiveMove(EnderDragon dragon) {
        if (dragon != null && this.activeDragonUuid != null && !dragon.getUUID().equals(this.activeDragonUuid)) {
            return;
        }
        this.activeMove = ActiveMove.NONE;
        this.diveSubPhase = DiveSubPhase.ASCEND;
        this.laserSubPhase = LaserSubPhase.TELEGRAPH;
        this.moveTick = 0;
        this.passesRemaining = 0;
        this.fireballsRemaining = 0;
        this.fireballIndex = 0;
        this.pendingStepVelocity = Vec3.ZERO;
    }

    public boolean isMovementOverridden(EnderDragon dragon) {
        if (dragon == null
                || !dragon.isAlive()
                || dragon.isDeadOrDying()
                || dragon.getPhaseManager().getCurrentPhase().getPhase() == EnderDragonPhase.DYING
                || activeDragonUuid == null
                || !dragon.getUUID().equals(activeDragonUuid)) {
            return false;
        }
        return activeMove == ActiveMove.DIVE
                || activeMove == ActiveMove.TRIPLE_DIVE
                || activeMove == ActiveMove.LASER;
    }

    public void applyMovementStep(EnderDragon dragon, ServerLevel level) {
        if (dragon == null || level == null) {
            return;
        }
        if (activeMove == ActiveMove.LASER) {
            dragon.setYRot(computeLaserDragonYaw(this.laserYaw));
            dragon.setXRot(this.laserPitch);
        } else if (pendingStepVelocity.lengthSqr() > 1e-6D) {
            float targetYaw = (float) (Mth.atan2(pendingStepVelocity.z, pendingStepVelocity.x) * (180.0D / Math.PI)) + 90.0F;
            dragon.setYRot(targetYaw);
        }
        dragon.setDeltaMovement(pendingStepVelocity);
        dragon.move(MoverType.SELF, pendingStepVelocity);
    }

    public void onDragonDeathBurst(ServerLevel level, EnderDragon dragon) {
        this.activeMove = ActiveMove.NONE;
        this.pendingStepVelocity = Vec3.ZERO;
        if (level == null || dragon == null) {
            return;
        }
        double x = dragon.getX();
        double y = dragon.getY();
        double z = dragon.getZ();
        level.playSound(null, x, y, z, SoundEvents.WARDEN_DEATH, SoundSource.HOSTILE, 5.0F, 0.85F);
        level.playSound(null, x, y, z, SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 4.5F, 0.7F);
        level.playSound(null, x, y, z, SoundEvents.ENDER_DRAGON_HURT, SoundSource.HOSTILE, 4.0F, 0.65F);

        level.sendParticles(PowerParticleOption.create(ParticleTypes.DRAGON_BREATH, 1.0F),
                x, y + 2.0D, z, 90, 4.5D, 3.0D, 4.5D, 0.08D);
        level.sendParticles(ParticleTypes.END_ROD, x, y + 2.0D, z, 70, 4.0D, 3.5D, 4.0D, 0.14D);
        level.sendParticles(ParticleTypes.CLOUD, x, y + 2.0D, z, 50, 3.5D, 2.5D, 3.5D, 0.06D);
    }

    public void onDeathTick(ServerLevel level, EnderDragon dragon, int deathTime) {
        if (level == null || dragon == null) {
            return;
        }
        double x = dragon.getX();
        double y = dragon.getY() + 2.0D;
        double z = dragon.getZ();

        if (deathTime % 5 == 0) {
            level.sendParticles(PowerParticleOption.create(ParticleTypes.DRAGON_BREATH, 1.0F),
                    x, y, z, 18, 2.5D, 2.0D, 2.5D, 0.06D);
            level.sendParticles(ParticleTypes.END_ROD, x, y, z, 12, 2.5D, 2.5D, 2.5D, 0.1D);
        }
        if (deathTime == 40 || deathTime == 90 || deathTime == 140) {
            level.playSound(null, x, y, z, SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 4.0F, 0.9F);
        }
        if (deathTime == 190) {
            level.playSound(null, x, y, z, SoundEvents.END_PORTAL_SPAWN, SoundSource.HOSTILE, 4.5F, 1.0F);
            level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, x, y, z, 3, 1.5D, 1.5D, 1.5D, 0.0D);
            level.sendParticles(ParticleTypes.CLOUD, x, y, z, 75, 4.5D, 3.5D, 4.5D, 0.12D);
        }
    }

    public void registerAreaCloud(UUID cloudUuid) {
        if (cloudUuid != null) {
            trackedCloudUuids.add(cloudUuid);
        }
    }

    public void cleanup(ServerLevel level) {
        if (level != null) {
            for (UUID fbUuid : trackedFireballUuids) {
                Entity fb = level.getEntity(fbUuid);
                if (fb != null && !fb.isRemoved()) {
                    fb.discard();
                }
            }
            for (UUID cloudUuid : trackedCloudUuids) {
                Entity cloud = level.getEntity(cloudUuid);
                if (cloud != null && !cloud.isRemoved()) {
                    cloud.discard();
                }
            }
        }
        resetInternal();
    }

    private void resetInternal() {
        this.activeDragonUuid = null;
        this.activeMove = ActiveMove.NONE;
        this.diveSubPhase = DiveSubPhase.ASCEND;
        this.laserSubPhase = LaserSubPhase.TELEGRAPH;
        this.moveTick = 0;
        this.passesRemaining = 0;
        this.fireballsRemaining = 0;
        this.fireballIndex = 0;
        this.diveCooldown = 0;
        this.tripleDiveCooldown = 0;
        this.laserCooldown = 0;
        this.chainFireballCooldown = 0;
        this.pendingStepVelocity = Vec3.ZERO;
        this.trackedFireballUuids.clear();
        this.trackedCloudUuids.clear();
    }

    private static List<ServerPlayer> getArenaPlayers(ServerLevel level) {
        List<ServerPlayer> active = new ArrayList<>();
        for (ServerPlayer player : level.players()) {
            if (player.isAlive() && !player.isSpectator() && !player.isCreative()) {
                double dx = player.getX();
                double dy = player.getY() - 80.0D;
                double dz = player.getZ();
                if (dx * dx + dy * dy + dz * dz <= ARENA_RANGE_SQ) {
                    active.add(player);
                }
            }
        }
        return active;
    }

    private static void emitGroundWarningRing(ServerLevel level, double cx, double y, double cz, double radius, int count) {
        for (Vec3 pt : computeRingPoints(cx, y, cz, radius, count)) {
            level.sendParticles(PowerParticleOption.create(ParticleTypes.DRAGON_BREATH, 1.0F),
                    pt.x, pt.y, pt.z, 1, 0.05D, 0.02D, 0.05D, 0.0D);
            level.sendParticles(ParticleTypes.END_ROD, pt.x, pt.y, pt.z, 1, 0.02D, 0.02D, 0.02D, 0.0D);
        }
    }
}
