package com.human.common.gameplay.entity.ai.goap;

import com.human.common.gameplay.entity.living.human.marine.Marine;
import com.human.common.registry.tag.HumanBiomeTags;
import com.human.compatibility.ad_astra.AdAstraHazards;
import com.human.compatibility.stellaris.StellarisHazards;
import com.just.ai.goap.StateKey;
import com.just.ai.goap.sensor.Sensor;
import com.just.ai.goap.sensor.Sensors;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

public class HumanGOAPSensors {

    public static final Sensor.Mono<Marine, Boolean> IS_NEAR_RADIOACTIVE_BIOME = Sensors.map(
        StateKey.sensed("is_near_radioactive_biome"),
        marine -> marine.getBiomeSenseCache().isNearby(HumanBiomeTags.IS_IRRADIATED)
    );

    /**
     * The leader this marine mirrors in the water has committed to being under it (water breathing, or just staying
     * down). This is what turns "surface to breathe" into "put a suit on" - see {@code BestArmorSetTargetSensor}.
     */
    public static final Sensor.Mono<Marine, Boolean> IS_LEADER_STAYING_UNDERWATER = Sensors.map(
        StateKey.sensed("is_leader_staying_underwater"),
        marine -> marine.getWaterMovement().isLeaderStayingUnderwater()
    );

    /**
     * No breathable air where the marine is standing - a Stellaris planet or Ad Astra world outside a pressurised
     * bubble. Both mods deal their own suffocation damage regardless of the vanilla air bar, so the marine has to DRESS
     * for it (see {@code BestArmorSetTargetSensor}); nothing else helps.
     */
    public static final Sensor.Mono<Marine, Boolean> IS_IN_AIRLESS_ENVIRONMENT = Sensors.map(
        StateKey.sensed("is_in_airless_environment"),
        marine -> marine.level() instanceof ServerLevel serverLevel
            && (StellarisHazards.isAirless(serverLevel, marine.blockPosition())
                || AdAstraHazards.isAirless(serverLevel, marine.blockPosition()))
    );

    /** Ad Astra extreme heat (Venus, Mercury's dayside). Only the MK50 shields it. */
    public static final Sensor.Mono<Marine, Boolean> IS_IN_EXTREME_HEAT = Sensors.map(
        StateKey.sensed("is_in_extreme_heat"),
        marine -> AdAstraHazards.isExtremeHeat(marine.level(), marine.blockPosition())
    );

    /** Air left, 0..1. The water-breathing potion's "low health": the closer to empty, the more urgent a drink. */
    public static final Sensor.Mono<LivingEntity, Float> AIR_RATIO = Sensors.map(
        StateKey.sensed("air_ratio"),
        livingEntity -> Math.max(0.0F, (float) livingEntity.getAirSupply() / Math.max(1, livingEntity.getMaxAirSupply()))
    );

    /** Ticks of Water Breathing still running, so a fresh potion is not wasted on top of a long one. */
    public static final Sensor.Mono<LivingEntity, Integer> WATER_BREATHING_REMAINING_TICKS = Sensors.map(
        StateKey.sensed("water_breathing_remaining_ticks"),
        livingEntity -> {
            var effect = livingEntity.getEffect(MobEffects.WATER_BREATHING);
            return effect == null ? 0 : effect.getDuration();
        }
    );

    /** Ad Astra extreme cold (Glacio, the Moon's night). Only the MK50 shields it. */
    public static final Sensor.Mono<Marine, Boolean> IS_IN_EXTREME_COLD = Sensors.map(
        StateKey.sensed("is_in_extreme_cold"),
        marine -> AdAstraHazards.isExtremeCold(marine.level(), marine.blockPosition())
    );
}
