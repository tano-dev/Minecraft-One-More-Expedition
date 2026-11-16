package com.expedition.fishing.gui;

import com.expedition.fishing.biome.BiomeResolver;
import com.expedition.fishing.config.FishingDataRegistry;
import com.expedition.fishing.data.FishingPlayerData;
import com.expedition.fishing.data.PlayerDataStorage;
import com.expedition.fishing.item.FishingItemFactory;
import com.expedition.fishing.mechanics.FishingEngine;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;

import java.util.*;

public class FishingGui extends ChestMenu {
    public enum PageType {
        MAIN, ENCYCLOPEDIA, ARTIFACTS, BAG, RECIPES, CHANCES
    }

    private static final int ROWS = 6;
    private static final int SIZE = ROWS * 9;

    private final ServerPlayer viewer;
    private final SimpleContainer box;
    private final PageType pageType;
    private final int pageIndex;
    private final Map<Integer, Runnable> slotActions = new HashMap<>();

    public static void open(ServerPlayer player, PageType type, int page) {
        String title = switch (type) {
            case MAIN -> "§8[§3🎣 Expedition Fishing§8]";
            case ENCYCLOPEDIA -> "§8[§b📖 Sea Creature Encyclopedia p." + (page + 1) + "§8]";
            case ARTIFACTS -> "§8[§d✦ Artifacts Collection§8]";
            case BAG -> "§8[§6🎒 Equipped Artifact Bag§8]";
            case RECIPES -> "§8[§a⚒ Fishing Crafting & Upgrades p." + (page + 1) + "§8]";
            case CHANCES -> "§8[§e📊 Biome Pool Chances§8]";
        };
        player.openMenu(new SimpleMenuProvider(
                (syncId, inv, p) -> new FishingGui(syncId, inv, player, type, page),
                Component.literal(title)
        ));
    }

    private FishingGui(int syncId, Inventory playerInv, ServerPlayer viewer, PageType pageType, int pageIndex) {
        super(MenuType.GENERIC_9x6, syncId, playerInv, new SimpleContainer(SIZE), ROWS);
        this.viewer = viewer;
        this.box = (SimpleContainer) this.getContainer();
        this.pageType = pageType;
        this.pageIndex = Math.max(0, pageIndex);
        render();
    }

    private static Item itemById(String path) {
        return BuiltInRegistries.ITEM.getValue(Identifier.withDefaultNamespace(path));
    }

    private void render() {
        slotActions.clear();
        ItemStack glass = makeIcon(itemById("black_stained_glass_pane"), " ", List.of());
        for (int i = 0; i < SIZE; i++) {
            box.setItem(i, glass.copy());
        }

        switch (pageType) {
            case MAIN -> renderMain();
            case ENCYCLOPEDIA -> renderEncyclopedia();
            case ARTIFACTS -> renderArtifacts();
            case BAG -> renderBag();
            case RECIPES -> renderRecipes();
            case CHANCES -> renderChances();
        }
    }

    private void renderMain() {
        FishingPlayerData d = PlayerDataStorage.get(viewer.getUUID());
        FishingEngine.PlayerFishingStats st = FishingEngine.computeStats(viewer, viewer.getMainHandItem(), false, false);

        box.setItem(13, makeIcon(Items.FISHING_ROD, "§b§lAngler Profile: §f" + viewer.getName().getString(), List.of(
                "§7Fishing Level: §aLv." + d.level + " §8(" + String.format(Locale.US, "%.1f%%", d.getXpProgressPercent()) + ")",
                "§7Current Heat: §6" + Math.round(d.heat) + "/100 🔥",
                "§7Total Catches: §e" + d.totalCatches,
                "§7Unique Creatures: §b" + d.mobCatches.size() + " / " + FishingDataRegistry.MOBS.size(),
                "§7Artifacts Found: §d" + d.discoveredArtifacts.size() + " / " + FishingDataRegistry.ARTIFACTS.size(),
                "",
                "§7Fishing Speed: §b+" + Math.round(st.fishingSpeed) + "%",
                "§7Sea Creature Chance: §3" + String.format(Locale.US, "%.1f%%", st.seaCreatureChance),
                "§7Treasure Chance: §6" + String.format(Locale.US, "%.1f%%", st.treasureChance),
                "§7Double Catch: §a" + String.format(Locale.US, "%.1f%%", st.doubleCatchChance)
        )));

        setBtn(20, makeIcon(Items.KNOWLEDGE_BOOK, "§b§l📖 Sea Creature Encyclopedia", List.of(
                "§7Browse all " + FishingDataRegistry.MOBS.size() + " Water, Lava & End/Void",
                "§7Sea Creatures, catch counts & weight records.",
                "§eClick to open!"
        )), () -> open(viewer, PageType.ENCYCLOPEDIA, 0));

        setBtn(22, makeIcon(Items.CRAFTING_TABLE, "§a§l⚒ Fishing Recipes & Upgrades", List.of(
                "§7Browse and craft custom Rods, Armor Sets,",
                "§7Baits, and Enchanted Materials directly!",
                "§eClick to open!"
        )), () -> open(viewer, PageType.RECIPES, 0));

        setBtn(24, makeIcon(Items.HEART_OF_THE_SEA, "§d§l✦ Artifacts & Bag", List.of(
                "§7View discovered Artifacts and manage",
                "§7your active 9-slot Artifact Bag.",
                "§eClick to open!"
        )), () -> open(viewer, PageType.BAG, 0));

        setBtn(31, makeIcon(Items.COMPASS, "§e§l📊 Current Biome Pool Chances", List.of(
                "§7Inspect active biome pool (Terralith,",
                "§7Incendium, Stellarity, Nullscape & Vanilla).",
                "§eClick to open!"
        )), () -> open(viewer, PageType.CHANCES, 0));
    }

