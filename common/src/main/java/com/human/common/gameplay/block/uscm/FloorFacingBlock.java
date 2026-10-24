package com.human.common.gameplay.block.uscm;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import org.jetbrains.annotations.NotNull;

/**
 * A floor block whose pattern has a direction (an arrow, a painted line, a trench, a stair tread). It faces the way the
 * player is looking when it is placed; the blockstate file turns the model to match, which turns only the top and
 * bottom faces' pattern - the sides look the same either way.
 */
public class FloorFacingBlock extends HorizontalDirectionalBlock {

    public static final MapCodec<FloorFacingBlock> CODEC = simpleCodec(FloorFacingBlock::new);

    public FloorFacingBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, net.minecraft.core.Direction.NORTH));
    }

    @Override
    protected @NotNull MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection());
    }
}
