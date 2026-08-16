package com.human.common.gameplay.entity.living.human.marine.ai.fall_back.action;

import com.blib.api.common.goap.v1.GOAPSensors;
import com.blib.api.common.goap.v1.action.impl.MoveToPosAction;
import com.human.common.gameplay.entity.living.human.marine.Marine;
import com.human.common.gameplay.entity.living.human.marine.MarineMoveControl;
import com.just.ai.goap.StateKey;
import com.just.ai.goap.action.Action;
import com.just.ai.goap.state.Blackboard;
import com.just.core.functional.option.Option;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import net.minecraft.world.phys.Vec3;

/**
 * Gives ground while the magazine runs out.
 * <p>
 * Two movement modes, tried in that order:
 * <ol>
 * <li>A BACKPEDAL. The marine keeps facing wherever it is aiming and moves along the away-vector using strafe inputs,
 * the same way a player holding S retreats while keeping the crosshair on a target. Nothing here touches rotation, so
 * the aim {@code GunStrategy} sets each tick survives and the gun points at what it is shooting.</li>
 * <li>A PATHFOUND RETREAT, when the ground directly behind will not take a backpedal. This turns the body and runs,
 * which looks worse and fires worse, but it routes around obstacles.</li>
 * </ol>
 * The distinction matters because MOVE_TO turns the body toward its waypoint every tick and derives movement from that
 * yaw. Used while firing it fights {@code GunStrategy}'s per-tick {@code lookAt} - the marine ends up crabbing sideways
 * at roughly ninety degrees to both the target and the retreat point. Strafing has no such conflict.
 */
public class FallBackFromAttackTargetAction {

    private static final StateKey<Vec3> RETREAT_POSITION = StateKey.sensed("retreat_position");

    private static final StateKey<Integer> FAILED_RETREAT_ATTEMPTS = StateKey.sensed("failed_retreat_attempts");

    /**
     * How far out to look for somewhere to run to when backpedalling is not an option. Larger than the eight blocks of
     * separation being asked for, because the search is a random one within ninety degrees of straight-away and rarely
     * lands on the exact bearing.
     */
    private static final int RETREAT_SEARCH_RADIUS_IN_BLOCKS = 12;

    private static final int RETREAT_SEARCH_Y_RANGE_IN_BLOCKS = 5;

    /**
     * Slightly faster than a walk. A marine giving ground is not strolling, but sprinting away reads as a rout.
     */
    private static final double RETREAT_SPEED_MODIFIER = 1.15;

    /**
     * How far behind the marine to test the footing before committing to a backpedal.
     * <p>
     * This test is ours rather than the one inside {@link net.minecraft.world.entity.ai.control.MoveControl}, because
     * vanilla's probe is scaled by the movement speed and so barely leaves the block the mob is standing in - and its
     * response to a blocked strafe is to walk FORWARD, which here would march the marine into the thing chasing it.
     */
    private static final double FOOTING_PROBE_DISTANCE_IN_BLOCKS = 1.5;

    /**
     * Distance at which the current retreat position counts as reached and a fresh one is picked.
     */
    private static final double ARRIVAL_DISTANCE_IN_BLOCKS = 2.0;

    /**
     * Consecutive failures to move anywhere before the marine gives up on retreating. Cornered, standing and fighting
     * beats shuffling on the spot - and aborting hands the decision back to the planner rather than burning ticks.
     */
    private static final int MAXIMUM_FAILED_ATTEMPTS = 3;

    public static Action.Signal perform(Action.Context<? extends Marine> context) {
        var marine = context.getActor();
        var worldState = context.getWorldState();
        var blackboard = context.getBlackboard(Blackboard.Scope.ACTION);
        var attackTargetOption = worldState.getOrDefault(GOAPSensors.NEAREST_ATTACKABLE_TARGET.key(), Option.none());

        if (attackTargetOption.isNone()) {
            return Action.Signal.ABORT;
        }

        var attackTarget = (LivingEntity) attackTargetOption.unwrap();
        var awayDirectionOrNull = awayDirectionOrNull(marine, attackTarget);

        if (awayDirectionOrNull == null) {
            // Standing on top of each other. There is no meaningful direction to give ground in.
            return registerFailedAttempt(blackboard);
        }

        if (tryBackpedal(marine, awayDirectionOrNull)) {
            blackboard.set(FAILED_RETREAT_ATTEMPTS, 0);

            return Action.Signal.CONTINUE;
        }

        return runForIt(context, marine, attackTarget, blackboard);
    }

    public static void onFinish(Action.Context<? extends Marine> context) {
        MoveToPosAction.onFinish(context);
    }

