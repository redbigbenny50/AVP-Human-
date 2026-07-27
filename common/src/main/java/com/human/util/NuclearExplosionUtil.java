package com.human.util;

import com.alien.common.gameplay.entity.living.alien.Alien;
import com.alien.common.model.alien.variant.AlienVariant;
import com.alien.common.util.AlienTransitionUtil;
import com.blib.api.common.explosion.v1.Explosion;
import com.blib.api.common.explosion.v1.ExplosionProgressTracker;
import com.blib.api.common.explosion.v1.ExplosionUtil;
import com.human.Human;
import com.human.common.gameplay.effect.RadiationLevel;
import com.human.common.gameplay.entity.nuke.MushroomCloudEntity;
import com.human.common.gameplay.explosion.nuke.NuclearExplosionEffects;
import com.human.common.model.RadiationExposure;
import com.human.compatibility.avp_alien.AVPAlien;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public class NuclearExplosionUtil {

    /** Nobody walks away from a detonation clean: the faintest dose still plants level I. */
    private static final int MINIMUM_FALLOUT_EXPOSURE = 1200;

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

                    applyFalloutDose(entity, distance, radius);

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

    /**
     * The blast's radiation dose, written for the exposure-counter system rather than as a fixed effect.
     * <p>
     * A nuke is the one source that contaminates INSTANTLY instead of accumulating: it adds exposure outright, scaled
     * by how close the victim was, so ground zero lands at {@link RadiationLevel#FATAL} and the fringe at a survivable
     * warning. Falloff uses the SQUARED distance the blast loop already has, which is both free and physically apt -
     * intensity drops with the square of range, giving a broad lethal core and a sharp taper rather than a linear
     * gradient.
     * </p>
     * <p>
     * Roughly: ground zero {@literal ->} level V, half the radius {@literal ->} level IV, seven tenths {@literal ->}
     * level III, nine tenths {@literal ->} level I. Anyone caught inside the radius at all is contaminated to at least
     * level I; walking away from a nuclear detonation completely clean is not a thing.
     * </p>
     * <p>
     * Exposure is ADDED, so a second blast compounds on an already-contaminated victim (clamped at the ceiling), and
     * the ordinary 90-seconds-per-level decay is what carries survivors back down. {@code canBeIrradiated} still gates
     * it, which means a sealed hazard suit turns the fallout aside entirely - that is the whole point of owning one -
     * and radiation-immune entities such as xenomorphs shrug it off.
     * </p>
     */
    private static void applyFalloutDose(Entity entity, double distanceSquared, int radius) {
        if (!(entity instanceof LivingEntity livingEntity) || !HumanPredicates.canBeIrradiated(livingEntity)) {
            return;
        }

        var radiusSquared = (double) radius * radius;
        var intensity = Mth.clamp(1.0 - distanceSquared / radiusSquared, 0.0, 1.0);
        var dose = (int) Math.round(RadiationLevel.MAX_EXPOSURE * intensity);

        ((RadiationExposure) livingEntity).avp_human$addRadiationExposure(
            Math.max(MINIMUM_FALLOUT_EXPOSURE, dose)
        );
    }
}
