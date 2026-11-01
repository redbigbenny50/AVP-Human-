package com.human.common.gameplay.level.patrol.impl;

import com.human.common.gameplay.entity.living.human.marine.Marine;
import com.human.common.gameplay.level.patrol.PatrolSpawnTimings;
import com.human.common.gameplay.level.patrol.PatrolSpawner;
import com.human.common.gameplay.level.patrol.PatrolSpawnerTicker;
import com.human.common.gameplay.level.patrol.decorator.gear.MarineGearDecorator;
import com.human.common.gameplay.level.patrol.decorator.gear.TacticalMarineGearDecorator;
import com.human.common.gameplay.level.patrol.decorator.squad.MarineSquadLeadershipDecorator;
import com.human.common.registry.init.HumanEntityTypes;
import com.human.common.registry.tag.HumanBiomeTags;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

public class MarinePatrolSpawnHandle {

    public static final MarinePatrolSpawnHandle INSTANCE = new MarinePatrolSpawnHandle();

    private final PatrolSpawner spawner;

    private final PatrolSpawnerTicker ticker;

    private MarinePatrolSpawnHandle() {
        this.spawner = PatrolSpawner.builder()
            .withCondition(level -> level.getGameRules().getBoolean(GameRules.RULE_DO_PATROL_SPAWNING))
            .withSpawnPositionSelector(player -> {
                var position = PatrolSpawner.PositionSelector.NEAR_PLAYER.select(player);

                if (position == null || !player.level().getBiome(position).is(HumanBiomeTags.HAS_MARINE_PATROLS)) {
                    return null;
                }

                return position;
            })
            .build(this::spawn);
        this.ticker = PatrolSpawnerTicker.builder()
            .withPlayerSelector(PatrolSpawnerTicker.PlayerSelector.RANDOM_NON_SPECTATOR)
            .withTiming(PatrolSpawnTimings.MARINE_PATROLS)
            .build(spawner);
    }

    public void tick(ServerLevel level) {
        ticker.tick(level);
    }

    public PatrolSpawner getSpawner() {
        return spawner;
    }

    /**
     * [stated] Oct 27 - tactical marines are a RARE variant mixed into ordinary patrols, and they are the only marines
     * that bring dogs. One patrol in five swaps one of its four soldiers for a tactical marine in camo.
     */
    public static final float TACTICAL_MIX_IN_CHANCE = 0.2F;

    private static final int PATROL_SIZE = 4;

    private void spawn(Level level, Player player, BlockPos.MutableBlockPos mutableBlockPos) {
        var withTactical = level.random.nextFloat() < TACTICAL_MIX_IN_CHANCE;
        var regulars = MarineSpawner.spawn(
            level,
            player,
            mutableBlockPos,
            HumanEntityTypes.MARINE.get(),
            withTactical ? PATROL_SIZE - 1 : PATROL_SIZE
        );
        var tacticals = withTactical
            ? MarineSpawner.spawn(level, player, mutableBlockPos, HumanEntityTypes.TACTICAL_MARINE.get(), 1)
            : List.<Marine>of();

        var squad = new ArrayList<Marine>(regulars);
        squad.addAll(tacticals);

        MarineSquadLeadershipDecorator.INSTANCE.decorate(level, squad);

        for (var marine : regulars) {
            MarineGearDecorator.INSTANCE.decorate(level, marine);
        }

        for (var marine : tacticals) {
            TacticalMarineGearDecorator.INSTANCE.decorate(level, marine);
        }

        WolfSpawner.spawnForTacticalMarines(level, tacticals);
    }
}
