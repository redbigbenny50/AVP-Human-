package com.human.common.gameplay.power;

import net.minecraft.core.Direction;

/**
 * Which faces of a power block can carry power right now.
 * <p>
 * Oct 24 - pulled out of {@code PowerNodeBlockEntity} so that a power block which cannot extend it can still say which
 * of its faces take a cable. The Create converters (alternator and electric gearbox) are Create kinetic block entities
 * - they MUST extend Create's own classes to turn with a Create network - so they implement this instead. Before this
 * existed, the grid walk, block placement and the cable's own arms only asked {@code PowerNodeBlockEntity}, and any
 * other {@link PowerNode} was treated as connectable on EVERY face, which would have joined a cable to a converter's
 * gear sides.
 * </p>
 * <p>
 * Every place that decides whether power crosses a face asks this interface: {@code PowerGridExploreUtil.portsOpen} and
 * {@code CableBlock.canConnectTo}.
 * </p>
 */
public interface PowerFaces {

    /** True when power can cross this face now - the face carries power at all and nothing has shut it. */
    boolean isFaceConnectable(Direction direction);
}
