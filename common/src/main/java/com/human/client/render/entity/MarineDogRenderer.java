package com.human.client.render.entity;

import com.human.common.gameplay.entity.living.dog.MarineDog;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.WolfModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;

public class MarineDogRenderer extends MobRenderer<MarineDog, WolfModel<MarineDog>> {

    public MarineDogRenderer(EntityRendererProvider.Context context) {
        super(context, new WolfModel<>(context.bakeLayer(ModelLayers.WOLF)), 0.5F);
        addLayer(new MarineDogArmorLayer(this, context.getModelSet()));
    }

    @Override
    protected float getBob(MarineDog marineDog, float partialTick) {
        return marineDog.getTailAngle();
    }

    @Override
    public void render(
        MarineDog marineDog,
        float entityYaw,
        float partialTicks,
        PoseStack poseStack,
        MultiBufferSource buffer,
        int packedLight
    ) {
        if (marineDog.isWet()) {
            var wetShade = marineDog.getWetShade(partialTicks);
            model.setColor(FastColor.ARGB32.colorFromFloat(1.0F, wetShade, wetShade, wetShade));
        }

        super.render(marineDog, entityYaw, partialTicks, poseStack, buffer, packedLight);

        if (marineDog.isWet()) {
            model.setColor(-1);
        }
    }

    @Override
    public ResourceLocation getTextureLocation(MarineDog marineDog) {
        return marineDog.getTexture();
    }
}
