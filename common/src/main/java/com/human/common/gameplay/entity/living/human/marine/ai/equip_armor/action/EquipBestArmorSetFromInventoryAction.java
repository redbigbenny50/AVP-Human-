package com.human.common.gameplay.entity.living.human.marine.ai.equip_armor.action;

import com.blib.api.common.goap.v1.action.impl.EquipItemAction;
import com.blib.api.common.inventory.v1.BLibInventory;
import com.human.common.gameplay.entity.living.human.ai.model.ArmorSetTarget;
import com.human.common.gameplay.entity.living.human.ai.model.ItemTarget;
import com.human.common.gameplay.entity.living.human.marine.Marine;
import com.human.common.gameplay.entity.living.human.marine.ai.equip_armor.EquipArmorSensors;
import com.just.ai.goap.action.Action;
import net.minecraft.world.entity.EquipmentSlot;

public class EquipBestArmorSetFromInventoryAction {

    public static Action.Signal perform(Action.Context<? extends Marine> context) {
        var marine = context.getActor();
        var worldState = context.getWorldState();
        var bestArmorTarget = worldState.getOrDefault(EquipArmorSensors.BEST_ARMOR_SET_TARGET.key(), ArmorSetTarget.EMPTY);

        if (bestArmorTarget.isEmpty()) {
            return Action.Signal.ABORT;
        }

        if (bestArmorTarget.helmet() instanceof ItemTarget.Inventory(BLibInventory.Entry entry)) {
            EquipItemAction.perform(marine, entry, EquipmentSlot.HEAD);
        }

        if (bestArmorTarget.chestplate() instanceof ItemTarget.Inventory(BLibInventory.Entry entry)) {
            EquipItemAction.perform(marine, entry, EquipmentSlot.CHEST);
        }

        if (bestArmorTarget.leggings() instanceof ItemTarget.Inventory(BLibInventory.Entry entry)) {
            EquipItemAction.perform(marine, entry, EquipmentSlot.LEGS);
        }

        if (bestArmorTarget.boots() instanceof ItemTarget.Inventory(BLibInventory.Entry entry)) {
            EquipItemAction.perform(marine, entry, EquipmentSlot.FEET);
        }

        return Action.Signal.CONTINUE;
    }

    private EquipBestArmorSetFromInventoryAction() {
        throw new UnsupportedOperationException();
    }
}
