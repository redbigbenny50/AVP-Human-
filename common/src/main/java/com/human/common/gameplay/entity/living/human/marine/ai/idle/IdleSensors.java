package com.human.common.gameplay.entity.living.human.marine.ai.idle;

import com.human.common.gameplay.entity.living.human.marine.Marine;
import com.just.ai.goap.StateKey;
import com.just.ai.goap.sensor.Sensor;
import com.just.ai.goap.sensor.Sensors;

public class IdleSensors {

    private static final StateKey.Sensed<Boolean> IS_BORED_KEY = StateKey.sensed("is_bored");

    public static final Sensor.Mono<Marine, Boolean> IS_BORED =
        Sensors.map(IS_BORED_KEY, marine -> marine.getTicksUntilBored() == 0);

    private static final StateKey.Sensed<Boolean> IS_AWAY_FROM_HOME_KEY = StateKey.sensed("is_away_from_home");

    /** How far a camp marine may be from home before it walks back, in blocks. */
    public static final double HOME_RADIUS = 24.0;

    /**
     * Oct 5 - how far a DISARMED camp marine may be from home: next to it. [stated] "if they have no knife for some
     * reason which should be impossible they would fall back to their camp".
     */
    public static final double DISARMED_HOME_RADIUS = 3.0;

    /** [stated] Oct 5: a camp marine further than {@link #HOME_RADIUS} from its camp (see Marine#getCampHome). */
    public static final Sensor.Mono<Marine, Boolean> IS_AWAY_FROM_HOME = Sensors.map(
        IS_AWAY_FROM_HOME_KEY,
        // Also false while home is flooded or blocked, or while resting after failed paths - so it never loops.
        marine -> marine.shouldReturnHome(marine.isDisarmed() ? DISARMED_HOME_RADIUS : HOME_RADIUS)
    );

    /**
     * Oct 5 audit - whether the marine may walk home now. A fight normally keeps it in place, but a DISARMED marine (no
     * usable gun, no melee weapon) is exactly the one that must go home through a fight - the walk-home action used to
     * require "no target", so the knifeless fall-back could never happen while anything hostile was near.
     */
    public static final Sensor.Mono<Marine, Boolean> IS_CLEAR_TO_GO_HOME = Sensors.lazyCompose(
        StateKey.sensed("is_clear_to_go_home"),
        (marine, worldState) -> marine.isDisarmed()
            || !worldState.getOrDefault(com.blib.api.common.goap.v1.GOAPSensors.HAS_ATTACK_TARGET.key(), false)
    );

    /**
     * Oct 5 audit - whether the marine may make for the shore now. "Get out of the water" used to require "no target",
     * so a marine in a pit with hostiles on the bank above never tried to leave - and could not reach them either. Now
     * it leaves whenever it cannot shoot from where it is.
     */
    public static final Sensor.Mono<Marine, Boolean> IS_CLEAR_TO_LEAVE_WATER = Sensors.lazyCompose(
        StateKey.sensed("is_clear_to_leave_water"),
        (marine, worldState) -> !worldState.getOrDefault(com.blib.api.common.goap.v1.GOAPSensors.HAS_ATTACK_TARGET.key(), false)
            || !worldState.getOrDefault(
                com.human.common.gameplay.entity.living.human.marine.ai.combat.CombatSensors.IS_ATTACK_TARGET_IN_RANGE_OF_EQUIPPED_BEST_WEAPON
                    .key(),
                false
            )
    );

    private static final StateKey.Sensed<Boolean> IS_IN_WATER_KEY = StateKey.sensed("is_in_water");

    /**
     * [stated] Oct 4: an idle marine in water heads for shore (see GetOutOfWaterAction). Only water DEEP ENOUGH TO SWIM
     * counts - the same threshold the float hop uses. [stated] "make sure marines aren't confused by flowing water":
     * {@code isInWater()} alone is true for a sliver of flowing water at the feet, so a marine wading a stream or the
     * run-off at a waterfall's foot would stop wandering and go hunting for a shore it is already standing on.
     */
    public static final Sensor.Mono<Marine, Boolean> IS_IN_WATER = Sensors.map(
        IS_IN_WATER_KEY,
        marine -> marine.isInWater()
            && marine.getFluidHeight(net.minecraft.tags.FluidTags.WATER) > marine.getFluidJumpThreshold()
    );

}
