package com.human.common.gameplay.level.patrol.impl;

import com.human.common.gameplay.level.patrol.PatrolSpawnTimings;
import com.human.common.gameplay.level.patrol.PatrolSpawner;
import com.human.common.gameplay.level.patrol.PatrolSpawnerTicker;
import com.human.common.gameplay.level.patrol.decorator.gear.ApeGearDecorator;
import com.human.common.gameplay.level.patrol.decorator.squad.MarineSquadLeadershipDecorator;
import com.human.common.registry.tag.HumanBiomeTags;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;

public class ApePatrolSpawnHandle {

    public static final ApePatrolSpawnHandle INSTANCE = new ApePatrolSpawnHandle();

    private final PatrolSpawner spawner;

    private final PatrolSpawnerTicker ticker;

    private ApePatrolSpawnHandle() {
        this.spawner = PatrolSpawner.builder()
            .withCondition(level -> level.getGameRules().getBoolean(GameRules.RULE_DO_PATROL_SPAWNING))
            .withSpawnPositionSelector(player -> {
                var position = PatrolSpawner.PositionSelector.NEAR_PLAYER.select(player);

                if (position == null) {
                    return null;
                }

                if (player.level().getBiome(position).is(HumanBiomeTags.HAS_WY_APE_PATROLS)) {
                    return position;
                }

                // [stated] Oct 13 - "the ape units should appear in xenomorph territory if avp alien is installed even
                // if
                // its not their usual biome". Without avp_alien no hive claim exists and this never passes.
                if (
                    player.level() instanceof net.minecraft.server.level.ServerLevel serverLevel
                        && com.human.common.gameplay.entity.living.human.soldier.HiveTerritory.isHiveTerritory(serverLevel, position)
                ) {
                    return position;
                }

                return null;
            })
            .build(this::spawn);
        this.ticker = PatrolSpawnerTicker.builder()
            .withPlayerSelector(PatrolSpawnerTicker.PlayerSelector.RANDOM_NON_SPECTATOR)
            .withTiming(PatrolSpawnTimings.WY_APE_PATROLS)
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
            com.human.common.registry.init.HumanEntityTypes.WY_APE_TROOPER.get()
        );

        MarineSquadLeadershipDecorator.INSTANCE.decorate(level, spawnedMarines);

        for (var marine : spawnedMarines) {
            ApeGearDecorator.INSTANCE.decorate(level, marine);
        }
    }
}
