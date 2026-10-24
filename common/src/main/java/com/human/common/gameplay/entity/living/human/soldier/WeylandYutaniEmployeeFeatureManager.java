package com.human.common.gameplay.entity.living.human.soldier;

import com.human.HumanResources;
import com.human.common.gameplay.entity.living.human.AbstractHuman;
import com.human.common.gameplay.entity.living.human.HumanFeatureManager;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.function.Function;

/**
 * The Weyland-Yutani soldiers' body textures, from {@code textures/entity/weyland_yutani_employee/} - the colonist's
 * look copied under its own name ([stated] Oct 13), so the two can be repainted apart. Same rules as the colonist's
 * ColonistFeatureManager: every jumpsuit texture in the outfit folder is in the pool (no code to add one), and each
 * soldier's own id picks, so it keeps the same jumpsuit and every player sees the same one, with nothing to sync.
 * <p>
 * The pool is listed on the client (the only side that draws it) by {@link #installOutfitCatalog}; common code never
 * touches client classes.
 * </p>
 */
public class WeylandYutaniEmployeeFeatureManager extends HumanFeatureManager {

    private static final String FOLDER = "textures/entity/weyland_yutani_employee/";

    private static final ResourceLocation EYES = texture("generic/eyes/eyes");

    private static final ResourceLocation MALE_OUTFIT = texture("male/outfit/weyland_yutani_employee_male1");

    private static final ResourceLocation FEMALE_OUTFIT = texture("female/outfit/weyland_yutani_employee_female1");

    /**
     * Male or female -> every outfit texture there, sorted. Until the client installs the real listing: the known two.
     */
    private static Function<Boolean, List<ResourceLocation>> outfitCatalog = male -> male
        ? List.of(texture("male/outfit/weyland_yutani_employee"), MALE_OUTFIT)
        : List.of(texture("female/outfit/weyland_yutani_employee"), FEMALE_OUTFIT);

    private final AbstractHuman entity;

    public WeylandYutaniEmployeeFeatureManager(AbstractHuman entity) {
        super(entity);
        this.entity = entity;
    }

    public static void installOutfitCatalog(Function<Boolean, List<ResourceLocation>> catalog) {
        outfitCatalog = catalog;
    }

    @Override
    public ResourceLocation getEyesTexture() {
        return EYES;
    }

    @Override
    public ResourceLocation getBeardTextureOrNull() {
        var beard = entity.getBeardVariantOrNull();

        return beard == null ? null : texture("male/beard/beard_" + beard);
    }

    @Override
    public ResourceLocation getHairTexture() {
        return texture((entity.isMale.get() ? "male" : "female") + "/hair/hair_" + entity.hairVariant.get());
    }

    @Override
    public ResourceLocation getOutfitTexture() {
        var male = entity.isMale.get();
        var pool = outfitCatalog.apply(male);

        if (pool.isEmpty()) {
            return male ? MALE_OUTFIT : FEMALE_OUTFIT;
        }

        // The soldier's own id picks - the same jumpsuit every time, on every client.
        return pool.get(Math.floorMod(entity.getUUID().hashCode(), pool.size()));
    }

    @Override
    public ResourceLocation getSkinTexture() {
        return entity.isMale.get()
            ? texture("male/skin/skin_weyland_yutani_employee_male")
            : texture("female/skin/skin_weyland_yutani_employee_female");
    }

    private static ResourceLocation texture(String path) {
        return HumanResources.location(FOLDER + path + ".png");
    }
}
