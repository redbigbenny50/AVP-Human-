package com.human.common.gameplay.entity.living.human.marine.ai.acquire_fire_resistance.action;

import com.human.common.gameplay.entity.living.human.marine.ai.acquire_fire_resistance.FRISensors;
import com.just.ai.goap.action.Action;
import com.just.core.functional.option.Option;
import net.minecraft.world.entity.LivingEntity;

public class UseFRIAction {

    public static Action.Signal perform(Action.Context<? extends LivingEntity> context) {
        var worldState = context.getWorldState();
        var itemTargetOption = worldState.getOrDefault(FRISensors.BEST_FRI.key(), Option.none());

        if (itemTargetOption.isNone()) {
            return Action.Signal.ABORT;
        }

        return itemTargetOption.unwrap().strategy().execute(context);
    }

    private UseFRIAction() {
        throw new UnsupportedOperationException();
    }
}
