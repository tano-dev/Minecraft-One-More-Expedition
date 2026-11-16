package com.expedition.fishing.mechanics;

import com.expedition.fishing.config.FishingDataRegistry;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.EvokerFangs;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class BossAbilityEngine {

    public static class ActiveCreatureController {
        public final UUID entityUuid;
        public final UUID ownerUuid;
        public final net.minecraft.resources.ResourceKey<Level> dimension;
        public final String mobId;
        public final String displayName;
        public final String rarity;
        public final JsonObject spawnCfg;
        public final ServerBossEvent bossBar;
        public final Map<String, Integer> abilityCooldowns = new HashMap<>();
        public int aggroHitCooldown = 0;

        public ActiveCreatureController(
                UUID entityUuid,
                UUID ownerUuid,
                net.minecraft.resources.ResourceKey<Level> dimension,
                String mobId,
                String displayName,
                String rarity,
                JsonObject spawnCfg,
                ServerBossEvent bossBar
        ) {
            this.entityUuid = entityUuid;
            this.ownerUuid = ownerUuid;
            this.dimension = dimension;
            this.mobId = mobId;
            this.displayName = displayName;
            this.rarity = rarity;
            this.spawnCfg = spawnCfg;
            this.bossBar = bossBar;

            if (spawnCfg != null && spawnCfg.has("abilities") && spawnCfg.get("abilities").isJsonArray()) {
                for (JsonElement el : spawnCfg.getAsJsonArray("abilities")) {
                    if (el.isJsonObject()) {
                        JsonObject ab = el.getAsJsonObject();
                        String id = FishingDataRegistry.getStr(ab, "id", "");
                        int cd = FishingDataRegistry.getInt(ab, "cooldown_ticks", 100);
                        if (!id.isEmpty()) {
                            this.abilityCooldowns.put(id, Math.max(20, cd / 2));
                        }
                    }
                }
            }
        }
    }

    private static final Map<UUID, ActiveCreatureController> CONTROLLERS = new ConcurrentHashMap<>();

    public static void registerController(
            ServerPlayer owner,
            ServerLevel level,
            LivingEntity entity,
            String mobId,
            String displayName,
            String rarity,
            JsonObject spawnCfg,
            boolean createBossBar
    ) {
        ServerBossEvent bar = null;
        if (createBossBar && ("RARE".equalsIgnoreCase(rarity) || "EPIC".equalsIgnoreCase(rarity) || "LEGENDARY".equalsIgnoreCase(rarity))) {
            BossEvent.BossBarColor barColor = switch (rarity.toUpperCase(Locale.ROOT)) {
                case "LEGENDARY" -> BossEvent.BossBarColor.RED;
                case "EPIC" -> BossEvent.BossBarColor.PURPLE;
                default -> BossEvent.BossBarColor.BLUE;
            };
            bar = new ServerBossEvent(
                    UUID.randomUUID(),
                    Component.literal(com.expedition.fishing.item.FishingItemFactory.getRarityColor(rarity) + displayName +
                            " §7(" + Math.round(entity.getHealth()) + "/" + Math.round(entity.getMaxHealth()) + "❤)"),
                    barColor,
                    BossEvent.BossBarOverlay.NOTCHED_10
            );
            bar.setProgress(1.0f);
            bar.addPlayer(owner);
        }

        CONTROLLERS.put(entity.getUUID(), new ActiveCreatureController(
                entity.getUUID(), owner.getUUID(), level.dimension(), mobId, displayName, rarity, spawnCfg, bar
        ));
    }

    public static void removeController(UUID entityUuid) {
        ActiveCreatureController ctrl = CONTROLLERS.remove(entityUuid);
        if (ctrl != null && ctrl.bossBar != null) {
            ctrl.bossBar.removeAllPlayers();
        }
    }

    public static void tickAll(MinecraftServer server) {
        if (CONTROLLERS.isEmpty()) return;

        Iterator<Map.Entry<UUID, ActiveCreatureController>> it = CONTROLLERS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, ActiveCreatureController> entry = it.next();
            ActiveCreatureController ctrl = entry.getValue();
            ServerLevel level = server.getLevel(ctrl.dimension);
            if (level == null) {
                if (ctrl.bossBar != null) ctrl.bossBar.removeAllPlayers();
                it.remove();
                continue;
            }

            Entity rawEnt = level.getEntity(ctrl.entityUuid);
            if (!(rawEnt instanceof LivingEntity boss) || !boss.isAlive()) {
                if (ctrl.bossBar != null) ctrl.bossBar.removeAllPlayers();
                it.remove();
                continue;
            }

            // 1. Update BossBar every 4 ticks
            if (ctrl.bossBar != null && level.getGameTime() % 4 == 0) {
                float pct = Math.max(0.0f, Math.min(1.0f, boss.getHealth() / Math.max(1.0f, boss.getMaxHealth())));
                ctrl.bossBar.setProgress(pct);
                ctrl.bossBar.setName(Component.literal(
                        com.expedition.fishing.item.FishingItemFactory.getRarityColor(ctrl.rarity) + ctrl.displayName +
                        " §7(" + Math.round(boss.getHealth()) + "/" + Math.round(boss.getMaxHealth()) + "❤)"
                ));

                for (ServerPlayer sp : level.players()) {
                    if (sp.distanceToSqr(boss) <= 36.0 * 36.0) {
                        ctrl.bossBar.addPlayer(sp);
                    } else {
                        ctrl.bossBar.removePlayer(sp);
                    }
                }
            }

            // 2. Ambient Boss Particles
            if (ctrl.spawnCfg != null && ctrl.spawnCfg.has("particle") && level.getGameTime() % 6 == 0) {
                String pName = FishingDataRegistry.getStr(ctrl.spawnCfg, "particle", "");
                ParticleOptions pType = switch (pName) {
                    case "gore", "crimson_ghast" -> ParticleTypes.CRIMSON_SPORE;
                    case "reverse_portal", "void_colossus" -> ParticleTypes.REVERSE_PORTAL;
                    case "portal" -> ParticleTypes.PORTAL;
                    case "potion" -> ParticleTypes.WITCH;
                    default -> ParticleTypes.SPLASH;
                };
                level.sendParticles(pType, boss.getX(), boss.getY() + boss.getBbHeight() * 0.5, boss.getZ(), 4, 0.35, 0.35, 0.35, 0.02);
            }

            // 3. Find Target Player
            ServerPlayer target = findTarget(level, boss, ctrl.ownerUuid);
            if (target == null) continue;

            if (boss instanceof Mob mob && mob.getTarget() != target) {
                mob.setTarget(target);
            }

            // 4. Custom Aggro AI (for Dolphins, Squids, Fish, Striders, Mounts, or mobs with 'aggro' block)
            if (ctrl.spawnCfg != null && ctrl.spawnCfg.has("aggro") && ctrl.spawnCfg.get("aggro").isJsonObject()) {
                tickCustomAggro(level, boss, target, ctrl, ctrl.spawnCfg.getAsJsonObject("aggro"));
            }

            // 5. Boss Abilities Execution
            if (ctrl.spawnCfg != null && ctrl.spawnCfg.has("abilities") && ctrl.spawnCfg.get("abilities").isJsonArray()) {
                tickAbilities(level, boss, target, ctrl, ctrl.spawnCfg.getAsJsonArray("abilities"));
            }
        }
    }

    private static ServerPlayer findTarget(ServerLevel level, LivingEntity boss, UUID ownerUuid) {
        Entity owner = level.getEntity(ownerUuid);
        if (owner instanceof ServerPlayer sp && sp.isAlive() && !sp.isSpectator() && sp.distanceToSqr(boss) <= 32.0 * 32.0) {
            return sp;
        }
        ServerPlayer nearest = null;
        double bestDist = 28.0 * 28.0;
        for (ServerPlayer p : level.players()) {
            if (!p.isAlive() || p.isSpectator() || p.isCreative()) continue;
            double d = p.distanceToSqr(boss);
            if (d < bestDist) {
                bestDist = d;
                nearest = p;
            }
        }
        return nearest;
    }

    private static void tickCustomAggro(ServerLevel level, LivingEntity boss, ServerPlayer target, ActiveCreatureController ctrl, JsonObject aggro) {
        double speed = FishingDataRegistry.getDouble(aggro, "speed", 0.8);
        double rawDamage = FishingDataRegistry.getDouble(aggro, "damage", 10.0) * FishingDataRegistry.getMobBalance("global_damage_scale", 1.0);
        double damage = Math.min(FishingDataRegistry.getMobBalance("max_boss_damage", 50.0), rawDamage);
        double range = FishingDataRegistry.getDouble(aggro, "range", 3.5);
        int hitInterval = Math.max(10, FishingDataRegistry.getInt(aggro, "hit_interval", 30));

        // Steer boss (or its vehicle if mounted) toward target every 3 ticks
        if (level.getGameTime() % 3 == 0) {
            Vec3 toTarget = target.position().add(0, 0.5, 0).subtract(boss.position());
            double dist = toTarget.length();
            if (dist > 0.5) {
                Vec3 dir = toTarget.normalize();
                double step = Math.min(0.42, speed * 0.22);
                Vec3 cur = boss.getDeltaMovement();
                double vy = cur.y;
                if (boss.isInWater() || boss.isInLava()) {
                    vy = dir.y * step;
                } else if (boss.onGround() && (dist < 8.0 || target.getY() > boss.getY() + 0.8)) {
                    vy = 0.34; // Lunge hop on land
                }
                boss.setDeltaMovement(cur.x * 0.5 + dir.x * step, vy, cur.z * 0.5 + dir.z * step);
            }
        }

        if (ctrl.aggroHitCooldown > 0) {
            ctrl.aggroHitCooldown--;
        } else if (boss.distanceTo(target) <= range) {
            ctrl.aggroHitCooldown = hitInterval;
            target.hurtServer(level, level.damageSources().mobAttack(boss), (float) damage);
            level.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY() + 1.0, target.getZ(), 8, 0.3, 0.3, 0.3, 0.1);
            level.playSound(null, boss.blockPosition(), SoundEvents.PHANTOM_BITE, SoundSource.HOSTILE, 0.9f, 0.9f);
        }
    }

    private static void tickAbilities(ServerLevel level, LivingEntity boss, ServerPlayer target, ActiveCreatureController ctrl, JsonArray abilities) {
        List<JsonObject> ready = new ArrayList<>();
        int totalWeight = 0;

        for (JsonElement el : abilities) {
            if (!el.isJsonObject()) continue;
            JsonObject ab = el.getAsJsonObject();
            String id = FishingDataRegistry.getStr(ab, "id", "");
            if (id.isEmpty()) continue;

            int rem = ctrl.abilityCooldowns.getOrDefault(id, 60) - 1;
            if (rem <= 0) {
                ctrl.abilityCooldowns.put(id, 0);
                double chance = FishingDataRegistry.getDouble(ab, "chance", 1.0);
                if (level.getRandom().nextDouble() <= chance) {
                    int w = Math.max(1, FishingDataRegistry.getInt(ab, "weight", 1));
                    totalWeight += w;
                    ready.add(ab);
                }
            } else {
                ctrl.abilityCooldowns.put(id, rem);
            }
        }

        if (ready.isEmpty() || boss.distanceToSqr(target) > 24.0 * 24.0) return;

        int roll = level.getRandom().nextInt(totalWeight);
        int acc = 0;
        JsonObject chosen = ready.get(0);
        for (JsonObject ab : ready) {
            acc += Math.max(1, FishingDataRegistry.getInt(ab, "weight", 1));
            if (roll < acc) {
                chosen = ab;
                break;
            }
        }

        String abId = FishingDataRegistry.getStr(chosen, "id", "");
        int cd = Math.max(40, FishingDataRegistry.getInt(chosen, "cooldown_ticks", 120));
        ctrl.abilityCooldowns.put(abId, cd);

        executeAbility(level, boss, target, ctrl.displayName, ctrl.rarity, abId);
    }

    private static float scaleAbDmg(float base) {
        double scaled = base * FishingDataRegistry.getMobBalance("ability_damage_scale", 1.0);
        return (float) Math.min(FishingDataRegistry.getMobBalance("max_boss_damage", 50.0), scaled);
    }

    private static void executeAbility(ServerLevel level, LivingEntity boss, ServerPlayer target, String bossName, String rarity, String abId) {
        Vec3 bPos = boss.position();
        Vec3 tPos = target.position();

        switch (abId) {
            case "POSEIDON_WAVE", "POSEIDON_STORM_WAVE" -> {
                level.sendParticles(ParticleTypes.SPLASH, bPos.x, bPos.y + 1.0, bPos.z, 60, 2.5, 0.6, 2.5, 0.3);
                level.sendParticles(ParticleTypes.BUBBLE_POP, tPos.x, tPos.y + 1.0, tPos.z, 35, 1.2, 0.5, 1.2, 0.2);
                level.playSound(null, boss.blockPosition(), SoundEvents.TRIDENT_THUNDER.value(), SoundSource.HOSTILE, 1.0f, 1.0f);
                Vec3 push = tPos.subtract(bPos).normalize().scale(1.35);
                target.setDeltaMovement(push.x, 0.55, push.z);
                target.connection.send(new ClientboundSetEntityMotionPacket(target));
                target.hurtServer(level, level.damageSources().mobAttack(boss), scaleAbDmg("POSEIDON_STORM_WAVE".equals(abId) ? 16.0f : 12.0f));
                if ("POSEIDON_STORM_WAVE".equals(abId)) {
                    EntityType<?> boltType = BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.withDefaultNamespace("lightning_bolt"));
                    Entity bolt = boltType != null ? boltType.create(level, EntitySpawnReason.EVENT) : null;
                    if (bolt != null) {
                        bolt.setPos(tPos.x, tPos.y, tPos.z);
                        level.addFreshEntity(bolt);
                    }
                }
            }
            case "TIDAL_PULL", "BLOOD_TONGUE" -> {
                level.sendParticles("BLOOD_TONGUE".equals(abId) ? ParticleTypes.CRIMSON_SPORE : ParticleTypes.NAUTILUS,
                        tPos.x, tPos.y + 1.0, tPos.z, 35, 0.8, 0.8, 0.8, 0.15);
                level.playSound(null, boss.blockPosition(), SoundEvents.TRIDENT_RIPTIDE_3.value(), SoundSource.HOSTILE, 1.0f, 0.8f);
                Vec3 pull = bPos.subtract(tPos).normalize().scale(1.25);
                target.setDeltaMovement(pull.x, 0.38, pull.z);
                target.connection.send(new ClientboundSetEntityMotionPacket(target));
                target.hurtServer(level, level.damageSources().mobAttack(boss), scaleAbDmg(11.0f));
            }
            case "GROUND_SLAM", "KING_SLIME_SPLASH", "BRUTE_TUSK_SWEEP" -> {
                level.sendParticles("KING_SLIME_SPLASH".equals(abId) ? ParticleTypes.ITEM_SLIME : ParticleTypes.EXPLOSION,
                        bPos.x, bPos.y + 0.3, bPos.z, 12, 2.0, 0.2, 2.0, 0.05);
                level.playSound(null, boss.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 0.9f, 0.9f);
                if (boss.distanceTo(target) <= 8.0) {
                    target.setDeltaMovement(target.getDeltaMovement().x, 0.88, target.getDeltaMovement().z);
                    target.connection.send(new ClientboundSetEntityMotionPacket(target));
                    target.hurtServer(level, level.damageSources().mobAttack(boss), scaleAbDmg(13.0f));
                    target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 80, 1));
                }
            }
            case "INK_CLOUD" -> {
                level.sendParticles(ParticleTypes.SQUID_INK, tPos.x, tPos.y + 1.0, tPos.z, 45, 1.5, 1.0, 1.5, 0.08);
                level.playSound(null, target.blockPosition(), SoundEvents.SQUID_SQUIRT, SoundSource.HOSTILE, 1.0f, 0.7f);
                target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 80, 0));
                target.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 100, 0));
                target.addEffect(new MobEffectInstance(MobEffects.POISON, 80, 1));
            }
            case "DUNE_BEAM", "FROST_BEAM", "MAGMA_EYE_LASERS", "WAILING_TWIN_LASER" -> {
                ParticleOptions beamParticle = switch (abId) {
                    case "FROST_BEAM" -> ParticleTypes.SNOWFLAKE;
                    case "DUNE_BEAM" -> ParticleTypes.CRIT;
                    default -> ParticleTypes.FLAME;
                };
                Vec3 start = bPos.add(0, boss.getEyeHeight(), 0);
                Vec3 end = tPos.add(0, 1.0, 0);
                Vec3 diff = end.subtract(start);
                int steps = 16;
                for (int i = 0; i <= steps; i++) {
                    Vec3 p = start.add(diff.scale((double) i / steps));
                    level.sendParticles(beamParticle, p.x, p.y, p.z, 2, 0.05, 0.05, 0.05, 0.01);
                }
                level.playSound(null, boss.blockPosition(), SoundEvents.GUARDIAN_ATTACK, SoundSource.HOSTILE, 1.0f, 1.2f);
                target.hurtServer(level, level.damageSources().indirectMagic(boss, boss), scaleAbDmg(13.0f));
                if ("FROST_BEAM".equals(abId)) {
                    target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 100, 2));
                } else {
                    target.igniteForSeconds(4.0f);
                }
            }
            case "FIRE_GEYSERS", "SOUL_GEYSERS", "LAVA_SURGE", "SOUL_RUPTURE", "NETHER_REND" -> {
                boolean soul = abId.contains("SOUL");
                level.sendParticles(soul ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.LAVA,
                        tPos.x, tPos.y + 0.2, tPos.z, 35, 0.9, 1.2, 0.9, 0.15);
                level.playSound(null, target.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.HOSTILE, 1.0f, 0.8f);
                target.setDeltaMovement(target.getDeltaMovement().x, 0.65, target.getDeltaMovement().z);
                target.connection.send(new ClientboundSetEntityMotionPacket(target));
                target.igniteForSeconds(5.0f);
                target.hurtServer(level, level.damageSources().mobAttack(boss), scaleAbDmg(14.0f));
            }
            case "STRIDER_LAVA_SPEW", "EMBER_VOLLEY", "HOGLIN_FLAMETHROWER", "MAGMA_ARM_ROCKETS", "MAGMA_BURST", "VULTURE_MISSILE", "WAILING_TWIN_TOXIC_SPIT" -> {
                level.sendParticles(ParticleTypes.FLAME, bPos.x, bPos.y + 1.2, bPos.z, 30, 0.8, 0.8, 0.8, 0.15);
                level.sendParticles(ParticleTypes.LARGE_SMOKE, tPos.x, tPos.y + 1.0, tPos.z, 25, 0.8, 0.8, 0.8, 0.1);
                level.playSound(null, boss.blockPosition(), SoundEvents.GHAST_SHOOT, SoundSource.HOSTILE, 1.0f, 0.9f);
                target.hurtServer(level, level.damageSources().mobAttack(boss), scaleAbDmg(12.0f));
                if (abId.contains("TOXIC")) {
                    target.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 1));
                    target.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 100, 0));
                } else {
                    target.igniteForSeconds(4.0f);
                }
            }
            case "WARPED_ENDERMAN_GAZE", "WARPED_ENDERMAN_SNARE", "RIFTBOUND_GAZE", "RIFTBOUND_SNARE" -> {
                level.sendParticles(ParticleTypes.PORTAL, bPos.x, bPos.y + 1.0, bPos.z, 40, 0.6, 1.0, 0.6, 0.3);
                Vec3 behind = tPos.subtract(target.getLookAngle().scale(1.8));
                boss.teleportTo(behind.x, tPos.y, behind.z);
                level.sendParticles(ParticleTypes.REVERSE_PORTAL, behind.x, tPos.y + 1.0, behind.z, 40, 0.6, 1.0, 0.6, 0.2);
                level.playSound(null, target.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1.0f, 0.8f);
                target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 70, 3));
                target.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 60, 0));
                target.hurtServer(level, level.damageSources().mobAttack(boss), scaleAbDmg(13.0f));
            }
            case "TEMPLE_RITUAL", "DARK_RITUAL", "VALLEY_DIRGE" -> {
                level.playSound(null, target.blockPosition(), SoundEvents.EVOKER_PREPARE_ATTACK, SoundSource.HOSTILE, 1.0f, 0.9f);
                for (int i = -1; i <= 1; i++) {
                    EvokerFangs fangs = new EvokerFangs(level, tPos.x + i * 1.2, tPos.y, tPos.z + i * 1.2, 0.0f, 5, boss);
                    level.addFreshEntity(fangs);
                }
                target.addEffect(new MobEffectInstance(MobEffects.WITHER, 100, 1));
            }
            case "BLOOD_YOKE", "BLOOD_EXPLOSION", "WAR_DRUM", "HEAT_MARK" -> {
                level.sendParticles(ParticleTypes.DAMAGE_INDICATOR, tPos.x, tPos.y + 1.0, tPos.z, 20, 0.6, 0.6, 0.6, 0.1);
                level.playSound(null, boss.blockPosition(), SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 1.0f, 0.8f);
                target.hurtServer(level, level.damageSources().mobAttack(boss), scaleAbDmg(14.0f));
                boss.heal(25.0f);
                boss.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 120, 0));
                boss.addEffect(new MobEffectInstance(MobEffects.SPEED, 120, 0));
            }
            default -> {
                // TORSO_TENTACLES, ABYSSAL_NOVA, MINI_BLAST, WARDEN_PHASE_TWO
                level.sendParticles(ParticleTypes.SONIC_BOOM, tPos.x, tPos.y + 1.0, tPos.z, 3, 0.5, 0.5, 0.5, 0.01);
                level.sendParticles(ParticleTypes.SCULK_SOUL, bPos.x, bPos.y + 1.0, bPos.z, 35, 1.5, 1.0, 1.5, 0.1);
                level.playSound(null, boss.blockPosition(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 0.9f, 1.0f);
                target.hurtServer(level, level.damageSources().sonicBoom(boss), scaleAbDmg(16.0f));
                target.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 80, 0));
            }
        }
    }
}
