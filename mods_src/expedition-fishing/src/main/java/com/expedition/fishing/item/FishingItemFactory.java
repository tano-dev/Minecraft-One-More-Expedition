package com.expedition.fishing.item;

import com.expedition.fishing.config.FishingDataRegistry;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class FishingItemFactory {
    public static final String NBT_ITEM_ID = "expedition_fishing_id";
    public static final String NBT_ARTIFACT_ID = "expedition_artifact_id";

    public static ItemStack createCustomItem(String itemId, int count) {
        JsonObject cfg = FishingDataRegistry.ITEMS.get(itemId);
        if (cfg == null) {
            return ItemStack.EMPTY;
        }

        String matName = FishingDataRegistry.getStr(cfg, "material", "PRISMARINE_SHARD").toLowerCase(Locale.ROOT);
        Item baseItem = BuiltInRegistries.ITEM.getValue(Identifier.withDefaultNamespace(matName));
        if (baseItem == null || baseItem == Items.AIR) {
            String cat = FishingDataRegistry.getStr(cfg, "category", "MATERIAL");
            baseItem = switch (cat) {
                case "ROD" -> Items.FISHING_ROD;
                case "WEAPON" -> Items.DIAMOND_SWORD;
                case "ARMOR" -> Items.IRON_CHESTPLATE;
                case "BAIT" -> Items.PRISMARINE_CRYSTALS;
                default -> Items.PRISMARINE_SHARD;
            };
        }

        ItemStack stack = new ItemStack(baseItem, Math.max(1, count));
        String displayName = FishingDataRegistry.getStr(cfg, "display_name", formatId(itemId));
        String rarity = FishingDataRegistry.getStr(cfg, "rarity", "COMMON").toUpperCase(Locale.ROOT);
        String colorPrefix = getRarityColor(rarity);

        stack.set(DataComponents.CUSTOM_NAME, Component.literal(colorPrefix + displayName));

        List<Component> loreLines = new ArrayList<>();
        String cat = FishingDataRegistry.getStr(cfg, "category", "MATERIAL");
        loreLines.add(Component.literal("§8" + rarity + " " + cat));

        double scc = FishingDataRegistry.getDouble(cfg, "scc", FishingDataRegistry.getDouble(cfg, "rare_creature_chance", 0.0));
        double speed = FishingDataRegistry.getDouble(cfg, "fishing_speed", FishingDataRegistry.getDouble(cfg, "fishing_speed_bonus", 0.0));
        double dbl = FishingDataRegistry.getDouble(cfg, "double_catch", 0.0);
        int reqLv = FishingDataRegistry.getInt(cfg, "required_level", 0);

        if (speed > 0) loreLines.add(Component.literal("§7Fishing Speed: §b+" + fmt(speed) + "%"));
        if (scc > 0) loreLines.add(Component.literal("§7Sea Creature Chance: §3+" + fmt(scc) + "%"));
        if (dbl > 0) loreLines.add(Component.literal("§7Double Catch: §a+" + fmt(dbl) + "%"));
        if (FishingDataRegistry.getBool(cfg, "lava_capable", itemId.contains("lava") || itemId.contains("magma") || itemId.contains("inferno") || itemId.contains("hellfire"))) {
            loreLines.add(Component.literal("§6🔥 Lava Fishing Capable"));
        }
        if (FishingDataRegistry.getBool(cfg, "void_capable", itemId.contains("void") || itemId.contains("starlight"))) {
            loreLines.add(Component.literal("§d✦ End / Void Fishing Capable"));
        }
        if (reqLv > 0) {
            loreLines.add(Component.literal("§eRequires Fishing Level " + reqLv));
        }

        if (cfg.has("lore") && cfg.get("lore").isJsonArray()) {
            for (JsonElement el : cfg.getAsJsonArray("lore")) {
                loreLines.add(Component.literal("§7" + el.getAsString()));
            }
        }

        if (FishingDataRegistry.getBool(cfg, "enchanted_glint", cat.equals("ENCHANTED_MATERIAL") || rarity.equals("LEGENDARY"))) {
            stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        }

        stack.set(DataComponents.LORE, new ItemLore(loreLines));

        CompoundTag tag = new CompoundTag();
        tag.putString(NBT_ITEM_ID, itemId);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return stack;
    }

    public static ItemStack createArtifactItem(String artifactId) {
        JsonObject cfg = FishingDataRegistry.ARTIFACTS.get(artifactId);
        if (cfg == null) return ItemStack.EMPTY;

        ItemStack stack = new ItemStack(Items.HEART_OF_THE_SEA, 1);
        String displayName = FishingDataRegistry.getStr(cfg, "display_name", formatId(artifactId));
        String rarity = FishingDataRegistry.getStr(cfg, "rarity", "RARE").toUpperCase(Locale.ROOT);
        String desc = FishingDataRegistry.getStr(cfg, "description", "A mysterious fishing artifact.");

        stack.set(DataComponents.CUSTOM_NAME, Component.literal(getRarityColor(rarity) + "✦ " + displayName));
        stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);

        List<Component> lore = new ArrayList<>();
        lore.add(Component.literal("§8" + rarity + " ARTIFACT"));
        lore.add(Component.literal("§7" + desc));
        if (cfg.has("passive_effects") && cfg.get("passive_effects").isJsonArray()) {
            lore.add(Component.empty());
            lore.add(Component.literal("§6Passive Bonus (in /fish bag):"));
            for (JsonElement el : cfg.getAsJsonArray("passive_effects")) {
                if (el.isJsonObject()) {
                    JsonObject eff = el.getAsJsonObject();
                    String stat = FishingDataRegistry.getStr(eff, "stat", "fishing_speed");
                    double val = FishingDataRegistry.getDouble(eff, "value", 1.0);
                    lore.add(Component.literal(" §a+" + fmt(val) + "% §f" + formatId(stat)));
                }
            }
        }
        stack.set(DataComponents.LORE, new ItemLore(lore));

        CompoundTag tag = new CompoundTag();
        tag.putString(NBT_ARTIFACT_ID, artifactId);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return stack;
    }

    public static String getCustomItemId(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        CustomData cd = stack.get(DataComponents.CUSTOM_DATA);
        if (cd == null) return null;
        CompoundTag tag = cd.copyTag();
        if (tag.contains(NBT_ITEM_ID)) {
            return tag.getString(NBT_ITEM_ID).orElse(null);
        }
        return null;
    }

    public static String getArtifactId(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        CustomData cd = stack.get(DataComponents.CUSTOM_DATA);
        if (cd == null) return null;
        CompoundTag tag = cd.copyTag();
        if (tag.contains(NBT_ARTIFACT_ID)) {
            return tag.getString(NBT_ARTIFACT_ID).orElse(null);
        }
        return null;
    }

    public static boolean isLavaCapableRod(ItemStack rod) {
        String id = getCustomItemId(rod);
        if (id == null) return false;
        JsonObject cfg = FishingDataRegistry.ITEMS.get(id);
        return FishingDataRegistry.getBool(cfg, "lava_capable",
                id.contains("lava") || id.contains("magma") || id.contains("inferno") || id.contains("hellfire") || id.contains("void") || id.contains("starlight"));
    }

    public static boolean isVoidCapableRod(ItemStack rod) {
        String id = getCustomItemId(rod);
        if (id == null) return false;
        JsonObject cfg = FishingDataRegistry.ITEMS.get(id);
        return FishingDataRegistry.getBool(cfg, "void_capable", id.contains("void") || id.contains("starlight"));
    }

    public static String getRarityColor(String rarity) {
        if (rarity == null) return "§f";
        return switch (rarity.toUpperCase(Locale.ROOT)) {
            case "UNCOMMON" -> "§a";
            case "RARE" -> "§b";
            case "EPIC" -> "§d";
            case "LEGENDARY", "ULTIMATE" -> "§6§l";
            default -> "§f";
        };
    }

    public static String formatId(String id) {
        if (id == null || id.isEmpty()) return "";
        String[] parts = id.split("_");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p.isEmpty()) continue;
            if (sb.length() > 0) sb.append(" ");
            sb.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1).toLowerCase(Locale.ROOT));
        }
        return sb.toString();
    }

    private static String fmt(double d) {
        return d == (long) d ? String.valueOf((long) d) : String.format(Locale.US, "%.1f", d);
    }
}
