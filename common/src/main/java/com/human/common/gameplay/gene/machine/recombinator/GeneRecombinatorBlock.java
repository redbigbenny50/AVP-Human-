package com.human.common.gameplay.gene.machine.recombinator;

import com.human.common.gameplay.gene.tube.GeneTubeItem;
import com.human.common.registry.init.HumanGeneRecombinator;
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
 * The Gene Recombinator - 2 wide, 1 deep, 2 tall, in bare-metal, W-Y and USCM editions ([stated] Oct 29). One class for
 * all three; the edition decides only the texture.
 * <p>
 * Right-click with a filled gene tube: it goes into the sample cradle if that is empty, else into an empty arm's clamp
 * ([stated] the player places vials in the arms' clamps). Right-click with an empty hand opens the screen. Breaking it
 * drops the sample, both vials and the empties. Power from underneath (any bottom block, [stated] Oct 31); no item port
 * ([stated] Oct 27).
 * </p>
 */
public class GeneRecombinatorBlock extends HorizontalDirectionalBlock implements EntityBlock, com.human.common.gameplay.power.PowerParts.PartBlock {

    public enum Edition {

        BARE("gene_recombinator"),
        WY("wy_gene_recombinator"),
        USCM("uscm_gene_recombinator");

        public final String id;

        Edition(String id) {
            this.id = id;
        }
    }

    public static final MapCodec<GeneRecombinatorBlock> CODEC = simpleCodec(
        properties -> new GeneRecombinatorBlock(Edition.BARE, properties)
    );

    public static final EnumProperty<GeneRecombinatorPart> PART = EnumProperty.create("part", GeneRecombinatorPart.class);

    /** The model is 30 px tall: the top row collides up to 14 px. */
    private static final VoxelShape TOP_SHAPE = Block.box(0, 0, 0, 16, 14, 16);

    private final Edition edition;

    public GeneRecombinatorBlock(Edition edition, Properties properties) {
        super(properties);
        this.edition = edition;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(PART, GeneRecombinatorPart.CORE));
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

        for (var part : GeneRecombinatorPart.values()) {
            if (part == GeneRecombinatorPart.CORE) {
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
        return defaultBlockState().setValue(FACING, facing).setValue(PART, GeneRecombinatorPart.CORE);
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
        for (var part : GeneRecombinatorPart.values()) {
            if (part != GeneRecombinatorPart.CORE) {
                level.setBlock(part.fromCore(pos, facing), state.setValue(PART, part), Block.UPDATE_ALL);
            }
        }
        // Oct 31 - the core linked to the grid the moment it was placed, before these blocks existed; cables at their
        // faces can only reach it now.
        if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            com.human.common.gameplay.power.PowerSystem.get(serverLevel).relink(serverLevel, pos);
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
        if (state.getValue(PART) != GeneRecombinatorPart.CORE) {
            var core = level.getBlockState(corePos(pos, state));
            if (!core.is(this) || core.getValue(PART) != GeneRecombinatorPart.CORE || core.getValue(FACING) != state.getValue(FACING)) {
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
        if (!level.isClientSide && state.getValue(PART) != GeneRecombinatorPart.CORE) {
            var core = corePos(pos, state);
            if (level.getBlockState(core).is(this)) {
                level.destroyBlock(core, !player.isCreative(), player);
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    /** Everything inside drops when the core goes. */
    @Override
    protected void onRemove(
        @NotNull BlockState state,
        @NotNull Level level,
        @NotNull BlockPos pos,
        @NotNull BlockState newState,
        boolean moved
    ) {
        if (
            !state.is(newState.getBlock()) && state.getValue(PART) == GeneRecombinatorPart.CORE && level.getBlockEntity(
                pos
            ) instanceof GeneRecombinatorBlockEntity machine
        ) {
            Containers.dropContents(level, pos, machine.items());
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
        if (GeneTubeItem.read(stack) == null) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!level.isClientSide && level.getBlockEntity(corePos(pos, state)) instanceof GeneRecombinatorBlockEntity machine) {
            if (!machine.loadByHand(stack)) {
                player.displayClientMessage(Component.translatable("gui.avp_human.gene_recombinator.no_room"), true);
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
        if (!level.isClientSide && level.getBlockEntity(corePos(pos, state)) instanceof GeneRecombinatorBlockEntity machine) {
            machine.use(player);
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
        return state.getValue(PART).isTop() ? TOP_SHAPE : Shapes.block();
    }

    @Override
    protected @NotNull RenderShape getRenderShape(@NotNull BlockState state) {
        return state.getValue(PART) == GeneRecombinatorPart.CORE ? RenderShape.ENTITYBLOCK_ANIMATED : RenderShape.INVISIBLE;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return state.getValue(PART) == GeneRecombinatorPart.CORE ? new GeneRecombinatorBlockEntity(pos, state) : null;
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(
        @NotNull Level level,
        @NotNull BlockState state,
        @NotNull BlockEntityType<T> type
    ) {
        if (type != HumanGeneRecombinator.BLOCK_ENTITY.get()) {
            return null;
        }
        if (level.isClientSide) {
            return (tickLevel, pos, tickState, entity) -> ((GeneRecombinatorBlockEntity) entity).clientTick();
        }
        return (tickLevel, pos, tickState, entity) -> ((GeneRecombinatorBlockEntity) entity).serverTick(tickLevel, pos, tickState);
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

        if (part == GeneRecombinatorPart.CORE) {
            return null;
        }

        var takes = face == Direction.DOWN && !part.isTop();
        if (!takes) {
            return null;
        }

        var core = corePos(pos, state);
        var coreState = level.getBlockState(core);

        return coreState.is(this) && coreState.getValue(PART) == GeneRecombinatorPart.CORE && coreState.getValue(FACING) == state.getValue(
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
        if (!coreState.is(this) || coreState.getValue(PART) != GeneRecombinatorPart.CORE) {
            return;
        }

        var facing = coreState.getValue(FACING);

        for (var part : GeneRecombinatorPart.values()) {
            if (part == GeneRecombinatorPart.CORE) {
                continue;
            }

            var partPos = part.fromCore(core, facing);

            if (!part.isTop()) {
                sink.accept(partPos, Direction.DOWN);
            }

        }
    }
}
