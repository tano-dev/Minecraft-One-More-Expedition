package com.expedition.enchants.ui;

import com.expedition.enchants.roll.CustomEnchantRoller;
import net.minecraft.core.Holder;
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
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import java.util.*;

public class RevealEnchantsMenu extends ChestMenu {
    private static final int ROWS = 3;
    private static final int TOTAL_SLOTS = ROWS * 9;
    private static final int PREVIEW_SLOT = 4;
    private static final int REVEAL_ALL_SLOT = 26;

    private final SimpleContainer container;
    private final Map<Integer, EnchantSlotData> revealSlots = new LinkedHashMap<>();

    private record EnchantSlotData(Holder<Enchantment> enchantment, int level, CustomEnchantRoller.Tier tier, boolean revealed) {
        EnchantSlotData withRevealed() {
            return new EnchantSlotData(enchantment, level, tier, true);
        }
    }

    public static void open(ServerPlayer player, ItemStack enchantedItem) {
        ItemEnchantments enchants = enchantedItem.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        if (enchants.isEmpty()) {
            enchants = enchantedItem.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY);
        }
        if (enchants.isEmpty()) return;

        List<Map.Entry<Holder<Enchantment>, Integer>> entries = new ArrayList<>(enchants.entrySet());
        player.openMenu(new SimpleMenuProvider(
            (syncId, inv, p) -> new RevealEnchantsMenu(syncId, inv, enchantedItem.copy(), entries),
            Component.literal("§8[§bReveal Enchants§8]")
        ));
    }

    private RevealEnchantsMenu(int syncId, Inventory playerInv, ItemStack previewItem, List<Map.Entry<Holder<Enchantment>, Integer>> entries) {
        super(MenuType.GENERIC_9x3, syncId, playerInv, new SimpleContainer(TOTAL_SLOTS), ROWS);
        this.container = (SimpleContainer) this.getContainer();
        setupUi(previewItem, entries);
    }

    private static Item itemById(String path) {
        return BuiltInRegistries.ITEM.getValue(Identifier.withDefaultNamespace(path));
    }

    private void setupUi(ItemStack previewItem, List<Map.Entry<Holder<Enchantment>, Integer>> entries) {
        ItemStack filler = new ItemStack(itemById("black_stained_glass_pane"));
        filler.set(DataComponents.CUSTOM_NAME, Component.literal(" "));
        for (int i = 0; i < TOTAL_SLOTS; i++) {
            container.setItem(i, filler.copy());
        }

        int count = Math.min(entries.size(), 7);
        int startCol = (9 - count) / 2;
        for (int i = 0; i < count; i++) {
            int slot = 9 + startCol + i;
            var entry = entries.get(i);
            CustomEnchantRoller.Tier tier = CustomEnchantRoller.detectTier(entry.getKey());
            revealSlots.put(slot, new EnchantSlotData(entry.getKey(), entry.getValue(), tier, false));
            container.setItem(slot, createMysteryPane(tier));
        }

        ItemStack revealAll = new ItemStack(Items.AMETHYST_SHARD);
        revealAll.set(DataComponents.CUSTOM_NAME, Component.literal("§e§lReveal All"));
        container.setItem(REVEAL_ALL_SLOT, revealAll);
    }

    private static ItemStack createMysteryPane(CustomEnchantRoller.Tier tier) {
        ItemStack pane = switch (tier) {
            case RARE -> new ItemStack(itemById("light_blue_stained_glass_pane"));
            case EPIC -> new ItemStack(itemById("purple_stained_glass_pane"));
            case ULTIMATE -> new ItemStack(itemById("yellow_stained_glass_pane"));
            case CURSE -> new ItemStack(itemById("red_stained_glass_pane"));
            default -> new ItemStack(itemById("light_gray_stained_glass_pane"));
        };
        String label = switch (tier) {
            case RARE -> "§b??? §3(Rare)";
            case EPIC -> "§d??? §5(Epic)";
            case ULTIMATE -> "§e§l??? §6(Ultimate)";
            case CURSE -> "§c??? §4(Curse)";
            default -> "§7??? §8(Common)";
        };
        pane.set(DataComponents.CUSTOM_NAME, Component.literal(label));
        return pane;
    }

    private static ItemStack createRevealedBook(EnchantSlotData data) {
        ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
        Component fullName = Enchantment.getFullname(data.enchantment(), data.level());
        book.set(DataComponents.CUSTOM_NAME, fullName);
        return book;
    }

    private void playTierSound(ServerPlayer player, CustomEnchantRoller.Tier tier) {
        switch (tier) {
            case ULTIMATE -> {
                player.level().playSound(null, player.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 0.95f, 1.0f);
                player.level().playSound(null, player.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.8f, 1.35f);
                player.level().playSound(null, player.blockPosition(), SoundEvents.FIREWORK_ROCKET_TWINKLE, SoundSource.PLAYERS, 0.9f, 1.1f);
            }
            case EPIC -> {
                player.level().playSound(null, player.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 0.75f, 1.2f);
                player.level().playSound(null, player.blockPosition(), SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.PLAYERS, 1.0f, 1.3f);
                player.level().playSound(null, player.blockPosition(), SoundEvents.TRIDENT_RETURN, SoundSource.PLAYERS, 0.6f, 1.4f);
            }
            case RARE -> {
                player.level().playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0f, 1.35f);
                player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.5f, 1.5f);
                player.level().playSound(null, player.blockPosition(), SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.PLAYERS, 0.9f, 1.2f);
            }
            case CURSE -> {
                player.level().playSound(null, player.blockPosition(), SoundEvents.WITHER_HURT, SoundSource.PLAYERS, 0.6f, 0.8f);
                player.level().playSound(null, player.blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 0.7f, 0.7f);
            }
            default -> {
                player.level().playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_HIT, SoundSource.PLAYERS, 0.8f, 1.2f);
                player.level().playSound(null, player.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.85f, 1.15f);
            }
        }
    }

    @Override
    public void clicked(int slotId, int button, ContainerInput clickType, Player player) {
        // Never delegate to super.clicked() — completely blocks taking, swapping, dropping, or duping any UI item.
        if (!(player instanceof ServerPlayer sp)) return;

        if (slotId == REVEAL_ALL_SLOT) {
            CustomEnchantRoller.Tier highest = null;
            for (var entry : revealSlots.entrySet()) {
                if (!entry.getValue().revealed()) {
                    EnchantSlotData updated = entry.getValue().withRevealed();
                    entry.setValue(updated);
                    container.setItem(entry.getKey(), createRevealedBook(updated));
                    if (highest == null || updated.tier().ordinal() > highest.ordinal()) {
                        highest = updated.tier();
                    }
                }
            }
            if (highest != null) {
                playTierSound(sp, highest);
            }
        } else if (revealSlots.containsKey(slotId)) {
            EnchantSlotData data = revealSlots.get(slotId);
            if (!data.revealed()) {
                EnchantSlotData updated = data.withRevealed();
                revealSlots.put(slotId, updated);
                container.setItem(slotId, createRevealedBook(updated));
                playTierSound(sp, updated.tier());
            }
        }

        // Force-clear cursor and resync to prevent packet-freeze / desync exploits
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
