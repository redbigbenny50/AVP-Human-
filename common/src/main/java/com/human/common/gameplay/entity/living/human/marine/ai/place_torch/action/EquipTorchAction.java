package com.human.common.gameplay.entity.living.human.marine.ai.place_torch.action;

import com.blib.api.common.goap.v1.action.impl.EquipItemAction;
import com.blib.api.common.inventory.v1.BLibInventoryHolder;
import com.just.ai.goap.action.Action;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Items;

public class EquipTorchAction {

    public static <T extends LivingEntity & BLibInventoryHolder> Action.Signal perform(Action.Context<T> context) {
        var livingEntityWithInventory = context.getActor();
        var inventory = livingEntityWithInventory.getInventory();

        var torchEntryOption = inventory.selectEntries(Items.TORCH)
            .stream()
            .findFirst();

        if (torchEntryOption.isEmpty()) {
            return Action.Signal.ABORT;
        }

        return EquipItemAction.perform(livingEntityWithInventory, torchEntryOption.get(), EquipmentSlot.OFFHAND);
    }

    private EquipTorchAction() {
        throw new UnsupportedOperationException();
    }
}
