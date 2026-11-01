package com.human.common.gameplay.cosmetic;

import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.DyeColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The colour of a piece's fabric (cushions, bedding, curtains, the coat). [stated] Oct 31: right-click with any dye to
 * recolour only the fabric; the dye is not used up and there is no reset - dye it again to change it. {@link #DEFAULT}
 * is the faction's own fabric colour.
 */
public enum CosmeticFabric implements StringRepresentable {

    DEFAULT(null),
    WHITE(DyeColor.WHITE),
    ORANGE(DyeColor.ORANGE),
    MAGENTA(DyeColor.MAGENTA),
    LIGHT_BLUE(DyeColor.LIGHT_BLUE),
    YELLOW(DyeColor.YELLOW),
    LIME(DyeColor.LIME),
    PINK(DyeColor.PINK),
    GRAY(DyeColor.GRAY),
    LIGHT_GRAY(DyeColor.LIGHT_GRAY),
    CYAN(DyeColor.CYAN),
    PURPLE(DyeColor.PURPLE),
    BLUE(DyeColor.BLUE),
    BROWN(DyeColor.BROWN),
    GREEN(DyeColor.GREEN),
    RED(DyeColor.RED),
    BLACK(DyeColor.BLACK);

    private final @Nullable DyeColor dye;

    CosmeticFabric(@Nullable DyeColor dye) {
        this.dye = dye;
    }

    @Override
    public @NotNull String getSerializedName() {
        return dye == null ? "default" : dye.getSerializedName();
    }

    public static CosmeticFabric of(DyeColor dye) {
        for (var fabric : values()) {
            if (fabric.dye == dye) {
                return fabric;
            }
        }

        return DEFAULT;
    }

    /**
     * The tint for this fabric on the block named {@code blockName}: the dye's own colour, or that edition's undyed
     * fabric colour. The fabric texture is greyscale, so the tint is the colour the player sees.
     */
    public int tint(String blockName) {
        if (dye != null) {
            return dye.getTextureDiffuseColor() | 0xFF000000;
        }

        return CosmeticModelTables.FABRIC_DEFAULT.getOrDefault(blockName, 0xFFFFFF) | 0xFF000000;
    }
}
