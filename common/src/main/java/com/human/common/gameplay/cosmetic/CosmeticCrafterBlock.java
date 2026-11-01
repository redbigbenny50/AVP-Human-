package com.human.common.gameplay.cosmetic;

import com.human.common.registry.init.HumanCosmetics;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The Cosmetic Crafter - 3 wide, 2 deep, 2 tall; standard, W-Y and USCM editions.
 * <h2>CORE</h2> The core is the BACK-MIDDLE block. [stated] Nov 1: power on the BACK only, through any of the three
 * bottom-row back blocks (one port centred on each); nothing reaches the underside. The player places the FRONT-middle block.
 */
public class CosmeticCrafterBlock extends CosmeticMultiblock implements EntityBlock, com.human.common.gameplay.power.PowerParts.PartBlock {

    /** Core (back-middle) first, then the rest. Front is -z. */
    public static final int[][] CELLS = {
        { 0, 0, 0 },
        { -1, 0, -1 }, { 0, 0, -1 }, { 1, 0, -1 },
        { -1, 0, 0 }, { 1, 0, 0 },
        { -1, 1, -1 }, { 0, 1, -1 }, { 1, 1, -1 },
        { -1, 1, 0 }, { 0, 1, 0 }, { 1, 1, 0 }
    };

    /** The front-middle block, where the player clicks. */
    public static final int ANCHOR = 2;

    /**
     * [stated] Nov 1: the ITEM PORT, on the pillar side - the clockwise side's front-bottom block (cell 1, 0, -1). An item
     * pipe connects only there, which is how the crafter reaches a pipe network's storage; finished pieces can also be
     * pulled out through it.
     */
    public static final int ITEM_PORT_CELL = 3;

    /** Whether {@code side} of this crafter block is its item port. */
    public static boolean isItemPort(BlockState state, Direction side) {
        return state.getValue(CELL) == ITEM_PORT_CELL && side == state.getValue(FACING).getClockWise();
    }

    private final CosmeticStyle edition;

    public CosmeticCrafterBlock(Properties properties, CosmeticStyle edition) {
        super(properties, edition.prefix() + "cosmetic_crafter");
        this.edition = edition;
    }

    /** Which edition of the machine this is (its skin and brand), not what it may craft. */
    public CosmeticStyle edition() {
        return edition;
    }

    @Override
    public int[][] cells() {
        return CELLS;
    }

    @Override
    public int anchorCell() {
        return ANCHOR;
    }

    // ------------------------------------------------------------------------------------------------------------
    // Power through the machine's other blocks - the same mechanism 0.1.12f gave the gene machines: a cable against a
    // port face of a non-core block feeds the core (the only block with the power data).
    // [stated] Nov 1: BACK ONLY, on any of the three bottom-row back blocks - one port is modelled centred on each.
    // No underside: the gap under the machine means nothing can reach it.
    // ------------------------------------------------------------------------------------------------------------

    /** Whether a cell's {@code face} takes a cable: the back face of a bottom-row back block (the three ports). */
    private static boolean takesCable(int[] cell, Direction facing, Direction face) {
        return face == facing.getOpposite() && cell[1] == 0 && cell[2] == 0;
    }

    @Override
    public @Nullable BlockPos coreThrough(
        @NotNull net.minecraft.world.level.BlockGetter level,
        @NotNull BlockPos pos,
        @NotNull BlockState state,
        @NotNull Direction face
    ) {
        if (!state.is(this)) {
            return null;
        }

        var cell = Math.min(state.getValue(CELL), CELLS.length - 1);

        // The core's own faces are its block entity's business (canConnectOn).
        if (cell == 0 || !takesCable(CELLS[cell], state.getValue(FACING), face)) {
            return null;
        }

        var core = corePos(pos, state);
        var coreState = level.getBlockState(core);

        return coreState.is(this) && coreState.getValue(CELL) == 0 && coreState.getValue(FACING) == state.getValue(FACING) ? core : null;
    }

    @Override
    public void cableFaces(
        @NotNull BlockPos core,
        @NotNull BlockState coreState,
        @NotNull java.util.function.BiConsumer<BlockPos, Direction> sink
    ) {
        if (!coreState.is(this) || coreState.getValue(CELL) != 0) {
            return;
        }

        var facing = coreState.getValue(FACING);

        for (int index = 1; index < CELLS.length; index++) {
            var cell = CELLS[index];

            if (takesCable(cell, facing, facing.getOpposite())) {
                sink.accept(cellPos(core, facing, cell), facing.getOpposite());
            }
        }
    }

    /**
     * The core joined the grid the moment it was placed - before the machine's other blocks existed - so cables at their
     * faces could not reach it yet. Walk again from the core now that they do (as the gene machines do).
     */
    @Override
    public void setPlacedBy(
        @NotNull Level level,
        @NotNull BlockPos pos,
        @NotNull BlockState state,
        @Nullable net.minecraft.world.entity.LivingEntity placer,
        @NotNull ItemStack stack
    ) {
        super.setPlacedBy(level, pos, state, placer, stack);

        if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            com.human.common.gameplay.power.PowerSystem.get(serverLevel).relink(serverLevel, corePos(pos, state));
        }
    }

    @Override
    protected @NotNull MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return simpleCodec(properties -> new CosmeticCrafterBlock(properties, edition));
    }

    @Override
    protected @NotNull RenderShape getRenderShape(@NotNull BlockState state) {
        return state.getValue(CELL) == 0 ? RenderShape.ENTITYBLOCK_ANIMATED : RenderShape.INVISIBLE;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return state.getValue(CELL) == 0 ? new CosmeticCrafterBlockEntity(pos, state) : null;
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(
        @NotNull Level level,
        @NotNull BlockState state,
        @NotNull BlockEntityType<T> type
    ) {
        if (type != HumanCosmetics.CRAFTER_BLOCK_ENTITY.get() || state.getValue(CELL) != 0) {
            return null;
        }

        if (level.isClientSide) {
            return (tickLevel, pos, tickState, entity) -> ((CosmeticCrafterBlockEntity) entity).clientTick();
        }

        return (tickLevel, pos, tickState, entity) -> ((CosmeticCrafterBlockEntity) entity).serverTick(tickLevel, pos, tickState);
    }

    @Override
    protected @NotNull InteractionResult useWithoutItem(
        @NotNull BlockState state,
        @NotNull Level level,
        @NotNull BlockPos pos,
        @NotNull Player player,
        @NotNull BlockHitResult hit
    ) {
        if (!level.isClientSide && level.getBlockEntity(corePos(pos, state)) instanceof CosmeticCrafterBlockEntity crafter) {
            player.openMenu(
                new SimpleMenuProvider(
                    (id, inventory, opener) -> new CosmeticCrafterMenu(id, inventory, crafter),
                    Component.translatable("container.avp_human.cosmetic_crafter")
                )
            );
        }

        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /** Creative mode skips loot tables; a crafter with anything in it still drops, like a shulker box. */
    @Override
    protected void beforeCoreBroken(Level level, BlockPos core, Player player) {
        if (player.isCreative() && level.getBlockEntity(core) instanceof CosmeticCrafterBlockEntity crafter && !crafter.isEmpty()) {
            var stack = new ItemStack(this);

            stack.applyComponents(crafter.collectComponents());

            var drop = new ItemEntity(level, core.getX() + 0.5D, core.getY() + 0.5D, core.getZ() + 0.5D, stack);

            drop.setDefaultPickUpDelay();
            level.addFreshEntity(drop);
        }
    }
}
