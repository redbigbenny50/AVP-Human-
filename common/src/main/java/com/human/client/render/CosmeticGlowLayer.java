package com.human.client.render;

import com.blib.api.client.render.v1.AzRendererPipelineContext;
import com.blib.api.client.render.v1.layer.AzAutoGlowingLayer;
import com.blib.api.client.texture.v1.AzAbstractTexture;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * BLib's glow layer for the cosmetic pieces, drawn only for a skin whose {@code _glowmask} exists AND has at least one
 * visible pixel.
 * <p>
 * ⚠ Nov 1 crash: 60 cosmetic glowmasks (20 designs x 3 editions - storage cabinet, lockers, fridge, specimen jars...)
 * were entirely transparent, and BLib refuses an empty glow layer: "Invalid glow layer texture provided, must have at
 * least one pixel!" the first time one was drawn (an inventory slot was enough). Deleting the file is not a fix on its
 * own: with no glowmask, BLib uses a copy of the WHOLE base texture as the glow, so the piece would draw full-bright.
 * One shared renderer draws every design, so the choice is made per skin as it is drawn: no usable glowmask, no glow.
 * </p>
 * <p>
 * Each glowmask is checked once and remembered (a resource pack swapped in mid-game is picked up on restart). Render
 * thread only.
 * </p>
 */
public class CosmeticGlowLayer<K, T> extends AzAutoGlowingLayer<K, T> {

    private static final Map<ResourceLocation, Boolean> USABLE = new ConcurrentHashMap<>();

    @Override
    public void render(AzRendererPipelineContext<K, T> context) {
        var texture = context.rendererPipeline().config().textureLocation(context.currentEntity(), context.animatable());
        if (texture != null && hasGlow(texture)) {
            super.render(context);
        }
    }

    /** Whether the glowmask BLib would use for {@code texture} is there and lights at least one pixel. */
    public static boolean hasGlow(ResourceLocation texture) {
        return USABLE.computeIfAbsent(AzAbstractTexture.getEmissiveResource(texture), CosmeticGlowLayer::check);
    }

    private static boolean check(ResourceLocation glowmask) {
        var resource = Minecraft.getInstance().getResourceManager().getResource(glowmask);
        if (resource.isEmpty()) {
            return false;
        }
        try (var in = resource.get().open(); var image = NativeImage.read(in)) {
            for (int y = 0; y < image.getHeight(); y++) {
                for (int x = 0; x < image.getWidth(); x++) {
                    // NativeImage pixels are ABGR: alpha is the top byte.
                    if ((image.getPixelRGBA(x, y) >>> 24) != 0) {
                        return true;
                    }
                }
            }
            return false;
        } catch (IOException exception) {
            return false;
        }
    }
}
