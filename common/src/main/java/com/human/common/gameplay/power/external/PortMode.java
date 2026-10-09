package com.human.common.gameplay.power.external;

/**
 * How a cable face treats the other mod's block it touches. [stated] colours: RED = power OUT of our system, GREEN =
 * INTO our system, YELLOW = both.
 */
public enum PortMode {

    /** Follow the other block: a machine is powered, a generator feeds in, a two-way block is used as a battery. */
    AUTO("auto", 0xFFA0A0A0),
    /** Red: our grid powers it. */
    OUT("out", 0xFFD04040),
    /** Green: it feeds our grid. */
    IN("in", 0xFF40C040),
    /** Yellow: a battery - charged from our surplus, drawn on when we are short. */
    BOTH("both", 0xFFE0C040),
    /** Not connected. */
    OFF("off", 0xFF404040);

    private final String id;

    private final int colour;

    PortMode(String id, int colour) {
        this.id = id;
        this.colour = colour;
    }

    public String id() {
        return id;
    }

    public int colour() {
        return colour;
    }

    public PortMode next() {
        return values()[(ordinal() + 1) % values().length];
    }

    public static PortMode byOrdinal(int ordinal) {
        return ordinal >= 0 && ordinal < values().length ? values()[ordinal] : AUTO;
    }
}
