package com.expedition.quests.api;

import com.expedition.quests.config.QuestsConfig;
import com.expedition.quests.data.QuestDataStorage;
import com.expedition.quests.data.QuestPlayerData;
import com.expedition.quests.exploration.UnexploredStructureLocator;
import com.expedition.quests.model.QuestEntry;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.core.component.DataComponents;

import java.lang.reflect.Method;
import java.util.*;

public class ExpeditionQuestsApi {

    private static boolean fishingChecked = false;
    private static Method fishingGetLevelMethod = null;

    public static int getAdventureLevel(UUID uuid) {
        return QuestDataStorage.get(uuid).level;
    }

    public static double getXpProgressPercent(UUID uuid) {
        return QuestDataStorage.get(uuid).getXpProgressPercent();
    }

    public static long getCurrentXp(UUID uuid) {
        return Math.round(QuestDataStorage.get(uuid).xp);
    }

    public static long getRequiredXp(UUID uuid) {
        return Math.round(QuestPlayerData.getRequiredXpForNextLevel(QuestDataStorage.get(uuid).level));
    }

    public static int getCompletedTodayCount(UUID uuid) {
        return QuestDataStorage.get(uuid).getCompletedTodayCount();
    }

    public static boolean isEnabledForPlayer(ServerPlayer player) {
        if (player == null) return false;
        String dim = player.level().dimension().identifier().toString();
        return !dim.contains("skyblock");
    }

    /**
     * Returns up to maxCount active (uncompleted) quest lines formatted for the Sidebar.
     * Completed quests are automatically skipped!
     * Exactly ONE exploration quest is designated for compass navigation & coordinates:
     * - Either the player's manually tracked slot, or automatically the closest exploration quest.
     */
    public static List<String> getSidebarQuestLines(ServerPlayer player, int maxCount) {
        if (player == null || !isEnabledForPlayer(player)) return Collections.emptyList();
        QuestPlayerData data = QuestDataStorage.get(player.getUUID());
        List<QuestEntry> active = new ArrayList<>();
        for (QuestEntry q : data.activeSlots.values()) {
            if (q != null && !q.completed) {
                active.add(q);
            }
        }
        if (active.isEmpty()) return Collections.emptyList();

        String currentDim = player.level().dimension().identifier().toString();

        // Ensure all exploration quests have their coordinates locked!
        for (QuestEntry q : active) {
            if (q.isExploration() && !q.located) {
                UnexploredStructureLocator.locateAndLockTarget(player.level().getServer(), player, q);
            }
        }

        // Determine the single primary exploration quest for compass navigation
        QuestEntry primaryExploration = null;
        if (data.trackedExplorationSlot >= 0) {
            QuestEntry manual = data.activeSlots.get(data.trackedExplorationSlot);
            if (manual != null && !manual.completed && manual.isExploration()) {
                primaryExploration = manual;
            }
        }

        // If not manually chosen or invalid, pick nearest in current dimension (or nearest overall)
        if (primaryExploration == null) {
            int bestDist = Integer.MAX_VALUE;
            for (QuestEntry q : active) {
                if (q.isExploration()) {
                    boolean sameDim = currentDim.equals(q.dimension);
                    int dist = sameDim ? (int) Math.hypot(q.targetX - player.getX(), q.targetZ - player.getZ()) : Integer.MAX_VALUE - 1000;
                    if (dist < bestDist) {
                        bestDist = dist;
                        primaryExploration = q;
                    }
                }
            }
        }

        final QuestEntry finalNavTarget = primaryExploration;

        // Sort: Primary exploration first, then slotIndex
        active.sort((a, b) -> {
            if (a == finalNavTarget) return -1;
            if (b == finalNavTarget) return 1;
            return Integer.compare(a.slotIndex, b.slotIndex);
        });

        List<String> lines = new ArrayList<>();
        int count = Math.min(maxCount, active.size());
        for (int i = 0; i < count; i++) {
            QuestEntry q = active.get(i);
            String tag = switch (q.difficulty) {
                case "EASY" -> "[E]";
                case "MEDIUM" -> "[M]";
                default -> "[H]";
            };

            if (q == finalNavTarget && q.located) {
                boolean sameDim = currentDim.equals(q.dimension);
                if (sameDim) {
                    int dist = (int) Math.hypot(q.targetX - player.getX(), q.targetZ - player.getZ());
                    String arrow = UnexploredStructureLocator.getCompassArrow(player, q.targetX, q.targetZ);
                    String shortName = q.displayName
                            .replace("Explore Biome: ", "")
                            .replace("Explore Structure: ", "")
                            .replace("Explore Dungeon: ", "");
                    if (shortName.length() > 12) shortName = shortName.substring(0, 11) + "..";
                    lines.add(tag + " " + arrow + " " + shortName + " " + dist + "m (" + q.targetX + "," + q.targetZ + ")");
                } else {
                    String dimTag = q.dimension.contains("nether") ? "Nether" : (q.dimension.contains("end") ? "The End" : "Overworld");
                    lines.add(tag + " [" + dimTag + "] " + q.displayName);
                }
            } else if (q.isExploration()) {
                String shortName = q.displayName
                        .replace("Explore Biome: ", "")
                        .replace("Explore Structure: ", "")
                        .replace("Explore Dungeon: ", "");
                if (shortName.length() > 16) shortName = shortName.substring(0, 15) + "..";
                lines.add(tag + " " + shortName + ": 0/1");
            } else {
                String shortName = q.displayName;
                if (shortName.length() > 18) shortName = shortName.substring(0, 17) + "..";
                lines.add(tag + " " + shortName + ": " + q.currentProgress + "/" + q.requiredProgress);
            }
        }
        return lines;
    }

