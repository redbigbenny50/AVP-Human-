package com.human.common.gameplay.block;

import com.human.common.gameplay.block.entity.LatexTapBlockEntity;
import com.human.common.registry.init.HumanBlockEntityTypes;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

/**
 * A tree tap. [stated] Sep 24: "theres a model for and item of a tree tap you place it against jungle trees and it
 * generates latex if its the full tree not just a block of jungle wood", and "one tap per tree unless its a large
 * jungle tree then it can support up to 4".
 * <h2>What counts as a tree</h2> A placed block of jungle wood is not a tree, so the tap walks the trunk it is driven
 * into and demands BOTH:
 * <ul>
 * <li>at least {@value #MIN_TRUNK_LOGS} connected jungle logs, and</li>
 * <li>jungle leaves within {@value #LEAF_SEARCH_RADIUS} of the trunk.</li>
 * </ul>
 * A wall someone built out of jungle logs has no leaves, so it taps nothing. ⚠ The leaf check is what makes this work -
 * log count alone is trivially cheesed by stacking six logs.
 * <h2>How many taps</h2> A large jungle tree has a 2x2 trunk, so trunk WIDTH is what separates the two cases: a trunk
 * occupying more than one column in both horizontal axes allows {@value #MAX_TAPS_LARGE_TREE}, anything else
 * {@value #MAX_TAPS_SMALL_TREE}. Existing taps on the same trunk are counted at placement and a further one is refused.
 */
public class LatexTapBlock extends BaseEntityBlock {

    public static final MapCodec<LatexTapBlock> CODEC = simpleCodec(LatexTapBlock::new);

    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    public static final int MIN_TRUNK_LOGS = 4;

    public static final int LEAF_SEARCH_RADIUS = 6;

    public static final int MAX_TAPS_SMALL_TREE = 1;

    public static final int MAX_TAPS_LARGE_TREE = 4;

    /** Bound on the trunk walk so a player-built jungle-log mega-structure cannot make placement expensive. */
    private static final int MAX_TRUNK_SEARCH = 256;

    // [stated] Oct 5: the hitbox sat on the far side from the model. FACING points AT the trunk and the model fills the
    // half of the block against it (model x/y 7.1-8.9, from the trunk face to mid-block), so each box does too.
    private static final VoxelShape SHAPE_NORTH = Block.box(6.0, 6.0, 0.0, 10.0, 10.0, 8.0);

    private static final VoxelShape SHAPE_SOUTH = Block.box(6.0, 6.0, 8.0, 10.0, 10.0, 16.0);

    private static final VoxelShape SHAPE_WEST = Block.box(0.0, 6.0, 6.0, 8.0, 10.0, 10.0);

    private static final VoxelShape SHAPE_EAST = Block.box(8.0, 6.0, 6.0, 16.0, 10.0, 10.0);

