package com.human.client.render.entity;

import com.blib.api.client.render.v1.entity.AzEntityRenderer;
import com.blib.api.client.render.v1.entity.AzEntityRendererConfig;
import com.blib.api.client.render.v1.entity.pipeline.AzEntityRendererPipeline;
import com.human.HumanResources;
import com.human.client.render.layer.human.HumanArmorLayer;
import com.human.client.render.layer.human.HumanBeardLayer;
import com.human.client.render.layer.human.HumanEyesLayer;
import com.human.client.render.layer.human.HumanHairLayer;
import com.human.client.render.layer.human.HumanItemLayer;
import com.human.client.render.layer.human.HumanSkinLayer;
import com.human.common.gameplay.entity.living.human.AbstractHuman;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public abstract class AbstractHumanRenderer<T extends AbstractHuman> extends AzEntityRenderer<T> {

    private static final String NAME = "marine";

    private static final ResourceLocation MALE_MODEL = HumanResources.entityGeoModelLocation(NAME + "_male");

    private static final ResourceLocation FEMALE_MODEL = HumanResources.entityGeoModelLocation(NAME + "_female");

    private static final ResourceLocation TEXTURE = HumanResources.entityTextureLocation("human/base");

    public static <T extends AbstractHuman> ResourceLocation getModel(T entity) {
        return entity.isMale.get() ? MALE_MODEL : FEMALE_MODEL;
    }

    public static <T extends AbstractHuman> ResourceLocation getTexture(T entity) {
        return TEXTURE;
    }

    public AbstractHumanRenderer(EntityRendererProvider.Context context) {
        this(AbstractHumanRenderer.createConfig(), context);
    }

    public AbstractHumanRenderer(AzEntityRendererConfig.Builder<T> builder, EntityRendererProvider.Context context) {
        super(builder.build(), context);
    }

    protected static <T extends AbstractHuman> AzEntityRendererConfig.Builder<T> createConfig() {
        return AzEntityRendererConfig.<T>builder(AbstractHumanRenderer::getModel, AbstractHumanRenderer::getTexture)
            // The human model renderer adds vanilla's swimming body tilt; everything else is BLib's default.
            .setModelRenderer(
                (pipeline, layerRenderer) -> new HumanEntityModelRenderer<>(
                    (AzEntityRendererPipeline<T>) pipeline,
                    layerRenderer
                )
            )
            .addRenderLayer(new HumanArmorLayer<>())
            .addRenderLayer(new HumanHairLayer<>())
            .addRenderLayer(new HumanEyesLayer<>())
            .addRenderLayer(new HumanSkinLayer<>())
            .addRenderLayer(new HumanBeardLayer<>())
            .addRenderLayer(new HumanItemLayer<>());
    }
}
