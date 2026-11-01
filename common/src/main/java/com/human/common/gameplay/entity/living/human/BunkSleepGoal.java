package com.human.common.gameplay.entity.living.human;

import com.human.common.gameplay.cosmetic.CosmeticBunks;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Monster;

import java.util.EnumSet;

/**
 * [stated] Oct 31: colonists, marines and W-Y troops sleep in free bunks at night. Shared by every human (added in the
 * {@link AbstractHuman} constructor, like the door goal), so every type gets it - including those that drive their
 * movement by GOAP: while this goal runs a marine's agent is paused (see {@link AbstractHuman#isBunking}), so the two
 * never fight over the path.
 * <ul>
 * <li>Night only, in a dimension where beds work; only for a human with a claimed bunk that may sleep now
 * ({@link AbstractHuman#mayUseBunkNow}: no target, not hurt lately, and for a marine, not hired).</li>
 * <li>Walks to the floor in front of the bunk, then lies down - unless someone already sleeps there.</li>
 * <li>Wakes and gets up when it gains a target, is hurt (vanilla also wakes any sleeper that is hurt), or a monster
 * comes within {@value #DANGER_RADIUS} blocks; then waits {@value #WAKE_COOLDOWN_TICKS} ticks before trying again.</li>
 * <li>Holds MOVE, LOOK and JUMP, so idle wandering and looking goals stand aside, as the colonist bed goal does.</li>
 * </ul>
 */
public class BunkSleepGoal extends Goal {

    private static final double LIE_DOWN_DISTANCE_SQR = 2.25D;

    private static final int REPATH_INTERVAL_TICKS = 40;

    private static final double WALK_SPEED = 0.7D;

    public static final int DANGER_RADIUS = 8;

    public static final int WAKE_COOLDOWN_TICKS = 200;

    private final AbstractHuman human;

    private int repathCooldown;

    private int dangerCheckCooldown;

    public BunkSleepGoal(AbstractHuman human) {
        this.human = human;
        setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        var bunk = human.getBunkPos();

        return bunk != null && human.isBunkTime() && human.mayUseBunkNow() && human.level().getGameTime() >= human.bunkWakeUntil()
            && CosmeticBunks.isBunkBed(human.level(), bunk);
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public void start() {
        repathCooldown = 0;
        dangerCheckCooldown = 0;
        human.setBunking(true);
    }

    @Override
    public void stop() {
        if (human.isSleeping()) {
            human.stopSleeping();
        }

        human.getNavigation().stop();
        human.setBunking(false);
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        var bunk = human.getBunkPos();

        if (bunk == null) {
            return;
        }

        if (--dangerCheckCooldown <= 0) {
            dangerCheckCooldown = 20;

            if (!human.level().getEntitiesOfClass(Monster.class, human.getBoundingBox().inflate(DANGER_RADIUS)).isEmpty()) {
                human.wakeFromBunk();
                return;
            }
        }

        if (human.isSleeping()) {
            human.getNavigation().stop();
            return;
        }

        var stand = CosmeticBunks.approach(human.level(), bunk);

        if (stand == null) {
            return;
        }

        var dx = human.getX() - (stand.getX() + 0.5D);
        var dz = human.getZ() - (stand.getZ() + 0.5D);

        if (dx * dx + dz * dz <= LIE_DOWN_DISTANCE_SQR && Math.abs(human.getY() - stand.getY()) < 1.5D) {
            human.getNavigation().stop();

            if (!CosmeticBunks.occupied(human.level(), bunk)) {
                human.startSleeping(bunk);
            }

            return;
        }

        if (--repathCooldown <= 0 || human.getNavigation().isDone()) {
            repathCooldown = REPATH_INTERVAL_TICKS;
            human.getNavigation().moveTo(stand.getX() + 0.5D, stand.getY(), stand.getZ() + 0.5D, WALK_SPEED);
        }
    }
}
