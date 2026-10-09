package com.human.common.gameplay.entity.living.human.marine.ai.acquire_water_breathing;

import com.human.common.gameplay.entity.ai.utility.sensor.BestItemSensor;
import com.human.common.gameplay.entity.ai.utility.sensor.EquippedItemSensor;
import com.human.common.gameplay.entity.ai.utility.sensor.ItemInInventorySensor;
import com.human.common.gameplay.entity.ai.utility.sensor.ItemInWorldSensor;
import com.human.common.gameplay.entity.living.human.ai.model.ItemTarget;
import com.human.common.gameplay.entity.living.human.marine.Marine;
import com.human.common.gameplay.entity.living.human.marine.ai.acquire_water_breathing.strategy.WBIStrategy;
import com.human.common.gameplay.entity.living.human.marine.ai.acquire_water_breathing.strategy.WBIStrategyResult;
import com.human.common.gameplay.entity.living.human.marine.ai.acquire_water_breathing.strategy.WBIStrategySet;
import com.just.ai.goap.StateKey;
import com.just.ai.goap.sensor.Compose;
import com.just.ai.goap.sensor.Sensor;
import com.just.ai.goap.sensor.Sensors;
import com.just.core.functional.option.Option;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;

/**
 * The water-breathing item sensors, mirroring the fire-resistance set one for one, plus the trigger.
 * <p>
 * [stated] Sep 21: "if the player stays underwater too long or has an effect letting them breathe underwater the
 * marines will put on the pressure suit or mk50 if they have them. if they have waterbreathing potions they will use
 * them. if they have no way to breathe underwater and start drowning they will swim to the surface." The suit is the
 * armor sensor's job and takes precedence on its own: a suited marine breathes indefinitely and
 * {@link #WANTS_WATER_BREATHING} goes false. This package is the potion clause.
 */
public class WBISensors {

    /**
     * True while the marine is in the water, cannot breathe there on its own (no suit, no effect), and either has its
     * head under or is following a leader who has settled below. Reads the same water-movement state the swim code runs
     * on, so the potion, the suit and the surfacing rule can never disagree about what "needs air" means.
     */
    public static final Sensor.Mono<Marine, Boolean> WANTS_WATER_BREATHING = Sensors.map(
        StateKey.sensed("wants_water_breathing"),
        marine -> marine.isInWater()
            && !marine.getWaterMovement().canBreatheIndefinitely()
            && (marine.isUnderWater() || marine.getWaterMovement().isLeaderStayingUnderwater())
    );

    public static final Sensor.Mono<LivingEntity, Option<WBIStrategyResult<ItemTarget.Equipped>>> BEST_WBI_IN_HANDS = Sensors.lazyCompose(
        StateKey.sensed("best_wbi_in_hands"),
        EquippedItemSensor.builder(WBIStrategySet.INSTANCE::getAll, WBIStrategyResult::new)
            .withEquipmentSlots(new EquipmentSlot[] { EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND })
            .build()::sense
    );

    public static final Sensor.Mono<Marine, Option<WBIStrategyResult<ItemTarget.Inventory>>> BEST_WBI_IN_INVENTORY =
        Sensors.lazyCompose(
            StateKey.sensed("best_wbi_in_inventory"),
            ItemInInventorySensor.builder(WBIStrategySet.INSTANCE::getAll, WBIStrategyResult::new)
                .build()::sense
        );

    public static final Sensor.Mono<Marine, Option<WBIStrategyResult<ItemTarget.World>>> BEST_WBI_IN_WORLD = Sensors.lazyCompose(
        StateKey.sensed("best_wbi_in_world"),
        ItemInWorldSensor.builder(WBIStrategySet.INSTANCE::getAll, WBIStrategyResult::new)
            .build()::sense
    );

    public static final Sensor.Mono<LivingEntity, Option<WBIStrategyResult<? extends ItemTarget>>> BEST_WBI = Sensors.lazyCompose(
        StateKey.sensed("best_wbi"),
        BestItemSensor.<WBIStrategy, WBIStrategyResult<? extends ItemTarget>>builder()
            .withSensor(BEST_WBI_IN_HANDS)
            .withSensor(BEST_WBI_IN_INVENTORY)
            .withSensor(BEST_WBI_IN_WORLD)
            .build()::sense
    );

    public static final Compose<Object, Option<WBIStrategyResult<? extends ItemTarget>>, ItemTarget.Location> BEST_WBI_LOCATION =
        Sensors.compose(
            BEST_WBI.key(),
            StateKey.sensed("best_wbi_location"),
            ($1, bwbiOption) -> bwbiOption.map(result -> result.itemTarget().location()).unwrapOr(ItemTarget.Location.NONE)
        );

    public static final Compose<LivingEntity, Option<WBIStrategyResult<ItemTarget.World>>, Boolean> IS_BEST_WORLD_WBI_IN_RANGE =
        Sensors
            .compose(
                BEST_WBI_IN_WORLD.key(),
                StateKey.sensed("is_best_world_wbi_in_range"),
                (livingEntity, bwbiOption) -> bwbiOption.isSomeAnd(
                    result -> livingEntity.distanceToSqr(result.itemTarget().itemEntity()) < 4
                )
            );

    private WBISensors() {
        throw new UnsupportedOperationException();
    }
}
