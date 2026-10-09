package com.human.common.gameplay.entity.living.human.marine.ai.cut_free;

import com.just.ai.goap.condition.expression.Expressions;
import com.just.ai.goap.goal.Goal;

public class CutFreeGoals {

    /**
     * Free a netted squadmate. Only ever live while one is in sight and this marine has a blade to cut with; satisfied
     * the moment there is no netted marine nearby, which is what the cut itself produces.
     */
    public static final Goal CUT_FREE_NETTED_MARINE_GOAL = Goal.builder("CutFreeNettedMarineGoal")
        .addPrecondition(CutFreeSensors.HAS_NETTED_MARINE_NEARBY.key(), Expressions.Boolean.isTrue())
        .addPrecondition(CutFreeSensors.HAS_BLADE_IN_INVENTORY.key(), Expressions.Boolean.isTrue())
        .addDesiredCondition(CutFreeSensors.HAS_NETTED_MARINE_NEARBY.key().asDerived(), Expressions.Boolean.isFalse())
        .build();

    private CutFreeGoals() {
        throw new UnsupportedOperationException();
    }
}
