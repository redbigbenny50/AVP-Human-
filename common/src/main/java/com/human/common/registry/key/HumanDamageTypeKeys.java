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

    /**
     * [stated] Oct 18 - the vapors (chem thrower blast, its lingering cloud, the distributor's fog) "ignore armour":
     * the mist reaches the whole body. All three are in minecraft:bypasses_armor; the liquids keep their own types.
     */
    public static final ResourceKey<DamageType> CRYO_VAPOR = create("cryo_vapor");

    public static final ResourceKey<DamageType> INFERNIUM_VAPOR = create("infernium_vapor");

    public static final ResourceKey<DamageType> ACID_VAPOR = create("acid_vapor");

    private static ResourceKey<DamageType> create(String id) {
        return ResourceKey.create(Registries.DAMAGE_TYPE, HumanResources.location(id));
    }
}
