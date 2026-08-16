package com.human.common.gameplay.entity.living.human.marine.ai.follow_leader;

import com.human.common.gameplay.entity.living.human.marine.ai.fall_back.FallBackSensors;
import com.human.common.gameplay.entity.living.human.marine.ai.standoff.StandoffSensors;
import com.just.ai.goap.condition.expression.Expressions;
import com.just.ai.goap.goal.Goal;

public class FollowLeaderGoals {

    /**
     * ⚠ The fall-back exclusion is load-bearing, not tidiness.
     * <p>
     * Following claims the MOVE mask, and so does a withdrawal, so BLib's resolver lets only one of them run and
     * rejects whichever asked second. "Close to the leader" is under three blocks, which a marine in a firefight is
     * almost never inside - so the follow plan finishes and re-plans constantly, and the two behaviours end up trading
     * the mask tick by tick. The marine is pulled toward its leader and pushed away from its attacker in alternating
     * bursts: fast movement, no ground gained. Making them mutually exclusive by condition rather than by race is the
     * fix.
     */
    public static final Goal STAY_CLOSE_TO_LEADER_GOAL = Goal.builder("StayCloseToLeaderGoal")
        .addPrecondition(FallBackSensors.IS_FALLING_BACK.key(), Expressions.Boolean.isFalse())
        // ⚠ Same reason as the line above: holding fighting distance also claims MOVE, and two plans trading
        // that mask tick by tick is exactly what made marines run side to side instead of anywhere.
        .addPrecondition(StandoffSensors.SHOULD_MAINTAIN_STANDOFF.key(), Expressions.Boolean.isFalse())
        .addPrecondition(FollowLeaderSensors.IS_CLOSE_TO_LEADER.key(), Expressions.Boolean.isFalse())
        .addDesiredCondition(FollowLeaderSensors.IS_CLOSE_TO_LEADER.key().asDerived(), Expressions.Boolean.isTrue())
        .build();

    private FollowLeaderGoals() {
        throw new UnsupportedOperationException();
    }
}
