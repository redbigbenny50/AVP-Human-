package com.human.mixin.client;

import com.human.client.input.GunInputHandler;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public class MixinMinecraft_GunControls {

    @Inject(method = "handleKeybinds", at = @At("HEAD"))
    private void avp_human$fireHeldGun(CallbackInfo callbackInfo) {
        GunInputHandler.tick((Minecraft) (Object) this);
    }

    @Inject(method = "startAttack", at = @At("HEAD"), cancellable = true)
    private void avp_human$cancelGunStartAttack(CallbackInfoReturnable<Boolean> callbackInfo) {
        if (GunInputHandler.isHoldingGun((Minecraft) (Object) this)) {
            callbackInfo.setReturnValue(false);
        }
    }

    @Inject(method = "continueAttack", at = @At("HEAD"), cancellable = true)
    private void avp_human$cancelGunContinueAttack(boolean attack, CallbackInfo callbackInfo) {
        if (GunInputHandler.isHoldingGun((Minecraft) (Object) this)) {
            callbackInfo.cancel();
        }
    }
}
