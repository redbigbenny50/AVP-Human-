package com.human.mixin.client;

import com.human.client.camera.SentryCameraClient;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * [stated] Oct 2 - looking through a turret's camera. The controls are read at the START of the tick, before vanilla
 * handles the key bindings; while looking, attack fires the turret instead of swinging, and use does nothing.
 */
@Mixin(Minecraft.class)
public class MixinMinecraft_SentryCamera {

    @Inject(method = "tick", at = @At("HEAD"))
    private void avp_human$tickSentryCamera(CallbackInfo callbackInfo) {
        SentryCameraClient.tick();
    }

    @Inject(method = "startAttack", at = @At("HEAD"), cancellable = true)
    private void avp_human$noAttackWhileViewing(CallbackInfoReturnable<Boolean> callbackInfo) {
        if (SentryCameraClient.isActive()) {
            callbackInfo.setReturnValue(false);
        }
    }

    @Inject(method = "continueAttack", at = @At("HEAD"), cancellable = true)
    private void avp_human$noMiningWhileViewing(boolean attack, CallbackInfo callbackInfo) {
        if (SentryCameraClient.isActive()) {
            callbackInfo.cancel();
        }
    }

    @Inject(method = "startUseItem", at = @At("HEAD"), cancellable = true)
    private void avp_human$noUseWhileViewing(CallbackInfo callbackInfo) {
        if (SentryCameraClient.isActive()) {
            callbackInfo.cancel();
        }
    }
}
