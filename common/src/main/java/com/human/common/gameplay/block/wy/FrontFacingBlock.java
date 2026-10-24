package com.human.common.gameplay.block.wy;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import org.jetbrains.annotations.NotNull;

/**
 * A W-Y block with a FRONT (a console, a monitor bank, a wall screen, a rack, a keypad or alarm pillar).
 * <p>
 * [stated] Oct 20: blocks with a front face the player when placed; everything else is a plain cube. The front is drawn
 * on the model's north face ({@code minecraft:block/orientable}); the blockstate file turns the model so that face
 * points back at whoever placed it. Only the four horizontal directions - these stand in walls.
 * <p>
 * COST: one direction property, no block entity, no tick.
 */
public class FrontFacingBlock extends HorizontalDirectionalBlock {

    public static final MapCodec<FrontFacingBlock> CODEC = simpleCodec(FrontFacingBlock::new);

    public FrontFacingBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
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
        // The player looks INTO the wall, so the front points the opposite way: back at them.
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }
}
