package com.human.common.gameplay.gene.machine.storage;

import com.human.common.registry.init.HumanGeneStorage;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The Gene Storage - 2x2 footprint, 2 tall, in bare-metal, W-Y and USCM editions ([stated] Oct 29). One class for all
 * three; the edition decides only the texture.
 * <p>
 * Use: right-click with a filled gene tube stores it; right-click with an empty hand opens it (both stages) - the
 * approved rack screen lands on this in the next pass. Unpowered it stays shut and refuses, but nothing stored is lost.
 * Breaking it drops what is in the rack.
 * </p>
 */
public class GeneStorageBlock extends HorizontalDirectionalBlock implements EntityBlock, com.human.common.gameplay.power.PowerParts.PartBlock {

    public enum Edition {

        BARE("gene_storage"),
        WY("wy_gene_storage"),
        USCM("uscm_gene_storage");

        public final String id;

        Edition(String id) {
            this.id = id;
        }
    }

    public static final MapCodec<GeneStorageBlock> CODEC = simpleCodec(properties -> new GeneStorageBlock(Edition.BARE, properties));

    public static final EnumProperty<GeneStoragePart> PART = EnumProperty.create("part", GeneStoragePart.class);

    /**
     * The bottom blocks are solid machine; the top blocks are the crown and lift, so only their lower half collides.
     */
    private static final VoxelShape TOP_SHAPE = Block.box(0, 0, 0, 16, 8, 16);

    private final Edition edition;

