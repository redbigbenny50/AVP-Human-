package com.human.common.gameplay.block.tank;

import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

/**
 * Where a tank block sits across the tank on one horizontal axis. Kept per axis: a corner block is a left edge on one
 * face and a right edge on the other.
 */
public enum TankAxisPos implements StringRepresentable {

    SINGLE("single"),
    MIN("min"),
    MID("mid"),
    MAX("max");

    private final String serializedName;

    TankAxisPos(String serializedName) {
        this.serializedName = serializedName;
    }

    @Override
    public @NotNull String getSerializedName() {
        return serializedName;
    }
}
