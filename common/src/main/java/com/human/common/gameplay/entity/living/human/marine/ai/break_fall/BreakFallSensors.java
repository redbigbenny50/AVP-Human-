package com.human.common.gameplay.entity.living.human.marine.ai.break_fall;

import com.human.common.gameplay.entity.living.human.marine.Marine;
import com.human.common.gameplay.entity.living.human.marine.ai.break_fall.sensor.CanPlaceWaterAtLandingSensor;
import com.human.common.gameplay.entity.living.human.marine.ai.break_fall.sensor.IsCloseToLandingSensor;
import com.human.common.gameplay.entity.living.human.marine.ai.break_fall.sensor.LandingBlockPosSensor;
import com.human.common.gameplay.entity.living.human.marine.ai.break_fall.sensor.WillLandInFluidSensor;
import com.just.ai.goap.StateKey;
import com.just.ai.goap.sensor.Sensor;
import com.just.ai.goap.sensor.Sensors;
import com.just.core.functional.option.Option;
import net.minecraft.core.BlockPos;

public class BreakFallSensors {

    /**
     * Minimum fall distance (in blocks) that would cause fall damage. Minecraft deals fall damage starting at 4 blocks
     * fallen, but we want to act before that.
     */
    private static final float FALL_DAMAGE_THRESHOLD = 3.0f;

    /**
     * Maximum distance below the marine to search for a landing block.
     */
    public static final int MAX_LANDING_SEARCH_DEPTH = 64;

    /**
     * Distance (in blocks) from the landing position at which the marine should place water.
     */
    public static final double WATER_PLACEMENT_DISTANCE = 4.0;

    public static final Sensor.Mono<Marine, Boolean> IS_FALLING = Sensors.map(
        StateKey.sensed("is_falling"),
        marine -> !marine.onGround() && marine.getDeltaMovement().y < 0
    );

    public static final Sensor.Mono<Marine, Float> FALL_DISTANCE = Sensors.map(
        StateKey.sensed("fall_distance"),
        marine -> marine.fallDistance
    );

    public static final Sensor.Mono<Marine, Boolean> WILL_TAKE_FALL_DAMAGE = Sensors.compose(
        FALL_DISTANCE.key(),
        StateKey.sensed("will_take_fall_damage"),
        (marine, fallDistance) -> fallDistance > FALL_DAMAGE_THRESHOLD
    );

    /**
     * Finds the landing position where water should be placed. This is the first air/replaceable block directly above a
     * solid block below the marine.
     */
    public static final Sensor.Mono<Marine, Option<BlockPos>> LANDING_BLOCK_POS = Sensors.map(
        StateKey.sensed("landing_block_pos"),
        LandingBlockPosSensor::sense
    );

    /**
     * Checks if the marine will land in a fluid (water, lava, etc.) and doesn't need to break their fall.
     */
    public static final Sensor.Mono<Marine, Boolean> WILL_LAND_IN_FLUID = Sensors.map(
        StateKey.sensed("will_land_in_fluid"),
        WillLandInFluidSensor::sense
    );

    /**
     * Checks if water can be placed at the landing position.
     */
    public static final Sensor.Mono<Marine, Boolean> CAN_PLACE_WATER_AT_LANDING = Sensors.lazyCompose(
        StateKey.sensed("can_place_water_at_landing"),
        CanPlaceWaterAtLandingSensor::sense
    );

    /**
     * Checks if the marine is close enough to the landing position to place water.
     */
    public static final Sensor.Mono<Marine, Boolean> IS_CLOSE_TO_LANDING = Sensors.lazyCompose(
        StateKey.sensed("is_close_to_landing"),
        IsCloseToLandingSensor::sense
    );

    public static final Sensor.Mono<Marine, Boolean> SHOULD_BREAK_FALL = Sensors.lazyCompose(
        StateKey.sensed("should_break_fall"),
        (marine, worldState) -> {
            var isFalling = worldState.getOrDefault(IS_FALLING.key(), false);
            var willTakeFallDamage = worldState.getOrDefault(WILL_TAKE_FALL_DAMAGE.key(), false);
            var willLandInFluid = worldState.getOrDefault(WILL_LAND_IN_FLUID.key(), false);

            return isFalling && willTakeFallDamage && !willLandInFluid;
        }
    );

    private BreakFallSensors() {
        throw new UnsupportedOperationException();
    }
}
