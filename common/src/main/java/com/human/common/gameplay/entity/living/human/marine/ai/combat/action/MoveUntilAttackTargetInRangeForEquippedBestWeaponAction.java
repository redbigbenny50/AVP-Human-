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

    /** The tick a fresh path may next be computed (a moving target, or after a failed search). */
    private static final StateKey<Integer> NEXT_PATH_TICK = StateKey.sensed("next_path_tick");

    /** How often the path is refreshed while chasing - a target moves, and the old path leads to where it WAS. */
    private static final int REPATH_INTERVAL_TICKS = 20;

    /**
     * [stated] Oct 5 performance review. How long to wait after a failed search. It used to retry EVERY tick: a null
     * path was stored, read back as "no path yet", and searched again - twenty full pathfinds a second, per marine, for
     * as long as an unreachable target (a cave mob, something across water) stayed nearest.
     */
    private static final int FAILED_PATH_COOLDOWN_TICKS = 30;

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

        if (pathfinderMob instanceof com.human.common.gameplay.entity.living.human.marine.Marine marine) {
            // ⚠⚠ Pre-merge review - A FALLING-BACK SQUAD DOES NOT CHASE. This action re-issues its path to the target
            // every tick, so a retreating marine whose target dropped out of range was dragged straight back toward
            // it, undoing the retreat move each time. While the squad falls back it holds the target and fires when in
            // range, but leaves moving to the retreat (Marine.tickFightingRetreat). Sentries and HOLD marines do not
            // retreat, so they keep their normal rules.
            if (
                com.human.common.gameplay.entity.living.human.marine.squad.MarineSquads.retreatPoint(marine) != null
                    && !marine.isSentry()
                    && marine.getMode() != com.human.common.gameplay.entity.living.human.marine.MarineMode.HOLD
            ) {
                return Action.Signal.CONTINUE;
            }

            // ⚠⚠ Oct 5 audit - GIVE UP ON WHAT CANNOT BE REACHED. This action used to return CONTINUE forever with no
            // path: a knife against a mob on a ledge, a gunner out of range across a ravine - frozen for good, because
            // the same target stayed nearest. Five seconds without getting closer and the target is set aside for ten.
            if (marine.recordChaseAndCheckGiveUp(attackTarget)) {
                pathfinderMob.getNavigation().stop();
                return Action.Signal.ABORT;
            }

            // ⚠ Oct 5 audit - A SENTRY'S LEASH HOLDS IN A FIGHT TOO. It used to apply only when idle, so a sentry
            // chased
            // a target as far as it ran and then walked all the way back. At the leash edge it now stops and fights
            // from there; if the target stays out of reach, the give-up above releases it and the sentry returns.
            if (isAtSentryLeashEdge(marine, attackTarget)) {
                pathfinderMob.getNavigation().stop();
                return Action.Signal.CONTINUE;
            }
        }

        var pathToAttackTargetOrNull = blackboard.getOrNull(PATH_TO_ATTACK_TARGET);
        var now = pathfinderMob.tickCount;
        var nextPathTick = blackboard.getOrDefault(NEXT_PATH_TICK, now);
        var wantsPath = pathToAttackTargetOrNull == null || pathToAttackTargetOrNull.isDone() || now >= nextPathTick;

        if (wantsPath && now >= nextPathTick) {
            pathToAttackTargetOrNull = pathfinderMob.getNavigation().createPath(attackTarget, 1);
            blackboard.set(PATH_TO_ATTACK_TARGET, pathToAttackTargetOrNull);
            blackboard.set(
                NEXT_PATH_TICK,
                now + (pathToAttackTargetOrNull == null ? FAILED_PATH_COOLDOWN_TICKS : REPATH_INTERVAL_TICKS)
            );
        }

        if (pathToAttackTargetOrNull != null) {
            pathfinderMob.getNavigation().moveTo(pathToAttackTargetOrNull, 1.0);
        }

        return Action.Signal.CONTINUE;
    }

    private static boolean isAtSentryLeashEdge(
        com.human.common.gameplay.entity.living.human.marine.Marine marine,
        net.minecraft.world.entity.LivingEntity target
    ) {
        if (!marine.isSentry() || marine.getSentryPos().isNone()) {
            return false;
        }

        var post = net.minecraft.world.phys.Vec3.atBottomCenterOf(marine.getSentryPos().unwrap());
        var leash = com.human.common.gameplay.entity.living.human.marine.ai.sentry.SentrySensors.SENTRY_LEASH_IN_BLOCKS;
        var leashSqr = leash * leash;

        // At or past the edge, and the target is further out still: going on would only drag the sentry off its post.
        return marine.distanceToSqr(post) >= leashSqr && target.distanceToSqr(post) > marine.distanceToSqr(post);
    }

    public static void onFinish(Action.Context<? extends PathfinderMob> context) {
        var pathfinderMob = context.getActor();
        pathfinderMob.getNavigation().stop();
    }

    private MoveUntilAttackTargetInRangeForEquippedBestWeaponAction() {
        throw new UnsupportedOperationException();
    }
}
