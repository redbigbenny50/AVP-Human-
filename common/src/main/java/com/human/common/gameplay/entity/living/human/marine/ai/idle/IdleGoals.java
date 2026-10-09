package com.human.common.gameplay.entity.living.human.marine.ai.idle;

import com.just.ai.goap.condition.expression.Expressions;
import com.just.ai.goap.goal.Goal;

public class IdleGoals {

    public static final Goal SATISFY_BOREDOM_GOAL = Goal.builder("SatisfyBoredomGoal")
        .addPrecondition(IdleSensors.IS_BORED.key(), Expressions.Boolean.isTrue())
        .addDesiredCondition(IdleSensors.IS_BORED.key().asDerived(), Expressions.Boolean.isFalse())
        .build();

    /**
     * [stated] Oct 4: an idle marine in water gets out of it - heads for the nearest shore and waits there. Only when
     * idle: no player leader, nothing to fight.
     */
    public static final Goal GET_OUT_OF_WATER_GOAL = Goal.builder("GetOutOfWaterGoal")
        .addPrecondition(IdleSensors.IS_IN_WATER.key(), Expressions.Boolean.isTrue())
        .addDesiredCondition(IdleSensors.IS_IN_WATER.key().asDerived(), Expressions.Boolean.isFalse())
        .build();

    /** [stated] Oct 5: an idle camp marine that has strayed walks back to its camp. */
    public static final Goal RETURN_HOME_GOAL = Goal.builder("ReturnHomeGoal")
        .addPrecondition(IdleSensors.IS_AWAY_FROM_HOME.key(), Expressions.Boolean.isTrue())
        .addDesiredCondition(IdleSensors.IS_AWAY_FROM_HOME.key().asDerived(), Expressions.Boolean.isFalse())
        .build();

    private IdleGoals() {
        throw new UnsupportedOperationException();
    }
}
