package com.human.common.gameplay.entity.living.human.marine.ai.sentry;

import com.blib.api.common.goap.v1.action.ActionMasks;
import com.blib.api.common.goap.v1.action.BLibAction;
import com.human.common.gameplay.entity.living.human.marine.Marine;
import com.human.common.gameplay.entity.living.human.marine.ai.fall_back.FallBackSensors;
import com.human.common.gameplay.entity.living.human.marine.ai.sentry.action.ReturnToPostAction;
import com.just.ai.goap.action.Action;
import com.just.ai.goap.condition.expression.Expressions;

public class SentryActions {

    /**
     * Walks a strayed sentry back to the point it was posted at.
     * <p>
     * Both conditions are runtime as well as plan-time: standing the sentry down or starting a withdrawal drops a
     * return already in progress rather than leaving the marine walking home through a firefight.
     */
    public static final Action<Marine> RETURN_TO_POST = BLibAction.<Marine>builder("ReturnToPostAction")
        .addMasks(ActionMasks.MOVE)
        .addPrecondition(SentrySensors.IS_SENTRY.key(), Expressions.Boolean.isTrue())
        .addPrecondition(FallBackSensors.IS_FALLING_BACK.key(), Expressions.Boolean.isFalse())
        .addEffect(SentrySensors.IS_AT_POST.key().asDerived(), true)
        .withPerformCallback(ReturnToPostAction::perform)
        .withFinishCallback(ReturnToPostAction::onFinish)
        .build();

    private SentryActions() {
        throw new UnsupportedOperationException();
    }
}