    /**
     * Moves along the away-vector without altering the marine's facing.
     * <p>
     * The away-vector is world space and the strafe inputs are relative to the body, so it is resolved against the
     * current yaw rather than assuming the marine is already facing the target. Backing straight up is then just the
     * common case of that, not a special one: whatever the marine happens to be looking at, it still moves away.
     *
     * @return whether the backpedal was taken.
     */
    private static boolean tryBackpedal(Marine marine, Vec3 awayDirection) {
        if (!(marine.getMoveControl() instanceof MarineMoveControl moveControl) || !hasFooting(marine, awayDirection)) {
            return false;
        }

        // Navigation would set a wanted position later this tick and put the move control back into MOVE_TO, undoing
        // the strafe before it is ever consumed.
        marine.getNavigation().stop();

        var yawInRadians = marine.getYRot() * Mth.DEG_TO_RAD;
        var sinYaw = Mth.sin(yawInRadians);
        var cosYaw = Mth.cos(yawInRadians);
        var forwardInput = (float) (awayDirection.z * cosYaw - awayDirection.x * sinYaw);
        var strafeInput = (float) (awayDirection.x * cosYaw + awayDirection.z * sinYaw);

        moveControl.strafe(forwardInput, strafeInput, RETREAT_SPEED_MODIFIER);

        return true;
    }

    /**
     * The pathfound fallback: vanilla's own flee search, the one AvoidEntityGoal uses. It picks a random spot within
     * ninety degrees of directly away from the target and rejects anything the navigation cannot stand on or that
     * carries a pathfinding malus, so ledges, lava and fire are screened out before a path is ever requested.
     */
    private static Action.Signal runForIt(
        Action.Context<? extends Marine> context,
        Marine marine,
        LivingEntity attackTarget,
        Blackboard blackboard
    ) {
        var retreatPositionOrNull = blackboard.getOrNull(RETREAT_POSITION);

        if (retreatPositionOrNull == null || shouldPickNewRetreatPosition(marine, attackTarget, retreatPositionOrNull)) {
            retreatPositionOrNull = DefaultRandomPos.getPosAway(
                marine,
                RETREAT_SEARCH_RADIUS_IN_BLOCKS,
                RETREAT_SEARCH_Y_RANGE_IN_BLOCKS,
                attackTarget.position()
            );

            if (retreatPositionOrNull == null) {
                return registerFailedAttempt(blackboard);
            }

            blackboard.set(RETREAT_POSITION, retreatPositionOrNull);
        }

        return switch (MoveToPosAction.perform(context, retreatPositionOrNull, RETREAT_SPEED_MODIFIER)) {
            case FINISHED, MOVING -> {
                blackboard.set(FAILED_RETREAT_ATTEMPTS, 0);

                yield Action.Signal.CONTINUE;
            }
            // Somewhere unreachable was chosen. Park the retreat position on top of the marine so the arrival test
            // forces a fresh pick next tick; MoveToPosAction has already pushed its own recalculation delay out.
            case NO_PATH -> {
                blackboard.set(RETREAT_POSITION, marine.position());

                yield registerFailedAttempt(blackboard);
            }
        };
    }

    /**
     * The horizontal unit vector pointing from the target to the marine, or null when the two are effectively in the
     * same place.
     */
    private static Vec3 awayDirectionOrNull(Marine marine, LivingEntity attackTarget) {
        var offset = marine.position().subtract(attackTarget.position());
        var horizontalOffset = new Vec3(offset.x, 0.0, offset.z);

        return horizontalOffset.lengthSqr() < 1.0E-4
            ? null
            : horizontalOffset.normalize();
    }

    private static boolean hasFooting(Marine marine, Vec3 awayDirection) {
        var probe = marine.position().add(awayDirection.scale(FOOTING_PROBE_DISTANCE_IN_BLOCKS));
        var probePos = BlockPos.containing(probe.x, marine.getY(), probe.z);

        return WalkNodeEvaluator.getPathTypeStatic(marine, probePos) == PathType.WALKABLE;
    }

    /**
     * A stored retreat position stops being useful either once it is reached, or once the target has chased past it -
     * running to a spot no further from the attacker than the marine already is would be worse than standing still.
     */
    private static boolean shouldPickNewRetreatPosition(Marine marine, LivingEntity attackTarget, Vec3 retreatPosition) {
        if (marine.distanceToSqr(retreatPosition) <= ARRIVAL_DISTANCE_IN_BLOCKS * ARRIVAL_DISTANCE_IN_BLOCKS) {
            return true;
        }

        return attackTarget.distanceToSqr(retreatPosition) <= attackTarget.distanceToSqr(marine.position());
    }

    private static Action.Signal registerFailedAttempt(Blackboard blackboard) {
        var failedAttempts = blackboard.getOrDefault(FAILED_RETREAT_ATTEMPTS, 0) + 1;

        blackboard.set(FAILED_RETREAT_ATTEMPTS, failedAttempts);

        return failedAttempts >= MAXIMUM_FAILED_ATTEMPTS
            ? Action.Signal.ABORT
            : Action.Signal.CONTINUE;
    }

    private FallBackFromAttackTargetAction() {
        throw new UnsupportedOperationException();
    }
}
