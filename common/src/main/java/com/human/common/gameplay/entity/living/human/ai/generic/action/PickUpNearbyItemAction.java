package com.human.common.gameplay.entity.living.human.ai.generic.action;

import com.blib.api.common.inventory.v1.BLibInventory;
import com.blib.api.common.inventory.v1.BLibInventoryHolder;
import com.just.ai.goap.StateKey;
import com.just.ai.goap.action.Action;
import com.just.core.functional.option.Option;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;

import java.util.function.Function;

/**
 * Picks up the item entity a sensor found.
 * <p>
 * ⚠ Oct 5 audit - TAKES ONLY WHAT FITS. This used to hand the whole stack to the pack and then delete the item entity
 * whatever the pack actually accepted, so a marine with a nearly full pack destroyed the rest of every stack it picked
 * up - ammunition, totems, torches, weapons. Now the item entity keeps whatever did not fit, and is only removed once
 * it is empty.
 * </p>
 */
public final class PickUpNearbyItemAction {

    /**
     * Picks up as much of the sensed item as the pack takes.
     *
     * @param context             the action context
     * @param sensorKey           the sensed value holding the target
     * @param itemEntityExtractor how to get the item entity out of it
     * @param <T>                 the actor
     * @param <V>                 the sensed value
     * @return CONTINUE once anything was taken (the effect then ends the action), ABORT otherwise
     */
    public static <T extends LivingEntity & BLibInventoryHolder, V> Action.Signal perform(
        Action.Context<T> context,
        StateKey<Option<V>> sensorKey,
        Function<V, ItemEntity> itemEntityExtractor
    ) {
        var actor = context.getActor();
        var worldState = context.getWorldState();
        var valueOption = worldState.getOrDefault(sensorKey, Option.none());

        if (valueOption.isNone()) {
            return Action.Signal.ABORT;
        }

        var itemEntity = itemEntityExtractor.apply(valueOption.unwrap());

        if (itemEntity == null || itemEntity.isRemoved() || itemEntity.getItem().isEmpty()) {
            return Action.Signal.ABORT;
        }

        var offered = itemEntity.getItem().copy();
        var addResult = actor.getInventory().addItemStack(offered);
        var taken = switch (addResult) {
            case BLibInventory.AddResult.Success success -> offered.getCount();
            case BLibInventory.AddResult.Partial partial -> partial.count();
            default -> 0;
        };

        if (taken <= 0) {
            return Action.Signal.ABORT;
        }

        actor.take(itemEntity, taken);

        var remaining = itemEntity.getItem().copy();
        remaining.shrink(taken);

        if (remaining.isEmpty()) {
            itemEntity.discard();
        } else {
            itemEntity.setItem(remaining);
        }

        return Action.Signal.CONTINUE;
    }

    private PickUpNearbyItemAction() {
        throw new UnsupportedOperationException();
    }
}
