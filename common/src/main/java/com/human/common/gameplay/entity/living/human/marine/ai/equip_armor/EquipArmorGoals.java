package com.human.common.gameplay.entity.living.human.marine.ai.equip_armor;

import com.just.ai.goap.condition.expression.Expressions;
import com.just.ai.goap.goal.Goal;

public class EquipArmorGoals {

    public static final Goal EQUIP_BEST_ARMOR_GOAL = Goal.builder("EquipBestArmorGoal")
        .addDesiredCondition(EquipArmorSensors.ARE_ALL_BEST_ARMOR_SET_PIECES_EQUIPPED.key().asDerived(), Expressions.Boolean.isTrue())
        .build();

    private EquipArmorGoals() {
        throw new UnsupportedOperationException();
    }
}
