package com.human.common.gameplay.block.tank;

import com.human.common.gameplay.fluid.FluidContainerInteractions;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A multiblock fluid tank block - one per edition, all sharing this code. [stated] Sep 28/29 design review; the full
 * spec is {@code FLUID_TANK_DESIGN.md}.
 * <ul>
 * <li>Place blocks together and they merge into one tank (square, up to 5 x 5 x 32); break one and it splits -
 * {@link FluidTankFormation}.</li>
 * <li>64 buckets per block, held by the tank's controller ({@link FluidTankBlockEntity}).</li>
 * <li>Buckets and canisters fill and drain it; comparators read the fill level.</li>
 * <li>Pipes: in through the top, out through the bottom, sides input by default. [stated] Sep 29 - everything is BARE
 * HAND, like the pipes' own settings, no tool: right-click a side where a pipe is attached to switch that port between
 * input and output.</li>
 * <li>Empty hand anywhere else: says what is inside. SNEAK + empty hand: windows on or off for the whole tank.</li>
 * <li>The item form does NOT keep fluid, as in Create.</li>
 * </ul>
 * The block-state properties are written only by forming; the models (design section 5) pick every face from them.
 */
public class FluidTankBlock extends BaseEntityBlock {

    public static final EnumProperty<TankAxisPos> X_POS = EnumProperty.create("x_pos", TankAxisPos.class);

    public static final EnumProperty<TankAxisPos> Z_POS = EnumProperty.create("z_pos", TankAxisPos.class);

    public static final EnumProperty<TankHeightPos> Y_POS = EnumProperty.create("y_pos", TankHeightPos.class);

    public static final EnumProperty<TankWindow> X_WINDOW = EnumProperty.create("x_window", TankWindow.class);

    public static final EnumProperty<TankWindow> Z_WINDOW = EnumProperty.create("z_window", TankWindow.class);

    private final FluidTankEdition edition;

    private final MapCodec<FluidTankBlock> codec;

    public FluidTankBlock(FluidTankEdition edition, Properties properties) {
        super(properties);
        this.edition = edition;
        this.codec = simpleCodec(settings -> edition.hasPanels() ? new WyFluidTankBlock(settings) : new FluidTankBlock(edition, settings));

        registerDefaultState(
            stateDefinition.any()
                .setValue(X_POS, TankAxisPos.SINGLE)
                .setValue(Z_POS, TankAxisPos.SINGLE)
                .setValue(Y_POS, TankHeightPos.SINGLE)
                .setValue(X_WINDOW, TankWindow.FULL)
                .setValue(Z_WINDOW, TankWindow.FULL)
        );
    }

    public FluidTankEdition getEdition() {
        return edition;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(X_POS, Z_POS, Y_POS, X_WINDOW, Z_WINDOW);
    }

    @Override
    protected @NotNull MapCodec<? extends BaseEntityBlock> codec() {
        return codec;
    }

    @Override
    protected @NotNull RenderShape getRenderShape(@NotNull BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new FluidTankBlockEntity(pos, state);
    }

    // ------------------------------------------------------------------------------------------------------------
    // Forming
    // ------------------------------------------------------------------------------------------------------------

    /**
     * ⚠ Vanilla calls onPlace on EVERY state change, properties included (read in {@code LevelChunk.setBlockState}).
     * Forming stamps tank blocks with setBlock, so a change where the block was already a tank is ignored - otherwise
     * every stamp would start another re-form.
     */
    @Override
    protected void onPlace(
        @NotNull BlockState state,
        @NotNull Level level,
        @NotNull BlockPos pos,
        @NotNull BlockState oldState,
        boolean movedByPiston
    ) {
        super.onPlace(state, level, pos, oldState, movedByPiston);

        if (!level.isClientSide && !oldState.is(this)) {
            FluidTankFormation.onPlaced(level, pos, this);

            if (level.getBlockEntity(pos) instanceof FluidTankBlockEntity tank) {
                tank.refreshPipes();
            }
        }
    }

    /** A neighbour changed - a pipe may have been attached or removed, so the valves are re-checked. */
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

