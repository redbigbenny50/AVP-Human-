package com.human.common.gameplay.entity.living.human.marine.ai.collect_ammo;

import com.blib.api.common.goap.v1.GOAPSensors;
import com.human.common.gameplay.entity.living.human.marine.Marine;
import com.just.ai.goap.StateKey;
import com.just.ai.goap.sensor.Compose;
import com.just.ai.goap.sensor.Sensor;
import com.just.ai.goap.sensor.Sensors;
import com.just.core.functional.option.Option;
import net.minecraft.world.entity.item.ItemEntity;

/**
 * Oct 5 - dropped ammunition a marine can use. [stated] "pick it up off the ground if they can use it", with the design
 * he approved: only ammunition one of its guns fires, within {@value #SEARCH_RADIUS} blocks, and only while it is not
 * fighting - except a marine with no usable ammunition at all, which may break off for ammunition within
 * {@value #DRY_SEARCH_RADIUS} blocks.
 * <p>
 * Reads the shared item scan every marine already runs (EntitySenseCache), one lookup per ammunition type its guns
 * take, so it costs nothing extra to sense.
 * </p>
 */
public final class AmmoSensors {

    /** How far a marine goes for dropped ammunition, in blocks. */
    public static final double SEARCH_RADIUS = 16.0;

    /** How far a marine with NO usable ammunition breaks off a fight for it, in blocks. */
    public static final double DRY_SEARCH_RADIUS = 8.0;

    private static final double SEARCH_RADIUS_SQR = SEARCH_RADIUS * SEARCH_RADIUS;

    private static final double DRY_SEARCH_RADIUS_SQR = DRY_SEARCH_RADIUS * DRY_SEARCH_RADIUS;

    /** The pick-up reach, matching the totem and torch collection. */
    private static final double PICK_UP_RANGE_SQR = 4.0;

    /** The nearest dropped stack of ammunition this marine's guns take, within {@link #SEARCH_RADIUS}. */
    public static final Sensor.Mono<Marine, Option<ItemEntity>> NEAREST_USABLE_AMMO_IN_WORLD = Sensors.lazyCompose(
        StateKey.sensed("nearest_usable_ammo_in_world"),
        (marine, worldState) -> {
            ItemEntity nearest = null;
            var nearestDistance = SEARCH_RADIUS_SQR;

            for (var item : marine.getUsableAmmunition()) {
                // Oct 5 audit - a full pack would walk to the stack and fail to take it, again and again.
                if (!com.human.common.gameplay.entity.living.human.marine.MarineResupply.canPackTake(marine, item)) {
                    continue;
                }

                for (var itemEntity : marine.getEntitySenseCache().getByItem(item)) {
                    if (!itemEntity.isAlive()) {
                        continue;
                    }

                    var distance = marine.distanceToSqr(itemEntity);

                    if (distance < nearestDistance) {
                        nearestDistance = distance;
                        nearest = itemEntity;
                    }
                }
            }

            return Option.ofNullable(nearest);
        }
    );

    /** Whether there is any. */
    public static final Sensor.Mono<Marine, Boolean> HAS_USABLE_AMMO_IN_WORLD = Sensors.compose(
        NEAREST_USABLE_AMMO_IN_WORLD.key(),
        StateKey.sensed("has_usable_ammo_in_world"),
        (marine, ammoOption) -> ammoOption.isSome()
    );

    /** Whether the nearest is within reach. */
    public static final Compose<Marine, Option<ItemEntity>, Boolean> IS_NEAREST_AMMO_IN_RANGE = Sensors.compose(
        NEAREST_USABLE_AMMO_IN_WORLD.key(),
        StateKey.sensed("is_nearest_ammo_in_range"),
        (marine, ammoOption) -> ammoOption.isSomeAnd(ammo -> marine.distanceToSqr(ammo) < PICK_UP_RANGE_SQR)
    );

    /**
     * The planning gate: there is ammunition to collect, and either nothing to fight, or the marine is dry and the
     * ammunition is close.
     */
    public static final Sensor.Mono<Marine, Boolean> SHOULD_COLLECT_AMMO = Sensors.lazyCompose(
        StateKey.sensed("should_collect_ammo"),
        (marine, worldState) -> {
            var ammoOption = worldState.getOrDefault(NEAREST_USABLE_AMMO_IN_WORLD.key(), Option.<ItemEntity>none());

            if (ammoOption.isNone()) {
                return false;
            }

            if (!worldState.getOrDefault(GOAPSensors.HAS_ATTACK_TARGET.key(), false)) {
                return true;
            }

            return marine.isOutOfAmmunition() && marine.distanceToSqr(ammoOption.unwrap()) <= DRY_SEARCH_RADIUS_SQR;
        }
    );

    private AmmoSensors() {
        throw new UnsupportedOperationException();
    }
}
