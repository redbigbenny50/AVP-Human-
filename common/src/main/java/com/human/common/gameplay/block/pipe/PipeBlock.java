package com.human.common.gameplay.block.pipe;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;

/**
 * What every pipe shares - the fluid pipe and the item pipe. [stated] Sep 29/30: his models (a 6x6 core, an arm to each
 * connected face, an 8x8 flange) and the same rules for both.
 * <h2>DUMB</h2> A pipe holds nothing and does nothing on its own: its NETWORK moves things straight from the blocks
 * feeding it into the blocks accepting it.
 * <h2>WHAT EACH FACE IS</h2> {@link Side#NONE}, {@link Side#PIPE} (another pipe OF THE SAME KIND - item and fluid pipes
 * never join) or {@link Side#PORT} (a block the pipe can move things in or out of). A port's flange is coloured by what
 * that face does - green in, red out, yellow both - drawn by the pipe's renderer; only pipes with a port have a block
 * entity.
 * <h2>THE FLANGE CLICK</h2> [stated] Sep 30: a bare-hand right-click on a pipe's flange sets that end: Auto (what the
 * block's face does), Pull (red - take from this block), Push (green - put into it), Both (yellow - a buffer) or Off.
 * <h2>JOINERS</h2> [stated] Sep 30:
 * <ul>
 * <li>A straight run gets a joiner between block 5 and 6, 10 and 11 ... counted from where the run starts.</li>
 * <li>"Any kind of direction change count it like an additional piece of pipe": a bend, T or cross is a FITTING, with a
 * joiner on each of its pipe arms, and the run after it starts counting again.</li>
 * </ul>
 * {@link #RUN} carries it: 0-4 is the position in a straight run, {@value #FITTING} a fitting. Each pipe reads the pipe
 * BEHIND it (west / north / down), so a change re-numbers the run through ordinary neighbour updates, and the model
 * draws every joiner without any renderer: a fitting on its pipe arms, a straight pipe at 4 on its forward face.
 */
public abstract class PipeBlock extends BaseEntityBlock {

    public static final int FITTING = 5;

    /** Horizontal speed kept each tick while a player squeezes past - about 60 % of walking pace. */
    private static final double SQUEEZE = 0.7D;

    public static final IntegerProperty RUN = IntegerProperty.create("run", 0, FITTING);

    public enum Side implements StringRepresentable {

        NONE("none"),
        PIPE("pipe"),
        PORT("port");

        private final String name;

        Side(String name) {
            this.name = name;
        }

        @Override
        public @NotNull String getSerializedName() {
            return name;
        }

        public boolean connected() {
            return this != NONE;
        }
    }

    public static final Map<Direction, EnumProperty<Side>> SIDES = new EnumMap<>(Direction.class);

    static {
        for (var direction : Direction.values()) {
            SIDES.put(direction, EnumProperty.create(direction.getSerializedName(), Side.class));
        }
    }

    private static final VoxelShape CORE = Block.box(5, 6, 5, 11, 12, 11);

    private static final Map<Direction, VoxelShape> ARMS = new EnumMap<>(Direction.class);

    static {
        ARMS.put(Direction.NORTH, Block.box(5, 6, 0, 11, 12, 5));
        ARMS.put(Direction.SOUTH, Block.box(5, 6, 11, 11, 12, 16));
        ARMS.put(Direction.WEST, Block.box(0, 6, 5, 5, 12, 11));
        ARMS.put(Direction.EAST, Block.box(11, 6, 5, 16, 12, 11));
        ARMS.put(Direction.UP, Block.box(5, 12, 5, 11, 16, 11));
        ARMS.put(Direction.DOWN, Block.box(5, 0, 5, 11, 6, 11));
    }

    /**
     * Every possible pipe shape - core plus any set of arms, 64 of them - built once. {@code getShape} is called for
     * every collision check near a pipe and every frame its outline is drawn, and merging shapes is one of the costlier
     * geometry operations; a table lookup is not.
     */
    private static final VoxelShape[] SHAPES = new VoxelShape[64];

    static {
        for (var mask = 0; mask < SHAPES.length; mask++) {
            var shape = CORE;

            for (var direction : Direction.values()) {
                if ((mask & 1 << direction.ordinal()) != 0) {
                    shape = Shapes.or(shape, ARMS.get(direction));
                }
            }

            SHAPES[mask] = shape.optimize();
        }
    }

