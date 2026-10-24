package com.human.common.gameplay.block.uscm;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.TransparentBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import org.jetbrains.annotations.NotNull;

/**
 * The USCM window - connected glass. You place plain 1x1 blocks and one steel frame forms round the whole window.
 * <p>
 * Each block remembers which of its six neighbours is also this window. Each FACE then shows one of 16 textures, picked
 * in the blockstate file from that face's four in-plane neighbours, so the frame is drawn only on outside edges. Faces
 * between two windows are culled by {@link TransparentBlock}'s own same-block check, so a big window has no inner
 * seams. It joins only to itself - never to vanilla glass or another window type.
 * <p>
 * COST: the six flags change only when a neighbour changes ({@link #updateShape}); there is no tick and no block
 * entity.
 */
public class ConnectedWindowBlock extends TransparentBlock {

    public static final MapCodec<ConnectedWindowBlock> CODEC = simpleCodec(ConnectedWindowBlock::new);

    public ConnectedWindowBlock(BlockBehaviour.Properties properties) {
        super(properties);

        var state = stateDefinition.any();

        for (var property : PipeBlock.PROPERTY_BY_DIRECTION.values()) {
            state = state.setValue(property, false);
        }

        registerDefaultState(state);
    }

    @Override
    protected @NotNull MapCodec<? extends TransparentBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        PipeBlock.PROPERTY_BY_DIRECTION.values().forEach(builder::add);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        var level = context.getLevel();
        var pos = context.getClickedPos();
        var state = defaultBlockState();

        for (var direction : Direction.values()) {
            state = state.setValue(property(direction), level.getBlockState(pos.relative(direction)).is(this));
        }

        return state;
    }

    @Override
    protected @NotNull BlockState updateShape(
        @NotNull BlockState state,
        @NotNull Direction direction,
        @NotNull BlockState neighborState,
        @NotNull LevelAccessor level,
        @NotNull BlockPos pos,
        @NotNull BlockPos neighborPos
    ) {
        return state.setValue(property(direction), neighborState.is(this));
    }

    @Override
    protected @NotNull BlockState rotate(@NotNull BlockState state, @NotNull Rotation rotation) {
        var rotated = state;

        for (var direction : Direction.Plane.HORIZONTAL) {
            rotated = rotated.setValue(property(rotation.rotate(direction)), state.getValue(property(direction)));
        }

        return rotated;
    }

    @Override
    protected @NotNull BlockState mirror(@NotNull BlockState state, @NotNull Mirror mirror) {
        var mirrored = state;

        for (var direction : Direction.Plane.HORIZONTAL) {
            mirrored = mirrored.setValue(property(mirror.mirror(direction)), state.getValue(property(direction)));
        }

        return mirrored;
    }

    private static BooleanProperty property(Direction direction) {
        return PipeBlock.PROPERTY_BY_DIRECTION.get(direction);
    }
}
