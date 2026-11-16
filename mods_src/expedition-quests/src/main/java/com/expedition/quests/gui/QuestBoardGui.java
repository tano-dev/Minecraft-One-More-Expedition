package com.expedition.quests.gui;

import com.expedition.quests.api.ExpeditionQuestsApi;
import com.expedition.quests.config.QuestsConfig;
import com.expedition.quests.data.QuestDataStorage;
import com.expedition.quests.data.QuestPlayerData;
import com.expedition.quests.exploration.UnexploredStructureLocator;
import com.expedition.quests.generator.QuestDraftGenerator;
import com.expedition.quests.model.QuestEntry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;

import java.util.ArrayList;
import java.util.List;

public class QuestBoardGui {

    public enum ViewMode {
        BOARD_10_SLOTS,
        DRAFT_PICKER
    }

    public static void openBoard(ServerPlayer player) {
        QuestPlayerData data = QuestDataStorage.get(player.getUUID());
        String title = "§b§l✦ DAILY QUESTS (" + data.getCompletedTodayCount() + "/10) ✦";
        player.openMenu(new SimpleMenuProvider(
                (syncId, inv, p) -> new QuestMenu(syncId, inv, player, ViewMode.BOARD_10_SLOTS, -1),
                Component.literal(title)
        ));
    }

    public static void openDraft(ServerPlayer player, int slotIndex) {
        QuestPlayerData data = QuestDataStorage.get(player.getUUID());
        int contractLv = ExpeditionQuestsApi.getItemEnchantLevel(player.getItemBySlot(EquipmentSlot.HEAD), "expedition_quests:contract_master");
        int choicesCount = QuestsConfig.getBaseDraftChoices() + (contractLv > 0 ? (int) QuestsConfig.getEnchantVal("contract_master_extra_choices", 1) : 0);

        data.draftOffers.computeIfAbsent(slotIndex, k -> QuestDraftGenerator.generateDraftChoices(player, data, slotIndex, choicesCount));
        data.dirty = true;

        String diffName = QuestPlayerData.getSlotDifficulty(slotIndex);
        player.openMenu(new SimpleMenuProvider(
                (syncId, inv, p) -> new QuestMenu(syncId, inv, player, ViewMode.DRAFT_PICKER, slotIndex),
                Component.literal("§e§lSELECT QUEST [" + diffName + "]")
        ));
    }

    public static class QuestMenu extends AbstractContainerMenu {
        private final SimpleContainer container = new SimpleContainer(54);
        private final ServerPlayer player;
        private final ViewMode mode;
        private final int draftSlotIndex;

        public QuestMenu(int syncId, Inventory playerInv, ServerPlayer player, ViewMode mode, int draftSlotIndex) {
            super(MenuType.GENERIC_9x6, syncId);
            this.player = player;
            this.mode = mode;
            this.draftSlotIndex = draftSlotIndex;

            for (int r = 0; r < 6; r++) {
                for (int c = 0; c < 9; c++) {
                    this.addSlot(new Slot(container, r * 9 + c, 8 + c * 18, 18 + r * 18) {
                        @Override public boolean mayPickup(Player p) { return false; }
                        @Override public boolean mayPlace(ItemStack s) { return false; }
                    });
                }
            }
            for (int r = 0; r < 3; r++) {
                for (int c = 0; c < 9; c++) {
                    this.addSlot(new Slot(playerInv, c + r * 9 + 9, 8 + c * 18, 140 + r * 18));
                }
            }
            for (int c = 0; c < 9; c++) {
                this.addSlot(new Slot(playerInv, c, 8 + c * 18, 198));
            }

            populate();
        }

