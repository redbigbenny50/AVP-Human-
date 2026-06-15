package com.human.common.gameplay.entity.living.human.marine.ai.follow_leader;

import com.blib.api.common.entity.v1.BLibEntityPredicates;
import com.human.common.gameplay.entity.living.human.marine.Marine;
import com.human.common.gameplay.entity.living.human.marine.MarineMode;
import com.just.ai.goap.StateKey;
import com.just.ai.goap.sensor.Sensor;
import com.just.ai.goap.sensor.Sensors;
import net.minecraft.world.entity.player.Player;

public class FollowLeaderSensors {

    public static final Sensor.Mono<Marine, Boolean> CAN_FOLLOW_LEADER = Sensors.map(
        StateKey.sensed("can_follow_leader"),
        marine -> !marine.isPassenger()
            && marine.getMode() == MarineMode.FOLLOW
            && marine.getLeader()
                .isSomeAnd(
                    leader -> BLibEntityPredicates.isAlive(leader)
                        && !leader.isSpectator()
                )
    );

    public static final Sensor.Mono<Marine, Boolean> HAS_LEADER = Sensors.map(
        StateKey.sensed("has_leader"),
        marine -> marine.getLeader().isSome()
    );

    public static final Sensor.Mono<Marine, Boolean> HAS_PLAYER_LEADER = Sensors.map(
        StateKey.sensed("has_player_leader"),
        marine -> marine.getLeader().isSomeAnd(leader -> leader instanceof Player)
    );

    public static final Sensor.Mono<Marine, Boolean> IS_CLOSE_TO_LEADER = Sensors.map(
        StateKey.sensed("is_close_to_leader"),
        marine -> marine.getLeader()
            .isSomeAnd(leader -> marine.distanceToSqr(leader) < 9)
    );

    private FollowLeaderSensors() {
        throw new UnsupportedOperationException();
    }
}
