package com.human.common.gameplay.entity.living.human.marine.ai;

import com.human.common.gameplay.entity.living.human.marine.Marine;
import com.human.common.gameplay.entity.living.human.marine.squad.MarineSquads;
import com.human.common.registry.init.HumanGameRules;
import net.minecraft.world.entity.player.Player;

/**
 * Oct 5 - Stage 2 performance: AI level of detail by player distance (game rule {@code humanMarineAiLod}).
 * <p>
 * A marine no player is near thinks less often: every tick with a player within {@value #NEAR_BLOCKS} blocks, every
 * {@value #MID_INTERVAL} ticks out to {@value #FAR_BLOCKS}, every {@value #FAR_INTERVAL} beyond that. Staggered by
 * entity id so a camp does not think on the same tick.
 * <p>
 * ⭐ WHY THIS IS SAFE FOR MARINES, when the Sep 5 idle throttle was not for xenomorphs: xenomorphs walk with BLib's own
 * navigator, which is TICKED FROM INSIDE the move action, so skipping the agent stopped them walking. Marines use
 * vanilla ground navigation, which vanilla ticks every tick in the mob's own AI step whatever the agent does - a
 * skipped update only delays the next re-path, door check or decision, never the walking. Checked: the mixin returns
 * before touching the agent when the graph is null, so plans are kept, not dropped.
 * <p>
 * ⭐ NEVER SLOWED, whatever the distance: a marine with a target, hurt in the last five seconds, on fire, in water,
 * following a player, or in a squad that is falling back. Those are full speed every tick.
 * <p>
 * The two intervals are multiples of BLib's own idle interval (4), on the same id stagger, so the two gates line up
 * instead of multiplying.
 */
public final class MarineAiLod {

    /** Full-rate radius, in blocks. */
    private static final double NEAR_BLOCKS = 32.0;

    /** Beyond this, the slowest rate, in blocks. */
    private static final double FAR_BLOCKS = 64.0;

    private static final int MID_INTERVAL = 4;

    private static final int FAR_INTERVAL = 8;

    /** How often the nearest-player distance is re-measured, in ticks. */
    private static final int RECHECK_TICKS = 20;

    /** A marine hurt this recently stays at full rate, in ticks. */
    private static final int HURT_GRACE_TICKS = 100;

    private MarineAiLod() {
        throw new UnsupportedOperationException();
    }

    /**
     * {@return whether the marine's agent should run this tick}
     *
     * @param marine the marine
     */
    public static boolean shouldRunAi(Marine marine) {
        if (!marine.level().getGameRules().getBoolean(HumanGameRules.MARINE_AI_LOD)) {
            return true;
        }

        var stagger = marine.tickCount + marine.getId();

        if (marine.aiLodInterval() <= 0 || stagger % RECHECK_TICKS == 0) {
            marine.setAiLodInterval(measureInterval(marine));
        }

        var interval = marine.aiLodInterval();

        if (interval <= 1 || isUrgent(marine)) {
            return true;
        }

        return stagger % interval == 0;
    }

    private static int measureInterval(Marine marine) {
        // Null filter: spectators count too - someone watching should see full behaviour.
        var player = marine.level().getNearestPlayer(marine.getX(), marine.getY(), marine.getZ(), FAR_BLOCKS, null);

        if (player == null) {
            return FAR_INTERVAL;
        }

        return marine.distanceToSqr(player) > NEAR_BLOCKS * NEAR_BLOCKS ? MID_INTERVAL : 1;
    }

    private static boolean isUrgent(Marine marine) {
        return marine.getTarget() != null
            || marine.isOnFire()
            || marine.isInWater()
            || marine.hurtTime > 0
            || marine.tickCount - marine.getLastHurtByMobTimestamp() < HURT_GRACE_TICKS
            || marine.getLeader().isSomeAnd(leader -> leader instanceof Player)
            || MarineSquads.retreatPoint(marine) != null;
    }
}
