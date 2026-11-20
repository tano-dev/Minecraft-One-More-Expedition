package com.expedition.enchants.roll;

import com.expedition.enchants.config.EnchantRollConfig;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import java.util.*;

public class StructureChestBookRoller {

    public static boolean isCurse(Holder<Enchantment> holder) {
        if (holder.is(net.minecraft.tags.EnchantmentTags.CURSE)) return true;
        if (CustomEnchantRoller.detectTier(holder) == CustomEnchantRoller.Tier.CURSE) return true;
        String id = holder.unwrapKey().map(k -> k.identifier().getPath()).orElse("");
        return id.toLowerCase(Locale.ROOT).endsWith("curse");
    }

    private static boolean isAllowedCurseNamespace(String ns) {
        List<String> allowed = EnchantRollConfig.get().chestBookCurseNamespaces;
        if (allowed == null) return false;
        for (String a : allowed) {
            if (ns.equalsIgnoreCase(a)) return true;
        }
        return false;
    }

    public static boolean isExcludedEnchantment(Holder<Enchantment> holder) {
        EnchantRollConfig cfg = EnchantRollConfig.get();
        String id = holder.unwrapKey().map(k -> k.identifier().toString()).orElse("");
        if (id.isEmpty()) return true;

        // Whitelist overrides namespace exclusion (e.g. stellarity:ambush, plated, levitation_shot)
        if (cfg.chestBookWhitelistedEnchants != null) {
            for (String w : cfg.chestBookWhitelistedEnchants) {
                if (id.equalsIgnoreCase(w)) return false;
            }
        }

        int colon = id.indexOf(':');
        String ns = colon > 0 ? id.substring(0, colon) : "minecraft";
        String path = colon > 0 ? id.substring(colon + 1) : id;
        String lowPath = path.toLowerCase(Locale.ROOT);

        // Technical / boss-only helpers are never allowed, even for allowed curse namespaces
        if (lowPath.contains("_technical")
                || lowPath.contains("technical/")
                || lowPath.contains("internal")
                || lowPath.contains("function_only")
                || lowPath.contains("raj_")
                || lowPath.contains("raj.")
                || lowPath.contains("minion")
                || lowPath.contains("boss_")) {
            return true;
        }

        // Curses: only minecraft + enchantencore are allowed
        if (isCurse(holder)) {
            return !isAllowedCurseNamespace(ns);
        }

        if (cfg.chestBookExcludedNamespaces != null) {
            for (String ex : cfg.chestBookExcludedNamespaces) {
                if (ns.equalsIgnoreCase(ex)) return true;
            }
        }

        return false;
    }

    public static boolean shouldReplaceInStructureChest(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;

        // Never replace Mine Treasure's own custom armor/weapons
        CustomData cd = stack.get(DataComponents.CUSTOM_DATA);
        if (cd != null && cd.copyTag().contains("mt")) {
            return false;
        }

        // For existing Enchanted Books: skip normal books, EXCEPT reroll books that rolled an excluded/illegal enchant
        if (stack.is(Items.ENCHANTED_BOOK)) {
            ItemEnchantments stored = stack.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY);
            for (Holder<Enchantment> h : stored.keySet()) {
                if (isExcludedEnchantment(h)) {
                    return true;
                }
            }
            return false;
        }

        // For non-book items: replace any enchanted gear (armor, weapons, tools, bows, rods, elytra, shields)
        ItemEnchantments enchs = stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        if (enchs.isEmpty()) return false;

