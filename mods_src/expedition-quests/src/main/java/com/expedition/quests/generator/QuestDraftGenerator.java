package com.expedition.quests.generator;

import com.expedition.quests.api.ExpeditionQuestsApi;
import com.expedition.quests.config.QuestsConfig;
import com.expedition.quests.data.QuestPlayerData;
import com.expedition.quests.exploration.StructureClassifier;
import com.expedition.quests.exploration.UnexploredStructureLocator;
import com.expedition.quests.model.QuestEntry;
import net.minecraft.server.level.ServerPlayer;

import java.util.*;

public class QuestDraftGenerator {

    private static final List<String> ALL_BRANCHES = List.of(
            "EXPLORE_BIOME",
            "EXPLORE_STRUCTURE",
            "EXPLORE_DUNGEON",
            "MINING",
            "LOGGING",
            "GATHER_SUBMIT",
            "FISHING",
            "SLAYER",
            "ACTION"
    );

    public static List<QuestEntry> generateDraftChoices(ServerPlayer player, QuestPlayerData data, int slotIndex, int count) {
        String difficulty = QuestPlayerData.getSlotDifficulty(slotIndex);
        int advLv = data.level;
        boolean netherUnlocked = advLv >= QuestsConfig.getNetherUnlockLevel();
        boolean endUnlocked = advLv >= QuestsConfig.getEndUnlockLevel();
        int fishLv = ExpeditionQuestsApi.getFishingLevel(player.getUUID());

        Random rng = new Random();
        List<String> branches = new ArrayList<>(ALL_BRANCHES);
        Collections.shuffle(branches, rng);

        List<QuestEntry> offers = new ArrayList<>();
        for (String branch : branches) {
            if (offers.size() >= count) break;
            QuestEntry q = buildQuestForBranch(branch, difficulty, advLv, fishLv, netherUnlocked, endUnlocked, rng);
            if (q != null) {
                q.slotIndex = slotIndex;
                offers.add(q);
            }
        }

        // Fill up if any branch returned null
        int guard = 0;
        while (offers.size() < count && guard++ < 20) {
            String branch = ALL_BRANCHES.get(rng.nextInt(ALL_BRANCHES.size()));
            QuestEntry q = buildQuestForBranch(branch, difficulty, advLv, fishLv, netherUnlocked, endUnlocked, rng);
            if (q != null) {
                q.slotIndex = slotIndex;
                offers.add(q);
            }
        }
        return offers;
    }

