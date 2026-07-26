package com.human.util;

import com.human.common.gameplay.explosion.nuke.NuclearExplosionEngine;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class NuclearExplosionUtil {

    public static void detonateNuke(ServerLevel level, Vec3 center, @Nullable Entity source) {
        NuclearExplosionEngine.detonate(level, center, source);
    }

    private NuclearExplosionUtil() {
        throw new UnsupportedOperationException();
    }
}
