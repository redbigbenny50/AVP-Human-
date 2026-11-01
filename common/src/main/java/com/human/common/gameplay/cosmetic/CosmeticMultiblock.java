package com.human.common.gameplay.cosmetic;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A block that covers several cells, the fabricator's rules generalised to any footprint:
 * <ul>
 * <li><b>PLACING</b> like a bed - every cell is checked first (replaceable, inside the world, nobody standing in it), so
 * it places whole or not at all. The player places the {@link #anchorCell() anchor}; the CORE (cell 0) and the rest
 * follow.</li>
 * <li><b>BREAKING</b> any cell breaks the core properly, so its one item drops. Cells whose core is gone tidy
 * themselves away; the core never removes itself on a neighbour change.</li>
 * </ul>
 * {@link #CELL} is shared by every cosmetic block (0-11), so every blockstate file lists the same 48 states.
 */
public abstract class CosmeticMultiblock extends HorizontalDirectionalBlock {

    public static final int MAX_CELLS = 12;

    public static final IntegerProperty CELL = IntegerProperty.create("cell", 0, MAX_CELLS - 1);

    /** The per-cell boxes, facing north, in cell-local pixels; null cells have no geometry of their own. */
    private final int[][] boxes;

    /** [facing 2D index][cell] */
    private final VoxelShape[][] shapes;

    protected CosmeticMultiblock(Properties properties, String tableName) {
        super(properties);
        this.boxes = CosmeticModelTables.BOXES.get(tableName);
        this.shapes = new VoxelShape[3 * 4][MAX_CELLS];
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(CELL, 0));
    }

    /** {cx, cy, cz} per cell from the core (core first): +x clockwise of the facing, +y up, +z back. */
    public abstract int[][] cells();

    /** The cell the player's click places. */
    public int anchorCell() {
        return 0;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.@NotNull Builder<Block, BlockState> builder) {
        builder.add(FACING, CELL);
    }

    // ------------------------------------------------------------------------------------------------------------
    // Cell maths
    // ------------------------------------------------------------------------------------------------------------

    public static BlockPos cellPos(BlockPos core, Direction facing, int[] cell) {
        return core.relative(facing.getClockWise(), cell[0]).above(cell[1]).relative(facing.getOpposite(), cell[2]);
    }

    public static BlockPos fromCell(BlockPos pos, Direction facing, int[] cell) {
        return pos.relative(facing.getClockWise(), -cell[0]).below(cell[1]).relative(facing.getOpposite(), -cell[2]);
    }

    public BlockPos corePos(BlockPos pos, BlockState state) {
        var cell = Math.min(state.getValue(CELL), cells().length - 1);

        return fromCell(pos, state.getValue(FACING), cells()[cell]);
    }

    /** The core of the cosmetic block at {@code pos}, or {@code pos} itself when there is none. */
    public static BlockPos corePosOrSelf(BlockGetter level, BlockPos pos) {
        var state = level.getBlockState(pos);

        return state.getBlock() instanceof CosmeticMultiblock block ? block.corePos(pos, state) : pos;
    }

    /** True when {@code pos} holds a cell of the block whose core is {@code core}. */
    public boolean isCellOf(BlockGetter level, BlockPos pos, BlockPos core) {
        var state = level.getBlockState(pos);

        return state.is(this) && corePos(pos, state).equals(core);
    }

    // ------------------------------------------------------------------------------------------------------------
    // Placing and breaking
    // ------------------------------------------------------------------------------------------------------------

    /**
     * Which way the placed block faces, or null to refuse the placement. By default it faces the player who places it;
     * wall pieces override this to face out of the wall that was clicked.
     */
    protected @Nullable Direction placementFacing(BlockPlaceContext context) {
        return context.getHorizontalDirection().getOpposite();
    }

    @Override
    public @Nullable BlockState getStateForPlacement(@NotNull BlockPlaceContext context) {
        var facing = placementFacing(context);

        if (facing == null) {
            return null;
        }

        var anchor = context.getClickedPos();
        var level = context.getLevel();
        var cells = cells();
        var core = fromCell(anchor, facing, cells[anchorCell()]);

        for (int index = 0; index < cells.length; index++) {
            if (index == anchorCell()) {
                continue;
            }

            var pos = cellPos(core, facing, cells[index]);

            if (
                !level.getBlockState(pos).canBeReplaced(context) || !level.getWorldBorder().isWithinBounds(pos) || pos.getY() >= level
                    .getMaxBuildHeight() || pos.getY() < level.getMinBuildHeight()
            ) {
                return null;
            }

            // Nobody standing in the space - a cell placed into a player or mob would trap them inside it.
            if (!level.isUnobstructed(defaultBlockState(), pos, CollisionContext.empty())) {
                return null;
            }
        }

        return defaultBlockState().setValue(FACING, facing).setValue(CELL, anchorCell());
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
        var cells = cells();
        var core = fromCell(pos, facing, cells[anchorCell()]);

        // The CORE first, so every other cell finds its core the moment it is placed.
        for (int index = 0; index < cells.length; index++) {
            if (index != anchorCell()) {
                level.setBlock(cellPos(core, facing, cells[index]), state.setValue(CELL, index), Block.UPDATE_ALL);
            }
        }

        // The item's components (a crafter's contents) belong to the CORE. BlockItem applied them to the clicked
        // position, which is not the core when the anchor is another cell - so they are applied here.
        if (anchorCell() != 0) {
            var blockEntity = level.getBlockEntity(core);

            if (blockEntity != null) {
                blockEntity.applyComponentsFromItemStack(stack);
                blockEntity.setChanged();
            }
        }
    }

    /** A cell with no core goes away. The core stays put whatever happens to its cells. */
    @Override
    protected @NotNull BlockState updateShape(
        @NotNull BlockState state,
        @NotNull Direction direction,
        @NotNull BlockState neighbor,
        @NotNull LevelAccessor level,
        @NotNull BlockPos pos,
        @NotNull BlockPos neighborPos
    ) {
        if (state.getValue(CELL) != 0) {
            var core = level.getBlockState(corePos(pos, state));

            if (!core.is(this) || core.getValue(CELL) != 0 || core.getValue(FACING) != state.getValue(FACING)) {
                return Blocks.AIR.defaultBlockState();
            }
        }

        return super.updateShape(state, direction, neighbor, level, pos, neighborPos);
    }

    /** Breaking a cell breaks the core properly, so its item drops. */
    @Override
    public @NotNull BlockState playerWillDestroy(
        @NotNull Level level,
        @NotNull BlockPos pos,
        @NotNull BlockState state,
        @NotNull Player player
    ) {
        if (!level.isClientSide) {
            var core = corePos(pos, state);

            beforeCoreBroken(level, core, player);

            if (state.getValue(CELL) != 0 && level.getBlockState(core).is(this)) {
                level.destroyBlock(core, !player.isCreative(), player);
            }
        }

        return super.playerWillDestroy(level, pos, state, player);
    }

    /** Hook before the core goes (the crafter drops its contents in creative here). */
    protected void beforeCoreBroken(Level level, BlockPos core, Player player) {}

    @Override
    public @NotNull ItemStack getCloneItemStack(@NotNull LevelReader level, @NotNull BlockPos pos, @NotNull BlockState state) {
        return new ItemStack(this);
    }

    // ------------------------------------------------------------------------------------------------------------
    // Shapes - from the model's own cubes, per cell, turned to the facing
    // ------------------------------------------------------------------------------------------------------------

    @Override
    protected @NotNull VoxelShape getShape(
        @NotNull BlockState state,
        @NotNull BlockGetter level,
        @NotNull BlockPos pos,
        @NotNull CollisionContext context
    ) {
        var facing = state.getValue(FACING);
        var cell = state.getValue(CELL);
        var mount = mountIndex(state);
        var slot = mount * 4 + facing.get2DDataValue();
        var cached = shapes[slot][cell];

        if (cached == null) {
            cached = buildShape(facing, cell, mount);
            shapes[slot][cell] = cached;
        }

        return cached;
    }

    /** 0 = as authored (on the floor, or on the wall for wall pieces), 1 = lying on a floor, 2 = on a ceiling. */
    protected int mountIndex(BlockState state) {
        return 0;
    }

    private VoxelShape buildShape(Direction facing, int cell, int mount) {
        if (boxes == null || cell >= boxes.length || boxes[cell] == null) {
            return Shapes.block();
        }

        var b = boxes[cell];
        double x1 = b[0], y1 = b[1], z1 = b[2], x2 = b[3], y2 = b[4], z2 = b[5];

        // Mounted on a floor / ceiling: turned about X through the block centre first (front up / front down), exactly
        // as the model is (vanilla x 270 / x 90), then by the facing like every other piece.
        if (mount == 1) {
            double ny1 = 16 - z2, ny2 = 16 - z1, nz1 = y1, nz2 = y2;
            y1 = ny1; y2 = ny2; z1 = nz1; z2 = nz2;
        } else if (mount == 2) {
            double ny1 = z1, ny2 = z2, nz1 = 16 - y2, nz2 = 16 - y1;
            y1 = ny1; y2 = ny2; z1 = nz1; z2 = nz2;
        }

        // Turned clockwise seen from above: north as authored, then east 90, south 180, west 270.
        return switch (facing) {
            case EAST -> Block.box(16 - z2, y1, x1, 16 - z1, y2, x2);
            case SOUTH -> Block.box(16 - x2, y1, 16 - z2, 16 - x1, y2, 16 - z1);
            case WEST -> Block.box(z1, y1, 16 - x2, z2, y2, 16 - x1);
            default -> Block.box(x1, y1, z1, x2, y2, z2);
        };
    }
}
