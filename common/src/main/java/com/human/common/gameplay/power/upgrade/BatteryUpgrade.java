package com.human.common.gameplay.power.upgrade;

/**
 * What can go in a battery's upgrade slots. [stated] Sep 29 - colour-coded cards: red redstone, grey armoured, green
 * capacity, purple emergency, blue transfer, white alarm.
 * <p>
 * STACKABLE upgrades (capacity, transfer) add up across slots; MODULES (the rest) either are installed or are not, so a
 * second copy is refused rather than silently doing nothing.
 * </p>
 */
public enum BatteryUpgrade {

    CAPACITY("capacity", true),
    TRANSFER("transfer", true),
    EMERGENCY("emergency", false),
    REDSTONE("redstone", false),
    ALARM("alarm", false),
    ARMORED("armored", false);

    private final String id;

    private final boolean stackable;

    BatteryUpgrade(String id, boolean stackable) {
        this.id = id;
        this.stackable = stackable;
    }

    public String id() {
        return id;
    }

    public boolean stackable() {
        return stackable;
    }
}
