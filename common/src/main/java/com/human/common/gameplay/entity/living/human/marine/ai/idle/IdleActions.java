package com.human.common.gameplay.entity.living.human.marine.ai.idle;

import com.blib.api.common.goap.v1.GOAPSensors;
import com.blib.api.common.goap.v1.action.ActionMasks;
import com.blib.api.common.goap.v1.action.BLibAction;
import com.human.common.gameplay.entity.living.human.marine.Marine;
import com.human.common.gameplay.entity.living.human.marine.ai.follow_leader.FollowLeaderSensors;
import com.human.common.gameplay.entity.living.human.marine.ai.idle.action.GetOutOfWaterAction;
import com.human.common.gameplay.entity.living.human.marine.ai.idle.action.ReturnHomeAction;
import com.human.common.gameplay.entity.living.human.marine.ai.idle.action.WanderAction;
import com.just.ai.goap.action.Action;
import com.just.ai.goap.condition.expression.Expressions;

public class IdleActions {

    public static final Action<Marine> WANDER_ACTION = BLibAction.<Marine>builder("WanderAction")
        .addMasks(ActionMasks.MOVE)
        .addPrecondition(FollowLeaderSensors.HAS_PLAYER_LEADER.key(), Expressions.Boolean.isFalse())
        .addPrecondition(GOAPSensors.HAS_ATTACK_TARGET.key(), Expressions.Boolean.isFalse())
        .addPrecondition(IdleSensors.IS_BORED.key(), Expressions.Boolean.isTrue())
        // [stated] Oct 4: never wander from water - it only finds land spots, failed every second, and spun the marine.
        // GET_OUT_OF_WATER_ACTION takes over there.
        .addPrecondition(IdleSensors.IS_IN_WATER.key(), Expressions.Boolean.isFalse())
        // [stated] Oct 5: a camp marine out of range goes home first rather than wandering further off.
        .addPrecondition(IdleSensors.IS_AWAY_FROM_HOME.key(), Expressions.Boolean.isFalse())
        .addEffect(IdleSensors.IS_BORED.key().asDerived(), false)
        .withPerformCallback(WanderAction::perform)
        .withFinishCallback(WanderAction::onFinish)
        .build();

    /** [stated] Oct 4: an idle marine in water heads for the nearest shore and waits there (or floats calmly). */
    public static final Action<Marine> GET_OUT_OF_WATER_ACTION = BLibAction.<Marine>builder("GetOutOfWaterAction")
        .addMasks(ActionMasks.MOVE)
        .addPrecondition(FollowLeaderSensors.HAS_PLAYER_LEADER.key(), Expressions.Boolean.isFalse())
        .addPrecondition(IdleSensors.IS_CLEAR_TO_LEAVE_WATER.key(), Expressions.Boolean.isTrue())
        .addPrecondition(IdleSensors.IS_IN_WATER.key(), Expressions.Boolean.isTrue())
        .addEffect(IdleSensors.IS_IN_WATER.key().asDerived(), false)
        .withPerformCallback(GetOutOfWaterAction::perform)
        .withFinishCallback(GetOutOfWaterAction::onFinish)
        .build();

    /** [stated] Oct 5: an idle camp marine out of range walks back to its camp. Out of water first (see above). */
    public static final Action<Marine> RETURN_HOME_ACTION = BLibAction.<Marine>builder("ReturnHomeAction")
        .addMasks(ActionMasks.MOVE)
        .addPrecondition(FollowLeaderSensors.HAS_PLAYER_LEADER.key(), Expressions.Boolean.isFalse())
        .addPrecondition(IdleSensors.IS_CLEAR_TO_GO_HOME.key(), Expressions.Boolean.isTrue())
        .addPrecondition(IdleSensors.IS_IN_WATER.key(), Expressions.Boolean.isFalse())
        .addPrecondition(IdleSensors.IS_AWAY_FROM_HOME.key(), Expressions.Boolean.isTrue())
        .addEffect(IdleSensors.IS_AWAY_FROM_HOME.key().asDerived(), false)
        .withPerformCallback(ReturnHomeAction::perform)
        .withFinishCallback(ReturnHomeAction::onFinish)
        .build();

    private IdleActions() {
        throw new UnsupportedOperationException();
    }
}
