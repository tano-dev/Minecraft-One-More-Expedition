package net.onemoreexpedition.dragon.core;

import net.onemoreexpedition.dragon.variants.DragonVariant;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class FightState {
    private final UUID fightId;
    private final DragonVariant variant;
    private final boolean fromAltar;
    private final UUID dragonUuid;
    private final double maxHealth;
    private long tickCount;
    private double lastHealthRatio = 1.0;
    private volatile boolean ended;
    private volatile boolean chunkForcedByFight;
    private int expectedAnchors;

    private final Map<UUID, Double> playerDamage = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> playerCrystalBreaks = new ConcurrentHashMap<>();
    private final Set<UUID> activeCrystalOrGuardianUuids = ConcurrentHashMap.newKeySet();
    private final Set<UUID> trackedMinionUuids = ConcurrentHashMap.newKeySet();

    public FightState(UUID dragonUuid, DragonVariant variant, boolean fromAltar, double maxHealth) {
        this.fightId = UUID.randomUUID();
        this.dragonUuid = dragonUuid;
        this.variant = variant != null ? variant : DragonVariant.INFERNAL;
        this.fromAltar = fromAltar;
        this.maxHealth = maxHealth;
    }

    public UUID getFightId() {
        return fightId;
    }

    public DragonVariant getVariant() {
        return variant;
    }

    public boolean isFromAltar() {
        return fromAltar;
    }

    public UUID getDragonUuid() {
        return dragonUuid;
    }

    public double getMaxHealth() {
        return maxHealth;
    }

    public long getTickCount() {
        return tickCount;
    }

    public long incrementTick() {
        return ++tickCount;
    }

    public double getLastHealthRatio() {
        return lastHealthRatio;
    }

    public void setLastHealthRatio(double lastHealthRatio) {
        this.lastHealthRatio = lastHealthRatio;
    }

    public boolean isEnded() {
        return ended;
    }

    public void markEnded() {
        this.ended = true;
    }

    public boolean isChunkForcedByFight() {
        return chunkForcedByFight;
    }

    public void setChunkForcedByFight(boolean chunkForcedByFight) {
        this.chunkForcedByFight = chunkForcedByFight;
    }

    public void recordDamage(UUID playerUuid, double amount) {
        if (ended || playerUuid == null || amount <= 0.0 || Double.isNaN(amount) || Double.isInfinite(amount)) {
            return;
        }
        playerDamage.merge(playerUuid, amount, Double::sum);
    }

    public void recordCrystalOrGuardianBreak(UUID playerUuid) {
        if (ended || playerUuid == null) {
            return;
        }
        playerCrystalBreaks.merge(playerUuid, 1, Integer::sum);
    }

    public double getDamage(UUID playerUuid) {
        if (playerUuid == null) {
            return 0.0;
        }
        return playerDamage.getOrDefault(playerUuid, 0.0);
    }

    public int getCrystalBreaks(UUID playerUuid) {
        if (playerUuid == null) {
            return 0;
        }
        return playerCrystalBreaks.getOrDefault(playerUuid, 0);
    }

    public double getTotalDamage() {
        double sum = 0.0;
        for (double val : playerDamage.values()) {
            sum += val;
        }
        return sum;
    }

    public Map<UUID, Double> getPlayerDamageSnapshot() {
        return Collections.unmodifiableMap(playerDamage);
    }

    public Map<UUID, Integer> getPlayerCrystalBreaksSnapshot() {
        return Collections.unmodifiableMap(playerCrystalBreaks);
    }

    public boolean isPlayerQualified(UUID playerUuid, double minDamagePercent, double minDamageFlat) {
        if (playerUuid == null) {
            return false;
        }
        double dmg = getDamage(playerUuid);
        if (dmg <= 0.0) {
            return false;
        }
        if (Double.isFinite(minDamageFlat) && minDamageFlat >= 0.0D && dmg >= minDamageFlat) {
            return true;
        }
        double total = getTotalDamage();
        return total > 0.0D
                && Double.isFinite(minDamagePercent)
                && minDamagePercent >= 0.0D
                && dmg >= total * minDamagePercent;
    }

    public void addAnchor(UUID uuid) {
        if (!ended && uuid != null) {
            activeCrystalOrGuardianUuids.add(uuid);
            expectedAnchors = Math.max(0, expectedAnchors - 1);
        }
    }

    public void markAnchorsExpected(int count) {
        this.expectedAnchors = count;
    }

    public void removeAnchor(UUID uuid) {
        if (uuid != null) {
            activeCrystalOrGuardianUuids.remove(uuid);
        }
    }

    public boolean hasActiveAnchors() {
        return !activeCrystalOrGuardianUuids.isEmpty() || expectedAnchors > 0;
    }

    public int getActiveAnchorCount() {
        return activeCrystalOrGuardianUuids.size();
    }

    public Set<UUID> getActiveAnchors() {
        return Collections.unmodifiableSet(activeCrystalOrGuardianUuids);
    }

    public void trackMinion(UUID uuid) {
        if (!ended && uuid != null) {
            trackedMinionUuids.add(uuid);
        }
    }

    public void untrackMinion(UUID uuid) {
        if (uuid != null) {
            trackedMinionUuids.remove(uuid);
        }
    }

    public boolean isTrackedMinion(UUID uuid) {
        return uuid != null && trackedMinionUuids.contains(uuid);
    }

    public int getTrackedMinionCount() {
        return trackedMinionUuids.size();
    }

    public Set<UUID> getTrackedMinions() {
        return Collections.unmodifiableSet(trackedMinionUuids);
    }
}
