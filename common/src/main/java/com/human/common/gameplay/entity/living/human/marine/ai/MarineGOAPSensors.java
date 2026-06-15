package com.human.common.gameplay.entity.living.human.marine.ai;

import com.just.ai.goap.StateKey;
import com.just.ai.goap.sensor.Sensor;
import com.just.ai.goap.sensor.Sensors;
import net.minecraft.world.entity.Entity;

public class MarineGOAPSensors {

    public static final Sensor.Mono<Entity, Boolean> IS_CURRENT_BLOCK_POS_REPLACEABLE = Sensors.map(
        StateKey.sensed("is_current_block_pos_replaceable"),
        entity -> entity.level().getBlockState(entity.blockPosition()).canBeReplaced()
    );

    public static final Sensor.Mono<Entity, Boolean> IS_IN_ULTRA_WARM_DIMENSION = Sensors.map(
        StateKey.sensed("is_in_ultra_warm_dimension"),
        entity -> entity.level().dimensionType().ultraWarm()
    );

    private MarineGOAPSensors() {
        throw new UnsupportedOperationException();
    }
}
