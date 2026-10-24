package com.human.common.gameplay.acid;

import com.human.common.registry.init.HumanChemicals;
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
 * A xeno acid as a fluid. [stated] Oct 17: it flows as far and as fast as WATER - water's own numbers below - but it is
 * never renewable: two sources never make a third, so a poured bucket stays one source. That source fades after 20
 * seconds, like a bleed's blood (see {@link AcidLiquidBlock}), and everything that flowed from it drains away after it.
 */
public abstract class AcidFluid extends FlowingFluid {

    /** Water's numbers: looks 4 blocks for a way down, loses 1 level a block, moves a block every 5 ticks. */
    public static final int SLOPE_FIND_DISTANCE = 4;

    public static final int DROP_OFF = 1;

    public static final int TICK_DELAY = 5;

    private static final float EXPLOSION_RESISTANCE = 100.0F;

    protected final AcidKind kind;

    protected AcidFluid(AcidKind kind) {
        this.kind = kind;
    }

    public AcidKind kind() {
        return kind;
    }

    @Override
    public @NotNull Fluid getFlowing() {
        return HumanChemicals.flowingAcid(kind);
    }

    @Override
    public @NotNull Fluid getSource() {
        return HumanChemicals.acid(kind);
    }

    @Override
    public @NotNull Item getBucket() {
        return HumanChemicals.acidBucket(kind);
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
    public int getTickDelay(@NotNull LevelReader level) {
        return TICK_DELAY;
    }

    @Override
    public @NotNull BlockState createLegacyBlock(@NotNull FluidState state) {
        return HumanChemicals.acidBlock(kind).defaultBlockState().setValue(LiquidBlock.LEVEL, getLegacyLevel(state));
    }

    @Override
    public boolean isSame(@NotNull Fluid fluid) {
        return fluid == HumanChemicals.acid(kind) || fluid == HumanChemicals.flowingAcid(kind);
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

    /** [stated] Never infinite: a poured bucket stays one source. */
    @Override
    protected boolean canConvertToSource(@NotNull Level level) {
        return false;
    }

    @Override
    protected float getExplosionResistance() {
        return EXPLOSION_RESISTANCE;
    }

    public static class Flowing extends AcidFluid {

        public Flowing(AcidKind kind) {
            super(kind);
        }

        @Override
        protected void createFluidStateDefinition(StateDefinition.@NotNull Builder<Fluid, FluidState> builder) {
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

    public static class Source extends AcidFluid {

        public Source(AcidKind kind) {
            super(kind);
        }

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
