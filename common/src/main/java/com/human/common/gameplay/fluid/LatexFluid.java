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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import org.jetbrains.annotations.NotNull;

/**
 * Liquid latex. [stated] Sep 24: "liquid latex spreads like lava flows slowly and if left alone the source block
 * hardens into latex blocks and the flowing blocks vanish."
 * <h2>The numbers and why</h2>
 * <ul>
 * <li>{@value #SLOPE_FIND_DISTANCE} spread, {@value #DROP_OFF} drop-off - a puddle reaches about 3 blocks, tighter than
 * water (4 at drop-off 1) and closer to overworld lava (2 at drop-off 2). Latex should pool, not sheet.</li>
 * <li>{@value #TICK_DELAY} tick delay - 3x slower than lava in the overworld and 6x slower than water, which is what
 * "flows slowly" comes to. ⚠ This is also a PERFORMANCE dial: a slower fluid ticks its spread less often, so a big
 * spill costs far less than the same volume of water.</li>
 * <li>Never converts to a source ({@link #canConvertToSource}), so two streams cannot breed infinite latex the way
 * water does. Latex is a resource - an infinite-source trick would make the whole tapping system pointless.</li>
 * </ul>
 * ⭐ The hardening is NOT here. It lives in {@link com.human.common.gameplay.block.LatexLiquidBlock}, because it is a
 * property of a placed block sitting still, and doing it in the fluid would mean random-ticking every flowing block for
 * a result only the source can produce.
 */
public abstract class LatexFluid extends FlowingFluid {

    public static final int SLOPE_FIND_DISTANCE = 3;

    public static final int DROP_OFF = 2;

    /** Water is 5, overworld lava 10. */
    public static final int TICK_DELAY = 30;

    private static final float EXPLOSION_RESISTANCE = 100.0F;

    @Override
    public @NotNull Fluid getFlowing() {
        return HumanFluids.FLOWING_LATEX.get();
    }

    @Override
    public @NotNull Fluid getSource() {
        return HumanFluids.LATEX.get();
    }

    @Override
    public @NotNull Item getBucket() {
        return HumanItems.LATEX_BUCKET.get();
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
        return CoreBlocks.LATEX.get()
            .defaultBlockState()
            .setValue(net.minecraft.world.level.block.LiquidBlock.LEVEL, getLegacyLevel(state));
    }

    @Override
    public boolean isSame(@NotNull Fluid fluid) {
        return fluid == HumanFluids.LATEX.get() || fluid == HumanFluids.FLOWING_LATEX.get();
    }

    @Override
    public boolean canBeReplacedWith(
        @NotNull FluidState fluidState,
        @NotNull BlockGetter blockReader,
        @NotNull BlockPos pos,
        @NotNull Fluid fluid,
        @NotNull Direction direction
    ) {
        // Anything else washes latex away; latex never displaces another fluid.
        return !isSame(fluid);
    }

    @Override
    public int getTickDelay(@NotNull LevelReader level) {
        return TICK_DELAY;
    }

    @Override
    protected boolean canConvertToSource(@NotNull Level level) {
        return false;
    }

    @Override
    protected float getExplosionResistance() {
        return EXPLOSION_RESISTANCE;
    }

    public static class Flowing extends LatexFluid {

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

    public static class Source extends LatexFluid {

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
