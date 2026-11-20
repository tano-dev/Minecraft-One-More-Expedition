package com.expedition.enchants.roll;

import com.expedition.enchants.command.EnchantRigManager;
import com.expedition.enchants.config.EnchantRollConfig;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentInstance;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

public class CustomEnchantRoller {
    public enum Tier {
        COMMON, RARE, EPIC, ULTIMATE, CURSE
    }

    private static final int HISTORY_WINDOW = 15;
    private static final Map<UUID, Deque<Integer>> PLAYER_COUNT_HISTORY = new ConcurrentHashMap<>();
    private static final ThreadLocal<Boolean> LAST_ROLL_GODROLL = ThreadLocal.withInitial(() -> false);

    public static boolean consumeLastRollGodroll() {
        boolean val = Boolean.TRUE.equals(LAST_ROLL_GODROLL.get());
        LAST_ROLL_GODROLL.set(false);
        return val;
    }

    public static Tier detectTier(Holder<Enchantment> holder) {
        if (holder.is(net.minecraft.tags.EnchantmentTags.CURSE)) {
            return Tier.CURSE;
        }

        String id = holder.unwrapKey().map(k -> k.identifier().toString()).orElse("");
        String override = EnchantRollConfig.get().tierOverrides.get(id);
        if (override != null) {
            return switch (override.toLowerCase(Locale.ROOT)) {
                case "rare" -> Tier.RARE;
                case "epic" -> Tier.EPIC;
                case "ultimate" -> Tier.ULTIMATE;
                case "curse" -> Tier.CURSE;
                default -> Tier.COMMON;
            };
        }

        TextColor color = holder.value().description().getStyle().getColor();
        if (color != null) {
            String name = color.serialize();
            return switch (name) {
                case "aqua" -> Tier.RARE;
                case "light_purple" -> Tier.EPIC;
                case "yellow", "gold" -> Tier.ULTIMATE;
                case "red" -> Tier.CURSE;
                default -> Tier.COMMON;
            };
        }
        return Tier.COMMON;
    }

    public static List<EnchantmentInstance> roll(
        RandomSource random,
        ItemStack stack,
        int tableLevel,
        Stream<Holder<Enchantment>> possibleStream
    ) {
        LAST_ROLL_GODROLL.set(false);
        EnchantRollConfig cfg = EnchantRollConfig.get();
        boolean isBook = stack.is(Items.BOOK);

        List<Holder<Enchantment>> eligible = new ArrayList<>();
        possibleStream.forEach(h -> {
            if (isBook || h.value().isPrimaryItem(stack)) {
                eligible.add(h);
            }
        });

        if (eligible.isEmpty()) {
            return List.of();
        }

        Map<Tier, List<Holder<Enchantment>>> byTier = new EnumMap<>(Tier.class);
        for (Tier t : Tier.values()) byTier.put(t, new ArrayList<>());
        for (Holder<Enchantment> h : eligible) {
            byTier.get(detectTier(h)).add(h);
        }

        ServerPlayer player = EnchantRigManager.getCurrentPlayer();
        EnchantRigManager.RigType rig = (player != null) ? EnchantRigManager.consumeRig(player.getUUID()) : EnchantRigManager.RigType.NONE;

        double levelScale = Math.min(1.0, Math.max(0.1, tableLevel / 30.0));
        boolean isGodroll = (rig == EnchantRigManager.RigType.GODROLL)
            || (tableLevel >= 20 && random.nextDouble() < cfg.godrollChance * levelScale);

        LAST_ROLL_GODROLL.set(isGodroll);

        int targetCount;
        if (isGodroll) {
            int minG = Math.max(2, cfg.godrollMinEnchants);
            int maxG = Math.max(minG, cfg.godrollMaxEnchants);
            targetCount = minG + random.nextInt(maxG - minG + 1);
        } else {
            targetCount = rollDynamicEnchantCount(random, tableLevel, cfg, player);
        }

        List<EnchantmentInstance> chosen = new ArrayList<>();
        List<Holder<Enchantment>> chosenHolders = new ArrayList<>();

        double ultChance = cfg.tierChances.getOrDefault("ultimate", 0.0) * levelScale;
        double epicChance = (isGodroll ? cfg.godrollEpicChance : cfg.tierChances.getOrDefault("epic", 0.005)) * levelScale;
        double rareChance = (isGodroll ? cfg.godrollRareChance : cfg.tierChances.getOrDefault("rare", 0.05)) * levelScale;

        for (int slot = 0; slot < targetCount; slot++) {
            Tier targetTier = Tier.COMMON;
            if (slot == 0 && rig == EnchantRigManager.RigType.FORCE_EPIC && !byTier.get(Tier.EPIC).isEmpty()) {
                targetTier = Tier.EPIC;
            } else if (slot == 0 && rig == EnchantRigManager.RigType.FORCE_RARE && !byTier.get(Tier.RARE).isEmpty()) {
                targetTier = Tier.RARE;
            } else if (slot == 0 || isGodroll) {
                double r = random.nextDouble();
                if (r < ultChance && !byTier.get(Tier.ULTIMATE).isEmpty()) {
                    targetTier = Tier.ULTIMATE;
                } else if (r < ultChance + epicChance && !byTier.get(Tier.EPIC).isEmpty()) {
                    targetTier = Tier.EPIC;
                } else if (r < ultChance + epicChance + rareChance && !byTier.get(Tier.RARE).isEmpty()) {
                    targetTier = Tier.RARE;
                }
            }

            Holder<Enchantment> picked = pickCompatible(random, byTier.get(targetTier), chosenHolders);
            if (picked == null && targetTier != Tier.COMMON) {
                picked = pickCompatible(random, byTier.get(Tier.COMMON), chosenHolders);
            }
            if (picked == null) {
                picked = pickCompatible(random, eligible, chosenHolders);
            }
            if (picked == null) {
                break;
            }

            double effectiveScale = isGodroll ? Math.min(1.0, levelScale + 0.15) : levelScale;
            int lv = rollLevel(random, picked.value().getMaxLevel(), effectiveScale, cfg);
            chosenHolders.add(picked);
            chosen.add(new EnchantmentInstance(picked, lv));
        }

        if (player != null && !isGodroll) {
            recordCountHistory(player.getUUID(), chosen.size());
        }

        return chosen;
    }

