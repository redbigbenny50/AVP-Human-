package com.human.common.gameplay.vaporizer;

import com.human.common.gameplay.block.entity.machine.ElectricMachineBlockEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.WorldlyContainerHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;

/**
 * The vaporizer's TOP block: it holds nothing and draws nothing (the bottom block draws the whole model). It is there
 * so the machine is solid two blocks tall, and so the item ports - which [stated] Oct 17 are on the top block - have a
 * block for pipes to touch. Everything it is asked goes to the machine below:
 * <ul>
 * <li>pipes and hoppers get the machine's item ports through {@link VaporizerPortContainer};</li>
 * <li>clicking it is clicking the machine (its screen, a bucket of liquid);</li>
 * <li>breaking it breaks the machine, which drops its item and contents.</li>
 * </ul>
 * One shared top block serves all three editions - it is never seen.
 */
public class VaporizerTopBlock extends Block implements WorldlyContainerHolder {

    public static final MapCodec<VaporizerTopBlock> CODEC = simpleCodec(VaporizerTopBlock::new);

    public VaporizerTopBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected @NotNull MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected @NotNull RenderShape getRenderShape(@NotNull BlockState state) {
        return RenderShape.INVISIBLE;
    }

    /** No machine under it any more - it goes. */
    @Override
    protected @NotNull BlockState updateShape(
        @NotNull BlockState state,
        @NotNull Direction direction,
        @NotNull BlockState neighbor,
        @NotNull LevelAccessor level,
        @NotNull BlockPos pos,
        @NotNull BlockPos neighborPos
    ) {
        if (direction == Direction.DOWN && !(neighbor.getBlock() instanceof VaporizerBlock)) {
            return Blocks.AIR.defaultBlockState();
        }

        return super.updateShape(state, direction, neighbor, level, pos, neighborPos);
    }

    @Override
    protected boolean canSurvive(@NotNull BlockState state, @NotNull LevelReader level, @NotNull BlockPos pos) {
        return level.getBlockState(pos.below()).getBlock() instanceof VaporizerBlock;
    }

    /** Breaking the top breaks the machine properly, so its item - with everything in it - drops. */
    @Override
    public @NotNull BlockState playerWillDestroy(
        @NotNull Level level,
        @NotNull BlockPos pos,
        @NotNull BlockState state,
        @NotNull Player player
    ) {
        if (!level.isClientSide && level.getBlockState(pos.below()).getBlock() instanceof VaporizerBlock) {
            level.destroyBlock(pos.below(), !player.isCreative(), player);
        }

        return super.playerWillDestroy(level, pos, state, player);
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
        var below = pos.below();
        var machine = level.getBlockState(below);

        if (!(machine.getBlock() instanceof VaporizerBlock)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        return machine.useItemOn(stack, level, player, hand, hitResult.withPosition(below));
    }

    @Override
    protected @NotNull InteractionResult useWithoutItem(
        @NotNull BlockState state,
        @NotNull Level level,
        @NotNull BlockPos pos,
        @NotNull Player player,
        @NotNull BlockHitResult hitResult
    ) {
        var below = pos.below();
        var machine = level.getBlockState(below);

        if (!(machine.getBlock() instanceof VaporizerBlock)) {
            return InteractionResult.PASS;
        }

        return machine.useWithoutItem(level, player, hitResult.withPosition(below));
    }

    @Override
    public @NotNull WorldlyContainer getContainer(@NotNull BlockState state, @NotNull LevelAccessor level, @NotNull BlockPos pos) {
        return level.getBlockEntity(pos.below()) instanceof ElectricMachineBlockEntity machine
            && level.getBlockState(pos.below()).getBlock() instanceof VaporizerBlock
                ? new VaporizerPortContainer(machine, true)
                : VaporizerBlock.EMPTY_PORTS;
    }

    /** Middle-click picks the machine. */
    @Override
    public @NotNull ItemStack getCloneItemStack(@NotNull LevelReader level, @NotNull BlockPos pos, @NotNull BlockState state) {
        var below = level.getBlockState(pos.below());

        return below.getBlock() instanceof VaporizerBlock ? new ItemStack(below.getBlock()) : ItemStack.EMPTY;
    }

    @Override
    protected boolean propagatesSkylightDown(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos) {
        return true;
    }
}