        if (!level.isClientSide && level.getBlockEntity(pos) instanceof FluidTankBlockEntity tank) {
            tank.refreshPipes();
        }
    }

    /**
     * Same guard as onPlace. For a real removal, what this block knew about its tank is captured BEFORE super removes
     * the block entity - if it was the controller, that is the only copy of the tank's fluid.
     */
    @Override
    protected void onRemove(
        @NotNull BlockState state,
        @NotNull Level level,
        @NotNull BlockPos pos,
        @NotNull BlockState newState,
        boolean movedByPiston
    ) {
        if (level.isClientSide || newState.is(this)) {
            super.onRemove(state, level, pos, newState, movedByPiston);
            return;
        }

        FluidTankFormation.Removed removed = null;

        if (level.getBlockEntity(pos) instanceof FluidTankBlockEntity tank && tank.hasTank()) {
            var controller = tank.controller();

            if (controller != null) {
                removed = new FluidTankFormation.Removed(
                    pos.immutable(),
                    controller.getBlockPos().immutable(),
                    controller.getContent(),
                    controller.getWidth() * controller.getWidth() * controller.getHeight(),
                    controller.hasWindows()
                );
            }
        }

        super.onRemove(state, level, pos, newState, movedByPiston);
        FluidTankFormation.onRemoved(level, pos, this, removed);
    }

    // ------------------------------------------------------------------------------------------------------------
    // Using it
    // ------------------------------------------------------------------------------------------------------------

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
        if (!(level.getBlockEntity(pos) instanceof FluidTankBlockEntity tank) || !tank.hasTank()) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        return FluidContainerInteractions.useItem(stack, level, pos, player, hand, tank);
    }

    /**
     * Bare hand. Sneaking: windows on or off for the whole tank. Otherwise, on a face with a pipe attached: a side port
     * switches between input and output, and the top or bottom says it is fixed. Anywhere else: what is inside.
     */
    @Override
    protected @NotNull InteractionResult useWithoutItem(
        @NotNull BlockState state,
        @NotNull Level level,
        @NotNull BlockPos pos,
        @NotNull Player player,
        @NotNull BlockHitResult hit
    ) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        if (!(level.getBlockEntity(pos) instanceof FluidTankBlockEntity tank)) {
            return InteractionResult.PASS;
        }

        var controller = tank.controller();

        if (controller == null) {
            return InteractionResult.PASS;
        }

        if (player.isShiftKeyDown()) {
            var windows = !controller.hasWindows();

            FluidTankFormation.setWindows(level, controller, windows);
            level.playSound(null, pos, SoundEvents.IRON_TRAPDOOR_CLOSE, SoundSource.BLOCKS, 0.6F, windows ? 1.2F : 0.9F);
            player.displayClientMessage(
                Component.translatable(windows ? "message.avp_human.fluid_tank.windows_on" : "message.avp_human.fluid_tank.windows_off"),
                true
            );
        } else if (
            com.human.common.gameplay.fluid.FluidConnections.isConnected(level, pos, hit.getDirection())
                && !(level.getBlockState(pos.relative(hit.getDirection())).getBlock() instanceof FluidTankBlock)
        ) {
            var face = hit.getDirection();

            if (face.getAxis().isHorizontal()) {
                var output = tank.toggleSide(face);

                level.playSound(null, pos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.4F, output ? 0.8F : 1.0F);
                player.displayClientMessage(
                    Component.translatable(output ? "message.avp_human.fluid_tank.side_output" : "message.avp_human.fluid_tank.side_input"),
                    true
                );
            } else {
                player.displayClientMessage(Component.translatable("message.avp_human.fluid_tank.fixed_port"), true);
            }
        } else {
            player.displayClientMessage(FluidContainerInteractions.describe(tank), true);
        }

        return InteractionResult.CONSUME;
    }

    @Override
    protected boolean hasAnalogOutputSignal(@NotNull BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos) {
        return level.getBlockEntity(pos) instanceof FluidTankBlockEntity tank ? tank.signal() : 0;
    }
}
