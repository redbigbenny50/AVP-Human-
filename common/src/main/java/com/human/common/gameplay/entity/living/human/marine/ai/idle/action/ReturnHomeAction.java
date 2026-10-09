package com.human.common.gameplay.entity.living.human.marine.ai.idle.action;

import com.blib.api.common.goap.v1.action.impl.MoveToPosAction;
import com.human.common.gameplay.entity.living.human.marine.Marine;
import com.just.ai.goap.action.Action;

/**
 * An idle camp marine that has strayed walks back to its camp. [stated] Oct 5: "if its an option yes otherwise wait on
 * shore" - a marine stranded in water gets out at the nearest shore first (GetOutOfWaterAction), then this brings it
 * home. A marine with no home never runs this (its precondition), so it waits where it is, as before.
 */
public class ReturnHomeAction {

    public static Action.Signal perform(Action.Context<? extends Marine> context) {
        var marine = context.getActor();
        var home = marine.getCampHome();

        if (home == null) {
            return Action.Signal.ABORT;
        }

        // The effect (back within range) ends the action. A failed path counts toward the marine's rest: after a few
        // in a row it stops trying for a while (Marine.homePathFailed), which ends this action through its sensor.
        switch (MoveToPosAction.perform(context, net.minecraft.world.phys.Vec3.atBottomCenterOf(home), 1.0D)) {
            case MOVING -> marine.homePathFound();
            case NO_PATH -> marine.homePathFailed();
            case FINISHED -> {}
        }

        return Action.Signal.CONTINUE;
    }

    public static void onFinish(Action.Context<? extends Marine> context) {
        MoveToPosAction.onFinish(context);
    }

    private ReturnHomeAction() {
        throw new UnsupportedOperationException();
    }
}
