package com.human.common.gameplay.entity.living.human.marine.ai.place_torch;

import com.just.ai.goap.condition.expression.Expressions;
import com.just.ai.goap.goal.Goal;

public class TorchGoals {

    /**
     * Goal to place a torch when in a dark area. The marine will equip a torch and place it either at their feet or on
     * a nearby wall.
     */
    public static final Goal LIGHT_DARK_AREA_GOAL = Goal.builder("LightDarkAreaGoal")
        .addPrecondition(TorchSensors.SHOULD_PLACE_TORCH.key(), Expressions.Boolean.isTrue())
        .addDesiredCondition(TorchSensors.IS_IN_DARK_AREA.key().asDerived(), Expressions.Boolean.isFalse())
        .build();

    /**
     * Goal to opportunistically collect torches when available in the world. Marines will collect up to a stack of
     * torches to have ready for dark areas.
     */
    public static final Goal COLLECT_TORCHES_GOAL = Goal.builder("CollectTorchesGoal")
        .addPrecondition(TorchSensors.SHOULD_COLLECT_MORE_TORCHES.key(), Expressions.Boolean.isTrue())
        .addDesiredCondition(TorchSensors.HAS_TORCH_IN_INVENTORY.key().asDerived(), Expressions.Boolean.isTrue())
        .build();

    private TorchGoals() {
        throw new UnsupportedOperationException();
    }
}
