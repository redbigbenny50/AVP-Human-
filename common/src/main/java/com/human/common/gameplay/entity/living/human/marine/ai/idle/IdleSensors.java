package com.human.common.gameplay.entity.living.human.marine.ai.idle;

import com.human.common.gameplay.entity.living.human.marine.Marine;
import com.just.ai.goap.StateKey;
import com.just.ai.goap.sensor.Sensor;
import com.just.ai.goap.sensor.Sensors;

public class IdleSensors {

    private static final StateKey.Sensed<Boolean> IS_BORED_KEY = StateKey.sensed("is_bored");

    public static final Sensor.Mono<Marine, Boolean> IS_BORED =
        Sensors.map(IS_BORED_KEY, marine -> marine.getTicksUntilBored() == 0);

}
