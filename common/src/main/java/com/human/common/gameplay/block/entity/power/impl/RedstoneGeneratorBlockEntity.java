package com.human.common.gameplay.block.entity.power.impl;

import com.human.common.gameplay.block.entity.power.PowerNodeBlockEntity;
import com.human.common.gameplay.power.PowerNode;
import com.human.common.registry.init.HumanBlockEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The redstone generator. [stated] Oct 3: an infinite power source, but a SMALL one - 16 FE a tick, less than a solar
 * panel's dimmest output, with no fuel and no conditions. Enough for one low-power device; its main use is the dropship
 * plugin's terminal, oxygen machine and the like, which are sized to draw no more than this.
 * <p>
 * It works like every other producer: it offers {@link #OUTPUT} each tick and the grid takes what it needs, up to that.
 * </p>
 */
public class RedstoneGeneratorBlockEntity extends PowerNodeBlockEntity implements PowerNode.PowerProducer {

    /** FE per tick, forever. */
    public static final long OUTPUT = 16L;

    public RedstoneGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(HumanBlockEntityTypes.REDSTONE_GENERATOR.get(), pos, state);
    }

    @Override
    public long getAvailablePower() {
        return level == null || level.isClientSide ? 0L : OUTPUT;
    }

    @Override
    public long extractPower(long maxAmount) {
        // Never more than its output, whatever is asked.
        return Math.max(0L, Math.min(maxAmount, OUTPUT));
    }
}
