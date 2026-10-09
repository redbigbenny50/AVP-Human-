package com.human.common.gameplay.block.entity.workstation;

import com.human.common.gameplay.menu.workstation.ElectronicsWorkstationMenu;
import com.human.common.registry.init.HumanBlockEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The electronics workstation's storage: {@value #SIZE} slots of materials and finished components it builds from. A
 * workstation saved with a smaller storage loads into the first slots - nothing is lost. [stated] Sep 30: UNPOWERED -
 * it has no tick and draws nothing.
 * <p>
 * Hoppers may fill it from any face but never empty it: the storage is the workstation's stock, and a hopper under it
 * draining the silicon would only get in the way. What it makes goes to the player through the result slot.
 * </p>
 */
public class ElectronicsWorkstationBlockEntity extends BlockEntity implements WorldlyContainer, MenuProvider {

    /**
     * [stated] Sep 30: at least a double chest's worth - it keeps every surplus a craft makes, so it needs the room. 56
     * = seven rows of eight, because the screen shows it eight wide with its scrollbar in the ninth column.
     */
    public static final int SIZE = 56;

    private static final int[] ALL_SLOTS = new int[SIZE];

    static {
        for (var index = 0; index < SIZE; index++) {
            ALL_SLOTS[index] = index;
        }
    }

    private final NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);

    public ElectronicsWorkstationBlockEntity(BlockPos pos, BlockState state) {
        super(HumanBlockEntityTypes.ELECTRONICS_WORKSTATION.get(), pos, state);
    }

    public NonNullList<ItemStack> getItems() {
        return items;
    }

    @Override
    public int @NotNull [] getSlotsForFace(@NotNull Direction side) {
        return ALL_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, @NotNull ItemStack stack, @Nullable Direction direction) {
        return true;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, @NotNull ItemStack stack, @NotNull Direction direction) {
        return false;
    }

    @Override
    public int getContainerSize() {
        return SIZE;
    }

    @Override
    public boolean isEmpty() {
        for (var stack : items) {
            if (!stack.isEmpty()) {
                return false;
            }
        }

        return true;
    }

    @Override
    public @NotNull ItemStack getItem(int slot) {
        return items.get(slot);
    }

    @Override
    public @NotNull ItemStack removeItem(int slot, int amount) {
        var removed = ContainerHelper.removeItem(items, slot, amount);

        if (!removed.isEmpty()) {
            setChanged();
        }

        return removed;
    }

    @Override
    public @NotNull ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(items, slot);
    }

    @Override
    public void setItem(int slot, @NotNull ItemStack stack) {
        items.set(slot, stack);
        stack.limitSize(getMaxStackSize(stack));
        setChanged();
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        items.clear();
    }

    @Override
    public @NotNull Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, @NotNull Inventory inventory, @NotNull Player player) {
        return new ElectronicsWorkstationMenu(containerId, inventory, this);
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, items, registries);
    }

    @Override
    protected void loadAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.loadAdditional(tag, registries);
        items.clear();
        ContainerHelper.loadAllItems(tag, items, registries);
    }
}
