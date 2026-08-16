package com.human.mixin.client;

import com.human.client.input.DaggerInputHandler;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Routes the mouse buttons for daggers.
 * <p>
 * A separate mixin from the gun controls rather than more methods on that one: the two are unrelated features that
 * happen to want the same hooks, and merging them would mean every future change to either risks the other.
 */
@Mixin(Minecraft.class)
public class MixinMinecraft_DaggerControls {

    /**
     * Duel wield mode: every other left click is spent by the off hand instead of the main one.
     */
    @Inject(method = "startAttack", at = @At("HEAD"), cancellable = true)
    private void avp_human$alternateDaggerAttack(CallbackInfoReturnable<Boolean> callbackInfo) {
        if (DaggerInputHandler.tryTakeAttackWithOffHand((Minecraft) (Object) this)) {
            callbackInfo.setReturnValue(true);
        }
    }

    /**
     * The off-hand stab. Cancelled at HEAD so the click never reaches vanilla's use-item handling, which would
     * otherwise try to place a block or eat something with whatever the main hand is holding.
     */
    @Inject(method = "startUseItem", at = @At("HEAD"), cancellable = true)
    private void avp_human$stabWithOffHandDagger(CallbackInfo callbackInfo) {
        if (DaggerInputHandler.tryStabWithOffHand((Minecraft) (Object) this)) {
            callbackInfo.cancel();
        }
    }
}
