package com.human.common.gameplay.power;

import org.jetbrains.annotations.Nullable;

/**
 * What a battery is for. [stated] Sep 25: a cryotank chamber fed from a main battery room, with a second room that
 * "only then does the secondary kick in" - so the modes are exactly those three roles.
 * <h2>⚠⚠ SAVED AS A STRING, NEVER AS AN ORDINAL</h2> [stated] Sep 25, on pasted and copied NBT: "doesnt mess up the
 * system or worse cause a crash. so mods dont take this into account and end up crashing needing later updates to fix."
 * <p>
 * An ordinal is a position in a list. Reorder the enum and every saved world silently means something else; hand-edit
 * or paste a tag with an out-of-range ordinal and {@code values()[i]} throws {@code ArrayIndexOutOfBounds} inside
 * {@code loadAdditional}, which takes the CHUNK down, not just the block. {@link #byName} matches a string and falls
 * back to {@link #PRIMARY} for anything it does not recognise, so a corrupt or future tag costs a default rather than a
 * crash.
 * </p>
 */
public enum StoreMode {

    /** Discharges whenever the grid is short. */
    PRIMARY("primary", PowerNode.STORE_PRIMARY),

    /** Holds its charge until every primary is empty or cut off. */
    RESERVE("reserve", PowerNode.STORE_RESERVE),

    /** A pure sink: charges, never gives power back. */
    STORAGE_ONLY("storage_only", PowerNode.STORE_NEVER_DISCHARGES);

    private final String id;

    private final int priority;

    StoreMode(String id, int priority) {
        this.id = id;
        this.priority = priority;
    }

    public String id() {
        return id;
    }

    public int priority() {
        return priority;
    }

    public StoreMode next() {
        return values()[(ordinal() + 1) % values().length];
    }

    /** ⚠ Never throws. An unknown, empty or absent name is PRIMARY. */
    public static StoreMode byName(@Nullable String name) {
        if (name != null) {
            for (var mode : values()) {
                if (mode.id.equals(name)) {
                    return mode;
                }
            }
        }

        return PRIMARY;
    }
}
