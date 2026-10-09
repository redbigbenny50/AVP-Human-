package com.human.mixin.client;

import com.human.client.camera.SentryCameraClient;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * [stated] Oct 5: smooth sentry remote control - the turret's aim is applied every FRAME, right before the world is
 * drawn, instead of only on game ticks (see {@link SentryCameraClient#applyViewThisFrame()}).
 */
@Mixin(GameRenderer.class)
public class MixinGameRenderer_SentryCamera {

    @Inject(method = "render", at = @At("HEAD"))
    private void avp_human$aimSentryThisFrame(
        DeltaTracker deltaTracker,
        boolean renderLevel,
        CallbackInfo callbackInfo
    ) {
        SentryCameraClient.applyViewThisFrame();
    }
}
