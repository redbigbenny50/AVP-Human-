package com.human.common.gameplay.entity.living.human.colonist;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.BedBlock;

import java.util.EnumSet;

/**
 * Night: walk to the colonist's claimed bed and sleep in it; day: get up.
 * <p>
 * [stated] Oct 23 - colonists "assign to beds like villagers do." The bed itself is claimed elsewhere
 * ({@link ColonistEntity#tickBedClaim}); this goal only gets the colonist into it and out again.
 * </p>
 * <ul>
 * <li>Holds MOVE, LOOK and JUMP while it runs, so the stroll, look-at-player and look-around goals below it stand
 * aside: a colonist heading to bed does not wander off, and a sleeping one does not turn its head.</li>
 * <li>Doors on the way are opened by the shared human door goal, which sits above this one.</li>
 * <li>Vanilla wakes any sleeper that takes damage ({@code LivingEntity.hurt}), and breaking the bed wakes it too
 * ({@code LivingEntity} checks the bed still exists every tick). Either way this goal then ends, and picks back up the
 * next time it is night and the bed is there.</li>
 * <li>A bed someone else is lying in (a player, say) is waited out beside it, not shared.</li>
 * </ul>
 */
public class ColonistSleepGoal extends Goal {

    /** Close enough to lie down: within one and a half blocks of the bed's head. */
    private static final double LIE_DOWN_DISTANCE_SQR = 2.25D;

    /** How often the route to the bed is renewed while walking - often enough to get round a closed door. */
    private static final int REPATH_INTERVAL_TICKS = 40;

    private static final double WALK_SPEED = 0.7D;

    private final ColonistEntity colonist;

    private int repathCooldown;

    public ColonistSleepGoal(ColonistEntity colonist) {
        this.colonist = colonist;
        setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        return colonist.isSleepTime() && colonist.getBedPos() != null && colonist.isBedStillThere();
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public void start() {
        repathCooldown = 0;
    }

    @Override
    public void stop() {
        if (colonist.isSleeping()) {
            colonist.stopSleeping();
        }
        colonist.getNavigation().stop();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        var bed = colonist.getBedPos();
        if (bed == null) {
            return;
        }

        if (colonist.isSleeping()) {
            colonist.getNavigation().stop();
            return;
        }

        var distance = colonist.distanceToSqr(bed.getX() + 0.5D, bed.getY() + 0.5D, bed.getZ() + 0.5D);

        if (distance <= LIE_DOWN_DISTANCE_SQR) {
            colonist.getNavigation().stop();
            if (!isOccupied(bed)) {
                colonist.startSleeping(bed);
            }
            return;
        }

        if (--repathCooldown <= 0 || colonist.getNavigation().isDone()) {
            repathCooldown = REPATH_INTERVAL_TICKS;
            colonist.getNavigation().moveTo(bed.getX() + 0.5D, bed.getY(), bed.getZ() + 0.5D, WALK_SPEED);
        }
    }

    private boolean isOccupied(BlockPos bed) {
        var state = colonist.level().getBlockState(bed);
        return state.getBlock() instanceof BedBlock && state.getValue(BedBlock.OCCUPIED);
    }
}
