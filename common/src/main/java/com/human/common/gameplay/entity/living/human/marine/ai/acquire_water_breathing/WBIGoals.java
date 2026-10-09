package com.human.common.gameplay.entity.living.human.marine.ai.acquire_water_breathing;

import com.blib.api.common.goap.v1.GOAPSensors;
import com.just.ai.goap.condition.expression.Expressions;
import com.just.ai.goap.goal.Goal;

public class WBIGoals {

    /** Live only while the marine needs air it cannot make for itself; satisfied by the Water Breathing effect. */
    public static final Goal ACQUIRE_WATER_BREATHING_GOAL = Goal.builder("AcquireWaterBreathingGoal")
        .addPrecondition(WBISensors.WANTS_WATER_BREATHING.key(), Expressions.Boolean.isTrue())
        .addDesiredCondition(GOAPSensors.HAS_WATER_BREATHING.key().asDerived(), Expressions.Boolean.isTrue())
        .build();

    private WBIGoals() {
        throw new UnsupportedOperationException();
    }
}