    public static int getFishingLevel(UUID uuid) {
        if (!fishingChecked) {
            fishingChecked = true;
            try {
                Class<?> api = Class.forName("com.expedition.fishing.api.ExpeditionFishingApi");
                fishingGetLevelMethod = api.getMethod("getLevel", UUID.class);
            } catch (Throwable ignored) {}
        }
        if (fishingGetLevelMethod != null) {
            try {
                return (Integer) fishingGetLevelMethod.invoke(null, uuid);
            } catch (Throwable ignored) {}
        }
        return 1;
    }

    // --- Progress Callbacks from other mods (expedition-fishing, expedition-enchants) ---
    public static void onFishingCatch(ServerPlayer player, boolean inLava, boolean inVoid, boolean isTreasure, boolean isRareOrBoss) {
        if (player == null || !isEnabledForPlayer(player)) return;
        advanceMatchingQuests(player, "FISHING", q -> {
            if ("ANY_CATCH".equals(q.subType)) return true;
            if ("LAVA_CATCH".equals(q.subType) && inLava) return true;
            if ("VOID_CATCH".equals(q.subType) && inVoid) return true;
            if ("RARE_OR_TREASURE".equals(q.subType) && (isTreasure || isRareOrBoss)) return true;
            return false;
        }, 1);
    }

    public static void onFishingSeaCreatureKilled(ServerPlayer player) {
        if (player == null || !isEnabledForPlayer(player)) return;
        advanceMatchingQuests(player, "FISHING", q -> "SEA_CREATURE_KILL".equals(q.subType), 1);
    }

    public static void onItemEnchanted(ServerPlayer player) {
        if (player == null || !isEnabledForPlayer(player)) return;
        advanceMatchingQuests(player, "ACTION", q -> "ENCHANT_ITEM".equals(q.subType), 1);
    }

    public interface QuestMatcher {
        boolean matches(QuestEntry q);
    }

    public static void advanceMatchingQuests(ServerPlayer player, String category, QuestMatcher matcher, int amount) {
        if (amount <= 0) return;
        QuestPlayerData data = QuestDataStorage.get(player.getUUID());
        boolean changed = false;

        for (QuestEntry q : data.activeSlots.values()) {
            if (q == null || q.completed) continue;
            if (!category.equals(q.category)) continue;
            if (!matcher.matches(q)) continue;

            q.currentProgress = Math.min(q.requiredProgress, q.currentProgress + amount);
            changed = true;

            if (q.currentProgress >= q.requiredProgress) {
                completeQuest(player, data, q);
            }
        }
        if (changed) {
            data.dirty = true;
            QuestDataStorage.saveAsync(player.getUUID());
        }
    }

    public static void completeQuest(ServerPlayer player, QuestPlayerData data, QuestEntry q) {
        if (q.completed) return;
        q.completed = true;
        q.currentProgress = q.requiredProgress;
        data.totalCompletedQuests++;

        // Calculate Rewards: Adventure Level +1%/lv + Bounty Hunter Armor + Adventurer Armor
        double lvCoinBonus = 1.0 + (data.level * QuestsConfig.getCoinBonusPerLevel());
        int bountyLevels = getTotalArmorEnchantLevel(player, "expedition_quests:bounty_hunter");
        double bountyMult = 1.0 + (bountyLevels * QuestsConfig.getEnchantVal("bounty_hunter_coin_bonus_per_level", 0.04));

        int advLevels = getTotalArmorEnchantLevel(player, "expedition_quests:adventurer");
        double advXpMult = 1.0 + (advLevels * QuestsConfig.getEnchantVal("adventurer_xp_bonus_per_level", 0.05));

        long finalCoins = Math.max(1L, Math.round(q.rewardCoins * lvCoinBonus * bountyMult));
        int finalXp = (int) Math.max(5, Math.round(q.rewardXp * advXpMult));

        depositEconomyCoins(player, finalCoins);
        int oldLv = data.level;
        int lvGained = data.addXp(finalXp);

        player.sendSystemMessage(Component.literal(
                "搂a搂l鉁?QUEST COMPLETED! 搂f" + q.displayName +
                " 搂7(搂6+" + finalCoins + " Coins搂7, 搂b+" + finalXp + " Adventure XP搂7)"
        ));
        player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8f, 1.3f);

