package com.human.common.gameplay.vapor;

/**
 * The six puff shapes the vapor particle draws ([stated] Oct 18 - "maybe have different mixes for each type"). Each is
 * six frames in {@code textures/particle/vapor_<id>_<frame>.png}, listed in this order in {@code particles/vapor.json},
 * so a style's frame is sprite {@code ordinal * FRAMES + frame}.
 */
public enum VaporStyle {

    /** Soft round clumps. */
    CLUMP,
    /** Campfire-style billows: a ring of round bumps rolling outward. */
    BILLOW,
    /** Streaked along the flow - fast gas. */
    WISP,
    /** Bubbly droplets that break apart. */
    GLOBULE,
    /** A small swirl that unwinds. */
    CURL,
    /** A heavy body with bumpy edges. */
    COTTON;

    public static final int FRAMES = 6;

    public static final int SPRITES = values().length * FRAMES;

    public static VaporStyle byOrdinal(int ordinal) {
        var values = values();
        return values[Math.floorMod(ordinal, values.length)];
    }

    /** The sprite index for this style at {@code frame} (0-5). */
    public int sprite(int frame) {
        return ordinal() * FRAMES + Math.max(0, Math.min(FRAMES - 1, frame));
    }
}
