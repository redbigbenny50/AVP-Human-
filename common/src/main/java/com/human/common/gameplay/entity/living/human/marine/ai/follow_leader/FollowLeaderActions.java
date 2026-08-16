package com.human.common.gameplay.entity.living.human.marine.ai.follow_leader;

import com.blib.api.common.goap.v1.action.ActionMasks;
import com.blib.api.common.goap.v1.action.BLibAction;
import com.human.common.gameplay.entity.living.human.marine.Marine;
import com.human.common.gameplay.entity.living.human.marine.ai.fall_back.FallBackSensors;
import com.human.common.gameplay.entity.living.human.marine.ai.follow_leader.action.MoveCloserToLeaderAction;
import com.human.common.gameplay.entity.living.human.marine.ai.standoff.StandoffSensors;
import com.just.ai.goap.action.Action;
import com.just.ai.goap.condition.expression.Expressions;

public class FollowLeaderActions {

    public static final Action<Marine> MOVE_CLOSER_TO_LEADER_ACTION = BLibAction.<Marine>builder("MoveCloserToLeaderAction")
        .addMasks(ActionMasks.MOVE)
        // A RUNTIME precondition, so a follow already in progress is dropped the instant the marine starts giving
        // ground. The goal precondition above only governs whether a new one gets planned.
        .addPrecondition(FallBackSensors.IS_FALLING_BACK.key(), Expressions.Boolean.isFalse())
        // ⚠ Same reason as the line above: holding fighting distance also claims MOVE, and two plans trading
        // that mask tick by tick is exactly what made marines run side to side instead of anywhere.
        .addPrecondition(StandoffSensors.SHOULD_MAINTAIN_STANDOFF.key(), Expressions.Boolean.isFalse())
        .addPrecondition(FollowLeaderSensors.CAN_FOLLOW_LEADER.key(), Expressions.Boolean.isTrue())
        .addPrecondition(FollowLeaderSensors.IS_CLOSE_TO_LEADER.key(), Expressions.Boolean.isFalse())
        .addEffect(FollowLeaderSensors.IS_CLOSE_TO_LEADER.key().asDerived(), true)
        .withStartCallback(MoveCloserToLeaderAction::onStart)
        .withPerformCallback(MoveCloserToLeaderAction::perform)
        .withFinishCallback(MoveCloserToLeaderAction::onFinish)
        .build();

    private FollowLeaderActions() {
        throw new UnsupportedOperationException();
    }
}
