package com.human.common.gameplay.block;

import com.human.common.gameplay.block.entity.CanisterBlockEntity;
import com.human.common.gameplay.fluid.FluidContainerInteractions;
import com.human.common.gameplay.fluid.FluidContent;
import com.human.common.gameplay.item.canister.UniversalCanisterItem;
import com.human.common.registry.init.item.HumanItems;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * A universal canister placed in the world. [stated] Sep 29: shift + left-click with a canister puts it down; the model
 * shows the fill level through its four windows.
 * <h2>IT HAS NO ITEM OF ITS OWN</h2> The block IS the canister: {@link #asItem()} answers the universal canister, and
 * breaking it drops that canister with the fluid still inside ({@link #getDrops}).
 * <h2>⚠ NO "CORRECT TOOL" REQUIREMENT</h2> Vanilla only drops a block that needs a tool when it is broken WITH that
 * tool; a canister broken by hand would drop nothing and lose its fluid - so it breaks and drops with anything, faster
 * with a pickaxe.
 * <h2>USING IT</h2> Buckets and canisters: {@link FluidContainerInteractions}, one bucket per click. Empty-handed, it
 * says what it holds. Pipes: fill through the top, drain through the bottom ({@link CanisterBlockEntity#canFill}).
 */
public class CanisterBlock extends BaseEntityBlock {

    public static final MapCodec<CanisterBlock> CODEC = simpleCodec(CanisterBlock::new);

    /** The model's frame: 10 x 16 x 10, centred. */
    private static final VoxelShape SHAPE = box(3.0D, 0.0D, 3.0D, 13.0D, 16.0D, 13.0D);

    public CanisterBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected @NotNull MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
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
    public @NotNull Item asItem() {
        return HumanItems.CANISTER.get();
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new CanisterBlockEntity(pos, state);
    }

    /** Drops the canister with its fluid, whatever broke it - an explosion included. */
    @Override
    protected @NotNull List<ItemStack> getDrops(@NotNull BlockState state, LootParams.@NotNull Builder params) {
        var blockEntity = params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        var content = blockEntity instanceof CanisterBlockEntity canister ? canister.getContent() : FluidContent.EMPTY;

        return List.of(
            content.isEmpty() ? new ItemStack(HumanItems.CANISTER.get()) : UniversalCanisterItem.filled(content.fluid(), content.amount())
        );
    }

    @Override
    protected @NotNull ItemInteractionResult useItemOn(
        @NotNull ItemStack stack,
        @NotNull BlockState state,
        @NotNull Level level,
        @NotNull BlockPos pos,
        @NotNull Player player,
        @NotNull InteractionHand hand,
        @NotNull BlockHitResult hit
    ) {
        if (!(level.getBlockEntity(pos) instanceof CanisterBlockEntity canister)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        return FluidContainerInteractions.useItem(stack, level, pos, player, hand, canister);
    }

    /** Empty-handed: say what is inside. */
    @Override
    protected @NotNull InteractionResult useWithoutItem(
        @NotNull BlockState state,
        @NotNull Level level,
        @NotNull BlockPos pos,
        @NotNull Player player,
        @NotNull BlockHitResult hit
    ) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof CanisterBlockEntity canister) {
            player.displayClientMessage(FluidContainerInteractions.describe(canister), true);
        }

        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected boolean hasAnalogOutputSignal(@NotNull BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos) {
        if (!(level.getBlockEntity(pos) instanceof CanisterBlockEntity canister) || canister.getContent().isEmpty()) {
            return 0;
        }

        return Math.max(1, canister.getContent().amount() * 15 / UniversalCanisterItem.CAPACITY);
    }
}
