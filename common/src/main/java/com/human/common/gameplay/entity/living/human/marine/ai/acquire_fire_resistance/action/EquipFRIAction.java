package com.human.common.gameplay.entity.living.human.marine.ai.acquire_fire_resistance.action;

import com.blib.api.common.goap.v1.action.impl.EquipItemAction;
import com.blib.api.common.inventory.v1.BLibInventoryHolder;
import com.human.common.gameplay.entity.living.human.marine.ai.acquire_fire_resistance.FRISensors;
import com.just.ai.goap.action.Action;
import com.just.core.functional.option.Option;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;

public class EquipFRIAction {

    public static <T extends LivingEntity & BLibInventoryHolder> Action.Signal perform(Action.Context<T> context) {
        var livingEntityWithInventory = context.getActor();
        var worldState = context.getWorldState();
        var inventoryItemTargetOption = worldState.getOrDefault(FRISensors.BEST_FRI_IN_INVENTORY.key(), Option.none());

        if (inventoryItemTargetOption.isNone()) {
            return Action.Signal.ABORT;
        }

        return EquipItemAction.perform(
            livingEntityWithInventory,
            inventoryItemTargetOption.unwrap().itemTarget().entry(),
            EquipmentSlot.MAINHAND
        );
    }

    private EquipFRIAction() {
        throw new UnsupportedOperationException();
    }
}
