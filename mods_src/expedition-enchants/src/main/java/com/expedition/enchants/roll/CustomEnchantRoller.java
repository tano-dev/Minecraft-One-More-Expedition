package com.expedition.enchants.roll;

import com.expedition.enchants.config.EnchantRollConfig;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.TextColor;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentInstance;

import java.util.*;
import java.util.stream.Stream;

public class CustomEnchantRoller {
    public enum Tier {
        COMMON, RARE, EPIC, ULTIMATE, CURSE
    }

    public static Tier detectTier(Holder<Enchantment> holder) {
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
        EnchantRollConfig cfg = EnchantRollConfig.get();
        boolean isBook = stack.is(Items.BOOK);

        List<Holder<Enchantment>> eligible = new ArrayList<>();
        possibleStream.forEach(h -> {
            if (isBook || h.value().canEnchant(stack)) {
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

        int targetCount = rollEnchantCount(random, tableLevel, cfg);
        double levelScale = Math.min(1.0, Math.max(0.1, tableLevel / 30.0));

        List<EnchantmentInstance> chosen = new ArrayList<>();
        List<Holder<Enchantment>> chosenHolders = new ArrayList<>();

        // Roll rare/epic/ultimate at most once per roll according to configured per-roll chances
        double ultChance = cfg.tierChances.getOrDefault("ultimate", 0.0) * levelScale;
        double epicChance = cfg.tierChances.getOrDefault("epic", 0.01) * levelScale;
        double rareChance = cfg.tierChances.getOrDefault("rare", 0.05) * levelScale;

        for (int slot = 0; slot < targetCount; slot++) {
            Tier targetTier = Tier.COMMON;
            if (slot == 0) {
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

            int lv = rollLevel(random, picked.value().getMaxLevel(), levelScale, cfg);
            chosenHolders.add(picked);
            chosen.add(new EnchantmentInstance(picked, lv));
        }

        return chosen;
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

    private static int rollEnchantCount(RandomSource random, int tableLevel, EnchantRollConfig cfg) {
        if (tableLevel <= 10) return 1;
        Map<String, Integer> weights = cfg.enchantCountWeights;
        int total = 0;
        for (int w : weights.values()) total += Math.max(0, w);
        if (total <= 0) return 1;

        int r = random.nextInt(total);
        int acc = 0;
        int count = 1;
        for (var e : weights.entrySet()) {
            acc += Math.max(0, e.getValue());
            if (r < acc) {
                count = Integer.parseInt(e.getKey());
                break;
            }
        }
        if (tableLevel < 20 && count > 2) count = 2;
        return Math.max(1, count);
    }

    private static int rollLevel(RandomSource random, int maxLevel, double levelScale, EnchantRollConfig cfg) {
        if (maxLevel <= 1) return 1;
        Map<String, Integer> curve = cfg.levelCurves.get(String.valueOf(maxLevel));
        if (curve == null || curve.isEmpty()) {
            // Generic fallback: 50% (max-2), 35% (max-1), 15% (max)
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
