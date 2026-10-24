package com.human.common.gameplay.quarry;

import com.human.common.registry.init.HumanQuarry;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
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

import java.util.EnumMap;
import java.util.Map;

/**
 * The quarry terminal: six blocks, see {@link QuarryTerminalPart}. Three editions share this class.
 * <h2>PLACING</h2> Like the fabricator: every space is checked first, so it places whole or not at all. On top of that
 * it must stand just outside a chunk edge with its back to the chunk - {@link QuarryPlacement} - and the player is told
 * how far to move when it does not. The placement hologram shows the same thing before they click.
 * <h2>BRAIN</h2> The block entity sits on BACK_TOP, under the power port; see {@link QuarryTerminalBlockEntity}.
 * <h2>BREAKING</h2> Breaking ANY part breaks the core properly, so the one item drops. Parts whose core is gone tidy
 * themselves away; the core never removes itself on a neighbour change.
 */
public class QuarryTerminalBlock extends HorizontalDirectionalBlock implements EntityBlock {

    /**
     * ⚠ The codec only matters to vanilla's block-type registry; it rebuilds the BARE edition. Every real terminal is
     * created by {@code HumanQuarry} with its own edition.
     */
    public static final MapCodec<QuarryTerminalBlock> CODEC = simpleCodec(
        properties -> new QuarryTerminalBlock(QuarryEdition.BARE, properties)
    );

    public static final EnumProperty<QuarryTerminalPart> PART = EnumProperty.create("part", QuarryTerminalPart.class);

    /**
     * The console slope, roughly: the back half full height, the front half a low step - so a player walking up the
     * front does not float on air over the screen. Built for NORTH (front at z = 0) and turned for the others.
     */
    private static final Map<Direction, VoxelShape> CONSOLE_SHAPES = new EnumMap<>(Direction.class);

    static {
        for (var facing : Direction.Plane.HORIZONTAL) {
            CONSOLE_SHAPES.put(facing, Shapes.or(box(facing, 0, 0, 8, 16, 16, 16), box(facing, 0, 0, 0, 16, 8, 8)));
        }
    }

    private final QuarryEdition edition;

