package com.human.common.gameplay.entity.living.human.marine.ai.equip_totem;

import com.just.ai.goap.condition.expression.Expressions;
import com.just.ai.goap.goal.Goal;

public class TotemGoals {

    /**
     * Goal to equip a totem of undying when near death (≤10% health). The marine will move to pick up a totem from the
     * world if necessary, then equip it to their offhand.
     */
    public static final Goal EQUIP_TOTEM_WHEN_NEAR_DEATH_GOAL = Goal.builder("EquipTotemWhenNearDeathGoal")
        .addPrecondition(TotemSensors.SHOULD_EQUIP_TOTEM.key(), Expressions.Boolean.isTrue())
        .addDesiredCondition(TotemSensors.HAS_TOTEM_EQUIPPED.key().asDerived(), Expressions.Boolean.isTrue())
        .build();

    /**
     * Goal to always keep at least one totem of undying in inventory. Marines will pick up totems from the world
     * whenever they have fewer than the minimum required.
     */
    public static final Goal COLLECT_TOTEM_GOAL = Goal.builder("CollectTotemGoal")
        .addPrecondition(TotemSensors.SHOULD_COLLECT_TOTEM.key(), Expressions.Boolean.isTrue())
        .addDesiredCondition(TotemSensors.HAS_TOTEM_IN_INVENTORY.key().asDerived(), Expressions.Boolean.isTrue())
        .build();

    private TotemGoals() {
        throw new UnsupportedOperationException();
    }
}
