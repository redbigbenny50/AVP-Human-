package com.human.common.gameplay.entity.living.human.colonist;

import com.human.HumanResources;
import com.human.common.gameplay.entity.living.human.AbstractHuman;
import com.human.common.gameplay.entity.living.human.HumanFeatureManager;
import net.minecraft.resources.ResourceLocation;

/**
 * [stated] Sep 30: "there will be a few different jumpsuits underneath - they randomly spawn with; the profession
 * strictly comes from the armour". Every outfit texture in {@code textures/entity/colonist/<male|female>/outfit/} is in
 * the pool - dropping in a new jumpsuit texture needs no code - and each colonist's pick comes from its own id, so it
 * keeps the same jumpsuit for good and every player sees the same one, with nothing to sync.
 * <p>
 * The pool is listed on the client (the only side that draws it) by {@link #installOutfitCatalog}, called from client
 * start-up - common code never touches client classes, which a dedicated server does not have.
 * </p>
 * The colonist's textures: the marines' tint pipeline, with the colonist's own skin, clothes, hair and beard (ported
 * from the dropship mod with the colonist, now under {@code avp_human:textures/entity/colonist/}).
 */
public class ColonistFeatureManager extends HumanFeatureManager {

    private static final ResourceLocation EYES = texture("generic/eyes/eyes");

    private final AbstractHuman entity;

    public ColonistFeatureManager(AbstractHuman entity) {
        super(entity);
        this.entity = entity;
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

    /**
     * Male or female -> every outfit texture there, sorted. Until the client installs the real listing: the known two.
     */
    private static java.util.function.Function<Boolean, java.util.List<ResourceLocation>> outfitCatalog = male -> male
        ? java.util.List.of(texture("male/outfit/colonist"), texture("male/outfit/colonist_male1"))
        : java.util.List.of(texture("female/outfit/colonist"), texture("female/outfit/colonist_female1"));

    public static void installOutfitCatalog(java.util.function.Function<Boolean, java.util.List<ResourceLocation>> catalog) {
        outfitCatalog = catalog;
    }

    @Override
    public ResourceLocation getOutfitTexture() {
        var male = entity.isMale.get();
        var pool = outfitCatalog.apply(male);

        if (pool.isEmpty()) {
            return male ? texture("male/outfit/colonist_male1") : texture("female/outfit/colonist_female1");
        }

        // The colonist's own id picks - the same jumpsuit every time, on every client.
        return pool.get(Math.floorMod(entity.getUUID().hashCode(), pool.size()));
    }

    @Override
    public ResourceLocation getSkinTexture() {
        return entity.isMale.get() ? texture("male/skin/skin_colonist_male") : texture("female/skin/skin_colonist_female");
    }

    private static ResourceLocation texture(String path) {
        return HumanResources.location("textures/entity/colonist/" + path + ".png");
    }
}
