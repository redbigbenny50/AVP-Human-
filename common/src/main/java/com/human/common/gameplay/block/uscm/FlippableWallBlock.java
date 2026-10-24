package com.human.common.gameplay.block.uscm;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import org.jetbrains.annotations.NotNull;

/**
 * The bordered composite wall: its steel kick strip sits at the bottom, or at the TOP when placed upside down.
 * <p>
 * Placement reads like a slab: clicking the underside of a block, or the upper half of a side, places it flipped. The
 * blockstate file turns the model 180 degrees for {@code flipped=true}.
 */
public class FlippableWallBlock extends Block {

    public static final MapCodec<FlippableWallBlock> CODEC = simpleCodec(FlippableWallBlock::new);

    public static final BooleanProperty FLIPPED = BooleanProperty.create("flipped");

    public FlippableWallBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FLIPPED, false));
    }

    @Override
    protected @NotNull MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FLIPPED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        var face = context.getClickedFace();
        var flipped = face == Direction.DOWN
            || face != Direction.UP && context.getClickLocation().y - context.getClickedPos().getY() > 0.5;

        return defaultBlockState().setValue(FLIPPED, flipped);
    }
}
