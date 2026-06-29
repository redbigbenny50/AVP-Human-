package com.human.client.render.entity;

import com.human.HumanResources;
import com.human.common.gameplay.entity.living.dog.MarineDog;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.WolfModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Crackiness;
import net.minecraft.world.item.AnimalArmorItem;
import net.minecraft.world.item.ItemStack;

import java.util.Map;

public class MarineDogArmorLayer extends RenderLayer<MarineDog, WolfModel<MarineDog>> {

    private static final ResourceLocation ARMOR_TEXTURE = HumanResources.entityTextureLocation("marine_dog_armor");

    private static final Map<Crackiness.Level, ResourceLocation> ARMOR_CRACK_LOCATIONS = Map.of(
        Crackiness.Level.LOW,
        ResourceLocation.withDefaultNamespace("textures/entity/wolf/wolf_armor_crackiness_low.png"),
        Crackiness.Level.MEDIUM,
        ResourceLocation.withDefaultNamespace("textures/entity/wolf/wolf_armor_crackiness_medium.png"),
        Crackiness.Level.HIGH,
        ResourceLocation.withDefaultNamespace("textures/entity/wolf/wolf_armor_crackiness_high.png")
    );

    private final WolfModel<MarineDog> model;

    public MarineDogArmorLayer(RenderLayerParent<MarineDog, WolfModel<MarineDog>> renderer, EntityModelSet models) {
        super(renderer);
        model = new WolfModel<>(models.bakeLayer(ModelLayers.WOLF_ARMOR));
    }

    @Override
    public void render(
        PoseStack poseStack,
        MultiBufferSource bufferSource,
        int packedLight,
        MarineDog marineDog,
        float limbSwing,
        float limbSwingAmount,
        float partialTick,
        float ageInTicks,
        float netHeadYaw,
        float headPitch
    ) {
        if (!marineDog.hasArmor()) {
            return;
        }

        var armorStack = marineDog.getBodyArmorItem();

        if (
            !(armorStack.getItem() instanceof AnimalArmorItem armorItem)
                || armorItem.getBodyType() != AnimalArmorItem.BodyType.CANINE
        ) {
            return;
        }

        getParentModel().copyPropertiesTo(model);
        model.prepareMobModel(marineDog, limbSwing, limbSwingAmount, partialTick);
        model.setupAnim(marineDog, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);

        VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(ARMOR_TEXTURE));
        model.renderToBuffer(poseStack, vertexConsumer, packedLight, OverlayTexture.NO_OVERLAY);
        renderCracks(poseStack, bufferSource, packedLight, armorStack);
    }

    private void renderCracks(PoseStack poseStack, MultiBufferSource buffer, int packedLight, ItemStack armorStack) {
        var crackinessLevel = Crackiness.WOLF_ARMOR.byDamage(armorStack);

        if (crackinessLevel == Crackiness.Level.NONE) {
            return;
        }

        var texture = ARMOR_CRACK_LOCATIONS.get(crackinessLevel);
        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.entityTranslucent(texture));
        model.renderToBuffer(poseStack, vertexConsumer, packedLight, OverlayTexture.NO_OVERLAY);
    }
}
