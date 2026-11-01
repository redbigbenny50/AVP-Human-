package com.human.common.gameplay.cosmetic;

/**
 * The crafter's SETS column - the concept sheets' themed sets. ALL is the screen's own filter, not a set. The column
 * scrolls ([stated] Oct 31), so the lab expansion's sets simply follow the furniture ones.
 */
public enum CosmeticSet {

    LABORATORY("laboratory"),
    HABITAT("habitat"),
    UTILITY("utility"),
    KITCHEN("kitchen"),
    SECURITY("security"),
    RECREATION("recreation"),
    PROPS("props"),
    BOTANICAL("botanical"),
    GENETICS("genetics"),
    VIROLOGY("virology"),
    GEOLOGY("geology"),
    SPECIMENS("specimens"),
    SUPPLIES("supplies"),
    ARMORY("armory"),
    OPERATIONS("operations"),
    ENGINEERING("engineering");

    private final String id;

    CosmeticSet(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public String translationKey() {
        return "gui.avp_human.cosmetic_crafter.set." + id;
    }
}
