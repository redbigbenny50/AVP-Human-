package com.human.common.gameplay.entity.living.human;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * A slow scramble up the side of a water-filled pit, for marines (and any human-shaped NPC) that would otherwise be
 * trapped in it.
 * <p>
 * [stated] Oct 5: "marines should be able to get out of a pit like that can you have them slowly crawl up the wall like
 * very slowly now that we have wall climbing as an option they would use the crawling or swimming animation to go up
 * it." The pit in question: water about two blocks below the rim. A marine's step is vanilla's 0.6, its water jump
 * clears about one block, so a two-block lip was a cell with no way out - they bobbed against the wall forever.
 * </p>
 * <p>
 * ⭐ HOW IT LOOKS. The human takes the player's SWIMMING pose for the climb - the same pose and the same
 * {@code MarineAnimator.applySwimStroke} a swimming marine already uses - so it reads as dragging itself up the bank on
 * its belly, and the 0.6-tall swimming box is what lets it slide up a one-block-wide gap between wall and water. At the
 * top it is pushed over the lip, and {@code HumanWaterMovement.updateSwimming} stands it up again the moment the
 * standing box fits.
 * </p>
 * <p>
 * ⚠ WATER ONLY, ON PURPOSE. Allowing it on dry land would make every marine scale every two-to-four block wall it
 * bumped while walking - a behaviour change nobody asked for. Lips of one block are left to the ordinary water jump.
 * </p>
 */
public final class HumanWallScramble {

    /** Blocks per tick - "very slowly": a two-block bank takes about two and a half seconds. */
    private static final double CLIMB_SPEED = 0.04;

    /** Sideways press into the wall while climbing, so contact holds. */
    private static final double HUG_SPEED = 0.03;

    /** Push over the lip once the feet clear it. */
    private static final double OVER_SPEED = 0.25;

    /** Lowest lip (above the feet) this handles - one block is the water jump's job. */
    private static final int MIN_LIP_HEIGHT = 2;

    /**
     * Oct 5 - the lowest lip a STUCK human climbs. A one-block lip is left to the water hop while the human is trying
     * to go somewhere, but the hop only happens when something walks it into the bank: an idle marine floating beside a
     * one-block bank never got out. Once it has been stuck in the water for the full window it climbs that too.
     */
    private static final int MIN_STUCK_LIP_HEIGHT = 1;

    /** Highest lip this handles; anything taller is not a pit bank. */
    private static final int MAX_LIP_HEIGHT = 4;

    /** A standing human is 1.8 tall: two clear blocks on top of the lip. */
    private static final int STANDING_HEADROOM = 2;

    /** How long a human must have been in water, unable to leave, before it starts looking for a bank to climb. */
    private static final int STUCK_TICKS_BEFORE_SEARCH = 60;

    /** The search is cheap but not free; once a second is plenty for a stranded human. */
    private static final int SEARCH_INTERVAL_TICKS = 20;

    private static final Map<AbstractHuman, Scramble> ACTIVE = new WeakHashMap<>();

    /**
     * How far (squared) a human must have drifted during the stuck window for it to count as still getting somewhere.
     */
    private static final double STUCK_DRIFT_SQUARED = 2.0 * 2.0;

    private static final Map<AbstractHuman, StuckWindow> STUCK = new WeakHashMap<>();

    /**
     * ⚠ Oct 6 - [stated] tester: marines "upon entering water are forever stuck t posing". A scramble that ran out of
     * time without getting over the lip ended, and the stuck search started a new one at the very same wall on its next
     * pass - so a marine at a bank it cannot actually top (an ice wall, a lip with something on it) climbed in place
     * for ever, arms up the wall in the hand-over-hand pose. A failed scramble now rests that marine for 10 seconds
     * before it may try again; the stranded-at-sea rescue covers one that never gets out.
     */
    private static final Map<AbstractHuman, Long> FAILED_UNTIL = new WeakHashMap<>();

    private static final long FAILED_SCRAMBLE_REST_TICKS = 200L;

