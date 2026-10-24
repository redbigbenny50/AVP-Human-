package com.human.common.gameplay.block.cryo;

import com.human.common.gameplay.block.power.PowerNodeEntityBlock;
import com.human.common.gameplay.fluid.FluidContainerInteractions;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The cryotube controller. [stated] Sep 30 - Oct 1: one machine block per edition (ferroaluminum, steel, W-Y, USCM),
 * with the ports fixed by its facing - TOP takes cryofluid in, BACK takes power, LEFT, RIGHT and BOTTOM are cryoconduit
 * ports. The front is the screen.
 * <p>
 * Right-click opens its screen; sneak-right-click on the back toggles the power breaker like every other machine;
 * right-click with a bucket or canister of cryofluid fills it.
 * </p>
 */
public class CryotubeControllerBlock extends PowerNodeEntityBlock<CryotubeControllerBlockEntity> implements CryoConduitEndpoint {

    public static final MapCodec<CryotubeControllerBlock> CODEC = simpleCodec(CryotubeControllerBlock::new);

    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    public CryotubeControllerBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected @NotNull MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
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
    protected @NotNull BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected @NotNull BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected @NotNull RenderShape getRenderShape(@NotNull BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new CryotubeControllerBlockEntity(pos, state);
    }

    /** Left, right and bottom. Never the front (screen), back (power) or top (cryofluid). */
    /**
     * Breaking the controller drops its batteries and hub chip, like every other machine. ⚠ Only when the BLOCK goes:
     * turning it (a FACING change) is the same block and must not spill anything. Unloading never calls this.
     */
    @Override
    protected void onRemove(
        @NotNull BlockState state,
        @NotNull Level level,
        @NotNull BlockPos pos,
        @NotNull BlockState newState,
        boolean movedByPiston
    ) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof CryotubeControllerBlockEntity controller) {
            Containers.dropContents(level, pos, controller.getBatteries());
            Containers.dropContents(level, pos, controller.getHub());
        }

        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public boolean acceptsCryoConduit(BlockState state, Direction face) {
        if (face == Direction.DOWN) {
            return true;
        }

        var facing = state.getValue(FACING);

        return face.getAxis().isHorizontal() && face != facing && face != facing.getOpposite();
    }

    @Override
    protected @NotNull ItemInteractionResult useItemOn(
        @NotNull ItemStack stack,
        @NotNull BlockState state,
        @NotNull Level level,
        @NotNull BlockPos pos,
        @NotNull Player player,
        @NotNull InteractionHand hand,
        @NotNull BlockHitResult hitResult
    ) {
        // Oct 10 - the wiring tool always reaches its own logic (readout, fuse binding, toggles) - see
        // PowerNodeEntityBlock#useItemOn.
        if (isWiringTool(stack)) {
            return net.minecraft.world.ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
        }

        if (!player.isShiftKeyDown() && level.getBlockEntity(pos) instanceof CryotubeControllerBlockEntity controller) {
            var result = FluidContainerInteractions.useItem(stack, level, pos, player, hand, controller);

            if (result != ItemInteractionResult.FAIL) {
                return result;
            }
        }

        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected @NotNull InteractionResult useWithoutItem(
        @NotNull BlockState state,
        @NotNull Level level,
        @NotNull BlockPos pos,
        @NotNull Player player,
        @NotNull BlockHitResult hitResult
    ) {
        if (player.isShiftKeyDown()) {
            return super.useWithoutItem(state, level, pos, player, hitResult);
        }

        if (!level.isClientSide && level.getBlockEntity(pos) instanceof CryotubeControllerBlockEntity controller) {
            player.openMenu(controller);
        }

        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
