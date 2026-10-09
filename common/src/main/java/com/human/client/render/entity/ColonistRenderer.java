package com.human.client.render.entity;

import com.blib.api.client.render.v1.entity.AzEntityRenderer;
import com.blib.api.client.render.v1.entity.AzEntityRendererConfig;
import com.human.HumanResources;
import com.human.client.animation.entity.ColonistAnimator;
import com.human.client.render.layer.human.HumanArmorLayer;
import com.human.client.render.layer.human.HumanBeardLayer;
import com.human.client.render.layer.human.HumanEyesLayer;
import com.human.client.render.layer.human.HumanHairLayer;
import com.human.client.render.layer.human.HumanItemLayer;
import com.human.client.render.layer.human.HumanOutfitLayer;
import com.human.client.render.layer.human.HumanSkinLayer;
import com.human.common.gameplay.entity.living.human.colonist.ColonistEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/** The colonist: male and female models, the marines' tinted layers, and whatever its job has it wear. */
public class ColonistRenderer extends AzEntityRenderer<ColonistEntity> {

    private static final ResourceLocation MALE_MODEL = HumanResources.location("geo/entity/colonist_male.geo.json");

    private static final ResourceLocation FEMALE_MODEL = HumanResources.location("geo/entity/colonist_female.geo.json");

    private static final ResourceLocation BASE_TEXTURE = HumanResources.location("textures/entity/colonist/base.png");

    public ColonistRenderer(EntityRendererProvider.Context context) {
        super(
            AzEntityRendererConfig.<ColonistEntity>builder(ColonistRenderer::getModel, entity -> BASE_TEXTURE)
                .setAnimatorProvider(ColonistAnimator::new)
                .addRenderLayer(new HumanArmorLayer<>())
                .addRenderLayer(new HumanSkinLayer<>())
                .addRenderLayer(new HumanEyesLayer<>())
                .addRenderLayer(new HumanHairLayer<>())
                .addRenderLayer(new HumanBeardLayer<>())
                .addRenderLayer(new HumanOutfitLayer<>())
                .addRenderLayer(new HumanItemLayer<>())
                .build(),
            context
        );
    }

    private static ResourceLocation getModel(ColonistEntity entity) {
        return entity.isMale.get() ? MALE_MODEL : FEMALE_MODEL;
    }
}
