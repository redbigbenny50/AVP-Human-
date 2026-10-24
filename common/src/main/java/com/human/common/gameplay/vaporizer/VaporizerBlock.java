package com.human.common.gameplay.vaporizer;

import com.human.common.gameplay.block.entity.machine.ElectricMachineBlockEntity;
import com.human.common.gameplay.block.power.machine.ElectricMachineBlock;
import com.human.common.gameplay.machine.ElectricMachineKind;
import com.human.common.registry.init.HumanVaporizer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.WorldlyContainerHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The vaporizer's BOTTOM block - the machine itself: power, tank, slots and model all live here. [stated] Oct 17 it is
 * 1 x 1 and two blocks tall; {@link VaporizerTopBlock} fills the space above.
 * <h2>PLACING</h2> Like the fabricator: the space above is checked first, so it places whole or not at all.
 * <h2>BREAKING</h2> Breaking either block breaks this one properly, so its item and its contents drop; the top block
 * never holds anything and drops nothing.
 * <h2>EDITIONS</h2> Industrial, W-Y and USCM share one block class and one block entity type; only the skin differs.
 */
public class VaporizerBlock extends ElectricMachineBlock implements WorldlyContainerHolder {

    public VaporizerBlock(Properties properties) {
        super(properties, ElectricMachineKind.VAPORIZER);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        var level = context.getLevel();
        var above = context.getClickedPos().above();

        if (
            above.getY() >= level.getMaxBuildHeight() || !level.getWorldBorder().isWithinBounds(above)
                || !level.getBlockState(above).canBeReplaced(context)
                // Nobody standing in the space: the top block would trap them inside it.
                || !level.isUnobstructed(HumanVaporizer.VAPORIZER_TOP.get().defaultBlockState(), above, CollisionContext.empty())
        ) {
            return null;
        }

        return super.getStateForPlacement(context);
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

        if (!level.isClientSide) {
            level.setBlock(pos.above(), HumanVaporizer.VAPORIZER_TOP.get().defaultBlockState(), Block.UPDATE_ALL);
        }
    }

    /** When the machine goes, its top block goes with it - after the contents have been spilled by the machine base. */
    @Override
    protected void onRemove(
        @NotNull BlockState state,
        @NotNull Level level,
        @NotNull BlockPos pos,
        @NotNull BlockState newState,
        boolean movedByPiston
    ) {
        var blockChanged = !state.is(newState.getBlock());

        super.onRemove(state, level, pos, newState, movedByPiston);

        if (blockChanged && !level.isClientSide && level.getBlockState(pos.above()).getBlock() instanceof VaporizerTopBlock) {
            level.setBlock(pos.above(), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        }
    }

    /** The geo model is drawn by VaporizerRenderer; there is no baked block model to draw. */
    @Override
    protected @NotNull RenderShape getRenderShape(@NotNull BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    /** The bottom block has no item port - see {@link VaporizerPortContainer}. */
    @Override
    public @NotNull WorldlyContainer getContainer(@NotNull BlockState state, @NotNull LevelAccessor level, @NotNull BlockPos pos) {
        return level.getBlockEntity(pos) instanceof ElectricMachineBlockEntity machine
            ? new VaporizerPortContainer(machine, false)
            : EMPTY_PORTS;
    }

    /** A port container with nothing behind it, for a lookup that lands mid-placement or mid-break. */
    static final WorldlyContainer EMPTY_PORTS = new EmptyPorts();

    private static final class EmptyPorts extends net.minecraft.world.SimpleContainer implements WorldlyContainer {

        private static final int[] NONE = {};

        EmptyPorts() {
            super(0);
        }

        @Override
        public int @NotNull [] getSlotsForFace(@NotNull net.minecraft.core.Direction side) {
            return NONE;
        }

        @Override
        public boolean canPlaceItemThroughFace(int slot, @NotNull ItemStack stack, @Nullable net.minecraft.core.Direction side) {
            return false;
        }

        @Override
        public boolean canTakeItemThroughFace(int slot, @NotNull ItemStack stack, @NotNull net.minecraft.core.Direction side) {
            return false;
        }
    }

    /** The machine at the core of a vaporizer, from either of its blocks - or null. */
    public static @Nullable ElectricMachineBlockEntity machineAt(LevelAccessor level, BlockPos pos) {
        var state = level.getBlockState(pos);
        var core = state.getBlock() instanceof VaporizerTopBlock ? pos.below() : pos;

        return level.getBlockState(core).getBlock() instanceof VaporizerBlock
            && level.getBlockEntity(core) instanceof ElectricMachineBlockEntity machine ? machine : null;
    }
}
