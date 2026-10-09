package com.human.common.gameplay.block.cryo;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A block a cryoconduit may attach to, other than another conduit: the cryotube controller (its left, right and bottom
 * ports) and the cryotanks (top and bottom). [stated] Oct 1: the flow is one way, controller to tanks, so a port has no
 * in/out setting and the flange has no colour.
 */
public interface CryoConduitEndpoint {

    /**
     * @param state this block's state
     * @param face  the face of THIS block the conduit is touching
     * @return whether a conduit seats a flange on that face
     */
    boolean acceptsCryoConduit(BlockState state, Direction face);
}
