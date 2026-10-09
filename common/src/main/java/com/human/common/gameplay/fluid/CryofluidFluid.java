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
 * Cryofluid, the cryotube fuel. [stated] Oct 1: cryogel goes into the press and comes out as cryofluid, which is what
 * the cryotube controller burns.
 * <p>
 * [stated] It has LAVA'S PROPERTIES - thick, slow and short-spreading - but none of lava's heat: no light, no fire, and
 * it puts fires out. Lava's own overworld numbers are used for the spread (slope 2, drop-off 2, 30-tick delay). There
 * are no infinite sources, so every block of it comes out of a press.
 * </p>
 * <p>
 * Standing in it freezes you; see {@link CryoFreezing}, which the liquid block calls.
 * </p>
 */
public abstract class CryofluidFluid extends FlowingFluid {

    public static final int SLOPE_FIND_DISTANCE = 2;

    public static final int DROP_OFF = 2;

    /** Lava's tick delay in the overworld. */
    public static final int TICK_DELAY = 30;

    private static final float EXPLOSION_RESISTANCE = 100.0F;

    @Override
    public @NotNull Fluid getFlowing() {
        return HumanFluids.FLOWING_CRYOFLUID.get();
    }

    @Override
    public @NotNull Fluid getSource() {
        return HumanFluids.CRYOFLUID.get();
    }

    @Override
    public @NotNull Item getBucket() {
        return HumanItems.CRYOFLUID_BUCKET.get();
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
        return CoreBlocks.CRYOFLUID.get().defaultBlockState().setValue(LiquidBlock.LEVEL, getLegacyLevel(state));
    }

    @Override
    public boolean isSame(@NotNull Fluid fluid) {
        return fluid == HumanFluids.CRYOFLUID.get() || fluid == HumanFluids.FLOWING_CRYOFLUID.get();
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

    /** No infinite cryofluid: two sources never make a third. */
    @Override
    protected boolean canConvertToSource(@NotNull Level level) {
        return false;
    }

    @Override
    protected float getExplosionResistance() {
        return EXPLOSION_RESISTANCE;
    }

    public static class Flowing extends CryofluidFluid {

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

    public static class Source extends CryofluidFluid {

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