    private HumanWallScramble() {
        throw new UnsupportedOperationException();
    }

    /** Whether this human is mid-scramble; the water movement holds the swimming pose while it is. */
    public static boolean isScrambling(AbstractHuman human) {
        return ACTIVE.containsKey(human);
    }

    /** Server only, from {@code AbstractHuman.customServerAiStep}, so a netted or NoAI human never scrambles. */
    public static void tick(AbstractHuman human) {
        var scramble = ACTIVE.get(human);

        if (scramble != null) {
            continueScramble(human, scramble);
            return;
        }

        // A scramble that ended any other way must not leave the client drawing the climb pose.
        if (human.scrambleDirection.get() >= 0) {
            human.scrambleDirection.set(-1);
        }

        if (!human.isInWater() || human.isPassenger() || isMeantToBeInWater(human)) {
            STUCK.remove(human);
            return;
        }

        var restUntil = FAILED_UNTIL.get(human);

        if (restUntil != null) {
            if (human.level().getGameTime() < restUntil) {
                return;
            }

            FAILED_UNTIL.remove(human);
        }

        var stuckTicks = trackStuckWindow(human);

        // ⚠ Two triggers. Pushing into the bank right now is the obvious one. The second catches a human that has
        // stopped trying - its path ran out against the wall and it is just floating - which is exactly the trapped
        // state in the report.
        Direction direction = null;
        Integer lipTop = null;

        if (human.horizontalCollision) {
            direction = Direction.fromYRot(human.getYRot());
            lipTop = findLipTop(human, direction, MIN_LIP_HEIGHT);
        }

        if (lipTop == null && stuckTicks >= STUCK_TICKS_BEFORE_SEARCH && stuckTicks % SEARCH_INTERVAL_TICKS == 0) {
            for (var candidate : Direction.Plane.HORIZONTAL) {
                var top = findLipTop(human, candidate, MIN_STUCK_LIP_HEIGHT);

                if (top != null) {
                    direction = candidate;
                    lipTop = top;
                    break;
                }
            }
        }

        if (lipTop == null) {
            return;
        }

        var height = lipTop - human.getY();
        var maxTicks = (int) Math.ceil(height / CLIMB_SPEED) + 40;
        ACTIVE.put(human, new Scramble(direction, lipTop, human.tickCount + maxTicks));
        human.scrambleDirection.set(direction.get2DDataValue());
        continueScramble(human, ACTIVE.get(human));
    }

    private static void continueScramble(AbstractHuman human, Scramble scramble) {
        // ⚠ Oct 5 - OUT OF THE WATER AND STANDING IS DONE, whatever the height check says. A marine was seen standing
        // on
        // the bank still drawn in the climb pose; this guarantees the state (and the synced pose) clears once it has
        // footing again.
        if (human.tickCount > scramble.endTick) {
            // Timed out below the lip: this wall beat it. Rest before the next try (see FAILED_UNTIL).
            FAILED_UNTIL.put(human, human.level().getGameTime() + FAILED_SCRAMBLE_REST_TICKS);
            end(human);
            return;
        }

        if (human.isPassenger() || (!human.isInWater() && human.onGround())) {
            end(human);
            return;
        }

        // ⚠ Facing is re-asserted every tick, server side too: the look control turns the head toward whatever the
        // marine is watching, and the body follows the head. The client draws the synced wall direction regardless
        // (HumanEntityModelRenderer); this keeps the server's own facing - and anything that reads it - in agreement.
        var yaw = scramble.direction.toYRot();
        human.setYRot(yaw);
        human.yBodyRot = yaw;
        human.setYHeadRot(yaw);
        // The look control runs after this in the same tick and would swing the head back to whatever it was watching;
        // give it the wall to look at instead, so the head and body agree.
        human.getLookControl()
            .setLookAt(
                human.getX() + scramble.direction.getStepX(),
                human.getEyeY() + 0.5,
                human.getZ() + scramble.direction.getStepZ()
            );

        var dx = scramble.direction.getStepX();
        var dz = scramble.direction.getStepZ();

        if (human.getY() < scramble.lipTop + 0.05) {
            human.setDeltaMovement(dx * HUG_SPEED, CLIMB_SPEED, dz * HUG_SPEED);
        } else {
            human.setDeltaMovement(dx * OVER_SPEED, 0.05, dz * OVER_SPEED);
            end(human);
            STUCK.remove(human);
        }

        human.hasImpulse = true;
        human.fallDistance = 0.0F;
    }

