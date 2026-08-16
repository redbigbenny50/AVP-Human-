package com.human.mixin.client;

import com.human.client.effect.NukeClientEffects;
import com.human.client.effect.VoxelGunEffects;
import com.human.client.input.GunInputHandler;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public class MixinMinecraft_GunControls {

    @Inject(method = "tick", at = @At("RETURN"))
    private void avp_human$tickClientEffects(CallbackInfo callbackInfo) {
        NukeClientEffects.clientTick((Minecraft) (Object) this);
        VoxelGunEffects.clientTick((Minecraft) (Object) this);
    }

    @Inject(method = "startAttack", at = @At("HEAD"), cancellable = true)
    private void avp_human$cancelGunStartAttack(CallbackInfoReturnable<Boolean> callbackInfo) {
        if (GunInputHandler.isHoldingGun((Minecraft) (Object) this)) {
            callbackInfo.setReturnValue(false);
        }
    }

    /**
     * Fires the held gun, and stops vanilla swinging at the same time.
     * <p>
     * ⚠⚠ THE FIRING USED TO RUN FROM {@code handleKeybinds} HEAD READING THE KEY DIRECTLY, AND THAT BROKE CONTROLLERS.
     * The {@code attack} parameter here is vanilla's fully-resolved attack signal, which is where Controllable (and any
     * other input mod) injects its controller button - patching the {@code isDown()} call inside the argument
     * expression rather than the KeyMapping. Reading the key ourselves bypassed all of it.
     */
    @Inject(method = "continueAttack", at = @At("HEAD"), cancellable = true)
    private void avp_human$cancelGunContinueAttack(boolean attack, CallbackInfo callbackInfo) {
        var minecraft = (Minecraft) (Object) this;

        GunInputHandler.tick(minecraft, attack);

        if (GunInputHandler.isHoldingGun(minecraft)) {
            callbackInfo.cancel();
        }
    }
}
