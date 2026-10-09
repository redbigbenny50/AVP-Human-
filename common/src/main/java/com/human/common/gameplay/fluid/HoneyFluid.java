package com.human.common.gameplay.fluid;

import com.human.common.registry.init.HumanFluids;
import com.human.common.registry.init.block.CoreBlocks;
import com.human.common.registry.init.item.HumanItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import org.jetbrains.annotations.NotNull;

/**
 * Liquid honey. [stated] Sep 29: "honey lets do it the same way latex is with flow and viscosity".
 * <p>
 * Built exactly like {@link LatexFluid} - no infinite sources, a short spread, a slow tick - with its own numbers: it
 * spreads the same distance but moves a little faster than latex, as runny honey should. The press makes it from hive
 * blocks, the freezer turns it into a honey block, and a source left in the world hardens into one by itself (see the
 * liquid block).
 * </p>
 */
public abstract class HoneyFluid extends FlowingFluid {

    public static final int SLOPE_FIND_DISTANCE = 3;

    public static final int DROP_OFF = 2;

    /** Latex is 30; water is 5, lava 30. Honey sits between - thick, but not as slow as latex. */
    public static final int TICK_DELAY = 20;

    private static final float EXPLOSION_RESISTANCE = 100.0F;

    @Override
    public @NotNull Fluid getFlowing() {
        return HumanFluids.FLOWING_HONEY.get();
    }

    @Override
    public @NotNull Fluid getSource() {
        return HumanFluids.HONEY.get();
    }

    @Override
    public @NotNull Item getBucket() {
        return HumanItems.HONEY_BUCKET.get();
    }

    @Override
    protected void beforeDestroyingBlock(@NotNull LevelAccessor level, @NotNull BlockPos pos, @NotNull BlockState state) {
        var blockEntity = state.hasBlockEntity() ? level.getBlockEntity(pos) : null;

        net.minecraft.world.level.block.Block.dropResources(state, level, pos, blockEntity);
    }

    @Override
    public int getSlopeFindDistance(@NotNull LevelReader level) {
        return SLOPE_FIND_DISTANCE;
    }

    @Override
    public int getDropOff(@NotNull LevelReader level) {
        return DROP_OFF;
    }

    @Override
    public @NotNull BlockState createLegacyBlock(@NotNull FluidState state) {
        return CoreBlocks.HONEY.get().defaultBlockState().setValue(LiquidBlock.LEVEL, getLegacyLevel(state));
    }

    @Override
    public boolean isSame(@NotNull Fluid fluid) {
        return fluid == HumanFluids.HONEY.get() || fluid == HumanFluids.FLOWING_HONEY.get();
    }

    @Override
    public boolean canBeReplacedWith(
        @NotNull FluidState fluidState,
        @NotNull BlockGetter blockReader,
        @NotNull BlockPos pos,
        @NotNull Fluid fluid,
        @NotNull Direction direction
    ) {
        return !isSame(fluid);
    }

    @Override
    public int getTickDelay(@NotNull LevelReader level) {
        return TICK_DELAY;
    }

    /** No infinite honey: two sources never make a third. */
    @Override
    protected boolean canConvertToSource(@NotNull Level level) {
        return false;
    }

    @Override
    protected float getExplosionResistance() {
        return EXPLOSION_RESISTANCE;
    }

    public static class Flowing extends HoneyFluid {

        @Override
        protected void createFluidStateDefinition(StateDefinition.Builder<Fluid, FluidState> builder) {
            super.createFluidStateDefinition(builder);
            builder.add(LEVEL);
        }

        @Override
        public int getAmount(@NotNull FluidState state) {
            return state.getValue(LEVEL);
        }

        @Override
        public boolean isSource(@NotNull FluidState state) {
            return false;
        }
    }

    public static class Source extends HoneyFluid {

        @Override
        public int getAmount(@NotNull FluidState state) {
            return 8;
        }

        @Override
        public boolean isSource(@NotNull FluidState state) {
            return true;
        }
    }
}
