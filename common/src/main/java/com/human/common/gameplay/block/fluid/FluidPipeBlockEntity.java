package com.human.common.gameplay.block.fluid;

import com.human.common.gameplay.block.pipe.PipeBlockEntity;
import com.human.common.gameplay.fluid.pipe.FluidPipeSystem;
import com.human.common.registry.init.HumanBlockEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A fluid pipe that touches a port: its flange colours and end settings ({@link PipeBlockEntity}), and how the fluid
 * network finds its pipes when a world loads - plain pipes have no block entity at all.
 */
public class FluidPipeBlockEntity extends PipeBlockEntity {

    public FluidPipeBlockEntity(BlockPos pos, BlockState state) {
        super(HumanBlockEntityTypes.FLUID_PIPE.get(), pos, state);
    }

    /**
     * Oct 7 - per face, the game time an intake end (FluidIntakePort) may take its next bucket. Not saved: after a
     * reload the first bucket simply comes straight away.
     */
    private final long[] intakeReadyAt = new long[6];

    public long intakeReadyAt(net.minecraft.core.Direction face) {
        return intakeReadyAt[face.get3DDataValue()];
    }

    public void setIntakeReadyAt(net.minecraft.core.Direction face, long gameTime) {
        intakeReadyAt[face.get3DDataValue()] = gameTime;
    }

    /** Vanilla calls this as the block entity joins the level - on placement and on chunk load alike. */
    @Override
    public void clearRemoved() {
        super.clearRemoved();

        if (level instanceof ServerLevel serverLevel) {
            FluidPipeSystem.get(serverLevel).registerPortPipe(worldPosition);
        }
    }

    @Override
    public void setRemoved() {
        if (level instanceof ServerLevel serverLevel) {
            FluidPipeSystem.get(serverLevel).unregisterPortPipe(worldPosition);
        }

        super.setRemoved();
    }
}
