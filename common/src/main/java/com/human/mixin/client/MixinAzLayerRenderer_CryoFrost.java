package com.human.mixin.client;

import com.blib.api.client.render.v1.AzLayerRenderer;
import com.blib.api.client.render.v1.AzRendererPipelineContext;
import com.human.client.render.cryo.AzFrostOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Frost on BLib-rendered specimens. {@code AzEntityRenderer} has no vanilla render layers, so - exactly as avp_predator
 * does for the net - this hooks the pipeline's own layer pass, after the mod's layers.
 */
@Mixin(value = AzLayerRenderer.class, remap = false)
public abstract class MixinAzLayerRenderer_CryoFrost<K, T> {

    @Inject(method = "applyRenderLayers", at = @At("TAIL"))
    private void avp_human$frostOverlay(AzRendererPipelineContext<K, T> context, CallbackInfo callback) {
        AzFrostOverlay.render(context);
    }
}
