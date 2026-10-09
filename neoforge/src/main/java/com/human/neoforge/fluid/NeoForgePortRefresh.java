package com.human.neoforge.fluid;

import com.human.common.gameplay.machine.MachinePorts;

/**
 * NeoForge caches which capabilities a block exposes on each side. [stated] Oct 2: machine ports can now be changed, so
 * when they are the cache for that block is dropped - otherwise another mod's pipe would keep using the old sides until
 * the chunk reloaded.
 */
public final class NeoForgePortRefresh {

    private NeoForgePortRefresh() {}

    public static void install() {
        MachinePorts.installCapabilityRefresh((level, pos) -> level.invalidateCapabilities(pos));
    }
}
