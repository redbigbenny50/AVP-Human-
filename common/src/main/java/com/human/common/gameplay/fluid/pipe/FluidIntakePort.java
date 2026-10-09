package com.human.common.gameplay.fluid.pipe;

import com.human.common.gameplay.block.fluid.FluidPipeBlockEntity;
import com.human.common.gameplay.fluid.FluidContent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.Nullable;

/**
 * ⭐ Oct 7 - AN OPEN PIPE END DRAWING FROM THE WORLD. [stated] "a pipe that ends in a liquid can be set to intake fluid
 * so like say i put the end of a pipe into water between two source blocks ie an infinite water source and i set it to
 * intake it will suck up water and it will auto apply that liquid to its filter." / "have it work on any liquid source
 * that way its functionality is the same even if it only sucks it in the one time." A real pump comes later.
 * <ul>
 * <li>Only a pipe END facing open fluid gets this port (PipeBlock.compute), and only while that end is set to
 * <b>Pull</b>; on Auto it does nothing, so a pipe dipped into a lake never starts draining it by itself.</li>
 * <li>Any fluid source that can be scooped: water, lava, black goo, waterlogged blocks. It takes a whole bucket and
 * removes that source, like a bucket. Infinite water refills itself; anything else is gone once taken.</li>
 * <li>One bucket every {@value #INTAKE_INTERVAL_TICKS} ticks per end (two a second), and only when something on the
 * network can take the whole bucket - the network asks the sink first, so a source is never removed for nothing.</li>
 * <li>The first fluid it takes goes into that end's filter (when the filter is empty and set to allow), so the end
 * carries only that fluid from then on. The player can still edit or clear it.</li>
 * <li>Flowing (non-source) fluid is never taken. The world is never filled through an intake.</li>
 * </ul>
 */
public final class FluidIntakePort implements PipeFluidAccess.Port {

    /** Ticks between buckets for one intake end. */
    public static final int INTAKE_INTERVAL_TICKS = 10;

    private final ServerLevel level;

    private final BlockPos target;

    private final @Nullable FluidPipeBlockEntity pipe;

    private final Direction face;

    private FluidIntakePort(ServerLevel level, BlockPos target, @Nullable FluidPipeBlockEntity pipe, Direction face) {
        this.level = level;
        this.target = target;
        this.pipe = pipe;
        this.face = face;
    }

    /** {@return an intake port onto open fluid at {@code target}, or null when there is no fluid there} */
    public static @Nullable FluidIntakePort open(ServerLevel level, BlockPos pipePos, Direction face, BlockPos target) {
        if (level.getFluidState(target).isEmpty() || !(level.getBlockState(target).getBlock() instanceof BucketPickup)) {
            return null;
        }

        var pipe = level.getBlockEntity(pipePos) instanceof FluidPipeBlockEntity entity ? entity : null;

        return new FluidIntakePort(level, target, pipe, face);
    }

    /** The source fluid that can be taken right now, or EMPTY. */
    private Fluid takeable() {
        var fluidState = level.getFluidState(target);

        if (fluidState.isEmpty() || !fluidState.isSource()) {
            return Fluids.EMPTY;
        }

        if (pipe != null && level.getGameTime() < pipe.intakeReadyAt(face)) {
            return Fluids.EMPTY;
        }

        return FluidContent.sourceOf(fluidState.getType());
    }

    @Override
    public boolean present() {
        return true;
    }

    @Override
    public boolean canFill() {
        return false;
    }

    @Override
    public boolean canDrain() {
        return true;
    }

    @Override
    public FluidContent peek(int max) {
        var fluid = takeable();

        // Only ever whole buckets: a source block cannot be split.
        return fluid == Fluids.EMPTY || max < FluidContent.BUCKET ? FluidContent.EMPTY : FluidContent.of(fluid, FluidContent.BUCKET);
    }

    @Override
    public int fill(Fluid fluid, int amount, boolean simulate) {
        return 0;
    }

    @Override
    public int drain(Fluid fluid, int amount, boolean simulate) {
        var available = takeable();

        if (available == Fluids.EMPTY || !available.isSame(fluid) || amount < FluidContent.BUCKET) {
            return 0;
        }

        if (simulate) {
            return FluidContent.BUCKET;
        }

        var state = level.getBlockState(target);

        if (!(state.getBlock() instanceof BucketPickup pickup) || pickup.pickupBlock(null, level, target, state).isEmpty()) {
            return 0;
        }

        if (pipe != null) {
            pipe.setIntakeReadyAt(face, level.getGameTime() + INTAKE_INTERVAL_TICKS);

            // [stated] "it will auto apply that liquid to its filter" - only into an empty allow-list, never over
            // what the player set, and never into a block-list (where it would do the opposite).
            var filter = pipe.getFilter(face);

            if (filter.isEmpty() && !filter.isBlockList()) {
                pipe.addFilterEntry(
                    face,
                    new com.human.common.gameplay.block.pipe.PipeFilter.Entry(
                        com.human.common.gameplay.block.pipe.PipeFilter.Kind.EXACT,
                        BuiltInRegistries.FLUID.getKey(fluid).toString(),
                        false
                    )
                );
            }
        }

        return FluidContent.BUCKET;
    }

    @Override
    public Object storage() {
        return target;
    }
}
