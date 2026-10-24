package com.human.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;

/**
 * [stated] Oct 20 - "shift right click should show the sphere through blocks too so you can actually see it". The
 * resonator's 32-block field drawn as a wire sphere (latitude and longitude rings) for {@value #SHOW_TICKS} ticks after
 * the wiring tool's shift-click, depth test OFF so it shows through terrain, fading out over its last second. Client
 * only; drawn from MixinLevelRenderer_ResonatorSphere. Nothing is drawn (or computed) while none is showing.
 */
public final class ResonatorSphereDisplay {

    private static final int SHOW_TICKS = 200;

    private static final int FADE_TICKS = 20;

    private static final int RINGS = 12;

    private static final int SEGMENTS = 64;

    /** resonator position -> {expiry game time, radius}. */
    private static final Map<BlockPos, long[]> SHOWING = new HashMap<>();

    private ResonatorSphereDisplay() {}

    public static void show(BlockPos pos, int radius) {
        var level = Minecraft.getInstance().level;

        if (level != null) {
            SHOWING.put(pos.immutable(), new long[] { level.getGameTime() + SHOW_TICKS, radius });
        }
    }

    public static void render(Vec3 camera) {
        if (SHOWING.isEmpty()) {
            return;
        }

        var level = Minecraft.getInstance().level;

        if (level == null) {
            SHOWING.clear();
            return;
        }

        var now = level.getGameTime();
        SHOWING.values().removeIf(entry -> entry[0] <= now);

        if (SHOWING.isEmpty()) {
            return;
        }

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getRendertypeLinesShader);
        RenderSystem.lineWidth(2.0F);

        var buffer = Tesselator.getInstance().begin(VertexFormat.Mode.LINES, DefaultVertexFormat.POSITION_COLOR_NORMAL);

        for (var entry : SHOWING.entrySet()) {
            var remaining = entry.getValue()[0] - now;
            var alpha = (int) (200 * Math.min(1.0D, remaining / (double) FADE_TICKS));
            var radius = (float) entry.getValue()[1];
            var centre = Vec3.atCenterOf(entry.getKey()).subtract(camera);

            sphere(buffer, (float) centre.x, (float) centre.y, (float) centre.z, radius, alpha);
        }

        var mesh = buffer.build();

        if (mesh != null) {
            BufferUploader.drawWithShader(mesh);
        }

        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        RenderSystem.lineWidth(1.0F);
    }

    private static void sphere(com.mojang.blaze3d.vertex.BufferBuilder buffer, float cx, float cy, float cz, float r, int alpha) {
        // Latitude rings (horizontal circles), then longitude rings (vertical circles through the poles).
        for (var ring = 1; ring < RINGS; ring++) {
            var phi = Math.PI * ring / RINGS;
            var y = (float) (r * Math.cos(phi));
            var ringRadius = (float) (r * Math.sin(phi));

            for (var s = 0; s < SEGMENTS; s++) {
                var a0 = 2 * Math.PI * s / SEGMENTS;
                var a1 = 2 * Math.PI * (s + 1) / SEGMENTS;
                line(
                    buffer,
                    cx + ringRadius * (float) Math.cos(a0),
                    cy + y,
                    cz + ringRadius * (float) Math.sin(a0),
                    cx + ringRadius * (float) Math.cos(a1),
                    cy + y,
                    cz + ringRadius * (float) Math.sin(a1),
                    alpha
                );
            }
        }

        for (var ring = 0; ring < RINGS; ring++) {
            var theta = Math.PI * ring / RINGS;
            var dx = (float) Math.cos(theta);
            var dz = (float) Math.sin(theta);

            for (var s = 0; s < SEGMENTS; s++) {
                var a0 = 2 * Math.PI * s / SEGMENTS;
                var a1 = 2 * Math.PI * (s + 1) / SEGMENTS;
                line(
                    buffer,
                    cx + r * (float) Math.sin(a0) * dx,
                    cy + r * (float) Math.cos(a0),
                    cz + r * (float) Math.sin(a0) * dz,
                    cx + r * (float) Math.sin(a1) * dx,
                    cy + r * (float) Math.cos(a1),
                    cz + r * (float) Math.sin(a1) * dz,
                    alpha
                );
            }
        }
    }

    /** One line in the resonator's colour - a cold violet, the "frequency" of the device. */
    private static void line(
        com.mojang.blaze3d.vertex.BufferBuilder buffer,
        float x0,
        float y0,
        float z0,
        float x1,
        float y1,
        float z1,
        int alpha
    ) {
        var nx = x1 - x0;
        var ny = y1 - y0;
        var nz = z1 - z0;
        var length = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);

        if (length <= 0.0F) {
            return;
        }

        nx /= length;
        ny /= length;
        nz /= length;
        buffer.addVertex(x0, y0, z0).setColor(150, 110, 255, alpha).setNormal(nx, ny, nz);
        buffer.addVertex(x1, y1, z1).setColor(150, 110, 255, alpha).setNormal(nx, ny, nz);
    }
}
