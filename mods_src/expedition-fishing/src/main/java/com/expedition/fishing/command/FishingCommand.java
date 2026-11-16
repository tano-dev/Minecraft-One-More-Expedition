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
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.Locale;
import java.util.Map;

public class FishingCommand {

    private static final SuggestionProvider<CommandSourceStack> SUGGEST_ITEMS_AND_ARTIFACTS = (ctx, builder) -> {
        String rem = builder.getRemaining().toLowerCase(Locale.ROOT);
        for (String k : FishingDataRegistry.ITEMS.keySet()) {
            if (k.toLowerCase(Locale.ROOT).startsWith(rem)) builder.suggest(k);
        }
        for (String a : FishingDataRegistry.ARTIFACTS.keySet()) {
            if (a.toLowerCase(Locale.ROOT).startsWith(rem)) builder.suggest(a);
        }
        return builder.buildFuture();
    };

    private static final SuggestionProvider<CommandSourceStack> SUGGEST_MOBS = (ctx, builder) -> {
        String rem = builder.getRemaining().toLowerCase(Locale.ROOT);
        for (String m : FishingDataRegistry.MOBS.keySet()) {
            if (m.toLowerCase(Locale.ROOT).startsWith(rem)) builder.suggest(m);
        }
        return builder.buildFuture();
    };

    private static final SuggestionProvider<CommandSourceStack> SUGGEST_BIOMES = (ctx, builder) -> {
        String rem = builder.getRemaining().toLowerCase(Locale.ROOT);
        for (String b : FishingDataRegistry.BIOMES.keySet()) {
            if (b.toLowerCase(Locale.ROOT).startsWith(rem)) builder.suggest(b);
        }
        return builder.buildFuture();
    };