    public LatexTapBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    protected @NotNull MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    /**
     * [stated] Oct 3: the tap was invisible. ENTITYBLOCK_ANIMATED means "a block-entity renderer draws this", and the
     * tap has none - it has an ordinary JSON model (block/latex_tap, turned per facing by its blockstate). MODEL draws
     * it.
     */
    @Override
    protected @NotNull RenderShape getRenderShape(@NotNull BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected @NotNull VoxelShape getShape(
        @NotNull BlockState state,
        @NotNull BlockGetter level,
        @NotNull BlockPos pos,
        @NotNull CollisionContext context
    ) {
        return switch (state.getValue(FACING)) {
            case SOUTH -> SHAPE_SOUTH;
            case WEST -> SHAPE_WEST;
            case EAST -> SHAPE_EAST;
            default -> SHAPE_NORTH;
        };
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        var level = context.getLevel();
        var pos = context.getClickedPos();

        for (var direction : context.getNearestLookingDirections()) {
            if (direction.getAxis().isVertical()) {
                continue;
            }

            // The tap points INTO the trunk, so it faces the opposite way from the face it was placed against.
            var facing = direction.getOpposite();
            var state = defaultBlockState().setValue(FACING, facing);

            if (canSurvive(state, level, pos) && tapsAvailable(level, pos, facing)) {
                return state;
            }
        }

        return null;
    }

    @Override
    protected boolean canSurvive(@NotNull BlockState state, @NotNull LevelReader level, @NotNull BlockPos pos) {
        var trunk = pos.relative(state.getValue(FACING));

        return level.getBlockState(trunk).is(BlockTags.JUNGLE_LOGS);
    }

    @Override
    protected @NotNull BlockState updateShape(
        @NotNull BlockState state,
        @NotNull Direction direction,
        @NotNull BlockState neighborState,
        @NotNull LevelAccessor level,
        @NotNull BlockPos pos,
        @NotNull BlockPos neighborPos
    ) {
        // Fell the tree and the tap falls off with it.
        return direction == state.getValue(FACING) && !canSurvive(state, level, pos)
            ? net.minecraft.world.level.block.Blocks.AIR.defaultBlockState()
            : super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new LatexTapBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(
        @NotNull Level level,
        @NotNull BlockState state,
        @NotNull BlockEntityType<T> blockEntityType
    ) {
        return level.isClientSide
            ? null
            : createTickerHelper(
                blockEntityType,
                HumanBlockEntityTypes.LATEX_TAP.get(),
                (l, p, s, blockEntity) -> blockEntity.serverTick(l, p, s)
            );
    }

    /** True when this trunk is a real tree AND has room for another tap. */
    public static boolean tapsAvailable(LevelReader level, BlockPos tapPos, Direction facing) {
        var trunk = collectTrunk(level, tapPos.relative(facing));

        if (trunk.size() < MIN_TRUNK_LOGS || !hasLeaves(level, trunk)) {
            return false;
        }

        var limit = isLargeTrunk(trunk) ? MAX_TAPS_LARGE_TREE : MAX_TAPS_SMALL_TREE;

        return countTaps(level, trunk) < limit;
    }

    /** Flood fill through connected jungle logs, so one tree is one trunk however it grew. */
    private static Set<BlockPos> collectTrunk(LevelReader level, BlockPos start) {
        var found = new HashSet<BlockPos>();
        var queue = new ArrayDeque<BlockPos>();

        if (!level.getBlockState(start).is(BlockTags.JUNGLE_LOGS)) {
            return found;
        }

        queue.add(start);
        found.add(start);

        while (!queue.isEmpty() && found.size() < MAX_TRUNK_SEARCH) {
            var current = queue.removeFirst();

            for (var direction : Direction.values()) {
                var next = current.relative(direction);

                if (found.contains(next) || !level.getBlockState(next).is(BlockTags.JUNGLE_LOGS)) {
                    continue;
                }

                found.add(next);
                queue.add(next);
            }
        }

        return found;
    }

    /** ⚠ The anti-cheese check: a built wall of jungle logs has no canopy. */
    private static boolean hasLeaves(LevelReader level, Set<BlockPos> trunk) {
        for (var log : trunk) {
            for (
                var offset : BlockPos.betweenClosed(
                    log.offset(-1, 0, -1),
                    log.offset(1, LEAF_SEARCH_RADIUS, 1)
                )
            ) {
                if (level.getBlockState(offset).is(BlockTags.LEAVES)) {
                    return true;
                }
            }
        }

        return false;
    }

    /** A large jungle tree has a 2x2 trunk, so it is wider than one column on BOTH horizontal axes. */
    private static boolean isLargeTrunk(Set<BlockPos> trunk) {
        var minX = Integer.MAX_VALUE;
        var maxX = Integer.MIN_VALUE;
        var minZ = Integer.MAX_VALUE;
        var maxZ = Integer.MIN_VALUE;

        for (var log : trunk) {
            minX = Math.min(minX, log.getX());
            maxX = Math.max(maxX, log.getX());
            minZ = Math.min(minZ, log.getZ());
            maxZ = Math.max(maxZ, log.getZ());
        }

        return maxX > minX && maxZ > minZ;
    }

    private static int countTaps(LevelReader level, Set<BlockPos> trunk) {
        var taps = 0;

        for (var log : trunk) {
            for (var direction : Direction.Plane.HORIZONTAL) {
                var side = log.relative(direction);
                var state = level.getBlockState(side);

                // A tap counts only if it is actually driven into THIS trunk.
                if (
                    state.getBlock() instanceof LatexTapBlock
                        && side.relative(state.getValue(FACING)).equals(log)
                ) {
                    taps++;
                }
            }
        }

        return taps;
    }
}
