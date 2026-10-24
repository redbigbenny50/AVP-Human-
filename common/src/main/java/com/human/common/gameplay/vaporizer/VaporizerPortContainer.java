package com.human.common.gameplay.vaporizer;

import com.human.common.gameplay.block.entity.machine.ElectricMachineBlockEntity;
import net.minecraft.core.Direction;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The vaporizer's item ports, as one of its two blocks shows them to a pipe or hopper. [stated] Oct 17: the item ports
 * are on the TOP block - empty tanks in through its top, filled tanks out of its left and right. The bottom block has
 * windows, the liquid intake and the power socket, and no item port at all.
 * <p>
 * Everything else - which slots, the filter, the recipe check, IN / OUT / BOTH / OFF per side - is the machine's own
 * rule ({@link ElectricMachineBlockEntity#getSlotsForFace}), so the port settings on its screen still apply; they apply
 * to the top block's sides. Its underside touches the bottom block and is never a port.
 * </p>
 */
public final class VaporizerPortContainer implements WorldlyContainer {

    private static final int[] NONE = {};

    private final ElectricMachineBlockEntity machine;

    private final boolean top;

    public VaporizerPortContainer(ElectricMachineBlockEntity machine, boolean top) {
        this.machine = machine;
        this.top = top;
    }

    private boolean isPort(@Nullable Direction side) {
        return top && side != Direction.DOWN;
    }

    @Override
    public int @NotNull [] getSlotsForFace(@NotNull Direction side) {
        return isPort(side) ? machine.getSlotsForFace(side) : NONE;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, @NotNull ItemStack stack, @Nullable Direction side) {
        return (side == null || isPort(side)) && machine.canPlaceItemThroughFace(slot, stack, side);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, @NotNull ItemStack stack, @NotNull Direction side) {
        return isPort(side) && machine.canTakeItemThroughFace(slot, stack, side);
    }

    @Override
    public int getContainerSize() {
        return machine.getContainerSize();
    }

    @Override
    public boolean isEmpty() {
        return machine.isEmpty();
    }

    @Override
    public @NotNull ItemStack getItem(int slot) {
        return machine.getItem(slot);
    }

    @Override
    public @NotNull ItemStack removeItem(int slot, int amount) {
        return machine.removeItem(slot, amount);
    }

    @Override
    public @NotNull ItemStack removeItemNoUpdate(int slot) {
        return machine.removeItemNoUpdate(slot);
    }

    @Override
    public void setItem(int slot, @NotNull ItemStack stack) {
        machine.setItem(slot, stack);
    }

    @Override
    public int getMaxStackSize() {
        return machine.getMaxStackSize();
    }

    @Override
    public boolean canPlaceItem(int slot, @NotNull ItemStack stack) {
        return machine.canPlaceItem(slot, stack);
    }

    @Override
    public void setChanged() {
        machine.setChanged();
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return machine.stillValid(player);
    }

    @Override
    public void clearContent() {
        machine.clearContent();
    }

    /** The machine itself - so a pipe network sees both blocks as one storage. */
    public ElectricMachineBlockEntity machine() {
        return machine;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof VaporizerPortContainer port && port.machine == machine;
    }

    @Override
    public int hashCode() {
        return System.identityHashCode(machine);
    }
}
