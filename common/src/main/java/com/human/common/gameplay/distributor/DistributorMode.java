package com.human.common.gameplay.distributor;

/**
 * When a distributor pours. [stated] Oct 17 (agreed): the three screen buttons.
 * <ul>
 * <li>ALWAYS - pours while it has vapor and power (suppression).</li>
 * <li>REDSTONE - pours only while powered by redstone (trap).</li>
 * <li>SENSOR - pours while a hostile mob or xenomorph is inside its room, and stops a few seconds after it clears
 * (defense).</li>
 * </ul>
 * A MUTHUR remote command ({@link DistributorRemote}) overrides whichever is set.
 */
public enum DistributorMode {

    ALWAYS("always"),
    REDSTONE("redstone"),
    SENSOR("sensor");

    private static final DistributorMode[] VALUES = values();

    private final String id;

    DistributorMode(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public String translationKey() {
        return "gui.avp_human.distributor.mode." + id;
    }

    public static DistributorMode byOrdinal(int ordinal) {
        return ordinal >= 0 && ordinal < VALUES.length ? VALUES[ordinal] : ALWAYS;
    }

    public static DistributorMode byId(String id) {
        for (var mode : VALUES) {
            if (mode.id.equals(id)) {
                return mode;
            }
        }

        return ALWAYS;
    }
}
