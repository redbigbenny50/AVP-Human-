package com.human.common.gameplay.entity.living.human.marine.ai.heal_self.action;

import com.human.common.gameplay.entity.living.human.marine.ai.heal_self.HealingSensors;
import com.just.ai.goap.action.Action;
import com.just.core.functional.option.Option;
import net.minecraft.world.entity.LivingEntity;

public class UseHealingItemAction {

    public static Action.Signal perform(Action.Context<? extends LivingEntity> context) {
        var worldState = context.getWorldState();
        var itemTargetOption = worldState.getOrDefault(HealingSensors.BEST_HEALING_ITEM.key(), Option.none());

        if (itemTargetOption.isNone()) {
            return Action.Signal.ABORT;
        }

        return itemTargetOption.unwrap().strategy().execute(context);
    }

    private UseHealingItemAction() {
        throw new UnsupportedOperationException();
    }
}
