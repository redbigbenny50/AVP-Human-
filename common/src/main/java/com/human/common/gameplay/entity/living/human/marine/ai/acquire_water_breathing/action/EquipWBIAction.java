package com.human.common.gameplay.entity.living.human.marine.ai.acquire_water_breathing.action;

import com.blib.api.common.goap.v1.action.impl.EquipItemAction;
import com.blib.api.common.inventory.v1.BLibInventoryHolder;
import com.human.common.gameplay.entity.living.human.marine.ai.acquire_water_breathing.WBISensors;
import com.just.ai.goap.action.Action;
import com.just.core.functional.option.Option;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;

public class EquipWBIAction {

    public static <T extends LivingEntity & BLibInventoryHolder> Action.Signal perform(Action.Context<T> context) {
        var livingEntityWithInventory = context.getActor();
        var worldState = context.getWorldState();
        var inventoryItemTargetOption = worldState.getOrDefault(WBISensors.BEST_WBI_IN_INVENTORY.key(), Option.none());

        if (inventoryItemTargetOption.isNone()) {
            return Action.Signal.ABORT;
        }

        return EquipItemAction.perform(
            livingEntityWithInventory,
            inventoryItemTargetOption.unwrap().itemTarget().entry(),
            EquipmentSlot.MAINHAND
        );
    }

    private EquipWBIAction() {
        throw new UnsupportedOperationException();
    }
}
