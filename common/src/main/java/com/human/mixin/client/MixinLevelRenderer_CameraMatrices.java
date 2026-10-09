package com.human.mixin.client;

import com.human.client.render.CameraMatrices;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Captures the camera and its two matrices at the top of the world render, for the smart HUD to project with.
 * <p>
 * ⚠ HEAD, NOT RETURN. Both matrices are METHOD PARAMETERS here, which is the only place they are handed over whole -
 * and taking them at HEAD means they are stored before anything in the world render can mutate them.
 * </p>
 */
@Mixin(LevelRenderer.class)
public class MixinLevelRenderer_CameraMatrices {

    @Inject(method = "renderLevel", at = @At("HEAD"))
    private void avp_human$captureCameraMatrices(
        DeltaTracker deltaTracker,
        boolean renderBlockOutline,
        Camera camera,
        GameRenderer gameRenderer,
        LightTexture lightTexture,
        Matrix4f frustumMatrix,
        Matrix4f projectionMatrix,
        CallbackInfo callbackInfo
    ) {
        CameraMatrices.capture(camera.getPosition(), frustumMatrix, projectionMatrix);
    }
}