    private void renderEncyclopedia() {
        FishingPlayerData d = PlayerDataStorage.get(viewer.getUUID());
        List<String> keys = new ArrayList<>(FishingDataRegistry.MOBS.keySet());
        Collections.sort(keys);

        int perPage = 45;
        int start = pageIndex * perPage;
        for (int i = 0; i < perPage && (start + i) < keys.size(); i++) {
            String mobId = keys.get(start + i);
            JsonObject m = FishingDataRegistry.MOBS.get(mobId);
            String name = FishingDataRegistry.getStr(m, "display_name", FishingItemFactory.formatId(mobId));
            String rarity = FishingDataRegistry.getStr(m, "rarity", "COMMON");
            int reqLv = FishingDataRegistry.getInt(m, "required_level", 0);
            int caught = d.mobCatches.getOrDefault(mobId, 0);
            double bestKg = d.bestWeights.getOrDefault(mobId, 0.0);

            Item icon = caught > 0 ? itemById("tropical_fish") : itemById("gray_dye");
            box.setItem(i, makeIcon(icon, FishingItemFactory.getRarityColor(rarity) + name, List.of(
                    "§8" + rarity + " SEA CREATURE",
                    "§7Required Level: §eLv." + reqLv,
                    "§7Times Caught: §a" + caught,
                    "§7Best Weight: §b" + bestKg + " kg"
            )));
        }

        addNavButtons(keys.size(), perPage);
    }

    private void renderArtifacts() {
        FishingPlayerData d = PlayerDataStorage.get(viewer.getUUID());
        List<String> keys = new ArrayList<>(FishingDataRegistry.ARTIFACTS.keySet());
        Collections.sort(keys);

        for (int i = 0; i < Math.min(45, keys.size()); i++) {
            String artId = keys.get(i);
            boolean unlocked = d.discoveredArtifacts.contains(artId);
            boolean equipped = d.artifactBag.contains(artId);
            ItemStack st = FishingItemFactory.createArtifactItem(artId);
            if (!unlocked) {
                st = makeIcon(itemById("gray_dye"), "§7??? (Undiscovered Artifact)", List.of("§8Fish up Treasure Chests to discover!"));
            } else {
                setBtn(i, st, () -> {
                    if (d.artifactBag.contains(artId)) {
                        d.artifactBag.remove(artId);
                        viewer.sendSystemMessage(Component.literal("§eUnequipped artifact: " + artId));
                    } else if (d.artifactBag.size() < 9) {
                        d.artifactBag.add(artId);
                        viewer.sendSystemMessage(Component.literal("§aEquipped artifact in Bag: " + artId));
                    } else {
                        viewer.sendSystemMessage(Component.literal("§cYour Artifact Bag is full (max 9)!"));
                    }
                    d.dirty = true;
                    PlayerDataStorage.saveAsync(viewer.getUUID());
                    open(viewer, PageType.BAG, 0);
                });
                continue;
            }
            box.setItem(i, st);
        }
        setBtn(49, makeIcon(Items.ARROW, "§e← Back to Main Menu", List.of()), () -> open(viewer, PageType.MAIN, 0));
    }

