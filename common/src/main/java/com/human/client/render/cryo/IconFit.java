package com.human.client.render.cryo;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Draws a CRYOLINK specimen icon shrunk to fit its frame. [stated] Oct 2: the model is scaled down IN CODE to fit.
 * <p>
 * A mob's hitbox says nothing about its tail or crest, so the model's real size is MEASURED: it is "drawn" once into a
 * buffer that only records where every vertex lands, in the pose it is actually being shown in, facing the viewer and
 * turned as far each way as the mouse can turn it. The icon is then drawn as vanilla draws a mob in the inventory -
 * turning to follow the mouse - but scaled and centred on that measured size.
 * </p>
 */
public final class IconFit {

    /** Leaves a little air between the model and the frame. */
    private static final float MARGIN = 0.88F;

    /**
     * [stated] Oct 2: small specimens stay small - the eggs should not fill the frame just because they could. A frame
     * never shows LESS than this many blocks, so anything smaller than two blocks is drawn at its true size against
     * that, and only bigger mobs are shrunk to fit.
     */
    private static final float MIN_BLOCKS_SHOWN = 2.0F;

    private IconFit() {}

    public static void draw(GuiGraphics graphics, LivingEntity mob, int x0, int y0, int x1, int y1, float mouseX, float mouseY) {
        var bounds = boundsFor(mob);

        if (bounds == null) {
            return;
        }

        var width = Math.max(0.1F, bounds.maxX - bounds.minX);
        var height = Math.max(0.1F, bounds.maxY - bounds.minY);
        var scale = Math.min((x1 - x0) / width, (y1 - y0) / height) * MARGIN;
        scale = Math.min(scale, Math.min(x1 - x0, y1 - y0) / MIN_BLOCKS_SHOWN);

        var cx = (x0 + x1) / 2.0F;
        var cy = (y0 + y1) / 2.0F;

        // Vanilla's InventoryScreen.renderEntityInInventoryFollowsMouse, with the measured centre and fitted scale.
        graphics.enableScissor(x0, y0, x1, y1);
        var turn = (float) Math.atan((cx - mouseX) / 40.0F);
        var tilt = (float) Math.atan((cy - mouseY) / 40.0F);
        var pose = new Quaternionf().rotateZ((float) Math.PI);
        var camera = new Quaternionf().rotateX(tilt * 20.0F * (float) (Math.PI / 180.0));
        pose.mul(camera);

        var saved = Saved.of(mob);
        face(mob, 180.0F + turn * 20.0F, 180.0F + turn * 40.0F, -tilt * 20.0F);

        // After the flip the model's +x is the screen's -x and its +y is up, so the measured centre goes straight in.
        var centre = new Vector3f((bounds.minX + bounds.maxX) / 2.0F, (bounds.minY + bounds.maxY) / 2.0F, 0.0F);
        InventoryScreen.renderEntityInInventory(graphics, cx, cy, scale, centre, pose, camera, mob);

        saved.restore(mob);
        graphics.disableScissor();
    }

    /** Re-measured this often, so the fit follows the idle as it settles without measuring every single frame. */
    private static final long REMEASURE_MS = 250L;

    private static final java.util.Map<LivingEntity, Timed> CACHE = new java.util.WeakHashMap<>();

    private record Timed(
        Bounds bounds,
        long at
    ) {}

    /**
     * The model's extent AS SEEN on screen - measured facing the viewer and at both ends of the turn the mouse can give
     * it, and the three put together. Fitting the turned extents, not a circle round the feet, keeps a mob with a long
     * tail behind it as large as its body allows, and the fit does not change size as the mouse moves.
     */
    private static Bounds boundsFor(LivingEntity mob) {
        var now = System.currentTimeMillis();
        var cached = CACHE.get(mob);

        if (cached != null && now - cached.at() < REMEASURE_MS) {
            return cached.bounds();
        }

        var saved = Saved.of(mob);
        Bounds union = null;

        for (var turn : new float[] { -31.0F, 0.0F, 31.0F }) {
            face(mob, 180.0F + turn, 180.0F + turn * 2.0F, 0.0F);
            var measured = measure(mob);

            if (measured != null) {
                union = union == null ? measured : union.with(measured);
            }
        }

        saved.restore(mob);

        if (union != null) {
            CACHE.put(mob, new Timed(union, now));
        }

        return union;
    }

    private static void face(LivingEntity mob, float bodyYaw, float yaw, float pitch) {
        mob.yBodyRot = bodyYaw;
        mob.yBodyRotO = bodyYaw;
        mob.setYRot(yaw);
        mob.setXRot(pitch);
        mob.yHeadRot = yaw;
        mob.yHeadRotO = yaw;
    }

    private record Saved(
        float bodyRot,
        float bodyRotO,
        float yRot,
        float xRot,
        float headRot,
        float headRotO
    ) {

        static Saved of(LivingEntity mob) {
            return new Saved(mob.yBodyRot, mob.yBodyRotO, mob.getYRot(), mob.getXRot(), mob.yHeadRot, mob.yHeadRotO);
        }

        void restore(LivingEntity mob) {
            mob.yBodyRot = bodyRot;
            mob.yBodyRotO = bodyRotO;
            mob.setYRot(yRot);
            mob.setXRot(xRot);
            mob.yHeadRot = headRot;
            mob.yHeadRotO = headRotO;
        }
    }

    private record Bounds(
        float minX,
        float maxX,
        float minY,
        float maxY
    ) {

        Bounds with(Bounds other) {
            return new Bounds(
                Math.min(minX, other.minX),
                Math.max(maxX, other.maxX),
                Math.min(minY, other.minY),
                Math.max(maxY, other.maxY)
            );
        }
    }

    /** The model's extent, in blocks, from a capture-only render. Null if it drew nothing. */
    private static Bounds measure(LivingEntity mob) {
        var capture = new Capture();
        var dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        dispatcher.setRenderShadow(false);

        try {
            dispatcher.render(mob, 0.0, 0.0, 0.0, 0.0F, 1.0F, new PoseStack(), capture, 0xF000F0);
        } catch (RuntimeException ignored) {
            // A renderer that cannot draw outside a level just gets no icon this frame.
            return null;
        } finally {
            dispatcher.setRenderShadow(true);
        }

        return capture.any ? new Bounds(capture.minX, capture.maxX, capture.minY, capture.maxY) : null;
    }

    /** A buffer source whose every buffer only records vertex positions. */
    private static final class Capture implements MultiBufferSource, VertexConsumer {

        boolean any;

        float minX = Float.MAX_VALUE;

        float maxX = -Float.MAX_VALUE;

        float minY = Float.MAX_VALUE;

        float maxY = -Float.MAX_VALUE;

        @Override
        public VertexConsumer getBuffer(RenderType renderType) {
            return this;
        }

        @Override
        public VertexConsumer addVertex(float x, float y, float z) {
            any = true;
            minX = Math.min(minX, x);
            maxX = Math.max(maxX, x);
            minY = Math.min(minY, y);
            maxY = Math.max(maxY, y);
            return this;
        }

        @Override
        public VertexConsumer setColor(int red, int green, int blue, int alpha) {
            return this;
        }

        @Override
        public VertexConsumer setUv(float u, float v) {
            return this;
        }

        @Override
        public VertexConsumer setUv1(int u, int v) {
            return this;
        }

        @Override
        public VertexConsumer setUv2(int u, int v) {
            return this;
        }

        @Override
        public VertexConsumer setNormal(float x, float y, float z) {
            return this;
        }
    }
}
