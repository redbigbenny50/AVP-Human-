package com.human.client.fluid;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;

import java.util.HashMap;
import java.util.Map;

/**
 * One flat colour per fluid: the average of its still texture, times its tint. What the canister's window is tinted by.
 * <h2>⚠ WHY THE TEXTURE FILE IS READ, NOT THE ATLAS</h2> Vanilla 1.21.1 exposes no public way to read a stitched
 * sprite's pixels - the image data sits in private and package-private fields. The texture file itself is public
 * through the resource manager, so it is read from there once, averaged, and cached. The cost is one small PNG read per
 * fluid per session, the first time a canister of it is drawn.
 * <h2>⚠ BYTE ORDER</h2> {@code NativeImage.getPixelRGBA} returns ABGR despite its name (it reads the raw little-endian
 * RGBA bytes as one int), and it only works on RGBA images - so the image is read as RGBA explicitly and red is the LOW
 * byte.
 */
public final class FluidColors {

    private static final Map<Fluid, Integer> CACHE = new HashMap<>();

    private FluidColors() {}

    public static int colorOf(Fluid fluid) {
        synchronized (CACHE) {
            return CACHE.computeIfAbsent(fluid, FluidColors::compute);
        }
    }

    /** Forgets every colour - after a resource reload, a resource pack may have repainted a fluid. */
    public static void clear() {
        synchronized (CACHE) {
            CACHE.clear();
        }
    }

    private static int compute(Fluid fluid) {
        var info = FluidClientInfo.Holder.get();
        var tint = info.tint(fluid);
        var texture = info.stillTexture(fluid);
        var average = texture == null ? 0xFFFFFF : averageOf(texture);

        return 0xFF000000 | multiply(average, tint);
    }

    private static int averageOf(ResourceLocation sprite) {
        var file = ResourceLocation.fromNamespaceAndPath(sprite.getNamespace(), "textures/" + sprite.getPath() + ".png");
        var resource = Minecraft.getInstance().getResourceManager().getResource(file);

        if (resource.isEmpty()) {
            return 0xFFFFFF;
        }

        try (var stream = resource.get().open(); var image = NativeImage.read(NativeImage.Format.RGBA, stream)) {
            long red = 0L;
            long green = 0L;
            long blue = 0L;
            long weight = 0L;

            for (var y = 0; y < image.getHeight(); y++) {
                for (var x = 0; x < image.getWidth(); x++) {
                    var abgr = image.getPixelRGBA(x, y);
                    var alpha = (abgr >>> 24) & 0xFF;

                    if (alpha == 0) {
                        continue;
                    }

                    red += (long) (abgr & 0xFF) * alpha;
                    green += (long) ((abgr >> 8) & 0xFF) * alpha;
                    blue += (long) ((abgr >> 16) & 0xFF) * alpha;
                    weight += alpha;
                }
            }

            if (weight == 0L) {
                return 0xFFFFFF;
            }

            return (int) (red / weight) << 16 | (int) (green / weight) << 8 | (int) (blue / weight);
        } catch (Exception ignored) {
            return 0xFFFFFF;
        }
    }

    private static int multiply(int rgb, int tint) {
        var red = ((rgb >> 16) & 0xFF) * ((tint >> 16) & 0xFF) / 255;
        var green = ((rgb >> 8) & 0xFF) * ((tint >> 8) & 0xFF) / 255;
        var blue = (rgb & 0xFF) * (tint & 0xFF) / 255;

        return red << 16 | green << 8 | blue;
    }
}
