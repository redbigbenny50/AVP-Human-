package com.human.common.gameplay.cosmetic;

import com.human.common.gameplay.fabrication.Faction;
import org.jetbrains.annotations.Nullable;

/**
 * The editions every cosmetic design comes in. [stated] Oct 31: faction licenses unlock the faction additions;
 * Industrial is free. UPP is modelled in every .bbmodel but not registered until the faction exists.
 * <p>
 * The crafter's STYLE drawer lists these in this order, one row each, so a new faction is one more constant here.
 * </p>
 */
public enum CosmeticStyle {

    INDUSTRIAL("industrial", "", null, 0xFF747A7D),
    WY("wy", "wy_", Faction.WY, 0xFFEAEAEA),
    USCM("uscm", "uscm_", Faction.USCM, 0xFF707362);

    private final String id;

    private final String prefix;

    private final @Nullable Faction faction;

    private final int swatch;

    CosmeticStyle(String id, String prefix, @Nullable Faction faction, int swatch) {
        this.id = id;
        this.prefix = prefix;
        this.faction = faction;
        this.swatch = swatch;
    }

    public String id() {
        return id;
    }

    /** The registry-name prefix of this edition's blocks: "" / "wy_" / "uscm_". */
    public String prefix() {
        return prefix;
    }

    /** The faction whose license unlocks this style, or null when it is free. */
    public @Nullable Faction faction() {
        return faction;
    }

    /** The palette colour shown beside the style's name in the drawer (ARGB). */
    public int swatch() {
        return swatch;
    }

    public String translationKey() {
        return "gui.avp_human.cosmetic_crafter.style." + id;
    }

    /** The style a faction license unlocks, or null. */
    public static @Nullable CosmeticStyle of(Faction faction) {
        for (var style : values()) {
            if (style.faction == faction) {
                return style;
            }
        }

        return null;
    }
}
