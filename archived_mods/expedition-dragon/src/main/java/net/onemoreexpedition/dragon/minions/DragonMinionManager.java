package net.onemoreexpedition.dragon.minions;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.monster.Enderman;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.hurtingprojectile.DragonFireball;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.onemoreexpedition.dragon.config.DragonConfig;
import net.onemoreexpedition.dragon.core.DragonFightManager;
import net.onemoreexpedition.dragon.core.FightState;
import net.onemoreexpedition.dragon.core.MinionHooks;
import net.onemoreexpedition.dragon.core.VariantBehavior;
import net.onemoreexpedition.dragon.variants.DragonVariant;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class DragonMinionManager implements MinionHooks {
    public static final String SHADOW_PHANTOM_IMMUNE_MSG = "\u00a7cThis Shadow Phantom cannot be killed!";
    public static final String NO_ELYTRA_MSG = "\u00a7cElytra gliding is disabled during the Dragon fight!";
    public static final String SPAWN_PULL_MSG = "\u00a7eThe Dragon has awakened! You were pulled safely back to the main island.";

    private static final DragonMinionManager INSTANCE = new DragonMinionManager();
    private static final long MESSAGE_COOLDOWN_MS = 1000L;

    private final Map<UUID, UUID> shadowPhantomToCrystal = new ConcurrentHashMap<>();
    private final Set<UUID> altarSentinelVexes = ConcurrentHashMap.newKeySet();
    private final Set<UUID> tunnelVexes = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Integer> playerAirborneTicks = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastPhantomMsgMs = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastElytraMsgMs = new ConcurrentHashMap<>();
    private final Set<Integer> triggeredThresholdPercents = ConcurrentHashMap.newKeySet();

    public static DragonMinionManager get() {
        return INSTANCE;
    }

    public boolean isInvulnerableMinion(UUID entityUuid) {
        if (entityUuid == null) {
            return false;
        }
        return shadowPhantomToCrystal.containsKey(entityUuid) || altarSentinelVexes.contains(entityUuid);
    }

    public boolean isShadowPhantom(UUID entityUuid) {
        return entityUuid != null && shadowPhantomToCrystal.containsKey(entityUuid);
    }

    public Set<UUID> getShadowPhantomsForCrystal(UUID crystalUuid) {
        if (crystalUuid == null) {
            return Collections.emptySet();
        }
        Set<UUID> result = new HashSet<>();
        for (Map.Entry<UUID, UUID> e : shadowPhantomToCrystal.entrySet()) {
            if (crystalUuid.equals(e.getValue())) {
                result.add(e.getKey());
            }
        }
        return result;
    }

    public int getAirborneTicks(UUID playerUuid) {
        return playerUuid != null ? playerAirborneTicks.getOrDefault(playerUuid, 0) : 0;
    }

    public int recordAirborneState(UUID playerUuid, boolean airborne) {
        if (playerUuid == null) {
            return 0;
        }
        if (!airborne) {
            playerAirborneTicks.remove(playerUuid);
            return 0;
        }
        return playerAirborneTicks.merge(playerUuid, 1, Integer::sum);
    }

    public static boolean isEnclosureConditionMet(boolean solidRoofWithinTwoBlocks, int blockedCardinalSides) {
        return solidRoofWithinTwoBlocks && blockedCardinalSides >= 3;
    }

    public static boolean isPlayerEnclosedInBlocks(ServerLevel level, BlockPos feetPos) {
        if (level == null || feetPos == null) {
            return false;
        }
        BlockPos headPos = feetPos.above();
        boolean solidRoof = isSolidEnclosureBlock(level, headPos.above(1))
                || isSolidEnclosureBlock(level, headPos.above(2));
        if (!solidRoof) {
            return false;
        }
        int blockedSides = 0;
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            if (isSolidEnclosureBlock(level, feetPos.relative(dir))
                    || isSolidEnclosureBlock(level, headPos.relative(dir))) {
                blockedSides++;
            }
        }
        return isEnclosureConditionMet(true, blockedSides);
    }

    private static boolean isSolidEnclosureBlock(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return !state.isAir() && !state.getCollisionShape(level, pos).isEmpty();
    }

    public static Vec3 computeKnockbackImpulse(Vec3 attackerPos, Vec3 victimPos, double multiplier) {
        if (attackerPos == null || victimPos == null) {
            return Vec3.ZERO;
        }
        double mult = Math.max(1.0D, multiplier);
        Vec3 diff = new Vec3(victimPos.x - attackerPos.x, 0.0D, victimPos.z - attackerPos.z);
        double len = diff.length();
        Vec3 dir = len > 1.0E-4D ? diff.scale(1.0D / len) : new Vec3(1.0D, 0.0D, 0.0D);
        double horizStrength = 0.55D * mult;
        double vertStrength = Math.min(0.85D, 0.25D * mult);
        return new Vec3(dir.x * horizStrength, vertStrength, dir.z * horizStrength);
    }

    @Override
    public void onFightStart(FightState state, ServerLevel level, EnderDragon dragon) {
        clearLocalTracking();
        if (state == null || level == null) {
            return;
        }
        DragonConfig.MinionConfig cfg = DragonConfig.get().minions;
        pullGlidingPlayersToSafeIsland(level, cfg.spawnPullRadius);
        spawnAltarSentinelVexes(state, level, cfg.altarAntiFlightVexCount);
    }

    private void pullGlidingPlayersToSafeIsland(ServerLevel level, double pullRadius) {
        double maxDistSq = pullRadius * pullRadius;
        Vec3 center = new Vec3(0.5D, 80.0D, 0.5D);
        int safeY = Math.max(60, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, 0, 4));
        for (ServerPlayer player : level.players()) {
            if (player.isSpectator() || player.isCreative() || !player.isAlive()) {
                continue;
            }
            if (player.isFallFlying() && player.position().distanceToSqr(center) <= maxDistSq) {
                player.stopFallFlying();
                player.fallDistance = 0.0F;
                player.setDeltaMovement(Vec3.ZERO);
                player.teleportTo(level, 0.5D, safeY + 0.5D, 4.5D, Set.of(), player.getYRot(), player.getXRot(), true);
                player.sendSystemMessage(Component.literal(SPAWN_PULL_MSG));
            }
        }
    }

    private void spawnAltarSentinelVexes(FightState state, ServerLevel level, int count) {
        int baseY = Math.max(62, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, 0, 0)) + 5;
        for (int i = 0; i < count; i++) {
            Vex vex = EntityTypes.VEX.create(level, EntitySpawnReason.EVENT);
            if (vex == null) {
                continue;
            }
            double angle = (2.0D * Math.PI * i) / Math.max(1, count);
            double x = 0.5D + Math.cos(angle) * 4.5D;
            double z = 0.5D + Math.sin(angle) * 4.5D;
            vex.snapTo(x, baseY, z, 0.0F, 0.0F);
            vex.setCustomName(Component.literal("\u00a7d\u00a7lAltar Sentinel"));
            vex.setCustomNameVisible(true);
            vex.setPersistenceRequired();
            vex.setBoundOrigin(new BlockPos(0, baseY, 0));
            if (level.addFreshEntity(vex)) {
                altarSentinelVexes.add(vex.getUUID());
                state.trackMinion(vex.getUUID());
            }
        }
    }

    @Override
    public void onCrystalSpawned(FightState state, ServerLevel level, EndCrystal crystal) {
        if (state == null || level == null || crystal == null) {
            return;
        }
        DragonConfig.MinionConfig cfg = DragonConfig.get().minions;
        VariantBehavior behavior = DragonFightManager.get(level.getServer()).getActiveBehavior();
        int count = behavior != null
                ? behavior.getShadowPhantomsPerCrystal(cfg)
                : (state.getVariant() == DragonVariant.REVENANT ? cfg.revenantShadowPhantomsPerCrystal : cfg.shadowPhantomsPerCrystal);

        for (int i = 0; i < count; i++) {
            Phantom phantom = EntityTypes.PHANTOM.create(level, EntitySpawnReason.EVENT);
            if (phantom == null) {
                continue;
            }
            double angle = (2.0D * Math.PI * i) / Math.max(1, count);
            double px = crystal.getX() + Math.cos(angle) * 3.0D;
            double py = crystal.getY() + 3.0D + (i % 2);
            double pz = crystal.getZ() + Math.sin(angle) * 3.0D;
            phantom.snapTo(px, py, pz, 0.0F, 0.0F);
            phantom.setCustomName(Component.literal("\u00a78\u00a7lShadow Phantom"));
            phantom.setCustomNameVisible(true);
            phantom.setPersistenceRequired();
            AttributeInstance dmgAttr = phantom.getAttribute(Attributes.ATTACK_DAMAGE);
            if (dmgAttr != null) {
                dmgAttr.setBaseValue(cfg.shadowPhantomDamage);
            }
            AttributeInstance kbRes = phantom.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
            if (kbRes != null) {
                kbRes.setBaseValue(1.0D);
            }
            if (level.addFreshEntity(phantom)) {
                shadowPhantomToCrystal.put(phantom.getUUID(), crystal.getUUID());
                state.trackMinion(phantom.getUUID());
            }
        }
    }

    @Override
    public void onCrystalDestroyed(FightState state, ServerLevel level, UUID anchorUuid, BlockPos crystalPos, DamageSource source) {
        // Shadow Phantoms intentionally do NOT despawn when their End Crystal is destroyed;
        // they remain active for the entire Dragon fight and only despawn when the fight ends.
    }

    @Override
    public void onHealthThreshold(FightState state, ServerLevel level, EnderDragon dragon, double prevRatio, double newRatio) {
        if (state == null) {
            return;
        }
        VariantBehavior behavior = level != null
                ? DragonFightManager.get(level.getServer()).getActiveBehavior()
                : VariantBehavior.create(state.getVariant());
        double[] thresholds = behavior != null ? behavior.getEndermanCallThresholds() : new double[]{0.75D, 0.50D, 0.25D};
        for (double threshold : thresholds) {
            int pctKey = (int) Math.round(threshold * 100.0D);
            if (prevRatio > threshold && newRatio <= threshold && triggeredThresholdPercents.add(pctKey)) {
                if (level != null) {
                    callIslandEndermenForHelp(state, level, behavior);
                }
            }
        }
    }

    public Set<Integer> getTriggeredThresholdPercents() {
        return Collections.unmodifiableSet(triggeredThresholdPercents);
    }

    private void callIslandEndermenForHelp(FightState state, ServerLevel level, VariantBehavior behavior) {
        double radius = DragonConfig.get().minions.endermanCallRadius;
        AABB box = new AABB(-radius, 0.0D, -radius, radius, 256.0D, radius);
        List<Enderman> endermen = level.getEntitiesOfClass(Enderman.class, box);
        List<ServerPlayer> validTargets = new ArrayList<>();
        for (ServerPlayer p : level.players()) {
            if (!p.isSpectator() && !p.isCreative() && p.isAlive()
                    && (p.getX() * p.getX() + p.getZ() * p.getZ()) <= radius * radius) {
                validTargets.add(p);
            }
        }
        if (validTargets.isEmpty()) {
            return;
        }
        int idx = 0;
        for (Enderman enderman : endermen) {
            if (!enderman.isAlive()) {
                continue;
            }
            ServerPlayer target = validTargets.get(idx % validTargets.size());
            idx++;
            enderman.setTarget(target);
            if (behavior != null) {
                behavior.applyCalledEndermanBuff(enderman);
            }
        }
        level.playSound(null, BlockPos.ZERO, SoundEvents.ENDERMAN_SCREAM, SoundSource.HOSTILE, 3.0F, 0.8F);
    }

    @Override
    public void onTick(FightState state, ServerLevel level, EnderDragon dragon) {
        if (state == null || level == null) {
            return;
        }
        DragonConfig.MinionConfig cfg = DragonConfig.get().minions;
        long tick = state.getTickCount();

        enforceNoElytraZoneAndTrackAirborne(state, level, cfg);
        tickAltarSentinelVexes(level, tick, cfg.altarAntiFlightAirborneTicks);

        if (tick % cfg.antiTunnelCheckIntervalTicks == 0L) {
            checkAntiTunnelPlayers(state, level);
        }
    }

    private void enforceNoElytraZoneAndTrackAirborne(FightState state, ServerLevel level, DragonConfig.MinionConfig cfg) {
        double noElytraSq = cfg.noElytraZoneRadius * cfg.noElytraZoneRadius;
        for (ServerPlayer player : level.players()) {
            if (player.isSpectator() || player.isCreative() || !player.isAlive()) {
                playerAirborneTicks.remove(player.getUUID());
                continue;
            }
            double horizDistSq = player.getX() * player.getX() + player.getZ() * player.getZ();
            if (horizDistSq <= noElytraSq && player.isFallFlying()) {
                player.stopFallFlying();
                sendThrottledActionBar(player, lastElytraMsgMs, NO_ELYTRA_MSG);
            }
            boolean airborne = horizDistSq <= noElytraSq
                    && !player.onGround()
                    && !player.isInWater()
                    && !player.isPassenger();
            recordAirborneState(player.getUUID(), airborne);
        }
    }

    private void tickAltarSentinelVexes(ServerLevel level, long tick, int airborneThresholdTicks) {
        ServerPlayer airborneTarget = null;
        for (ServerPlayer player : level.players()) {
            if (!player.isSpectator() && !player.isCreative() && player.isAlive()
                    && getAirborneTicks(player.getUUID()) > airborneThresholdTicks) {
                airborneTarget = player;
                break;
            }
        }

        int baseY = Math.max(62, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, 0, 0)) + 5;
        int idx = 0;
        int total = Math.max(1, altarSentinelVexes.size());
        for (UUID vexId : altarSentinelVexes) {
            Entity entity = level.getEntity(vexId);
            if (!(entity instanceof Vex vex) || vex.isRemoved() || !vex.isAlive()) {
                continue;
            }
            if (airborneTarget != null) {
                vex.setTarget(airborneTarget);
            } else {
                vex.setTarget(null);
                double angle = ((2.0D * Math.PI * idx) / total) + (tick * 0.05D);
                double ox = 0.5D + Math.cos(angle) * 4.5D;
                double oz = 0.5D + Math.sin(angle) * 4.5D;
                Vec3 desired = new Vec3(ox, baseY, oz);
                Vec3 step = desired.subtract(vex.position()).scale(0.25D);
                vex.setDeltaMovement(step);
            }
            idx++;
        }
    }

    private void checkAntiTunnelPlayers(FightState state, ServerLevel level) {
        double maxDistSq = 160.0D * 160.0D;
        for (ServerPlayer player : level.players()) {
            if (player.isSpectator() || player.isCreative() || !player.isAlive()) {
                continue;
            }
            double horizDistSq = player.getX() * player.getX() + player.getZ() * player.getZ();
            if (horizDistSq > maxDistSq) {
                continue;
            }
            if (isPlayerEnclosedInBlocks(level, player.blockPosition())) {
                for (int i = 0; i < 2; i++) {
                    Vex vex = EntityTypes.VEX.create(level, EntitySpawnReason.EVENT);
                    if (vex == null) {
                        continue;
                    }
                    vex.snapTo(player.getX() + (i == 0 ? 0.5D : -0.5D), player.getY() + 0.5D, player.getZ(), 0.0F, 0.0F);
                    vex.setCustomName(Component.literal("\u00a7c\u00a7lVoid Tunnel Vex"));
                    vex.setCustomNameVisible(true);
                    vex.setLimitedLife(400);
                    vex.setTarget(player);
                    if (level.addFreshEntity(vex)) {
                        tunnelVexes.add(vex.getUUID());
                        state.trackMinion(vex.getUUID());
                    }
                }
            }
        }
    }

    @Override
    public boolean onDragonDamageEntity(FightState state, ServerLevel level, Entity target, DamageSource source, float amount) {
        if (target instanceof Enderman) {
            return true;
        }
        return target != null && isInvulnerableMinion(target.getUUID());
    }

    @Override
    public boolean isPlayerFlightRestricted(FightState state, ServerPlayer player) {
        if (state == null || state.isEnded() || player == null || player.isSpectator() || player.isCreative()) {
            return false;
        }
        double r = DragonConfig.get().minions.noElytraZoneRadius;
        return (player.getX() * player.getX() + player.getZ() * player.getZ()) <= r * r;
    }

    public InteractionResult handleAttackEntity(Player player, Level world, Entity target) {
        if (world.isClientSide() || target == null) {
            return InteractionResult.PASS;
        }
        if (isShadowPhantom(target.getUUID())) {
            if (player instanceof ServerPlayer serverPlayer) {
                sendThrottledActionBar(serverPlayer, lastPhantomMsgMs, SHADOW_PHANTOM_IMMUNE_MSG);
            }
            return InteractionResult.FAIL;
        }
        if (altarSentinelVexes.contains(target.getUUID())) {
            return InteractionResult.FAIL;
        }
        return InteractionResult.PASS;
    }

    public boolean allowEntityDamage(LivingEntity entity, DamageSource source, float amount) {
        if (entity == null || !(entity.level() instanceof ServerLevel serverLevel)) {
            return true;
        }
        // 1. Invulnerable Shadow Phantoms & Altar Sentinel Vexes
        if (isShadowPhantom(entity.getUUID())) {
            if (source != null && source.getEntity() instanceof ServerPlayer player) {
                sendThrottledActionBar(player, lastPhantomMsgMs, SHADOW_PHANTOM_IMMUNE_MSG);
            }
            return false;
        }
        if (altarSentinelVexes.contains(entity.getUUID())) {
            return false;
        }

        FightState active = DragonFightManager.get(serverLevel.getServer()).getActiveFight();
        if (active == null || active.isEnded()) {
            return true;
        }

        // 2. Endermen on the main island are immune to all Ender Dragon damage
        if (entity instanceof Enderman) {
            if (isDamageFromDragon(source, active.getDragonUuid())) {
                return false;
            }
        }

        // 3. Shadow Phantom 3x knockback when hitting a player
        if (entity instanceof ServerPlayer victim && source != null && source.getEntity() != null) {
            Entity attacker = source.getEntity();
            if (isShadowPhantom(attacker.getUUID())) {
                Vec3 impulse = computeKnockbackImpulse(
                        attacker.position(),
                        victim.position(),
                        DragonConfig.get().minions.shadowPhantomKnockbackMultiplier
                );
                victim.setDeltaMovement(victim.getDeltaMovement().add(impulse));
                victim.needsSync = true;
            }
        }
        return true;
    }

    public static boolean isDamageFromDragon(DamageSource source, UUID dragonUuid) {
        if (source == null || dragonUuid == null) {
            return false;
        }
        Entity attacker = source.getEntity();
        if (attacker != null && (attacker instanceof EnderDragon || dragonUuid.equals(attacker.getUUID()))) {
            return true;
        }
        Entity direct = source.getDirectEntity();
        if (direct instanceof EnderDragon || (direct != null && dragonUuid.equals(direct.getUUID()))) {
            return true;
        }
        if (direct instanceof DragonFireball fb && fb.getOwner() != null && dragonUuid.equals(fb.getOwner().getUUID())) {
            return true;
        }
        if (direct instanceof AreaEffectCloud aec && aec.getOwner() != null && dragonUuid.equals(aec.getOwner().getUUID())) {
            return true;
        }
        String msgId = source.getMsgId();
        return "dragonBreath".equalsIgnoreCase(msgId);
    }

    @Override
    public void onFightEnded(FightState state, ServerLevel level, boolean dragonKilled) {
        if (level != null) {
            for (UUID id : shadowPhantomToCrystal.keySet()) {
                Entity e = level.getEntity(id);
                if (e != null && !e.isRemoved()) {
                    e.discard();
                }
            }
            for (UUID id : altarSentinelVexes) {
                Entity e = level.getEntity(id);
                if (e != null && !e.isRemoved()) {
                    e.discard();
                }
            }
            for (UUID id : tunnelVexes) {
                Entity e = level.getEntity(id);
                if (e != null && !e.isRemoved()) {
                    e.discard();
                }
            }
        }
        clearLocalTracking();
    }

    public void clearLocalTracking() {
        shadowPhantomToCrystal.clear();
        altarSentinelVexes.clear();
        tunnelVexes.clear();
        playerAirborneTicks.clear();
        lastPhantomMsgMs.clear();
        lastElytraMsgMs.clear();
        triggeredThresholdPercents.clear();
    }

    private static void sendThrottledActionBar(ServerPlayer player, Map<UUID, Long> cooldownMap, String msg) {
        long now = System.currentTimeMillis();
        Long last = cooldownMap.get(player.getUUID());
        if (last == null || (now - last) >= MESSAGE_COOLDOWN_MS) {
            cooldownMap.put(player.getUUID(), now);
            player.sendSystemMessage(Component.literal(msg), true);
        }
    }
}
