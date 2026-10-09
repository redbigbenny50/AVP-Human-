package com.human.mixin.client;

import com.human.client.effect.NukeClientEffects;
import com.human.client.render.hud.SmartHudOverlay;
import com.human.client.render.hud.SniperScopeOverlay;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public class MixinGui_SniperScopeOverlay {

    @Inject(method = "render", at = @At("RETURN"))
    private void avp_human$renderSniperScopeOverlay(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo callbackInfo) {
        SniperScopeOverlay.render(guiGraphics, deltaTracker);
        // ⚠ ONE HUD HOOK, NOT ONE PER OVERLAY. This mixin is already the mod's post-Gui draw point; adding a second
        // @Inject on Gui.render for the smart tracker would make the draw ORDER between them depend on mixin apply
        // order, which is not something to leave to chance.
        SmartHudOverlay.render(guiGraphics, deltaTracker);
        // [stated] Oct 2 - looking through a sentry turret's camera.
        com.human.client.camera.SentryCameraClient.renderHud(guiGraphics);
        NukeClientEffects.renderFlash(guiGraphics, deltaTracker);
    }
}
