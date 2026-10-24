package com.human.common.gameplay.storage;

/**
 * Crate footprints in blocks: width across the front, height, and length running back from the front. Capacity is 36
 * slots for every block the crate fills.
 */
public enum CrateSize {

    ONE(1, 1, 1, "1x1"),
    TWO(2, 2, 2, "2x2"),
    THREE(3, 3, 3, "3x3"),
    CONTAINER(3, 3, 5, "container");

    public static final int SLOTS_PER_BLOCK = 36;

    private final int width;

    private final int height;

    private final int length;

    private final String suffix;

    CrateSize(int width, int height, int length, String suffix) {
        this.width = width;
        this.height = height;
        this.length = length;
        this.suffix = suffix;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public int length() {
        return length;
    }

    public String suffix() {
        return suffix;
    }

    public int blocks() {
        return width * height * length;
    }

    public int slots() {
        return blocks() * SLOTS_PER_BLOCK;
    }

    /** The largest side, used to shrink the held item down to one block. */
    public int largestSide() {
        return Math.max(width, Math.max(height, length));
    }
}
