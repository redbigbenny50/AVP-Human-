package com.human.mixin.client;

import com.human.client.input.GunZoomHandler;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public class MixinGameRenderer_GunZoom {

    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
    private void avp_human$applyGunZoom(
        Camera camera,
        float tickDelta,
        boolean changingFov,
        CallbackInfoReturnable<Double> callbackInfo
    ) {
        if (!changingFov) {
            return;
        }

        callbackInfo.setReturnValue(GunZoomHandler.applyZoom(callbackInfo.getReturnValue(), Minecraft.getInstance()));
    }
}
