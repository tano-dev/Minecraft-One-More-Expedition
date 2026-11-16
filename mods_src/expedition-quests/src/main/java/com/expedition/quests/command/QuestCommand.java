package com.expedition.quests.command;

import com.expedition.quests.api.ExpeditionQuestsApi;
import com.expedition.quests.config.QuestsConfig;
import com.expedition.quests.data.QuestDataStorage;
import com.expedition.quests.data.QuestPlayerData;
import com.expedition.quests.exploration.UnexploredStructureLocator;
import com.expedition.quests.gui.QuestBoardGui;
import com.expedition.quests.model.QuestEntry;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class QuestCommand {

    private static final Map<String, QuestEntry> SHARED_QUESTS = new ConcurrentHashMap<>();

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        var root = Commands.literal("quest")
                .executes(ctx -> {
                    ServerPlayer sp = ctx.getSource().getPlayerOrException();
                    if (!ExpeditionQuestsApi.isEnabledForPlayer(sp)) {
                        sp.sendSystemMessage(Component.literal("§cNhiệm vụ Thám hiểm chỉ hoạt động ở thế giới Sinh tồn!"));
                        return 0;
                    }
                    QuestBoardGui.openBoard(sp);
                    return 1;
                })
                .then(Commands.literal("stats").executes(ctx -> {
                    ServerPlayer sp = ctx.getSource().getPlayerOrException();
                    QuestPlayerData d = QuestDataStorage.get(sp.getUUID());
                    sp.sendSystemMessage(Component.literal(
                            "§b§l[Adventure Stats] §fLv." + d.level + " §7(" + Math.round(d.xp) + "/" +
                            ExpeditionQuestsApi.getRequiredXp(sp.getUUID()) + " XP) | §6+" + d.level + "% Tiền thưởng Quest §7| Hôm nay: §a" +
                            d.getCompletedTodayCount() + "/10"
                    ));
                    return 1;
                }))
                .then(Commands.literal("share")
                        .then(Commands.argument("slot", IntegerArgumentType.integer(1, 10))
                                .executes(ctx -> {
                                    ServerPlayer sp = ctx.getSource().getPlayerOrException();
                                    int slot = IntegerArgumentType.getInteger(ctx, "slot") - 1;
                                    shareQuestToChat(sp, slot);
                                    return 1;
                                })))
                .then(Commands.literal("accept")
                        .then(Commands.argument("code", StringArgumentType.word())
                                .executes(ctx -> {
                                    ServerPlayer sp = ctx.getSource().getPlayerOrException();
                                    String code = StringArgumentType.getString(ctx, "code");
                                    acceptSharedQuest(sp, code);
                                    return 1;
                                })))
                .then(Commands.literal("submit")
                        .then(Commands.argument("slot", IntegerArgumentType.integer(1, 10))
                                .executes(ctx -> {
                                    ServerPlayer sp = ctx.getSource().getPlayerOrException();
                                    int slot = IntegerArgumentType.getInteger(ctx, "slot") - 1;
                                    QuestPlayerData data = QuestDataStorage.get(sp.getUUID());
                                    QuestEntry q = data.activeSlots.get(slot);
                                    if (q == null || q.completed || !"GATHER_SUBMIT".equals(q.category)) {
                                        sp.sendSystemMessage(Component.literal("§cÔ #" + (slot + 1) + " không phải nhiệm vụ nộp vật phẩm đang mở!"));
                                        return 0;
                                    }
                                    QuestBoardGui.QuestMenu.submitItemsForQuest(sp, data, q);
                                    return 1;
                                })))
                .then(Commands.literal("reload")
                        .requires(src -> src.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
                        .executes(ctx -> {
                            QuestsConfig.load();
                            UnexploredStructureLocator.scanRegistries(ctx.getSource().getServer());
                            ctx.getSource().sendSuccess(() -> Component.literal("§a[Expedition Quests] Đã tải lại quests_config.json & quét lại Registry!"), true);
                            return 1;
                        }));

        dispatcher.register(root);
        dispatcher.register(Commands.literal("q").redirect(dispatcher.getRoot().getChild("quest")));
    }

    public static void shareQuestToChat(ServerPlayer sp, int slotIndex) {
        QuestPlayerData data = QuestDataStorage.get(sp.getUUID());
        QuestEntry q = data.activeSlots.get(slotIndex);
        if (q == null || q.completed) {
            sp.sendSystemMessage(Component.literal("§cÔ nhiệm vụ này đang trống hoặc đã hoàn thành!"));
            return;
        }
        String code = q.questId;
        SHARED_QUESTS.put(code, q);

        Component acceptBtn = Component.literal(" §a§l[NHẬN CHUNG]")
                .setStyle(Style.EMPTY
                        .withClickEvent(new ClickEvent.RunCommand("/quest accept " + code))
                        .withHoverEvent(new HoverEvent.ShowText(Component.literal("§eNhấp để nhận nhiệm vụ " + q.displayName + " (" + q.difficulty + ")"))));

        Component msg = Component.literal("§b§l[Co-op Quest] §e" + sp.getName().getString() + " §7chia sẻ nhiệm vụ " +
                q.getDifficultyTag() + " §f" + q.displayName +
                (q.isExploration() && q.located ? " §7(§b" + q.targetX + ", " + q.targetZ + "§7)" : ""))
                .append(acceptBtn);

        sp.level().getServer().getPlayerList().broadcastSystemMessage(msg, false);
    }

    public static void acceptSharedQuest(ServerPlayer sp, String code) {
        QuestEntry shared = SHARED_QUESTS.get(code);
        if (shared == null) {
            sp.sendSystemMessage(Component.literal("§cLời mời nhiệm vụ này đã hết hạn!"));
            return;
        }
        QuestPlayerData data = QuestDataStorage.get(sp.getUUID());
        if ("minecraft:the_nether".equals(shared.dimension) && data.level < QuestsConfig.getNetherUnlockLevel()) {
            sp.sendSystemMessage(Component.literal("§cBạn cần Adventure Lv." + QuestsConfig.getNetherUnlockLevel() + " để nhận nhiệm vụ Nether!"));
            return;
        }
        if ("minecraft:the_end".equals(shared.dimension) && data.level < QuestsConfig.getEndUnlockLevel()) {
            sp.sendSystemMessage(Component.literal("§cBạn cần Adventure Lv." + QuestsConfig.getEndUnlockLevel() + " để nhận nhiệm vụ The End!"));
            return;
        }

        // Find an empty slot matching the difficulty
        int start = "EASY".equals(shared.difficulty) ? 0 : ("MEDIUM".equals(shared.difficulty) ? 5 : 8);
        int end = "EASY".equals(shared.difficulty) ? 5 : ("MEDIUM".equals(shared.difficulty) ? 8 : 10);
        int freeSlot = -1;
        for (int i = start; i < end; i++) {
            if (!data.activeSlots.containsKey(i)) {
                freeSlot = i;
                break;
            }
        }
        if (freeSlot == -1) {
            sp.sendSystemMessage(Component.literal("§cBạn không còn ô trống nào ở bậc " + shared.difficulty + "!"));
            return;
        }

        QuestEntry copy = shared.copyForSlot(freeSlot);
        data.activeSlots.put(freeSlot, copy);
        data.draftOffers.remove(freeSlot);
        data.dirty = true;
        QuestDataStorage.saveAsync(sp.getUUID());
        sp.sendSystemMessage(Component.literal("§a✔ Đã nhận nhiệm vụ chung: §f" + copy.displayName));
    }
}
