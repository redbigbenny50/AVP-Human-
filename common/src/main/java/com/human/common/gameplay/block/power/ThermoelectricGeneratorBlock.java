package com.human.common.gameplay.block.power;

import com.human.common.gameplay.block.entity.power.impl.ThermoelectricGeneratorBlockEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** The Thermoelectric Generator - the thermal generator renamed, with plates on five sides and the fan on top. */
public class ThermoelectricGeneratorBlock extends PowerNodeEntityBlock<ThermoelectricGeneratorBlockEntity> {

    public static final MapCodec<ThermoelectricGeneratorBlock> CODEC = simpleCodec(ThermoelectricGeneratorBlock::new);

    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    public ThermoelectricGeneratorBlock(Properties properties) {
        super(properties);
        registerDefaultState(
            stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(PowerMode.PROPERTY, PowerMode.OFF)
        );
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, PowerMode.PROPERTY);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected @NotNull BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected @NotNull BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    /** Oct 21 - lava, fire or a furnace placed, removed or lit beside it: the plates are re-read on the next tick. */
    @Override
    protected void neighborChanged(
        @NotNull BlockState state,
        @NotNull net.minecraft.world.level.Level level,
        @NotNull BlockPos pos,
        @NotNull Block neighbourBlock,
        @NotNull BlockPos neighbourPos,
        boolean movedByPiston
    ) {
        super.neighborChanged(state, level, pos, neighbourBlock, neighbourPos, movedByPiston);

        if (
            !level.isClientSide
                && level.getBlockEntity(
                    pos
                ) instanceof com.human.common.gameplay.block.entity.power.impl.ThermoelectricGeneratorBlockEntity generator
        ) {
            generator.neighbourChanged();
        }
    }

    @Override
    protected @NotNull MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(@NotNull BlockPos blockPos, @NotNull BlockState blockState) {
        return new ThermoelectricGeneratorBlockEntity(blockPos, blockState);
    }
}
