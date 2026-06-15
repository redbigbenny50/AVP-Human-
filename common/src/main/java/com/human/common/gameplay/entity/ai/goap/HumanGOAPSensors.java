package com.human.common.gameplay.entity.ai.goap;

import com.human.common.gameplay.entity.living.human.marine.Marine;
import com.human.common.registry.tag.HumanBiomeTags;
import com.just.ai.goap.StateKey;
import com.just.ai.goap.sensor.Sensor;
import com.just.ai.goap.sensor.Sensors;

public class HumanGOAPSensors {

    public static final Sensor.Mono<Marine, Boolean> IS_NEAR_RADIOACTIVE_BIOME = Sensors.map(
        StateKey.sensed("is_near_radioactive_biome"),
        marine -> marine.getBiomeSenseCache().isNearby(HumanBiomeTags.IS_IRRADIATED)
    );
}
