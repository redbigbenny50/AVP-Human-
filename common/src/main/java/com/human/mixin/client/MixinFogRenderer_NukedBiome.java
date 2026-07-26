package com.human.mixin.client;

import com.human.client.effect.NukedBiomeFog;
import com.mojang.blaze3d.shaders.FogShape;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.material.FogType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FogRenderer.class)
public class MixinFogRenderer_NukedBiome {

    private static final float TERRAIN_FOG_DISTANCE = 72.0F;

    private static final float SKY_FOG_DISTANCE = 112.0F;

    @Inject(method = "setupFog", at = @At("TAIL"))
    private static void applyNukedBiomeFog(
        Camera camera,
        FogRenderer.FogMode fogMode,
        float farPlaneDistance,
        boolean shouldCreateFog,
        float partialTick,
        CallbackInfo callbackInfo
    ) {
        var blend = NukedBiomeFog.getBlend(partialTick);
        if (blend <= 0.0F || camera.getFluidInCamera() != FogType.NONE) {
            return;
        }

        var vanillaStart = RenderSystem.getShaderFogStart();
        var vanillaEnd = RenderSystem.getShaderFogEnd();
        var fogDistance = fogMode == FogRenderer.FogMode.FOG_SKY ? SKY_FOG_DISTANCE : TERRAIN_FOG_DISTANCE;
        var targetStart = Math.min(12.0F, farPlaneDistance * 0.08F);
        var targetEnd = Math.min(fogDistance, farPlaneDistance);
        RenderSystem.setShaderFogStart(Mth.lerp(blend, vanillaStart, targetStart));
        RenderSystem.setShaderFogEnd(Mth.lerp(blend, vanillaEnd, targetEnd));
        RenderSystem.setShaderFogShape(FogShape.SPHERE);
    }
}
