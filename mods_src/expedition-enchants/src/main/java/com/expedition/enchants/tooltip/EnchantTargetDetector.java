package com.expedition.enchants.tooltip;

import com.expedition.enchants.config.EnchantRollConfig;
import com.expedition.enchants.roll.CustomEnchantRoller;
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
        SHIELD,
        PET,
        MELEE,
        SWORD,
        AXE,
        SPEAR,
        TOOLS,
        MINING,
        BOW,
        CROSSBOW,
        RANGED,
        TRIDENT,
        MACE,
        FISHING_ROD,
        ELYTRA,
        SHEARS,
        CURSE,
        UNKNOWN
    }

    private static final Map<Holder<Enchantment>, TargetType> CACHE = new ConcurrentHashMap<>();

    public static TargetType detect(Holder<Enchantment> holder) {
        if (holder == null) return TargetType.UNKNOWN;
        return CACHE.computeIfAbsent(holder, EnchantTargetDetector::compute);
    }

    private static TargetType compute(Holder<Enchantment> holder) {
        if (holder.is(EnchantmentTags.CURSE) || CustomEnchantRoller.detectTier(holder) == CustomEnchantRoller.Tier.CURSE) {
            return TargetType.CURSE;
        }

        Enchantment ench = holder.value();

        boolean helmet = ench.isSupportedItem(new ItemStack(Items.DIAMOND_HELMET));
        boolean chest = ench.isSupportedItem(new ItemStack(Items.DIAMOND_CHESTPLATE));
        boolean legs = ench.isSupportedItem(new ItemStack(Items.DIAMOND_LEGGINGS));
        boolean boots = ench.isSupportedItem(new ItemStack(Items.DIAMOND_BOOTS));

        boolean shield = ench.isSupportedItem(new ItemStack(Items.SHIELD));
        boolean pet = ench.isSupportedItem(new ItemStack(Items.WOLF_ARMOR)) || ench.isSupportedItem(new ItemStack(Items.DIAMOND_HORSE_ARMOR));

        boolean sword = ench.isSupportedItem(new ItemStack(Items.DIAMOND_SWORD));
        boolean axe = ench.isSupportedItem(new ItemStack(Items.DIAMOND_AXE));
        boolean spear = ench.isSupportedItem(new ItemStack(Items.DIAMOND_SPEAR));
        boolean bow = ench.isSupportedItem(new ItemStack(Items.BOW));
        boolean crossbow = ench.isSupportedItem(new ItemStack(Items.CROSSBOW));
        boolean trident = ench.isSupportedItem(new ItemStack(Items.TRIDENT));
        boolean mace = ench.isSupportedItem(new ItemStack(Items.MACE));

        boolean pickaxe = ench.isSupportedItem(new ItemStack(Items.DIAMOND_PICKAXE));
        boolean shovel = ench.isSupportedItem(new ItemStack(Items.DIAMOND_SHOVEL));
        boolean hoe = ench.isSupportedItem(new ItemStack(Items.DIAMOND_HOE));

        boolean fishing = ench.isSupportedItem(new ItemStack(Items.FISHING_ROD));
        boolean elytra = ench.isSupportedItem(new ItemStack(Items.ELYTRA));
        boolean shears = ench.isSupportedItem(new ItemStack(Items.SHEARS));

        boolean allArmor = helmet && chest && legs && boots;
        boolean hasArmor = helmet || chest || legs || boots;
        boolean hasWeapons = sword || axe || spear || bow || crossbow || trident || mace;
        boolean hasMining = pickaxe || shovel || hoe;

        // Universal (Unbreaking, Mending, Curse of Vanishing...)
        if (allArmor && (hasWeapons || hasMining)) {
            return TargetType.ALL;
        }

        // Armor & Defense
        if (allArmor && !hasWeapons && !hasMining) return TargetType.ARMOR;
        if (helmet && !chest && !legs && !boots) return TargetType.HELMET;
        if (chest && !helmet && !legs && !boots) return TargetType.CHESTPLATE;
        if (legs && !helmet && !chest && !boots) return TargetType.LEGGINGS;
        if (boots && !helmet && !chest && !legs) return TargetType.BOOTS;
        if (shield && !hasArmor && !hasWeapons && !hasMining) return TargetType.SHIELD;
        if (pet && !hasArmor && !hasWeapons) return TargetType.PET;
        if (hasArmor && !hasWeapons && !hasMining) return TargetType.ARMOR;

        // Tools
        if (hasMining && axe) return TargetType.TOOLS; // Efficiency, Fortune, Silk Touch
        if (hasMining) return TargetType.MINING;
        if (shears && !hasMining && !hasWeapons) return TargetType.SHEARS;

        // Ranged Weapons
        if (bow && crossbow) return TargetType.RANGED;
        if (bow && !sword) return TargetType.BOW;
        if (crossbow && !sword) return TargetType.CROSSBOW;

        // Unique Weapons (Exclusive to specific weapon type)
        if (trident && !sword) return TargetType.TRIDENT;
        if (mace && !sword && !axe && !spear) return TargetType.MACE; // Density, Breach, Wind Burst
        if (spear && !sword && !axe && !mace) return TargetType.SPEAR; // Lunge, Impale Rush

        // Melee Weapons (Swords, Axes, and hybrid weapons that also accept Mace/Spear like Fire Aspect, Smite)
        if (sword && axe) return TargetType.MELEE;
        if (sword) return TargetType.SWORD; // Fire Aspect, Oozing Aspect, Looting, Sweeping Edge
        if (axe) return TargetType.AXE;

        // Misc
        if (fishing) return TargetType.FISHING_ROD;
        if (elytra) return TargetType.ELYTRA;

        if (mace) return TargetType.MACE;
        if (spear) return TargetType.SPEAR;

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
            case SHIELD -> "🛡";
            case PET -> "🐾";
            case MELEE -> "⚔🪓";
            case SWORD -> "⚔";
            case AXE -> "🪓";
            case SPEAR -> "🗡";
            case TOOLS -> "⛏🪓";
            case MINING -> "⛏";
            case RANGED -> "🏹🎯";
            case BOW -> "🏹";
            case CROSSBOW -> "🎯";
            case TRIDENT -> "🔱";
            case MACE -> "🔨";
            case FISHING_ROD -> "🎣";
            case ELYTRA -> "🪽";
            case SHEARS -> "✂";
            case CURSE -> "☠";
            case UNKNOWN -> "✦";
        };
    }
}
