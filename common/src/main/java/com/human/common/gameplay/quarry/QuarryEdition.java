package com.human.common.gameplay.quarry;

/**
 * The three quarry editions ([stated] Oct 13): BARE (worn steel field machine, the crafted one), W-Y (corporate white,
 * grey and yellow) and USCM (olive composite and steel). An edition only changes the skin - the terminal, drill head,
 * shaft and gantry of one quarry always share it.
 */
public enum QuarryEdition {

    BARE("bare"),
    WY("wy"),
    USCM("uscm");

    private final String textureKey;

    QuarryEdition(String textureKey) {
        this.textureKey = textureKey;
    }

    /** The part of every quarry texture name that names the edition, e.g. {@code quarry_terminal_wy_off}. */
    public String textureKey() {
        return textureKey;
    }
}