    private void renderBag() {
        FishingPlayerData d = PlayerDataStorage.get(viewer.getUUID());
        for (int i = 0; i < 9; i++) {
            int slot = 18 + i;
            if (i < d.artifactBag.size()) {
                String artId = d.artifactBag.get(i);
                ItemStack st = FishingItemFactory.createArtifactItem(artId);
                setBtn(slot, st, () -> {
                    d.artifactBag.remove(artId);
                    d.dirty = true;
                    PlayerDataStorage.saveAsync(viewer.getUUID());
                    render();
                });
            } else {
                box.setItem(slot, makeIcon(itemById("light_gray_stained_glass_pane"), "§7Empty Artifact Slot #" + (i + 1), List.of(
                        "§7Click 'Browse All Artifacts' below to equip!"
                )));
            }
        }
        setBtn(31, makeIcon(Items.CHEST, "§d§lBrowse All Discovered Artifacts", List.of(
                "§7Click to toggle/equip discovered artifacts."
        )), () -> open(viewer, PageType.ARTIFACTS, 0));
        setBtn(49, makeIcon(Items.ARROW, "§e← Back to Main Menu", List.of()), () -> open(viewer, PageType.MAIN, 0));
    }

    private void renderRecipes() {
        FishingPlayerData d = PlayerDataStorage.get(viewer.getUUID());
        List<String> keys = new ArrayList<>(FishingDataRegistry.RECIPES.keySet());
        Collections.sort(keys);

        int perPage = 45;
        int start = pageIndex * perPage;
        for (int i = 0; i < perPage && (start + i) < keys.size(); i++) {
            String recId = keys.get(start + i);
            JsonObject rec = FishingDataRegistry.RECIPES.get(recId);
            String resId = FishingDataRegistry.getStr(rec, "result", recId.replace("_recipe", ""));
            int reqLv = FishingDataRegistry.getInt(rec, "level", 1);

            ItemStack preview = FishingItemFactory.createCustomItem(resId, 1);
            if (preview.isEmpty()) preview = makeIcon(Items.CRAFTING_TABLE, "§a" + FishingItemFactory.formatId(resId), List.of());

            Map<String, Integer> reqMaterials = extractIngredients(rec);
            List<Component> lore = new ArrayList<>();
            lore.add(Component.literal("§7Required Fishing Level: " + (d.level >= reqLv ? "§aLv." + reqLv : "§cLv." + reqLv)));
            lore.add(Component.literal("§6Ingredients:"));
            for (Map.Entry<String, Integer> ing : reqMaterials.entrySet()) {
                lore.add(Component.literal(" §7- §f" + ing.getValue() + "x §e" + FishingItemFactory.formatId(ing.getKey())));
            }
            lore.add(Component.empty());
            lore.add(Component.literal("§a▶ Click to Craft directly from Inventory!"));
            preview.set(DataComponents.LORE, new ItemLore(lore));

            final String targetItemId = resId;
            setBtn(i, preview, () -> attemptCraft(d, reqLv, targetItemId, reqMaterials));
        }

        addNavButtons(keys.size(), perPage);
    }

    private Map<String, Integer> extractIngredients(JsonObject rec) {
        Map<String, Integer> map = new LinkedHashMap<>();
        if (rec.has("ingredients") && rec.get("ingredients").isJsonObject()) {
            JsonObject ingObj = rec.getAsJsonObject("ingredients");
            for (Map.Entry<String, JsonElement> e : ingObj.entrySet()) {
                if (e.getValue().isJsonPrimitive() && e.getValue().getAsJsonPrimitive().isNumber()) {
                    map.put(e.getKey(), e.getValue().getAsInt());
                } else if (e.getValue().isJsonPrimitive()) {
                    map.merge(e.getValue().getAsString(), 1, Integer::sum);
                }
            }
        } else if (rec.has("input")) {
            String inp = FishingDataRegistry.getStr(rec, "input", "cod");
            int amt = FishingDataRegistry.getInt(rec, "amount", 16);
            map.put(inp, amt);
        }
        return map;
    }

    private void attemptCraft(FishingPlayerData d, int reqLv, String resultId, Map<String, Integer> reqMaterials) {
        if (d.level < reqLv) {
            viewer.sendSystemMessage(Component.literal("§cRequires Fishing Level " + reqLv + " to craft this item!"));
            return;
        }
        for (Map.Entry<String, Integer> entry : reqMaterials.entrySet()) {
            if (countInInventory(viewer, entry.getKey()) < entry.getValue()) {
                viewer.sendSystemMessage(Component.literal("§cMissing ingredient: " + entry.getValue() + "x " + FishingItemFactory.formatId(entry.getKey())));
                return;
            }
        }
        for (Map.Entry<String, Integer> entry : reqMaterials.entrySet()) {
            consumeFromInventory(viewer, entry.getKey(), entry.getValue());
        }
        ItemStack crafted = FishingItemFactory.createCustomItem(resultId, 1);
        if (!crafted.isEmpty()) {
            FishingEngine.giveOrDrop(viewer, crafted);
            viewer.level().playSound(null, viewer.blockPosition(), SoundEvents.ANVIL_USE, SoundSource.PLAYERS, 0.8f, 1.2f);
            viewer.sendSystemMessage(Component.literal("§a✦ Crafted ").append(crafted.getHoverName()).append(Component.literal("§a!")));
        }
    }

