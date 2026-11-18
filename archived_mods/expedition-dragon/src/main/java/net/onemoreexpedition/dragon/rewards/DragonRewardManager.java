package net.onemoreexpedition.dragon.rewards;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.onemoreexpedition.dragon.config.DragonConfig;
import net.onemoreexpedition.dragon.core.FightState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public final class DragonRewardManager {
    public static final String OVERFLOW_ORDERS_NOTICE = "\u00a7eInventory full! Overflow items were sent to /orders.";
    private static final Logger LOGGER = LoggerFactory.getLogger("expedition-dragon");

    private DragonRewardManager() {
    }

    public static int computeBonusRolls(double damageShare, int maxDamageBonusRolls) {
        if (!Double.isFinite(damageShare) || damageShare <= 0.0D || maxDamageBonusRolls <= 0) {
            return 0;
        }
        double clamped = Math.min(1.0D, Math.max(0.0D, damageShare));
        return (int) Math.round(clamped * maxDamageBonusRolls);
    }

    public static int computeTotalRolls(double damageShare, int baseRolls, int maxDamageBonusRolls) {
        int base = Math.max(0, baseRolls);
        return base + computeBonusRolls(damageShare, maxDamageBonusRolls);
    }

    public static long computeCoinsReward(double damageShare, int crystalBreaks, DragonConfig.RewardConfig cfg) {
        if (cfg == null) {
            return 0L;
        }
        int bonusRolls = computeBonusRolls(damageShare, cfg.maxDamageBonusRolls);
        int breaks = Math.max(0, crystalBreaks);
        long base = Math.max(0L, cfg.guaranteedCoins);
        long perRoll = Math.max(0L, cfg.bonusCoinsPerRoll);
        return base + ((long) (bonusRolls + breaks) * perRoll);
    }

    public static void distributeFightRewards(FightState state, ServerLevel level) {
        if (state == null || level == null) {
            return;
        }
        MinecraftServer server = level.getServer();
        DragonConfig.RewardConfig cfg = DragonConfig.get().rewards;
        double totalDamage = state.getTotalDamage();
        Random rng = new Random();

        for (Map.Entry<UUID, Double> entry : state.getPlayerDamageSnapshot().entrySet()) {
            UUID playerUuid = entry.getKey();
            double dmg = entry.getValue() != null ? entry.getValue() : 0.0D;
            if (!state.isPlayerQualified(playerUuid, cfg.minDamagePercent, cfg.minDamageFlat)) {
                continue;
            }
            double share = totalDamage > 0.0D ? (dmg / totalDamage) : 0.0D;
            int crystalBreaks = state.getCrystalBreaks(playerUuid);
            int totalRolls = computeTotalRolls(share, cfg.baseRolls, cfg.maxDamageBonusRolls);
            long coins = computeCoinsReward(share, crystalBreaks, cfg);

            List<ItemStack> rolledItems = rollItemsForPlayer(state, cfg, totalRolls, rng);
            ServerPlayer onlinePlayer = server.getPlayerList().getPlayer(playerUuid);

            boolean anyOverflowToOrders = false;
            List<String> summaryLines = new ArrayList<>();

            for (ItemStack stack : rolledItems) {
                if (stack == null || stack.isEmpty()) {
                    continue;
                }
                int count = stack.getCount();
                String displayName = stack.getHoverName().getString();
                summaryLines.add("\u00a77- \u00a7f" + count + "x \u00a7d" + displayName);

                if (onlinePlayer != null) {
                    ItemStack toInsert = stack.copy();
                    onlinePlayer.getInventory().add(toInsert);
                    if (!toInsert.isEmpty()) {
                        boolean delivered = routeOverflowToOrders(server, playerUuid, toInsert.copy());
                        if (delivered) {
                            anyOverflowToOrders = true;
                        } else {
                            ItemEntity drop = new ItemEntity(
                                    level,
                                    onlinePlayer.getX(),
                                    onlinePlayer.getY() + 0.5D,
                                    onlinePlayer.getZ(),
                                    toInsert.copy()
                            );
                            level.addFreshEntity(drop);
                        }
                    }
                } else {
                    routeOverflowToOrders(server, playerUuid, stack.copy());
                }
            }

            if (coins > 0L) {
                depositEconomyCoins(server, playerUuid, coins);
            }

            if (onlinePlayer != null) {
                sendPrivateRewardSummary(
                        onlinePlayer,
                        state.getVariant().defaultBossBarTitle(),
                        dmg,
                        share * 100.0D,
                        crystalBreaks,
                        coins,
                        summaryLines,
                        anyOverflowToOrders
                );
            }
        }
    }

    public static List<ItemStack> rollItemsForPlayer(
            FightState state,
            DragonConfig.RewardConfig cfg,
            int totalRolls,
            Random rng
    ) {
        List<ItemStack> drops = new ArrayList<>();
        if (state == null || cfg == null) {
            return drops;
        }
        Random random = rng != null ? rng : new Random();
        int rolls = Math.max(1, totalRolls);

        List<DragonConfig.RewardItemEntry> baseEntries = cfg.baseLoot != null ? cfg.baseLoot : List.of();
        for (int r = 0; r < rolls; r++) {
            for (DragonConfig.RewardItemEntry entry : baseEntries) {
                ItemStack rolled = tryRollEntry(entry, random);
                if (!rolled.isEmpty()) {
                    mergeOrAppendStack(drops, rolled);
                }
            }
        }

        List<DragonConfig.RewardItemEntry> variantEntries = cfg.variantBonusLoot != null
                ? cfg.variantBonusLoot.getOrDefault(state.getVariant().id(), List.of())
                : List.of();
        for (DragonConfig.RewardItemEntry entry : variantEntries) {
            ItemStack rolled = tryRollEntry(entry, random);
            if (!rolled.isEmpty()) {
                mergeOrAppendStack(drops, rolled);
            }
        }
        return drops;
    }

    private static ItemStack tryRollEntry(DragonConfig.RewardItemEntry entry, Random random) {
        if (entry == null || entry.itemId == null || entry.itemId.isBlank()) {
            return ItemStack.EMPTY;
        }
        if (entry.chance <= 0.0D || (entry.chance < 1.0D && random.nextDouble() > entry.chance)) {
            return ItemStack.EMPTY;
        }
        int min = Math.max(1, entry.minCount);
        int max = Math.max(min, entry.maxCount);
        int count = min == max ? min : (min + random.nextInt(max - min + 1));
        try {
            Identifier id = Identifier.parse(entry.itemId);
            Item item = BuiltInRegistries.ITEM.getValue(id);
            if (item == null || item == Items.AIR) {
                return ItemStack.EMPTY;
            }
            return new ItemStack(item, count);
        } catch (Exception e) {
            return ItemStack.EMPTY;
        }
    }

    private static void mergeOrAppendStack(List<ItemStack> list, ItemStack incoming) {
        if (incoming == null || incoming.isEmpty()) {
            return;
        }
        for (ItemStack existing : list) {
            if (ItemStack.isSameItemSameComponents(existing, incoming)) {
                int space = existing.getMaxStackSize() - existing.getCount();
                if (space > 0) {
                    int add = Math.min(space, incoming.getCount());
                    existing.grow(add);
                    incoming.shrink(add);
                    if (incoming.isEmpty()) {
                        return;
                    }
                }
            }
        }
        while (!incoming.isEmpty()) {
            int take = Math.min(incoming.getMaxStackSize(), incoming.getCount());
            ItemStack split = incoming.copyWithCount(take);
            list.add(split);
            incoming.shrink(take);
        }
    }

    /**
     * Routes an overflow ItemStack to EconomyCraft's DeliveryManager (/orders claim inbox):
     * EconomyCraft.getManager(server).getDeliveries().addDelivery(uuid, stack, true)
     */
    public static boolean routeOverflowToOrders(MinecraftServer server, UUID playerUuid, ItemStack stack) {
        if (server == null || playerUuid == null || stack == null || stack.isEmpty()) {
            return false;
        }
        try {
            Class<?> ecoClass = Class.forName("com.reazip.economycraft.EconomyCraft");
            Method getManager = ecoClass.getMethod("getManager", MinecraftServer.class);
            Object manager = getManager.invoke(null, server);
            if (manager == null) {
                return false;
            }
            Method getDeliveries = manager.getClass().getMethod("getDeliveries");
            Object deliveryManager = getDeliveries.invoke(manager);
            if (deliveryManager == null) {
                return false;
            }
            Method addDelivery = deliveryManager.getClass().getMethod("addDelivery", UUID.class, ItemStack.class, boolean.class);
            addDelivery.invoke(deliveryManager, playerUuid, stack, true);
            return true;
        } catch (ClassNotFoundException ignored) {
            return false;
        } catch (Exception e) {
            LOGGER.warn("Failed to route overflow item to EconomyCraft /orders for {}", playerUuid, e);
            return false;
        }
    }

    public static boolean depositEconomyCoins(MinecraftServer server, UUID playerUuid, long amount) {
        if (server == null || playerUuid == null || amount <= 0L) {
            return false;
        }
        try {
            Class<?> apiClass = Class.forName("com.reazip.economycraft.api.v1.EconomyCraftApi");
            Method getApi = apiClass.getMethod("get", MinecraftServer.class);
            Object api = getApi.invoke(null, server);
            if (api != null) {
                Method balances = api.getClass().getMethod("balances");
                Object balanceApi = balances.invoke(api);
                if (balanceApi != null) {
                    Method addMoney = balanceApi.getClass().getMethod("addMoney", UUID.class, long.class);
                    addMoney.invoke(balanceApi, playerUuid, amount);
                    return true;
                }
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    private static void sendPrivateRewardSummary(
            ServerPlayer player,
            String dragonTitle,
            double damageDealt,
            double sharePct,
            int crystalBreaks,
            long coins,
            List<String> itemLines,
            boolean anyOverflowToOrders
    ) {
        player.sendSystemMessage(Component.literal("\u00a75\u00a7m----------------------------------------"));
        player.sendSystemMessage(Component.literal("\u00a7d\u00a7l\u262c DRAGON DOWN! \u00a7r" + dragonTitle));
        player.sendSystemMessage(Component.literal(String.format(
                "\u00a77Your Damage: \u00a7c%.1f \u00a78(\u00a7e%.1f%%\u00a78) \u00a77| Crystals Broken: \u00a7b%d",
                damageDealt, sharePct, crystalBreaks
        )));
        if (coins > 0L) {
            player.sendSystemMessage(Component.literal("\u00a77Coins Earned: \u00a76+" + coins + " Coins"));
        }
        if (!itemLines.isEmpty()) {
            player.sendSystemMessage(Component.literal("\u00a7dRewards Received:"));
            for (String line : itemLines) {
                player.sendSystemMessage(Component.literal("  " + line));
            }
        }
        if (anyOverflowToOrders) {
            player.sendSystemMessage(Component.literal(OVERFLOW_ORDERS_NOTICE));
        }
        player.sendSystemMessage(Component.literal("\u00a75\u00a7m----------------------------------------"));
    }
}
