package com.expedition.fishing.data;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class FishingPlayerData {
    public int level = 1;
    public double xp = 0.0;
    public double heat = 0.0;
    public int totalCatches = 0;
    public int seaCreaturesCaught = 0;
    public int treasuresCaught = 0;
    public Map<String, Integer> mobCatches = new ConcurrentHashMap<>();
    public Map<String, Double> bestWeights = new ConcurrentHashMap<>();
    public Set<String> discoveredArtifacts = ConcurrentHashMap.newKeySet();
    public List<String> artifactBag = new ArrayList<>();

    // Transient runtime states (not persisted)
    public transient int frenzyStacks = 0;
    public transient long bloodBuffUntilTick = 0L;
    public transient boolean nextCastFast = false;
    public transient long slayerBuffUntilTick = 0L;
    public transient int slayerBuffLevel = 0;
    public transient boolean dirty = false;

    public static double getXpForNextLevel(int currentLevel) {
        if (currentLevel >= 50) return Double.MAX_VALUE;
        return Math.round(100.0 * Math.pow(currentLevel, 1.65));
    }

    public double getXpProgressPercent() {
        if (level >= 50) return 100.0;
        double req = getXpForNextLevel(level);
        if (req <= 0) return 100.0;
        return Math.min(100.0, (xp / req) * 100.0);
    }

    public int addXp(double amount) {
        if (amount <= 0 || level >= 50) return 0;
        this.xp += amount;
        int levelsGained = 0;
        while (this.level < 50) {
            double req = getXpForNextLevel(this.level);
            if (this.xp >= req) {
                this.xp -= req;
                this.level++;
                levelsGained++;
            } else {
                break;
            }
        }
        if (this.level >= 50) {
            this.xp = 0;
        }
        this.dirty = true;
        return levelsGained;
    }

    public void addHeat(double delta) {
        double next = Math.max(0.0, Math.min(100.0, this.heat + delta));
        if (Math.abs(next - this.heat) > 0.01) {
            this.heat = next;
            this.dirty = true;
        }
    }

    public boolean recordMobCatch(String mobId, double weightKg) {
        this.totalCatches++;
        this.seaCreaturesCaught++;
        this.mobCatches.merge(mobId, 1, Integer::sum);
        double prevBest = this.bestWeights.getOrDefault(mobId, 0.0);
        boolean isNewRecord = weightKg > prevBest;
        if (isNewRecord) {
            this.bestWeights.put(mobId, Math.round(weightKg * 100.0) / 100.0);
        }
        this.dirty = true;
        return isNewRecord;
    }
}
