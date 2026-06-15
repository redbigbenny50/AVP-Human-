package com.human.common.gameplay.entity.living.human.marine.ai.combat.action;

import com.human.common.gameplay.entity.living.human.marine.ai.combat.CombatSensors;
import com.just.ai.goap.action.Action;
import com.just.core.functional.option.Option;
import net.minecraft.world.entity.LivingEntity;

public class UseWeaponAction {

    public static Action.Signal perform(Action.Context<? extends LivingEntity> context) {
        var worldState = context.getWorldState();
        var itemTargetOption = worldState.getOrDefault(CombatSensors.BEST_WEAPON.key(), Option.none());

        if (itemTargetOption.isNone()) {
            return Action.Signal.ABORT;
        }

        return itemTargetOption.unwrap().strategy().execute(context);
    }

    private UseWeaponAction() {
        throw new UnsupportedOperationException();
    }
}
