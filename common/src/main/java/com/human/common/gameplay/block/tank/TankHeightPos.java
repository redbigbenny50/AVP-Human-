package com.human.common.gameplay.block.tank;

import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

/** Where a tank block sits up the tank. */
public enum TankHeightPos implements StringRepresentable {

    SINGLE("single"),
    BOTTOM("bottom"),
    MID("mid"),
    TOP("top");

    private final String serializedName;

    TankHeightPos(String serializedName) {
        this.serializedName = serializedName;
    }

    @Override
    public @NotNull String getSerializedName() {
        return serializedName;
    }
}
