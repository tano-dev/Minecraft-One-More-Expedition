package com.expedition.enchants.tooltip;

import com.expedition.enchants.config.EnchantRollConfig;
import com.expedition.enchants.roll.CustomEnchantRoller;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import java.util.*;
import java.util.function.Consumer;

public class EnchantTooltipFormatter {

    private record EnchantEntry(
        Holder<Enchantment> holder,
        int level,
        CustomEnchantRoller.Tier tier,
        MutableComponent component,
        int estimatedLength,
        String sortName
    ) {}

    public static boolean formatTooltip(ItemEnchantments enchantments, Item.TooltipContext context, Consumer<Component> tooltip) {
        if (!EnchantRollConfig.get().tooltip.enabled) {
            return false;
        }
        if (enchantments == null || enchantments.isEmpty()) {
            return false;
        }
        List<Component> lines = buildTooltipLines(enchantments);
        if (lines.isEmpty()) {
            return false;
        }
        for (Component line : lines) {
            tooltip.accept(line);
        }
        return true;
    }

    public static boolean applyToItemStack(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        EnchantRollConfig.TooltipConfig cfg = EnchantRollConfig.get().tooltip;
        if (!cfg.enabled || !cfg.enableServerLoreSync) return false;

        ItemEnchantments enchants = stack.get(DataComponents.STORED_ENCHANTMENTS);
        if (enchants == null || enchants.isEmpty()) {
            enchants = stack.get(DataComponents.ENCHANTMENTS);
        }
        if (enchants == null || enchants.isEmpty()) {
            return false;
        }

        List<Component> lines = buildTooltipLines(enchants);
        if (lines.isEmpty()) return false;

        TooltipDisplay display = stack.getOrDefault(DataComponents.TOOLTIP_DISPLAY, TooltipDisplay.DEFAULT)
            .withHidden(DataComponents.ENCHANTMENTS, true)
            .withHidden(DataComponents.STORED_ENCHANTMENTS, true);
        stack.set(DataComponents.TOOLTIP_DISPLAY, display);
        stack.set(DataComponents.LORE, new ItemLore(lines));
        return true;
    }

