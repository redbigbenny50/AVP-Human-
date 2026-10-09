package com.human.mixin.client;

import com.blib.internal.client.territory.BLibClaimHud;
import com.human.common.gameplay.item.gun.targeting.SmartTargeting;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Keeps BLib's territory claim readout off the screen while a smart optic is up.
 * <p>
 * ⚠⚠ THE CLAIM TEXT IS AN ACTION-BAR MESSAGE, NOT A HUD ELEMENT. {@code BLibClaimHud.tick} ends in
 * {@code LocalPlayer.displayClientMessage(component, true)}, so there is nothing to reposition or hide at draw time -
 * by then it is just the vanilla overlay message, indistinguishable from any other. The only precise place to stop it
 * is BEFORE it is sent, which is why this cancels the tick rather than a render.
 * </p>
 * <p>
 * ⚠ CANCELLING THE TICK IS SAFE. Everything it does is derived fresh from the player's current chunk each time; there
 * is no accumulated state to corrupt by skipping it, and it resumes normally the moment the weapon is stowed.
 * </p>
 * <p>
 * ⚠ A MESSAGE ALREADY ON SCREEN WILL FINISH FADING - vanilla owns that timer, and yanking it would mean clearing the
 * overlay message outright, which would also eat unrelated messages from other mods.
 * </p>
 */
@Mixin(BLibClaimHud.class)
public class MixinBLibClaimHud_HideForSmartHud {

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true, remap = false)
    private static void avp_human$suppressWhileSmartHudActive(CallbackInfo callbackInfo) {
        var player = Minecraft.getInstance().player;

        if (player != null && SmartTargeting.hasTracker(player.getMainHandItem())) {
            callbackInfo.cancel();
        }
    }
}
