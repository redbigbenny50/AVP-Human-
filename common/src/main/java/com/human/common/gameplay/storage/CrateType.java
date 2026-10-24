package com.human.common.gameplay.storage;

import com.human.HumanResources;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** One registered crate: a family, a colour or metal, and a size. */
public record CrateType(
    CrateFamily family,
    String variant,
    CrateSize size
) {

    /** Every crate and container in registration order (family, then size, then colour). */
    public static final List<CrateType> ALL = build();

    public String id() {
        return family.id(variant, size);
    }

    public String displayName() {
        return family.displayName(variant, size);
    }

    public ResourceLocation modelLocation() {
        return HumanResources.blockGeoModelLocation("crate/" + family.modelName(size));
    }

    /** Crate sheets are kept out of textures/block so they are not stitched into the block atlas. */
    public ResourceLocation textureLocation() {
        return HumanResources.location("textures/crate/" + id() + ".png");
    }

    private static List<CrateType> build() {
        var list = new ArrayList<CrateType>();

        for (var family : CrateFamily.values()) {
            for (var size : CrateSize.values()) {
                for (var variant : family.variants()) {
                    list.add(new CrateType(family, variant, size));
                }
            }
        }

        return Collections.unmodifiableList(list);
    }
}
