package com.human.mixin.client;

import com.human.client.render.ResonatorSphereDisplay;
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
 * [stated] Oct 20 - draws a resonator's field sphere (ResonatorSphereDisplay) at the same point in the frame as the
 * quarry hologram and the gun voxels: world space, after the level. Returns at once while no sphere is showing.
 */
@Mixin(LevelRenderer.class)
public class MixinLevelRenderer_ResonatorSphere {

    @Inject(
        method = "renderLevel",
        at = @At(
            value = "INVOKE",
            target = "Lorg/joml/Matrix4fStack;popMatrix()Lorg/joml/Matrix4fStack;",
            shift = At.Shift.BEFORE
        )
    )
    private void avp_human$renderResonatorSphere(
        DeltaTracker deltaTracker,
        boolean renderBlockOutline,
        Camera camera,
        GameRenderer gameRenderer,
        LightTexture lightTexture,
        Matrix4f projectionMatrix,
        Matrix4f positionMatrix,
        CallbackInfo callbackInfo
    ) {
        ResonatorSphereDisplay.render(camera.getPosition());
    }
}
