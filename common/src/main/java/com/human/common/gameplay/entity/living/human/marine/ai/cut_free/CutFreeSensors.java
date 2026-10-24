package com.human.common.gameplay.entity.living.human.marine.ai.cut_free;

import com.human.common.gameplay.entity.living.human.marine.Marine;
import com.human.common.gameplay.entity.living.human.marine.MarineAllyUtil;
import com.human.common.registry.tag.HumanItemTags;
import com.human.compatibility.avp_predator.HumanPredatorNet;
import com.just.ai.goap.StateKey;
import com.just.ai.goap.sensor.Compose;
import com.just.ai.goap.sensor.Sensor;
import com.just.ai.goap.sensor.Sensors;
import com.just.core.functional.option.Option;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;

/**
 * Sensing a squadmate caught in avp_predator's net, and whether this marine has a blade to cut it loose with.
 * <p>
 * [stated] "if a marine is netted and there are other marines around they will move to the netted marine and cut them
 * free with the combat knife." Same shape as the tame-wolf package: find the target, equip the tool, walk over, use it.
 */
public class CutFreeSensors {

    /** Reach at which the knife can be used - a little over two blocks, the same "in range" the wolf package uses. */
    public static final double CUT_RANGE_SQR = 2.5 * 2.5;

    public static final Sensor.Mono<Marine, Option<Marine>> NEAREST_NETTED_MARINE = Sensors.lazyCompose(
        StateKey.sensed("nearest_netted_marine"),
        (marine, worldState) -> {
            // Every soldier type (all share the Marine class) - a netted W-Y or tactical squadmate is cut free too.
            var marines = marine.getEntitySenseCache().getByClass(Marine.class);

            Marine nearest = null;
            double nearestDistance = Double.MAX_VALUE;

            for (var other : marines) {
                if (other == marine || !other.isAlive() || !HumanPredatorNet.isNetted(other)) {
                    continue;
                }

                // A marine on the other side of a broken alliance stays in the net.
                if (!MarineAllyUtil.isMarineAlly(marine, other)) {
                    continue;
                }

                var distance = marine.distanceToSqr(other);

                if (distance < nearestDistance) {
                    nearestDistance = distance;
                    nearest = other;
                }
            }

            return Option.ofNullable(nearest);
        }
    );

    public static final Sensor.Mono<Marine, Boolean> HAS_NETTED_MARINE_NEARBY = Sensors.compose(
        NEAREST_NETTED_MARINE.key(),
        StateKey.sensed("has_netted_marine_nearby"),
        (marine, nettedOption) -> nettedOption.isSome()
    );

    public static final Compose<Marine, Option<Marine>, Boolean> IS_NETTED_MARINE_IN_RANGE = Sensors.compose(
        NEAREST_NETTED_MARINE.key(),
        StateKey.sensed("is_netted_marine_in_range"),
        (marine, nettedOption) -> nettedOption.isSomeAnd(netted -> marine.distanceToSqr(netted) < CUT_RANGE_SQR)
    );

    public static final Sensor.Mono<Marine, Boolean> HAS_BLADE_IN_INVENTORY = Sensors.lazyCompose(
        StateKey.sensed("has_blade_in_inventory"),
        (marine, worldState) -> !marine.getInventory().filterEntriesByStack(CutFreeSensors::isBlade).isEmpty()
    );

    public static final Sensor.Mono<Marine, Boolean> HAS_BLADE_EQUIPPED = Sensors.lazyCompose(
        StateKey.sensed("has_blade_equipped"),
        (marine, worldState) -> isBlade(marine.getMainHandItem())
    );

    /**
     * The combat knife first and foremost, plus whatever the player's own cut-free rule accepts (any sword or axe), so
     * a marine handed a sword can still do the job.
     */
    public static boolean isBlade(ItemStack itemStack) {
        return itemStack.is(HumanItemTags.DAGGERS) || itemStack.is(ItemTags.SWORDS) || itemStack.is(ItemTags.AXES);
    }

    private CutFreeSensors() {
        throw new UnsupportedOperationException();
    }
}
