package com.human.common.gameplay.entity.living.human.marine.ai.sentry;

import com.human.common.gameplay.entity.living.human.marine.Marine;
import com.just.ai.goap.StateKey;
import com.just.ai.goap.sensor.Sensor;
import com.just.ai.goap.sensor.Sensors;

public class SentrySensors {

    /**
     * How far a sentry may wander from its post.
     * <p>
     * Deliberately generous. A sentry that could not leave its own block would be a turret; the point is a marine
     * guarding an area, free to step behind cover, walk to a supply chest, or push a few paces toward something it is
     * shooting at.
     */
    public static final double SENTRY_LEASH_IN_BLOCKS = 16.0;

    /**
     * Where the marine settles when returning. Smaller than the leash so it comes properly back to its post rather than
     * stopping the instant it clips the edge of the circle and drifting outward again on the next disturbance.
     */
    public static final double SENTRY_RETURN_DISTANCE_IN_BLOCKS = 4.0;

    private static final double SENTRY_LEASH_SQR = SENTRY_LEASH_IN_BLOCKS * SENTRY_LEASH_IN_BLOCKS;

    private static final double SENTRY_RETURN_DISTANCE_SQR =
        SENTRY_RETURN_DISTANCE_IN_BLOCKS * SENTRY_RETURN_DISTANCE_IN_BLOCKS;

    public static final Sensor.Mono<Marine, Boolean> IS_SENTRY = Sensors.map(
        StateKey.sensed("is_sentry"),
        Marine::isSentry
    );

    /**
     * Whether the marine has strayed past the leash and needs to come back.
     * <p>
     * ⚠ Hysteresis is the point of the two distances: leaving is judged at 16 blocks and arriving at 4, so a sentry
     * hovering right on the boundary does not flip between returning and not returning every few ticks.
     */
    public static final Sensor.Mono<Marine, Boolean> IS_AWAY_FROM_POST = Sensors.map(
        StateKey.sensed("is_away_from_post"),
        marine -> marine.isSentry()
            && marine.getSentryPos()
                .isSomeAnd(
                    sentryPos -> marine.distanceToSqr(
                        sentryPos.getX() + 0.5,
                        sentryPos.getY(),
                        sentryPos.getZ() + 0.5
                    ) > SENTRY_LEASH_SQR
                )
    );

    /**
     * Whether the marine is back at its post. The goal's desired condition, and therefore what ends the return.
     */
    public static final Sensor.Mono<Marine, Boolean> IS_AT_POST = Sensors.map(
        StateKey.sensed("is_at_post"),
        marine -> !marine.isSentry()
            || marine.getSentryPos()
                .isSomeAnd(
                    sentryPos -> marine.distanceToSqr(
                        sentryPos.getX() + 0.5,
                        sentryPos.getY(),
                        sentryPos.getZ() + 0.5
                    ) <= SENTRY_RETURN_DISTANCE_SQR
                )
    );

    private SentrySensors() {
        throw new UnsupportedOperationException();
    }
}
