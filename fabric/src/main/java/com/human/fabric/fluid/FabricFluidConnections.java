package com.human.fabric.fluid;

import com.human.common.gameplay.fluid.FluidConnections;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;

/** Fabric's answer: does the block expose Transfer API fluid storage on that face. */
public final class FabricFluidConnections {

    private FabricFluidConnections() {}

    public static void install() {
        FluidConnections.setBridge((level, pos, side) -> FluidStorage.SIDED.find(level, pos, side) != null);
    }
}
