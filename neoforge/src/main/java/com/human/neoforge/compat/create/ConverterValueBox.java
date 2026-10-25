package com.human.neoforge.compat.create;

import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** Create's value box for the electric gearbox's speed, on its blank FRONT - the only face with no port. */
final class ConverterValueBox extends ValueBoxTransform.Sided {

    @Override
    protected Vec3 getSouthLocation() {
        return VecHelper.voxelSpace(8, 8, 16.05);
    }

    @Override
    protected boolean isSideActive(BlockState state, Direction direction) {
        return state.hasProperty(HorizontalKineticBlock.HORIZONTAL_FACING)
            && direction == state.getValue(HorizontalKineticBlock.HORIZONTAL_FACING);
    }
}
