package com.human.common.gameplay.block.creative;

import com.human.common.gameplay.block.entity.power.PowerNodeBlockEntity;
import com.human.common.gameplay.power.PowerNode;
import com.human.common.registry.init.HumanCreativeBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/**
 * [stated] Oct 20 - the Creative Battery: a store that is always full and never fills. It gives its grid unlimited
 * power, like the Creative Generator, and also swallows any amount sent into it - so generators can be run into it and
 * tested without anything ever filling up. Creative only: no recipe, no screen, no upgrades.
 */
public class CreativeBatteryBlockEntity extends PowerNodeBlockEntity implements PowerNode.PowerStore {

    /** "Unlimited", kept well clear of overflow when the grid sums several stores. */
    private static final long BOTTOMLESS = 1_000_000_000L;

    public CreativeBatteryBlockEntity(BlockPos pos, BlockState state) {
        super(HumanCreativeBlocks.CREATIVE_BATTERY_BLOCK_ENTITY.get(), pos, state);
    }

    /**
     * Last in line both ways: real batteries on the grid charge from spare generation before this swallows the rest,
     * and they discharge first before this takes over. It never runs dry, so being last costs nothing.
     */
    @Override
    public int getStorePriority() {
        return STORE_RESERVE;
    }

    @Override
    public long getStoredPower() {
        return BOTTOMLESS;
    }

    @Override
    public long getCapacity() {
        return BOTTOMLESS * 2;
    }

    @Override
    public long getAvailablePower() {
        return BOTTOMLESS;
    }

    /** Gives exactly what was asked for, forever. */
    @Override
    public long extractPower(long maxAmount) {
        return maxAmount;
    }

    @Override
    public long getRequestedPower() {
        return BOTTOMLESS;
    }

    /** Takes everything offered and destroys it. */
    @Override
    public long receivePower(long maxAmount) {
        return maxAmount;
    }
}
