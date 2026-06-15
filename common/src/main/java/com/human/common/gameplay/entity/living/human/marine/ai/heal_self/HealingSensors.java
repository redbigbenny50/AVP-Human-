package com.human.common.gameplay.entity.living.human.marine.ai.heal_self;

import com.human.common.gameplay.entity.ai.utility.sensor.BestItemSensor;
import com.human.common.gameplay.entity.ai.utility.sensor.EquippedItemSensor;
import com.human.common.gameplay.entity.ai.utility.sensor.ItemInInventorySensor;
import com.human.common.gameplay.entity.ai.utility.sensor.ItemInWorldSensor;
import com.human.common.gameplay.entity.living.human.ai.model.ItemTarget;
import com.human.common.gameplay.entity.living.human.marine.Marine;
import com.human.common.gameplay.entity.living.human.marine.ai.heal_self.strategy.HealingStrategy;
import com.human.common.gameplay.entity.living.human.marine.ai.heal_self.strategy.HealingStrategyResult;
import com.human.common.gameplay.entity.living.human.marine.ai.heal_self.strategy.HealingStrategySet;
import com.just.ai.goap.StateKey;
import com.just.ai.goap.sensor.Compose;
import com.just.ai.goap.sensor.Sensor;
import com.just.ai.goap.sensor.Sensors;
import com.just.core.functional.option.Option;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;

public class HealingSensors {

    public static final Sensor.Mono<LivingEntity, Option<HealingStrategyResult<ItemTarget.Equipped>>> BEST_HEALING_ITEM_IN_HANDS = Sensors
        .lazyCompose(
            StateKey.sensed("best_healing_item_in_hands"),
            EquippedItemSensor.builder(HealingStrategySet.INSTANCE::getAll, HealingStrategyResult::new)
                .withEquipmentSlots(new EquipmentSlot[] { EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND })
                .build()::sense
        );

    public static final Sensor.Mono<Marine, Option<HealingStrategyResult<ItemTarget.Inventory>>> BEST_HEALING_ITEM_IN_INVENTORY =
        Sensors.lazyCompose(
            StateKey.sensed("best_healing_item_in_inventory"),
            ItemInInventorySensor.builder(HealingStrategySet.INSTANCE::getAll, HealingStrategyResult::new)
                .build()::sense
        );

    public static final Sensor.Mono<Marine, Option<HealingStrategyResult<ItemTarget.World>>> BEST_HEALING_ITEM_IN_WORLD = Sensors
        .lazyCompose(
            StateKey.sensed("best_healing_item_in_world"),
            ItemInWorldSensor.builder(HealingStrategySet.INSTANCE::getAll, HealingStrategyResult::new)
                .build()::sense
        );

    public static final Sensor.Mono<LivingEntity, Option<HealingStrategyResult<? extends ItemTarget>>> BEST_HEALING_ITEM = Sensors
        .lazyCompose(
            StateKey.sensed("best_healing_item"),
            BestItemSensor.<HealingStrategy, HealingStrategyResult<? extends ItemTarget>>builder()
                .withSensor(BEST_HEALING_ITEM_IN_HANDS)
                .withSensor(BEST_HEALING_ITEM_IN_INVENTORY)
                .withSensor(BEST_HEALING_ITEM_IN_WORLD)
                .build()::sense
        );

    public static final Compose<Object, Option<HealingStrategyResult<? extends ItemTarget>>, ItemTarget.Location> BEST_HEALING_ITEM_LOCATION =
        Sensors.compose(
            BEST_HEALING_ITEM.key(),
            StateKey.sensed("best_healing_item_location"),
            ($1, bhiOption) -> bhiOption.map(result -> result.itemTarget().location()).unwrapOr(ItemTarget.Location.NONE)
        );

    public static final Compose<LivingEntity, Option<HealingStrategyResult<ItemTarget.World>>, Boolean> IS_BEST_WORLD_HEALING_ITEM_IN_RANGE =
        Sensors.compose(
            BEST_HEALING_ITEM_IN_WORLD.key(),
            StateKey.sensed("is_best_world_healing_item_in_range"),
            (livingEntity, bhiOption) -> bhiOption.isSomeAnd(
                result -> livingEntity.distanceToSqr(result.itemTarget().itemEntity()) < 4
            )
        );
}
