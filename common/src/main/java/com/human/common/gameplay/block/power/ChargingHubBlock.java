package com.human.common.gameplay.block.power;

import com.human.common.gameplay.block.entity.power.impl.ChargingHubBlockEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The charging hub - a 500k store with a plate that charges the item resting on it.
 * <p>
 * ⭐ [stated] Sep 29: this block was the ESU, registered as {@code avp_human:battery}. It is now
 * {@code avp_human:charging_hub}, keeping the ESU's model, art and charging plate, and the {@code battery} id belongs
 * to the new full-block battery. No jar had shipped with the ESU, so the rename needs no world migration.
 * </p>
 * <p>
 * ⚠ RenderShape.ENTITYBLOCK_ANIMATED - the geometry is a geo model drawn by the block entity renderer, so there is no
 * baked block model. Leave it on the default and the block draws as a missing-texture cube.
 * </p>
 */
public class ChargingHubBlock extends PowerNodeEntityBlock<ChargingHubBlockEntity> {

    public static final MapCodec<ChargingHubBlock> CODEC = simpleCodec(ChargingHubBlock::new);

    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    public ChargingHubBlock(Properties properties) {
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
    protected @NotNull BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected @NotNull BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    /**
     * Put a chargeable item on the pad, or take back whatever is there.
     * <p>
     * ⚠ SNEAK IS NOT HANDLED HERE. Sneak + click is the breaker toggle on {@code PowerNodeEntityBlock}, and this method
     * is only reached when the player is not sneaking - so the two interactions cannot collide.
     * </p>
     * <p>
     * ⚠ An item that cannot take charge is REFUSED rather than accepted and ignored. A pad that silently swallows a
     * pickaxe looks broken; one that declines it tells the player something immediately.
     * </p>
     */
    @Override
    protected @NotNull net.minecraft.world.ItemInteractionResult useItemOn(
        @NotNull net.minecraft.world.item.ItemStack stack,
        @NotNull BlockState state,
        @NotNull net.minecraft.world.level.Level level,
        @NotNull BlockPos pos,
        @NotNull net.minecraft.world.entity.player.Player player,
        @NotNull net.minecraft.world.InteractionHand hand,
        @NotNull net.minecraft.world.phys.BlockHitResult hitResult
    ) {
        if (!(level.getBlockEntity(pos) instanceof ChargingHubBlockEntity battery)) {
            return net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        if (stack.isEmpty()) {
            // Empty hand: take the item back off the pad.
            if (battery.getPadItem().isEmpty()) {
                return net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            }

            if (!level.isClientSide) {
                var taken = battery.swapPadItem(net.minecraft.world.item.ItemStack.EMPTY);

                if (!player.getInventory().add(taken)) {
                    player.drop(taken, false);
                }
            }

            return net.minecraft.world.ItemInteractionResult.sidedSuccess(level.isClientSide);
        }

        if (!com.human.common.gameplay.power.ItemEnergyBridge.canCharge(stack)) {
            return net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        if (!level.isClientSide) {
            var previous = battery.swapPadItem(stack);

            stack.shrink(1);

            if (!previous.isEmpty() && !player.getInventory().add(previous)) {
                player.drop(previous, false);
            }
        }

        return net.minecraft.world.ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    /** Anything left on the pad comes back when the block goes. */
    @Override
    protected void onRemove(
        @NotNull BlockState state,
        @NotNull net.minecraft.world.level.Level level,
        @NotNull BlockPos pos,
        @NotNull BlockState newState,
        boolean movedByPiston
    ) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof ChargingHubBlockEntity battery) {
            net.minecraft.world.Containers.dropItemStack(
                level,
                pos.getX(),
                pos.getY(),
                pos.getZ(),
                battery.getPadItem()
            );
        }

        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    protected @NotNull RenderShape getRenderShape(@NotNull BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    protected @NotNull MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(@NotNull BlockPos blockPos, @NotNull BlockState blockState) {
        return new ChargingHubBlockEntity(blockPos, blockState);
    }
}
