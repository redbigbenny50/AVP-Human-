package com.human.client.render.util;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Draws only the part of each quad BELOW a horizontal plane - how the fabricator builds an item up layer by layer
 * ([stated] Oct 13). The plane rises with the print, so the bottom appears first and the top last.
 * <p>
 * Works on whatever is drawn through it - block models, flat items, a gun's own 3D renderer - because it sits on the
 * vertex stream itself. Positions arrive already transformed (camera-relative, world-aligned), so "below the plane" is
 * simply a smaller y. Each quad is collected, clipped (Sutherland-Hodgman against one plane), and re-emitted as quads:
 * a clipped quad has 3 to 5 corners, sent as one quad (a triangle repeats its last corner) or two. Colour, texture,
 * overlay, light and normal are interpolated along the cut edge.
 * </p>
 * <p>
 * {@link #flush()} must be called after drawing, so the last quad - whose attributes arrive after its position - goes
 * out too.
 * </p>
 */
public final class ClippingVertexConsumer implements VertexConsumer {

    private final VertexConsumer delegate;

    private final float clipY;

    private final List<Vertex> quad = new ArrayList<>(4);

    private Vertex current;

    private static final class Vertex {

        float x, y, z, u, v, nx, ny, nz;

        int r = 255, g = 255, b = 255, a = 255, overlayU, overlayV, lightU, lightV;

        Vertex lerp(Vertex to, float t) {
            var out = new Vertex();

            out.x = x + (to.x - x) * t;
            out.y = y + (to.y - y) * t;
            out.z = z + (to.z - z) * t;
            out.u = u + (to.u - u) * t;
            out.v = v + (to.v - v) * t;
            out.nx = nx + (to.nx - nx) * t;
            out.ny = ny + (to.ny - ny) * t;
            out.nz = nz + (to.nz - nz) * t;
            out.r = Math.round(r + (to.r - r) * t);
            out.g = Math.round(g + (to.g - g) * t);
            out.b = Math.round(b + (to.b - b) * t);
            out.a = Math.round(a + (to.a - a) * t);
            out.overlayU = t < 0.5F ? overlayU : to.overlayU;
            out.overlayV = t < 0.5F ? overlayV : to.overlayV;
            out.lightU = t < 0.5F ? lightU : to.lightU;
            out.lightV = t < 0.5F ? lightV : to.lightV;

            return out;
        }
    }

    public ClippingVertexConsumer(VertexConsumer delegate, float clipY) {
        this.delegate = delegate;
        this.clipY = clipY;
    }

    /**
     * A buffer source whose every buffer is clipped at {@code clipY}. Flush the returned list after drawing.
     * <p>
     * ⚠ Vanilla's buffer source ENDS the previous buffer when a different render type is asked for, so every pending
     * quad is flushed before any buffer is handed out - a quad written late would go into a closed buffer and crash.
     * One clipper per render type, reused.
     * </p>
     */
    public static MultiBufferSource wrap(MultiBufferSource buffers, float clipY, List<ClippingVertexConsumer> opened) {
        var byType = new java.util.HashMap<RenderType, ClippingVertexConsumer>();

        return type -> {
            opened.forEach(ClippingVertexConsumer::flush);

            var underlying = buffers.getBuffer(type);

            // Only quad buffers are clipped - lines or triangles would be regrouped wrongly, so they pass through
            // whole.
            if (type.mode() != com.mojang.blaze3d.vertex.VertexFormat.Mode.QUADS) {
                return underlying;
            }
            var clipped = byType.get(type);

            if (clipped == null || clipped.delegate != underlying) {
                clipped = new ClippingVertexConsumer(underlying, clipY);
                byType.put(type, clipped);
                opened.add(clipped);
            }

            return clipped;
        };
    }

    @Override
    public @NotNull VertexConsumer addVertex(float x, float y, float z) {
        if (current != null) {
            quad.add(current);

            if (quad.size() == 4) {
                emit();
            }
        }

        current = new Vertex();
        current.x = x;
        current.y = y;
        current.z = z;

        return this;
    }

    @Override
    public @NotNull VertexConsumer setColor(int red, int green, int blue, int alpha) {
        if (current == null) {
            return this;
        }

        current.r = red;
        current.g = green;
        current.b = blue;
        current.a = alpha;

        return this;
    }

    @Override
    public @NotNull VertexConsumer setUv(float u, float v) {
        if (current == null) {
            return this;
        }

        current.u = u;
        current.v = v;

        return this;
    }

    @Override
    public @NotNull VertexConsumer setUv1(int u, int v) {
        if (current == null) {
            return this;
        }

        current.overlayU = u;
        current.overlayV = v;

        return this;
    }

    @Override
    public @NotNull VertexConsumer setUv2(int u, int v) {
        if (current == null) {
            return this;
        }

        current.lightU = u;
        current.lightV = v;

        return this;
    }

    @Override
    public @NotNull VertexConsumer setNormal(float x, float y, float z) {
        if (current == null) {
            return this;
        }

        current.nx = x;
        current.ny = y;
        current.nz = z;

        return this;
    }

    /** Sends the last quad on. */
    public void flush() {
        if (current != null) {
            quad.add(current);
            current = null;
        }

        if (quad.size() == 4) {
            emit();
        }

        quad.clear();
    }

    private void emit() {
        var kept = new ArrayList<Vertex>(5);

        for (int i = 0; i < 4; i++) {
            var from = quad.get(i);
            var to = quad.get((i + 1) % 4);
            var fromIn = from.y <= clipY;
            var toIn = to.y <= clipY;

            if (fromIn) {
                kept.add(from);
            }

            if (fromIn != toIn) {
                kept.add(from.lerp(to, (clipY - from.y) / (to.y - from.y)));
            }
        }

        quad.clear();

        if (kept.size() < 3) {
            return;
        }

        if (kept.size() == 3) {
            send(kept.get(0), kept.get(1), kept.get(2), kept.get(2));
        } else {
            send(kept.get(0), kept.get(1), kept.get(2), kept.get(3));

            if (kept.size() == 5) {
                send(kept.get(0), kept.get(3), kept.get(4), kept.get(4));
            }
        }
    }

    private void send(Vertex... corners) {
        for (var corner : corners) {
            delegate.addVertex(corner.x, corner.y, corner.z)
                .setColor(corner.r, corner.g, corner.b, corner.a)
                .setUv(corner.u, corner.v)
                .setUv1(corner.overlayU, corner.overlayV)
                .setUv2(corner.lightU, corner.lightV)
                .setNormal(corner.nx, corner.ny, corner.nz);
        }
    }
}