    private int countInInventory(ServerPlayer player, String id) {
        int total = 0;
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack st = inv.getItem(i);
            if (matchesIngredient(st, id)) {
                total += st.getCount();
            }
        }
        return total;
    }

    private void consumeFromInventory(ServerPlayer player, String id, int amount) {
        int rem = amount;
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize() && rem > 0; i++) {
            ItemStack st = inv.getItem(i);
            if (matchesIngredient(st, id)) {
                int take = Math.min(rem, st.getCount());
                st.shrink(take);
                rem -= take;
            }
        }
    }

    private boolean matchesIngredient(ItemStack st, String id) {
        if (st == null || st.isEmpty()) return false;
        String customId = FishingItemFactory.getCustomItemId(st);
        if (customId != null) {
            return customId.equalsIgnoreCase(id);
        }
        String vanKey = BuiltInRegistries.ITEM.getKey(st.getItem()).getPath();
        return vanKey.equalsIgnoreCase(id);
    }

    private void renderChances() {
        String biomeGroup = BiomeResolver.resolveBiomeGroup(viewer.level(), viewer.blockPosition(), false, false);
        JsonObject profile = FishingDataRegistry.BIOMES.get(biomeGroup);
        box.setItem(4, makeIcon(Items.COMPASS, "§eActive Biome Pool: §b" + biomeGroup, List.of(
                "§7Your current biome resolves to §f" + biomeGroup
        )));

        if (profile != null && profile.has("hostile_weights") && profile.get("hostile_weights").isJsonObject()) {
            int slot = 9;
            for (Map.Entry<String, JsonElement> e : profile.getAsJsonObject("hostile_weights").entrySet()) {
                if (slot >= 45) break;
                String mobId = e.getKey();
                double w = e.getValue().getAsDouble();
                JsonObject mCfg = FishingDataRegistry.MOBS.get(mobId);
                String rarity = mCfg != null ? FishingDataRegistry.getStr(mCfg, "rarity", "COMMON") : "COMMON";
                int reqLv = mCfg != null ? FishingDataRegistry.getInt(mCfg, "required_level", 0) : 0;
                box.setItem(slot++, makeIcon(Items.PRISMARINE_CRYSTALS, FishingItemFactory.getRarityColor(rarity) + FishingItemFactory.formatId(mobId), List.of(
                        "§7Pool Weight: §e" + w,
                        "§7Required Level: §bLv." + reqLv
                )));
            }
        }
        setBtn(49, makeIcon(Items.ARROW, "§e← Back to Main Menu", List.of()), () -> open(viewer, PageType.MAIN, 0));
    }

    private void addNavButtons(int totalItems, int perPage) {
        if (pageIndex > 0) {
            setBtn(45, makeIcon(Items.ARROW, "§e« Previous Page", List.of()), () -> open(viewer, pageType, pageIndex - 1));
        }
        setBtn(49, makeIcon(Items.BARRIER, "§c← Back to Main Menu", List.of()), () -> open(viewer, PageType.MAIN, 0));
        if ((pageIndex + 1) * perPage < totalItems) {
            setBtn(53, makeIcon(Items.ARROW, "§eNext Page »", List.of()), () -> open(viewer, pageType, pageIndex + 1));
        }
    }

    private void setBtn(int slot, ItemStack stack, Runnable action) {
        box.setItem(slot, stack);
        slotActions.put(slot, action);
    }

    private static ItemStack makeIcon(Item item, String name, List<String> lore) {
        ItemStack st = new ItemStack(item);
        st.set(DataComponents.CUSTOM_NAME, Component.literal(name));
        if (!lore.isEmpty()) {
            List<Component> comps = new ArrayList<>();
            for (String line : lore) comps.add(Component.literal(line));
            st.set(DataComponents.LORE, new ItemLore(comps));
        }
        return st;
    }

    @Override
    public void clicked(int slotId, int button, ContainerInput clickType, Player player) {
        if (!(player instanceof ServerPlayer)) return;
        Runnable action = slotActions.get(slotId);
        if (action != null) {
            action.run();
        }
        setCarried(ItemStack.EMPTY);
        broadcastChanges();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
        return false;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }
}
