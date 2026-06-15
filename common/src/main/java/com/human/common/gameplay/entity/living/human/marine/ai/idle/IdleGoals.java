package com.human.common.gameplay.entity.living.human.marine.ai.idle;

import com.just.ai.goap.condition.expression.Expressions;
import com.just.ai.goap.goal.Goal;

public class IdleGoals {

    public static final Goal SATISFY_BOREDOM_GOAL = Goal.builder("SatisfyBoredomGoal")
        .addPrecondition(IdleSensors.IS_BORED.key(), Expressions.Boolean.isTrue())
        .addDesiredCondition(IdleSensors.IS_BORED.key().asDerived(), Expressions.Boolean.isFalse())
        .build();

    private IdleGoals() {
        throw new UnsupportedOperationException();
    }
}
