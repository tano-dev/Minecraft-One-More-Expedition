package com.expedition.quests.model;

import java.util.UUID;

public class QuestEntry {
    public String questId = UUID.randomUUID().toString().substring(0, 8);
    public int slotIndex = 0; // 0..4 EASY, 5..7 MEDIUM, 8..9 HARD
    public String difficulty = "EASY"; // EASY, MEDIUM, HARD
    public String category = "MINING"; // EXPLORE_BIOME, EXPLORE_STRUCTURE, EXPLORE_DUNGEON, FISHING, SLAYER, MINING, LOGGING, GATHER_SUBMIT, ACTION
    public String subType = "";        // Registry ID or action subtype
    public String displayName = "";
    public String description = "";
    public String dimension = "minecraft:overworld";
    public String iconItem = "minecraft:compass";

    public int requiredProgress = 1;
    public int currentProgress = 0;
    public boolean completed = false;

    // For Exploration Quests (Lazy-Located on accept or shared)
    public boolean located = false;
    public int targetX = 0;
    public int targetZ = 0;
    public int minExpectedDist = 600;
    public int maxExpectedDist = 1600;
    public int actualDistance = 0;

    public long rewardCoins = 15;
    public int rewardXp = 50;
    public long acceptedAtTick = 0L;

    public QuestEntry() {}

    public QuestEntry copyForSlot(int newSlotIndex) {
        QuestEntry c = new QuestEntry();
        c.questId = this.questId;
        c.slotIndex = newSlotIndex;
        c.difficulty = this.difficulty;
        c.category = this.category;
        c.subType = this.subType;
        c.displayName = this.displayName;
        c.description = this.description;
        c.dimension = this.dimension;
        c.iconItem = this.iconItem;
        c.requiredProgress = this.requiredProgress;
        c.currentProgress = 0;
        c.completed = false;
        c.located = this.located;
        c.targetX = this.targetX;
        c.targetZ = this.targetZ;
        c.minExpectedDist = this.minExpectedDist;
        c.maxExpectedDist = this.maxExpectedDist;
        c.actualDistance = this.actualDistance;
        c.rewardCoins = this.rewardCoins;
        c.rewardXp = this.rewardXp;
        return c;
    }

    public boolean isExploration() {
        return "EXPLORE_BIOME".equals(category)
                || "EXPLORE_STRUCTURE".equals(category)
                || "EXPLORE_DUNGEON".equals(category);
    }

    public String getDifficultyTag() {
        return switch (difficulty) {
            case "EASY" -> "§a[E]";
            case "MEDIUM" -> "§e[M]";
            default -> "§c[H]";
        };
    }

    public String getDifficultyTitle() {
        return switch (difficulty) {
            case "EASY" -> "§a§lDỄ (EASY)";
            case "MEDIUM" -> "§e§lTRUNG BÌNH (MEDIUM)";
            default -> "§c§lKHÓ (HARD)";
        };
    }
}
