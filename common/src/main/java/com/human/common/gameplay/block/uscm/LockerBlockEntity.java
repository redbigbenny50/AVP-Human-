package com.human.common.gameplay.block.uscm;

import com.human.common.registry.init.block.HumanUscmBlocks;
import com.human.common.registry.init.block.HumanWyBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.ContainerOpenersCounter;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

/**
 * The inventory behind a USCM or W-Y locker: 27 slots, opened and closed exactly like a barrel (the same opener
 * counting, so the door shuts on its own when the last viewer leaves).
 * <p>
 * Each set has its own block entity type, and a type only accepts its own blocks, so the constructor picks the type
 * from the block it is built for.
 */
public class LockerBlockEntity extends RandomizableContainerBlockEntity {

    public static final int SIZE = 27;

    private NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);

    private final ContainerOpenersCounter openersCounter = new ContainerOpenersCounter() {

        @Override
        protected void onOpen(@NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState state) {
            playSound(state, SoundEvents.BARREL_OPEN);
            updateBlockState(state, true);
        }

        @Override
        protected void onClose(@NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState state) {
            playSound(state, SoundEvents.BARREL_CLOSE);
            updateBlockState(state, false);
        }

        @Override
        protected void openerCountChanged(
            @NotNull Level level,
            @NotNull BlockPos pos,
            @NotNull BlockState state,
            int count,
            int openCount
        ) {}

        @Override
        protected boolean isOwnContainer(@NotNull Player player) {
            return player.containerMenu instanceof ChestMenu menu && menu.getContainer() == LockerBlockEntity.this;
        }
    };

    public LockerBlockEntity(BlockPos pos, BlockState state) {
        super(typeFor(state), pos, state);
    }

    /** The W-Y locker uses the W-Y type; the two USCM lockers keep the USCM type. */
    private static BlockEntityType<?> typeFor(BlockState state) {
        return state.is(HumanWyBlocks.LOCKER.get())
            ? HumanWyBlocks.LOCKER_BLOCK_ENTITY.get()
            : HumanUscmBlocks.LOCKER_BLOCK_ENTITY.get();
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.saveAdditional(tag, registries);

        if (!trySaveLootTable(tag)) {
            net.minecraft.world.ContainerHelper.saveAllItems(tag, items, registries);
        }
    }

    @Override
    protected void loadAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.loadAdditional(tag, registries);
        items = NonNullList.withSize(getContainerSize(), ItemStack.EMPTY);

        if (!tryLoadLootTable(tag)) {
            net.minecraft.world.ContainerHelper.loadAllItems(tag, items, registries);
        }
    }

    @Override
    public int getContainerSize() {
        return SIZE;
    }

    @Override
    protected @NotNull NonNullList<ItemStack> getItems() {
        return items;
    }

    @Override
    protected void setItems(@NotNull NonNullList<ItemStack> items) {
        this.items = items;
    }

    @Override
    protected @NotNull Component getDefaultName() {
        // The block's own name: "USCM Locker" / "USCM Weapon Locker".
        return Component.translatable(getBlockState().getBlock().getDescriptionId());
    }

    @Override
    protected @NotNull AbstractContainerMenu createMenu(int id, @NotNull Inventory inventory) {
        return ChestMenu.threeRows(id, inventory, this);
    }

    @Override
    public void startOpen(@NotNull Player player) {
        if (!remove && !player.isSpectator() && level != null) {
            openersCounter.incrementOpeners(player, level, getBlockPos(), getBlockState());
        }
    }

    @Override
    public void stopOpen(@NotNull Player player) {
        if (!remove && !player.isSpectator() && level != null) {
            openersCounter.decrementOpeners(player, level, getBlockPos(), getBlockState());
        }
    }

    public void recheckOpen() {
        if (!remove && level != null) {
            openersCounter.recheckOpeners(level, getBlockPos(), getBlockState());
        }
    }

    private void updateBlockState(BlockState state, boolean open) {
        if (level != null && state.hasProperty(LockerBlock.OPEN)) {
            level.setBlock(getBlockPos(), state.setValue(LockerBlock.OPEN, open), 3);
        }
    }

    private void playSound(BlockState state, SoundEvent sound) {
        if (level == null || !state.hasProperty(LockerBlock.FACING)) {
            return;
        }

        var normal = state.getValue(LockerBlock.FACING).getNormal();
        var x = worldPosition.getX() + 0.5 + normal.getX() / 2.0;
        var y = worldPosition.getY() + 0.5;
        var z = worldPosition.getZ() + 0.5 + normal.getZ() / 2.0;

        level.playSound(null, x, y, z, sound, SoundSource.BLOCKS, 0.5F, level.random.nextFloat() * 0.1F + 0.9F);
    }
}
