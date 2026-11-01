package com.human.common.gameplay.power;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Cables into a machine that is bigger than one block.
 * <p>
 * [stated] Oct 31: the Gene Storage and Gene Separator took no cable on ANY face. Only the core block holds the power
 * block entity, and their core's back face sits INSIDE the 2x2 - against the machine's own back block - so the one face
 * that accepted a cable could never be reached. [stated] Oct 31: genetic machines take power from the BOTTOM ONLY (a
 * back port would sit over the models' counters), through ANY of their bottom blocks - which only the core's own bottom
 * could have done before.
 * </p>
 * <p>
 * The fix keeps the grid exactly as it was - only real power block entities are nodes - and adds one idea: a face of a
 * NON-core block can lead straight to the core. A cable touching such a face links to the core as if it touched the
 * core. The part blocks themselves never join a grid, hold nothing and tick nothing.
 * </p>
 * <p>
 * Every place that walks or draws cable connections asks this class: {@code PowerGridExploreUtil.discover} (both
 * directions: cable to core, and core out to its cables), {@code PowerGridManager} when a cable is cut, a frontier
 * loads, or a machine finishes placing its parts, and {@code CableBlock} for the arm, the flange colour and placement.
 * </p>
 */
public final class PowerParts {

    /** A block, some of whose non-core parts take a cable for the core. */
    public interface PartBlock {

        /**
         * The core this block's {@code face} feeds, when {@code pos} is a NON-core part of this machine and that face
         * takes a cable. Null for the core itself (its own faces are its block entity's business), and for every face
         * that takes no cable.
         */
        @Nullable
        BlockPos coreThrough(BlockGetter level, BlockPos pos, BlockState state, Direction face);

        /**
         * Every (part position, face) of the machine whose core is at {@code core} that takes a cable. Called from the
         * core's side of a walk, so the walk can step out of the machine to those cables.
         */
        void cableFaces(BlockPos core, BlockState coreState, BiConsumer<BlockPos, Direction> sink);
    }

    private PowerParts() {}

    /**
     * The core a cable at {@code pos.relative(face)} would reach through the block at {@code pos}, or null. Checked
     * against the world: the core must really be there, loaded, holding a power block entity.
     */
    public static @Nullable BlockPos coreThrough(Level level, BlockPos pos, Direction face) {
        if (!level.isLoaded(pos)) {
            return null;
        }

        var state = level.getBlockState(pos);

        if (!(state.getBlock() instanceof PartBlock part)) {
            return null;
        }

        var core = part.coreThrough(level, pos, state, face);

        if (core == null || !level.isLoaded(core) || !(level.getBlockEntity(core) instanceof PowerNode)) {
            return null;
        }

        return core;
    }

    /** Same question for the client, which only needs to know whether to draw the cable's arm. */
    public static boolean takesCable(BlockGetter level, BlockPos pos, Direction face) {
        var state = level.getBlockState(pos);
        return state.getBlock() instanceof PartBlock part && part.coreThrough(level, pos, state, face) != null;
    }

    /**
     * The cable positions just outside the machine whose core is at {@code core}, each reached through one of its
     * parts. Positions in unloaded chunks are passed to {@code unloaded} instead, so a walk can retry them when they
     * load.
     */
    public static void cableLinks(Level level, BlockPos core, Consumer<BlockPos> loaded, Consumer<BlockPos> unloaded) {
        var coreState = level.getBlockState(core);

        if (!(coreState.getBlock() instanceof PartBlock machine)) {
            return;
        }

        machine.cableFaces(core, coreState, (partPos, face) -> {
            if (!level.isLoaded(partPos)) {
                unloaded.accept(partPos.immutable());
                return;
            }

            // The part must really be this machine's, and still take a cable on that face.
            if (!core.equals(coreThrough(level, partPos, face))) {
                return;
            }

            var outside = partPos.relative(face);

            if (level.isLoaded(outside)) {
                loaded.accept(outside);
            } else {
                unloaded.accept(outside.immutable());
            }
        });
    }
}
