package com.human.common.gameplay.power.external;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;

/**
 * Other mods' energy blocks, seen from our cables. [stated] Sep 29/30: our grid is FE-compatible - "RF/FE compatible
 * ... 1:1" - so a cable touching another mod's machine, generator or battery can power it, be powered by it, or both.
 * <p>
 * Each loader installs its half: NeoForge's energy capability, or Team Reborn Energy on Fabric. Until one is installed
 * nothing external is found, and the grid behaves exactly as before.
 * </p>
 */
public interface ExternalEnergy {

    /** One face of another mod's block, as a cable touching it sees it. Looked up afresh each time it is asked. */
    interface Port {

        /** Whether the block still offers energy on this face at all. */
        boolean present();

        boolean canReceive();

        boolean canExtract();

        /** @return how much it accepted (or would accept, when simulating) */
        long receive(long amount, boolean simulate);

        /** @return how much came out (or would, when simulating) */
        long extract(long amount, boolean simulate);

        long stored();

        long capacity();
    }

    /**
     * A port onto the block at {@code pos}, through its {@code side} - the face that touches the cable. May cache the
     * lookup; returns a port even when nothing is there yet, so {@link Port#present()} decides.
     */
    Port open(ServerLevel level, BlockPos pos, Direction side);

    /**
     * Whether a block could hold energy at all - energy storage lives in block entities. Checked BEFORE opening a port:
     * a cable network is mostly air and stone around its faces, and each port opened on NeoForge registers a capability
     * cache with the level, so skipping those faces keeps grid rebuilds cheap.
     */
    static boolean mayHoldEnergy(net.minecraft.world.level.Level level, BlockPos pos) {
        var state = level.getBlockState(pos);

        return !state.isAir() && state.hasBlockEntity();
    }

    /** Nothing installed: nothing is ever found. */
    ExternalEnergy NONE = (level, pos, side) -> new Port() {

        @Override
        public boolean present() {
            return false;
        }

        @Override
        public boolean canReceive() {
            return false;
        }

        @Override
        public boolean canExtract() {
            return false;
        }

        @Override
        public long receive(long amount, boolean simulate) {
            return 0L;
        }

        @Override
        public long extract(long amount, boolean simulate) {
            return 0L;
        }

        @Override
        public long stored() {
            return 0L;
        }

        @Override
        public long capacity() {
            return 0L;
        }
    };

    final class Holder {

        private static ExternalEnergy current = NONE;

        private Holder() {}

        public static ExternalEnergy get() {
            return current;
        }

        /** Called once by each loader at start-up. */
        public static void install(@Nullable ExternalEnergy energy) {
            current = energy == null ? NONE : energy;
        }
    }
}