        private void populate() {
            ItemStack glass = named(resolveItem("minecraft:black_stained_glass_pane"), "§8", List.of());
            for (int i = 0; i < 54; i++) container.setItem(i, glass.copy());

            QuestPlayerData data = QuestDataStorage.get(player.getUUID());

            double pct = data.getXpProgressPercent();
            long req = ExpeditionQuestsApi.getRequiredXp(player.getUUID());
            List<String> profLore = List.of(
                    "§7Adventure Level: §bLv." + data.level + " §8/ " + QuestsConfig.getMaxLevel(),
                    "§7Progress: §e" + Math.round(data.xp) + " / " + req + " XP §7(" + String.format("%.0f%%", pct) + ")",
                    "§7Perk: §6+" + data.level + "% Quest Coins Bonus",
                    "§7Nether Tier: " + (data.level >= QuestsConfig.getNetherUnlockLevel() ? "§a✔ Unlocked" : "§c✖ Requires Lv." + QuestsConfig.getNetherUnlockLevel()),
                    "§7The End & Elytra: " + (data.level >= QuestsConfig.getEndUnlockLevel() ? "§a✔ Unlocked" : "§c✖ Requires Lv." + QuestsConfig.getEndUnlockLevel())
            );
            container.setItem(4, named(Items.NETHER_STAR, "§b§lADVENTURE PROFILE", profLore));

            if (mode == ViewMode.BOARD_10_SLOTS) {
                for (int i = 0; i < 5; i++) renderBoardSlot(data, i, 11 + i);
                for (int i = 5; i < 8; i++) renderBoardSlot(data, i, 21 + (i - 5));
                renderBoardSlot(data, 8, 30);
                renderBoardSlot(data, 9, 32);

                container.setItem(49, named(Items.BARRIER, "§cClose", List.of("§7Click to close Quest Board")));
            } else {
                List<QuestEntry> offers = data.draftOffers.getOrDefault(draftSlotIndex, List.of());
                int[] positions = offers.size() >= 5 ? new int[]{20, 21, 22, 23, 24} : new int[]{19, 21, 23, 25};
                for (int i = 0; i < Math.min(positions.length, offers.size()); i++) {
                    QuestEntry q = offers.get(i);
                    Item icon = resolveItem(q.iconItem);
                    List<String> lore = new ArrayList<>();
                    lore.add("§7Difficulty: " + q.getDifficultyTitle());
                    lore.add("§7Dimension: §b" + q.dimension.replace("minecraft:", ""));
                    lore.add("§7Goal: §f" + q.description);
                    lore.add("§8");
                    lore.add("§7Rewards:");
                    lore.add("  §6• +" + q.rewardCoins + " Coins");
                    lore.add("  §b• +" + q.rewardXp + " Adventure XP");
                    lore.add("§8");
                    lore.add("§a▶ Click to ACCEPT this quest!");
                    container.setItem(positions[i], named(icon, "§e§l" + q.displayName, lore));
                }

                int contractLv = ExpeditionQuestsApi.getItemEnchantLevel(player.getItemBySlot(EquipmentSlot.HEAD), "expedition_quests:contract_master");
                int maxFreeRerolls = QuestsConfig.getBaseFreeRerolls() + (contractLv > 0 ? (int) QuestsConfig.getEnchantVal("contract_master_extra_rerolls", 1) : 0);
                int left = Math.max(0, maxFreeRerolls - data.rerollsUsedToday);
                container.setItem(40, named(Items.ENDER_EYE, "§d§lReroll Offers", List.of(
                        "§7Free rerolls left today: §e" + left,
                        left > 0 ? "§a▶ Click to refresh quest choices!" : "§c✖ No free rerolls left today!"
                )));

                container.setItem(49, named(Items.ARROW, "§e<- Back to 10-Quest Board", List.of()));
            }
        }

