package com.human.common.gameplay.entity.living.human.marine.ai.extinguish_fire;

import com.blib.api.common.goap.v1.GOAPSensors;
import com.just.ai.goap.condition.expression.Expressions;
import com.just.ai.goap.goal.Goal;

public class ExtinguishFireGoals {

    public static final Goal EXTINGUISH_SELF_GOAL = Goal.builder("ExtinguishSelfGoal")
        .addPrecondition(GOAPSensors.HAS_FIRE_RESISTANCE.key(), Expressions.Boolean.isFalse())
        .addDesiredCondition(GOAPSensors.IS_ON_FIRE.key().asDerived(), Expressions.Boolean.isFalse())
        .build();

    private ExtinguishFireGoals() {
        throw new UnsupportedOperationException();
    }
}
