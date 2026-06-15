package com.human.common.gameplay.entity.living.human.marine.ai.tame_wolf;

import com.blib.api.common.inventory.v1.BLibInventory;
import com.human.common.gameplay.entity.living.human.marine.Marine;
import com.just.ai.goap.StateKey;
import com.just.ai.goap.sensor.Compose;
import com.just.ai.goap.sensor.Sensor;
import com.just.ai.goap.sensor.Sensors;
import com.just.core.functional.option.Option;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Items;

public class TameWolfSensors {

    private static final int MAX_BONES_TO_COLLECT = 64;

    public static final Sensor.Mono<Marine, Option<Wolf>> NEAREST_UNTAMED_WOLF = Sensors.lazyCompose(
        StateKey.sensed("nearest_untamed_wolf"),
        (marine, worldState) -> {
            var wolves = marine.getEntitySenseCache().getByType(EntityType.WOLF);

            Wolf nearestWolf = null;
            double nearestDistance = Double.MAX_VALUE;

            for (var wolf : wolves) {
                if (wolf.isTame()) {
                    continue;
                }

                var distance = marine.distanceToSqr(wolf);

                if (distance < nearestDistance) {
                    nearestDistance = distance;
                    nearestWolf = wolf;
                }
            }

            return Option.ofNullable(nearestWolf);
        }
    );

    public static final Sensor.Mono<Marine, Boolean> HAS_UNTAMED_WOLF_NEARBY = Sensors.compose(
        NEAREST_UNTAMED_WOLF.key(),
        StateKey.sensed("has_untamed_wolf_nearby"),
        (marine, wolfOption) -> wolfOption.isSome()
    );

    public static final Sensor.Mono<Marine, Option<ItemEntity>> NEAREST_BONE_IN_WORLD = Sensors.lazyCompose(
        StateKey.sensed("nearest_bone_in_world"),
        (marine, worldState) -> {
            var itemEntities = marine.getEntitySenseCache()
                .getByItem(Items.BONE);

            ItemEntity nearestBone = null;
            double nearestDistance = Double.MAX_VALUE;

            for (var itemEntity : itemEntities) {
                var distance = marine.distanceToSqr(itemEntity);

                if (distance < nearestDistance) {
                    nearestDistance = distance;
                    nearestBone = itemEntity;
                }
            }

            return Option.ofNullable(nearestBone);
        }
    );

    public static final Sensor.Mono<Marine, Boolean> HAS_BONE_IN_WORLD = Sensors.compose(
        NEAREST_BONE_IN_WORLD.key(),
        StateKey.sensed("has_bone_in_world"),
        (marine, boneOption) -> boneOption.isSome()
    );

    public static final Compose<Marine, Option<ItemEntity>, Boolean> IS_NEAREST_BONE_IN_RANGE = Sensors.compose(
        NEAREST_BONE_IN_WORLD.key(),
        StateKey.sensed("is_nearest_bone_in_range"),
        (marine, boneOption) -> boneOption.isSomeAnd(bone -> marine.distanceToSqr(bone) < 4)
    );

    public static final Sensor.Mono<Marine, Integer> BONE_COUNT_IN_INVENTORY = Sensors.lazyCompose(
        StateKey.sensed("bone_count_in_inventory"),
        (marine, worldState) -> {
            var inventory = marine.getInventory();
            return inventory.selectEntries(Items.BONE)
                .stream()
                .mapToInt(BLibInventory.Entry::getItemCount)
                .sum();
        }
    );

    public static final Sensor.Mono<Marine, Boolean> HAS_BONE_IN_INVENTORY = Sensors.compose(
        BONE_COUNT_IN_INVENTORY.key(),
        StateKey.sensed("has_bone_in_inventory"),
        (marine, count) -> count > 0
    );

    public static final Sensor.Mono<Marine, Boolean> HAS_BONE_EQUIPPED = Sensors.lazyCompose(
        StateKey.sensed("has_bone_equipped"),
        (marine, worldState) -> marine.getMainHandItem().is(Items.BONE) || marine.getOffhandItem().is(Items.BONE)
    );

    public static final Compose<Marine, Option<Wolf>, Boolean> IS_WOLF_IN_RANGE = Sensors.compose(
        NEAREST_UNTAMED_WOLF.key(),
        StateKey.sensed("is_wolf_in_range"),
        (marine, wolfOption) -> wolfOption.isSomeAnd(wolf -> marine.distanceToSqr(wolf) < 4)
    );

    public static final Sensor.Mono<Marine, Boolean> SHOULD_COLLECT_MORE_BONES = Sensors.lazyCompose(
        StateKey.sensed("should_collect_more_bones"),
        (marine, worldState) -> {
            var boneCount = worldState.getOrDefault(BONE_COUNT_IN_INVENTORY.key(), 0);
            var hasWolfNearby = worldState.getOrDefault(HAS_UNTAMED_WOLF_NEARBY.key(), false);
            var hasBoneInWorld = worldState.getOrDefault(HAS_BONE_IN_WORLD.key(), false);

            // Collect bones if: no wolf nearby, bones available in world, and we have less than a stack.
            return !hasWolfNearby && hasBoneInWorld && boneCount < MAX_BONES_TO_COLLECT;
        }
    );

    private TameWolfSensors() {
        throw new UnsupportedOperationException();
    }
}
