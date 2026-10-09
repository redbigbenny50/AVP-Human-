package com.human.common.gameplay.block.fluid;

import com.human.common.gameplay.block.pipe.PipeBlock;
import com.human.common.gameplay.fluid.FluidConnections;
import com.human.common.gameplay.fluid.pipe.FluidPipeSystem;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

/**
 * The fluid pipe - everything visual and the flange click is {@link PipeBlock}. Its ports are anything that holds fluid
 * on that face: tanks, canisters, machines, other mods' fluid blocks. Its network is {@link FluidPipeSystem}.
 */
public class FluidPipeBlock extends PipeBlock {

    public static final MapCodec<FluidPipeBlock> CODEC = simpleCodec(FluidPipeBlock::new);

    public FluidPipeBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected boolean joins(BlockState neighbour) {
        return neighbour.getBlock() instanceof FluidPipeBlock;
    }

    @Override
    protected boolean isPort(Level level, BlockPos pos, Direction side) {
        return FluidConnections.exposesFluid(level, pos, side);
    }

    /**
     * Oct 7 - open fluid in the world: any fluid that can be scooped (a liquid block, or a waterlogged block). See
     * PipeBlock.compute and FluidIntakePort.
     */
    @Override
    protected boolean isOpenEndPort(Level level, BlockPos pos) {
        return !level.getFluidState(pos).isEmpty()
            && level.getBlockState(pos).getBlock() instanceof net.minecraft.world.level.block.BucketPickup;
    }

    @Override
    protected BlockEntity newPortEntity(BlockPos pos, BlockState state) {
        return new FluidPipeBlockEntity(pos, state);
    }

    @Override
    protected void invalidate(ServerLevel level, BlockPos pos) {
        FluidPipeSystem.get(level).invalidate(pos);
    }

    @Override
    protected @NotNull MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }
}
