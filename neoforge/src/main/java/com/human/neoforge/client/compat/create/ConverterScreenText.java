package com.human.neoforge.client.compat.create;

import net.minecraft.client.gui.Font;

/**
 * [stated] Oct 16 / Oct 18 - GUI text stays ONE size and never runs past its frame or into other text. Numbers cannot
 * be reworded, so a value too wide for its column (only possible once the config raises the rates) is shown in
 * thousands (12K FE/T) - same font, same size - and only as a last resort as MAX.
 */
final class ConverterScreenText {

    static final int HI = 0x6EF68D;

    static final int DIM = 0x33974F;

    static final int RED = 0xF66E6E;

    static final int AMBER = 0xF0D25A;

    private ConverterScreenText() {}

    static String fit(Font font, long value, String unit, int maxWidth) {
        var full = value + " " + unit;

        if (font.width(full) <= maxWidth) {
            return full;
        }

        var thousands = (value / 1000L) + "K " + unit;
        return font.width(thousands) <= maxWidth ? thousands : "MAX";
    }
}
