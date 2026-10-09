package com.human.common.gameplay.entity.living.human.marine.ai.follow_leader.action;

import com.blib.api.common.goap.v1.action.impl.MoveToPosAction;
import com.human.common.gameplay.entity.living.human.AbstractHuman;
import com.human.common.gameplay.entity.living.human.marine.Marine;
import com.just.ai.goap.action.Action;
import com.just.ai.goap.state.Blackboard;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;

public class MoveCloserToLeaderAction {

    public static void onStart(Action.Context<? extends Marine> context) {
        var marine = context.getActor();
        // Free water while following, so a marine goes straight through after its leader. [stated] Oct 4: put back by
        // Marine.tick once this stops running - the GOAP library skips onFinish for an interrupted plan, so a
        // save-and-restore here could leave water free for good.
        marine.markFollowingLeader();
        marine.setPathfindingMalus(PathType.WATER, 0.0F);
    }

    public static Action.Signal perform(Action.Context<? extends Marine> context) {
        var marine = context.getActor();
        marine.markFollowingLeader();
        var leaderOrNull = marine.getLeader().unwrapOr(null);

        if (leaderOrNull == null) {
            return Action.Signal.ABORT;
        }

        if (marine.distanceToSqr(leaderOrNull) >= 144.0) {
            teleportToAroundBlockPos(marine, leaderOrNull.blockPosition());
            return Action.Signal.CONTINUE;
        }

        // Oct 5 - tactic 10: walk to this marine's place in the wedge behind the leader, or to the leader when it has
        // none (no squad, or no standable ground there).
        var slot = com.human.common.gameplay.entity.living.human.marine.squad.MarineSquads.formationSlot(marine, leaderOrNull);
        var destination = slot != null ? slot : leaderOrNull.position();

        var result = MoveToPosAction.perform(context, destination, 1);

        // ⚠ Bug pass 3 - an unreachable formation place must not stop the marine following at all: it used to abort and
        // retry the same unreachable spot forever while the leader walked off. Fall back on the leader himself.
        // (MoveToPosAction has just set its repath cooldown, so a second perform() this tick would not path at all -
        // ask
        // the navigation directly.)
        if (result == MoveToPosAction.Result.NO_PATH && slot != null) {
            var leaderPos = leaderOrNull.position();
            result = context.getActor().getNavigation().moveTo(leaderPos.x, leaderPos.y, leaderPos.z, 1)
                ? MoveToPosAction.Result.MOVING
                : MoveToPosAction.Result.NO_PATH;
        }

        return switch (result) {
            case FINISHED, MOVING -> Action.Signal.CONTINUE;
            case NO_PATH -> Action.Signal.ABORT;
        };
    }

    public static void onFinish(Action.Context<? extends Marine> context) {
        var marine = context.getActor();
        var blackboard = context.getBlackboard(Blackboard.Scope.ACTION);
        MoveToPosAction.onFinish(context);
        marine.setPathfindingMalus(PathType.WATER, AbstractHuman.DEFAULT_WATER_MALUS);
    }

    private static void teleportToAroundBlockPos(Marine marine, BlockPos pos) {
        for (var i = 0; i < 10; ++i) {
            var j = marine.getRandom().nextIntBetweenInclusive(-3, 3);
            var k = marine.getRandom().nextIntBetweenInclusive(-3, 3);

            if (Math.abs(j) >= 2 || Math.abs(k) >= 2) {
                var l = marine.getRandom().nextIntBetweenInclusive(-1, 1);

                if (maybeTeleportTo(marine, pos.getX() + j, pos.getY() + l, pos.getZ() + k)) {
                    return;
                }
            }
        }
    }

    private static boolean maybeTeleportTo(Marine marine, int x, int y, int z) {
        if (!canTeleportTo(marine, new BlockPos(x, y, z))) {
            return false;
        } else {
            marine.moveTo((double) x + 0.5, y, (double) z + 0.5, marine.getYRot(), marine.getXRot());
            marine.getNavigation().stop();
            return true;
        }
    }

    private static boolean canTeleportTo(Marine marine, BlockPos pos) {
        var pathType = WalkNodeEvaluator.getPathTypeStatic(marine, pos);

        if (pathType != PathType.WALKABLE) {
            return false;
        } else {
            var blockPos = pos.subtract(marine.blockPosition());
            return marine.level().noCollision(marine, marine.getBoundingBox().move(blockPos));
        }
    }

    private MoveCloserToLeaderAction() {
        throw new UnsupportedOperationException();
    }
}
