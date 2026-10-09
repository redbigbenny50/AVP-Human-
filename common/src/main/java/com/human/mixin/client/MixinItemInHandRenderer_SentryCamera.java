package com.human.mixin.client;

import com.human.client.camera.SentryCameraClient;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Oct 7 - [tester] "whilst holding an item and in sentry cam it appears as if held in the sentry view".
 * <p>
 * Vanilla draws the first-person hands whenever the camera mode is first person, whatever entity the camera is on - it
 * only skips them for spectators and sleepers. Looking through a turret moves the camera onto the turret but leaves the
 * player in first-person mode, so the player's held item was drawn over the turret's view. While the sentry camera is
 * active, no hands or held items are drawn.
 * </p>
 * <p>
 * Cut at {@code ItemInHandRenderer.renderHandsWithItems}, where the hands and held items are actually drawn, rather
 * than one step up at GameRenderer.renderItemInHand, so a renderer that draws the hand pass itself (a shader pack's
 * pipeline, for one) is covered too as long as it draws through this method. Not verified against Iris.
 * </p>
 */
@Mixin(ItemInHandRenderer.class)
public class MixinItemInHandRenderer_SentryCamera {

    @Inject(method = "renderHandsWithItems", at = @At("HEAD"), cancellable = true)
    private void avp_human$noHandsInSentryView(
        float partialTicks,
        PoseStack poseStack,
        MultiBufferSource.BufferSource bufferSource,
        LocalPlayer player,
        int packedLight,
        CallbackInfo callbackInfo
    ) {
        if (SentryCameraClient.isActive()) {
            callbackInfo.cancel();
        }
    }
}
