package com.human.common.gameplay.block.entity.power.impl;

import com.human.common.gameplay.power.ItemEnergyBridge;
import com.human.common.gameplay.power.StackSlot;
import com.human.common.registry.init.HumanBlockEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

/**
 * The charging hub: a smaller store that also charges an item resting on its angled plate.
 * <p>
 * ⭐ [stated] Sep 29: "the battery thats built in the mdk currently is being changed to the charging_hub its kind of
 * like a battery but it also charges items too it would store about 500k power." This is that block - the old ESU's
 * model, art and plate, moved under a new name, at a third of its old capacity. The full-size store is now
 * {@link BatteryBlockEntity}.
 * </p>
 */
public class ChargingHubBlockEntity extends PowerStorageBlockEntity {

    public static final long CAPACITY = 500_000L;

    public static final long MAX_TRANSFER_RATE = 5_000L;

    private static final String NBT_PAD_ITEM = "padItem";

    /** FE per tick offered to the item on the plate - a slow trickle, so the plate never competes with the grid. */
    private static final long PAD_CHARGE_RATE = 256L;

    private ItemStack padItem = ItemStack.EMPTY;

    public ChargingHubBlockEntity(BlockPos pos, BlockState state) {
        super(HumanBlockEntityTypes.CHARGING_HUB.get(), pos, state, CAPACITY, MAX_TRANSFER_RATE);
    }

    public ItemStack getPadItem() {
        return padItem;
    }

    public ItemStack swapPadItem(ItemStack stack) {
        var previous = padItem;

        padItem = stack == null ? ItemStack.EMPTY : stack.copyWithCount(1);

        setChanged();

        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }

        return previous;
    }

    private void tickPad() {
        if (padItem.isEmpty() || getStoredPower() <= 0L) {
            return;
        }

        var offer = Math.min(PAD_CHARGE_RATE, Math.min(getStoredPower(), MAX_TRANSFER_RATE));
        var accepted = ItemEnergyBridge.charge(new StackSlot(() -> padItem, updated -> padItem = updated), offer);

        if (accepted <= 0L) {
            return;
        }

        // ⚠ Drained through the base so the charge change is recorded exactly once, however the item took it.
        drainStored(accepted);
    }

    @Override
    public void serverTick(Level level, BlockPos blockPos, BlockState blockState) {
        super.serverTick(level, blockPos, blockState);
        tickPad();
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.saveAdditional(tag, registries);

        if (!padItem.isEmpty()) {
            tag.put(NBT_PAD_ITEM, padItem.save(registries));
        }
    }

    @Override
    protected void loadAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.loadAdditional(tag, registries);
        padItem = tag.contains(NBT_PAD_ITEM, net.minecraft.nbt.Tag.TAG_COMPOUND)
            ? ItemStack.parseOptional(registries, tag.getCompound(NBT_PAD_ITEM))
            : ItemStack.EMPTY;
    }
}
