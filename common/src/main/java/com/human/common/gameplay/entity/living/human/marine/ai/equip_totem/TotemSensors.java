package com.human.common.gameplay.entity.living.human.marine.ai.equip_totem;

import com.blib.api.common.inventory.v1.BLibInventory;
import com.human.common.gameplay.entity.living.human.marine.Marine;
import com.just.ai.goap.StateKey;
import com.just.ai.goap.sensor.Compose;
import com.just.ai.goap.sensor.Sensor;
import com.just.ai.goap.sensor.Sensors;
import com.just.core.functional.option.Option;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Items;

public class TotemSensors {

    private static final float NEAR_DEATH_THRESHOLD = 0.1f;

    private static final int MIN_TOTEMS_TO_KEEP = 1;

    public static final Sensor.Mono<Marine, Option<ItemEntity>> NEAREST_TOTEM_IN_WORLD = Sensors.lazyCompose(
        StateKey.sensed("nearest_totem_in_world"),
        (marine, worldState) -> {
            var itemEntities = marine.getEntitySenseCache()
                .getByItem(Items.TOTEM_OF_UNDYING);

            ItemEntity nearestTotem = null;
            double nearestDistance = Double.MAX_VALUE;

            for (var itemEntity : itemEntities) {
                var distance = marine.distanceToSqr(itemEntity);

                if (distance < nearestDistance) {
                    nearestDistance = distance;
                    nearestTotem = itemEntity;
                }
            }

            return Option.ofNullable(nearestTotem);
        }
    );

    public static final Sensor.Mono<Marine, Boolean> HAS_TOTEM_IN_WORLD = Sensors.compose(
        NEAREST_TOTEM_IN_WORLD.key(),
        StateKey.sensed("has_totem_in_world"),
        (marine, totemOption) -> totemOption.isSome()
    );

    public static final Compose<Marine, Option<ItemEntity>, Boolean> IS_NEAREST_TOTEM_IN_RANGE = Sensors.compose(
        NEAREST_TOTEM_IN_WORLD.key(),
        StateKey.sensed("is_nearest_totem_in_range"),
        (marine, totemOption) -> totemOption.isSomeAnd(totem -> marine.distanceToSqr(totem) < 4)
    );

    public static final Sensor.Mono<Marine, Integer> TOTEM_COUNT_IN_INVENTORY = Sensors.lazyCompose(
        StateKey.sensed("totem_count_in_inventory"),
        (marine, worldState) -> {
            var inventory = marine.getInventory();
            return inventory.selectEntries(Items.TOTEM_OF_UNDYING)
                .stream()
                .mapToInt(BLibInventory.Entry::getItemCount)
                .sum();
        }
    );

    public static final Sensor.Mono<Marine, Boolean> HAS_TOTEM_IN_INVENTORY = Sensors.compose(
        TOTEM_COUNT_IN_INVENTORY.key(),
        StateKey.sensed("has_totem_in_inventory"),
        (marine, count) -> count > 0
    );

    public static final Sensor.Mono<Marine, Boolean> SHOULD_COLLECT_TOTEM = Sensors.lazyCompose(
        StateKey.sensed("should_collect_totem"),
        (marine, worldState) -> {
            var totemCount = worldState.getOrDefault(TOTEM_COUNT_IN_INVENTORY.key(), 0);
            var hasTotemInWorld = worldState.getOrDefault(HAS_TOTEM_IN_WORLD.key(), false);

            return hasTotemInWorld && totemCount < MIN_TOTEMS_TO_KEEP;
        }
    );

    public static final Sensor.Mono<Marine, Boolean> HAS_TOTEM_EQUIPPED = Sensors.lazyCompose(
        StateKey.sensed("has_totem_equipped"),
        (marine, worldState) -> marine.getOffhandItem().is(Items.TOTEM_OF_UNDYING)
    );

    public static final Sensor.Mono<Marine, Boolean> IS_NEAR_DEATH = Sensors.lazyCompose(
        StateKey.sensed("is_near_death"),
        (marine, worldState) -> {
            var healthRatio = marine.getHealth() / marine.getMaxHealth();
            return healthRatio <= NEAR_DEATH_THRESHOLD;
        }
    );

    public static final Sensor.Mono<Marine, Boolean> SHOULD_EQUIP_TOTEM = Sensors.lazyCompose(
        StateKey.sensed("should_equip_totem"),
        (marine, worldState) -> {
            var isNearDeath = worldState.getOrDefault(IS_NEAR_DEATH.key(), false);
            var hasTotemEquipped = worldState.getOrDefault(HAS_TOTEM_EQUIPPED.key(), false);
            var hasTotemInInventory = worldState.getOrDefault(HAS_TOTEM_IN_INVENTORY.key(), false);
            var hasTotemInWorld = worldState.getOrDefault(HAS_TOTEM_IN_WORLD.key(), false);

            return isNearDeath && !hasTotemEquipped && (hasTotemInInventory || hasTotemInWorld);
        }
    );

    private TotemSensors() {
        throw new UnsupportedOperationException();
    }
}