        if (lvGained > 0) {
            onAdventureLevelUp(player, oldLv, data.level);
        }

        // Check if all 10 daily quests completed!
        if (!data.all10BonusClaimedToday && data.getCompletedTodayCount() >= 10) {
            data.all10BonusClaimedToday = true;
            long bonusCoins = Math.round(QuestsConfig.getAll10BonusCoins() * lvCoinBonus * bountyMult);
            int bonusXp = (int) Math.round(QuestsConfig.getAll10BonusXp() * advXpMult);
            depositEconomyCoins(player, bonusCoins);
            int o2 = data.level;
            int g2 = data.addXp(bonusXp);
            player.sendSystemMessage(Component.literal(
                    "搂6搂l鈽?ALL 10 DAILY QUESTS COMPLETED! 搂eBonus: 搂6+" + bonusCoins + " Coins 搂e& 搂b+" + bonusXp + " Adventure XP搂e!"
            ));
            player.level().playSound(null, player.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0f, 1.1f);
            if (g2 > 0) {
                onAdventureLevelUp(player, o2, data.level);
            }
        }
        data.dirty = true;
    }

    private static void onAdventureLevelUp(ServerPlayer player, int oldLv, int newLv) {
        player.sendSystemMessage(Component.literal(
                "搂b搂l鈻?ADVENTURE LEVEL UP! 搂eLv." + oldLv + " 搂7-> 搂bLv." + newLv +
                " 搂7(搂6+" + newLv + "% Quest Coins Bonus搂7)"
        ));
        int netherUnlock = QuestsConfig.getNetherUnlockLevel();
        int endUnlock = QuestsConfig.getEndUnlockLevel();
        if (oldLv < netherUnlock && newLv >= netherUnlock) {
            player.sendSystemMessage(Component.literal("搂c搂l鉁?NETHER TIER UNLOCKED! 搂7Nether Quests (+20% Rewards) are now available in /quest!"));
        }
        if (oldLv < endUnlock && newLv >= endUnlock) {
            player.sendSystemMessage(Component.literal("搂d搂l鉁?THE END TIER UNLOCKED! 搂7End & Elytra Quests (+40% Rewards) are now available in /quest!"));
        }
    }

    public static int getTotalArmorEnchantLevel(ServerPlayer player, String enchantId) {
        int total = 0;
        EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
        for (EquipmentSlot slot : slots) {
            total += getItemEnchantLevel(player.getItemBySlot(slot), enchantId);
        }
        return total;
    }

    public static int getItemEnchantLevel(ItemStack stack, String enchantId) {
        if (stack == null || stack.isEmpty()) return 0;
        ItemEnchantments enchs = stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        for (Map.Entry<Holder<Enchantment>, Integer> e : enchs.entrySet()) {
            String id = e.getKey().unwrapKey().map(k -> k.identifier().toString()).orElse("");
            if (enchantId.equals(id)) {
                return e.getValue();
            }
        }
        return 0;
    }

    public static void depositEconomyCoins(ServerPlayer player, long amount) {
        if (amount <= 0 || player == null) return;
        try {
            MinecraftServer server = player.level().getServer();
            // Correct class: com.reazip.economycraft.EconomyCraft (verified via javap)
            Class<?> ecoClass = Class.forName("com.reazip.economycraft.EconomyCraft");
            Object mgr = ecoClass.getMethod("getManager", MinecraftServer.class).invoke(null, server);
            if (mgr != null) {
                Class<?> mutClass = Class.forName("com.reazip.economycraft.api.v1.MutationSource");
                Object src = mutClass.getMethod("of", String.class).invoke(null, "expedition_quests");
                mgr.getClass().getMethod("addMoney", UUID.class, long.class, mutClass).invoke(mgr, player.getUUID(), amount, src);
            }
        } catch (Throwable t) {
            com.expedition.quests.ExpeditionQuestsMod.LOGGER.warn("[Expedition Quests] Failed to deposit {} coins for {}: {}", amount, player.getName().getString(), t.getMessage());
        }
    }
}
