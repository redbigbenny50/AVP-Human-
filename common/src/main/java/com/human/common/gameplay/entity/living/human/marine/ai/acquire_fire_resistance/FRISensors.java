package com.human.common.gameplay.entity.living.human.marine.ai.acquire_fire_resistance;

import com.human.common.gameplay.entity.ai.utility.sensor.BestItemSensor;
import com.human.common.gameplay.entity.ai.utility.sensor.EquippedItemSensor;
import com.human.common.gameplay.entity.ai.utility.sensor.ItemInInventorySensor;
import com.human.common.gameplay.entity.ai.utility.sensor.ItemInWorldSensor;
import com.human.common.gameplay.entity.living.human.ai.model.ItemTarget;
import com.human.common.gameplay.entity.living.human.marine.Marine;
import com.human.common.gameplay.entity.living.human.marine.ai.acquire_fire_resistance.strategy.FRIStrategy;
import com.human.common.gameplay.entity.living.human.marine.ai.acquire_fire_resistance.strategy.FRIStrategyResult;
import com.human.common.gameplay.entity.living.human.marine.ai.acquire_fire_resistance.strategy.FRIStrategySet;
import com.just.ai.goap.StateKey;
import com.just.ai.goap.sensor.Compose;
import com.just.ai.goap.sensor.Sensor;
import com.just.ai.goap.sensor.Sensors;
import com.just.core.functional.option.Option;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;

public class FRISensors {

    public static final Sensor.Mono<LivingEntity, Option<FRIStrategyResult<ItemTarget.Equipped>>> BEST_FRI_IN_HANDS = Sensors.lazyCompose(
        StateKey.sensed("best_fri_in_hands"),
        EquippedItemSensor.builder(FRIStrategySet.INSTANCE::getAll, FRIStrategyResult::new)
            .withEquipmentSlots(new EquipmentSlot[] { EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND })
            .build()::sense
    );

    public static final Sensor.Mono<Marine, Option<FRIStrategyResult<ItemTarget.Inventory>>> BEST_FRI_IN_INVENTORY =
        Sensors.lazyCompose(
            StateKey.sensed("best_fri_in_inventory"),
            ItemInInventorySensor.builder(FRIStrategySet.INSTANCE::getAll, FRIStrategyResult::new)
                .build()::sense
        );

    public static final Sensor.Mono<Marine, Option<FRIStrategyResult<ItemTarget.World>>> BEST_FRI_IN_WORLD = Sensors.lazyCompose(
        StateKey.sensed("best_fri_in_world"),
        ItemInWorldSensor.builder(FRIStrategySet.INSTANCE::getAll, FRIStrategyResult::new)
            .build()::sense
    );

    public static final Sensor.Mono<LivingEntity, Option<FRIStrategyResult<? extends ItemTarget>>> BEST_FRI = Sensors.lazyCompose(
        StateKey.sensed("best_fri"),
        BestItemSensor.<FRIStrategy, FRIStrategyResult<? extends ItemTarget>>builder()
            .withSensor(BEST_FRI_IN_HANDS)
            .withSensor(BEST_FRI_IN_INVENTORY)
            .withSensor(BEST_FRI_IN_WORLD)
            .build()::sense
    );

    // TODO: We don't need the marine here, GOAP should support this case.
    public static final Compose<Object, Option<FRIStrategyResult<? extends ItemTarget>>, ItemTarget.Location> BEST_FRI_LOCATION =
        Sensors.compose(
            BEST_FRI.key(),
            StateKey.sensed("best_fri_location"),
            ($1, bfriOption) -> bfriOption.map(result -> result.itemTarget().location()).unwrapOr(ItemTarget.Location.NONE)
        );

    public static final Compose<LivingEntity, Option<FRIStrategyResult<ItemTarget.World>>, Boolean> IS_BEST_WORLD_FRI_IN_RANGE =
        Sensors
            .compose(
                BEST_FRI_IN_WORLD.key(),
                StateKey.sensed("is_best_world_fri_in_range"),
                (livingEntity, bfriOption) -> bfriOption.isSomeAnd(
                    result -> livingEntity.distanceToSqr(result.itemTarget().itemEntity()) < 4
                )
            );
}