        return stack.isDamageableItem()
                || stack.has(DataComponents.EQUIPPABLE)
                || stack.has(DataComponents.TOOL)
                || stack.has(DataComponents.WEAPON);
    }

    public record RollResult(ItemStack book, boolean godroll) {}

    public static RollResult rollChestEnchantedBook(ServerLevel level, RandomSource random) {
        EnchantRollConfig cfg = EnchantRollConfig.get();
        HolderLookup.RegistryLookup<Enchantment> lookup = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);

        List<Holder<Enchantment>> commonPool = new ArrayList<>();
        List<Holder<Enchantment>> rarePool = new ArrayList<>();
        List<Holder<Enchantment>> epicPool = new ArrayList<>();
        List<Holder<Enchantment>> cursePool = new ArrayList<>();

        lookup.listElements().forEach(holder -> {
            if (isExcludedEnchantment(holder)) return;
            if (isCurse(holder)) {
                cursePool.add(holder); // already limited to minecraft + enchantencore by isExcludedEnchantment
                return;
            }
            CustomEnchantRoller.Tier tier = CustomEnchantRoller.detectTier(holder);
            switch (tier) {
                case COMMON -> commonPool.add(holder);
                case RARE -> rarePool.add(holder);
                case EPIC -> epicPool.add(holder);
                default -> {} // Exclude ULTIMATE
            }
        });

        if (commonPool.isEmpty() && rarePool.isEmpty() && epicPool.isEmpty() && cursePool.isEmpty()) {
            return new RollResult(ItemStack.EMPTY, false);
        }

        // Roll enchantment count: small Godroll chance -> 6, otherwise weighted 1..5 (30/25/20/15/10)
        boolean isGodroll = random.nextDouble() < cfg.chestBookGodrollChance;
        int count = isGodroll ? 6 : rollWeightedCount(random, cfg);

        ItemStack book = new ItemStack(Items.ENCHANTED_BOOK, 1);
        ItemEnchantments.Mutable mut = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        Set<Holder<Enchantment>> pickedSet = new HashSet<>();

        // Normal: 60% Common / 25% Rare / 5% Epic / 10% Curse. Common is always the remainder.
        double eChance = Math.max(0.0, cfg.chestBookEpicChance) * (isGodroll ? Math.max(1.0, cfg.chestBookGodrollEpicMultiplier) : 1.0);
        double rChance = Math.max(0.0, cfg.chestBookRareChance) * (isGodroll ? Math.max(1.0, cfg.chestBookGodrollRareMultiplier) : 1.0);
        double curseChance = Math.max(0.0, isGodroll ? cfg.chestBookGodrollCurseChance : cfg.chestBookCurseChance);
        double total = eChance + rChance + curseChance;
        if (total > 1.0) { // keep probabilities valid if someone over-configures
            eChance /= total;
            rChance /= total;
            curseChance /= total;
        }

        for (int i = 0; i < count; i++) {
            double roll = random.nextDouble();
            List<Holder<Enchantment>> primary;
            if (roll < curseChance) {
                primary = cursePool;
            } else if (roll < curseChance + eChance) {
                primary = epicPool;
            } else if (roll < curseChance + eChance + rChance) {
                primary = rarePool;
            } else {
                primary = commonPool;
            }

            Holder<Enchantment> chosen = pickUnused(random, primary, pickedSet);
            if (chosen == null) chosen = pickUnused(random, commonPool, pickedSet);
            if (chosen == null) chosen = pickUnused(random, rarePool, pickedSet);
            if (chosen == null) chosen = pickUnused(random, epicPool, pickedSet);
            if (chosen == null) chosen = pickUnused(random, cursePool, pickedSet);
            if (chosen == null) break;

            pickedSet.add(chosen);
            int maxLv = Math.max(1, chosen.value().getMaxLevel());
            int rolledLv = 1 + random.nextInt(maxLv); // Pure random level in [1, max_level]
            mut.set(chosen, rolledLv);
        }

        book.set(DataComponents.STORED_ENCHANTMENTS, mut.toImmutable());
        com.expedition.enchants.tooltip.EnchantTooltipFormatter.applyToItemStack(book);
        return new RollResult(book, isGodroll);
    }

    private static int rollWeightedCount(RandomSource random, EnchantRollConfig cfg) {
        Map<String, Integer> weights = cfg.chestBookCountWeights;
        if (weights == null || weights.isEmpty()) {
            weights = Map.of("1", 20, "2", 20, "3", 30, "4", 20, "5", 10);
        }
        int total = 0;
        for (int w : weights.values()) {
            if (w > 0) total += w;
        }
        if (total <= 0) return 3;
        int r = random.nextInt(total);
        int acc = 0;
        for (Map.Entry<String, Integer> entry : weights.entrySet()) {
            int w = Math.max(0, entry.getValue());
            acc += w;
            if (r < acc) {
                try {
                    return Math.max(1, Integer.parseInt(entry.getKey()));
                } catch (NumberFormatException ignored) {
                    return 3;
                }
            }
        }
        return 3;
    }

    private static Holder<Enchantment> pickUnused(RandomSource random, List<Holder<Enchantment>> pool, Set<Holder<Enchantment>> used) {
        if (pool == null || pool.isEmpty()) return null;
        List<Holder<Enchantment>> candidates = new ArrayList<>();
        for (Holder<Enchantment> h : pool) {
            if (!used.contains(h)) candidates.add(h);
        }
        if (candidates.isEmpty()) return null;
        return candidates.get(random.nextInt(candidates.size()));
    }
}
