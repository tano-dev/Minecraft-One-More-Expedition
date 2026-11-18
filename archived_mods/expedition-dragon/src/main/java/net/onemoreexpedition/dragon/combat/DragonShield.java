package net.onemoreexpedition.dragon.combat;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.projectile.Projectile;
import net.onemoreexpedition.dragon.core.DragonFightManager;
import net.onemoreexpedition.dragon.core.FightState;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class DragonShield {
    public static final String SHIELD_ACTIONBAR_MESSAGE = "§cDestroy all End Crystals first!";
    public static final long ACTIONBAR_COOLDOWN_MS = 1000L;
    private static final ConcurrentHashMap<UUID, Long> ACTIONBAR_COOLDOWNS = new ConcurrentHashMap<>();

    private DragonShield() {
    }

    public static ServerPlayer resolvePlayerAttacker(DamageSource source) {
        if (source == null) {
            return null;
        }
        if (source.getEntity() instanceof ServerPlayer sp) {
            return sp;
        }
        if (source.getDirectEntity() instanceof ServerPlayer sp) {
            return sp;
        }
        if (source.getDirectEntity() instanceof Projectile projectile && projectile.getOwner() instanceof ServerPlayer sp) {
            return sp;
        }
        return null;
    }

    public static boolean isShieldActive(
            boolean fightActive,
            boolean dragonMatches,
            boolean hasActiveAnchors,
            boolean bypassesInvulnerability,
            boolean dragonAlive
    ) {
        return fightActive
                && dragonMatches
                && hasActiveAnchors
                && !bypassesInvulnerability
                && dragonAlive;
    }

    public static boolean tryConsumeActionBarCooldown(
            Map<UUID, Long> cooldownMap,
            UUID playerUuid,
            long nowMs,
            long cooldownMs
    ) {
        if (cooldownMap == null || playerUuid == null) {
            return false;
        }
        Long lastMs = cooldownMap.get(playerUuid);
        if (lastMs != null && cooldownMs > 0L && nowMs >= lastMs) {
            long elapsed = nowMs - lastMs;
            if (elapsed >= 0L && elapsed < cooldownMs) {
                return false;
            }
        }
        cooldownMap.put(playerUuid, nowMs);
        return true;
    }

    public static float computePostReductionDamage(float oldHealth, float newHealth) {
        if (!Float.isFinite(oldHealth) || !Float.isFinite(newHealth)) {
            return 0.0F;
        }
        float clampedOld = Math.max(0.0F, oldHealth);
        float clampedNew = Math.max(0.0F, newHealth);
        float dealt = clampedOld - clampedNew;
        return dealt > 0.0F ? dealt : 0.0F;
    }

    public static boolean shouldCancelDamage(ServerLevel level, EnderDragon dragon, DamageSource source) {
        if (level == null || dragon == null || source == null) {
            return false;
        }
        boolean bypasses = source.is(DamageTypeTags.BYPASSES_INVULNERABILITY);
        DragonFightManager manager = DragonFightManager.get(level.getServer());
        FightState state = manager.getActiveFight();
        boolean fightActive = state != null && !state.isEnded();
        boolean dragonMatches = fightActive && dragon.getUUID().equals(state.getDragonUuid());
        boolean hasAnchors = fightActive && state.hasActiveAnchors();
        
        // If perching, the shield is weakened
        if (DragonFightManager.get(level.getServer()).getMovesetController().isPerching()) {
            hasAnchors = false; 
        }

        if (!isShieldActive(fightActive, dragonMatches, hasAnchors, bypasses, dragon.isAlive())) {
            return false;
        }

        if (!(source.getDirectEntity() instanceof EndCrystal)) {
            ServerPlayer attacker = resolvePlayerAttacker(source);
            if (attacker != null && tryConsumeActionBarCooldown(
                    ACTIONBAR_COOLDOWNS,
                    attacker.getUUID(),
                    System.currentTimeMillis(),
                    ACTIONBAR_COOLDOWN_MS
            )) {
                attacker.sendOverlayMessage(Component.literal(SHIELD_ACTIONBAR_MESSAGE));
            }
        }
        return true;
    }

    public static void recordPostReductionDamage(ServerLevel level, EnderDragon dragon, DamageSource source, float oldHealth) {
        if (level == null || dragon == null || source == null) {
            return;
        }
        float dealt = computePostReductionDamage(oldHealth, dragon.getHealth());
        if (dealt <= 0.0F) {
            return;
        }
        ServerPlayer attacker = resolvePlayerAttacker(source);
        if (attacker != null) {
            DragonFightManager manager = DragonFightManager.get(level.getServer());
            if (manager.isCustomDragon(dragon)) {
                manager.recordDamage(attacker.getUUID(), dealt);
            }
        }
    }

    public static void clearCooldowns() {
        ACTIONBAR_COOLDOWNS.clear();
    }
}
