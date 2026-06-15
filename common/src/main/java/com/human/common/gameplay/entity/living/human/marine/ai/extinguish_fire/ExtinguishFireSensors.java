package com.human.common.gameplay.entity.living.human.marine.ai.extinguish_fire;

import com.blib.api.common.inventory.v1.BLibInventory;
import com.human.common.gameplay.entity.living.human.marine.Marine;
import com.human.common.gameplay.entity.living.human.marine.ai.extinguish_fire.sensor.WaterBucketInInventorySensor;
import com.just.ai.goap.StateKey;
import com.just.ai.goap.sensor.Compose;
import com.just.ai.goap.sensor.Sensor;
import com.just.ai.goap.sensor.Sensors;
import com.just.core.functional.option.Option;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Items;

public class ExtinguishFireSensors {

    public static final Sensor.Mono<Marine, Option<BLibInventory.Entry>> WATER_BUCKET_IN_INVENTORY =
        Sensors.lazyCompose(
            WaterBucketInInventorySensor.KEY,
            WaterBucketInInventorySensor::sense
        );

    public static final Sensor.Mono<Marine, Boolean> HAS_WATER_BUCKET_IN_INVENTORY = Sensors.compose(
        WATER_BUCKET_IN_INVENTORY.key(),
        StateKey.sensed("has_water_bucket_in_inventory"),
        (marine, waterBucketOption) -> waterBucketOption.isSome()
    );

    public static final Sensor.Mono<LivingEntity, Boolean> HAS_WATER_BUCKET_EQUIPPED = Sensors.map(
        StateKey.sensed("has_water_bucket_equipped"),
        livingEntity -> livingEntity.getMainHandItem().is(Items.WATER_BUCKET)
    );

    public static final Sensor.Mono<Marine, Option<ItemEntity>> NEAREST_WATER_BUCKET_IN_WORLD = Sensors.map(
        StateKey.sensed("nearest_water_bucket_in_world"),
        marine -> {
            var itemEntities = marine.getEntitySenseCache().getByItem(Items.WATER_BUCKET);

            ItemEntity nearestWaterBucket = null;
            double nearestDistance = Double.MAX_VALUE;

            for (var itemEntity : itemEntities) {
                var distance = marine.distanceToSqr(itemEntity);

                if (distance < nearestDistance) {
                    nearestDistance = distance;
                    nearestWaterBucket = itemEntity;
                }
            }

            return Option.ofNullable(nearestWaterBucket);
        }
    );

    public static final Sensor.Mono<Marine, Boolean> HAS_WATER_BUCKET_IN_WORLD = Sensors.compose(
        NEAREST_WATER_BUCKET_IN_WORLD.key(),
        StateKey.sensed("has_water_bucket_in_world"),
        (marine, waterBucketOption) -> waterBucketOption.isSome()
    );

    public static final Compose<Marine, Option<ItemEntity>, Boolean> IS_NEAREST_WATER_BUCKET_IN_RANGE = Sensors.compose(
        NEAREST_WATER_BUCKET_IN_WORLD.key(),
        StateKey.sensed("is_nearest_water_bucket_in_range"),
        (marine, waterBucketOption) -> waterBucketOption.isSomeAnd(waterBucket -> marine.distanceToSqr(waterBucket) < 4)
    );
}
