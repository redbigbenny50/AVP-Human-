package com.human.common.gameplay.entity.living.human.marine.ai.standoff.action;

import com.human.common.gameplay.entity.living.human.marine.Marine;
import com.human.common.gameplay.entity.living.human.marine.MarineMoveControl;
import com.human.common.gameplay.entity.living.human.marine.ai.standoff.StandoffSensors;
import com.just.ai.goap.action.Action;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import net.minecraft.world.phys.Vec3;

/**
 * Backing off just far enough to keep fighting from outside the enemy's reach.
 * <p>
 * This is what a player does without thinking about it: hit, give ground as the thing closes, hit again. A marine that
 * plants its feet at the edge of its own reach and lets a zombie walk into contact is trading blow for blow with
 * something that has more health than it does, and losing.
 * <p>
 * Movement is a STRAFE, never a path. Strafing is the one mode that leaves {@code yRot} alone, so the marine gives
 * ground while still facing - and still shooting or swinging at - what it is fighting. A path would turn it around.
 */
public class MaintainStandoffAction {

    /**
     * Unhurried. The marine is holding distance while it fights, not fleeing, and a full sprint backwards would open
     * the range further than the weapon wants and read as a rout.
     */
    private static final double STANDOFF_SPEED_MODIFIER = 0.8;

    /**
     * How far ahead the ground is checked before committing to a step.
     */
    private static final double FOOTING_PROBE_DISTANCE_IN_BLOCKS = 1.5;

    /**
     * ⚠⚠ THE DROP GUARD. How far the ground behind may fall away and still be worth stepping onto.
     * <p>
     * One block is a step a marine walks back up without thinking. Two is a scramble. Anything past that is a ledge,
     * and a marine that backs off a cliff while concentrating on a zombie has done something far worse to itself than
     * the zombie was going to. So the probe walks DOWN from the intended position looking for real footing within this
     * many blocks, and if it does not find any the marine stands its ground instead.
     * <p>
     * Standing and fighting is the correct failure: it is what the marine would have done anyway before this behaviour
     * existed, so a cornered or cliff-edge marine simply loses the improvement rather than gaining a hazard.
     */
    private static final int MAXIMUM_SAFE_DROP_IN_BLOCKS = 1;

    public static Action.Signal perform(Action.Context<? extends Marine> context) {
        var marine = context.getActor();
        var attackTarget = StandoffSensors.computeStandoffOrNull(marine, context.getWorldState(), 0.0);

        if (attackTarget == null) {
            // Already at distance, or nothing to hold distance from. The effect is satisfied and the plan completes.
            return Action.Signal.CONTINUE;
        }

        var awayDirection = computeAwayDirectionOrNull(marine, attackTarget);

        if (awayDirection == null) {
            return Action.Signal.CONTINUE;
        }

        tryStepBack(marine, awayDirection);

        return Action.Signal.CONTINUE;
    }

    public static void onFinish(Action.Context<? extends Marine> context) {
        var marine = context.getActor();

        marine.setZza(0.0F);
        marine.setXxa(0.0F);
    }

    /**
     * @return whether the step was taken. False means the ground behind failed the check and the marine holds position.
     */
    private static boolean tryStepBack(Marine marine, Vec3 awayDirection) {
        if (!(marine.getMoveControl() instanceof MarineMoveControl moveControl) || !hasSafeFooting(marine, awayDirection)) {
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

        moveControl.strafe(forwardInput, strafeInput, STANDOFF_SPEED_MODIFIER);

        return true;
    }

    /**
     * Whether there is ground behind the marine it can step onto and get back from.
     * <p>
     * ⚠ {@code getPathTypeStatic} returns WALKABLE only when the probe block is open AND the block beneath it is solid,
     * so a level step passes on the first test. When it does not, the probe walks downward: finding solid footing one
     * block down is a step, and anything deeper is refused. It also screens lava and fire for free, since those return
     * their own path types rather than WALKABLE.
     */
    private static boolean hasSafeFooting(Marine marine, Vec3 awayDirection) {
        var probe = marine.position().add(awayDirection.scale(FOOTING_PROBE_DISTANCE_IN_BLOCKS));
        var probePos = BlockPos.containing(probe.x, marine.getY(), probe.z);

        for (var dropInBlocks = 0; dropInBlocks <= MAXIMUM_SAFE_DROP_IN_BLOCKS; dropInBlocks++) {
            if (WalkNodeEvaluator.getPathTypeStatic(marine, probePos.below(dropInBlocks)) == PathType.WALKABLE) {
                return true;
            }
        }

        return false;
    }

    /**
     * Straight away from the target, flattened. The vertical component is dropped because a marine cannot back away
     * upward and following it would only tilt the strafe inputs.
     */
    private static Vec3 computeAwayDirectionOrNull(Marine marine, LivingEntity attackTarget) {
        var horizontalOffset = new Vec3(
            marine.getX() - attackTarget.getX(),
            0.0,
            marine.getZ() - attackTarget.getZ()
        );

        return horizontalOffset.lengthSqr() < 1.0E-4
            ? null
            : horizontalOffset.normalize();
    }

    private MaintainStandoffAction() {
        throw new UnsupportedOperationException();
    }
}
