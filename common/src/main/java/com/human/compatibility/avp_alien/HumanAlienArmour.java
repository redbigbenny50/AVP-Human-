package com.human.compatibility.avp_alien;

import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

/**
 * Asks avp_alien how much of a bullet a xenomorph's armour turns aside, given the caliber and where it landed.
 * <p>
 * Needed because neither mod knows both halves: the CALIBER exists only here, and the ARMOUR RULE exists only over
 * there. The crusher, for example, is meant to be immune to small rounds through the head and take a quarter less from
 * medium ones - a distinction the limb hitbox system cannot express, because it carries a single flat
 * {@code healthDamageMultiplier} per volume with no idea what shot it.
 * </p>
 * <p>
 * ⚠ THE GUARD IS THE INNER CLASS, NOT THE IF - same rule as {@link HumanAlienBlood}. The JVM resolves the types a
 * method references when that method is first verified, so a bare {@code isLoaded()} check in a method that also names
 * an avp_alien class crashes anyway. Every avp_alien reference lives in {@link Compatibility}, which is not loaded
 * until control actually reaches it.
 * </p>
 * <p>
 * ⚠ Returns 1.0 when avp_alien is absent, so a hit is completely unaffected rather than silently zeroed.
 * </p>
 */
public final class HumanAlienArmour {

    private HumanAlienArmour() {}

    /**
     * @param limbPath the path of the struck limb volume's id, or null for a plain body hit
     * @param caliber  the ammunition class - see {@code GunCalibers}
     * @return a multiplier for the damage; 1.0 when there is no rule
     */
    public static float damageMultiplier(LivingEntity target, @Nullable String limbPath, String caliber) {
        if (!AVPAlien.MOD.isLoaded()) {
            return 1.0F;
        }

        return Compatibility.damageMultiplier(target, limbPath, caliber);
    }

    /** Everything that names an avp_alien type lives here, so it loads only after the guard above passes. */
    private static final class Compatibility {

        private static float damageMultiplier(LivingEntity target, @Nullable String limbPath, String caliber) {
            return com.alien.common.gameplay.entity.dismemberment.BulletResistance.damageMultiplier(
                target,
                limbPath,
                caliber
            );
        }
    }
}
