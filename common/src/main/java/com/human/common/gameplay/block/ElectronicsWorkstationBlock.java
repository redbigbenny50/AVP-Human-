package com.human.common.gameplay.block;

import com.human.common.gameplay.block.entity.workstation.ElectronicsWorkstationBlockEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The electronics workstation. [stated] Sep 29/30: an UNPOWERED station for electronic components - pick what to make
 * from a list, and it builds it from what its storage holds, crafting any missing components from raw materials. See
 * {@link com.human.common.gameplay.workstation.ElectronicsCrafting}. It is also the electrician colonist's job site.
 * <p>
 * No ticker: it does nothing until someone takes from its result slot.
 * </p>
 */
public class ElectronicsWorkstationBlock extends HorizontalDirectionalBlock implements EntityBlock {

    public static final MapCodec<ElectronicsWorkstationBlock> CODEC = simpleCodec(ElectronicsWorkstationBlock::new);

    public ElectronicsWorkstationBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected @NotNull InteractionResult useWithoutItem(
        @NotNull BlockState state,
        @NotNull Level level,
        @NotNull BlockPos pos,
        @NotNull Player player,
        @NotNull BlockHitResult hitResult
    ) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof ElectronicsWorkstationBlockEntity workstation) {
            player.openMenu(workstation);
        }

        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void onRemove(
        @NotNull BlockState state,
        @NotNull Level level,
        @NotNull BlockPos pos,
        @NotNull BlockState newState,
        boolean movedByPiston
    ) {
        // ⚠ Only when the block goes - turning it with a wrench-style rotate keeps the stock.
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof ElectronicsWorkstationBlockEntity workstation) {
            Containers.dropContents(level, pos, workstation.getItems());
            level.updateNeighbourForOutputSignal(pos, this);
        }

        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new ElectronicsWorkstationBlockEntity(pos, state);
    }

    @Override
    protected @NotNull MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }
}
