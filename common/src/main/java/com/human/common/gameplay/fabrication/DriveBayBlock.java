package com.human.common.gameplay.fabrication;

import com.human.common.registry.init.HumanFabrication;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * [stated] Oct 13: the drive bay - standard, W-Y and USCM editions, which differ only in their frame. Right-click with
 * an SSD to slot it in; right-click otherwise to open its screen.
 */
public class DriveBayBlock extends HorizontalDirectionalBlock implements EntityBlock {

    public static final MapCodec<DriveBayBlock> CODEC = simpleCodec(DriveBayBlock::new);

    public DriveBayBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, net.minecraft.core.Direction.NORTH));
    }

    @Override
    protected @NotNull MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.@NotNull Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(@NotNull BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected @NotNull RenderShape getRenderShape(@NotNull BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new DriveBayBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(
        @NotNull Level level,
        @NotNull BlockState state,
        @NotNull BlockEntityType<T> type
    ) {
        if (level.isClientSide || type != HumanFabrication.DRIVE_BAY_BLOCK_ENTITY.get()) {
            return null;
        }

        return (tickLevel, pos, tickState, entity) -> ((DriveBayBlockEntity) entity).serverTick(tickLevel, pos, tickState);
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
        if (!DriveBayBlockEntity.isSsd(stack) || !(level.getBlockEntity(pos) instanceof DriveBayBlockEntity bay)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        if (level.isClientSide) {
            return ItemInteractionResult.SUCCESS;
        }

        var toInsert = player.getAbilities().instabuild ? stack.copy() : stack;

        if (!bay.insertSsd(toInsert)) {
            // All four positions full: open the screen instead, so the click still does something useful.
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        level.playSound(null, pos, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 0.8F, 1.3F);

        return ItemInteractionResult.CONSUME;
    }

    @Override
    protected @NotNull InteractionResult useWithoutItem(
        @NotNull BlockState state,
        @NotNull Level level,
        @NotNull BlockPos pos,
        @NotNull Player player,
        @NotNull BlockHitResult hit
    ) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof DriveBayBlockEntity bay) {
            player.openMenu(
                new SimpleMenuProvider(
                    (id, inventory, opener) -> new DriveBayMenu(id, inventory, bay),
                    Component.translatable(getDescriptionId())
                )
            );
        }

        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /**
     * Creative mode skips loot tables, which would silently delete a full bay. Like a shulker box, a creative break of
     * a bay with anything in it still drops it, contents and all.
     */
    @Override
    public @NotNull BlockState playerWillDestroy(
        @NotNull Level level,
        @NotNull BlockPos pos,
        @NotNull BlockState state,
        @NotNull Player player
    ) {
        if (!level.isClientSide && player.isCreative() && level.getBlockEntity(pos) instanceof DriveBayBlockEntity bay && !bay.isEmpty()) {
            var stack = new ItemStack(this);

            stack.applyComponents(bay.collectComponents());

            var drop = new ItemEntity(level, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, stack);

            drop.setDefaultPickUpDelay();
            level.addFreshEntity(drop);
        }

        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected boolean hasAnalogOutputSignal(@NotNull BlockState state) {
        return false;
    }

    @Override
    public @NotNull ItemStack getCloneItemStack(
        @NotNull net.minecraft.world.level.LevelReader level,
        @NotNull BlockPos pos,
        @NotNull BlockState state
    ) {
        // Pick-block gives an empty bay; it must never copy blueprints.
        return new ItemStack(this);
    }
}
