package com.human.neoforge.fluid;

import com.human.common.gameplay.fluid.FluidConnections;
import net.neoforged.neoforge.capabilities.Capabilities;

/** NeoForge's answer: does the block expose the fluid handler capability on that face. */
public final class NeoForgeFluidConnections {

    private NeoForgeFluidConnections() {}

    public static void install() {
        FluidConnections.setBridge(
            (level, pos, side) -> level.getCapability(Capabilities.FluidHandler.BLOCK, pos, side) != null
        );
    }
}
