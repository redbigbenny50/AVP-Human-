package com.human.common.gameplay.menu.marine;

import com.blib.api.common.inventory.v1.BLibInventory;
import com.human.common.gameplay.entity.living.human.marine.Marine;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * Presents a marine's {@link BLibInventory} to the container-menu system.
 * <p>
 * Only one inventory exists per marine and this exposes all of it. Issued gear is not hidden or split off into a second
 * inventory; it is shown and locked, by {@link MarineInventorySlot}. Splitting was the obvious-looking design and it is
 * the wrong one - {@code getInventory()} is what every AI package searches, including the reload in
 * {@code GunStrategy}, so anything moved out of it becomes invisible to the marine that owns it.
 */
public class MarineInventoryContainer implements Container {

    /**
     * How close a player has to stay to keep the screen open. Matches the leash vanilla puts on a chest.
     */
    private static final double MAXIMUM_INTERACTION_DISTANCE = 8.0;

    private final Marine marine;

    private final BLibInventory inventory;

    public MarineInventoryContainer(Marine marine) {
        this.marine = marine;
        this.inventory = marine.getInventory();
    }

    @Override
    public int getContainerSize() {
        return inventory.getSize();
    }

    @Override
    public boolean isEmpty() {
        for (var slotIndex = 0; slotIndex < inventory.getSize(); slotIndex++) {
            if (!inventory.isSlotEmpty(slotIndex)) {
                return false;
            }
        }

        return true;
    }

    @Override
    public @NotNull ItemStack getItem(int slotIndex) {
        return inventory.getItemStack(slotIndex);
    }

    @Override
    public @NotNull ItemStack removeItem(int slotIndex, int count) {
        var itemStack = inventory.getItemStack(slotIndex);

        if (itemStack.isEmpty() || count <= 0) {
            return ItemStack.EMPTY;
        }

        var removed = itemStack.split(count);

        // split() mutated the stack in place, so the slot has to be written back even though the object in it did not
        // change - that write is what re-files the entry, and what clears it out of the item index if it is now empty.
        inventory.setItemStack(slotIndex, itemStack);

        return removed;
    }

    @Override
    public @NotNull ItemStack removeItemNoUpdate(int slotIndex) {
        var itemStack = inventory.getItemStack(slotIndex);

        inventory.setItemStack(slotIndex, ItemStack.EMPTY);

        return itemStack;
    }

    @Override
    public void setItem(int slotIndex, @NotNull ItemStack itemStack) {
        inventory.setItemStack(slotIndex, itemStack);
    }

    /**
     * Re-files every slot in the inventory's item index.
     * <p>
     * Menu code is entitled to grow and shrink the live stacks it is handed and then simply report a change, without
     * ever writing the slot back. That leaves the index describing stacks that no longer look the way it thinks, and
     * the index is what {@code hasItem} and the AI's item sensors read - so a marine could end up unable to find gear
     * sitting in its own inventory. Rewriting all twenty-seven slots on a click is cheap next to being wrong.
     */
    @Override
    public void setChanged() {
        for (var slotIndex = 0; slotIndex < inventory.getSize(); slotIndex++) {
            inventory.setItemStack(slotIndex, inventory.getItemStack(slotIndex));
        }
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return marine.isAlive()
            && !marine.isRemoved()
            && marine.distanceToSqr(player) <= MAXIMUM_INTERACTION_DISTANCE * MAXIMUM_INTERACTION_DISTANCE;
    }

    @Override
    public void clearContent() {
        inventory.clear();
    }

    public Marine getMarine() {
        return marine;
    }
}
