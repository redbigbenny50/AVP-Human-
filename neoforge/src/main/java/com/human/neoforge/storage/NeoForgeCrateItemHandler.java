package com.human.neoforge.storage;

import com.human.common.gameplay.storage.CrateInventory;
import com.human.common.registry.init.HumanCrates;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;

/**
 * A crate as a NeoForge item handler, for hoppers and other mods. Every block of a crate answers with its master's
 * inventory, 64 per slot for anything. Taking out never returns more than the item's normal stack size, so other mods
 * always receive legal stacks.
 */
public final class NeoForgeCrateItemHandler implements IItemHandler {

    private final CrateInventory inventory;

    private NeoForgeCrateItemHandler(CrateInventory inventory) {
        this.inventory = inventory;
    }

    public static void register(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
            Capabilities.ItemHandler.BLOCK,
            HumanCrates.CRATE_BLOCK_ENTITY.get(),
            (crate, side) -> {
                var master = crate.master();

                return master == null ? null : master.<IItemHandler>loaderView(NeoForgeCrateItemHandler::new);
            }
        );
    }

    @Override
    public int getSlots() {
        return inventory.size();
    }

    @Override
    public @NotNull ItemStack getStackInSlot(int slot) {
        return inventory.get(slot);
    }

    @Override
    public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
        return inventory.insertSlot(slot, stack, simulate);
    }

    @Override
    public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
        return inventory.extractSlot(slot, amount, simulate);
    }

    @Override
    public int getSlotLimit(int slot) {
        return CrateInventory.SLOT_LIMIT;
    }

    @Override
    public boolean isItemValid(int slot, @NotNull ItemStack stack) {
        return CrateInventory.canStore(stack);
    }
}
