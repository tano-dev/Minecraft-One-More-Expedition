package com.expedition.enchants.tooltip;

import com.expedition.enchants.config.EnchantRollConfig;
import net.minecraft.core.Holder;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class EnchantTargetDetector {

    public enum TargetType {
        ALL,
        ARMOR,
        HELMET,
        CHESTPLATE,
        LEGGINGS,
        BOOTS,
        MELEE,
        SWORD,
        AXE,
        TOOLS,
        MINING,
        BOW,
        CROSSBOW,
        RANGED,
        TRIDENT,
        MACE,
        FISHING_ROD,
        ELYTRA,
        CURSE,
        UNKNOWN
    }

    private static final Map<Holder<Enchantment>, TargetType> CACHE = new ConcurrentHashMap<>();

    public static TargetType detect(Holder<Enchantment> holder) {
        if (holder == null) return TargetType.UNKNOWN;
        return CACHE.computeIfAbsent(holder, EnchantTargetDetector::compute);
    }

    private static TargetType compute(Holder<Enchantment> holder) {
        Enchantment ench = holder.value();

        boolean helmet = ench.isSupportedItem(new ItemStack(Items.DIAMOND_HELMET));
        boolean chest = ench.isSupportedItem(new ItemStack(Items.DIAMOND_CHESTPLATE));
        boolean legs = ench.isSupportedItem(new ItemStack(Items.DIAMOND_LEGGINGS));
        boolean boots = ench.isSupportedItem(new ItemStack(Items.DIAMOND_BOOTS));

        boolean sword = ench.isSupportedItem(new ItemStack(Items.DIAMOND_SWORD));
        boolean axe = ench.isSupportedItem(new ItemStack(Items.DIAMOND_AXE));
        boolean bow = ench.isSupportedItem(new ItemStack(Items.BOW));
        boolean crossbow = ench.isSupportedItem(new ItemStack(Items.CROSSBOW));
        boolean trident = ench.isSupportedItem(new ItemStack(Items.TRIDENT));
        boolean mace = ench.isSupportedItem(new ItemStack(Items.MACE));

        boolean pickaxe = ench.isSupportedItem(new ItemStack(Items.DIAMOND_PICKAXE));
        boolean shovel = ench.isSupportedItem(new ItemStack(Items.DIAMOND_SHOVEL));
        boolean hoe = ench.isSupportedItem(new ItemStack(Items.DIAMOND_HOE));

        boolean fishing = ench.isSupportedItem(new ItemStack(Items.FISHING_ROD));
        boolean elytra = ench.isSupportedItem(new ItemStack(Items.ELYTRA));

        boolean allArmor = helmet && chest && legs && boots;
        boolean hasArmor = helmet || chest || legs || boots;
        boolean hasWeapons = sword || axe || bow || crossbow || trident || mace;
        boolean hasMining = pickaxe || shovel || hoe;

        // Universal (Unbreaking, Mending, Curse of Vanishing...)
        if (allArmor && (hasWeapons || hasMining)) {
            return TargetType.ALL;
        }

        // Armor
        if (allArmor && !hasWeapons && !hasMining) return TargetType.ARMOR;
        if (helmet && !chest && !legs && !boots) return TargetType.HELMET;
        if (chest && !helmet && !legs && !boots) return TargetType.CHESTPLATE;
        if (legs && !helmet && !chest && !boots) return TargetType.LEGGINGS;
        if (boots && !helmet && !chest && !legs) return TargetType.BOOTS;
        if (hasArmor && !hasWeapons && !hasMining) return TargetType.ARMOR;

        // Weapons
        if (sword && axe && !bow && !crossbow && !trident && !mace && !hasMining) return TargetType.MELEE;
        if (sword && !axe && !bow && !crossbow && !trident && !mace && !hasMining) return TargetType.SWORD;
        if (axe && !sword && !bow && !crossbow && !trident && !mace && !hasMining) return TargetType.AXE;
        if (bow && crossbow) return TargetType.RANGED;
        if (bow) return TargetType.BOW;
        if (crossbow) return TargetType.CROSSBOW;
        if (trident) return TargetType.TRIDENT;
        if (mace) return TargetType.MACE;

        // Tools
        if (hasMining && axe) return TargetType.TOOLS; // Efficiency, Fortune, Silk Touch
        if (hasMining) return TargetType.MINING;

        // Misc
        if (fishing) return TargetType.FISHING_ROD;
        if (elytra) return TargetType.ELYTRA;

        if (holder.is(EnchantmentTags.CURSE)) return TargetType.CURSE;

        return TargetType.UNKNOWN;
    }

    public static String getIcon(Holder<Enchantment> holder) {
        TargetType type = detect(holder);
        EnchantRollConfig.TooltipConfig cfg = EnchantRollConfig.get().tooltip;
        if (cfg.targetIcons != null) {
            String key = type.name().toLowerCase(Locale.ROOT);
            String icon = cfg.targetIcons.get(key);
            if (icon != null && !icon.isEmpty()) return icon;
        }
        return switch (type) {
            case ALL -> "✦";
            case ARMOR -> "🛡";
            case HELMET -> "🪖";
            case CHESTPLATE -> "🦺";
            case LEGGINGS -> "👖";
            case BOOTS -> "🥾";
            case MELEE -> "⚔🪓";
            case SWORD -> "⚔";
            case AXE -> "🪓";
            case TOOLS -> "⛏🪓";
            case MINING -> "⛏";
            case RANGED -> "🏹🎯";
            case BOW -> "🏹";
            case CROSSBOW -> "🎯";
            case TRIDENT -> "🔱";
            case MACE -> "🔨";
            case FISHING_ROD -> "🎣";
            case ELYTRA -> "🪽";
            case CURSE -> "☠";
            case UNKNOWN -> "✦";
        };
    }
}
