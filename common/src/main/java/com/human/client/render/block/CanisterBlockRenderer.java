package com.human.client.render.block;

import com.blib.api.client.render.v1.block.AzBlockEntityRenderer;
import com.blib.api.client.render.v1.block.AzBlockEntityRendererConfig;
import com.human.HumanResources;
import com.human.client.render.layer.CanisterFluidLayer;
import com.human.common.gameplay.block.entity.CanisterBlockEntity;
import net.minecraft.resources.ResourceLocation;

/**
 * A placed canister: his {@code canister} geo model, with the liquid drawn by {@link CanisterFluidLayer}.
 * <p>
 * ⚠ The fill groups are hidden with the renderer's BONE FILTER, not {@code AzBone.setHidden}. The baked model is shared
 * by every canister in the world; {@code setHidden} would change it for all of them, while the filter is asked per
 * canister, per frame. The builder is held in a variable because {@code setBoneVisibilityFilter} lives on the base
 * builder and returns the base type, which has no block-entity {@code build()}.
 * </p>
 */
public class CanisterBlockRenderer extends AzBlockEntityRenderer<CanisterBlockEntity> {

    public static final String NAME = "canister";

    private static final ResourceLocation MODEL_LOCATION = HumanResources.blockGeoModelLocation(NAME);

    private static final ResourceLocation TEXTURE = HumanResources.blockTextureLocation(NAME);

    public CanisterBlockRenderer() {
        super(config());
    }

    private static AzBlockEntityRendererConfig<CanisterBlockEntity> config() {
        var builder = AzBlockEntityRendererConfig.<CanisterBlockEntity>builder($ -> MODEL_LOCATION, $ -> TEXTURE);

        builder.addRenderLayer(new CanisterFluidLayer());
        builder.setBoneVisibilityFilter((bone, canister) -> bone.getName().startsWith(CanisterFluidLayer.FILL_BONE_PREFIX));

        return builder.build();
    }
}
