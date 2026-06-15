package com.human.common.gameplay.entity.living.human.marine.ai.break_fall;

import com.blib.api.common.goap.v1.GOAPSensors;
import com.just.ai.goap.condition.expression.Expressions;
import com.just.ai.goap.goal.Goal;

public class BreakFallGoals {

    /**
     * Goal to break a fall using a water bucket. The marine will equip a water bucket and place water at the landing
     * position when falling from a height that would cause fall damage.
     */
    public static final Goal BREAK_FALL_GOAL = Goal.builder("BreakFallGoal")
        .addPrecondition(BreakFallSensors.SHOULD_BREAK_FALL.key(), Expressions.Boolean.isTrue())
        .addDesiredCondition(GOAPSensors.IS_ON_GROUND.key().asDerived(), Expressions.Boolean.isTrue())
        .build();

    private BreakFallGoals() {
        throw new UnsupportedOperationException();
    }
}