        private void renderBoardSlot(QuestPlayerData data, int questSlotIdx, int invSlot) {
            QuestEntry q = data.activeSlots.get(questSlotIdx);
            String diff = QuestPlayerData.getSlotDifficulty(questSlotIdx);
            if (q == null) {
                Item pane = "EASY".equals(diff) ? resolveItem("minecraft:lime_stained_glass_pane")
                        : ("MEDIUM".equals(diff) ? resolveItem("minecraft:yellow_stained_glass_pane") : resolveItem("minecraft:red_stained_glass_pane"));
                container.setItem(invSlot, named(pane, "§fSlot #" + (questSlotIdx + 1) + " [" + diff + "] - §aEmpty", List.of(
                        "§7Click to view §e4 quest offers§7!",
                        "§7Complete to earn §6Coins §7& §bAdventure XP§7."
                )));
                return;
            }

            if (q.completed) {
                container.setItem(invSlot, named(Items.EMERALD_BLOCK, "§a§l✔ " + q.displayName, List.of(
                        "§aCompleted! (Hidden from Sidebar)",
                        "§7Rewards have been added to your Purse."
                )));
                return;
            }

            Item icon = resolveItem(q.iconItem);
            List<String> lore = new ArrayList<>();
            lore.add("§7Progress: §e" + q.currentProgress + " / " + q.requiredProgress);
            if (q.isExploration() && q.located) {
                int dist = (int) Math.hypot(q.targetX - player.getX(), q.targetZ - player.getZ());
                lore.add("§bTarget: §fX=" + q.targetX + ", Z=" + q.targetZ + " §7(" + dist + "m away)");
            }
            lore.add("§7Rewards: §6+" + q.rewardCoins + " Coins §7| §b+" + q.rewardXp + " XP");
            lore.add("§8");
            if ("GATHER_SUBMIT".equals(q.category)) {
                lore.add("§a▶ Click to SUBMIT ITEMS from inventory!");
            } else {
                lore.add("§e▶ Click to SHARE in chat (/quest share " + (questSlotIdx + 1) + ")!");
            }
            container.setItem(invSlot, named(icon, q.getDifficultyTag() + " §f" + q.displayName, lore));
        }

