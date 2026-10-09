package com.human.common.gameplay.block.power;

import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/**
 * A generator's current band, shown by its light. [stated] Sep 24: "red is underpowered, yellow is mid power, and green
 * is max power" - plus OFF for a generator that is making nothing at all (a solar panel at night). Kept as a block
 * state so the client renderer reads it for free and it changes at most once per sensing interval, never per tick.
 */
public enum PowerMode implements StringRepresentable {

    OFF("off"),
    LOW("low"),
    MED("med"),
    MAX("max");

    public static final EnumProperty<PowerMode> PROPERTY = EnumProperty.create("power_mode", PowerMode.class);

    private final String serializedName;

    PowerMode(String serializedName) {
        this.serializedName = serializedName;
    }

    @Override
    public String getSerializedName() {
        return serializedName;
    }
}
