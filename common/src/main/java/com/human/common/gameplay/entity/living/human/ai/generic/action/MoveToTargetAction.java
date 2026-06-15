package com.human.common.gameplay.entity.living.human.ai.generic.action;

import com.blib.api.common.goap.v1.action.impl.MoveToPosAction;
import com.just.ai.goap.StateKey;
import com.just.ai.goap.action.Action;
import com.just.core.functional.option.Option;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.phys.Vec3;

import java.util.function.Function;

public final class MoveToTargetAction {

    public static <V> Action.Signal perform(
        Action.Context<? extends PathfinderMob> context,
        StateKey<Option<V>> sensorKey,
        Function<V, Vec3> positionExtractor
    ) {
        var pathfinderMob = context.getActor();
        var worldState = context.getWorldState();
        var valueOption = worldState.getOrDefault(sensorKey, Option.none());

        if (valueOption.isNone()) {
            return Action.Signal.ABORT;
        }

        var position = positionExtractor.apply(valueOption.unwrap());

        return switch (MoveToPosAction.perform(context, position, 1)) {
            case FINISHED, MOVING -> Action.Signal.CONTINUE;
            case NO_PATH -> Action.Signal.ABORT;
        };
    }

    public static void onFinish(Action.Context<? extends PathfinderMob> context) {
        MoveToPosAction.onFinish(context);
    }

    private MoveToTargetAction() {
        throw new UnsupportedOperationException();
    }
}