    private static int mask(BlockState state) {
        var mask = 0;

        for (var direction : Direction.values()) {
            if (side(state, direction).connected()) {
                mask |= 1 << direction.ordinal();
            }
        }

        return mask;
    }

    protected PipeBlock(Properties properties) {
        super(properties);

        var state = stateDefinition.any().setValue(RUN, 0);

        for (var property : SIDES.values()) {
            state = state.setValue(property, Side.NONE);
        }

        registerDefaultState(state);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(RUN);
        SIDES.values().forEach(builder::add);
    }

    public static Side side(BlockState state, Direction face) {
        return state.getValue(SIDES.get(face));
    }

    // ------------------------------------------------------------------------------------------------------------
    // What differs between the fluid and the item pipe
    // ------------------------------------------------------------------------------------------------------------

    /** Whether this neighbour is a pipe of the same kind - fluid pipes join fluid pipes, item pipes item pipes. */
    protected abstract boolean joins(BlockState neighbour);

    /** Whether the block at {@code pos} can take or give on its {@code side} face - a port for this kind of pipe. */
    protected abstract boolean isPort(Level level, BlockPos pos, Direction side);

    /**
     * Oct 7 - whether the block at {@code pos} is open fluid an OPEN PIPE END may draw from (see compute). Only the
     * fluid pipe says yes; item pipes never do.
     */
    protected boolean isOpenEndPort(Level level, BlockPos pos) {
        return false;
    }

    /** The block entity for a pipe with at least one port. */
    protected abstract BlockEntity newPortEntity(BlockPos pos, BlockState state);

    /** Tell this pipe's network something changed here. */
    protected abstract void invalidate(ServerLevel level, BlockPos pos);

    // ------------------------------------------------------------------------------------------------------------
    // Connections
    // ------------------------------------------------------------------------------------------------------------

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        // [stated] Oct 5: never into a sentry turret's space (see SentryTurret.standsIn).
        var pos = context.getClickedPos();

        if (com.human.common.gameplay.entity.machine.SentryTurret.standsIn(context.getLevel(), pos)) {
            return null;
        }

        return compute(context.getLevel(), context.getClickedPos(), defaultBlockState());
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
        // ⚠ The server decides. A client may not see another mod's block the way the server does, so it keeps what
        // the server sent rather than guessing.
        if (level.isClientSide()) {
            return state;
        }

