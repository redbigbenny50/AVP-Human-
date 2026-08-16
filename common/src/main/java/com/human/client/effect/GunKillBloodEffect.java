package com.human.client.effect;

import com.human.common.network.packet.S2CGunKillEffectPayload;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Kill-only blood spray. A jet's line, flight physics, collision and decal share one trajectory.
 */
public final class GunKillBloodEffect {

    private static final int MAX_JETS = 160;

    private static final int MAX_SPLATS = 64;

    private static final List<BloodJet> JETS = new ArrayList<>();

    private static final List<BloodSplat> SPLATS = new ArrayList<>();

    public static void spawn(S2CGunKillEffectPayload payload) {
        var minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null || minecraft.player.distanceToSqr(payload.position()) > 128.0D * 128.0D) {
            return;
        }

        var random = RandomSource.create(payload.seed());
        var baseDirection = payload.direction().normalize();
        var color = colorFor(payload.fluidType());
        // A normal-sized mob should produce a sharp burst, not a wall of blood.
        var count = Math.min(payload.burstCount(), 18);
        for (int index = 0; index < count; index++) {
            var scatter = randomUnitVector(random);
            var direction = baseDirection.scale(0.58D).add(scatter.scale(0.82D)).normalize();
            var velocity = direction.scale(0.12D + random.nextDouble() * 0.16D)
                .add(0.0D, 0.08D + random.nextDouble() * 0.12D, 0.0D);
            addJet(new BloodJet(payload.position(), velocity, 0.20F + random.nextFloat() * 0.60F, 140, color));
        }
    }

    public static void tick(Minecraft minecraft) {
        for (Iterator<BloodJet> iterator = JETS.iterator(); iterator.hasNext();) {
            var jet = iterator.next();
            if (!jet.tick(minecraft)) {
                iterator.remove();
            }
        }
        for (Iterator<BloodSplat> iterator = SPLATS.iterator(); iterator.hasNext();) {
            var splat = iterator.next();
            splat.age++;
            if (splat.age >= splat.lifetime) {
                iterator.remove();
            }
        }
    }

    public static void render(PoseStack poseStack, MultiBufferSource buffers, Vec3 cameraPosition, float partialTick) {
        var matrix = poseStack.last().pose();
        var lineVertices = buffers.getBuffer(RenderType.lines());
        for (var jet : JETS) {
            // A jet stays solid while it is flying. It vanishes only on its own impact,
            // where it immediately leaves its splat.
            var alpha = 0.92F;
            addLine(
                lineVertices,
                matrix,
                jet.tail.subtract(cameraPosition),
                jet.head.add(jet.velocity.scale(partialTick)).subtract(cameraPosition),
                jet.color,
                alpha
            );
        }

        var quadVertices = buffers.getBuffer(RenderType.debugQuads());
        for (var splat : SPLATS) {
            var alpha = 0.78F * (1.0F - ((splat.age + partialTick) / splat.lifetime));
            addSplat(quadVertices, matrix, splat.position.subtract(cameraPosition), splat.normal, splat.radius, splat.color, alpha);
        }
    }

    private static void addJet(BloodJet jet) {
        if (JETS.size() >= MAX_JETS) {
            JETS.removeFirst();
        }
        JETS.add(jet);
    }

    private static void addSplat(BloodSplat splat) {
        for (var existing : SPLATS) {
            if (existing.normal.equals(splat.normal) && existing.position.distanceToSqr(splat.position) < 0.08D) {
                return;
            }
        }
        if (SPLATS.size() >= MAX_SPLATS) {
            SPLATS.removeFirst();
        }
        SPLATS.add(splat);
    }

    private static Vec3 randomUnitVector(RandomSource random) {
        return new Vec3(random.nextDouble() * 2.0D - 1.0D, random.nextDouble() * 2.0D - 1.0D, random.nextDouble() * 2.0D - 1.0D)
            .normalize();
    }

    static float[] colorFor(int fluidType) {
        return switch (fluidType) {
            // These are the base RGB values used by AVP-Alien's AcidParticle, BlueAcidParticle and
            // IrradiatedAcidParticle.
            case 1 -> new float[] { 160.0F / 255.0F, 158.0F / 255.0F, 9.0F / 255.0F };
            case 2, 3, 4 -> new float[] { 78.0F / 255.0F, 101.0F / 255.0F, 229.0F / 255.0F };
            default -> new float[] { 0.92F, 0.015F, 0.02F };
        };
    }

    private static void addLine(VertexConsumer vertices, org.joml.Matrix4f matrix, Vec3 start, Vec3 end, float[] color, float alpha) {
        vertices.addVertex(matrix, (float) start.x, (float) start.y, (float) start.z)
            .setColor(color[0], color[1], color[2], alpha)
            .setNormal(0.0F, 1.0F, 0.0F);
        vertices.addVertex(matrix, (float) end.x, (float) end.y, (float) end.z)
            .setColor(color[0], color[1], color[2], alpha)
            .setNormal(0.0F, 1.0F, 0.0F);
    }

    private static void addSplat(
        VertexConsumer vertices,
        org.joml.Matrix4f matrix,
        Vec3 center,
        Vec3 normal,
        float radius,
        float[] color,
        float alpha
    ) {
        var firstAxis = Math.abs(normal.y) < 0.9D
            ? normal.cross(new Vec3(0.0D, 1.0D, 0.0D)).normalize()
            : normal.cross(new Vec3(1.0D, 0.0D, 0.0D)).normalize();
        var secondAxis = normal.cross(firstAxis).normalize();
        addQuad(
            vertices,
            matrix,
            center.add(firstAxis.scale(-radius)).add(secondAxis.scale(-radius * 0.62D)),
            center.add(firstAxis.scale(radius)).add(secondAxis.scale(-radius * 0.62D)),
            center.add(firstAxis.scale(radius)).add(secondAxis.scale(radius * 0.62D)),
            center.add(firstAxis.scale(-radius)).add(secondAxis.scale(radius * 0.62D)),
            color,
            alpha
        );
    }

    private static void addQuad(
        VertexConsumer vertices,
        org.joml.Matrix4f matrix,
        Vec3 a,
        Vec3 b,
        Vec3 c,
        Vec3 d,
        float[] color,
        float alpha
    ) {
        vertices.addVertex(matrix, (float) a.x, (float) a.y, (float) a.z)
            .setColor(color[0] * 0.72F, color[1] * 0.72F, color[2] * 0.72F, alpha)
            .setNormal(0.0F, 1.0F, 0.0F);
        vertices.addVertex(matrix, (float) b.x, (float) b.y, (float) b.z)
            .setColor(color[0] * 0.72F, color[1] * 0.72F, color[2] * 0.72F, alpha)
            .setNormal(0.0F, 1.0F, 0.0F);
        vertices.addVertex(matrix, (float) c.x, (float) c.y, (float) c.z)
            .setColor(color[0] * 0.72F, color[1] * 0.72F, color[2] * 0.72F, alpha)
            .setNormal(0.0F, 1.0F, 0.0F);
        vertices.addVertex(matrix, (float) d.x, (float) d.y, (float) d.z)
            .setColor(color[0] * 0.72F, color[1] * 0.72F, color[2] * 0.72F, alpha)
            .setNormal(0.0F, 1.0F, 0.0F);
    }

    private static final class BloodJet {

        private Vec3 head;

        private Vec3 tail;

        private Vec3 velocity;

        private final float maxTrailLength;

        private final int lifetime;

        private final float[] color;

        private int age;

        private BloodJet(Vec3 position, Vec3 velocity, float maxTrailLength, int lifetime, float[] color) {
            this.head = position;
            this.tail = position;
            this.velocity = velocity;
            this.maxTrailLength = maxTrailLength;
            this.lifetime = lifetime;
            this.color = color;
        }

        private boolean tick(Minecraft minecraft) {
            var nextHead = head.add(velocity);
            var hit = minecraft.level.clip(
                new ClipContext(head, nextHead, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, minecraft.player)
            );
            if (hit.getType() == HitResult.Type.BLOCK) {
                var normal = Vec3.atLowerCornerOf(hit.getDirection().getNormal());
                addSplat(
                    new BloodSplat(hit.getLocation().add(normal.scale(0.018D)), normal, Math.min(0.28F, maxTrailLength * 0.15F), color)
                );
                return false;
            }

            head = nextHead;
            if (head.distanceTo(tail) > maxTrailLength) {
                tail = head.subtract(velocity.normalize().scale(maxTrailLength));
            }
            velocity = velocity.add(0.0D, -0.012D, 0.0D).scale(0.975D);
            return ++age < lifetime;
        }
    }

    private static final class BloodSplat {

        private final Vec3 position;

        private final Vec3 normal;

        private final float radius;

        private final float[] color;

        private final int lifetime = 110;

        private int age;

        private BloodSplat(Vec3 position, Vec3 normal, float radius, float[] color) {
            this.position = position;
            this.normal = normal;
            this.radius = radius;
            this.color = color;
        }
    }

    private GunKillBloodEffect() {}
}
