package com.human.mixin.client;

import com.human.client.render.cryo.CryoIcon;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The flat green icon for vanilla-model specimens: the mob's own texture swapped for its flat twin, and its render
 * layers (armour, held items, wool, villager clothing, eyes) left out - they would draw in their own colours over the
 * green.
 */
@Mixin(LivingEntityRenderer.class)
public abstract class MixinLivingEntityRenderer_CryoIcon {

    @Inject(method = "getRenderType", at = @At("HEAD"), cancellable = true)
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void avp_human$iconRenderType(
        LivingEntity entity,
        boolean bodyVisible,
        boolean translucent,
        boolean glowing,
        CallbackInfoReturnable<RenderType> callback
    ) {
        if (CryoIcon.isIcon(entity)) {
            var original = ((EntityRenderer) (Object) this).getTextureLocation(entity);
            callback.setReturnValue(RenderType.entityCutoutNoCull(CryoIcon.flatFor(original)));
        }
    }

    @Redirect(
        method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/entity/layers/RenderLayer;render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/Entity;FFFFFF)V"
        )
    )
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void avp_human$skipLayersOnIcons(
        RenderLayer layer,
        PoseStack poseStack,
        MultiBufferSource buffer,
        int packedLight,
        Entity entity,
        float limbSwing,
        float limbSwingAmount,
        float partialTick,
        float ageInTicks,
        float netHeadYaw,
        float headPitch
    ) {
        if (CryoIcon.isIcon(entity)) {
            return;
        }

        layer.render(poseStack, buffer, packedLight, entity, limbSwing, limbSwingAmount, partialTick, ageInTicks, netHeadYaw, headPitch);
    }
}
