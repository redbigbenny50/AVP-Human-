package com.human.client.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;

/**
 * Hands out tinted vertex consumers in place of ordinary ones.
 * <p>
 * ⭐⭐ THIS IS WHY THE HIGHLIGHT NEEDS NO PER-MOD CODE. It is substituted at the DISPATCHER, before any renderer is
 * called, so whatever the renderer is - a vanilla LivingEntityRenderer, one of BLib's AzEntityRenderers, or something a
 * mod adds next year - it asks this for its buffers and gets tinted ones. Verified against avp_alien: every xenomorph
 * renderer extends {@code AzEntityRenderer}, which extends {@code EntityRenderer} and NOT {@code LivingEntityRenderer},
 * so a mixin on the living renderer would have coloured vanilla mobs and silently missed every xenomorph.
 * </p>
 * <p>
 * ⚠ NOT A FRAMEBUFFER PASS. Vanilla's glowing outline is a post-process, which is the category shader packs routinely
 * break - the reason the predator vision needed the whole classification-pass framework to survive Iris. This is
 * ordinary geometry going through the ordinary pipeline, so it is depth tested (walls occlude it for free, which is the
 * stated requirement) and there is no post chain for a shader pack to replace.
 * </p>
 */
public record TintedMultiBufferSource(
    MultiBufferSource delegate,
    int tintColor,
    float strength,
    int overlayPacked
) implements MultiBufferSource {

    /**
     * ⚠⚠ THE FLAME IS NOT PART OF THE CREATURE. {@code EntityRenderDispatcher.render} hands this same buffer source to
     * {@code renderFlame} immediately after the entity itself (line 163), so a burning target would have its fire quads
     * forced to the mark's overlay and full brightness as well - which turns flames into flat opaque slabs. Fire
     * already renders at full brightness with its own animation; it needs nothing from us.
     * </p>
     * <p>
     * ⚠ MATCHED BY RENDER TYPE, not by a flag, because the dispatcher gives no other signal about which part of the
     * entity is being drawn.
     * </p>
     */
    @Override
    public VertexConsumer getBuffer(RenderType renderType) {
        if (renderType == Sheets.cutoutBlockSheet()) {
            return delegate.getBuffer(renderType);
        }

        return new TintedVertexConsumer(delegate.getBuffer(renderType), tintColor, strength, overlayPacked);
    }
}
