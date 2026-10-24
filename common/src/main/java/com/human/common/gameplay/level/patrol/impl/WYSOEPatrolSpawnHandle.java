package com.human.common.gameplay.level.patrol.impl;

import com.human.common.gameplay.level.patrol.PatrolSpawnTimings;
import com.human.common.gameplay.level.patrol.PatrolSpawner;
import com.human.common.gameplay.level.patrol.PatrolSpawnerTicker;
import com.human.common.gameplay.level.patrol.decorator.gear.WYSOEGearDecorator;
import com.human.common.gameplay.level.patrol.decorator.squad.MarineSquadLeadershipDecorator;
import com.human.common.registry.tag.HumanBiomeTags;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;

public class WYSOEPatrolSpawnHandle {

    public static final WYSOEPatrolSpawnHandle INSTANCE = new WYSOEPatrolSpawnHandle();

    private final PatrolSpawner spawner;

    private final PatrolSpawnerTicker ticker;

    private WYSOEPatrolSpawnHandle() {
        this.spawner = PatrolSpawner.builder()
            .withCondition(level -> level.getGameRules().getBoolean(GameRules.RULE_DO_PATROL_SPAWNING))
            .withSpawnPositionSelector(player -> {
                var position = PatrolSpawner.PositionSelector.NEAR_PLAYER.select(player);

                if (position == null || !player.level().getBiome(position).is(HumanBiomeTags.HAS_WY_SPEC_OPS_ELITE_PATROLS)) {
                    return null;
                }

                return position;
            })
            .build(this::spawn);
        this.ticker = PatrolSpawnerTicker.builder()
            .withPlayerSelector(PatrolSpawnerTicker.PlayerSelector.RANDOM_NON_SPECTATOR)
            .withTiming(PatrolSpawnTimings.WY_SPEC_OPS_ELITE_PATROLS)
            .build(spawner);
    }

    public void tick(ServerLevel level) {
        ticker.tick(level);
    }

    public PatrolSpawner getSpawner() {
        return spawner;
    }

    private void spawn(Level level, Player player, BlockPos.MutableBlockPos mutableBlockPos) {
        var spawnedMarines = MarineSpawner.spawn(
            level,
            player,
            mutableBlockPos,
            com.human.common.registry.init.HumanEntityTypes.WY_SPEC_OPS_ELITE.get()
        );

        MarineSquadLeadershipDecorator.INSTANCE.decorate(level, spawnedMarines);

        for (var marine : spawnedMarines) {
            WYSOEGearDecorator.INSTANCE.decorate(level, marine);
        }
    }
}