    public GeneStorageBlock(Edition edition, Properties properties) {
        super(properties);
        this.edition = edition;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(PART, GeneStoragePart.CORE));
    }

    public Edition edition() {
        return edition;
    }

    @Override
    protected @NotNull MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.@NotNull Builder<Block, BlockState> builder) {
        builder.add(FACING, PART);
    }

    public static BlockPos corePos(BlockPos pos, BlockState state) {
        return state.getValue(PART).toCore(pos, state.getValue(FACING));
    }

    @Override
    public @Nullable BlockState getStateForPlacement(@NotNull BlockPlaceContext context) {
        var facing = context.getHorizontalDirection().getOpposite();
        var core = context.getClickedPos();
        var level = context.getLevel();

        for (var part : GeneStoragePart.values()) {
            if (part == GeneStoragePart.CORE) {
                continue;
            }
            var pos = part.fromCore(core, facing);
            if (
                !level.getBlockState(pos).canBeReplaced(context) || !level.getWorldBorder().isWithinBounds(pos) || pos.getY() >= level
                    .getMaxBuildHeight()
            ) {
                return null;
            }
            // Nobody standing in the space - a part placed into a player or mob would trap them inside it.
            if (!level.isUnobstructed(defaultBlockState(), pos, CollisionContext.empty())) {
                return null;
            }
        }
        return defaultBlockState().setValue(FACING, facing).setValue(PART, GeneStoragePart.CORE);
    }

    @Override
    public void setPlacedBy(
        @NotNull Level level,
        @NotNull BlockPos pos,
        @NotNull BlockState state,
        @Nullable LivingEntity placer,
        @NotNull ItemStack stack
    ) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide) {
            return;
        }
        var facing = state.getValue(FACING);
        for (var part : GeneStoragePart.values()) {
            if (part != GeneStoragePart.CORE) {
                level.setBlock(part.fromCore(pos, facing), state.setValue(PART, part), Block.UPDATE_ALL);
            }
        }
        // Oct 31 - the core linked to the grid the moment it was placed, before these blocks existed; cables at their
        // faces can only reach it now.
        if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            com.human.common.gameplay.power.PowerSystem.get(serverLevel).relink(serverLevel, pos);
        }
        if (level.getBlockEntity(pos) instanceof GeneStorageBlockEntity storage) {
            storage.assignFillOrder();
        }
    }

    /** A part with no core next to it goes away. */
    @Override
    protected @NotNull BlockState updateShape(
        @NotNull BlockState state,
        @NotNull Direction direction,
        @NotNull BlockState neighbor,
        @NotNull LevelAccessor level,
        @NotNull BlockPos pos,
        @NotNull BlockPos neighborPos
    ) {
        if (state.getValue(PART) != GeneStoragePart.CORE) {
            var core = level.getBlockState(corePos(pos, state));
            if (!core.is(this) || core.getValue(PART) != GeneStoragePart.CORE || core.getValue(FACING) != state.getValue(FACING)) {
                return Blocks.AIR.defaultBlockState();
            }
        }
        return super.updateShape(state, direction, neighbor, level, pos, neighborPos);
    }

    /** Breaking a part breaks the core properly, so its item drops. */
    @Override
    public @NotNull BlockState playerWillDestroy(
        @NotNull Level level,
        @NotNull BlockPos pos,
        @NotNull BlockState state,
        @NotNull Player player
    ) {
        if (!level.isClientSide && state.getValue(PART) != GeneStoragePart.CORE) {
            var core = corePos(pos, state);
            if (level.getBlockState(core).is(this)) {
                level.destroyBlock(core, !player.isCreative(), player);
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    /** The rack's tubes drop when the core goes, and the storage leaves its network. */
    @Override
    protected void onRemove(
        @NotNull BlockState state,
        @NotNull Level level,
        @NotNull BlockPos pos,
        @NotNull BlockState newState,
        boolean moved
    ) {
        if (
            !state.is(newState.getBlock()) && state.getValue(PART) == GeneStoragePart.CORE && level.getBlockEntity(
                pos
            ) instanceof GeneStorageBlockEntity storage
        ) {
            Containers.dropContents(level, pos, storage.rack());
            GeneStorageNetwork.leave(level, pos);
        }
        super.onRemove(state, level, pos, newState, moved);
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
        if (!GeneStorageInventory.accepts(stack)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!level.isClientSide && level.getBlockEntity(corePos(pos, state)) instanceof GeneStorageBlockEntity storage) {
            if (!storage.isSupplied()) {
                player.displayClientMessage(Component.translatable("gui.avp_human.gene_storage.no_power"), true);
                return ItemInteractionResult.sidedSuccess(false);
            }
            var left = storage.rack().insert(stack);
            if (left.getCount() == stack.getCount()) {
                player.displayClientMessage(Component.translatable("gui.avp_human.gene_storage.full"), true);
            } else {
                stack.setCount(left.getCount());
                storage.rackChanged();
            }
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected @NotNull InteractionResult useWithoutItem(
        @NotNull BlockState state,
        @NotNull Level level,
        @NotNull BlockPos pos,
        @NotNull Player player,
        @NotNull BlockHitResult hit
    ) {
        if (!level.isClientSide && level.getBlockEntity(corePos(pos, state)) instanceof GeneStorageBlockEntity storage) {
            storage.use(player);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected @NotNull VoxelShape getShape(
        @NotNull BlockState state,
        @NotNull BlockGetter level,
        @NotNull BlockPos pos,
        @NotNull CollisionContext context
    ) {
        var part = state.getValue(PART);
        return part == GeneStoragePart.TOP || part == GeneStoragePart.TOP_SIDE || part == GeneStoragePart.TOP_BACK
            || part == GeneStoragePart.TOP_BACK_SIDE
                ? TOP_SHAPE
                : Shapes.block();
    }

    @Override
    protected @NotNull RenderShape getRenderShape(@NotNull BlockState state) {
        return state.getValue(PART) == GeneStoragePart.CORE ? RenderShape.ENTITYBLOCK_ANIMATED : RenderShape.INVISIBLE;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return state.getValue(PART) == GeneStoragePart.CORE ? new GeneStorageBlockEntity(pos, state) : null;
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(
        @NotNull Level level,
        @NotNull BlockState state,
        @NotNull BlockEntityType<T> type
    ) {
        if (type != HumanGeneStorage.BLOCK_ENTITY.get()) {
            return null;
        }
        if (level.isClientSide) {
            return (tickLevel, pos, tickState, entity) -> ((GeneStorageBlockEntity) entity).clientTick();
        }
        return (tickLevel, pos, tickState, entity) -> ((GeneStorageBlockEntity) entity).serverTick(tickLevel, pos, tickState);
    }

    // ------------------------------------------------------------------------------------------------------------
    // Power through the machine's other blocks (PowerParts)
    // ------------------------------------------------------------------------------------------------------------

    /**
     * [stated] Oct 31: the genetic machines take power from the BOTTOM ONLY - any of their bottom blocks. A back port
     * would have sat over the models' counters. The core's own faces are its block entity's ({@code canConnectOn});
     * this answers for the other blocks.
     */
    @Override
    public @Nullable BlockPos coreThrough(
        @NotNull BlockGetter level,
        @NotNull BlockPos pos,
        @NotNull BlockState state,
        @NotNull Direction face
    ) {
        if (!state.is(this)) {
            return null;
        }

        var part = state.getValue(PART);

        if (part == GeneStoragePart.CORE) {
            return null;
        }

        var takes = face == Direction.DOWN && !part.isTop();
        if (!takes) {
            return null;
        }

        var core = corePos(pos, state);
        var coreState = level.getBlockState(core);

        return coreState.is(this) && coreState.getValue(PART) == GeneStoragePart.CORE && coreState.getValue(FACING) == state.getValue(
            FACING
        )
            ? core
            : null;
    }

    @Override
    public void cableFaces(
        @NotNull BlockPos core,
        @NotNull BlockState coreState,
        @NotNull java.util.function.BiConsumer<BlockPos, Direction> sink
    ) {
        if (!coreState.is(this) || coreState.getValue(PART) != GeneStoragePart.CORE) {
            return;
        }

        var facing = coreState.getValue(FACING);

        for (var part : GeneStoragePart.values()) {
            if (part == GeneStoragePart.CORE) {
                continue;
            }

            var partPos = part.fromCore(core, facing);

            if (!part.isTop()) {
                sink.accept(partPos, Direction.DOWN);
            }

        }
    }
}
