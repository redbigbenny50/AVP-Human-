package com.human.common.gameplay.entity.living.human.marine.ai.break_fall;

import com.blib.api.common.goap.v1.GOAPSensors;
import com.blib.api.common.goap.v1.action.ActionMasks;
import com.blib.api.common.goap.v1.action.BLibAction;
import com.human.common.gameplay.entity.living.human.marine.Marine;
import com.human.common.gameplay.entity.living.human.marine.ai.MarineGOAPSensors;
import com.human.common.gameplay.entity.living.human.marine.ai.break_fall.action.PlaceWaterToBreakFallAction;
import com.human.common.gameplay.entity.living.human.marine.ai.extinguish_fire.ExtinguishFireSensors;
import com.just.ai.goap.action.Action;
import com.just.ai.goap.condition.expression.Expressions;

public class BreakFallActions {

    public static final Action<Marine> PLACE_WATER_TO_BREAK_FALL = BLibAction.<Marine>builder("PlaceWaterToBreakFallAction")
        .addMasks(ActionMasks.USE_MAIN_HAND)
        .addPlanTimePrecondition(ExtinguishFireSensors.HAS_WATER_BUCKET_EQUIPPED.key(), Expressions.Boolean.isTrue())
        .addPlanTimePrecondition(BreakFallSensors.CAN_PLACE_WATER_AT_LANDING.key(), Expressions.Boolean.isTrue())
        .addPrecondition(BreakFallSensors.IS_FALLING.key(), Expressions.Boolean.isTrue())
        .addPrecondition(BreakFallSensors.IS_CLOSE_TO_LANDING.key(), Expressions.Boolean.isTrue())
        .addPrecondition(MarineGOAPSensors.IS_IN_ULTRA_WARM_DIMENSION.key(), Expressions.Boolean.isFalse())
        .addEffect(GOAPSensors.IS_ON_GROUND.key().asDerived(), true)
        .addEffect(BreakFallSensors.WILL_TAKE_FALL_DAMAGE.key().asDerived(), false)
        .withPerformCallback(PlaceWaterToBreakFallAction::perform)
        .withFinishCallback(PlaceWaterToBreakFallAction::onFinish)
        .build();

    private BreakFallActions() {
        throw new UnsupportedOperationException();
    }
}
