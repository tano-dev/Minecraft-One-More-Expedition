package com.expedition.fishing.gui;

import com.expedition.fishing.biome.BiomeResolver;
import com.expedition.fishing.config.FishingDataRegistry;
import com.expedition.fishing.data.FishingPlayerData;
import com.expedition.fishing.data.PlayerDataStorage;
import com.expedition.fishing.item.FishingItemFactory;
import com.expedition.fishing.mechanics.FishingEngine;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
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
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import java.util.*;

public class FishingGui extends ChestMenu {
    public enum PageType {
        MAIN, ENCYCLOPEDIA, ARTIFACTS, BAG, TRADE, CHANCES
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
            case TRADE -> "§8[§6✦ Essence Enchantment Trade§8]";
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
            case TRADE -> renderTradeShop();
            case CHANCES -> renderChances();
        }
    }

    private void renderMain() {
        FishingPlayerData d = PlayerDataStorage.get(viewer.getUUID());
        FishingEngine.PlayerFishingStats st = FishingEngine.computeStats(viewer, viewer.getMainHandItem(), false, false);

        box.setItem(13, makeIcon(Items.FISHING_ROD, "§b§lAngler Profile: §f" + viewer.getName().getString(), List.of(
                "§7Fishing Level: §aLv." + d.level + " §8(" + String.format(Locale.US, "%.1f%%", d.getXpProgressPercent()) + ")",
                "",
                "§6§lEssences Held:",
                "§b💧 Water: " + d.getEssenceAmount("WATER"),
                "§c🔥 Magma: " + d.getEssenceAmount("MAGMA"),
                "§5✨ Void: " + d.getEssenceAmount("VOID"),
                "",
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

        setBtn(22, makeIcon(Items.ENCHANTED_BOOK, "§6§l✦ Essence Enchantment Trade", List.of(
                "§7Trade §bWater§7, §cMagma§7, and §5Void Essence",
                "§7for random §dLevel 1 Enchanted Books§7!",
                "§8(Exclusive non-table modpack & fishing enchants)",
                "§eClick to open!"
        )), () -> open(viewer, PageType.TRADE, 0));

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

    private void renderTradeShop() {
        FishingPlayerData d = PlayerDataStorage.get(viewer.getUUID());

        // Top Center: Live Essence Balance display
        box.setItem(4, makeIcon(Items.NETHER_STAR, "§6§l✦ Your Essence Balance", List.of(
                "§b💧 Water Essence (Overworld): §f" + d.getEssenceAmount("WATER"),
                "§c🔥 Magma Essence (Nether): §f" + d.getEssenceAmount("MAGMA"),
                "§5✨ Void Essence (The End): §f" + d.getEssenceAmount("VOID"),
                "",
                "§7Slay hostile Sea Creatures to earn",
                "§7Essence equal to §e(Required Level)²§7!"
        )));

        // Row 1 (Water): 1 random enchant, Lv.1
        box.setItem(10, makeIcon(Items.HEART_OF_THE_SEA, "§b§l💧 Water Essence Shop", List.of(
                "§7Currency: §bWater Essence",
                "§7Reward: §f1 Random Enchant §8(Lv.1)",
                "§7Your Balance: §f" + d.getEssenceAmount("WATER")
        )));
        renderTradeRow(d, "water", "WATER", "§b💧 Water Essence", 1, "FIXED_1", 50, 150, 300, 12, 14, 16);

        // Row 2 (Magma): 2 random enchants, random levels [1..max]
        box.setItem(19, makeIcon(Items.MAGMA_CREAM, "§c§l🔥 Magma Essence Shop", List.of(
                "§7Currency: §cMagma Essence",
                "§7Reward: §f2 Random Enchants §8(Random Lv. 1~Max)",
                "§7Your Balance: §f" + d.getEssenceAmount("MAGMA")
        )));
        renderTradeRow(d, "magma", "MAGMA", "§c🔥 Magma Essence", 2, "RANDOM_ANY", 50, 150, 300, 21, 23, 25);

        // Row 3 (Void): 3 random enchants, high levels [max-1..max]
        box.setItem(28, makeIcon(Items.ENDER_EYE, "§5§l✨ Void Essence Shop", List.of(
                "§7Currency: §5Void Essence",
                "§7Reward: §f3 Random Enchants §8(Lv. Max-1 ~ Max)",
                "§7Your Balance: §f" + d.getEssenceAmount("VOID")
        )));
        renderTradeRow(d, "void", "VOID", "§5✨ Void Essence", 3, "HIGH_MAX", 100, 300, 600, 30, 32, 34);

        setBtn(49, makeIcon(Items.ARROW, "§e← Back to Main Menu", List.of()), () -> open(viewer, PageType.MAIN, 0));
    }

    private void renderTradeRow(FishingPlayerData d, String shopKey, String essenceKey, String essenceLabel,
                                int defCount, String defLevelMode, int defCommon, int defRare, int defEpic,
                                int slotCommon, int slotRare, int slotEpic) {
        JsonObject shopsRoot = FishingDataRegistry.SHOP.get("shops");
        JsonObject shopCfg = (shopsRoot != null && shopsRoot.has(shopKey) && shopsRoot.get(shopKey).isJsonObject())
                ? shopsRoot.getAsJsonObject(shopKey)
                : null;

        int enchCount = shopCfg != null ? FishingDataRegistry.getInt(shopCfg, "enchant_count", defCount) : defCount;
        String levelMode = shopCfg != null ? FishingDataRegistry.getStr(shopCfg, "level_mode", defLevelMode) : defLevelMode;
        int cCost = shopCfg != null ? FishingDataRegistry.getInt(shopCfg, "common_cost", defCommon) : defCommon;
        int rCost = shopCfg != null ? FishingDataRegistry.getInt(shopCfg, "rare_cost", defRare) : defRare;
        int eCost = shopCfg != null ? FishingDataRegistry.getInt(shopCfg, "epic_cost", defEpic) : defEpic;

        addTradeButton(d, "common", "COMMON", "§7§lCommon", essenceKey, essenceLabel, cCost, enchCount, levelMode, slotCommon);
        addTradeButton(d, "rare", "RARE", "§b§lRare", essenceKey, essenceLabel, rCost, enchCount, levelMode, slotRare);
        addTradeButton(d, "epic", "EPIC", "§d§lEpic", essenceKey, essenceLabel, eCost, enchCount, levelMode, slotEpic);
    }

    private List<String> getRarityPoolIds(String rarityKey) {
        List<String> poolIds = new ArrayList<>();
        JsonObject poolsRoot = FishingDataRegistry.SHOP.get("rarity_pools");
        if (poolsRoot != null && poolsRoot.has(rarityKey) && poolsRoot.get(rarityKey).isJsonObject()) {
            JsonObject pObj = poolsRoot.getAsJsonObject(rarityKey);
            if (pObj.has("items") && pObj.get("items").isJsonArray()) {
                JsonArray arr = pObj.getAsJsonArray("items");
                for (JsonElement el : arr) {
                    if (el.isJsonPrimitive()) poolIds.add(el.getAsString());
                }
            }
        }
        return poolIds;
    }

    private void addTradeButton(FishingPlayerData d, String rarityKey, String rarityLabel, String colorTitle,
                                String essenceKey, String essenceLabel, int cost, int enchCount, String levelMode, int slot) {
        List<String> poolIds = getRarityPoolIds(rarityKey);
        long balance = d.getEssenceAmount(essenceKey);
        boolean canAfford = balance >= cost;

        String lvDesc = switch (levelMode.toUpperCase(Locale.ROOT)) {
            case "RANDOM_ANY" -> "Random Level (1 ~ Max)";
            case "HIGH_MAX" -> "High Level (Max-1 ~ Max)";
            default -> "Level I";
        };

        ItemStack icon = makeIcon(Items.ENCHANTED_BOOK, colorTitle + " §fEnchanted Book §8(" + enchCount + "x)", List.of(
                "§8" + rarityLabel + " RARITY POOL",
                "§7Enchants: §e" + enchCount + " Random " + colorTitle,
                "§7Level Roll: §b" + lvDesc,
                "§7Cost: " + (canAfford ? "§a" : "§c") + cost + "x " + essenceLabel,
                "§7Your Balance: §f" + balance,
                "§7Pool Size: §e" + poolIds.size() + " exclusive enchants",
                "",
                canAfford ? "§a▶ Click to Trade!" : "§c✖ Not enough " + essenceKey + " Essence!"
        ));
        icon.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);

        setBtn(slot, icon, () -> attemptTrade(d, essenceKey, essenceLabel, cost, rarityLabel, enchCount, levelMode, poolIds));
    }

    private void attemptTrade(FishingPlayerData d, String essenceKey, String essenceLabel, int cost,
                              String rarityLabel, int enchCount, String levelMode, List<String> poolIds) {
        if (d.getEssenceAmount(essenceKey) < cost) {
            viewer.sendSystemMessage(Component.literal("§c✖ You need " + cost + "x " + essenceLabel + " §c(Have: " + d.getEssenceAmount(essenceKey) + ")!"));
            viewer.level().playSound(null, viewer.blockPosition(), SoundEvents.VILLAGER_NO, SoundSource.PLAYERS, 0.8f, 1.0f);
            return;
        }

        HolderLookup.RegistryLookup<Enchantment> enchLookup = viewer.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        List<Holder.Reference<Enchantment>> validHolders = new ArrayList<>();
        for (String idStr : poolIds) {
            Identifier id = Identifier.tryParse(idStr);
            if (id == null) continue;
            ResourceKey<Enchantment> rk = ResourceKey.create(Registries.ENCHANTMENT, id);
            enchLookup.get(rk).ifPresent(validHolders::add);
        }

        if (validHolders.isEmpty()) {
            viewer.sendSystemMessage(Component.literal("§c✖ No valid enchantments found in this pool! Check shop_config.json."));
            return;
        }

        net.minecraft.util.RandomSource rng = viewer.level().getRandom();
        Collections.shuffle(validHolders, new java.util.Random(rng.nextLong()));
        int picks = Math.min(Math.max(1, enchCount), validHolders.size());

        ItemStack book = new ItemStack(Items.ENCHANTED_BOOK, 1);
        ItemEnchantments.Mutable mut = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        List<Component> rolledNames = new ArrayList<>();

        for (int i = 0; i < picks; i++) {
            Holder.Reference<Enchantment> chosen = validHolders.get(i);
            int maxLv = Math.max(1, chosen.value().getMaxLevel());
            int rolledLv = switch (levelMode.toUpperCase(Locale.ROOT)) {
                case "RANDOM_ANY" -> 1 + rng.nextInt(maxLv);
                case "HIGH_MAX" -> {
                    int minLv = Math.max(1, maxLv - 1);
                    yield minLv + rng.nextInt(maxLv - minLv + 1);
                }
                default -> 1;
            };
            mut.set(chosen, rolledLv);
            rolledNames.add(Enchantment.getFullname(chosen, rolledLv));
        }
        book.set(DataComponents.STORED_ENCHANTMENTS, mut.toImmutable());

        d.addEssence(essenceKey, -cost);
        PlayerDataStorage.saveAsync(viewer.getUUID());
        FishingEngine.giveOrDrop(viewer, book);

        viewer.level().playSound(null, viewer.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.9f, 1.2f);
        net.minecraft.network.chat.MutableComponent msg = Component.literal("§a✦ Traded §e" + cost + "x " + essenceLabel + " §afor §8[" + rarityLabel + "§8] ");
        for (int i = 0; i < rolledNames.size(); i++) {
            if (i > 0) msg.append(Component.literal("§7, "));
            msg.append(rolledNames.get(i));
        }
        viewer.sendSystemMessage(msg);

        // Refresh GUI immediately to show updated Essence balance
        render();
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
