package com.human.common.gameplay.entity.living.human.marine.ai.follow_leader;

import com.blib.api.common.goap.v1.action.ActionMasks;
import com.blib.api.common.goap.v1.action.BLibAction;
import com.human.common.gameplay.entity.living.human.marine.Marine;
import com.human.common.gameplay.entity.living.human.marine.ai.follow_leader.action.MoveCloserToLeaderAction;
import com.just.ai.goap.action.Action;
import com.just.ai.goap.condition.expression.Expressions;

public class FollowLeaderActions {

    public static final Action<Marine> MOVE_CLOSER_TO_LEADER_ACTION = BLibAction.<Marine>builder("MoveCloserToLeaderAction")
        .addMasks(ActionMasks.MOVE)
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
