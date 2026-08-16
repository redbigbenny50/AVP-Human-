package com.human.common.gameplay.entity.living.human.marine.ai.standoff;

import com.just.ai.goap.condition.expression.Expressions;
import com.just.ai.goap.goal.Goal;

public class StandoffGoals {

    /**
     * Keep fighting distance from whatever is being fought.
     * <p>
     * A separate goal from the combat one, for the same reason the withdrawal is: its only action holds MOVE, and with
     * the target already in range the combat plan is the single USE_BEST_WEAPON action holding LOOK and USE_MAIN_HAND.
     * Disjoint masks, so both plans run at once - the marine keeps swinging or firing while it gives ground, which is
     * the entire behaviour being asked for.
     */
    public static final Goal MAINTAIN_STANDOFF_GOAL = Goal.builder("MaintainStandoffGoal")
        .addPrecondition(StandoffSensors.SHOULD_MAINTAIN_STANDOFF.key(), Expressions.Boolean.isTrue())
        .addDesiredCondition(StandoffSensors.IS_AT_STANDOFF_DISTANCE.key().asDerived(), Expressions.Boolean.isTrue())
        .build();

    private StandoffGoals() {
        throw new UnsupportedOperationException();
    }
}
