package com.human.common.gameplay.entity.living.human.marine.ai.sentry;

import com.human.common.gameplay.entity.living.human.marine.ai.fall_back.FallBackSensors;
import com.just.ai.goap.condition.expression.Expressions;
import com.just.ai.goap.goal.Goal;

public class SentryGoals {

    /**
     * Come back to the guarded point after straying too far.
     * <p>
     * ⚠ The fall-back exclusion is the same one follow-leader needed, and for the same reason: returning to post claims
     * the MOVE mask and so does a withdrawal, so without this the two would trade it tick by tick and the marine would
     * jitter between them instead of giving ground. A sentry that is retreating is allowed off its leash until the
     * withdrawal ends; it walks back afterwards.
     */
    public static final Goal RETURN_TO_POST_GOAL = Goal.builder("ReturnToPostGoal")
        .addPrecondition(SentrySensors.IS_AWAY_FROM_POST.key(), Expressions.Boolean.isTrue())
        .addPrecondition(FallBackSensors.IS_FALLING_BACK.key(), Expressions.Boolean.isFalse())
        .addDesiredCondition(SentrySensors.IS_AT_POST.key().asDerived(), Expressions.Boolean.isTrue())
        .build();

    private SentryGoals() {
        throw new UnsupportedOperationException();
    }
}
