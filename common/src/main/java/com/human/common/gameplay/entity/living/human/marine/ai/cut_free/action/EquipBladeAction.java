package com.human.common.gameplay.entity.living.human.marine.ai.cut_free.action;

import com.blib.api.common.goap.v1.action.impl.EquipItemAction;
import com.human.common.gameplay.entity.living.human.marine.Marine;
import com.human.common.gameplay.entity.living.human.marine.ai.cut_free.CutFreeSensors;
import com.human.common.registry.tag.HumanItemTags;
import com.just.ai.goap.action.Action;
import net.minecraft.world.entity.EquipmentSlot;

public class EquipBladeAction {

    /** The combat knife is preferred; any other blade only if the marine has no knife. */
    public static Action.Signal perform(Action.Context<? extends Marine> context) {
        var marine = context.getActor();
        var inventory = marine.getInventory();

        var knives = inventory.filterEntriesByStack(itemStack -> itemStack.is(HumanItemTags.DAGGERS));
        var blades = knives.isEmpty() ? inventory.filterEntriesByStack(CutFreeSensors::isBlade) : knives;

        if (blades.isEmpty()) {
            return Action.Signal.ABORT;
        }

        return EquipItemAction.perform(marine, blades.getFirst(), EquipmentSlot.MAINHAND);
    }

    private EquipBladeAction() {
        throw new UnsupportedOperationException();
    }
}
