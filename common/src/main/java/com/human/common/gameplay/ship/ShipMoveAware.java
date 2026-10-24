package com.human.common.gameplay.ship;

/**
 * [stated] Oct 21 - a block entity whose saved data names other positions or is kept per face, and so has to be put
 * right when a ship is built or lands (see {@link ShipMove}). Called on the server once the WHOLE batch is in place.
 */
public interface ShipMoveAware {

    /**
     * Phase 1, for every moved block entity: re-point stored positions ({@link ShipMove#remap}) and turn per-face data
     * ({@link ShipMove#rotateFaces}). Touch only this block entity's own data here - other moved blocks may not have
     * had theirs put right yet.
     */
    void onShipMove(ShipMove move);

    /** Phase 2, after phase 1 has run for EVERY moved block: anything that reads other blocks (re-forming). */
    default void afterShipMove(ShipMove move) {}
}