    public QuarryTerminalBlock(QuarryEdition edition, Properties properties) {
        super(properties);
        this.edition = edition;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(PART, QuarryTerminalPart.CORE));
    }

    public QuarryEdition edition() {
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
        // The front faces the player; the back faces the chunk they are looking into.
        var facing = context.getHorizontalDirection().getOpposite();
        var core = context.getClickedPos();
        var level = context.getLevel();
        var state = defaultBlockState().setValue(FACING, facing).setValue(PART, QuarryTerminalPart.CORE);
        var result = QuarryPlacement.check(level, core, facing, state, context);

        if (result.ok()) {
            return state;
        }

        // Tell the player why, once, from the client (the server runs this too and stays quiet).
        if (level.isClientSide && context.getPlayer() != null) {
            context.getPlayer().displayClientMessage(refusal(result), true);
        }

        return null;
    }

    /** The line shown when a terminal cannot go where the player aimed it. */
    public static Component refusal(QuarryPlacement.Result result) {
        if (!result.fits()) {
            return Component.translatable("message.avp_human.quarry.place.no_room");
        }

        var blocks = Math.abs(result.shift());

        return Component.translatable(
            result.shift() > 0 ? "message.avp_human.quarry.place.move_toward" : "message.avp_human.quarry.place.move_away",
            blocks
        );
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

        for (var part : QuarryTerminalPart.values()) {
            if (part != QuarryTerminalPart.CORE) {
                level.setBlock(part.fromCore(pos, facing), state.setValue(PART, part), Block.UPDATE_ALL);
            }
        }

        // The placer owns it: their own claims are theirs to mine.
        if (placer != null && level.getBlockEntity(BRAIN.fromCore(pos, facing)) instanceof QuarryTerminalBlockEntity terminal) {
            terminal.setOwner(placer.getUUID());
        }
    }

    /** A part with no core goes away. The core stays put whatever happens to its parts. */
    @Override
    protected @NotNull BlockState updateShape(
        @NotNull BlockState state,
        @NotNull Direction direction,
        @NotNull BlockState neighbor,
        @NotNull LevelAccessor level,
        @NotNull BlockPos pos,
        @NotNull BlockPos neighborPos
    ) {
        if (state.getValue(PART) != QuarryTerminalPart.CORE) {
            var core = level.getBlockState(corePos(pos, state));

            if (!core.is(this) || core.getValue(PART) != QuarryTerminalPart.CORE || core.getValue(FACING) != state.getValue(FACING)) {
                return Blocks.AIR.defaultBlockState();
            }
        }

        return super.updateShape(state, direction, neighbor, level, pos, neighborPos);
    }

    /** Breaking a part breaks the core properly, so the terminal item drops (once - parts drop nothing). */
    @Override
    public @NotNull BlockState playerWillDestroy(
        @NotNull Level level,
        @NotNull BlockPos pos,
        @NotNull BlockState state,
        @NotNull Player player
    ) {
        if (!level.isClientSide && state.getValue(PART) != QuarryTerminalPart.CORE) {
            var core = corePos(pos, state);

            if (level.getBlockState(core).is(this)) {
                level.destroyBlock(core, !player.isCreative(), player);
            }
        }

        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected @NotNull VoxelShape getShape(
        @NotNull BlockState state,
        @NotNull BlockGetter level,
        @NotNull BlockPos pos,
        @NotNull CollisionContext context
    ) {
        return state.getValue(PART) == QuarryTerminalPart.CONSOLE ? CONSOLE_SHAPES.get(state.getValue(FACING)) : Shapes.block();
    }

    /**
     * The brain (block entity) lives on the BACK_TOP block: its top face is the power port. It also draws the model.
     */
    public static final QuarryTerminalPart BRAIN = QuarryTerminalPart.BACK_TOP;

    public static BlockPos brainPos(BlockPos pos, BlockState state) {
        return BRAIN.fromCore(corePos(pos, state), state.getValue(FACING));
    }

    @Override
    protected @NotNull RenderShape getRenderShape(@NotNull BlockState state) {
        return state.getValue(PART) == BRAIN ? RenderShape.ENTITYBLOCK_ANIMATED : RenderShape.INVISIBLE;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return state.getValue(PART) == BRAIN ? new QuarryTerminalBlockEntity(pos, state) : null;
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(
        @NotNull Level level,
        @NotNull BlockState state,
        @NotNull BlockEntityType<T> type
    ) {
        if (level.isClientSide || type != HumanQuarry.QUARRY_TERMINAL_BLOCK_ENTITY.get() || state.getValue(PART) != BRAIN) {
            return null;
        }

        return (tickLevel, pos, tickState, entity) -> ((QuarryTerminalBlockEntity) entity).serverTick(tickLevel, pos, tickState);
    }

    /** Any of the six blocks opens the terminal's screen. */
    @Override
    protected @NotNull InteractionResult useWithoutItem(
        @NotNull BlockState state,
        @NotNull Level level,
        @NotNull BlockPos pos,
        @NotNull Player player,
        @NotNull BlockHitResult hit
    ) {
        if (!level.isClientSide && level.getBlockEntity(brainPos(pos, state)) instanceof QuarryTerminalBlockEntity terminal) {
            player.openMenu(
                new SimpleMenuProvider(
                    (id, inventory, opener) -> new QuarryTerminalMenu(id, inventory, terminal),
                    Component.translatable(getDescriptionId())
                )
            );
        }

        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /** Breaking the terminal drops everything it holds - nothing is ever deleted. */
    @Override
    protected void onRemove(
        @NotNull BlockState state,
        @NotNull Level level,
        @NotNull BlockPos pos,
        @NotNull BlockState newState,
        boolean movedByPiston
    ) {
        if (
            !state.is(newState.getBlock()) && state.getValue(PART) == BRAIN && level.getBlockEntity(
                pos
            ) instanceof QuarryTerminalBlockEntity terminal
        ) {
            terminal.dropContents(level, pos);
            terminal.forget(level);
        }

        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public @NotNull ItemStack getCloneItemStack(@NotNull LevelReader level, @NotNull BlockPos pos, @NotNull BlockState state) {
        return new ItemStack(this);
    }

    /** A box given in NORTH-facing coordinates (front at z = 0), turned to {@code facing}. */
    private static VoxelShape box(Direction facing, double x0, double y0, double z0, double x1, double y1, double z1) {
        return switch (facing) {
            case SOUTH -> Block.box(16 - x1, y0, 16 - z1, 16 - x0, y1, 16 - z0);
            case EAST -> Block.box(16 - z1, y0, x0, 16 - z0, y1, x1);
            case WEST -> Block.box(z0, y0, 16 - x1, z1, y1, 16 - x0);
            default -> Block.box(x0, y0, z0, x1, y1, z1);
        };
    }

    /**
     * The LIQUID port ([stated] Oct 13: top right, the hydrator's liquid texture): the TOP face of the back-row
     * counter-clockwise block. Returns the terminal's tank there, null on every other face of every other part. Each
     * loader exposes it to pipes - ours and other mods' - through its fluid capability / transfer API.
     */
    public static com.human.common.gameplay.fluid.@Nullable FluidEndpoint liquidPort(
        net.minecraft.world.level.Level level,
        BlockPos pos,
        BlockState state,
        @Nullable net.minecraft.core.Direction side
    ) {
        if (
            side != net.minecraft.core.Direction.UP || !(state.getBlock() instanceof QuarryTerminalBlock) || state.getValue(
                PART
            ) != QuarryTerminalPart.BACK_CCW
        ) {
            return null;
        }

        return level.getBlockEntity(brainPos(pos, state)) instanceof QuarryTerminalBlockEntity terminal ? terminal.tank() : null;
    }

    /**
     * Hands block events to the block entity (the atomizer pulse). HorizontalDirectionalBlock does not do this itself -
     * only BaseEntityBlock does - so without it the event is dropped and never reaches clients.
     */
    @Override
    protected boolean triggerEvent(BlockState state, net.minecraft.world.level.Level level, BlockPos pos, int id, int param) {
        super.triggerEvent(state, level, pos, id, param);

        var blockEntity = level.getBlockEntity(pos);

        return blockEntity != null && blockEntity.triggerEvent(id, param);
    }
}
