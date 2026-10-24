package com.human.common.gameplay.fabrication;

import com.human.common.registry.init.HumanFabrication;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The drive bay's screen: 4 SSD slots and the 64 blueprint slots, 8 by 8. Slots past the bay's capacity are greyed:
 * nothing can go in, but a blueprint already there can still be taken out by hand.
 */
public class DriveBayMenu extends AbstractContainerMenu {

    public static final int GRID_COLUMNS = 8;

    public static final int SSD_X = 10;

    public static final int GRID_X = 36;

    public static final int TOP_Y = 18;

    public static final int INVENTORY_X = 15;

    public static final int INVENTORY_Y = 174;

    private static final int BAY_SLOTS = DriveBayBlockEntity.SSD_SLOTS + DriveBayBlockEntity.BLUEPRINT_SLOTS;

    private final @Nullable DriveBayBlockEntity bay;

    private final Container container;

    private final DataSlot capacity = DataSlot.standalone();

    /** Client side: the contents arrive through the normal slot sync. */
    public DriveBayMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, null);
    }

    public DriveBayMenu(int containerId, Inventory inventory, @Nullable DriveBayBlockEntity bay) {
        super(HumanFabrication.DRIVE_BAY_MENU.get(), containerId);
        this.bay = bay;
        this.container = bay == null ? new SimpleContainer(BAY_SLOTS) : new BayContainer(bay);

        for (int ssd = 0; ssd < DriveBayBlockEntity.SSD_SLOTS; ssd++) {
            addSlot(new Slot(container, ssd, SSD_X, TOP_Y + 9 + ssd * 36) {

                @Override
                public boolean mayPlace(@NotNull ItemStack stack) {
                    return DriveBayBlockEntity.isSsd(stack);
                }

                @Override
                public int getMaxStackSize() {
                    return 1;
                }
            });
        }

        for (int slot = 0; slot < DriveBayBlockEntity.BLUEPRINT_SLOTS; slot++) {
            var index = slot;

            addSlot(
                new Slot(
                    container,
                    DriveBayBlockEntity.SSD_SLOTS + slot,
                    GRID_X + (slot % GRID_COLUMNS) * 18,
                    TOP_Y + (slot / GRID_COLUMNS) * 18
                ) {

                    @Override
                    public boolean mayPlace(@NotNull ItemStack stack) {
                        return index < capacity() && DriveBayBlockEntity.isBlueprint(stack);
                    }

                    @Override
                    public int getMaxStackSize() {
                        return 1;
                    }
                }
            );
        }

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9, INVENTORY_X + column * 18, INVENTORY_Y + row * 18));
            }
        }

        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, INVENTORY_X + column * 18, INVENTORY_Y + 58));
        }

        addDataSlot(capacity);
        broadcastCapacity();
    }

    /** Usable blueprint slots: 16 per installed SSD. */
    public int capacity() {
        return bay != null ? bay.capacity() : capacity.get();
    }

    private void broadcastCapacity() {
        if (bay != null) {
            capacity.set(bay.capacity());
        }
    }

    @Override
    public void broadcastChanges() {
        broadcastCapacity();
        super.broadcastChanges();
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
        var slot = slots.get(index);

        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        var stack = slot.getItem();
        var original = stack.copy();

        if (index < BAY_SLOTS) {
            if (!moveItemStackTo(stack, BAY_SLOTS, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (DriveBayBlockEntity.isSsd(stack)) {
            if (!moveItemStackTo(stack, 0, DriveBayBlockEntity.SSD_SLOTS, false)) {
                return ItemStack.EMPTY;
            }
        } else if (DriveBayBlockEntity.isBlueprint(stack)) {
            // Only into usable slots: moveItemStackTo asks each slot's mayPlace, which refuses greyed ones.
            if (!moveItemStackTo(stack, DriveBayBlockEntity.SSD_SLOTS, BAY_SLOTS, false)) {
                return ItemStack.EMPTY;
            }
        } else {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }

        if (stack.getCount() == original.getCount()) {
            return ItemStack.EMPTY;
        }

        slot.onTake(player, stack);

        return original;
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        if (bay == null) {
            return true;
        }

        return !bay.isRemoved()
            && bay.getLevel() == player.level()
            && player.canInteractWithBlock(bay.getBlockPos(), 4.0D);
    }

    /** The bay's own lists, seen as one container: SSDs at 0-3, blueprints at 4-67. Every write saves the bay. */
    private static final class BayContainer implements Container {

        private final DriveBayBlockEntity bay;

        private BayContainer(DriveBayBlockEntity bay) {
            this.bay = bay;
        }

        @Override
        public int getContainerSize() {
            return BAY_SLOTS;
        }

        @Override
        public boolean isEmpty() {
            return bay.isEmpty();
        }

        @Override
        public @NotNull ItemStack getItem(int slot) {
            return slot < DriveBayBlockEntity.SSD_SLOTS ? bay.getSsd(slot) : bay.getBlueprint(slot - DriveBayBlockEntity.SSD_SLOTS);
        }

        @Override
        public @NotNull ItemStack removeItem(int slot, int amount) {
            var stack = getItem(slot);

            if (stack.isEmpty() || amount <= 0) {
                return ItemStack.EMPTY;
            }

            var taken = stack.split(amount);

            setItem(slot, stack.isEmpty() ? ItemStack.EMPTY : stack);

            return taken;
        }

        @Override
        public @NotNull ItemStack removeItemNoUpdate(int slot) {
            var stack = getItem(slot);

            setItem(slot, ItemStack.EMPTY);

            return stack;
        }

        @Override
        public void setItem(int slot, @NotNull ItemStack stack) {
            if (slot < DriveBayBlockEntity.SSD_SLOTS) {
                bay.setSsd(slot, stack);
            } else {
                bay.setBlueprint(slot - DriveBayBlockEntity.SSD_SLOTS, stack);
            }
        }

        @Override
        public void setChanged() {
            bay.setChanged();
        }

        @Override
        public boolean stillValid(@NotNull Player player) {
            return !bay.isRemoved();
        }

        @Override
        public void clearContent() {
            // Never cleared through the screen.
        }
    }
}
