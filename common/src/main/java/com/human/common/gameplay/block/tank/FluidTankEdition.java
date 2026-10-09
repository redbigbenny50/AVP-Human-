package com.human.common.gameplay.block.tank;

/**
 * The six fluid tank editions. [stated] Sep 28/29 design review: each edition is its own block sharing one code path,
 * all with identical stats - the choice is purely looks - and a tank only merges with its OWN edition.
 */
public enum FluidTankEdition {

    STEEL("steel"),
    FERROALUMINUM("ferroaluminum"),
    ALUMINUM("aluminum"),
    TITANIUM("titanium"),
    WY("wy"),
    USCM("uscm");

    private final String id;

    FluidTankEdition(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    /** The block id: {@code steel_fluid_tank}, {@code wy_fluid_tank} ... */
    public String blockId() {
        return id + "_fluid_tank";
    }

    /** W-Y hulls are staggered 1 x 2 panels, so its blocks also carry which half of a panel they are. */
    public boolean hasPanels() {
        return this == WY;
    }
}
