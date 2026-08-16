package com.human.common.gameplay.entity.living.human.marine.ai.standoff;

import com.blib.api.common.goap.v1.GOAPSensors;
import com.human.common.gameplay.entity.living.human.marine.Marine;
import com.human.common.gameplay.entity.living.human.marine.ai.combat.CombatSensors;
import com.human.common.gameplay.entity.living.human.marine.ai.fall_back.FallBackSensors;
import com.just.ai.goap.StateKey;
import com.just.ai.goap.sensor.Sensor;
import com.just.ai.goap.sensor.Sensors;
import com.just.ai.goap.state.ReadableWorldState;
import com.just.core.functional.option.Option;
import net.minecraft.world.entity.LivingEntity;

public class StandoffSensors {

    /**
     * Extra distance the marine must regain before it stops giving ground.
     * <p>
     * ⚠ HYSTERESIS, AND IT IS NOT OPTIONAL. Without it the marine steps back over the standoff line, the condition goes
     * false, it stops, the target closes half a block, and it starts again - a stutter every few ticks. Backing off a
     * little past the line means the target has real ground to make up before this fires a second time.
     */
    private static final double RELEASE_MARGIN_IN_BLOCKS = 1.0;

    /**
     * Whether whatever the marine is fighting has got inside the distance it wants to hold for the weapon in its hands.
     */
    public static final Sensor.Mono<Marine, Boolean> IS_TARGET_INSIDE_STANDOFF = Sensors.lazyCompose(
        StateKey.sensed("is_target_inside_standoff"),
        (marine, worldState) -> computeStandoffOrNull(marine, worldState, 0.0) != null
    );

    /**
     * The condition the standoff is trying to reach, and therefore the thing that ends it.
     * <p>
     * ⚠ Uses the RELEASE margin, so it becomes true a little further out than {@link #IS_TARGET_INSIDE_STANDOFF}
     * becomes false. True with no target at all, so a marine whose attacker dies mid-step finds the effect already
     * satisfied and the plan completes instead of running on.
     */
    public static final Sensor.Mono<Marine, Boolean> IS_AT_STANDOFF_DISTANCE = Sensors.lazyCompose(
        StateKey.sensed("is_at_standoff_distance"),
        (marine, worldState) -> computeStandoffOrNull(marine, worldState, RELEASE_MARGIN_IN_BLOCKS) == null
    );

    /**
     * Whether the marine should be giving ground to hold its weapon's distance.
     * <p>
     * ⚠ EXPLICITLY YIELDS TO THE WITHDRAWAL. Both behaviours hold the MOVE mask, and a marine that is low on ammunition
     * with something charging it has a bigger problem than spacing - so if a fall-back is under way this stays false
     * rather than the two plans trading the mask tick by tick, which is precisely what caused the side-to-side bug.
     */
    public static final Sensor.Mono<Marine, Boolean> SHOULD_MAINTAIN_STANDOFF = Sensors.lazyCompose(
        StateKey.sensed("should_maintain_standoff"),
        (marine, worldState) -> worldState.getOrDefault(IS_TARGET_INSIDE_STANDOFF.key(), false)
            && !worldState.getOrDefault(FallBackSensors.IS_FALLING_BACK.key(), false)
    );

    /**
     * The target that is too close, or null if none is. Shared by both distance sensors so the two can never disagree
     * about which weapon or which target they are talking about.
     *
     * @param extraDistance widens the standoff, for the release test.
     */
    public static LivingEntity computeStandoffOrNull(Marine marine, ReadableWorldState worldState, double extraDistance) {
        var attackTargetOption = worldState.getOrDefault(GOAPSensors.NEAREST_ATTACKABLE_TARGET.key(), Option.<LivingEntity>none());

        if (attackTargetOption.isNone()) {
            return null;
        }

        var equippedWeaponOption = worldState.getOrDefault(CombatSensors.BEST_WEAPON_IN_HANDS.key(), Option.none());

        if (equippedWeaponOption.isNone()) {
            return null;
        }

        var attackTarget = attackTargetOption.unwrap();
        var equippedWeapon = equippedWeaponOption.unwrap();
        var itemStack = marine.getItemBySlot(equippedWeapon.itemTarget().equipmentSlot());
        var weaponReach = equippedWeapon.strategy().getRangeForWeapon(marine, itemStack);
        var standoff = StandoffRange.computeStandoffDistance(marine, attackTarget, itemStack, weaponReach) + extraDistance;

        return marine.distanceToSqr(attackTarget) < standoff * standoff
            ? attackTarget
            : null;
    }

    private StandoffSensors() {
        throw new UnsupportedOperationException();
    }
}