    private static final SuggestionProvider<CommandSourceStack> SUGGEST_FULFILL = (ctx, builder) -> {
        builder.suggest("all");
        builder.suggest("creatures");
        builder.suggest("artifacts");
        return builder.buildFuture();
    };

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        for (String alias : new String[]{"fishing", "fish", "fs", "efish"}) {
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
                // Player Commands
                .then(Commands.literal("help").executes(ctx -> {
                    ctx.getSource().sendSuccess(() -> Component.literal(
                            "§3§l🎣 Expedition Fishing — Commands:\n" +
                            "§b/fish §7- Open Main Fishing Hub GUI\n" +
                            "§b/fish encyclopedia §7- Browse all 104 Water/Lava/End-Void Sea Creatures\n" +
                            "§b/fish recipe §7- Browse & craft Rods, Armor Sets, Weapons, and Baits\n" +
                            "§b/fish bag §7- Open your 9-slot Equipped Artifact Bag\n" +
                            "§b/fish artifacts §7- View discovered Artifacts collection\n" +
                            "§b/fish chances §7- View active Biome Pool & spawn weights\n" +
                            "§b/fish stats [player] §7- View detailed Fishing Level, XP & catch stats\n" +
                            "§b/fish heat §7- View your current Lava Heat meter (0-100)\n" +
                            "§8--- Admin Commands ---\n" +
                            "§e/fish give [player] <item|artifact> [count]\n" +
                            "§e/fish spawn <mob_id> [player]\n" +
                            "§e/fish setlevel [player] <1-50>\n" +
                            "§e/fish addxp [player] <amount>\n" +
                            "§e/fish setheat [player] <0-100>\n" +
                            "§e/fish fulfill [player] <all|creatures|artifacts>\n" +
                            "§e/fish reset [player]\n" +
                            "§e/fish sim [biome] [iterations]\n" +
                            "§e/fish reload"
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
                .then(Commands.literal("recipes").executes(ctx -> {
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
                .then(Commands.literal("stats")
                        .executes(ctx -> showStats(ctx.getSource(), ctx.getSource().getPlayerOrException()))
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(ctx -> showStats(ctx.getSource(), EntityArgument.getPlayer(ctx, "target")))))
                .then(Commands.literal("heat").executes(ctx -> {
                    ServerPlayer sp = ctx.getSource().getPlayerOrException();
                    FishingPlayerData d = PlayerDataStorage.get(sp.getUUID());
                    sp.sendSystemMessage(Component.literal("§6🔥 Current Lava Heat: §e" + Math.round(d.heat) + "/100"));
                    return 1;
                }))

                // Admin Commands (support both self & target player arguments)
                .then(Commands.literal("reload")
                        .requires(Commands.hasPermission(Commands.LEVEL_ADMINS))
                        .executes(ctx -> {
                            FishingDataRegistry.loadAll();
                            BiomeResolver.clearCache();
                            ctx.getSource().sendSuccess(() -> Component.literal("§a✔ Reloaded Expedition Fishing configs (Mobs: " +
                                    FishingDataRegistry.MOBS.size() + ", Items: " + FishingDataRegistry.ITEMS.size() +
                                    ", Artifacts: " + FishingDataRegistry.ARTIFACTS.size() + ")!"), true);
                            return 1;
                        }))

                // /fish setlevel <level> OR /fish setlevel <player> <level>
                .then(Commands.literal("setlevel")
                        .requires(Commands.hasPermission(Commands.LEVEL_ADMINS))
                        .then(Commands.argument("level", IntegerArgumentType.integer(1, 50))
                                .executes(ctx -> executeSetLevel(ctx.getSource(), ctx.getSource().getPlayerOrException(), IntegerArgumentType.getInteger(ctx, "level"))))
                        .then(Commands.argument("target", EntityArgument.player())
                                .then(Commands.argument("level", IntegerArgumentType.integer(1, 50))
                                        .executes(ctx -> executeSetLevel(ctx.getSource(), EntityArgument.getPlayer(ctx, "target"), IntegerArgumentType.getInteger(ctx, "level"))))))

                // /fish addxp <amount> OR /fish addxp <player> <amount>
                .then(Commands.literal("addxp")
                        .requires(Commands.hasPermission(Commands.LEVEL_ADMINS))
                        .then(Commands.argument("amount", DoubleArgumentType.doubleArg(1.0))
                                .executes(ctx -> executeAddXp(ctx.getSource(), ctx.getSource().getPlayerOrException(), DoubleArgumentType.getDouble(ctx, "amount"))))
                        .then(Commands.argument("target", EntityArgument.player())
                                .then(Commands.argument("amount", DoubleArgumentType.doubleArg(1.0))
                                        .executes(ctx -> executeAddXp(ctx.getSource(), EntityArgument.getPlayer(ctx, "target"), DoubleArgumentType.getDouble(ctx, "amount"))))))

                // /fish setheat <heat> OR /fish setheat <player> <heat>
                .then(Commands.literal("setheat")
                        .requires(Commands.hasPermission(Commands.LEVEL_ADMINS))
                        .then(Commands.argument("heat", DoubleArgumentType.doubleArg(0.0, 100.0))
                                .executes(ctx -> executeSetHeat(ctx.getSource(), ctx.getSource().getPlayerOrException(), DoubleArgumentType.getDouble(ctx, "heat"))))
                        .then(Commands.argument("target", EntityArgument.player())
                                .then(Commands.argument("heat", DoubleArgumentType.doubleArg(0.0, 100.0))
                                        .executes(ctx -> executeSetHeat(ctx.getSource(), EntityArgument.getPlayer(ctx, "target"), DoubleArgumentType.getDouble(ctx, "heat"))))))

                // /fish give <item_id> [count] OR /fish give <player> <item_id> [count]
                .then(Commands.literal("give")
                        .requires(Commands.hasPermission(Commands.LEVEL_ADMINS))
                        .then(Commands.argument("item_id", StringArgumentType.word())
                                .suggests(SUGGEST_ITEMS_AND_ARTIFACTS)
                                .executes(ctx -> executeGive(ctx.getSource(), ctx.getSource().getPlayerOrException(), StringArgumentType.getString(ctx, "item_id"), 1))
                                .then(Commands.argument("count", IntegerArgumentType.integer(1, 64))
                                        .executes(ctx -> executeGive(ctx.getSource(), ctx.getSource().getPlayerOrException(), StringArgumentType.getString(ctx, "item_id"), IntegerArgumentType.getInteger(ctx, "count")))))
                        .then(Commands.argument("target", EntityArgument.player())
                                .then(Commands.argument("item_id", StringArgumentType.word())
                                        .suggests(SUGGEST_ITEMS_AND_ARTIFACTS)
                                        .executes(ctx -> executeGive(ctx.getSource(), EntityArgument.getPlayer(ctx, "target"), StringArgumentType.getString(ctx, "item_id"), 1))
                                        .then(Commands.argument("count", IntegerArgumentType.integer(1, 64))
                                                .executes(ctx -> executeGive(ctx.getSource(), EntityArgument.getPlayer(ctx, "target"), StringArgumentType.getString(ctx, "item_id"), IntegerArgumentType.getInteger(ctx, "count")))))))

                // /fish spawn <mob_id> [player]
                .then(Commands.literal("spawn")
                        .requires(Commands.hasPermission(Commands.LEVEL_ADMINS))
                        .then(Commands.argument("mob_id", StringArgumentType.word())
                                .suggests(SUGGEST_MOBS)
                                .executes(ctx -> executeSpawn(ctx.getSource(), ctx.getSource().getPlayerOrException(), StringArgumentType.getString(ctx, "mob_id")))
                                .then(Commands.argument("target", EntityArgument.player())
                                        .executes(ctx -> executeSpawn(ctx.getSource(), EntityArgument.getPlayer(ctx, "target"), StringArgumentType.getString(ctx, "mob_id"))))))

                // /fish fulfill <all|creatures|artifacts> [player] OR /fish fulfill <player> <all|creatures|artifacts>
                .then(Commands.literal("fulfill")
                        .requires(Commands.hasPermission(Commands.LEVEL_ADMINS))
                        .then(Commands.argument("mode", StringArgumentType.word())
                                .suggests(SUGGEST_FULFILL)
                                .executes(ctx -> executeFulfill(ctx.getSource(), ctx.getSource().getPlayerOrException(), StringArgumentType.getString(ctx, "mode"))))
                        .then(Commands.argument("target", EntityArgument.player())
                                .then(Commands.argument("mode", StringArgumentType.word())
                                        .suggests(SUGGEST_FULFILL)
                                        .executes(ctx -> executeFulfill(ctx.getSource(), EntityArgument.getPlayer(ctx, "target"), StringArgumentType.getString(ctx, "mode"))))))

                // /fish reset [player]
                .then(Commands.literal("reset")
                        .requires(Commands.hasPermission(Commands.LEVEL_ADMINS))
                        .executes(ctx -> executeReset(ctx.getSource(), ctx.getSource().getPlayerOrException()))
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(ctx -> executeReset(ctx.getSource(), EntityArgument.getPlayer(ctx, "target")))))

                // /fish sim [biome] [iterations]
                .then(Commands.literal("sim")
                        .requires(Commands.hasPermission(Commands.LEVEL_ADMINS))
                        .executes(ctx -> {
                            ServerPlayer sp = ctx.getSource().getPlayerOrException();
                            String b = BiomeResolver.resolveBiomeGroup(sp.level(), sp.blockPosition(), false, false);
                            return executeSim(ctx.getSource(), b, 1000);
                        })
                        .then(Commands.argument("biome", StringArgumentType.word())
                                .suggests(SUGGEST_BIOMES)
                                .executes(ctx -> executeSim(ctx.getSource(), StringArgumentType.getString(ctx, "biome"), 1000))
                                .then(Commands.argument("iterations", IntegerArgumentType.integer(10, 100000))
                                        .executes(ctx -> executeSim(ctx.getSource(), StringArgumentType.getString(ctx, "biome"), IntegerArgumentType.getInteger(ctx, "iterations"))))));
    }

    private static int showStats(CommandSourceStack src, ServerPlayer target) {
        FishingPlayerData d = PlayerDataStorage.get(target.getUUID());
        long curXp = Math.round(d.xp);
        long reqXp = d.level >= 50 ? 0L : Math.round(FishingPlayerData.getXpForNextLevel(d.level));
        String xpDetail = d.level >= 50 ? "MAX" : String.format(Locale.US, "%.1f%% (%d/%d XP)", d.getXpProgressPercent(), curXp, reqXp);
        src.sendSuccess(() -> Component.literal(
                "§3§l🎣 Fishing Stats (" + target.getName().getString() + "):\n" +
                "§7Level: §aLv." + d.level + " §e" + xpDetail + "\n" +
                "§7Heat: §6" + Math.round(d.heat) + "/100 🔥\n" +
                "§7Total Catches: §e" + d.totalCatches + " §8| §7Sea Creatures: §b" + d.mobCatches.size() + "/" + FishingDataRegistry.MOBS.size() + "\n" +
                "§7Artifacts Found: §d" + d.discoveredArtifacts.size() + "/" + FishingDataRegistry.ARTIFACTS.size()
        ), false);
        return 1;
    }

    private static int executeSetLevel(CommandSourceStack src, ServerPlayer target, int lv) {
        FishingPlayerData d = PlayerDataStorage.get(target.getUUID());
        d.level = lv;
        d.xp = 0;
        d.dirty = true;
        PlayerDataStorage.saveAsync(target.getUUID());
        src.sendSuccess(() -> Component.literal("§a✔ Set " + target.getName().getString() + "'s Fishing Level to Lv." + lv), true);
        return 1;
    }

    private static int executeAddXp(CommandSourceStack src, ServerPlayer target, double amt) {
        FishingPlayerData d = PlayerDataStorage.get(target.getUUID());
        int oldLv = d.level;
        int gained = d.addXp(amt);
        if (gained > 0) FishingEngine.onLevelUp(target, oldLv, d.level);
        PlayerDataStorage.saveAsync(target.getUUID());
        src.sendSuccess(() -> Component.literal("§a✔ Added " + Math.round(amt) + " Fishing XP to " + target.getName().getString() + " (Now Lv." + d.level + ")"), true);
        return 1;
    }

    private static int executeSetHeat(CommandSourceStack src, ServerPlayer target, double heat) {
        FishingPlayerData d = PlayerDataStorage.get(target.getUUID());
        d.heat = heat;
        d.dirty = true;
        PlayerDataStorage.saveAsync(target.getUUID());
        src.sendSuccess(() -> Component.literal("§6✔ Set " + target.getName().getString() + "'s Lava Heat to " + Math.round(heat) + "/100"), true);
        return 1;
    }

    private static int executeGive(CommandSourceStack src, ServerPlayer target, String id, int count) {
        ItemStack st = FishingItemFactory.createCustomItem(id, count);
        if (st.isEmpty()) {
            st = FishingItemFactory.createArtifactItem(id);
            if (!st.isEmpty()) {
                st.setCount(count);
                FishingPlayerData d = PlayerDataStorage.get(target.getUUID());
                d.discoveredArtifacts.add(id);
                d.dirty = true;
                PlayerDataStorage.saveAsync(target.getUUID());
            }
        }
        if (st.isEmpty()) {
            src.sendFailure(Component.literal("§cUnknown item or artifact ID: " + id));
            return 0;
        }
        final Component hover = st.getHoverName();
        FishingEngine.giveOrDrop(target, st);
        src.sendSuccess(() -> Component.literal("§a✔ Gave " + count + "x ").append(hover).append(Component.literal(" §ato " + target.getName().getString())), true);
        return 1;
    }

    private static int executeSpawn(CommandSourceStack src, ServerPlayer target, String mobId) {
        if (!FishingEngine.spawnMobAtPlayer(target, mobId)) {
            src.sendFailure(Component.literal("§cUnknown Sea Creature ID: " + mobId));
            return 0;
        }
        src.sendSuccess(() -> Component.literal("§a✔ Spawned Sea Creature '" + mobId + "' at " + target.getName().getString()), true);
        return 1;
    }

    private static int executeFulfill(CommandSourceStack src, ServerPlayer target, String mode) {
        FishingPlayerData d = PlayerDataStorage.get(target.getUUID());
        String m = mode.toLowerCase(Locale.ROOT);
        if ("all".equals(m) || "creatures".equals(m)) {
            for (String mobId : FishingDataRegistry.MOBS.keySet()) {
                d.mobCatches.putIfAbsent(mobId, 1);
                d.bestWeights.putIfAbsent(mobId, 10.0);
            }
        }
        if ("all".equals(m) || "artifacts".equals(m)) {
            d.discoveredArtifacts.addAll(FishingDataRegistry.ARTIFACTS.keySet());
        }
        d.dirty = true;
        PlayerDataStorage.saveAsync(target.getUUID());
        src.sendSuccess(() -> Component.literal("§a✔ Unlocked " + m + " collection entries for " + target.getName().getString()), true);
        return 1;
    }

    private static int executeReset(CommandSourceStack src, ServerPlayer target) {
        FishingPlayerData d = PlayerDataStorage.get(target.getUUID());
        d.level = 1;
        d.xp = 0.0;
        d.heat = 0.0;
        d.totalCatches = 0;
        d.seaCreaturesCaught = 0;
        d.treasuresCaught = 0;
        d.mobCatches.clear();
        d.bestWeights.clear();
        d.discoveredArtifacts.clear();
        d.artifactBag.clear();
        d.dirty = true;
        PlayerDataStorage.saveAsync(target.getUUID());
        src.sendSuccess(() -> Component.literal("§e✔ Reset all Fishing progress for " + target.getName().getString()), true);
        return 1;
    }

    private static int executeSim(CommandSourceStack src, String biome, int n) {
        Map<String, Integer> res = FishingEngine.simulateRolls(biome, 50, n);
        StringBuilder sb = new StringBuilder("§e📊 Simulated " + n + " catches in §b" + biome + "§e:\n");
        res.entrySet().stream()
                .sorted((a, c) -> Integer.compare(c.getValue(), a.getValue()))
                .limit(12)
                .forEach(e -> sb.append(" §7- §f").append(e.getKey()).append(": §a")
                        .append(String.format(Locale.US, "%.2f%%", e.getValue() * 100.0 / n)).append(" §8(").append(e.getValue()).append(")\n"));
        src.sendSuccess(() -> Component.literal(sb.toString()), false);
        return 1;
    }
}
