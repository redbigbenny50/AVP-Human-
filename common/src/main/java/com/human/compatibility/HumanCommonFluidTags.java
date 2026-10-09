package com.human.compatibility;

import com.blib.api.common.tag.v1.CommonConstants;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.material.Fluid;

/**
 * Shared {@code c:} fluid tags - the names every mod agrees on. Machine recipes ask for these rather than for our own
 * fluids, so another mod's honey or latex works in our machines, and ours in theirs.
 */
public final class HumanCommonFluidTags {

    public static final TagKey<Fluid> HONEY = create("honey");

    public static final TagKey<Fluid> LATEX = create("latex");

    private HumanCommonFluidTags() {}

    private static TagKey<Fluid> create(String name) {
        return TagKey.create(Registries.FLUID, CommonConstants.location(name));
    }
}
