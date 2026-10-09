package com.human.common.gameplay.menu.workstation;

import com.human.common.gameplay.block.entity.workstation.ElectronicsWorkstationBlockEntity;
import com.human.common.gameplay.workstation.ElectronicsCrafting;
import com.human.common.registry.init.HumanMenuTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * The electronics workstation's menu: pick a component from the list, and the result slot offers it when the storage
 * can build it. [stated] Sep 29/30 - stonecutter-style, no input slot, with a material list.
 * <h2>SCROLLING STORAGE - TWO ROWS OF EIGHT ON SCREEN</h2> [stated] Sep 30: storage the size of a double chest, shown
 * two rows of eight at a time with its scrollbar in the ninth column. Vanilla fixes a slot's screen position for good,
 * so scrolling is done the way that stays in sync with no packet: every storage row gets a slot at EACH screen row it
 * can scroll to, all pointing at the same container slot, and only the one matching the current scroll is active
 * ({@link Slot#isActive()} - vanilla's screen neither draws nor clicks an inactive slot, checked in its bytecode).
 * Clicks name a menu slot id, so the server never needs to know the scroll position, and every container slot is always
 * synced - which the material list relies on.
 * <h2>SLOTS</h2> The storage's row slots first, then the result, then the player's inventory. The recipe list is picked
 * with menu buttons (the button id is the list index), as vanilla's stonecutter does, so no packet is needed.
 * <h2>THE RESULT SLOT</h2> Shows the component only when the plan is craftable. TAKING it commits the plan: storage
 * becomes what the plan said it would hold. The plan is made again at that moment, not trusted from earlier - a hopper
 * may have moved something since - and if it no longer works the taken stack is cancelled, so nothing is ever made from
 * nothing. Shift-click crafts repeatedly, up to a stack, while it still can.
 */
public class ElectronicsWorkstationMenu extends AbstractContainerMenu {

    public static final int STORAGE_X = 8;

    public static final int STORAGE_Y = 114;

    public static final int RESULT_X = 12;

    public static final int RESULT_Y = 79;

    public static final int PLAYER_Y = 164;

    public static final int HOTBAR_Y = 222;

    /** [stated] Sep 30: eight wide, the storage's scrollbar where the ninth slot would be. */
    public static final int COLUMNS = 8;

    /** [stated] Sep 30: two rows on screen. */
    public static final int VISIBLE_ROWS = 2;

    private static final int SIZE = ElectronicsWorkstationBlockEntity.SIZE;

    public static final int ROWS = SIZE / COLUMNS;

    /** One row slot per (storage row, screen row) pair it can appear at. */
    private static final int STORAGE_SLOTS = countStorageSlots();

    private static final int RESULT = STORAGE_SLOTS;

    private static final int PLAYER_FIRST = STORAGE_SLOTS + 1;

    private static final int PLAYER_END = PLAYER_FIRST + 36;

    /** CLIENT: the first storage row on screen. The server never reads it. */
    private int scrollRow;

    private final Container storage;

    private final SimpleContainer result = new SimpleContainer(1);

    private final DataSlot selected = DataSlot.standalone();

    private final Level level;

    private final List<ElectronicsCrafting.Craftable> craftables;

    /** Server: what the result was last computed from, so it is only redone when something changed. */
    private int lastSelected = -2;

    private int lastStorageHash;

    public ElectronicsWorkstationMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, new SimpleContainer(SIZE));
    }

    public ElectronicsWorkstationMenu(int containerId, Inventory inventory, Container storage) {
        super(HumanMenuTypes.ELECTRONICS_WORKSTATION.get(), containerId);
        checkContainerSize(storage, SIZE);

        this.storage = storage;
        this.level = inventory.player.level();
        this.craftables = ElectronicsCrafting.craftables(level);
        this.selected.set(-1);

        storage.startOpen(inventory.player);

        for (var row = 0; row < ROWS; row++) {
            for (var screenRow = 0; screenRow < VISIBLE_ROWS; screenRow++) {
                if (!canShow(row, screenRow)) {
                    continue;
                }

                for (var column = 0; column < COLUMNS; column++) {
                    addSlot(
                        new StorageSlot(
                            storage,
                            column + row * COLUMNS,
                            STORAGE_X + column * 18,
                            STORAGE_Y + screenRow * 18,
                            row - screenRow
                        )
                    );
                }
            }
        }

        addSlot(new ResultSlot(result, RESULT_X, RESULT_Y));

        for (var row = 0; row < 3; row++) {
            for (var column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9, 8 + column * 18, PLAYER_Y + row * 18));
            }
        }

        for (var column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, 8 + column * 18, HOTBAR_Y));
        }

        addDataSlot(selected);
    }

    /** Storage row {@code row} can be drawn at screen row {@code screenRow} for some scroll position. */
    private static boolean canShow(int row, int screenRow) {
        var scroll = row - screenRow;

        return scroll >= 0 && scroll <= ROWS - VISIBLE_ROWS;
    }

    private static int countStorageSlots() {
        var count = 0;

        for (var row = 0; row < ROWS; row++) {
            for (var screenRow = 0; screenRow < VISIBLE_ROWS; screenRow++) {
                if (canShow(row, screenRow)) {
                    count += COLUMNS;
                }
            }
        }

        return count;
    }

    public int getScrollRow() {
        return scrollRow;
    }

    public void setScrollRow(int row) {
        scrollRow = Math.max(0, Math.min(ROWS - VISIBLE_ROWS, row));
    }

    public static int maxScrollRow() {
        return ROWS - VISIBLE_ROWS;
    }

    public List<ElectronicsCrafting.Craftable> getCraftables() {
        return craftables;
    }

    public int getSelected() {
        return selected.get();
    }

    public @Nullable ElectronicsCrafting.Craftable getSelectedCraftable() {
        var index = selected.get();

        return index >= 0 && index < craftables.size() ? craftables.get(index) : null;
    }

    /** The storage as the menu sees it - the synced copy on the client. */
    public List<ItemStack> storageContents() {
        var list = new ArrayList<ItemStack>(SIZE);

        for (var index = 0; index < SIZE; index++) {
            list.add(storage.getItem(index));
        }

        return list;
    }

    public @Nullable ElectronicsCrafting.Plan plan() {
        var craftable = getSelectedCraftable();

        return craftable == null ? null : ElectronicsCrafting.plan(level, craftable, storageContents(), storage.getMaxStackSize());
    }

    @Override
    public boolean clickMenuButton(@NotNull Player player, int id) {
        if (id < 0 || id >= craftables.size()) {
            return false;
        }

        selected.set(id);
        refreshResult();

        return true;
    }

    @Override
    public void broadcastChanges() {
        var hash = storageHash();

        if (hash != lastStorageHash || selected.get() != lastSelected) {
            refreshResult();
        }

        super.broadcastChanges();
    }

    private void refreshResult() {
        lastStorageHash = storageHash();
        lastSelected = selected.get();

        var plan = plan();

        result.setItem(0, plan != null && plan.craftable() ? plan.result().copy() : ItemStack.EMPTY);
    }

    private int storageHash() {
        var hash = 1;

        for (var index = 0; index < SIZE; index++) {
            var stack = storage.getItem(index);

            hash = 31 * hash + (stack.isEmpty() ? 0 : ItemStack.hashItemAndComponents(stack) * 31 + stack.getCount());
        }

        return hash;
    }

    /** Commits a craft to storage if it still works. @return whether it did */
    private boolean commit() {
        var plan = plan();

        if (plan == null || !plan.craftable()) {
            return false;
        }

        for (var index = 0; index < SIZE; index++) {
            storage.setItem(index, plan.after().get(index));
        }

        storage.setChanged();

        return true;
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
        var slot = slots.get(index);

        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        if (index == RESULT) {
            // Craft repeatedly, up to one stack, while the storage can and the player has room.
            var made = 0;
            var template = slot.getItem().copy();

            while (made < template.getMaxStackSize()) {
                var plan = plan();

                if (plan == null || !plan.craftable()) {
                    break;
                }

                var crafted = plan.result().copy();

                if (!moveItemStackTo(crafted, PLAYER_FIRST, PLAYER_END, true) || !crafted.isEmpty()) {
                    // No room for all of it: put back whatever did move and stop, without committing.
                    if (crafted.getCount() != plan.result().getCount()) {
                        player.getInventory().placeItemBackInInventory(crafted);
                        commit();
                    }

                    break;
                }

                commit();
                made += plan.result().getCount();
            }

            if (made > 0) {
                playCraftSound(player);
            }

            refreshResult();

            return ItemStack.EMPTY;
        }

        var stack = slot.getItem();
        var original = stack.copy();

        if (index < STORAGE_SLOTS) {
            if (!moveItemStackTo(stack, PLAYER_FIRST, PLAYER_END, true)) {
                return ItemStack.EMPTY;
            }
        } else if (!insertIntoStorage(stack)) {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }

        return stack.getCount() == original.getCount() ? ItemStack.EMPTY : original;
    }

    /**
     * Shift-click into storage goes by CONTAINER order, first slot first, rather than through the row slots (whose menu
     * order is by screen position). Merges into matching stacks, then fills empty slots. @return whether anything moved
     */
    private boolean insertIntoStorage(ItemStack stack) {
        var before = stack.getCount();

        for (var index = 0; index < SIZE && !stack.isEmpty(); index++) {
            var slot = storage.getItem(index);

            if (!slot.isEmpty() && ItemStack.isSameItemSameComponents(slot, stack)) {
                var moved = Math.min(stack.getCount(), Math.min(slot.getMaxStackSize(), storage.getMaxStackSize()) - slot.getCount());

                if (moved > 0) {
                    slot.grow(moved);
                    stack.shrink(moved);
                    storage.setChanged();
                }
            }
        }

        for (var index = 0; index < SIZE && !stack.isEmpty(); index++) {
            if (storage.getItem(index).isEmpty()) {
                storage.setItem(index, stack.split(Math.min(stack.getMaxStackSize(), storage.getMaxStackSize())));
            }
        }

        return stack.getCount() != before;
    }

    private void playCraftSound(Player player) {
        if (!level.isClientSide) {
            level.playSound(null, player.blockPosition(), SoundEvents.UI_STONECUTTER_TAKE_RESULT, SoundSource.BLOCKS, 1.0F, 1.0F);
        }
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return storage.stillValid(player);
    }

    @Override
    public void removed(@NotNull Player player) {
        super.removed(player);
        storage.stopOpen(player);
    }

    /** A storage slot shown at one screen row: active only when the scroll puts its row there. */
    private final class StorageSlot extends Slot {

        private final int scrollWhenShown;

        StorageSlot(Container container, int slot, int x, int y, int scrollWhenShown) {
            super(container, slot, x, y);
            this.scrollWhenShown = scrollWhenShown;
        }

        @Override
        public boolean isActive() {
            return scrollRow == scrollWhenShown;
        }
    }

    /** The result: take only, and taking is what crafts. */
    private final class ResultSlot extends Slot {

        ResultSlot(Container container, int x, int y) {
            super(container, 0, x, y);
        }

        @Override
        public boolean mayPlace(@NotNull ItemStack stack) {
            return false;
        }

        @Override
        public void onTake(@NotNull Player player, @NotNull ItemStack stack) {
            if (commit()) {
                playCraftSound(player);
            } else {
                // ⚠ The storage changed since the result was shown: cancel what was taken rather than make it free.
                stack.setCount(0);
            }

            refreshResult();
            super.onTake(player, stack);
        }
    }
}
