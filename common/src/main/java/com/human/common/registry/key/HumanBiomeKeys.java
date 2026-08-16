package com.human.common.registry.key;

import com.human.HumanResources;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;

public class HumanBiomeKeys {

    public static final ResourceKey<Biome> NUKED_BIOME = create("nuked_biome");

    /**
     * The fallout biome's foliage colour override, #904141.
     * <p>
     * Lives here because two places need to agree on it: the biome definition that SETS it, and the leaf tint that
     * IDENTIFIES the biome by it. A colour resolver is handed a raw Biome rather than a holder, so it cannot match on
     * {@link #NUKED_BIOME} and has to recognise the override instead.
     * </p>
     */
    public static final int NUKED_FOLIAGE_COLOR = 9453889;

    private static ResourceKey<Biome> create(String id) {
        return ResourceKey.create(Registries.BIOME, HumanResources.location(id));
    }
}
