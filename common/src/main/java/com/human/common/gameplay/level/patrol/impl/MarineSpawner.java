package com.human.common.gameplay.level.patrol.impl;

import com.human.common.gameplay.entity.living.human.marine.Marine;
import com.human.common.registry.init.HumanEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MarineSpawner {

    public static List<Marine> spawn(Level level, Player player, BlockPos.MutableBlockPos startPos) {
        return spawn(level, player, startPos, HumanEntityTypes.MARINE.get());
    }

    /** [stated] Oct 13 - a patrol of one soldier type (the W-Y units and the tactical marine have their own). */
    public static List<Marine> spawn(Level level, Player player, BlockPos.MutableBlockPos startPos, EntityType<Marine> type) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return List.of();
        }

        var spawnedMarines = new ArrayList<Marine>();
        var randomSource = level.random;
        var spawnCount = 4;

        for (var i = 0; i < spawnCount; ++i) {
            startPos.setY(level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, startPos).getY());

            var spawnedMarineOrNull = spawnMarine(serverLevel, startPos, type);

            if (spawnedMarineOrNull != null) {
                spawnedMarines.add(spawnedMarineOrNull);
            }

            startPos.setX(startPos.getX() + randomSource.nextInt(5) - randomSource.nextInt(5));
            startPos.setZ(startPos.getZ() + randomSource.nextInt(5) - randomSource.nextInt(5));
        }

        return Collections.unmodifiableList(spawnedMarines);
    }

    private static @Nullable Marine spawnMarine(ServerLevel level, BlockPos pos, EntityType<Marine> type) {
        var marine = type.create(level);

        if (marine != null) {
            marine.setPos(pos.getX(), pos.getY(), pos.getZ());
            marine.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.PATROL, null);
            level.addFreshEntityWithPassengers(marine);
        }

        return marine;
    }
}
