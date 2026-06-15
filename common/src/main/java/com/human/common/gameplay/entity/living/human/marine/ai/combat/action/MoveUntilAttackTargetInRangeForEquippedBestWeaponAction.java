package com.human.common.gameplay.entity.living.human.marine.ai.combat.action;

import com.blib.api.common.goap.v1.GOAPSensors;
import com.human.common.gameplay.entity.living.human.marine.ai.combat.CombatSensors;
import com.just.ai.goap.StateKey;
import com.just.ai.goap.action.Action;
import com.just.ai.goap.state.Blackboard;
import com.just.core.functional.option.Option;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.pathfinder.Path;

public class MoveUntilAttackTargetInRangeForEquippedBestWeaponAction {

    private static final StateKey<Path> PATH_TO_ATTACK_TARGET = StateKey.sensed("path_to_attack_target");

    public static Action.Signal perform(Action.Context<? extends PathfinderMob> context) {
        var pathfinderMob = context.getActor();
        var worldState = context.getWorldState();
        var blackboard = context.getBlackboard(Blackboard.Scope.ACTION);
        var weaponStrategyResultOption = worldState.getOrDefault(CombatSensors.BEST_WEAPON_IN_HANDS.key(), Option.none());

        var attackTargetOption = worldState.getOrDefault(GOAPSensors.NEAREST_ATTACKABLE_TARGET.key(), Option.none());

        if (weaponStrategyResultOption.isNone() || attackTargetOption.isNone()) {
            return Action.Signal.ABORT;
        }

        var attackTarget = attackTargetOption.unwrap();
        var pathToAttackTargetOrNull = blackboard.getOrNull(PATH_TO_ATTACK_TARGET);

        if (pathToAttackTargetOrNull == null || pathToAttackTargetOrNull.isDone()) {
            pathToAttackTargetOrNull = pathfinderMob.getNavigation().createPath(attackTarget, 1);
            blackboard.set(PATH_TO_ATTACK_TARGET, pathToAttackTargetOrNull);
        }

        if (pathToAttackTargetOrNull != null) {
            pathfinderMob.getNavigation().moveTo(pathToAttackTargetOrNull, 1.0);
        }

        return Action.Signal.CONTINUE;
    }

    public static void onFinish(Action.Context<? extends PathfinderMob> context) {
        var pathfinderMob = context.getActor();
        pathfinderMob.getNavigation().stop();
    }

    private MoveUntilAttackTargetInRangeForEquippedBestWeaponAction() {
        throw new UnsupportedOperationException();
    }
}
