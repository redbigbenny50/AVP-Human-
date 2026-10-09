package com.human.common.gameplay.machine;

import com.human.common.gameplay.power.PowerNode;

/**
 * Which machines are fed first when a grid cannot cover everyone - the grid's existing priority bands, set per machine
 * from the left panel of its screen.
 */
public enum MachinePriority {

    CRITICAL("critical", PowerNode.PRIORITY_CRITICAL),

    DEFAULT("default", PowerNode.PRIORITY_DEFAULT),

    DEFERRABLE("deferrable", PowerNode.PRIORITY_DEFERRABLE);

    private final String id;

    private final int band;

    MachinePriority(String id, int band) {
        this.id = id;
        this.band = band;
    }

    public String id() {
        return id;
    }

    public int band() {
        return band;
    }

    public MachinePriority next() {
        return values()[(ordinal() + 1) % values().length];
    }

    /** ⚠ Cannot throw: an unknown id from an edited tag falls back to the default band. */
    public static MachinePriority byId(String id) {
        for (var priority : values()) {
            if (priority.id.equals(id)) {
                return priority;
            }
        }

        return DEFAULT;
    }

    /** ⚠ Clamped: a stale or forged menu value can never index out of range. */
    public static MachinePriority byOrdinal(int ordinal) {
        var values = values();

        return values[Math.max(0, Math.min(values.length - 1, ordinal))];
    }
}
