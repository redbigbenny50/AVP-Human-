package com.human.mixin.client;

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
    }
}
