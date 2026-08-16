package com.human.compatibility.avp_alien;

import net.minecraft.world.entity.Entity;

import java.util.OptionalInt;

/**
 * Resolves the kill-effect fluid type for xenomorph strains, without avp_alien being required at runtime.
 * <p>
 * The gun hit pipeline used to reference {@code com.alien...Alien} directly from
 * {@code HitScanGunAttackAction#killFluidType}. That is a hard link to another mod's class sitting in the firing path
 * of every conventional firearm - anyone running avp_human on its own would take a {@link NoClassDefFoundError} the
 * first time they shot something.
 * <p>
 * THE GUARD IS THE INNER CLASS, NOT THE IF. A method that merely checks {@code isLoaded()} before touching
 * {@code Alien} does not help: the JVM resolves the types a method references when that method is first verified, so
 * the crash arrives anyway. Keeping every avp_alien reference inside {@link Compatibility} means that class is not
 * loaded until control actually reaches it, which only happens once the mod check has passed. This mirrors the
 * {@code MixinItem_ChorusEmbryo$Compatibility} pattern already used in this codebase.
 * <p>
 * Returns EMPTY rather than a default so the caller keeps its own registry-key fallback, which still classifies aliens
 * from other sources when avp_alien is absent.
 */
public final class HumanAlienBlood {

    /** Irradiated strain blood. */
    public static final int FLUID_IRRADIATED = 4;

    /** Nether-afflicted strain blood. */
    public static final int FLUID_NETHER = 3;

    /** Ordinary xenomorph acid. */
    public static final int FLUID_ACID = 1;

    public static OptionalInt fluidType(Entity entity) {
        if (!AVPAlien.MOD.isLoaded()) {
            return OptionalInt.empty();
        }

        return Compatibility.fluidType(entity);
    }

    /** Everything that names an avp_alien type lives here, so it loads only after the guard above passes. */
    private static final class Compatibility {

        private static OptionalInt fluidType(Entity entity) {
            if (!(entity instanceof com.alien.common.gameplay.entity.living.alien.Alien alien)) {
                return OptionalInt.empty();
            }

            if (alien.isIrradiated()) {
                return OptionalInt.of(FLUID_IRRADIATED);
            }

            if (alien.isNetherAfflicted()) {
                return OptionalInt.of(FLUID_NETHER);
            }

            return OptionalInt.of(FLUID_ACID);
        }

        private Compatibility() {
            throw new UnsupportedOperationException();
        }
    }

    private HumanAlienBlood() {
        throw new UnsupportedOperationException();
    }
}
