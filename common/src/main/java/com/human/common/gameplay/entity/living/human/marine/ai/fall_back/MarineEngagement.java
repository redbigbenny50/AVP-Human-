package com.human.common.gameplay.entity.living.human.marine.ai.fall_back;

import com.human.common.gameplay.entity.living.human.marine.Marine;
import net.minecraft.world.entity.LivingEntity;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * ⭐ Oct 7 - A TOUGH ENEMY IS ONE THAT IS NOT DROPPING. [stated] "if they are fighting an enemy thats tough like theyve
 * been fighting for a good 1-2 seconds and its not dropping they fall back from it too."
 * <p>
 * Each marine remembers the target it is shooting at: who, when the fight with it began, and how much health it had
 * then. The target counts as tough once the marine has been fighting it for {@value #TOUGH_AFTER_TICKS} ticks and it
 * still has at least {@value #TOUGH_HEALTH_FRACTION} of that starting health. A target that IS dropping stops counting
 * the moment it falls below that, and the marine stands its ground again to finish it.
 * </p>
 * <p>
 * The fight is measured from the first shot, not continuous fire: burst pauses and reloads drop the aim pose for a
 * moment, so a fight only resets when the target changes or the marine has not fired for {@value #STALE_AFTER_TICKS}
 * ticks.
 * </p>
 * <p>
 * Server thread only (marine sensors never run on the client). Weak-keyed, so a removed marine is not held.
 * </p>
 */
public final class MarineEngagement {

    /** Two seconds of fighting the same target. */
    public static final int TOUGH_AFTER_TICKS = 40;

    /** "Not dropping": still at half or more of the health it had when the fight began. */
    public static final float TOUGH_HEALTH_FRACTION = 0.5F;

    /** Three seconds without a shot ends the fight. */
    public static final int STALE_AFTER_TICKS = 60;

    private static final Map<Marine, Engagement> ENGAGEMENTS = new WeakHashMap<>();

    private MarineEngagement() {}

    /** {@return true when {@code target} is a tough enemy for this marine right now} */
    public static boolean isTough(Marine marine, LivingEntity target) {
        if (marine.level().isClientSide()) {
            return false;
        }

        var now = marine.tickCount;
        var engagement = ENGAGEMENTS.get(marine);

        if (engagement != null && (engagement.targetId != target.getId() || now - engagement.lastFiringTick > STALE_AFTER_TICKS)) {
            engagement = null;
        }

        if (engagement == null) {
            if (!marine.isAggressive()) {
                ENGAGEMENTS.remove(marine);
                return false; // not shooting at it yet - no fight has started
            }

            ENGAGEMENTS.put(marine, new Engagement(target.getId(), now, target.getHealth()));
            return false;
        }

        if (marine.isAggressive()) {
            engagement.lastFiringTick = now;
        }

        return now - engagement.startTick >= TOUGH_AFTER_TICKS
            && target.getHealth() >= engagement.startHealth * TOUGH_HEALTH_FRACTION;
    }

    private static final class Engagement {

        private final int targetId;

        private final int startTick;

        private final float startHealth;

        private int lastFiringTick;

        private Engagement(int targetId, int startTick, float startHealth) {
            this.targetId = targetId;
            this.startTick = startTick;
            this.startHealth = startHealth;
            this.lastFiringTick = startTick;
        }
    }
}
