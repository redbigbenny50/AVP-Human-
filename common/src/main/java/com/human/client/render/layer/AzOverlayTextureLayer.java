package com.human.client.render.layer;

import com.blib.api.client.model.v1.AzBone;
import com.blib.api.client.render.v1.AzRendererPipelineContext;
import com.blib.api.client.render.v1.layer.AzRenderLayer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Function;

/**
 * Re-renders the whole model with a SECOND texture laid over the first.
 * <h2>⚠⚠ WHY THE SECOND TEXTURE MUST BE IN THE MODEL'S OWN UV SPACE</h2> A layer cannot swap the baked model -
 * {@code AzRendererPipelineContext} exposes {@code bakedModel()} as a getter only, and the single {@code setBakedModel}
 * in BLib lives on the internal bone cache. So a layer always redraws the SAME geometry with the SAME UVs; the only
 * thing it can change is the texture and the render type.
 * <p>
 * That is exactly how {@code AzAutoGlowingLayer} works, and it is why the battery's animated plate texture had to be
 * rebuilt from a 16-wide strip into the model's 64-wide space. A 16x96 sheet would have had its UVs read against
 * {@code texture_width: 64} and sampled a sixth of the intended area. The rebuilt sheet is transparent everywhere
 * except the plate, so redrawing the whole model with it paints only the plate - no bone filtering needed.
 * </p>
 * <p>
 * ⚠ Animated textures still work through this path: the mcmeta drives the frame, and the UV simply points into
 * whichever frame is current.
 * </p>
 */
public class AzOverlayTextureLayer<K, T> implements AzRenderLayer<K, T> {

    private final Function<T, ResourceLocation> textureProvider;

    private final boolean emissive;

    public AzOverlayTextureLayer(Function<T, ResourceLocation> textureProvider, boolean emissive) {
        this.textureProvider = textureProvider;
        this.emissive = emissive;
    }

    @Override
    public void preRender(AzRendererPipelineContext<K, T> context) {}

    @Override
    public void renderForBone(AzRendererPipelineContext<K, T> context, AzBone bone) {}

    @Override
    public void render(AzRendererPipelineContext<K, T> context) {
        var texture = textureProvider.apply(context.animatable());

        if (texture == null) {
            return;
        }

        // ⚠⚠ CUTOUT, NOT TRANSLUCENT. entityTranslucent BLENDS, and a blended pass still writes DEPTH for pixels
        // that are fully transparent - so redrawing the whole model with a mask that is empty except for the plate
        // stamped depth across every face and left a hollow shell you could see the ground through.
        //
        // entityCutoutNoCull alpha-TESTS instead: transparent fragments are discarded outright and write nothing at
        // all, which is what a mask overlay needs. NoCull because the overlay redraws the same geometry and must not
        // fight the base pass over back-face winding.
        // ⚠⚠ CUTOUT WITH CULLING. Two separate mistakes were made here, in order:
        //
        // 1. entityTranslucent BLENDS, and a blended pass writes DEPTH even for fully transparent pixels - so
        // redrawing the whole model with a mask that is empty except the plate stamped depth over every face and
        // left a hollow shell.
        // 2. entityCutoutNoCull fixed that but disabled BACK-FACE CULLING, so the pass drew the model's INTERIOR
        // surfaces on top of its exterior - the side ports appeared to sit inside the block.
        //
        // entityCutout is both: alpha-TESTED, so transparent fragments are discarded and write nothing, and CULLED,
        // so only outward-facing surfaces are drawn - matching how the base pass renders the same geometry.
        var renderType = emissive ? RenderType.eyes(texture) : RenderType.entityCutout(texture);

        context.setRenderType(renderType);
        context.setVertexConsumer(context.multiBufferSource().getBuffer(renderType));

        if (emissive) {
            context.setPackedLight(net.minecraft.client.renderer.LightTexture.FULL_SKY);
        }

        context.setPackedOverlay(OverlayTexture.NO_OVERLAY);
        context.rendererPipeline().reRender(context);
    }
}