    /**
     * ⚠ Oct 5 review - THE TWO CASES WHERE CLIMBING OUT IS WRONG. Walking the code, not the intent: a hired marine
     * mirrors its leader in the water (HumanWaterMovement), so a marine swimming alongside a player would have bumped
     * the bank, or sat in the water three seconds, and dragged itself out - away from the leader it is meant to follow,
     * only for its follow action to walk it straight back in. Same for a marine fighting something that is itself in
     * the water. Neither is "trapped".
     */
    private static boolean isMeantToBeInWater(AbstractHuman human) {
        var leader = human.getWaterMovementLeaderOrNull();

        if (leader != null && leader.isInWater()) {
            return true;
        }

        var target = human.getTarget();

        return target != null && target.isAlive() && target.isInWater();
    }

    /**
     * {@return how long this human has been in the water WITHOUT GETTING ANYWHERE} - ticks since it last drifted two
     * blocks. ⚠ Time in the water alone was the first version, and it would have pulled out any human that simply swam
     * for three seconds; a trapped one is in the water AND going nowhere.
     */
    private static int trackStuckWindow(AbstractHuman human) {
        var window = STUCK.get(human);

        if (window == null || human.distanceToSqr(window.anchorX, human.getY(), window.anchorZ) > STUCK_DRIFT_SQUARED) {
            window = new StuckWindow(human.getX(), human.getZ());
            STUCK.put(human, window);
        }

        return ++window.ticks;
    }

    private static final class StuckWindow {

        private final double anchorX;

        private final double anchorZ;

        private int ticks;

        private StuckWindow(double anchorX, double anchorZ) {
            this.anchorX = anchorX;
            this.anchorZ = anchorZ;
        }
    }

    private static void end(AbstractHuman human) {
        ACTIVE.remove(human);
        human.scrambleDirection.set(-1);
    }

    /**
     * {@return the Y of the bank's top on the column in {@code direction}, or null when that side is not a bank this
     * can climb} The column beside the body must be solid from the feet up to the top, the top must have standing room
     * and be dry, and it must sit {@value #MIN_LIP_HEIGHT} to {@value #MAX_LIP_HEIGHT} blocks above the feet.
     */
    private static @Nullable Integer findLipTop(AbstractHuman human, Direction direction, int minimumLipHeight) {
        var level = human.level();
        var feet = human.blockPosition();
        var column = feet.relative(direction);

        if (level.getBlockState(column).getCollisionShape(level, column).isEmpty()) {
            return null; // open water or air beside it - not a wall
        }

        for (var height = 1; height <= MAX_LIP_HEIGHT; height++) {
            var pos = column.above(height);

            if (!level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()) {
                continue;
            }

            // First open block up the wall: the bank's top is the block below it.
            if (height < minimumLipHeight || !isOpenAndDry(human, pos, STANDING_HEADROOM)) {
                return null;
            }

            return pos.getY();
        }

        return null;
    }

    private static boolean isOpenAndDry(AbstractHuman human, BlockPos base, int headroom) {
        var level = human.level();

        for (var i = 0; i < headroom; i++) {
            var pos = base.above(i);

            if (!level.getBlockState(pos).getCollisionShape(level, pos).isEmpty() || !level.getFluidState(pos).isEmpty()) {
                return false;
            }
        }

        return true;
    }

    private record Scramble(
        Direction direction,
        int lipTop,
        int endTick
    ) {}
}
