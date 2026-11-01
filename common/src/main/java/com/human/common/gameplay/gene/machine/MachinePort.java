package com.human.common.gameplay.gene.machine;

import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.BooleanSupplier;
import java.util.function.Predicate;

/**
 * A genetic machine's item port as pipes and hoppers see it ([stated] Oct 27: the Separator takes empty tubes, the
 * Splicer empty syringes): items IN only, through one face, into the given slots, and only items the filter accepts.
 * Nothing comes out. With no port face it is closed on every side.
 */
public record MachinePort(
    Container items,
    @Nullable Direction portFace,
    int[] slots,
    Predicate<ItemStack> accepts,
    BooleanSupplier valid
) implements WorldlyContainer {

    private static final int[] NONE = {};

    /** For the blocks of a machine that are not its port. */
    public static final MachinePort CLOSED = new MachinePort(new SimpleContainer(0), null, NONE, stack -> false, () -> false);

    private boolean isPortSlot(int slot) {
        for (var s : slots) {
            if (s == slot) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int @NotNull [] getSlotsForFace(@NotNull Direction side) {
        return portFace != null && side == portFace ? slots : NONE;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, @NotNull ItemStack stack, @Nullable Direction side) {
        return portFace != null && (side == null || side == portFace) && isPortSlot(slot) && accepts.test(stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, @NotNull ItemStack stack, @NotNull Direction side) {
        return false;
    }

    @Override
    public int getContainerSize() {
        return items.getContainerSize();
    }

    @Override
    public boolean isEmpty() {
        return items.isEmpty();
    }

    @Override
    public @NotNull ItemStack getItem(int slot) {
        return items.getItem(slot);
    }

    @Override
    public @NotNull ItemStack removeItem(int slot, int amount) {
        return items.removeItem(slot, amount);
    }

    @Override
    public @NotNull ItemStack removeItemNoUpdate(int slot) {
        return items.removeItemNoUpdate(slot);
    }

    @Override
    public void setItem(int slot, @NotNull ItemStack stack) {
        items.setItem(slot, stack);
    }

    @Override
    public boolean canPlaceItem(int slot, @NotNull ItemStack stack) {
        return isPortSlot(slot) && accepts.test(stack);
    }

    @Override
    public void setChanged() {
        items.setChanged();
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return valid.getAsBoolean();
    }

    @Override
    public void clearContent() {}
}
