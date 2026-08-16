package com.human.common.gameplay.entity.living.human.marine.ai.fall_back;

import com.blib.api.common.goap.v1.GOAPSensors;
import com.blib.api.common.goap.v1.action.ActionMasks;
import com.blib.api.common.goap.v1.action.BLibAction;
import com.human.common.gameplay.entity.living.human.marine.Marine;
import com.human.common.gameplay.entity.living.human.marine.ai.fall_back.action.FallBackFromAttackTargetAction;
import com.just.ai.goap.action.Action;
import com.just.ai.goap.condition.expression.Expressions;

public class FallBackActions {

    /**
     * Backs away from whatever is charging while the gun runs dry.
     * <p>
     * MOVE is the only mask taken: firing belongs to the combat plan and must keep running alongside this one.
     * <p>
     * The "is it charging" test is a PLAN-TIME precondition only. As a runtime one it would unsatisfy itself the moment
     * the marine crossed the six-block charge distance, killing the plan at six blocks instead of the eight it is
     * trying to reach. Being low on ammunition and having a target are runtime preconditions, so the retreat ends by
     * itself when the reload completes or the attacker dies.
     */
    public static final Action<Marine> FALL_BACK_FROM_ATTACK_TARGET = BLibAction.<Marine>builder("FallBackFromAttackTargetAction")
        .addMasks(ActionMasks.MOVE)
        .addPlanTimePrecondition(FallBackSensors.IS_ATTACK_TARGET_CHARGING.key(), Expressions.Boolean.isTrue())
        .addPrecondition(FallBackSensors.IS_LOW_ON_AMMUNITION.key(), Expressions.Boolean.isTrue())
        .addPrecondition(GOAPSensors.HAS_ATTACK_TARGET.key(), Expressions.Boolean.isTrue())
        .addEffect(FallBackSensors.IS_AT_SAFE_DISTANCE.key().asDerived(), true)
        .withPerformCallback(FallBackFromAttackTargetAction::perform)
        .withFinishCallback(FallBackFromAttackTargetAction::onFinish)
        .build();

    private FallBackActions() {
        throw new UnsupportedOperationException();
    }
}
