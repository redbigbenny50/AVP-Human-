package com.human.common.gameplay.entity.living.human.marine.ai.collect_ammo;

import com.just.ai.goap.condition.expression.Expressions;
import com.just.ai.goap.goal.Goal;

/** Oct 5 - collect dropped ammunition the marine can use (see AmmoSensors for when). */
public final class AmmoGoals {

    /** Leave no usable ammunition lying within reach. */
    public static final Goal COLLECT_AMMO_GOAL = Goal.builder("CollectAmmoGoal")
        .addPrecondition(AmmoSensors.SHOULD_COLLECT_AMMO.key(), Expressions.Boolean.isTrue())
        .addDesiredCondition(AmmoSensors.HAS_USABLE_AMMO_IN_WORLD.key().asDerived(), Expressions.Boolean.isFalse())
        .build();

    private AmmoGoals() {
        throw new UnsupportedOperationException();
    }
}
