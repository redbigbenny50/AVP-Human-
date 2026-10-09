package com.human.common.gameplay.entity.living.human.marine.ai.idle.action;

import com.blib.api.common.goap.v1.action.impl.MoveToPosAction;
import com.human.common.gameplay.entity.living.human.marine.Marine;
import com.just.ai.goap.StateKey;
import com.just.ai.goap.action.Action;
import com.just.ai.goap.state.Blackboard;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * An idle marine in water heads for the nearest dry land and waits there. [stated] Oct 4: marines dropped in water (a
 * camp that generated in a lake) bobbed in place forever and spun in circles. The bobbing was the surface float with
 * nowhere to go; the spinning was the wander failing over and over in water - it only ever picks LAND spots, finds none
 * within reach, aborts, and the planner restarts it every second.
 * <p>
 * [stated] "If its an option [go back to camp] otherwise wait on shore" - camp marines keep no record of their camp, so
 * they wait on the shore. Only an idle marine does this: one following a player, or with something to fight, is left to
 * those behaviours (preconditions in {@code IdleActions}).
 * </p>
 */
public class GetOutOfWaterAction {

    private static final StateKey<Vec3> SHORE_POS = StateKey.sensed("shore_pos");

    /** How far out to look for dry ground. */
    private static final int SEARCH_RADIUS = 16;

    /** How often to look again (a moving marine, a changing shore), in ticks. */
    private static final int SEARCH_INTERVAL = 40;

    public static Action.Signal perform(Action.Context<? extends Marine> context) {
        var marine = context.getActor();
        var blackboard = context.getBlackboard(Blackboard.Scope.ACTION);
        var shore = blackboard.getOrNull(SHORE_POS);

        if (shore == null || marine.tickCount % SEARCH_INTERVAL == 0) {
            shore = findShore(marine.level(), marine.blockPosition());

            if (shore == null) {
                // Open water, no shore in reach: float calmly (the surface float keeps it up) instead of thrashing.
                blackboard.clear(); // this action's own scratch space - nothing worth keeping
                marine.getNavigation().stop();
                return Action.Signal.CONTINUE;
            }

            blackboard.set(SHORE_POS, shore);
        }

        // Climbing out: a bank one block up stops a swimmer - hop it, as a player would.
        if (marine.horizontalCollision && marine.isInWater()) {
            marine.getJumpControl().jump();
        }

        return switch (MoveToPosAction.perform(context, shore, 1.0D)) {
            case FINISHED, MOVING -> Action.Signal.CONTINUE;
            case NO_PATH -> {
                // No walkable path from the water: swim straight for it; the move control handles open water.
                marine.getMoveControl().setWantedPosition(shore.x, shore.y, shore.z, 1.0D);
                yield Action.Signal.CONTINUE;
            }
        };
    }

    public static void onFinish(Action.Context<? extends Marine> context) {
        MoveToPosAction.onFinish(context);
    }

    /**
     * The nearest dry spot a marine can stand on, searched ring by ring outward so the first ring with any wins (cheap
     * when the shore is close), or null if there is none within {@link #SEARCH_RADIUS}.
     */
    private static @Nullable Vec3 findShore(Level level, BlockPos origin) {
        var cursor = new BlockPos.MutableBlockPos();
        BlockPos best = null;
        var bestDistance = Double.MAX_VALUE;

        for (int ring = 1; ring <= SEARCH_RADIUS && best == null; ring++) {
            for (int dx = -ring; dx <= ring; dx++) {
                for (int dz = -ring; dz <= ring; dz++) {
                    if (Math.abs(dx) != ring && Math.abs(dz) != ring) {
                        continue; // only this ring's edge
                    }

                    for (int dy = -2; dy <= 3; dy++) {
                        cursor.set(origin.getX() + dx, origin.getY() + dy, origin.getZ() + dz);

                        if (isStandable(level, cursor)) {
                            var distance = cursor.distSqr(origin);

                            if (distance < bestDistance) {
                                bestDistance = distance;
                                best = cursor.immutable();
                            }
                        }
                    }
                }
            }
        }

        return best == null ? null : Vec3.atBottomCenterOf(best);
    }

    /** Dry, solid footing with room for a marine: no water at the feet, a sturdy top below, two clear blocks of air. */
    private static boolean isStandable(Level level, BlockPos feet) {
        if (!level.isLoaded(feet) || !level.getFluidState(feet).isEmpty()) {
            return false;
        }

        var below = feet.below();
        var head = feet.above();

        return level.getBlockState(below).isFaceSturdy(level, below, Direction.UP)
            && level.getFluidState(below).isEmpty()
            && level.getBlockState(feet).getCollisionShape(level, feet).isEmpty()
            && level.getBlockState(head).getCollisionShape(level, head).isEmpty();
    }

    private GetOutOfWaterAction() {
        throw new UnsupportedOperationException();
    }
}
