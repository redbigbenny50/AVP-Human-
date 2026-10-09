package com.human.client.render.cord;

import com.human.HumanResources;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * Draws the titanium capture cord as a braided titanium cable. [stated] Oct 2: textured from the cord item's own rope
 * strip, thicker than a lead but nowhere near chain thick.
 * <p>
 * Built like avp_alien's capture-chain ribbon (two perpendicular textured strips around a sagging curve, so it reads
 * from every angle), but anchored exactly where vanilla anchors a lead, so switching between a lead and a cord does not
 * move the attachment point on the mob.
 * </p>
 */
public final class CaptureCordRopeRenderer {

    private static final ResourceLocation TEXTURE = HumanResources.location("textures/entity/titanium_capture_cord_rope.png");

    private static final int SEGMENTS = 20;

    /** A lead is 0.025 across; the capture chain is 0.4. */
    private static final float HALF_WIDTH = 0.045F;

    /** The rope strip is 32x8, so one tile is four widths long - this keeps the braid square. */
    private static final double TILE_LENGTH = HALF_WIDTH * 2.0 * 4.0;

    private static final double SAG_FACTOR = 0.12;

    private static final double SAG_MAX = 0.9;

    private CaptureCordRopeRenderer() {}

    /** Called with the pose stack at the held entity's render origin, as vanilla's own {@code renderLeash} is. */
    public static void render(Entity entity, float partialTick, PoseStack poseStack, MultiBufferSource buffers, Entity holder) {
        // Vanilla's lead anchor, verbatim.
        var bodyAngle = (double) (entity.getPreciseBodyRotation(partialTick) * (float) (Math.PI / 180.0)) + (Math.PI / 2);
        var offset = entity.getLeashOffset(partialTick);
        var dx = Math.cos(bodyAngle) * offset.z + Math.sin(bodyAngle) * offset.x;
        var dz = Math.sin(bodyAngle) * offset.z - Math.cos(bodyAngle) * offset.x;

        var entityPos = new Vec3(
            Mth.lerp(partialTick, entity.xo, entity.getX()),
            Mth.lerp(partialTick, entity.yo, entity.getY()),
            Mth.lerp(partialTick, entity.zo, entity.getZ())
        );

        var start = new Vec3(dx, offset.y, dz);
        var end = holder.getRopeHoldPosition(partialTick).subtract(entityPos);
        var light = LevelRenderer.getLightColor(entity.level(), entity.blockPosition());

        draw(poseStack, buffers, start, end, light);
    }

    private static void draw(PoseStack poseStack, MultiBufferSource buffers, Vec3 start, Vec3 end, int light) {
        var distance = start.distanceTo(end);

        if (distance < 1.0e-4) {
            return;
        }

        var sag = Math.min(distance * SAG_FACTOR, SAG_MAX);
        var points = new Vec3[SEGMENTS + 1];

        for (int i = 0; i <= SEGMENTS; i++) {
            var t = (double) i / SEGMENTS;
            points[i] = new Vec3(
                start.x + (end.x - start.x) * t,
                start.y + (end.y - start.y) * t - sag * 4.0 * t * (1.0 - t),
                start.z + (end.z - start.z) * t
            );
        }

        var aMinus = new Vec3[SEGMENTS + 1];
        var aPlus = new Vec3[SEGMENTS + 1];
        var bMinus = new Vec3[SEGMENTS + 1];
        var bPlus = new Vec3[SEGMENTS + 1];

        for (int i = 0; i <= SEGMENTS; i++) {
            var tangent = points[Math.min(i + 1, SEGMENTS)].subtract(points[Math.max(i - 1, 0)]);
            var length = tangent.length();
            tangent = length < 1.0e-5 ? new Vec3(0.0, 0.0, 1.0) : tangent.scale(1.0 / length);

            var reference = Math.abs(tangent.y) > 0.99 ? new Vec3(1.0, 0.0, 0.0) : new Vec3(0.0, 1.0, 0.0);
            var perpA = tangent.cross(reference);
            perpA = perpA.scale(HALF_WIDTH / perpA.length());
            var perpB = tangent.cross(perpA);
            perpB = perpB.scale(HALF_WIDTH / perpB.length());

            aMinus[i] = points[i].subtract(perpA);
            aPlus[i] = points[i].add(perpA);
            bMinus[i] = points[i].subtract(perpB);
            bPlus[i] = points[i].add(perpB);
        }

        VertexConsumer buffer = buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        var pose = poseStack.last();
        var u = 0.0F;

        for (int i = 0; i < SEGMENTS; i++) {
            var segment = points[i + 1].subtract(points[i]);
            var segmentLength = segment.length();

            if (segmentLength < 1.0e-5) {
                continue;
            }

            var direction = segment.scale(1.0 / segmentLength);
            var uNext = u + (float) (segmentLength / TILE_LENGTH);
            var nx = (float) direction.x;
            var ny = (float) direction.y;
            var nz = (float) direction.z;

            quad(buffer, pose, aMinus[i], aPlus[i], aPlus[i + 1], aMinus[i + 1], u, uNext, light, nx, ny, nz);
            quad(buffer, pose, bMinus[i], bPlus[i], bPlus[i + 1], bMinus[i + 1], u, uNext, light, nx, ny, nz);
            u = uNext;
        }
    }

    private static void quad(
        VertexConsumer buffer,
        PoseStack.Pose pose,
        Vec3 p0,
        Vec3 p1,
        Vec3 p2,
        Vec3 p3,
        float u0,
        float u1,
        int light,
        float nx,
        float ny,
        float nz
    ) {
        vertex(buffer, pose, p0, u0, 0.0F, light, nx, ny, nz);
        vertex(buffer, pose, p1, u0, 1.0F, light, nx, ny, nz);
        vertex(buffer, pose, p2, u1, 1.0F, light, nx, ny, nz);
        vertex(buffer, pose, p3, u1, 0.0F, light, nx, ny, nz);
    }

    private static void vertex(
        VertexConsumer buffer,
        PoseStack.Pose pose,
        Vec3 p,
        float u,
        float v,
        int light,
        float nx,
        float ny,
        float nz
    ) {
        buffer.addVertex(pose, (float) p.x, (float) p.y, (float) p.z)
            .setColor(255, 255, 255, 255)
            .setUv(u, v)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(light)
            .setNormal(pose, nx, ny, nz);
    }
}