    private static QuestEntry buildQuestForBranch(
            String branch,
            String difficulty,
            int advLv,
            int fishLv,
            boolean netherUnlocked,
            boolean endUnlocked,
            Random rng
    ) {
        String dim = pickDimension(netherUnlocked, endUnlocked, rng);
        long baseCoins = QuestsConfig.getBaseTierCoins(difficulty);
        int baseXp = QuestsConfig.getBaseTierXp(difficulty);
        double dimMult = QuestsConfig.getDimensionRewardMultiplier(dim);

        QuestEntry q = new QuestEntry();
        q.difficulty = difficulty;
        q.category = branch;
        q.dimension = dim;

        switch (branch) {
            case "EXPLORE_BIOME", "EXPLORE_STRUCTURE", "EXPLORE_DUNGEON" -> {
                List<String> pool = UnexploredStructureLocator.getPool(branch, dim);
                if (pool.isEmpty()) {
                    dim = "minecraft:overworld";
                    q.dimension = dim;
                    dimMult = 1.0;
                    pool = UnexploredStructureLocator.getPool(branch, dim);
                }
                if (pool.isEmpty()) return null;

                String targetId = pool.get(rng.nextInt(pool.size()));
                q.subType = targetId;
                q.requiredProgress = 1;
                q.minExpectedDist = QuestsConfig.getExplorationMinBlocks(branch, difficulty, dim);
                q.maxExpectedDist = QuestsConfig.getExplorationMaxBlocks(branch, difficulty, dim);

                double schoolMult = QuestsConfig.getExplorationSchoolRewardMultiplier(branch);
                q.rewardCoins = Math.max(1L, Math.round(baseCoins * dimMult * schoolMult));
                q.rewardXp = (int) Math.max(10, Math.round(baseXp * dimMult * schoolMult));

                String prefix = switch (branch) {
                    case "EXPLORE_BIOME" -> "🌿 Khám phá Quần xã: ";
                    case "EXPLORE_DUNGEON" -> "🗝️ Thám hiểm Dungeon: ";
                    default -> "🏛️ Khám phá Công trình: ";
                };
                q.displayName = prefix + StructureClassifier.formatDisplayName(targetId);
                q.description = "Đi tới vùng chưa ai khám phá (" + q.minExpectedDist + "m – " + q.maxExpectedDist + "m). Tọa độ sẽ lộ sau khi nhận!";
                q.iconItem = switch (branch) {
                    case "EXPLORE_BIOME" -> "minecraft:grass_block";
                    case "EXPLORE_DUNGEON" -> "minecraft:spawner";
                    default -> "minecraft:filled_map";
                };
                return q;
            }
            case "MINING" -> {
                q.rewardCoins = Math.max(1L, Math.round(baseCoins * dimMult));
                q.rewardXp = (int) Math.max(10, Math.round(baseXp * dimMult));
                q.iconItem = "minecraft:iron_pickaxe";
                if ("minecraft:the_nether".equals(dim)) {
                    if ("HARD".equals(difficulty)) {
                        q.subType = "minecraft:ancient_debris";
                        q.requiredProgress = 2 + rng.nextInt(3);
                        q.displayName = "⛏️ Đào " + q.requiredProgress + " Ancient Debris";
                        q.iconItem = "minecraft:netherite_scrap";
                    } else {
                        q.subType = "minecraft:nether_quartz_ore";
                        q.requiredProgress = "EASY".equals(difficulty) ? 24 : 40;
                        q.displayName = "⛏️ Đào " + q.requiredProgress + " Quặng Thạch Anh (Quartz)";
                    }
                } else {
                    if ("EASY".equals(difficulty)) {
                        String[] ores = {"minecraft:coal_ore", "minecraft:iron_ore", "minecraft:copper_ore"};
                        q.subType = ores[rng.nextInt(ores.length)];
                        q.requiredProgress = 24 + rng.nextInt(13);
                        q.displayName = "⛏️ Đào " + q.requiredProgress + " " + prettyItem(q.subType);
                    } else if ("MEDIUM".equals(difficulty)) {
                        String[] ores = {"minecraft:gold_ore", "minecraft:redstone_ore", "minecraft:lapis_ore"};
                        q.subType = ores[rng.nextInt(ores.length)];
                        q.requiredProgress = 16 + rng.nextInt(13);
                        q.displayName = "⛏️ Đào " + q.requiredProgress + " " + prettyItem(q.subType);
                    } else {
                        String[] ores = {"minecraft:diamond_ore", "minecraft:emerald_ore"};
                        q.subType = ores[rng.nextInt(ores.length)];
                        q.requiredProgress = 6 + rng.nextInt(6);
                        q.displayName = "⛏️ Đào " + q.requiredProgress + " " + prettyItem(q.subType);
                        q.iconItem = "minecraft:diamond_pickaxe";
                    }
                }
                q.description = "Khai thác quặng tự nhiên (chặn đặt block ra đập lại).";
                return q;
            }
            case "LOGGING" -> {
                q.rewardCoins = Math.max(1L, Math.round(baseCoins * dimMult));
                q.rewardXp = (int) Math.max(10, Math.round(baseXp * dimMult));
                q.iconItem = "minecraft:iron_axe";
                if ("minecraft:the_nether".equals(dim)) {
                    q.subType = rng.nextBoolean() ? "minecraft:crimson_stem" : "minecraft:warped_stem";
                    q.requiredProgress = "EASY".equals(difficulty) ? 24 : ("MEDIUM".equals(difficulty) ? 48 : 72);
                    q.displayName = "🪓 Đốn " + q.requiredProgress + " " + prettyItem(q.subType);
                } else {
                    String[] logs = {"minecraft:oak_log", "minecraft:birch_log", "minecraft:spruce_log", "minecraft:dark_oak_log", "minecraft:cherry_log"};
                    q.subType = logs[rng.nextInt(logs.length)];
                    q.requiredProgress = "EASY".equals(difficulty) ? 32 : ("MEDIUM".equals(difficulty) ? 64 : 96);
                    q.displayName = "🪓 Đốn " + q.requiredProgress + " " + prettyItem(q.subType);
                }
                q.description = "Chặt gỗ tự nhiên (hỗ trợ Veinminer, không tính gỗ tự đặt).";
                return q;
            }
            case "GATHER_SUBMIT" -> {
                q.rewardCoins = Math.max(1L, Math.round(baseCoins * dimMult));
                q.rewardXp = (int) Math.max(10, Math.round(baseXp * dimMult));
                q.iconItem = "minecraft:chest";
                if ("minecraft:the_end".equals(dim)) {
                    q.subType = rng.nextBoolean() ? "minecraft:end_stone_bricks" : "minecraft:purpur_block";
                    q.requiredProgress = "EASY".equals(difficulty) ? 32 : 64;
                } else if ("minecraft:the_nether".equals(dim)) {
                    q.subType = rng.nextBoolean() ? "minecraft:magma_block" : "minecraft:blackstone";
                    q.requiredProgress = "EASY".equals(difficulty) ? 32 : 64;
                } else {
                    String[] mats = {"minecraft:moss_block", "minecraft:clay", "minecraft:prismarine", "minecraft:tuff", "minecraft:mud_bricks"};
                    q.subType = mats[rng.nextInt(mats.length)];
                    q.requiredProgress = "EASY".equals(difficulty) ? 32 : ("MEDIUM".equals(difficulty) ? 48 : 64);
                }
                q.displayName = "📦 Nộp " + q.requiredProgress + " " + prettyItem(q.subType);
                q.description = "Thu thập đủ số lượng và bấm Nộp trong /quest (hoặc /quest submit).";
                return q;
            }
            case "FISHING" -> {
                q.rewardCoins = Math.max(1L, Math.round(baseCoins * dimMult));
                q.rewardXp = (int) Math.max(10, Math.round(baseXp * dimMult));
                q.iconItem = "minecraft:fishing_rod";
                if ("minecraft:the_end".equals(dim) && fishLv >= 35) {
                    q.subType = "VOID_CATCH";
                    q.requiredProgress = "EASY".equals(difficulty) ? 5 : ("MEDIUM".equals(difficulty) ? 10 : 15);
                    q.displayName = "🌌 Câu " + q.requiredProgress + " lần tại The End Void";
                } else if ("minecraft:the_nether".equals(dim) && fishLv >= 27) {
                    q.subType = "LAVA_CATCH";
                    q.requiredProgress = "EASY".equals(difficulty) ? 6 : ("MEDIUM".equals(difficulty) ? 12 : 18);
                    q.displayName = "🔥 Câu " + q.requiredProgress + " lần trong Lava Nether";
                } else {
                    q.dimension = "minecraft:overworld";
                    if ("EASY".equals(difficulty)) {
                        q.subType = "ANY_CATCH";
                        q.requiredProgress = 12;
                        q.displayName = "🎣 Câu thành công 12 lần";
                    } else if ("MEDIUM".equals(difficulty)) {
                        q.subType = "SEA_CREATURE_KILL";
                        q.requiredProgress = 4;
                        q.displayName = "🐙 Hạ gục 4 Thủy Quái câu được";
                    } else {
                        q.subType = "RARE_OR_TREASURE";
                        q.requiredProgress = 2;
                        q.displayName = "👑 Câu 2 Rương Kho Báu hoặc Thủy Quái Hiếm";
                    }
                }
                q.description = "Tích hợp trực tiếp với hệ thống Expedition Fishing.";
                return q;
            }
            case "SLAYER" -> {
                q.rewardCoins = Math.max(1L, Math.round(baseCoins * dimMult));
                q.rewardXp = (int) Math.max(10, Math.round(baseXp * dimMult));
                q.iconItem = "minecraft:diamond_sword";
                if ("minecraft:the_end".equals(dim)) {
                    q.subType = "minecraft:enderman";
                    q.requiredProgress = "EASY".equals(difficulty) ? 12 : ("MEDIUM".equals(difficulty) ? 20 : 30);
                    q.displayName = "⚔️ Tiêu diệt " + q.requiredProgress + " Enderman";
                } else if ("minecraft:the_nether".equals(dim)) {
                    String[] mobs = {"minecraft:blaze", "minecraft:wither_skeleton", "minecraft:ghast"};
                    q.subType = mobs[rng.nextInt(mobs.length)];
                    q.requiredProgress = "EASY".equals(difficulty) ? 8 : ("MEDIUM".equals(difficulty) ? 14 : 22);
                    q.displayName = "⚔️ Tiêu diệt " + q.requiredProgress + " " + prettyItem(q.subType);
                } else {
                    if ("EASY".equals(difficulty)) {
                        String[] mobs = {"minecraft:zombie", "minecraft:skeleton", "minecraft:spider", "minecraft:creeper"};
                        q.subType = mobs[rng.nextInt(mobs.length)];
                        q.requiredProgress = 15 + rng.nextInt(10);
                    } else if ("MEDIUM".equals(difficulty)) {
                        String[] mobs = {"minecraft:pillager", "minecraft:drowned", "minecraft:witch"};
                        q.subType = mobs[rng.nextInt(mobs.length)];
                        q.requiredProgress = 10 + rng.nextInt(6);
                    } else {
                        q.subType = "minecraft:ravager";
                        q.requiredProgress = 2;
                    }
                    q.displayName = "⚔️ Tiêu diệt " + q.requiredProgress + " " + prettyItem(q.subType);
                }
                q.description = "Trực tiếp chiến đấu tiêu diệt quái vật (chống đứng yên farm spawner).";
                return q;
            }
            case "ACTION" -> {
                q.rewardCoins = Math.max(1L, Math.round(baseCoins * dimMult));
                q.rewardXp = (int) Math.max(10, Math.round(baseXp * dimMult));
                boolean allowElytra = !QuestsConfig.requireEndUnlockForElytra() || endUnlocked;

                if ("minecraft:the_end".equals(dim) && allowElytra) {
                    q.subType = "ELYTRA";
                    q.requiredProgress = QuestsConfig.getTravelDistance("ELYTRA", difficulty);
                    q.displayName = "🪽 Bay Elytra " + q.requiredProgress + " blocks";
                    q.iconItem = "minecraft:elytra";
                } else if ("minecraft:the_nether".equals(dim)) {
                    if (rng.nextBoolean()) {
                        q.subType = "LAVA_STRIDER";
                        q.requiredProgress = QuestsConfig.getTravelDistance("LAVA_STRIDER", difficulty);
                        q.displayName = "🦎 Cưỡi Strider trên Lava " + q.requiredProgress + "m";
                        q.iconItem = "minecraft:warped_fungus_on_a_stick";
                    } else {
                        q.subType = "BREW_POTION";
                        q.requiredProgress = "EASY".equals(difficulty) ? 3 : ("MEDIUM".equals(difficulty) ? 6 : 9);
                        q.displayName = "🧪 Pha chế " + q.requiredProgress + " bình Thuốc";
                        q.iconItem = "minecraft:brewing_stand";
                    }
                } else {
                    int roll = rng.nextInt(3);
                    if (roll == 0) {
                        q.subType = "BOAT";
                        q.requiredProgress = QuestsConfig.getTravelDistance("BOAT", difficulty);
                        q.displayName = "🚣 Chèo thuyền " + q.requiredProgress + " blocks";
                        q.iconItem = "minecraft:oak_boat";
                    } else if (roll == 1) {
                        q.subType = "LAND_MOUNT";
                        q.requiredProgress = QuestsConfig.getTravelDistance("LAND_MOUNT", difficulty);
                        q.displayName = "🐎 Cưỡi Ngựa/Lạc đà " + q.requiredProgress + " blocks";
                        q.iconItem = "minecraft:saddle";
                    } else {
                        q.subType = "ENCHANT_ITEM";
                        q.requiredProgress = "EASY".equals(difficulty) ? 2 : ("MEDIUM".equals(difficulty) ? 4 : 6);
                        q.displayName = "✨ Phù phép " + q.requiredProgress + " vật phẩm tại Bàn Enchant";
                        q.iconItem = "minecraft:enchanting_table";
                    }
                }
                q.description = "Thực hiện hành động tương tác hoặc di chuyển thực tế.";
                return q;
            }
        }
        return null;
    }

    private static String pickDimension(boolean netherUnlocked, boolean endUnlocked, Random rng) {
        int roll = rng.nextInt(100);
        if (endUnlocked && roll < 22) return "minecraft:the_end";
        if (netherUnlocked && roll < 52) return "minecraft:the_nether";
        return "minecraft:overworld";
    }

    private static String prettyItem(String id) {
        String p = id.contains(":") ? id.split(":", 2)[1] : id;
        String[] words = p.replace('_', ' ').split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String w : words) {
            if (w.isEmpty()) continue;
            sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1).toLowerCase(Locale.ROOT)).append(" ");
        }
        return sb.toString().trim();
    }
}
