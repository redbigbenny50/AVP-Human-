package com.human.common.gameplay.level.patrol.decorator.squad;

import com.human.common.gameplay.entity.living.human.marine.Marine;
import com.human.common.gameplay.level.patrol.decorator.MarineSquadDecorator;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Objects;

public class MarineSquadLeadershipDecorator implements MarineSquadDecorator {

    public static final MarineSquadLeadershipDecorator INSTANCE = new MarineSquadLeadershipDecorator();

    private MarineSquadLeadershipDecorator() {}

    @Override
    public void decorate(Level level, List<Marine> marines) {
        if (marines.isEmpty()) {
            return;
        }

        var leader = marines.get(level.random.nextInt(marines.size()));

        for (var marine : marines) {
            if (Objects.equals(marine.getUUID(), leader.getUUID())) {
                continue;
            }

            // ⚠⚠ setSquadLeader, NOT setLeader. Writing a MARINE into the employer slot is what made every patrol
            // member permanently unhireable and un-openable - hasLeader() was already true so the diamond was
            // refused, and the inventory gate compares the leader to the clicking PLAYER so it never matched.
            marine.setSquadLeader(leader);
        }
    }
}
