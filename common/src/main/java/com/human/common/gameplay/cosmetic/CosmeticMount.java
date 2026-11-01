package com.human.common.gameplay.cosmetic;

import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

/**
 * Where a wall piece is mounted - [stated] Oct 31: on any of the six faces. Wall pieces are modelled flush against the
 * wall behind them; on a FLOOR they lie face-up, on a CEILING face-down, and their FACING sets which way they are
 * turned. The model turns about X first (floor +90, ceiling -90 - vanilla blockstate x 270 / 90), then by its facing.
 */
public enum CosmeticMount implements StringRepresentable {

    WALL("wall"),
    FLOOR("floor"),
    CEILING("ceiling");

    private final String id;

    CosmeticMount(String id) {
        this.id = id;
    }

    @Override
    public @NotNull String getSerializedName() {
        return id;
    }
}
