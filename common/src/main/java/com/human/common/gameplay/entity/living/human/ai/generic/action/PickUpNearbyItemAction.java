package com.human.common.gameplay.entity.living.human.ai.generic.action;

import com.blib.api.common.goap.v1.action.impl.PickUpItemAction;
import com.blib.api.common.inventory.v1.BLibInventoryHolder;
import com.just.ai.goap.StateKey;
import com.just.ai.goap.action.Action;
import com.just.core.functional.option.Option;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;

import java.util.function.Function;

public final class PickUpNearbyItemAction {

    public static <T extends LivingEntity & BLibInventoryHolder, V> Action.Signal perform(
        Action.Context<T> context,
        StateKey<Option<V>> sensorKey,
        Function<V, ItemEntity> itemEntityExtractor
    ) {
        var livingEntityWithInventory = context.getActor();
        var worldState = context.getWorldState();
        var valueOption = worldState.getOrDefault(sensorKey, Option.none());

        if (valueOption.isNone()) {
            return Action.Signal.ABORT;
        }

        return PickUpItemAction.perform(livingEntityWithInventory, itemEntityExtractor.apply(valueOption.unwrap()));
    }

    private PickUpNearbyItemAction() {
        throw new UnsupportedOperationException();
    }
}
