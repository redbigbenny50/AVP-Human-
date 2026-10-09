package com.human.common.gameplay.power;

import net.minecraft.world.item.ItemStack;

import java.util.function.Consumer;

/**
 * One item slot, as something an energy API can write BACK through.
 * <h2>⚠⚠ WHY A BARE ItemStack IS NOT ENOUGH</h2> NeoForge's energy capability mutates the stack in place, so a bare
 * stack works there - which is exactly why the first version of this bridge took one, and why the gap did not show up
 * until Fabric.
 * <p>
 * Fabric's API cannot. {@code EnergyStorage.ITEM} is an {@code ItemApiLookup} keyed by {@code ContainerItemContext},
 * and the context is how the MODIFIED stack gets back to its owner. Handing it
 * {@code ContainerItemContext.withConstant(stack)} compiles, runs, reports a successful insert - and drops the charge,
 * because that context backs itself with its own throwaway storage. The pad would look like it was charging and forget
 * on reload.
 * </p>
 * <p>
 * ⭐ Oritech solves it the same way and it is worth recording: their {@code StackContext} is a stack plus a
 * {@code Consumer<ItemStack>} updater plus a {@code sync()}, and their machines never touch Fabric's context type at
 * all - it is built at the loader boundary. This is that, under our own names.
 * </p>
 */
public final class StackSlot {

    private final Supplier stack;

    private final Consumer<ItemStack> writeBack;

    public StackSlot(Supplier stack, Consumer<ItemStack> writeBack) {
        this.stack = stack;
        this.writeBack = writeBack;
    }

    /** A slot that can be read but never written - for a query where no transfer will happen. */
    public static StackSlot readOnly(ItemStack stack) {
        return new StackSlot(() -> stack, ignored -> {});
    }

    public ItemStack get() {
        var current = stack.get();

        return current == null ? ItemStack.EMPTY : current;
    }

    /** ⚠ Call after ANY successful transfer, even when the loader mutated in place - it is a no-op if it did. */
    public void set(ItemStack updated) {
        writeBack.accept(updated == null ? ItemStack.EMPTY : updated);
    }

    @FunctionalInterface
    public interface Supplier {

        ItemStack get();
    }
}
