package com.human.common.gameplay.block.cryo;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The cryoconduit. [stated] Oct 1: it works like the power cable - a CONNECTION ONLY - and links a cryotube controller
 * to its cryotanks. It has no block entity, no tick and stores nothing; the controller walks the conduit when it needs
 * to know which tanks it feeds.
 * <p>
 * Each face is one of three things: nothing, an arm to the next conduit, or an arm ending in a FLANGE where it seats on
 * a {@link CryoConduitEndpoint} that accepts it on that face. [stated] The flow is one way, controller to tanks, so the
 * flange has no in/out colour and nothing to switch.
 * </p>
 * The models are his: {@code cryoconduit_core}, {@code cryoconduit_side} and {@code cryoconduit_flange}, all pointing
 * north and turned by the blockstate.
 */
public class CryoConduitBlock extends Block {

    public static final MapCodec<CryoConduitBlock> CODEC = simpleCodec(CryoConduitBlock::new);

    public enum Link implements StringRepresentable {

        NONE("none"),
        CONDUIT("conduit"),
        ENDPOINT("endpoint");

        private final String name;

        Link(String name) {
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

    public static final Map<Direction, EnumProperty<Link>> LINKS = new EnumMap<>(Direction.class);

    static {
        for (var direction : Direction.values()) {
            LINKS.put(direction, EnumProperty.create(direction.getSerializedName(), Link.class));
        }
    }

    /** 4x4 conduit through the middle, as his models draw it. */
    private static final VoxelShape CORE = Block.box(6, 6, 6, 10, 10, 10);

    private static final Map<Direction, VoxelShape> ARMS = new EnumMap<>(Direction.class);

    private static final Map<Direction, VoxelShape> FLANGES = new EnumMap<>(Direction.class);

    static {
        ARMS.put(Direction.NORTH, Block.box(6, 6, 0, 10, 10, 6));
        ARMS.put(Direction.SOUTH, Block.box(6, 6, 10, 10, 10, 16));
        ARMS.put(Direction.WEST, Block.box(0, 6, 6, 6, 10, 10));
        ARMS.put(Direction.EAST, Block.box(10, 6, 6, 16, 10, 10));
        ARMS.put(Direction.DOWN, Block.box(6, 0, 6, 10, 6, 10));
        ARMS.put(Direction.UP, Block.box(6, 10, 6, 10, 16, 10));

        // The 6x6 flange, two deep, seated on the face - the same footprint as the controller's ports.
        FLANGES.put(Direction.NORTH, Block.box(5, 5, 0, 11, 11, 2));
        FLANGES.put(Direction.SOUTH, Block.box(5, 5, 14, 11, 11, 16));
        FLANGES.put(Direction.WEST, Block.box(0, 5, 5, 2, 11, 11));
        FLANGES.put(Direction.EAST, Block.box(14, 5, 5, 16, 11, 11));
        FLANGES.put(Direction.DOWN, Block.box(5, 0, 5, 11, 2, 11));
        FLANGES.put(Direction.UP, Block.box(5, 14, 5, 11, 16, 11));
    }

    /** One shape per state, built on first use (729 states, most never placed). Concurrent: render threads ask too. */
    private final Map<BlockState, VoxelShape> shapeCache = new ConcurrentHashMap<>();

    public CryoConduitBlock(Properties properties) {
        super(properties);

        var state = stateDefinition.any();

        for (var property : LINKS.values()) {
            state = state.setValue(property, Link.NONE);
        }

        registerDefaultState(state);
    }

    @Override
    protected @NotNull MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        LINKS.values().forEach(builder::add);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        var level = context.getLevel();
        var pos = context.getClickedPos();
        var state = defaultBlockState();

        for (var direction : Direction.values()) {
            var neighbourPos = pos.relative(direction);
            state = state.setValue(LINKS.get(direction), linkTo(level, neighbourPos, direction));
        }

        return state;
    }

    @Override
    protected @NotNull BlockState updateShape(
        @NotNull BlockState state,
        @NotNull Direction direction,
        @NotNull BlockState neighbourState,
        @NotNull LevelAccessor level,
        @NotNull BlockPos pos,
        @NotNull BlockPos neighbourPos
    ) {
        return state.setValue(LINKS.get(direction), linkTo(neighbourState, direction));
    }

    /**
     * ⚠ Only ever called for a neighbour whose chunk is loaded: placement and {@code updateShape} both happen because
     * that neighbour is there to be read. Nothing here walks further than one block.
     */
    private static Link linkTo(BlockGetter level, BlockPos neighbourPos, Direction direction) {
        return linkTo(level.getBlockState(neighbourPos), direction);
    }

    /**
     * @param direction the direction from this conduit to the neighbour
     */
    public static Link linkTo(BlockState neighbour, Direction direction) {
        if (neighbour.getBlock() instanceof CryoConduitBlock) {
            return Link.CONDUIT;
        }

        if (
            neighbour.getBlock() instanceof CryoConduitEndpoint endpoint
                && endpoint.acceptsCryoConduit(neighbour, direction.getOpposite())
        ) {
            return Link.ENDPOINT;
        }

        return Link.NONE;
    }

    @Override
    protected @NotNull VoxelShape getShape(
        @NotNull BlockState state,
        @NotNull BlockGetter level,
        @NotNull BlockPos pos,
        @NotNull CollisionContext context
    ) {
        return shapeCache.computeIfAbsent(state, CryoConduitBlock::buildShape);
    }

    private static VoxelShape buildShape(BlockState state) {
        var shape = CORE;

        for (var direction : Direction.values()) {
            var link = state.getValue(LINKS.get(direction));

            if (link.connected()) {
                shape = Shapes.or(shape, ARMS.get(direction));
            }

            if (link == Link.ENDPOINT) {
                shape = Shapes.or(shape, FLANGES.get(direction));
            }
        }

        return shape.optimize();
    }
}
