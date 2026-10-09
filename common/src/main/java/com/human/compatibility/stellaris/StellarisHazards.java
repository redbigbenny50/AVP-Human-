package com.human.compatibility.stellaris;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/**
 * Stellaris' one environmental hazard the marines dress for - no air - asked of its public
 * {@code PlanetUtil.hasOxygenAt(ServerLevel, BlockPos)} (read from the 1.4.25 jar; it is what
 * {@code DimensionOxygenManager.breath} consults, room bubbles included). Same inner-class guard as
 * {@code AdAstraHazards}; Stellaris is compile-only.
 */
public final class StellarisHazards {

    public static boolean isAirless(ServerLevel level, BlockPos pos) {
        return Stellaris.MOD.isLoaded() && Compatibility.isAirless(level, pos);
    }

    private static final class Compatibility {

        private static boolean isAirless(ServerLevel level, BlockPos pos) {
            return !com.st0x0ef.stellaris.common.utils.PlanetUtil.hasOxygenAt(level, pos);
        }

        private Compatibility() {
            throw new UnsupportedOperationException();
        }
    }

    private StellarisHazards() {
        throw new UnsupportedOperationException();
    }
}
