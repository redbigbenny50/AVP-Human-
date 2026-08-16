package com.human.common.gameplay.entity.living.human.marine.ai.sentry.action;

import com.blib.api.common.goap.v1.action.impl.MoveToPosAction;
import com.human.common.gameplay.entity.living.human.marine.Marine;
import com.just.ai.goap.action.Action;
import net.minecraft.world.phys.Vec3;

public class ReturnToPostAction {

    /**
     * Walked rather than run. A sentry returning to its post is not in a hurry, and an ordinary walk reads as a marine
     * going back to its position instead of fleeing to it.
     */
    private static final double RETURN_SPEED_MODIFIER = 1.0;

    public static Action.Signal perform(Action.Context<? extends Marine> context) {
        var marine = context.getActor();
        var sentryPosOrNull = marine.getSentryPos().unwrapOr(null);

        if (sentryPosOrNull == null) {
            return Action.Signal.ABORT;
        }

        var target = new Vec3(sentryPosOrNull.getX() + 0.5, sentryPosOrNull.getY(), sentryPosOrNull.getZ() + 0.5);

        return switch (MoveToPosAction.perform(context, target, RETURN_SPEED_MODIFIER)) {
            case FINISHED, MOVING -> Action.Signal.CONTINUE;
            // Walled off from its own post - the world changed, or it was posted somewhere it can no longer reach.
            // Aborting hands the decision back to the planner rather than pathing at it forever.
            case NO_PATH -> Action.Signal.ABORT;
        };
    }

    public static void onFinish(Action.Context<? extends Marine> context) {
        MoveToPosAction.onFinish(context);
    }

    private ReturnToPostAction() {
        throw new UnsupportedOperationException();
    }
}
