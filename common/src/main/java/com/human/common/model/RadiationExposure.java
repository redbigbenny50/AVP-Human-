package com.human.common.model;

/**
 * Radiation exposure accumulated by a living entity, implemented on {@code LivingEntity} by
 * {@code MixinLivingEntity_RadiationDamage}. Radiation sources do not apply an effect directly - they MARK the entity
 * as exposed each tick they are present, and the manager turns sustained exposure into a rising sickness level. Cast a
 * {@code LivingEntity} to this interface to read or drive that counter.
 */
public interface RadiationExposure {

    int avp_human$getRadiationExposure();

    void avp_human$setRadiationExposure(int exposure);

    /** Adds exposure outright - used by instantaneous sources such as blasts and radioactive attacks. */
    void avp_human$addRadiationExposure(int amount);

    /**
     * Declares that a radiation source is affecting this entity THIS TICK, at the given rate multiplier (1 = a baseline
     * source such as a hot item in the inventory). The strongest rate marked in a tick wins; the flag is consumed by
     * the manager every tick, so a source only has to keep marking while it is present.
     */
    void avp_human$markRadiationSource(int rate);
}
