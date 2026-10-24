package com.human.mixin.client;

import com.blib.api.client.model.v1.AzBone;
import com.blib.api.client.render.v1.AzModelRenderer;
import com.blib.api.client.render.v1.AzRendererPipelineContext;
import com.human.client.render.cryo.AzFrostOverlay;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * During the frost pass every bone is drawn with the frost, whatever texture the model gives that bone of its own.
 * <p>
 * [Oct 19] The queen had the Frozen Solid effect (checked in game) but no frost, while raid xenos, yautja and vanilla
 * mobs all showed it. {@code getOrRefreshRenderBuffer} is where BLib picks each bone's buffer, and a bone with a
 * texture override of its own is drawn in THAT texture in every pass - so the frost re-render redrew the normal skin on
 * those bones. While {@link AzFrostOverlay#inFrostPass()} is set this returns the frost buffer the pass set up instead,
 * with no texture override (so no override UV remapping either). Outside the frost pass nothing changes.
 * </p>
 */
@Mixin(value = AzModelRenderer.class, remap = false)
public abstract class MixinAzModelRenderer_FrostPass<K, T> {

    @Inject(method = "getOrRefreshRenderBuffer", at = @At("HEAD"), cancellable = true)
    private void avp_human$frostEveryBone(
        boolean isReRender,
        AzRendererPipelineContext<K, T> context,
        AzBone bone,
        CallbackInfoReturnable<VertexConsumer> cir
    ) {
        if (AzFrostOverlay.inFrostPass()) {
            context.setTextureOverride(null);
            cir.setReturnValue(context.vertexConsumer());
        }
    }
}
