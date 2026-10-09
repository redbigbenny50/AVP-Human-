package com.human.common.gameplay.entity.living.human.marine.ai.acquire_water_breathing.action;

import com.human.common.gameplay.entity.living.human.marine.ai.acquire_water_breathing.WBISensors;
import com.just.ai.goap.action.Action;
import com.just.core.functional.option.Option;
import net.minecraft.world.entity.LivingEntity;

public class UseWBIAction {

    public static Action.Signal perform(Action.Context<? extends LivingEntity> context) {
        var worldState = context.getWorldState();
        var itemTargetOption = worldState.getOrDefault(WBISensors.BEST_WBI.key(), Option.none());

        if (itemTargetOption.isNone()) {
            return Action.Signal.ABORT;
        }

        return itemTargetOption.unwrap().strategy().execute(context);
    }

    private UseWBIAction() {
        throw new UnsupportedOperationException();
    }
}
