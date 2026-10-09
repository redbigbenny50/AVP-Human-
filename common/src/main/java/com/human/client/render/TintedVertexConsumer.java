package com.human.client.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;

/**
 * A vertex consumer that blends every colour it is given toward a highlight tint.
 * <p>
 * ⚠⚠ ONLY {@code setColor(int,int,int,int)} IS OVERRIDDEN, AND THAT IS SUFFICIENT ON PURPOSE. Every other colour method
 * on VertexConsumer - the float overload, the packed-int overload, setWhiteAlpha - is a {@code default} method that
 * funnels into this one. Overriding the defaults as well would double-apply the tint on any renderer that uses them.
 * </p>
 * <p>
 * ⚠ A BLEND, NOT A REPLACEMENT. Replacing the colour outright flattens the model to a single silhouette and throws away
 * shading, so a xenomorph becomes an amber cut-out. Blending keeps the lighting and the texture readable underneath
 * while still colouring the whole creature.
 * </p>
 */
public class TintedVertexConsumer implements VertexConsumer {

    private final VertexConsumer delegate;

    private final int tintRed;

    private final int tintGreen;

    private final int tintBlue;

    private final float strength;

    private final int overlayPacked;

    public TintedVertexConsumer(VertexConsumer delegate, int tintColor, float strength, int overlayPacked) {
        this.delegate = delegate;
        this.tintRed = (tintColor >> 16) & 0xFF;
        this.tintGreen = (tintColor >> 8) & 0xFF;
        this.tintBlue = tintColor & 0xFF;
        this.strength = Math.max(0.0F, Math.min(1.0F, strength));
        this.overlayPacked = overlayPacked;
    }

    @Override
    public VertexConsumer setColor(int red, int green, int blue, int alpha) {
        // ⚠ ALPHA IS PASSED THROUGH UNTOUCHED. Tinting it would make the target translucent, which is a different
        // effect entirely and would let terrain show through a creature that is standing in front of it.
        delegate.setColor(blend(red, tintRed), blend(green, tintGreen), blend(blue, tintBlue), alpha);
        return this;
    }

    private int blend(int original, int tint) {
        return Math.round(original * (1.0F - strength) + tint * strength);
    }

    @Override
    public VertexConsumer addVertex(float x, float y, float z) {
        delegate.addVertex(x, y, z);
        return this;
    }

    @Override
    public VertexConsumer setUv(float u, float v) {
        delegate.setUv(u, v);
        return this;
    }

    /**
     * ⭐ THE OVERLAY IS THE ONLY CHANNEL THAT CAN ADD LIGHT, but it does NOT give the tint headroom - it paints over it,
     * and getting that backwards is what turned a crusher into a white silhouette.
     * <p>
     * ⚠⚠ READ THE SHADER ORDER BEFORE CHANGING ANY OF THIS. rendertype_entity_cutout.fsh:
     *
     * <pre>
     *     25:  color *= vertexColor                                    &lt;- the tint
     *     26:  color.rgb = mix(overlayColor.rgb, color.rgb, alpha)     &lt;- the lift
     * </pre>
     *
     * The lift runs AFTER the tint. It does not brighten the model so the colour can act on it; it MIXES WHITE OVER the
     * finished colour. At u=15 that is 75% pure white and the tint is all but gone - which is exactly what a xenomorph
     * rendered as a white cut-out was telling us.
     * </p>
     * <p>
     * ⭐⭐ THE OVERLAY TEXTURE HAS TWO ZONES AND ONLY ONE OF THEM CAN ADD A HUE. Rows 8-15 are WHITE at an alpha that
     * fades with u, so they can only wash a model out - brighter always meant whiter, never greener, which is what
     * defeated every attempt at a green tint. Rows 0-7 are PURE RED at a fixed alpha of 178, painting 30% red over
     * whatever is underneath.
     * <p>
     * That red is the only saturated colour reachable without a post-process, and it is enormous next to what white
     * could do: on a black drone it gives a red-over-green lead of 88 where the best green ever managed was 34.
     * </p>
     * <p>
     * ⚠ WHICH IS WHY THE ROW IS PASSED IN PACKED, not computed here. The caller picks the zone - red to mark the
     * target, white to flash it on a hit - and this only forwards it.
     * </p>
     * <p>
     * ⚠ ROW 10, NOT ROW 3. Rows below 8 are the RED hurt flash (0xB2 alpha over pure red) - using one of those would
     * fight the tint with red. {@code OverlayTexture.v(false)} is row 10, the plain white-flash row.
     * </p>
     * <p>
     * ⚠ THIS DOES SUPPRESS THE HURT FLASH ON THE LOCKED TARGET. It is already lit and coloured, so the flash had little
     * left to say - and the health bar in the target panel reports damage far more precisely than a red blink.
     * </p>
     */
    @Override
    public VertexConsumer setUv1(int u, int v) {
        delegate.setUv1(overlayPacked & 0xFFFF, overlayPacked >> 16 & 0xFFFF);
        return this;
    }

    /**
     * ⭐⭐ THIS IS WHAT ACTUALLY MAKES THE HIGHLIGHT VISIBLE, AND THE COLOUR TINT ALONE NEVER COULD.
     * <p>
     * The final pixel is texture x vertexColor x light. Vertex colour is a MULTIPLIER, and renderers hand it in as
     * white - so tinting it can only ever REMOVE light from a channel, never add any. Against a xenomorph, whose
     * texture is nearly black, a multiply changes nothing you can see: black times amber is still black. That is why
     * the tint was invisible in the world and yet showed up under thermal, where the shader recolours everything from a
     * classification pass rather than from the lit texture.
     * </p>
     * <p>
     * Forcing the lightmap coordinate to full brightness lifts the model out of the ambient lighting entirely, and THEN
     * the colour multiplier has something to tint. Bright first, coloured second.
     * </p>
     * <p>
     * ⚠ FULL_BRIGHT IS 0xF000F0 - block light 15 in the low bits, sky light 15 in the high bits. Passing 0xFFFFFF or
     * similar overruns the lightmap texture and produces sampling artefacts rather than a brighter model.
     * </p>
     */
    @Override
    public VertexConsumer setUv2(int u, int v) {
        delegate.setUv2(LightTexture.FULL_BRIGHT & 0xFFFF, LightTexture.FULL_BRIGHT >> 16 & 0xFFFF);
        return this;
    }

    @Override
    public VertexConsumer setNormal(float normalX, float normalY, float normalZ) {
        delegate.setNormal(normalX, normalY, normalZ);
        return this;
    }
}
