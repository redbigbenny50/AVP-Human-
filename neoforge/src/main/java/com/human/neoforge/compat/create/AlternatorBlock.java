package com.human.neoforge.compat.create;

import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * The alternator: Create rotation in, avp_human power out.
 * <p>
 * [stated] Oct 24 - "front is blank back left and right are ins and the top and bottoms are out." Back, left and right
 * all sit around the UPRIGHT axis, so this is a Create gearbox standing up: shafts connect on any of the three, they
 * share one rotation, and the front simply has no shaft. Power leaves through the top and bottom only.
 * </p>
 */
public class AlternatorBlock extends HorizontalKineticBlock implements IBE<AlternatorBlockEntity> {

    /** [stated] Oct 24 - the front LED: lit while it is generating. */
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    private final ConverterEdition edition;

    public AlternatorBlock(Properties properties, ConverterEdition edition) {
        super(properties);
        this.edition = edition;
        registerDefaultState(defaultBlockState().setValue(LIT, false));
    }

    public ConverterEdition edition() {
        return edition;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(LIT);
    }

    /** The front faces the player who placed it, like every other machine. */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(HORIZONTAL_FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
        return face.getAxis().isHorizontal() && face != state.getValue(HORIZONTAL_FACING);
    }

    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        return Direction.Axis.Y;
    }

    /**
     * ⚠ Create only re-forms the network when this says the states differ. Its own test compares rotation axes, which
     * are always upright here - so turning the block with a wrench (which moves the blank front, and so which sides
     * have shafts) would have left the network on the old shafts. The LED switching changes nothing kinetic.
     */
    @Override
    protected boolean areStatesKineticallyEquivalent(BlockState oldState, BlockState newState) {
        return super.areStatesKineticallyEquivalent(oldState, newState)
            && oldState.getValue(HORIZONTAL_FACING) == newState.getValue(HORIZONTAL_FACING);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);

        if (!level.isClientSide) {
            if (oldState.getBlock() != state.getBlock()) {
                ConverterGrid.onPlaced(level, pos);
            } else if (
                oldState.hasProperty(HORIZONTAL_FACING) && oldState.getValue(HORIZONTAL_FACING) != state.getValue(HORIZONTAL_FACING)
            ) {
                // Turned with a wrench: its power faces moved with it, so the grid must be re-formed around it - the
                // same queued split a removal uses, then a fresh join through the faces it has now.
                ConverterGrid.onRemoved(level, pos);
                ConverterGrid.onPlaced(level, pos);
            }
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!level.isClientSide && state.getBlock() != newState.getBlock()) {
            ConverterGrid.onRemoved(level, pos);
        }

        super.onRemove(state, level, pos, newState, isMoving);
    }

    /** [stated] Oct 24 - a simple reading screen. Wrench and value-box clicks are Create's and never reach here. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        if (level.getBlockEntity(pos) instanceof AlternatorBlockEntity menu) {
            player.openMenu(menu, buf -> buf.writeBlockPos(pos));
        }

        return InteractionResult.CONSUME;
    }

    @Override
    public Class<AlternatorBlockEntity> getBlockEntityClass() {
        return AlternatorBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends AlternatorBlockEntity> getBlockEntityType() {
        return HumanCreate.ALTERNATOR_ENTITY.get();
    }
}
