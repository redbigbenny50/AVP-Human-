package com.human.common.gameplay.entity.living.human.marine.ai.idle.action;

import com.blib.api.common.goap.v1.action.impl.MoveToPosAction;
import com.human.common.gameplay.entity.living.human.marine.Marine;
import com.just.ai.goap.StateKey;
import com.just.ai.goap.action.Action;
import com.just.ai.goap.state.Blackboard;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.phys.Vec3;

public class WanderAction {

    private static final StateKey<Vec3> TARGET_POS = StateKey.sensed("target_pos");

    public static Action.Signal perform(Action.Context<? extends Marine> context) {
        var marine = context.getActor();
        var blackboard = context.getBlackboard(Blackboard.Scope.ACTION);
        var targetPosOrNull = blackboard.getOrNull(TARGET_POS);

        if (targetPosOrNull == null) {
            targetPosOrNull = LandRandomPos.getPos(marine, 10, 7);

            if (targetPosOrNull == null) {
                return Action.Signal.ABORT;
            }

            blackboard.set(TARGET_POS, targetPosOrNull);
        }

        return switch (MoveToPosAction.perform(context, targetPosOrNull, 0.8D)) {
            case FINISHED -> {
                marine.resetTicksUntilBored();
                yield Action.Signal.CONTINUE;
            }
            case MOVING -> Action.Signal.CONTINUE;
            case NO_PATH -> Action.Signal.ABORT;
        };
    }

    public static void onFinish(Action.Context<? extends Marine> context) {
        MoveToPosAction.onFinish(context);
    }

    private WanderAction() {
        throw new UnsupportedOperationException();
    }
}
