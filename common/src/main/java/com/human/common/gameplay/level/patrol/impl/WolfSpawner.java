package com.human.common.gameplay.level.patrol.impl;

import com.human.common.gameplay.entity.living.dog.MarineDog;
import com.human.common.gameplay.entity.living.human.marine.Marine;
import com.human.common.registry.init.HumanEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Patrol dogs.
 * <p>
 * [stated] Oct 27 - ONLY TACTICAL MARINES COME WITH DOGS. This used to roll 0-2 dogs for every regular marine patrol,
 * which together with every regular marine carrying bones (and converting every wild wolf it saw) filled marine camps
 * with dogs. Now each tactical marine gets its own roll, and the dog belongs to that marine.
 */
public class WolfSpawner {

    /** Chance that a tactical marine brings a dog. */
    public static final float TACTICAL_DOG_CHANCE = 0.5F;

    public static List<MarineDog> spawnForTacticalMarines(Level level, List<Marine> marines) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return List.of();
        }

        var spawned = new ArrayList<MarineDog>();

        for (var marine : marines) {
            if (!isTactical(marine) || level.random.nextFloat() >= TACTICAL_DOG_CHANCE) {
                continue;
            }

            var dog = spawnMarineDog(serverLevel, besideMarine(serverLevel, marine));

            if (dog != null) {
                dog.assignMarineOwner(marine);
                spawned.add(dog);
            }
        }

        return Collections.unmodifiableList(spawned);
    }

    public static boolean isTactical(Marine marine) {
        return marine.getType() == HumanEntityTypes.TACTICAL_MARINE.get();
    }

    private static BlockPos besideMarine(ServerLevel level, Marine marine) {
        var pos = marine.blockPosition().mutable();
        pos.setX(pos.getX() + level.random.nextInt(3) - 1);
        pos.setZ(pos.getZ() + level.random.nextInt(3) - 1);
        pos.setY(level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, pos).getY());
        return pos.immutable();
    }

    private static @Nullable MarineDog spawnMarineDog(ServerLevel level, BlockPos pos) {
        var dog = HumanEntityTypes.MARINE_DOG.get().create(level);

        if (dog != null) {
            dog.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
            dog.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.PATROL, null);
            level.addFreshEntityWithPassengers(dog);
        }

        return dog;
    }

    private WolfSpawner() {
        throw new UnsupportedOperationException();
    }
}
