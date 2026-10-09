package com.human.mixin.client;

import com.blib.api.client.render.v1.AzLayerRenderer;
import com.blib.api.client.render.v1.AzRendererPipelineContext;
import com.human.client.render.cryo.CryoIcon;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Icons draw no render layers (glow layers and the like would draw in their own colours over the flat green). */
@Mixin(value = AzLayerRenderer.class, remap = false)
public abstract class MixinAzLayerRenderer_CryoIcon<K, T> {

    @Inject(method = "applyRenderLayers", at = @At("HEAD"), cancellable = true)
    private void avp_human$noLayersOnIcons(AzRendererPipelineContext<K, T> context, CallbackInfo callback) {
        var entity = context.animatable() instanceof Entity animatable ? animatable : context.currentEntity();

        if (entity != null && CryoIcon.isIcon(entity)) {
            callback.cancel();
        }
    }
}
