package com.human.common.gameplay.entity.living.human.marine.ai.follow_leader;

import com.just.ai.goap.condition.expression.Expressions;
import com.just.ai.goap.goal.Goal;

public class FollowLeaderGoals {

    public static final Goal STAY_CLOSE_TO_LEADER_GOAL = Goal.builder("StayCloseToLeaderGoal")
        .addPrecondition(FollowLeaderSensors.IS_CLOSE_TO_LEADER.key(), Expressions.Boolean.isFalse())
        .addDesiredCondition(FollowLeaderSensors.IS_CLOSE_TO_LEADER.key().asDerived(), Expressions.Boolean.isTrue())
        .build();

    private FollowLeaderGoals() {
        throw new UnsupportedOperationException();
    }
}
