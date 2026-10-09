package com.human.common.gameplay.block.tank;

import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

/**
 * Which part of the centred window a block carries on the faces along one axis. FULL on odd widths (the middle block);
 * on even widths the window straddles the middle seam: LOW is the half on the lower-coordinate block, HIGH the half on
 * the higher. NONE everywhere else, and everywhere when windows are off.
 */
public enum TankWindow implements StringRepresentable {

    NONE("none"),
    FULL("full"),
    LOW("low"),
    HIGH("high");

    private final String serializedName;

    TankWindow(String serializedName) {
        this.serializedName = serializedName;
    }

    @Override
    public @NotNull String getSerializedName() {
        return serializedName;
    }
}
