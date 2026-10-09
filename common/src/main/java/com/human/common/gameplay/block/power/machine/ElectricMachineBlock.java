package com.human.common.gameplay.block.power.machine;

import com.human.common.gameplay.block.entity.machine.ElectricMachineBlockEntity;
import com.human.common.gameplay.block.power.PowerNodeEntityBlock;
import com.human.common.gameplay.machine.ElectricMachineKind;
import com.human.common.gameplay.machine.MachineState;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * One block class for every tier-1 cooking machine; {@link ElectricMachineKind} says which.
 * <p>
 * ⚠ The codec rebuilds with {@link ElectricMachineKind#FURNACE}. Block codecs exist for data-driven block definitions
 * and are never used to recreate a registered block, so the kind passed there is never read - but every block must
 * provide one.
 * </p>
 */
public class ElectricMachineBlock extends PowerNodeEntityBlock<ElectricMachineBlockEntity> {

    public static final MapCodec<ElectricMachineBlock> CODEC = simpleCodec(
        properties -> new ElectricMachineBlock(properties, ElectricMachineKind.FURNACE)
    );

    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    public static final EnumProperty<MachineState> STATE = EnumProperty.create("state", MachineState.class);

    private final ElectricMachineKind kind;

    public ElectricMachineBlock(Properties properties, ElectricMachineKind kind) {
        super(properties);
        this.kind = kind;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(STATE, MachineState.OFF));
    }

    public ElectricMachineKind getKind() {
        return kind;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, STATE);
    }

    /** The front faces the player who placed it, so the power port on the back faces away. */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected @NotNull BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected @NotNull BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    /**
     * A bucket or universal canister on a machine with a tank pours in (freezer) or draws off (press, hydrator),
     * exactly as on a fluid tank - the machine's own insert/extract rules decide which. Anything else, or a container
     * that has nothing to do here, falls through to opening the screen.
     */
    @Override
    protected @NotNull net.minecraft.world.ItemInteractionResult useItemOn(
        @NotNull net.minecraft.world.item.ItemStack stack,
        @NotNull BlockState state,
        @NotNull Level level,
        @NotNull BlockPos pos,
        @NotNull Player player,
        @NotNull net.minecraft.world.InteractionHand hand,
        @NotNull BlockHitResult hitResult
    ) {
        if (kind.tank() != null && !player.isShiftKeyDown() && level.getBlockEntity(pos) instanceof ElectricMachineBlockEntity machine) {
            var result = com.human.common.gameplay.fluid.FluidContainerInteractions.useItem(stack, level, pos, player, hand, machine);

            if (result != net.minecraft.world.ItemInteractionResult.FAIL) {
                return result;
            }
        }

        return net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    /**
     * Plain right-click opens the machine. Sneaking goes to the shared power-block handling, which toggles the back
     * breaker when the back face is clicked and does nothing on any other face.
     */
    @Override
    protected @NotNull InteractionResult useWithoutItem(
        @NotNull BlockState state,
        @NotNull Level level,
        @NotNull BlockPos pos,
        @NotNull Player player,
        @NotNull BlockHitResult hitResult
    ) {
        if (player.isShiftKeyDown()) {
            return super.useWithoutItem(state, level, pos, player, hitResult);
        }

        if (!level.isClientSide && level.getBlockEntity(pos) instanceof ElectricMachineBlockEntity machine) {
            player.openMenu(machine);
        }

        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void onRemove(
        @NotNull BlockState state,
        @NotNull Level level,
        @NotNull BlockPos pos,
        @NotNull BlockState newState,
        boolean movedByPiston
    ) {
        // ⚠ Only when the BLOCK goes. A STATE change (off / on / working) is the same block and must not spill items.
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof ElectricMachineBlockEntity machine) {
            Containers.dropContents(level, pos, machine.getItems());
            Containers.dropContents(level, pos, machine.getUpgrades());

            if (level instanceof ServerLevel serverLevel) {
                machine.dropExperience(serverLevel, Vec3.atCenterOf(pos));
            }

            level.updateNeighbourForOutputSignal(pos, this);
        }

        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    protected boolean hasAnalogOutputSignal(@NotNull BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos) {
        return level.getBlockEntity(pos) instanceof ElectricMachineBlockEntity machine
            ? net.minecraft.world.inventory.AbstractContainerMenu.getRedstoneSignalFromContainer(machine)
            : 0;
    }

    /**
     * Server ticks come from the shared power-block ticker. The CLIENT gets one too, but only to follow the machine's
     * state for its animation clock and working sound - it never runs game logic.
     */
    @Override
    @Nullable
    public <U extends BlockEntity> BlockEntityTicker<U> getTicker(
        @NotNull Level level,
        @NotNull BlockState blockState,
        @NotNull BlockEntityType<U> blockEntityType
    ) {
        if (!level.isClientSide) {
            return super.getTicker(level, blockState, blockEntityType);
        }

        return createTickerHelper(
            blockEntityType,
            kind.blockEntityType().get(),
            (clientLevel, pos, state, machine) -> ElectricMachineBlockEntity.clientTick(machine)
        );
    }

    /** ⚠ BaseEntityBlock defaults to INVISIBLE - the baked model is only drawn when this says MODEL. */
    @Override
    protected @NotNull RenderShape getRenderShape(@NotNull BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected @NotNull MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(@NotNull BlockPos blockPos, @NotNull BlockState blockState) {
        return new ElectricMachineBlockEntity(kind, blockPos, blockState);
    }
}
