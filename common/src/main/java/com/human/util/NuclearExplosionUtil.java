package com.human.util;

import com.alien.common.gameplay.entity.living.alien.Alien;
import com.alien.common.model.alien.variant.AlienVariant;
import com.alien.common.util.AlienTransitionUtil;
import com.blib.api.common.explosion.v1.Explosion;
import com.blib.api.common.explosion.v1.ExplosionProgressTracker;
import com.blib.api.common.explosion.v1.ExplosionUtil;
import com.human.Human;
import com.human.common.gameplay.entity.nuke.MushroomCloudEntity;
import com.human.common.gameplay.explosion.nuke.NuclearExplosionEffects;
import com.human.compatibility.avp_alien.AVPAlien;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

public class NuclearExplosionUtil {

    public static Explosion createNuclearExplosion(ServerLevel level, Vec3 center, int radius, int maxKnockback) {
        var progressTracker = new ExplosionProgressTracker();
        var nuclearExplosionEffects = new NuclearExplosionEffects();

        return Explosion.builder(level, center)
            .withRadius(Direction.Plane.HORIZONTAL, radius)
            .withRadius(Direction.UP, radius / 2)
            .withRadius(Direction.DOWN, 16 * 2)
            .onExplosionStart(() -> {
                progressTracker.startTimer();

                var entities = ExplosionUtil.getEntitiesInRadius(level, center, radius);

                for (var entity : entities) {
                    var distance = entity.distanceToSqr(center);
                    var damage = ExplosionUtil.computeDamage(radius, 5, 1000, distance);

                    if (AVPAlien.MOD.isLoaded()) {
                        if (entity instanceof Alien alien) {
                            AlienTransitionUtil.transitionIntoVariant(alien, AlienVariant.IRRADIATED);
                        }
                    }

                    entity.igniteForSeconds(15);
                    entity.hurt(level.damageSources().explosion(null), (float) damage);
                    ExplosionUtil.applyKnockback(center, radius, entity, maxKnockback, distance);
                }
                var mushroomCloud = new MushroomCloudEntity(level, center.x(), center.y() - 23, center.z());
                level.addFreshEntity(mushroomCloud);
            })
            .onBlockSample(($, pos) -> {
                nuclearExplosionEffects.apply($, pos);
                progressTracker.incrementBlockDestroyCounter();
            })
            .onExplosionFinish(() -> {
                progressTracker.stopTimer();

                var timeTakenInMillis = progressTracker.timeTaken();
                var timeTakenInTicks = timeTakenInMillis / 50;

                Human.LOGGER.info(
                    "Explosion @ {} completed in {}ms ({} ticks), destroying {} blocks!",
                    center,
                    timeTakenInMillis,
                    timeTakenInTicks,
                    progressTracker.blocksDestroyed()
                );
            })
            .build();
    }
}
