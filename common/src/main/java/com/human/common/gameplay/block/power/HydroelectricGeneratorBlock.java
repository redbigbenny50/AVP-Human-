package com.human.common.gameplay.block.power;

import com.human.common.gameplay.block.entity.power.impl.HydroelectricGeneratorBlockEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * One section of a hydroelectric wheel. [stated] Sep 24: "the place looking at the player water can flow from the front
 * or the back", sections "can space directly next to each other", and "only the middle block is solid".
 * <p>
 * ⭐ THE FINS ARE NOT SOLID AND THAT IS DELIBERATE. The model's blades reach roughly two blocks out; the collision and
 * occlusion shape is the CENTRE CUBE ONLY, so water flows through the blades instead of round them and a player can
 * crawl through, exactly as specified. The visual overhang is handled by the renderer's bounding box, not by the shape.
 * </p>
 */
public class HydroelectricGeneratorBlock extends PowerNodeEntityBlock<HydroelectricGeneratorBlockEntity> {

    public static final MapCodec<HydroelectricGeneratorBlock> CODEC = simpleCodec(HydroelectricGeneratorBlock::new);

    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    /** Only the centre cube is solid - the blades are decoration the world passes straight through. */
    private static final VoxelShape SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 16.0, 16.0);

    public HydroelectricGeneratorBlock(Properties properties) {
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

    @Override
    protected @NotNull VoxelShape getShape(
        @NotNull BlockState state,
        @NotNull BlockGetter level,
        @NotNull BlockPos pos,
        @NotNull CollisionContext context
    ) {
        return SHAPE;
    }

    @Override
    protected @NotNull MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(@NotNull BlockPos blockPos, @NotNull BlockState blockState) {
        return new HydroelectricGeneratorBlockEntity(blockPos, blockState);
    }
}
