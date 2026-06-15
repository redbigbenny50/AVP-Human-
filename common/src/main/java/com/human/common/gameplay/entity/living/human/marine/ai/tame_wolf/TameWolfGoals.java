package com.human.common.gameplay.entity.living.human.marine.ai.tame_wolf;

import com.just.ai.goap.condition.expression.Expressions;
import com.just.ai.goap.goal.Goal;

public class TameWolfGoals {

    /**
     * Goal to tame nearby wolves. Only activates when there is an untamed wolf nearby and the marine has bones
     * available (either in inventory or in the world).
     */
    public static final Goal TAME_WOLF_GOAL = Goal.builder("TameWolfGoal")
        .addPrecondition(TameWolfSensors.HAS_UNTAMED_WOLF_NEARBY.key(), Expressions.Boolean.isTrue())
        .addDesiredCondition(TameWolfSensors.HAS_UNTAMED_WOLF_NEARBY.key().asDerived(), Expressions.Boolean.isFalse())
        .build();

    /**
     * Goal to opportunistically collect bones when no wolf is nearby. Marines will collect up to a stack of bones to
     * have ready for future wolf encounters.
     */
    public static final Goal COLLECT_BONES_GOAL = Goal.builder("CollectBonesGoal")
        .addPrecondition(TameWolfSensors.SHOULD_COLLECT_MORE_BONES.key(), Expressions.Boolean.isTrue())
        .addDesiredCondition(TameWolfSensors.HAS_BONE_IN_INVENTORY.key().asDerived(), Expressions.Boolean.isTrue())
        .build();

    private TameWolfGoals() {
        throw new UnsupportedOperationException();
    }
}