    public static List<Component> buildTooltipLines(ItemEnchantments enchantments) {
        List<Component> result = new ArrayList<>();
        if (enchantments == null || enchantments.isEmpty()) {
            return result;
        }

        EnchantRollConfig.TooltipConfig cfg = EnchantRollConfig.get().tooltip;

        Map<CustomEnchantRoller.Tier, List<EnchantEntry>> byTier = new EnumMap<>(CustomEnchantRoller.Tier.class);
        for (CustomEnchantRoller.Tier t : CustomEnchantRoller.Tier.values()) {
            byTier.put(t, new ArrayList<>());
        }

        for (Object2IntMap.Entry<Holder<Enchantment>> entry : enchantments.entrySet()) {
            Holder<Enchantment> holder = entry.getKey();
            int level = entry.getIntValue();
            if (level <= 0) continue;

            CustomEnchantRoller.Tier tier = CustomEnchantRoller.detectTier(holder);
            Style style = getTierStyle(tier);
            boolean isCurse = (tier == CustomEnchantRoller.Tier.CURSE);
            boolean isMax = !isCurse && (level >= holder.value().getMaxLevel());

            MutableComponent comp = Component.empty().withStyle(style);
            int estLen = 0;

            String symbol = cfg.maxLevelSymbol != null ? cfg.maxLevelSymbol : "";
            if (isMax && cfg.symbolBeforeName && !symbol.isEmpty()) {
                comp.append(Component.literal(symbol + " ").withStyle(style));
                estLen += symbol.length() + 1;
            }

            // Name
            Component desc = holder.value().description();
            comp.append(desc.copy().withStyle(style));
            estLen += desc.getString().length();

            // Level (omit if level == 1 and maxLevel == 1, matching vanilla behavior)
            if (level != 1 || holder.value().getMaxLevel() != 1) {
                comp.append(Component.literal(" ").withStyle(style));
                Component levelComp = (level >= 1 && level <= 10)
                    ? Component.translatable("enchantment.level." + level).withStyle(style)
                    : Component.literal(toRoman(level)).withStyle(style);
                comp.append(levelComp);
                estLen += 1 + (level <= 10 ? levelRomanApprox(level) : toRoman(level).length());
            }

            if (isMax && !cfg.symbolBeforeName && !symbol.isEmpty()) {
                comp.append(Component.literal(" " + symbol).withStyle(style));
                estLen += 1 + symbol.length();
            }

            String sortName = desc.getString().toLowerCase(Locale.ROOT);
            byTier.get(tier).add(new EnchantEntry(holder, level, tier, comp, estLen, sortName));
        }

        // Ordered tiers: ULTIMATE -> EPIC -> RARE -> COMMON -> CURSE
        List<CustomEnchantRoller.Tier> tierOrder = new ArrayList<>();
        tierOrder.add(CustomEnchantRoller.Tier.ULTIMATE);
        tierOrder.add(CustomEnchantRoller.Tier.EPIC);
        tierOrder.add(CustomEnchantRoller.Tier.RARE);
        tierOrder.add(CustomEnchantRoller.Tier.COMMON);
        if (cfg.showCurseAtBottom) {
            tierOrder.add(CustomEnchantRoller.Tier.CURSE);
        }

        int maxPerLine = Math.max(1, cfg.maxEnchantsPerLine);
        int maxLineLen = Math.max(15, cfg.maxLineLength);

        for (CustomEnchantRoller.Tier t : tierOrder) {
            List<EnchantEntry> entries = byTier.get(t);
            if (entries == null || entries.isEmpty()) continue;

            // Sort alphabetically within the tier
            entries.sort(Comparator.comparing(EnchantEntry::sortName));

            // Wrap into lines
            MutableComponent currentLine = Component.empty();
            int currentCount = 0;
            int currentLen = 0;
            Style tierStyle = getTierStyle(t);

            for (EnchantEntry e : entries) {
                int commaLen = (currentCount > 0) ? 2 : 0;
                if (currentCount > 0 && (currentCount >= maxPerLine || (currentLen + commaLen + e.estimatedLength > maxLineLen))) {
                    result.add(currentLine);
                    currentLine = Component.empty();
                    currentCount = 0;
                    currentLen = 0;
                }

                if (currentCount > 0) {
                    currentLine.append(Component.literal(", ").withStyle(tierStyle));
                    currentLen += 2;
                }

                currentLine.append(e.component);
                currentLen += e.estimatedLength;
                currentCount++;
            }

            if (currentCount > 0) {
                result.add(currentLine);
            }
        }

        return result;
    }

    public static Style getTierStyle(CustomEnchantRoller.Tier tier) {
        EnchantRollConfig.TooltipConfig cfg = EnchantRollConfig.get().tooltip;
        String colorKey = tier.name().toLowerCase(Locale.ROOT);
        String configured = cfg.tierColors != null ? cfg.tierColors.get(colorKey) : null;
        TextColor textColor = null;
        if (configured != null) {
            var parsed = TextColor.parseColor(configured);
            if (parsed != null && parsed.result().isPresent()) {
                textColor = parsed.result().get();
            }
        }
        if (textColor == null) {
            textColor = switch (tier) {
                case ULTIMATE -> TextColor.GOLD;
                case EPIC -> TextColor.LIGHT_PURPLE;
                case RARE -> TextColor.AQUA;
                case COMMON -> TextColor.GRAY;
                case CURSE -> TextColor.RED;
            };
        }
        return Style.EMPTY.withColor(textColor).withItalic(false);
    }

    private static int levelRomanApprox(int level) {
        return switch (level) {
            case 1 -> 1;
            case 2 -> 2;
            case 3 -> 3;
            case 4 -> 2;
            case 5 -> 1;
            case 6 -> 2;
            case 7 -> 3;
            case 8 -> 4;
            case 9 -> 2;
            case 10 -> 1;
            default -> 3;
        };
    }

    private static String toRoman(int number) {
        if (number <= 0) return String.valueOf(number);
        String[] roman = {"M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I"};
        int[] values = {1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1};
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            while (number >= values[i]) {
                number -= values[i];
                sb.append(roman[i]);
            }
        }
        return sb.toString();
    }
}
