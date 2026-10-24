package com.human.common.gameplay.storage;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * One block of a crate. A crate is placed whole by {@link CrateItem} and always comes away whole: whatever removes ANY
 * of its blocks - mining, an explosion, acid, a command - the crate turns into a single item that keeps its contents,
 * and its other blocks are cleared. It never spills.
 * <p>
 * {@link #FACING} is the direction the placing player looked (the front faces back toward them). The renderer turns the
 * model by it; with NORTH the model is unrotated and its front points south, at the player.
 */
public class CrateBlock extends Block implements EntityBlock {

    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    private final CrateType type;

    public CrateBlock(CrateType type, Properties properties) {
        super(properties);
        this.type = type;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    public CrateType type() {
        return type;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(@NotNull BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection());
    }

    @Override
    protected @NotNull RenderShape getRenderShape(@NotNull BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new CrateBlockEntity(pos, state);
    }

    @Override
    public @NotNull ItemStack getCloneItemStack(@NotNull LevelReader level, @NotNull BlockPos pos, @NotNull BlockState state) {
        return new ItemStack(this);
    }

    @Override
    protected @NotNull InteractionResult useWithoutItem(
        @NotNull BlockState state,
        @NotNull Level level,
        @NotNull BlockPos pos,
        @NotNull Player player,
        @NotNull BlockHitResult hit
    ) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        if (level.getBlockEntity(pos) instanceof CrateBlockEntity part) {
            var master = part.master();

            if (master != null) {
                player.openMenu(
                    new SimpleMenuProvider(
                        (id, inventory, opener) -> new CrateMenu(id, inventory, master),
                        Component.translatable(getDescriptionId())
                    )
                );
            }
        }

        return InteractionResult.CONSUME;
    }

    @Override
    public @NotNull BlockState playerWillDestroy(
        @NotNull Level level,
        @NotNull BlockPos pos,
        @NotNull BlockState state,
        @NotNull Player player
    ) {
        if (!level.isClientSide && player.isCreative() && level.getBlockEntity(pos) instanceof CrateBlockEntity part) {
            var master = part.master();

            if (master != null && master.inventory().isEmpty()) {
                master.setSkipDrop();
            }
        }

        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected void onRemove(
        @NotNull BlockState state,
        @NotNull Level level,
        @NotNull BlockPos pos,
        @NotNull BlockState newState,
        boolean movedByPiston
    ) {
        if (
            !state.is(newState.getBlock()) && level instanceof ServerLevel serverLevel
                && level.getBlockEntity(pos) instanceof CrateBlockEntity part
        ) {
            var master = part.master();

            if (master != null && !master.isDismantled()) {
                master.setDismantled();
                dismantle(serverLevel, master, pos);
            }
        }

        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    /** Turns the whole crate into one item at its centre and clears every other block of it. */
    private void dismantle(ServerLevel level, CrateBlockEntity master, BlockPos brokenPos) {
        var centre = master.centre().add(0.0, 0.5, 0.0);

        if (!master.skipDrop()) {
            var stack = packUp(level, master);
            var entity = new ItemEntity(level, centre.x, centre.y, centre.z, stack);

            entity.setDefaultPickUpDelay();

            if (CrateItem.hasContents(stack)) {
                entity.setUnlimitedLifetime();
            }

            if (!level.addFreshEntity(entity)) {
                var contents = stack.get(com.human.common.registry.init.HumanCrates.CRATE_CONTENTS.get());

                com.human.Human.LOGGER.error(
                    "Crate at {} could not drop its item; its contents are kept in crate storage under id {}",
                    master.getBlockPos(),
                    contents == null ? "(empty)" : contents.id()
                );
            }
        }

        var masterPos = master.getBlockPos();

        for (var part : master.parts()) {
            // Only blocks that belong to THIS crate: never a different crate that now stands in a gap.
            if (
                !part.equals(brokenPos) && level.getBlockState(part).is(this)
                    && level.getBlockEntity(part) instanceof CrateBlockEntity other && other.masterPos().equals(masterPos)
            ) {
                level.removeBlock(part, false);
            }
        }
    }

    /** The crate as an item. A full crate's items move into {@link CrateStorageData} under a fresh id. */
    static ItemStack packUp(ServerLevel level, CrateBlockEntity master) {
        var stack = new ItemStack(master.getBlockState().getBlock());
        var inventory = master.inventory();

        if (inventory.isEmpty()) {
            return stack;
        }

        var id = UUID.randomUUID();

        CrateStorageData.get(level.getServer()).put(id, inventory.save(level.registryAccess()));
        CrateItem.markFull(stack, CrateContents.of(id, inventory));
        inventory.clear();

        return stack;
    }

    @Override
    protected boolean propagatesSkylightDown(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos) {
        return false;
    }
}