    private static void recordCountHistory(UUID playerId, int count) {
        Deque<Integer> q = PLAYER_COUNT_HISTORY.computeIfAbsent(playerId, k -> new ArrayDeque<>());
        synchronized (q) {
            q.addLast(count);
            while (q.size() > HISTORY_WINDOW) {
                q.removeFirst();
            }
        }
    }

    private static int rollDynamicEnchantCount(RandomSource random, int tableLevel, EnchantRollConfig cfg, ServerPlayer player) {
        if (tableLevel <= 10) return 2;

        Map<Integer, Double> adjusted = new LinkedHashMap<>();
        double baseTotal = 0;
        for (var e : cfg.enchantCountWeights.entrySet()) {
            int k = Integer.parseInt(e.getKey());
            double w = Math.max(0, e.getValue());
            adjusted.put(k, w);
            baseTotal += w;
        }
        if (baseTotal <= 0) return 3;

        // Adaptive Anti-Streak: adjust weights based on player's recent 15-roll history
        if (player != null) {
            Deque<Integer> q = PLAYER_COUNT_HISTORY.get(player.getUUID());
            if (q != null && !q.isEmpty()) {
                Map<Integer, Integer> seen = new HashMap<>();
                int n;
                synchronized (q) {
                    n = q.size();
                    for (int c : q) seen.merge(c, 1, Integer::sum);
                }
                for (var entry : adjusted.entrySet()) {
                    int k = entry.getKey();
                    double targetRatio = entry.getValue() / baseTotal;
                    double actualRatio = seen.getOrDefault(k, 0) / (double) n;
                    // Dampen if over-represented recently, boost if under-represented
                    double factor = Math.max(0.25, Math.min(2.0, 1.0 + (targetRatio - actualRatio) * 2.0));
                    entry.setValue(entry.getValue() * factor);
                }
            }
        }

        double total = 0;
        for (double w : adjusted.values()) total += w;
        double r = random.nextDouble() * total;
        double acc = 0;
        int count = 3;
        for (var e : adjusted.entrySet()) {
            acc += e.getValue();
            if (r < acc) {
                count = e.getKey();
                break;
            }
        }
        if (tableLevel < 20 && count > 3) count = 3;
        return Math.max(2, count);
    }

    private static Holder<Enchantment> pickCompatible(
        RandomSource random,
        List<Holder<Enchantment>> pool,
        List<Holder<Enchantment>> alreadyChosen
    ) {
        if (pool.isEmpty()) return null;
        List<Holder<Enchantment>> compat = new ArrayList<>();
        for (Holder<Enchantment> candidate : pool) {
            boolean ok = true;
            for (Holder<Enchantment> existing : alreadyChosen) {
                if (!Enchantment.areCompatible(existing, candidate)) {
                    ok = false;
                    break;
                }
            }
            if (ok) compat.add(candidate);
        }
        if (compat.isEmpty()) return null;
        return compat.get(random.nextInt(compat.size()));
    }

    private static int rollLevel(RandomSource random, int maxLevel, double levelScale, EnchantRollConfig cfg) {
        if (maxLevel <= 1) return 1;
        Map<String, Integer> curve = cfg.levelCurves.get(String.valueOf(maxLevel));
        if (curve == null || curve.isEmpty()) {
            int r = random.nextInt(100);
            int lv = (r < 15) ? maxLevel : (r < 50 ? Math.max(1, maxLevel - 1) : Math.max(1, maxLevel - 2));
            return Math.max(1, (int) Math.round(lv * levelScale));
        }

        int total = 0;
        for (int w : curve.values()) total += Math.max(0, w);
        if (total <= 0) return 1;

        int r = random.nextInt(total);
        int acc = 0;
        int pickedLv = 1;
        for (var e : curve.entrySet()) {
            acc += Math.max(0, e.getValue());
            if (r < acc) {
                pickedLv = Integer.parseInt(e.getKey());
                break;
            }
        }
        if (levelScale < 0.85) {
            pickedLv = Math.max(1, (int) Math.floor(pickedLv * levelScale));
        }
        return Math.min(maxLevel, Math.max(1, pickedLv));
    }
}
