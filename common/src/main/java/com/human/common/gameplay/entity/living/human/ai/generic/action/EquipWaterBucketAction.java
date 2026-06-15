package com.human.common.gameplay.entity.living.human.ai.generic.action;

import com.blib.api.common.goap.v1.action.impl.EquipItemAction;
import com.blib.api.common.inventory.v1.BLibInventoryHolder;
import com.human.common.gameplay.entity.living.human.marine.ai.extinguish_fire.ExtinguishFireSensors;
import com.just.ai.goap.action.Action;
import com.just.core.functional.option.Option;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;

public class EquipWaterBucketAction {

    public static <T extends LivingEntity & BLibInventoryHolder> Action.Signal perform(Action.Context<T> context) {
        var livingEntityWithInventory = context.getActor();
        var worldState = context.getWorldState();
        var inventoryWaterBucketOption = worldState.getOrDefault(ExtinguishFireSensors.WATER_BUCKET_IN_INVENTORY.key(), Option.none());

        if (inventoryWaterBucketOption.isNone()) {
            return Action.Signal.ABORT;
        }

        return EquipItemAction.perform(livingEntityWithInventory, inventoryWaterBucketOption.unwrap(), EquipmentSlot.MAINHAND);
    }

    private EquipWaterBucketAction() {
        throw new UnsupportedOperationException();
    }
}
