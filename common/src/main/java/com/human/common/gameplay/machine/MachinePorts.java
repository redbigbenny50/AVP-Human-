package com.human.common.gameplay.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;

import java.util.function.BiConsumer;

/**
 * Machine port configuration. [stated] Oct 2: each side of a machine is set for items, and for fluids on a machine with
 * a tank, to IN, OUT, BOTH or OFF. Sides are RELATIVE to the machine's front, so turning or re-placing a machine keeps
 * its layout; the defaults are exactly the fixed layouts the machines had before.
 */
public final class MachinePorts {

    /** The six sides as a player looking at the front sees them. */
    public enum Face {

        FRONT,
        BACK,
        LEFT,
        RIGHT,
        TOP,
        BOTTOM;

        /** Which face {@code side} is, for a machine facing {@code front}. */
        public static Face of(Direction side, Direction front) {
            if (side == Direction.UP) {
                return TOP;
            }

            if (side == Direction.DOWN) {
                return BOTTOM;
            }

            if (side == front) {
                return FRONT;
            }

            if (side == front.getOpposite()) {
                return BACK;
            }

            // As a player looking at the front sees it: their left is the front's clockwise turn.
            return side == front.getClockWise() ? LEFT : RIGHT;
        }
    }

    public enum Mode {

        IN,
        OUT,
        BOTH,
        OFF;

        public boolean allowsIn() {
            return this == IN || this == BOTH;
        }

        public boolean allowsOut() {
            return this == OUT || this == BOTH;
        }

        public static Mode byOrdinal(int ordinal) {
            return ordinal >= 0 && ordinal < values().length ? values()[ordinal] : OFF;
        }
    }

    public static final int FACES = Face.values().length;

    private MachinePorts() {}

    /** Six modes packed two bits each - twelve bits, so they travel in one menu data value. */
    public static int pack(Mode[] modes) {
        int packed = 0;

        for (int face = 0; face < FACES; face++) {
            packed |= modes[face].ordinal() << face * 2;
        }

        return packed;
    }

    public static Mode unpack(int packed, Face face) {
        return Mode.byOrdinal(packed >> face.ordinal() * 2 & 3);
    }

    // ------------------------------------------------------------------------------------------------------------
    // Telling the world a machine's sides changed
    // ------------------------------------------------------------------------------------------------------------

    /**
     * Set by a loader that caches what a block exposes per side (NeoForge's capabilities), so that cache is dropped
     * when ports change. Fabric needs nothing - its lookups ask the block each time.
     */
    private static BiConsumer<ServerLevel, BlockPos> capabilityRefresh = (level, pos) -> {};

    public static void installCapabilityRefresh(BiConsumer<ServerLevel, BlockPos> refresh) {
        capabilityRefresh = refresh;
    }

    /**
     * A machine's ports changed: drop any cached capabilities, and tell the neighbours - our pipes re-check their sides
     * (and flanges) on a neighbour change, the same way they already do when a tank switches sides.
     */
    public static void refresh(ServerLevel level, BlockPos pos) {
        capabilityRefresh.accept(level, pos);
        level.updateNeighborsAt(pos, level.getBlockState(pos).getBlock());
    }
}
