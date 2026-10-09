package com.human.common.registry.key;

import com.human.HumanResources;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageType;

public class HumanDamageTypeKeys {

    public static final ResourceKey<DamageType> BULLET = create("bullet");

    public static final ResourceKey<DamageType> FLAMETHROW = create("flamethrow");

    public static final ResourceKey<DamageType> RAZOR_WIRE = create("razor_wire");

    public static final ResourceKey<DamageType> RADIATION = create("radiation");

    /** [stated] Oct 1: standing in cryofluid without a full environmental suit. */
    public static final ResourceKey<DamageType> CRYOFLUID = create("cryofluid");

    private static ResourceKey<DamageType> create(String id) {
        return ResourceKey.create(Registries.DAMAGE_TYPE, HumanResources.location(id));
    }
}
