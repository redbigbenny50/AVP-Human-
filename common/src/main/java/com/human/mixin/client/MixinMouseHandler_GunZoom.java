package com.human.mixin.client;

import com.human.client.input.GunZoomHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public class MixinMouseHandler_GunZoom {

    @Shadow
    @Final
    private Minecraft minecraft;

    @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
    private void avp_human$zoomGun(long window, double scrollX, double scrollY, CallbackInfo callbackInfo) {
        if (window != minecraft.getWindow().getWindow() || !GunZoomHandler.shouldHandleScroll(minecraft)) {
            return;
        }

        if (GunZoomHandler.shouldZoomOnScroll(minecraft)) {
            GunZoomHandler.adjust(scrollY);
        }

        callbackInfo.cancel();
    }
}
