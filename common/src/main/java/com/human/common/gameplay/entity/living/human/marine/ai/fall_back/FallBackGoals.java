package com.human.common.gameplay.entity.living.human.marine.ai.fall_back;

import com.just.ai.goap.condition.expression.Expressions;
import com.just.ai.goap.goal.Goal;

public class FallBackGoals {

    /**
     * Give ground while the magazine is nearly out and something is charging.
     * <p>
     * A separate goal from the combat one on purpose. Its only action holds MOVE, and with the target already in range
     * the combat plan is the single USE_BEST_WEAPON action holding LOOK and USE_MAIN_HAND - disjoint mask sets, so
     * BLib's ActionMaskPlanResolver lets both plans run at once and the marine keeps firing while it backs up.
     */
    public static final Goal FALL_BACK_GOAL = Goal.builder("FallBackGoal")
        .addPrecondition(FallBackSensors.SHOULD_FALL_BACK.key(), Expressions.Boolean.isTrue())
        .addDesiredCondition(FallBackSensors.IS_AT_SAFE_DISTANCE.key().asDerived(), Expressions.Boolean.isTrue())
        .build();

    private FallBackGoals() {
        throw new UnsupportedOperationException();
    }
}