        @Override
        public void clicked(int slotId, int button, ContainerInput clickType, Player p) {
            if (!(p instanceof ServerPlayer sp)) return;
            if (slotId < 0 || slotId >= 54) return;
            QuestPlayerData data = QuestDataStorage.get(sp.getUUID());

            if (mode == ViewMode.BOARD_10_SLOTS) {
                if (slotId == 49) {
                    sp.closeContainer();
                    return;
                }
                int qSlot = mapBoardSlotToQuestIndex(slotId);
                if (qSlot >= 0) {
                    QuestEntry existing = data.activeSlots.get(qSlot);
                    if (existing == null) {
                        openDraft(sp, qSlot);
                    } else if (!existing.completed) {
                        if ("GATHER_SUBMIT".equals(existing.category)) {
                            submitItemsForQuest(sp, data, existing);
                            populate();
                        } else {
                            data.trackedSlot = qSlot;
                            data.trackedExplorationSlot = qSlot;
                            data.dirty = true;
                            sp.sendSystemMessage(Component.literal("\u00a7a\u2714 Now tracking on sidebar: \u00a7f" + existing.displayName));
                            sp.level().playSound(null, sp.blockPosition(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.8f, 1.4f);
                            populate();
                        }
                    }
                }
            } else {
                if (slotId == 49) {
                    openBoard(sp);
                    return;
                }
                if (slotId == 40) {
                    int contractLv = ExpeditionQuestsApi.getItemEnchantLevel(sp.getItemBySlot(EquipmentSlot.HEAD), "expedition_quests:contract_master");
                    int maxFree = QuestsConfig.getBaseFreeRerolls() + (contractLv > 0 ? (int) QuestsConfig.getEnchantVal("contract_master_extra_rerolls", 1) : 0);
                    if (data.rerollsUsedToday < maxFree) {
                        data.rerollsUsedToday++;
                        data.draftOffers.remove(draftSlotIndex);
                        sp.level().playSound(null, sp.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.8f, 1.2f);
                        openDraft(sp, draftSlotIndex);
                    } else {
                        sp.sendSystemMessage(Component.literal("§c✖ You have no free rerolls left today!"));
                    }
                    return;
                }
                List<QuestEntry> offers = data.draftOffers.getOrDefault(draftSlotIndex, List.of());
                int[] positions = offers.size() >= 5 ? new int[]{20, 21, 22, 23, 24} : new int[]{19, 21, 23, 25};
                for (int i = 0; i < Math.min(positions.length, offers.size()); i++) {
                    if (slotId == positions[i]) {
                        QuestEntry chosen = offers.get(i).copyForSlot(draftSlotIndex);
                        if (chosen.isExploration()) {
                            UnexploredStructureLocator.locateAndLockTarget(sp.level().getServer(), sp, chosen);
                            sp.sendSystemMessage(Component.literal(
                                    "§b§l✦ EXPLORATION QUEST ACCEPTED! §f" + chosen.displayName +
                                    " §7- Target: §eX=" + chosen.targetX + ", Z=" + chosen.targetZ +
                                    " §7(~" + chosen.actualDistance + "m away)"
                            ));
                        } else {
                            sp.sendSystemMessage(Component.literal("§a✔ Quest accepted: §f" + chosen.displayName));
                        }
                        data.activeSlots.put(draftSlotIndex, chosen);
                        data.draftOffers.remove(draftSlotIndex);
                        data.dirty = true;
                        QuestDataStorage.saveAsync(sp.getUUID());
                        sp.level().playSound(null, sp.blockPosition(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.8f, 1.2f);
                        openBoard(sp);
                        return;
                    }
                }
            }
        }

        public static void submitItemsForQuest(ServerPlayer sp, QuestPlayerData data, QuestEntry q) {
            Item targetItem = resolveItem(q.subType);
            int needed = q.requiredProgress - q.currentProgress;
            if (needed <= 0) return;

            int taken = 0;
            Inventory inv = sp.getInventory();
            for (int i = 0; i < inv.getContainerSize() && taken < needed; i++) {
                ItemStack st = inv.getItem(i);
                if (!st.isEmpty() && st.getItem() == targetItem) {
                    int take = Math.min(needed - taken, st.getCount());
                    st.shrink(take);
                    taken += take;
                }
            }
            if (taken > 0) {
                q.currentProgress += taken;
                data.dirty = true;
                if (q.currentProgress >= q.requiredProgress) {
                    ExpeditionQuestsApi.completeQuest(sp, data, q);
                } else {
                    sp.sendSystemMessage(Component.literal("§a✔ Submitted §e" + taken + " §fitems! Progress: §e" + q.currentProgress + "/" + q.requiredProgress));
                }
                QuestDataStorage.saveAsync(sp.getUUID());
            } else {
                sp.sendSystemMessage(Component.literal("§c✖ You do not have enough §f" + q.subType + " §cin your inventory!"));
            }
        }

        private int mapBoardSlotToQuestIndex(int invSlot) {
            if (invSlot >= 11 && invSlot <= 15) return invSlot - 11;
            if (invSlot >= 21 && invSlot <= 23) return 5 + (invSlot - 21);
            if (invSlot == 30) return 8;
            if (invSlot == 32) return 9;
            return -1;
        }

        @Override public ItemStack quickMoveStack(Player p, int i) { return ItemStack.EMPTY; }
        @Override public boolean stillValid(Player p) { return true; }
    }

    private static Item resolveItem(String id) {
        try {
            Item it = BuiltInRegistries.ITEM.getValue(Identifier.parse(id));
            return it != Items.AIR ? it : Items.PAPER;
        } catch (Exception e) {
            return Items.PAPER;
        }
    }

    private static ItemStack named(Item item, String name, List<String> loreLines) {
        ItemStack stack = new ItemStack(item);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(name));
        if (loreLines != null && !loreLines.isEmpty()) {
            List<Component> comps = new ArrayList<>();
            for (String l : loreLines) comps.add(Component.literal(l));
            stack.set(DataComponents.LORE, new ItemLore(comps));
        }
        return stack;
    }
}