        return compute(level, pos, state);
    }

    /**
     * A tank switching a side, or another mod's block appearing, does not always change the neighbour's shape - so a
     * plain neighbour change is looked at too, and the network told.
     */
    @Override
    protected void neighborChanged(
        @NotNull BlockState state,
        @NotNull Level level,
        @NotNull BlockPos pos,
        @NotNull Block neighborBlock,
        @NotNull BlockPos neighborPos,
        boolean movedByPiston
    ) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston);

        if (level.isClientSide) {
            return;
        }

        var updated = compute(level, pos, state);

        if (updated != state) {
            level.setBlock(pos, updated, Block.UPDATE_ALL);
        }

        invalidate((ServerLevel) level, pos);
    }

    @Override
    protected void onPlace(
        @NotNull BlockState state,
        @NotNull Level level,
        @NotNull BlockPos pos,
        @NotNull BlockState oldState,
        boolean movedByPiston
    ) {
        super.onPlace(state, level, pos, oldState, movedByPiston);

        if (!level.isClientSide && !state.is(oldState.getBlock())) {
            invalidate((ServerLevel) level, pos);
        }
    }

    @Override
    protected void onRemove(
        @NotNull BlockState state,
        @NotNull Level level,
        @NotNull BlockPos pos,
        @NotNull BlockState newState,
        boolean movedByPiston
    ) {
        if (!level.isClientSide && !state.is(newState.getBlock())) {
            invalidate((ServerLevel) level, pos);
        }

        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    /** Every face, then this pipe's place in its run. */
    private BlockState compute(LevelAccessor level, BlockPos pos, BlockState state) {
        var connections = 0;

        for (var direction : Direction.values()) {
            var side = sideToward(level, pos, direction);

            state = state.setValue(SIDES.get(direction), side);

            if (side.connected()) {
                connections++;
            }
        }

        // ⭐ Oct 7 - [stated] "a pipe that ends in a liquid can be set to intake fluid". A pipe END - connected on
        // exactly one side - whose far face looks into open fluid gets a port there, so its flange can be set to Pull
        // (intake). Only an end: a pipe running along a lake bed or through a flooded corridor sprouts no flanges.
        if (connections == 1 && level instanceof Level world) {
            for (var direction : Direction.values()) {
                if (!side(state, direction).connected()) {
                    continue;
                }

                var open = direction.getOpposite();
                var neighbour = pos.relative(open);

                if (side(state, open) == Side.NONE && world.isLoaded(neighbour) && isOpenEndPort(world, neighbour)) {
                    state = state.setValue(SIDES.get(open), Side.PORT);
                    connections++;
                }

                break;
            }
        }

        return state.setValue(RUN, run(level, pos, state, connections));
    }

    private Side sideToward(LevelAccessor level, BlockPos pos, Direction direction) {
        var neighbour = pos.relative(direction);

        if (!(level instanceof Level world) ? !level.hasChunkAt(neighbour) : !world.isLoaded(neighbour)) {
            return Side.NONE;
        }

        if (joins(level.getBlockState(neighbour))) {
            return Side.PIPE;
        }

        return level instanceof Level world && isPort(world, neighbour, direction.getOpposite()) ? Side.PORT : Side.NONE;
    }

    /** The axis a pipe runs straight along, or null for a fitting (a bend, a T, a cross). */
    public static Direction.@Nullable Axis straightAxis(BlockState state) {
        Direction.Axis axis = null;

        for (var direction : Direction.values()) {
            if (!side(state, direction).connected()) {
                continue;
            }

            if (axis == null) {
                axis = direction.getAxis();
            } else if (axis != direction.getAxis()) {
                return null;
            }
        }

        return axis;
    }

    private int run(LevelAccessor level, BlockPos pos, BlockState state, int connections) {
        if (connections == 0) {
            return 0;
        }

        var axis = straightAxis(state);

        if (axis == null) {
            return FITTING;
        }

        // The pipe behind, along the run - west, north or down.
        var back = Direction.fromAxisAndDirection(axis, Direction.AxisDirection.NEGATIVE);

        if (side(state, back) != Side.PIPE) {
            return 0;
        }

        var behind = level.getBlockState(pos.relative(back));

        if (!(behind.getBlock() instanceof PipeBlock) || !joins(behind) || straightAxis(behind) != axis) {
            // The run starts here - after a fitting, [stated] "count it like an additional piece of pipe".
            return 0;
        }

        var previous = behind.getValue(RUN);

        return previous >= FITTING ? 0 : (previous + 1) % 5;
    }

    // ------------------------------------------------------------------------------------------------------------
    // Shape and block entity
    // ------------------------------------------------------------------------------------------------------------

    @Override
    protected @NotNull VoxelShape getShape(
        @NotNull BlockState state,
        @NotNull BlockGetter level,
        @NotNull BlockPos pos,
        @NotNull CollisionContext context
    ) {
        return SHAPES[mask(state)];
    }

    @Override
    protected @NotNull RenderShape getRenderShape(@NotNull BlockState state) {
        return RenderShape.MODEL;
    }

    // ------------------------------------------------------------------------------------------------------------
    // Players pass through, slowed
    // ------------------------------------------------------------------------------------------------------------

    /**
     * [stated] Sep 30: "we want the player to be able to pass through the pipes and wires but you get slowed" - and
     * also leashed animals the player is pulling, hired marines and pets
     * ({@link com.human.common.gameplay.block.pipe.PipePassage}). They get no collision; everything else collides as
     * before.
     */
    @Override
    protected @NotNull VoxelShape getCollisionShape(
        @NotNull BlockState state,
        @NotNull BlockGetter level,
        @NotNull BlockPos pos,
        @NotNull CollisionContext context
    ) {
        if (
            context instanceof net.minecraft.world.phys.shapes.EntityCollisionContext entityContext
                && com.human.common.gameplay.block.pipe.PipePassage.passes(entityContext.getEntity())
        ) {
            return Shapes.empty();
        }

        return getShape(state, level, pos, context);
    }

    /**
     * Squeezing past: a player touching the pipe or cable itself - not merely its block space - loses some horizontal
     * speed each tick, like pushing through berry bushes. Vertical movement is untouched, so jumping, falling and fall
     * damage are exactly as normal (vanilla's "stuck in block" slow resets fall distance, which would let a fall be
     * cancelled by dropping through a pipe). Flying and spectating players are not slowed.
     */
    @Override
    protected void entityInside(
        @NotNull BlockState state,
        @NotNull Level level,
        @NotNull BlockPos pos,
        @NotNull net.minecraft.world.entity.Entity entity
    ) {
        super.entityInside(state, level, pos, entity);

        if (
            !com.human.common.gameplay.block.pipe.PipePassage.passes(entity)
                || !com.human.common.gameplay.block.pipe.PipePassage.slowed(entity)
        ) {
            return;
        }

        var shape = getShape(state, level, pos, CollisionContext.empty()).move(pos.getX(), pos.getY(), pos.getZ());

        if (!Shapes.joinIsNotEmpty(shape, Shapes.create(entity.getBoundingBox()), net.minecraft.world.phys.shapes.BooleanOp.AND)) {
            return;
        }

        var motion = entity.getDeltaMovement();

        entity.setDeltaMovement(motion.x * SQUEEZE, motion.y, motion.z * SQUEEZE);
    }

    /** Only a pipe that touches a port needs one - for its coloured flanges and for the network to find it. */
    @Override
    public @Nullable BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        for (var direction : Direction.values()) {
            if (side(state, direction) == Side.PORT) {
                return newPortEntity(pos, state);
            }
        }

        return null;
    }

    // ------------------------------------------------------------------------------------------------------------
    // The flange click
    // ------------------------------------------------------------------------------------------------------------

    /**
     * Bare hand on a flange: cycle that end. The clicked face if it is a port, otherwise the port the click landed
     * nearest - a flange is small, and a click on the core should still reach the obvious end.
     */
    @Override
    protected @NotNull InteractionResult useWithoutItem(
        @NotNull BlockState state,
        @NotNull Level level,
        @NotNull BlockPos pos,
        @NotNull Player player,
        @NotNull BlockHitResult hitResult
    ) {
        var face = portNearest(state, pos, hitResult);

        if (face == null) {
            return InteractionResult.PASS;
        }

        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        // [stated] Sep 30: sneak opens that end's screen - mode, filter and search. A plain click cycles the mode.
        if (player.isShiftKeyDown()) {
            var serverLevel = (ServerLevel) level;
            var end = face;

            player.openMenu(
                new net.minecraft.world.SimpleMenuProvider(
                    (containerId, inventory, menuPlayer) -> new PipeEndMenu(containerId, inventory, serverLevel, pos, end),
                    net.minecraft.network.chat.Component.translatable(
                        "gui.avp_human.pipe_end.title",
                        net.minecraft.network.chat.Component.translatable("message.avp_human.direction." + end.getSerializedName())
                    )
                )
            );

            return InteractionResult.CONSUME;
        }

        if (level.getBlockEntity(pos) instanceof PipeBlockEntity pipe) {
            var mode = pipe.cycleOverride(face);

            player.displayClientMessage(
                net.minecraft.network.chat.Component.translatable(
                    "message.avp_human.pipe_end",
                    net.minecraft.network.chat.Component.translatable("message.avp_human.direction." + face.getSerializedName()),
                    net.minecraft.network.chat.Component.translatable("message.avp_human.pipe_end." + PipeBlockEntity.overrideName(mode))
                ),
                true
            );
            level.playSound(null, pos, net.minecraft.sounds.SoundEvents.LEVER_CLICK, net.minecraft.sounds.SoundSource.BLOCKS, 0.5F, 1.1F);
            invalidate((ServerLevel) level, pos);
        }

        return InteractionResult.CONSUME;
    }

    private static @Nullable Direction portNearest(BlockState state, BlockPos pos, BlockHitResult hit) {
        if (side(state, hit.getDirection()) == Side.PORT) {
            return hit.getDirection();
        }

        var local = hit.getLocation().subtract(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D);
        Direction best = null;
        var bestDot = -Double.MAX_VALUE;

        for (var direction : Direction.values()) {
            if (side(state, direction) != Side.PORT) {
                continue;
            }

            var dot = local.x * direction.getStepX() + local.y * direction.getStepY() + local.z * direction.getStepZ();

            if (dot > bestDot) {
                bestDot = dot;
                best = direction;
            }
        }

        return best;
    }
}
