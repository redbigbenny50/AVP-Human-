package com.human.common.gameplay.block.tank;

import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

/**
 * W-Y only: which half of a 1 x 2 hull panel a block is, counted from the tank corner so the pattern never shifts as
 * blocks are added. Neighbouring columns are staggered by one.
 */
public enum TankPanel implements StringRepresentable {

    LOWER("lower"),
    UPPER("upper");

    private final String serializedName;

    TankPanel(String serializedName) {
        this.serializedName = serializedName;
    }

    @Override
    public @NotNull String getSerializedName() {
        return serializedName;
    }
}
