package com.human.common.gameplay.entity.living.human.marine.ai.heal_self;

import com.blib.api.common.goap.v1.GOAPSensors;
import com.human.common.gameplay.entity.living.human.marine.ai.heal_self.strategy.HealingStrategyUtil;
import com.just.ai.goap.condition.expression.Expressions;
import com.just.ai.goap.goal.Goal;

public class HealingGoals {

    public static final Goal HEAL_SELF_GOAL = Goal.builder("HealSelfGoal")
        .addPrecondition(GOAPSensors.HEALTH_RATIO.key(), Expressions.Compare.lessThan(HealingStrategyUtil.HEAL_THRESHOLD))
        .addDesiredCondition(GOAPSensors.HEALTH_RATIO.key().asDerived(), Expressions.Compare.equalTo(1.0F))
        .build();
}
