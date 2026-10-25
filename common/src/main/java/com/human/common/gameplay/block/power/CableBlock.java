package com.human.common.gameplay.block.power;

import com.human.common.gameplay.power.PowerNode;
import com.human.common.gameplay.power.PowerSystem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class CableBlock extends Block implements net.minecraft.world.level.block.EntityBlock, net.minecraft.world.level.block.SimpleWaterloggedBlock {

    public static final BooleanProperty WATERLOGGED = net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED;

    /**
     * [stated] Sep 30: whether any arm meets a BLOCK (not another cable) - such a cable ends in his coloured flange
     * there, drawn by {@link com.human.client.render.block.CableFlangeRenderer}. Only these cables get a block entity.
     */
    public static final BooleanProperty PORTED = BooleanProperty.create("ported");

    /** Horizontal speed kept each tick while a player squeezes past - about 60 % of walking pace. */
    private static final double SQUEEZE = 0.7D;

    private static final Direction[] VALUES = Direction.values();

    public static final BooleanProperty NORTH = BlockStateProperties.NORTH;

    public static final BooleanProperty SOUTH = BlockStateProperties.SOUTH;

    public static final BooleanProperty EAST = BlockStateProperties.EAST;

    public static final BooleanProperty WEST = BlockStateProperties.WEST;

    public static final BooleanProperty UP = BlockStateProperties.UP;

    public static final BooleanProperty DOWN = BlockStateProperties.DOWN;

    public static final Map<Direction, BooleanProperty> PROPERTY_BY_DIRECTION = Map.of(
        Direction.NORTH,
        NORTH,
        Direction.SOUTH,
        SOUTH,
        Direction.EAST,
        EAST,
        Direction.WEST,
        WEST,
        Direction.UP,
        UP,
        Direction.DOWN,
        DOWN
    );

    private static final VoxelShape CORE = box(6, 6, 6, 10, 10, 10);

    private static final VoxelShape SIDE_NORTH = box(6, 6, 0, 10, 10, 6);

    private static final VoxelShape SIDE_SOUTH = box(6, 6, 10, 10, 10, 16);

    private static final VoxelShape SIDE_EAST = box(10, 6, 6, 16, 10, 10);

    private static final VoxelShape SIDE_WEST = box(0, 6, 6, 6, 10, 10);

    private static final VoxelShape SIDE_UP = box(6, 10, 6, 10, 16, 10);

    private static final VoxelShape SIDE_DOWN = box(6, 0, 6, 10, 6, 10);

    // bit order: N,S,E,W,U,D
    private static final VoxelShape[] SHAPES_BY_MASK = makeShapes();

    private static VoxelShape[] makeShapes() {
        var voxelShapes = new VoxelShape[64];

        for (var mask = 0; mask < 64; mask++) {
            var voxelShape = CORE;

            if ((mask & 1) != 0) {
                voxelShape = Shapes.or(voxelShape, SIDE_NORTH);
            }

            if ((mask & 2) != 0) {
                voxelShape = Shapes.or(voxelShape, SIDE_SOUTH);
            }

            if ((mask & 4) != 0) {
                voxelShape = Shapes.or(voxelShape, SIDE_EAST);
            }

            if ((mask & 8) != 0) {
                voxelShape = Shapes.or(voxelShape, SIDE_WEST);
            }

            if ((mask & 16) != 0) {
                voxelShape = Shapes.or(voxelShape, SIDE_UP);
            }

            if ((mask & 32) != 0) {
                voxelShape = Shapes.or(voxelShape, SIDE_DOWN);
            }

            // merge boxes to reduce raytrace cost.
            voxelShapes[mask] = voxelShape.optimize();
        }
        return voxelShapes;
    }

    private static int maskFromState(BlockState s) {
        var mask = 0;

        if (s.getValue(NORTH)) {
            mask |= 1;
        }

        if (s.getValue(SOUTH)) {
            mask |= 2;
        }

        if (s.getValue(EAST)) {
            mask |= 4;
        }

        if (s.getValue(WEST)) {
            mask |= 8;
        }

        if (s.getValue(UP)) {
            mask |= 16;
        }

        if (s.getValue(DOWN)) {
            mask |= 32;
        }

        return mask;
    }

    public CableBlock(Properties properties) {
        super(properties);

        registerDefaultState(
            this.defaultBlockState()
                .setValue(NORTH, false)
                .setValue(SOUTH, false)
                .setValue(EAST, false)
                .setValue(WEST, false)
                .setValue(UP, false)
                .setValue(DOWN, false)
                .setValue(PORTED, false)
                .setValue(WATERLOGGED, false)
        );
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        var level = context.getLevel();
        var pos = context.getClickedPos();

        // [stated] Oct 5: never into a sentry turret's space.
        if (com.human.common.gameplay.entity.machine.SentryTurret.standsIn(level, pos)) {
            return null;
        }

        // Placed into water, it is placed waterlogged.
        var water = level.getFluidState(pos).getType() == net.minecraft.world.level.material.Fluids.WATER;

        return computeState(level, pos, this.defaultBlockState().setValue(WATERLOGGED, water));
    }

    /**
     * Each arm: toward our cables and power blocks as always, and - [stated] Sep 30 - toward another mod's energy block
     * whose face is not switched Off. Then whether any arm meets a block.
     */
    private BlockState computeState(LevelAccessor level, BlockPos pos, BlockState state) {
        var ported = false;

        for (var direction : VALUES) {
            var neighborPos = pos.relative(direction);

            // A neighbour in an unloaded chunk keeps whatever the cable showed - looking would load that chunk.
            if (!level.hasChunkAt(neighborPos)) {
                if (state.getValue(PROPERTY_BY_DIRECTION.get(direction)) && state.getValue(PORTED)) {
                    ported = true;
                }

                continue;
            }

            var arm = canConnectTo(level, neighborPos, direction) || foreignArm(level, pos, direction);

            state = state.setValue(PROPERTY_BY_DIRECTION.get(direction), arm);

            if (arm && !(level.getBlockState(neighborPos).getBlock() instanceof CableBlock)) {
                ported = true;
            }
        }

        return state.setValue(PORTED, ported);
    }

    /** Another mod's energy block on this face, connected in its current mode. Only the server can know. */
    private static boolean foreignArm(LevelAccessor level, BlockPos pos, Direction direction) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return false;
        }

        var target = pos.relative(direction);

        if (
            !serverLevel.isLoaded(target) || com.human.common.gameplay.power.grid.PowerGridExploreUtil.isConnectable(serverLevel, target)
                || !com.human.common.gameplay.power.external.ExternalEnergy.mayHoldEnergy(serverLevel, target)
        ) {
            return false;
        }

        var port = com.human.common.gameplay.power.external.ExternalEnergy.Holder.get().open(serverLevel, target, direction.getOpposite());
        var mode = com.human.common.gameplay.power.external.CablePortModes.get(serverLevel).get(pos, direction);

        return com.human.common.gameplay.power.external.ExternalPortNodes.create(port, mode) != null;
    }

    /**
     * Recomputes the arms and the flange colours - after a neighbour changed or a port mode was switched, neither of
     * which always changes a neighbour's shape.
     */
    public static void refreshVisual(ServerLevel level, BlockPos pos) {
        var state = level.getBlockState(pos);

        if (!(state.getBlock() instanceof CableBlock cable)) {
            return;
        }

        var updated = cable.computeState(level, pos, state);

        if (updated != state) {
            level.setBlock(pos, updated, Block.UPDATE_CLIENTS);
        }

        if (level.getBlockEntity(pos) instanceof com.human.common.gameplay.block.entity.power.CableBlockEntity flanges) {
            for (var direction : VALUES) {
                // Unloaded neighbour: keep the colour it has.
                if (level.isLoaded(pos.relative(direction))) {
                    flanges.setMode(
                        direction,
                        updated.getValue(PROPERTY_BY_DIRECTION.get(direction)) ? flangeColour(level, pos, direction) : 0
                    );
                }
            }
        }
    }

    /**
     * The colour of the flange where this cable meets a block. [stated] RED = power out of our system (a machine),
     * GREEN = into it (a generator), YELLOW = both (a battery). Another mod's block follows the role its port mode
     * gives it. A face meeting another cable has no flange.
     */
    private static byte flangeColour(ServerLevel level, BlockPos pos, Direction direction) {
        var target = pos.relative(direction);

        if (level.getBlockState(target).getBlock() instanceof CableBlock) {
            return 0;
        }

        PowerNode node = level.getBlockEntity(target) instanceof PowerNode ours ? ours : null;

        if (node == null) {
            var port = com.human.common.gameplay.power.external.ExternalEnergy.Holder.get().open(level, target, direction.getOpposite());

            node = com.human.common.gameplay.power.external.ExternalPortNodes.create(
                port,
                com.human.common.gameplay.power.external.CablePortModes.get(level).get(pos, direction)
            );
        }

        return switch (node) {
            case PowerNode.PowerStore store -> com.human.common.gameplay.block.entity.power.CableBlockEntity.BOTH;
            case PowerNode.PowerProducer producer -> com.human.common.gameplay.block.entity.power.CableBlockEntity.IN;
            case PowerNode.PowerConsumer consumer -> com.human.common.gameplay.block.entity.power.CableBlockEntity.OUT;
            case null -> (byte) 0;
        };
    }

    @Override
    public @org.jetbrains.annotations.Nullable net.minecraft.world.level.block.entity.BlockEntity newBlockEntity(
        @NotNull BlockPos pos,
        @NotNull BlockState state
    ) {
        return state.getValue(PORTED) ? new com.human.common.gameplay.block.entity.power.CableBlockEntity(pos, state) : null;
    }

    @Override
    public @NotNull BlockState updateShape(
        BlockState state,
        @NotNull Direction direction,
        @NotNull BlockState neighborState,
        @NotNull LevelAccessor level,
        @NotNull BlockPos pos,
        @NotNull BlockPos neighborPos
    ) {
        // Waterlogged: keep the water in it flowing, as vanilla waterloggable blocks do.
        if (state.getValue(WATERLOGGED)) {
            level.scheduleTick(
                pos,
                net.minecraft.world.level.material.Fluids.WATER,
                net.minecraft.world.level.material.Fluids.WATER.getTickDelay(level)
            );
        }

        // ⚠ The server decides. A client cannot see another mod's energy block, so it keeps what the server sent.
        if (level.isClientSide()) {
            return state;
        }

        return computeState(level, pos, state);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, SOUTH, EAST, WEST, UP, DOWN, PORTED, WATERLOGGED);
    }

    // ------------------------------------------------------------------------------------------------------------
    // Water - Oct 17 FIX: [stated] "pipes both and cables pop when water touches them and they cant be placed in
    // water or waterlogged". A thin block that is not a liquid container is one water washes away, and one placed into
    // water is washed away the moment the water next moves. Now it is WATERLOGGABLE like a vanilla fence or chain: it
    // can be placed in water, sits in it, and water flowing into it fills it instead of breaking it.
    // ------------------------------------------------------------------------------------------------------------

    @Override
    protected @NotNull net.minecraft.world.level.material.FluidState getFluidState(@NotNull BlockState state) {
        return state.getValue(WATERLOGGED)
            ? net.minecraft.world.level.material.Fluids.WATER.getSource(false)
            : super.getFluidState(state);
    }

    @Override
    public @NotNull VoxelShape getShape(
        @NotNull BlockState state,
        @NotNull BlockGetter level,
        @NotNull BlockPos pos,
        @NotNull CollisionContext ctx
    ) {
        // used for outline + ray hit.
        return SHAPES_BY_MASK[maskFromState(state)];
    }

    @Override
    public @NotNull VoxelShape getOcclusionShape(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos) {
        // matches lighting/occlusion too
        return SHAPES_BY_MASK[maskFromState(state)];
    }

    @Override
    public boolean propagatesSkylightDown(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos) {
        // let skylight pass through to blocks below.
        return true;
    }

    @Override
    public int getLightBlock(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos) {
        // this block blocks 0 light (0..15).
        return 0;
    }

    @Override
    public boolean useShapeForLightOcclusion(@NotNull BlockState state) {
        return true;
    }

    // TODO: Deduplicate this code.
    @Override
    protected void onPlace(
        @NotNull BlockState state,
        @NotNull Level level,
        @NotNull BlockPos blockPos,
        @NotNull BlockState oldState,
        boolean movedByPiston
    ) {
        super.onPlace(state, level, blockPos, oldState, movedByPiston);

        // ⚠ Only a real placement. The cable's own arm or flange state changing calls this too, and must not re-link.
        if (level.isClientSide || oldState.is(state.getBlock())) {
            return;
        }

        var manager = PowerSystem.get((ServerLevel) level);

        for (var direction : VALUES) {
            var neighbor = blockPos.relative(direction);

            if (canConnectTo(level, neighbor, direction)) {
                manager.union(blockPos, neighbor);
            }
        }

        // Other mods' energy blocks around the new cable.
        manager.refreshCable(level, blockPos);
        refreshVisual((ServerLevel) level, blockPos);
    }

    /** A block beside the cable changed - another mod's machine placed or broken: look at the faces again. */
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

        // Another cable changing never changes this cable's ports or flanges - its arms follow through updateShape.
        if (!level.isClientSide && !(level.getBlockState(neighborPos).getBlock() instanceof CableBlock)) {
            PowerSystem.get((ServerLevel) level).refreshCable(level, pos);
            refreshVisual((ServerLevel) level, pos);
        }
    }

    /**
     * [stated] Sep 30: bare-hand right-click a cable that touches another mod's energy block opens its port screen,
     * where each such face is set to auto, out, in, both or off. A cable touching nothing foreign does nothing.
     */
    /**
     * Oct 10 - the wiring tool's cable readout must reach the tool: a cable with an external port consumes the click to
     * open its port screen, which ran before the tool and swallowed it. Same fix as PowerNodeEntityBlock#useItemOn.
     */
    @Override
    protected @NotNull net.minecraft.world.ItemInteractionResult useItemOn(
        @NotNull net.minecraft.world.item.ItemStack stack,
        @NotNull BlockState state,
        @NotNull net.minecraft.world.level.Level level,
        @NotNull BlockPos pos,
        @NotNull net.minecraft.world.entity.player.Player player,
        @NotNull net.minecraft.world.InteractionHand hand,
        @NotNull net.minecraft.world.phys.BlockHitResult hitResult
    ) {
        if (stack.getItem() instanceof com.human.common.gameplay.item.WiringToolItem) {
            return net.minecraft.world.ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
        }

        return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
    }

    @Override
    protected @NotNull net.minecraft.world.InteractionResult useWithoutItem(
        @NotNull BlockState state,
        @NotNull Level level,
        @NotNull BlockPos pos,
        @NotNull net.minecraft.world.entity.player.Player player,
        @NotNull net.minecraft.world.phys.BlockHitResult hitResult
    ) {
        if (level.isClientSide) {
            return net.minecraft.world.InteractionResult.SUCCESS;
        }

        var serverLevel = (ServerLevel) level;

        if (com.human.common.gameplay.power.external.CablePortMenu.externalFaces(serverLevel, pos) == 0) {
            return net.minecraft.world.InteractionResult.PASS;
        }

        player.openMenu(
            new net.minecraft.world.SimpleMenuProvider(
                (containerId, inventory, menuPlayer) -> new com.human.common.gameplay.power.external.CablePortMenu(
                    containerId,
                    inventory,
                    serverLevel,
                    pos
                ),
                net.minecraft.network.chat.Component.translatable("gui.avp_human.cable_ports")
            )
        );

        return net.minecraft.world.InteractionResult.CONSUME;
    }

    // TODO: Deduplicate this code.
    @Override
    protected void onRemove(
        @NotNull BlockState state,
        Level level,
        @NotNull BlockPos pos,
        @NotNull BlockState newState,
        boolean movedByPiston
    ) {
        if (!level.isClientSide && state.getBlock() != newState.getBlock()) {
            // Queued, not rebuilt now: one explosion can remove hundreds of blocks in a single tick, and each
            // immediate rebuild walked the whole network up to six times.
            PowerSystem.get((ServerLevel) level).queueSplit(pos);
            com.human.common.gameplay.power.external.CablePortModes.get((ServerLevel) level).clear(pos);
        }

        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    /**
     * @param direction from this cable toward the neighbour
     */
    // TODO: Use block tags here.
    private boolean canConnectTo(LevelAccessor level, BlockPos neighbor, Direction direction) {
        // Cable connects to other cables or valid machines.
        if (level.getBlockState(neighbor).getBlock() instanceof CableBlock) {
            return true;
        }

        var blockEntity = level.getBlockEntity(neighbor);

        // ⚠ The neighbour's FACE decides, not merely that it is a power block. A machine takes power at its back only
        // and a battery never at its front, and a closed breaker is closed to cables too - so the cable neither draws
        // an arm to that face nor joins the grid through it.
        // Oct 24 - PowerFaces, so the Create converters' gear sides take no cable arm (see PowerFaces).
        if (blockEntity instanceof com.human.common.gameplay.power.PowerFaces node) {
            return node.isFaceConnectable(direction.getOpposite());
        }

        return blockEntity instanceof PowerNode;
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

        return SHAPES_BY_MASK[maskFromState(state)];
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

        var shape = SHAPES_BY_MASK[maskFromState(state)].move(pos.getX(), pos.getY(), pos.getZ());

        if (!Shapes.joinIsNotEmpty(shape, Shapes.create(entity.getBoundingBox()), net.minecraft.world.phys.shapes.BooleanOp.AND)) {
            return;
        }

        var motion = entity.getDeltaMovement();

        entity.setDeltaMovement(motion.x * SQUEEZE, motion.y, motion.z * SQUEEZE);
    }
}
