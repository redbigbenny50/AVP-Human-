package com.human.common.gameplay.entity.living.human.marine.ai.standoff;

import com.human.common.gameplay.item.GunItem;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;

/**
 * How far a marine wants to stay from what it is fighting, for the weapon it is holding.
 * <p>
 * ⚠ THE POINT IS NOT TO RETREAT. It is to stop standing inside something's swing while shooting it. A marine holding a
 * shotgun should be close and a marine holding a rifle should not, but neither should be letting a zombie walk into
 * contact and stay there.
 * <p>
 * Two forces set the distance. The FLOOR is the enemy's own reach plus a margin, so whatever is attacking cannot
 * actually land a hit. The CEILING is the weapon's OPTIMAL range - full damage, before falloff starts - so backing off
 * can never cost the marine the shot it is backing off to take. A shotgun's optimal range is short, so a shotgun marine
 * stays close, exactly as it should.
 */
public class StandoffRange {

    /**
     * Clear air the marine wants between itself and the enemy's reach. Roughly a stride: enough that a target closing
     * in has to actually cross ground before it can swing, rather than arriving already in contact.
     */
    private static final double REACH_MARGIN_IN_BLOCKS = 1.5;

    /**
     * A marine never crowds closer than this even with the shortest-ranged weapon in the game.
     */
    private static final double MINIMUM_STANDOFF_IN_BLOCKS = 2.0;

    /**
     * ⚠ AND NEVER BACKS FURTHER THAN THIS, whatever the weapon says. A sniper's optimal range is seventy blocks; a
     * marine reversing seventy blocks out of a firefight has left the squad, not held its ground. Long-range weapons
     * get a preference for distance, not permission to leave.
     */
    private static final double MAXIMUM_STANDOFF_IN_BLOCKS = 6.0;

    /**
     * How much of a weapon's optimal range the marine would LIKE between itself and the target, before the floor and
     * the ceiling get their say. Small, because this is a preference for elbow room and not an attempt to snipe.
     */
    private static final double PREFERRED_FRACTION_OF_OPTIMAL_RANGE = 0.15;

    public static double computeStandoffDistance(Mob mob, LivingEntity target, ItemStack itemStack, double weaponReach) {
        var optimalRange = computeOptimalRange(itemStack, weaponReach);
        var floor = computeTargetReach(mob, target) + REACH_MARGIN_IN_BLOCKS;
        var preferred = Math.max(floor, optimalRange * PREFERRED_FRACTION_OF_OPTIMAL_RANGE);
        // The ceiling is the weapon's own optimal range, so a short-ranged weapon pulls the marine back IN rather than
        // letting the preference above push it out of its effective band.
        var ceiling = Math.min(optimalRange, MAXIMUM_STANDOFF_IN_BLOCKS);

        return Math.clamp(preferred, MINIMUM_STANDOFF_IN_BLOCKS, Math.max(MINIMUM_STANDOFF_IN_BLOCKS, ceiling));
    }

    /**
     * The distance at which the weapon still does full damage.
     * <p>
     * For a gun that is where its damage falloff begins - the same {@code damageFalloffStartFraction} the hit
     * resolution uses, so "optimal" here means exactly what it means when a round actually lands. For a knife there is
     * no falloff, so the reach IS the optimal range.
     */
    private static double computeOptimalRange(ItemStack itemStack, double weaponReach) {
        if (itemStack.getItem() instanceof GunItem gunItem) {
            var fireMode = gunItem.getGunConfig().getDefaultFireMode();

            return weaponReach * fireMode.damageFalloffStartFraction();
        }

        return weaponReach;
    }

    /**
     * How far the TARGET can reach, from the same hitboxes vanilla uses to resolve its attack: its bounding box widened
     * a little, against the marine's. Derived rather than assumed, so a crusher gets a wider berth than a facehugger
     * without anything here naming either of them.
     */
    private static double computeTargetReach(Mob mob, LivingEntity target) {
        return (target.getBbWidth() / 2.0) + (mob.getBbWidth() / 2.0) + 0.2;
    }

    private StandoffRange() {
        throw new UnsupportedOperationException();
    }
}
