package com.human.common.gameplay.quarry;

import net.minecraft.nbt.CompoundTag;

/**
 * The SETTINGS tab ([stated] Oct 15), stored on the terminal. Each option is a small index into its list of choices;
 * the screen shows the names and descriptions, the menu changes them by button id.
 * <ul>
 * <li>REDSTONE - ignore / needs a signal / a signal stops it</li>
 * <li>WHEN FINISHED - retract the frames / leave them built</li>
 * <li>ATOMIZER SWEEP - on / off (clears only where the frame will stand)</li>
 * <li>DRAIN LIQUIDS - off / collect / void (off by default)</li>
 * <li>PRESERVE STRUCTURES - on / off</li>
 * <li>PRESERVE SPAWNERS - on / off</li>
 * <li>REFILL HOLE - on / off (off by default)</li>
 * </ul>
 */
public final class QuarrySettings {

    public enum Option {

        REDSTONE("redstone", 3, 0),
        WHEN_FINISHED("when_finished", 2, 0),
        ATOMIZER("atomizer", 2, 0),
        DRAIN_LIQUIDS("drain_liquids", 3, 0),
        PRESERVE_STRUCTURES("preserve_structures", 2, 0),
        PRESERVE_SPAWNERS("preserve_spawners", 2, 0),
        REFILL("refill", 2, 1);

        private final String key;

        private final int choices;

        private final int fallback;

        Option(String key, int choices, int fallback) {
            this.key = key;
            this.choices = choices;
            this.fallback = fallback;
        }

        public String key() {
            return key;
        }

        public int choices() {
            return choices;
        }
    }

    private final int[] values = new int[Option.values().length];

    public QuarrySettings() {
        for (var option : Option.values()) {
            values[option.ordinal()] = option.fallback;
        }
    }

    public int get(Option option) {
        return values[option.ordinal()];
    }

    /** @return whether anything changed */
    public boolean set(Option option, int choice) {
        if (choice < 0 || choice >= option.choices || values[option.ordinal()] == choice) {
            return false;
        }

        values[option.ordinal()] = choice;

        return true;
    }

    public CompoundTag save() {
        var tag = new CompoundTag();

        for (var option : Option.values()) {
            tag.putInt(option.key, get(option));
        }

        return tag;
    }

    public void load(CompoundTag tag) {
        for (var option : Option.values()) {
            values[option.ordinal()] = tag.contains(option.key)
                ? Math.clamp(tag.getInt(option.key), 0, option.choices - 1)
                : option.fallback;
        }
    }
}
