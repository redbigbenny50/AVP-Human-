package com.human.common.gameplay.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class CryotubeBlock extends Block {

    private static final Direction[] VALUES = Direction.values();

    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    public static final BooleanProperty NORTH = BlockStateProperties.NORTH;

    public static final BooleanProperty SOUTH = BlockStateProperties.SOUTH;

    public static final BooleanProperty EAST = BlockStateProperties.EAST;

    public static final BooleanProperty WEST = BlockStateProperties.WEST;

    public static final BooleanProperty UP = BlockStateProperties.UP;

    public static final BooleanProperty DOWN = BlockStateProperties.DOWN;

    public static final Map<Direction, BooleanProperty> PROPERTY_BY_DIRECTION = Map.of(
        Direction.NORTH,
        NORTH,
        Direction.SOUTH,
        SOUTH,
        Direction.EAST,
        EAST,
        Direction.WEST,
        WEST,
        Direction.UP,
        UP,
        Direction.DOWN,
        DOWN
    );

    public CryotubeBlock(Properties properties) {
        super(properties);

        registerDefaultState(
            this.defaultBlockState()
                .setValue(FACING, Direction.NORTH)
                .setValue(NORTH, false)
                .setValue(SOUTH, false)
                .setValue(EAST, false)
                .setValue(WEST, false)
                .setValue(UP, false)
                .setValue(DOWN, false)
        );
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        var level = context.getLevel();
        var pos = context.getClickedPos();
        var state = this.defaultBlockState()
            .setValue(FACING, context.getHorizontalDirection().getOpposite());

        for (var direction : VALUES) {
            var neighborPos = pos.relative(direction);
            state = state.setValue(PROPERTY_BY_DIRECTION.get(direction), canConnectTo(level, neighborPos));
        }

        return state;
    }

    @Override
    public @NotNull BlockState updateShape(
        BlockState state,
        @NotNull Direction direction,
        @NotNull BlockState neighborState,
        @NotNull LevelAccessor level,
        @NotNull BlockPos pos,
        @NotNull BlockPos neighborPos
    ) {
        return state.setValue(PROPERTY_BY_DIRECTION.get(direction), canConnectTo(neighborState));
    }

    @Override
    protected @NotNull BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected @NotNull BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, NORTH, SOUTH, EAST, WEST, UP, DOWN);
    }

    private boolean canConnectTo(LevelAccessor level, BlockPos neighbor) {
        return canConnectTo(level.getBlockState(neighbor));
    }

    private boolean canConnectTo(BlockState neighborState) {
        return neighborState.getBlock() instanceof CryotubeBlock;
    }
}
