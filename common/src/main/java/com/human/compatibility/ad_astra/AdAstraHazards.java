package com.human.compatibility.ad_astra;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/**
 * Ad Astra's three environmental hazards, asked of its public API, without Ad Astra being required at runtime. Read
 * from the 1.16.26 jar: {@code OxygenApi.API.hasOxygen(Level, BlockPos)}, {@code TemperatureApi.API.isHot} and
 * {@code isCold(Level, BlockPos)} - the same calls its own {@code entityTick} makes before dealing damage.
 * <p>
 * THE GUARD IS THE INNER CLASS, NOT THE IF - same shape as {@code HumanAlienBlood}. Every Ad Astra type lives in
 * {@link Compatibility}, which is not loaded until the mod check has passed; Ad Astra is a compile-only dependency for
 * exactly these three lines.
 */
public final class AdAstraHazards {

    /** True when there is no breathable air at this position (space, an unpressurised planet, outside a bubble). */
    public static boolean isAirless(Level level, BlockPos pos) {
        return AdAstra.MOD.isLoaded() && Compatibility.isAirless(level, pos);
    }

    public static boolean isExtremeHeat(Level level, BlockPos pos) {
        return AdAstra.MOD.isLoaded() && Compatibility.isExtremeHeat(level, pos);
    }

    public static boolean isExtremeCold(Level level, BlockPos pos) {
        return AdAstra.MOD.isLoaded() && Compatibility.isExtremeCold(level, pos);
    }

    private static final class Compatibility {

        private static boolean isAirless(Level level, BlockPos pos) {
            return !earth.terrarium.adastra.api.systems.OxygenApi.API.hasOxygen(level, pos);
        }

        private static boolean isExtremeHeat(Level level, BlockPos pos) {
            return earth.terrarium.adastra.api.systems.TemperatureApi.API.isHot(level, pos);
        }

        private static boolean isExtremeCold(Level level, BlockPos pos) {
            return earth.terrarium.adastra.api.systems.TemperatureApi.API.isCold(level, pos);
        }

        private Compatibility() {
            throw new UnsupportedOperationException();
        }
    }

    private AdAstraHazards() {
        throw new UnsupportedOperationException();
    }
}
