package com.human.common.util;

import com.human.compatibility.avp_alien.AVPAlien;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

/**
 * Volatile Acid (advanced gene): the carrier bursts in an acid splash when it dies. Everything living within
 * {@link #RADIUS} blocks takes {@link #DAMAGE} acid damage, credited to the dead carrier, so the target's acid
 * resistance cuts it and the carrier's own Corrosive Damage boosts it. Uses avp_alien's acid when that mod is present,
 * otherwise avp_human's acid vapour.
 */
public final class GeneAcidSplash {

    public static final double RADIUS = 3.0;

    public static final float DAMAGE = 4.0F;

    private GeneAcidSplash() {}

    public static void splash(LivingEntity source) {
        if (!(source.level() instanceof ServerLevel level)) {
            return;
        }

        var box = source.getBoundingBox().inflate(RADIUS);
        var damageSource = acidSource(source);

        for (var target : level.getEntitiesOfClass(LivingEntity.class, box, other -> other != source && other.isAlive())) {
            if (target.distanceToSqr(source) <= RADIUS * RADIUS) {
                target.hurt(damageSource, DAMAGE);
            }
        }

        level.sendParticles(
            ParticleTypes.ITEM_SLIME,
            source.getX(),
            source.getY(0.5),
            source.getZ(),
            40,
            RADIUS * 0.4,
            0.4,
            RADIUS * 0.4,
            0.05
        );
    }

    private static DamageSource acidSource(LivingEntity source) {
        var sources = source.level().damageSources();

        if (AVPAlien.MOD.isLoaded()) {
            return sources.source(com.alien.common.registry.key.AlienDamageTypeKeys.ACID, source);
        }

        return sources.source(com.human.common.registry.key.HumanDamageTypeKeys.ACID_VAPOR, source);
    }
}
