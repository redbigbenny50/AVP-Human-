package com.human.common.gameplay.machine;

import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

/**
 * What a machine's front shows. [stated] Sep 27: OFF is the default with the LED dark, ON means it has power (green
 * LED), WORKING means it is processing and animates.
 */
public enum MachineState implements StringRepresentable {

    OFF("off"),

    ON("on"),

    WORKING("working");

    private final String name;

    MachineState(String name) {
        this.name = name;
    }

    @Override
    public @NotNull String getSerializedName() {
        return name;
    }
}
