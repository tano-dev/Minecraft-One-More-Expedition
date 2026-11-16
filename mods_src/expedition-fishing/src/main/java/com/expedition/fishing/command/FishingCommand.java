package com.expedition.fishing.command;

import com.expedition.fishing.biome.BiomeResolver;
import com.expedition.fishing.config.FishingDataRegistry;
import com.expedition.fishing.data.FishingPlayerData;
import com.expedition.fishing.data.PlayerDataStorage;
import com.expedition.fishing.gui.FishingGui;
import com.expedition.fishing.item.FishingItemFactory;
import com.expedition.fishing.mechanics.FishingEngine;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.Locale;
import java.util.Map;

public class FishingCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        for (String alias : new String[]{"fishing", "fish", "fs"}) {
            dispatcher.register(buildCommand(alias));
        }
    }

    private static LiteralArgumentBuilder<CommandSourceStack> buildCommand(String root) {
        return Commands.literal(root)
                .executes(ctx -> {
                    ServerPlayer sp = ctx.getSource().getPlayerOrException();
                    FishingGui.open(sp, FishingGui.PageType.MAIN, 0);
                    return 1;
                })
                .then(Commands.literal("help").executes(ctx -> {
                    ctx.getSource().sendSuccess(() -> Component.literal(
                            "§3§l🎣 Expedition Fishing Commands:\n" +
                            "§b/fish §7- Open Main Fishing Hub\n" +
                            "§b/fish encyclopedia §7- Browse 104 Water/Lava/Void Sea Creatures\n" +
                            "§b/fish recipe §7- Browse & craft Rods, Armor, and Baits\n" +
                            "§b/fish bag §7- Manage your 9-slot Artifact Bag\n" +
                            "§b/fish artifacts §7- View discovered Artifacts\n" +
                            "§b/fish chances §7- View current Biome Pool weights\n" +
                            "§b/fish stats §7- View your Fishing Stats & Heat\n" +
                            "§b/fish heat §7- Check current Lava Heat meter"
                    ), false);
                    return 1;
                }))
                .then(Commands.literal("encyclopedia").executes(ctx -> {
                    FishingGui.open(ctx.getSource().getPlayerOrException(), FishingGui.PageType.ENCYCLOPEDIA, 0);
                    return 1;
                }))
                .then(Commands.literal("recipe").executes(ctx -> {
                    FishingGui.open(ctx.getSource().getPlayerOrException(), FishingGui.PageType.RECIPES, 0);
                    return 1;
                }))
                .then(Commands.literal("bag").executes(ctx -> {
                    FishingGui.open(ctx.getSource().getPlayerOrException(), FishingGui.PageType.BAG, 0);
                    return 1;
                }))
                .then(Commands.literal("artifacts").executes(ctx -> {
                    FishingGui.open(ctx.getSource().getPlayerOrException(), FishingGui.PageType.ARTIFACTS, 0);
                    return 1;
                }))
                .then(Commands.literal("chances").executes(ctx -> {
                    FishingGui.open(ctx.getSource().getPlayerOrException(), FishingGui.PageType.CHANCES, 0);
                    return 1;
                }))
                .then(Commands.literal("stats").executes(ctx -> {
                    ServerPlayer sp = ctx.getSource().getPlayerOrException();
                    FishingPlayerData d = PlayerDataStorage.get(sp.getUUID());
                    sp.sendSystemMessage(Component.literal(
                            "§3§l🎣 Fishing Stats (" + sp.getName().getString() + "):\n" +
                            "§7Level: §aLv." + d.level + " §8(" + String.format(Locale.US, "%.1f%%", d.getXpProgressPercent()) + ")\n" +
                            "§7Heat: §6" + Math.round(d.heat) + "/100 🔥\n" +
                            "§7Total Catches: §e" + d.totalCatches + " §8| §7Unique Creatures: §b" + d.mobCatches.size() + "/" + FishingDataRegistry.MOBS.size()
                    ));
                    return 1;
                }))
                .then(Commands.literal("heat").executes(ctx -> {
                    ServerPlayer sp = ctx.getSource().getPlayerOrException();
                    FishingPlayerData d = PlayerDataStorage.get(sp.getUUID());
                    sp.sendSystemMessage(Component.literal("§6🔥 Current Lava Heat: §e" + Math.round(d.heat) + "/100"));
                    return 1;
                }))
                // Admin Subcommands
                .then(Commands.literal("reload")
                        .requires(Commands.hasPermission(Commands.LEVEL_ADMINS))
                        .executes(ctx -> {
                            FishingDataRegistry.loadAll();
                            BiomeResolver.clearCache();
                            ctx.getSource().sendSuccess(() -> Component.literal("§a✔ Reloaded Expedition Fishing configs (Mobs: " +
                                    FishingDataRegistry.MOBS.size() + ", Items: " + FishingDataRegistry.ITEMS.size() + ")!"), true);
                            return 1;
                        }))
                .then(Commands.literal("setlevel")
                        .requires(Commands.hasPermission(Commands.LEVEL_ADMINS))
                        .then(Commands.argument("level", IntegerArgumentType.integer(1, 50))
                                .executes(ctx -> {
                                    ServerPlayer sp = ctx.getSource().getPlayerOrException();
                                    int lv = IntegerArgumentType.getInteger(ctx, "level");
                                    FishingPlayerData d = PlayerDataStorage.get(sp.getUUID());
                                    d.level = lv;
                                    d.xp = 0;
                                    d.dirty = true;
                                    PlayerDataStorage.saveAsync(sp.getUUID());
                                    ctx.getSource().sendSuccess(() -> Component.literal("§aSet Fishing Level to " + lv), true);
                                    return 1;
                                })))
                .then(Commands.literal("addxp")
                        .requires(Commands.hasPermission(Commands.LEVEL_ADMINS))
                        .then(Commands.argument("amount", DoubleArgumentType.doubleArg(1.0))
                                .executes(ctx -> {
                                    ServerPlayer sp = ctx.getSource().getPlayerOrException();
                                    double amt = DoubleArgumentType.getDouble(ctx, "amount");
                                    FishingPlayerData d = PlayerDataStorage.get(sp.getUUID());
                                    int oldLv = d.level;
                                    int gained = d.addXp(amt);
                                    if (gained > 0) FishingEngine.onLevelUp(sp, oldLv, d.level);
                                    PlayerDataStorage.saveAsync(sp.getUUID());
                                    ctx.getSource().sendSuccess(() -> Component.literal("§aAdded " + amt + " Fishing XP (Now Lv." + d.level + ")"), true);
                                    return 1;
                                })))
                .then(Commands.literal("setheat")
                        .requires(Commands.hasPermission(Commands.LEVEL_ADMINS))
                        .then(Commands.argument("heat", DoubleArgumentType.doubleArg(0.0, 100.0))
                                .executes(ctx -> {
                                    ServerPlayer sp = ctx.getSource().getPlayerOrException();
                                    double h = DoubleArgumentType.getDouble(ctx, "heat");
                                    FishingPlayerData d = PlayerDataStorage.get(sp.getUUID());
                                    d.heat = h;
                                    d.dirty = true;
                                    PlayerDataStorage.saveAsync(sp.getUUID());
                                    ctx.getSource().sendSuccess(() -> Component.literal("§6Set Heat to " + Math.round(h) + "/100"), true);
                                    return 1;
                                })))
                .then(Commands.literal("give")
                        .requires(Commands.hasPermission(Commands.LEVEL_ADMINS))
                        .then(Commands.argument("item_id", StringArgumentType.word())
                                .suggests((ctx, builder) -> {
                                    for (String k : FishingDataRegistry.ITEMS.keySet()) builder.suggest(k);
                                    for (String a : FishingDataRegistry.ARTIFACTS.keySet()) builder.suggest(a);
                                    return builder.buildFuture();
                                })
                                .executes(ctx -> {
                                    ServerPlayer sp = ctx.getSource().getPlayerOrException();
                                    String id = StringArgumentType.getString(ctx, "item_id");
                                    ItemStack st = FishingItemFactory.createCustomItem(id, 1);
                                    if (st.isEmpty()) st = FishingItemFactory.createArtifactItem(id);
                                    if (st.isEmpty()) {
                                        ctx.getSource().sendFailure(Component.literal("§cUnknown item or artifact ID: " + id));
                                        return 0;
                                    }
                                    FishingEngine.giveOrDrop(sp, st);
                                    ctx.getSource().sendSuccess(() -> Component.literal("§aGave 1x " + id), true);
                                    return 1;
                                })))
                .then(Commands.literal("sim")
                        .requires(Commands.hasPermission(Commands.LEVEL_ADMINS))
                        .then(Commands.argument("biome", StringArgumentType.word())
                                .suggests((ctx, builder) -> {
                                    for (String b : FishingDataRegistry.BIOMES.keySet()) builder.suggest(b);
                                    return builder.buildFuture();
                                })
                                .then(Commands.argument("iterations", IntegerArgumentType.integer(10, 100000))
                                        .executes(ctx -> {
                                            String b = StringArgumentType.getString(ctx, "biome");
                                            int n = IntegerArgumentType.getInteger(ctx, "iterations");
                                            Map<String, Integer> res = FishingEngine.simulateRolls(b, 50, n);
                                            StringBuilder sb = new StringBuilder("§e📊 Simulated " + n + " catches in §b" + b + "§e:\n");
                                            res.entrySet().stream()
                                                    .sorted((a, c) -> Integer.compare(c.getValue(), a.getValue()))
                                                    .limit(10)
                                                    .forEach(e -> sb.append(" §7- §f").append(e.getKey()).append(": §a")
                                                            .append(String.format(Locale.US, "%.2f%%", e.getValue() * 100.0 / n)).append("\n"));
                                            ctx.getSource().sendSuccess(() -> Component.literal(sb.toString()), false);
                                            return 1;
                                        }))));
    }
}
