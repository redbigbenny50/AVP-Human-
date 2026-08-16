package com.human.common.gameplay.entity.living.human.marine.ai.standoff;

import com.blib.api.common.goap.v1.GOAPSensors;
import com.blib.api.common.goap.v1.action.ActionMasks;
import com.blib.api.common.goap.v1.action.BLibAction;
import com.human.common.gameplay.entity.living.human.marine.Marine;
import com.human.common.gameplay.entity.living.human.marine.ai.standoff.action.MaintainStandoffAction;
import com.just.ai.goap.action.Action;
import com.just.ai.goap.condition.expression.Expressions;

public class StandoffActions {

    /**
     * Steps back out of a closing enemy's reach without stopping the attack.
     * <p>
     * MOVE is the only mask taken - swinging and shooting belong to the combat plan and must keep running alongside.
     * Having a target is a runtime precondition, so the step-back ends by itself the moment the target dies.
     */
    public static final Action<Marine> MAINTAIN_STANDOFF = BLibAction.<Marine>builder("MaintainStandoffAction")
        .addMasks(ActionMasks.MOVE)
        .addPrecondition(StandoffSensors.SHOULD_MAINTAIN_STANDOFF.key(), Expressions.Boolean.isTrue())
        .addPrecondition(GOAPSensors.HAS_ATTACK_TARGET.key(), Expressions.Boolean.isTrue())
        .addEffect(StandoffSensors.IS_AT_STANDOFF_DISTANCE.key().asDerived(), true)
        .withPerformCallback(MaintainStandoffAction::perform)
        .withFinishCallback(MaintainStandoffAction::onFinish)
        .build();

    private StandoffActions() {
        throw new UnsupportedOperationException();
    }
}
