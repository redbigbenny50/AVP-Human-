package com.human.common.gameplay.power.upgrade;

/**
 * How a battery with the redstone upgrade reacts to a redstone signal. Without the upgrade it is always IGNORED.
 */
public enum RedstoneControl {

    /** Redstone makes no difference. */
    IGNORED(true, true, true, true),
    /** Backup power: gives power out only while powered. Always charges. */
    BACKUP(true, false, true, true),
    /** Holds its charge while powered: never gives power out then, still charges. */
    HOLD_WHEN_POWERED(true, true, true, false),
    /** Switched off while powered: neither charges nor gives power out. */
    OFF_WHEN_POWERED(true, true, false, false);

    private final boolean chargeUnpowered;

    private final boolean dischargeUnpowered;

    private final boolean chargePowered;

    private final boolean dischargePowered;

    RedstoneControl(boolean chargeUnpowered, boolean dischargeUnpowered, boolean chargePowered, boolean dischargePowered) {
        this.chargeUnpowered = chargeUnpowered;
        this.dischargeUnpowered = dischargeUnpowered;
        this.chargePowered = chargePowered;
        this.dischargePowered = dischargePowered;
    }

    public boolean canCharge(boolean powered) {
        return powered ? chargePowered : chargeUnpowered;
    }

    public boolean canDischarge(boolean powered) {
        return powered ? dischargePowered : dischargeUnpowered;
    }

    public RedstoneControl next() {
        return values()[(ordinal() + 1) % values().length];
    }

    public static RedstoneControl byOrdinal(int ordinal) {
        return ordinal >= 0 && ordinal < values().length ? values()[ordinal] : IGNORED;
    }
}
