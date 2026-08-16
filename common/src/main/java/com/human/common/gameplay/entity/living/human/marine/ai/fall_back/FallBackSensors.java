package com.human.common.gameplay.entity.living.human.marine.ai.fall_back;

import com.blib.api.common.goap.v1.GOAPSensors;
import com.human.common.gameplay.entity.living.human.marine.Marine;
import com.human.common.gameplay.entity.living.human.marine.ai.combat.CombatSensors;
import com.human.common.gameplay.item.GunItem;
import com.human.common.registry.init.HumanDataComponents;
import com.just.ai.goap.StateKey;
import com.just.ai.goap.sensor.Sensor;
import com.just.ai.goap.sensor.Sensors;
import com.just.core.functional.option.Option;
import net.minecraft.world.entity.LivingEntity;

public class FallBackSensors {

    /**
     * Fraction of a magazine below which a marine starts giving ground.
     * <p>
     * Deliberately higher than the reload trigger in {@code GunStrategy} (0.10F). If both fired at the same point the
     * marine would take a single step and then stand still for the whole magazine change, which is the opposite of the
     * intent. Starting at a quarter leaves time to open up the distance before the swap actually begins. It also sits
     * just above the 20% low-ammunition HUD warning, so the player sees the warning as the marine starts backing off.
     */
    public static final float LOW_AMMUNITION_THRESHOLD = 0.25F;

    /**
     * How close an attack target has to be to count as charging. A sniper trading shots across a valley is not under
     * pressure and has no reason to retreat, so this gate is an absolute distance rather than a fraction of the
     * weapon's range.
     */
    public static final double CHARGE_DISTANCE_IN_BLOCKS = 6.0;

    /**
     * Separation the marine is trying to open up. Comfortably inside every weapon's range - the shortest is the
     * flamethrower at 16 and the shotguns at 20 - so withdrawing can never push the target out of range and flip the
     * combat plan back into the form that claims the MOVE mask for itself.
     */
    public static final double SAFE_DISTANCE_IN_BLOCKS = 8.0;

    private static final double CHARGE_DISTANCE_SQR = CHARGE_DISTANCE_IN_BLOCKS * CHARGE_DISTANCE_IN_BLOCKS;

    private static final double SAFE_DISTANCE_SQR = SAFE_DISTANCE_IN_BLOCKS * SAFE_DISTANCE_IN_BLOCKS;

    /**
     * Whether the gun actually in the marine's hands is running dry.
     */
    public static final Sensor.Mono<Marine, Boolean> IS_LOW_ON_AMMUNITION = Sensors.lazyCompose(
        StateKey.sensed("is_low_on_ammunition"),
        (marine, worldState) -> {
            var equippedWeaponOption = worldState.getOrDefault(CombatSensors.BEST_WEAPON_IN_HANDS.key(), Option.none());

            if (equippedWeaponOption.isNone()) {
                return false;
            }

            var itemStack = marine.getItemBySlot(equippedWeaponOption.unwrap().itemTarget().equipmentSlot());

            if (!(itemStack.getItem() instanceof GunItem gunItem)) {
                return false;
            }

            var currentAmmunition = itemStack.getOrDefault(HumanDataComponents.AMMUNITION.get(), 0);
            var maximumAmmunition = gunItem.getGunConfig().maximumAmmunition();

            return currentAmmunition < maximumAmmunition * LOW_AMMUNITION_THRESHOLD;
        }
    );

    /**
     * Whether something is close enough to be bearing down on the marine.
     */
    public static final Sensor.Mono<Marine, Boolean> IS_ATTACK_TARGET_CHARGING = Sensors.lazyCompose(
        StateKey.sensed("is_attack_target_charging"),
        (marine, worldState) -> {
            var attackTargetOption = worldState.getOrDefault(GOAPSensors.NEAREST_ATTACKABLE_TARGET.key(), Option.none());

            return attackTargetOption.isSomeAnd(attackTarget -> marine.distanceToSqr(attackTarget) <= CHARGE_DISTANCE_SQR);
        }
    );

    /**
     * The condition the withdrawal is trying to reach, and therefore the thing that ends it.
     * <p>
     * True with no target at all, so a marine whose attacker dies mid-retreat finds the effect already satisfied and
     * the plan simply completes rather than running on.
     */
    public static final Sensor.Mono<Marine, Boolean> IS_AT_SAFE_DISTANCE = Sensors.lazyCompose(
        StateKey.sensed("is_at_safe_distance"),
        (marine, worldState) -> {
            var attackTargetOption = worldState.getOrDefault(GOAPSensors.NEAREST_ATTACKABLE_TARGET.key(), Option.none());

            if (attackTargetOption.isNone()) {
                return true;
            }

            return marine.distanceToSqr((LivingEntity) attackTargetOption.unwrap()) >= SAFE_DISTANCE_SQR;
        }
    );

    /**
     * Whether a withdrawal is currently under way.
     * <p>
     * Mirrors the runtime preconditions of the fall-back action rather than the conditions that STARTED it, so it stays
     * true for the whole retreat and goes false at exactly the moment the retreat ends. Other behaviours that want to
     * keep out of the way should test this, not {@link #SHOULD_FALL_BACK} - that one stops being true as soon as the
     * marine is more than a charge away from its attacker, which is a third of the way into the withdrawal.
     */
    public static final Sensor.Mono<Marine, Boolean> IS_FALLING_BACK = Sensors.lazyCompose(
        StateKey.sensed("is_falling_back"),
        (marine, worldState) -> worldState.getOrDefault(IS_LOW_ON_AMMUNITION.key(), false)
            && worldState.getOrDefault(GOAPSensors.HAS_ATTACK_TARGET.key(), false)
            && !worldState.getOrDefault(IS_AT_SAFE_DISTANCE.key(), false)
    );

    /**
     * Planning gate for the whole behaviour: low on rounds, something is on top of us, and we have not already backed
     * off far enough.
     */
    public static final Sensor.Mono<Marine, Boolean> SHOULD_FALL_BACK = Sensors.lazyCompose(
        StateKey.sensed("should_fall_back"),
        (marine, worldState) -> worldState.getOrDefault(IS_FALLING_BACK.key(), false)
            && worldState.getOrDefault(IS_ATTACK_TARGET_CHARGING.key(), false)
    );

    private FallBackSensors() {
        throw new UnsupportedOperationException();
    }
}
