package com.human.common.gameplay.distributor;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * [stated] Oct 17 (agreed): the hook for MUTHUR's future security menu - every distributor accepts a remote POUR or
 * STOP command. Nothing in the mod sends one yet; MUTHUR will call {@link #pour}, {@link #stop} and {@link #release}.
 * <ul>
 * <li>NONE - the distributor follows its own mode (the default).</li>
 * <li>POUR - pours whatever its mode says, as long as it has vapor and power.</li>
 * <li>STOP - never pours, whatever its mode says.</li>
 * </ul>
 * The command is saved with the machine, so a relog or an unload does not quietly drop a lockdown. Release hands
 * control back to the mode.
 */
public enum DistributorRemote {

    NONE("none"),
    POUR("pour"),
    STOP("stop");

    private static final DistributorRemote[] VALUES = values();

    private final String id;

    DistributorRemote(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public static DistributorRemote byOrdinal(int ordinal) {
        return ordinal >= 0 && ordinal < VALUES.length ? VALUES[ordinal] : NONE;
    }

    public static DistributorRemote byId(String id) {
        for (var command : VALUES) {
            if (command.id.equals(id)) {
                return command;
            }
        }

        return NONE;
    }

    // ------------------------------------------------------------------------------------------------------------
    // The entry points MUTHUR will call. Server side; each returns false when there is no distributor there or the
    // chunk is not loaded (never loads it).
    // ------------------------------------------------------------------------------------------------------------

    public static boolean pour(Level level, BlockPos pos) {
        return send(level, pos, POUR);
    }

    public static boolean stop(Level level, BlockPos pos) {
        return send(level, pos, STOP);
    }

    public static boolean release(Level level, BlockPos pos) {
        return send(level, pos, NONE);
    }

    private static boolean send(Level level, BlockPos pos, DistributorRemote command) {
        var distributor = at(level, pos);

        if (distributor == null) {
            return false;
        }

        distributor.setRemote(command);
        return true;
    }

    /** The distributor at a position, or null - without loading the chunk. */
    public static @Nullable DistributorBlockEntity at(Level level, BlockPos pos) {
        if (level.isClientSide || !level.isLoaded(pos)) {
            return null;
        }

        return level.getBlockEntity(pos) instanceof DistributorBlockEntity distributor ? distributor : null;
    }
}
