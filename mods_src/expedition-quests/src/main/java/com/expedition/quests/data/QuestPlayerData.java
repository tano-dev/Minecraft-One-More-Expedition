package com.expedition.quests.data;

import com.expedition.quests.config.QuestsConfig;
import com.expedition.quests.model.QuestEntry;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class QuestPlayerData {
    public UUID uuid;
    public int level = 1;
    public double xp = 0.0;
    public int totalCompletedQuests = 0;
    public String currentDayKey = "";
    public int rerollsUsedToday = 0;
    public boolean all10BonusClaimedToday = false;

    // 10 Daily Slots: 0..4 (EASY), 5..7 (MEDIUM), 8..9 (HARD)
    public Map<Integer, QuestEntry> activeSlots = new ConcurrentHashMap<>();
    // Draft Choices per empty slot (3-5 choices)
    public Map<Integer, List<QuestEntry>> draftOffers = new ConcurrentHashMap<>();

    // Explicitly tracked exploration quest slot index for compass navigation (-1 = auto nearest)
    public int trackedExplorationSlot = -1;

    public transient boolean dirty = false;

    public QuestPlayerData(UUID uuid) {
        this.uuid = uuid;
        this.currentDayKey = LocalDate.now().toString();
    }

    public void checkDailyReset() {
        String today = LocalDate.now().toString();
        if (!today.equals(this.currentDayKey)) {
            this.currentDayKey = today;
            this.activeSlots.clear();
            this.draftOffers.clear();
            this.rerollsUsedToday = 0;
            this.all10BonusClaimedToday = false;
            this.dirty = true;
        }
    }

    public static String getSlotDifficulty(int slotIndex) {
        if (slotIndex >= 0 && slotIndex < 5) return "EASY";
        if (slotIndex >= 5 && slotIndex < 8) return "MEDIUM";
        return "HARD";
    }

    public int getCompletedTodayCount() {
        checkDailyReset();
        int cnt = 0;
        for (QuestEntry q : activeSlots.values()) {
            if (q != null && q.completed) cnt++;
        }
        return cnt;
    }

    public static double getRequiredXpForNextLevel(int currentLevel) {
        int maxLv = QuestsConfig.getMaxLevel();
        if (currentLevel >= maxLv) return 0.0;
        return Math.round(100.0 * Math.pow(currentLevel, 1.55));
    }

    public double getXpProgressPercent() {
        int maxLv = QuestsConfig.getMaxLevel();
        if (level >= maxLv) return 100.0;
        double req = getRequiredXpForNextLevel(level);
        if (req <= 0) return 100.0;
        return Math.min(100.0, Math.max(0.0, (xp / req) * 100.0));
    }

    public int addXp(double amount) {
        if (amount <= 0) return 0;
        int maxLv = QuestsConfig.getMaxLevel();
        if (this.level >= maxLv) return 0;

        this.xp += amount;
        int gained = 0;
        while (this.level < maxLv) {
            double req = getRequiredXpForNextLevel(this.level);
            if (this.xp >= req) {
                this.xp -= req;
                this.level++;
                gained++;
            } else {
                break;
            }
        }
        if (this.level >= maxLv) {
            this.xp = 0;
        }
        this.dirty = true;
        return gained;
    }
}
