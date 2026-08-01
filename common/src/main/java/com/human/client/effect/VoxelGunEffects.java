package com.human.client.effect;

import com.human.common.network.packet.S2CGunKillEffectPayload;
import com.human.common.network.packet.S2CGunVoxelEffectPayload;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** Texture-free, geometry-only impact shards. */
public final class VoxelGunEffects {

    private static final int MAX_SEGMENTS = 420;

    private static final int MAX_KILL_DROPLETS = 300;

    private static final int MAX_KILL_SPLATS = 100;

    private static final int MAX_KILL_JETS = 240;

    private static final List<LineSegment> SEGMENTS = new ArrayList<>();

    private static final List<KillDroplet> KILL_DROPLETS = new ArrayList<>();

    private static final List<KillSplat> KILL_SPLATS = new ArrayList<>();

    private static final List<KillJet> KILL_JETS = new ArrayList<>();

    private static long localMuzzleFlashEndTick;

    public static void trigger(S2CGunVoxelEffectPayload payload) {
        var minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null || minecraft.player.distanceToSqr(payload.origin()) > 256.0D * 256.0D) {
            return;
        }

        if (minecraft.player.getEyePosition().distanceToSqr(payload.origin()) < 1.0D) {
            localMuzzleFlashEndTick = minecraft.level.getGameTime() + 4L;
        }

        if (!payload.entityImpact() && payload.impactNormal().equals(Vec3.ZERO)) {
            return;
        }

        var endpoint = payload.endpoint().add(payload.impactNormal().scale(0.015D));
        var cameraPosition = minecraft.gameRenderer.getMainCamera().getPosition();
        if (!isVisible(minecraft, cameraPosition, endpoint)) {
            return;
        }

        var random = RandomSource.create(payload.seed());
        spawnImpactShards(endpoint, payload.entityImpact(), payload.fluidType(), random);
    }

    public static boolean hasLocalMuzzleFlash(ItemStack itemStack) {
        var minecraft = Minecraft.getInstance();
        return minecraft.level != null
            && minecraft.player != null
            && ItemStack.isSameItem(itemStack, minecraft.player.getMainHandItem())
            && minecraft.level.getGameTime() < localMuzzleFlashEndTick;
    }

    public static void triggerKillEffect(S2CGunKillEffectPayload payload) {
        GunKillBloodEffect.spawn(payload);
    }

    public static void clientTick(Minecraft minecraft) {
        GunKillBloodEffect.tick(minecraft);
        for (Iterator<LineSegment> iterator = SEGMENTS.iterator(); iterator.hasNext();) {
            var segment = iterator.next();
            segment.tick();
            if (segment.age >= segment.lifetime) {
                iterator.remove();
            }
        }
        for (Iterator<KillDroplet> iterator = KILL_DROPLETS.iterator(); iterator.hasNext();) {
            var droplet = iterator.next();
            droplet.tick(minecraft);
            if (droplet.age >= droplet.lifetime) {
                iterator.remove();
            }
        }
        for (Iterator<KillJet> iterator = KILL_JETS.iterator(); iterator.hasNext();) {
            var jet = iterator.next();
            jet.tick(minecraft);
            if (jet.age >= jet.lifetime) {
                iterator.remove();
            }
        }
        for (Iterator<KillSplat> iterator = KILL_SPLATS.iterator(); iterator.hasNext();) {
            var splat = iterator.next();
            splat.age++;
            if (splat.age >= splat.lifetime) {
                iterator.remove();
            }
        }
    }

    public static void render(PoseStack poseStack, MultiBufferSource buffers, Vec3 cameraPosition, float partialTick) {
        GunKillBloodEffect.render(poseStack, buffers, cameraPosition, partialTick);
        var vertices = buffers.getBuffer(RenderType.lines());
        var matrix = poseStack.last().pose();
        for (var segment : SEGMENTS) {
            var alpha = 1.0F - ((segment.age + partialTick) / segment.lifetime);
            var start = segment.start.add(segment.velocity.scale(partialTick)).subtract(cameraPosition);
            var end = segment.end.add(segment.velocity.scale(partialTick)).subtract(cameraPosition);
            addLine(vertices, matrix, start, end, segment.red, segment.green, segment.blue, alpha);
        }
        for (var jet : KILL_JETS) {
            var alpha = 1.0F - ((jet.age + partialTick) / jet.lifetime);
            var start = jet.tailPosition.subtract(cameraPosition);
            var end = jet.position.add(jet.velocity.scale(partialTick)).subtract(cameraPosition);
            addLine(vertices, matrix, start, end, jet.red, jet.green, jet.blue, alpha);
        }

        var dropletVertices = buffers.getBuffer(RenderType.debugQuads());
        for (var droplet : KILL_DROPLETS) {
            var alpha = 1.0F - ((droplet.age + partialTick) / droplet.lifetime);
            var position = droplet.position.add(droplet.velocity.scale(partialTick)).subtract(cameraPosition);
            addDroplet(dropletVertices, matrix, position, droplet.size, droplet.red, droplet.green, droplet.blue, alpha);
        }
        for (var splat : KILL_SPLATS) {
            var alpha = 0.82F * (1.0F - ((splat.age + partialTick) / splat.lifetime));
            addSplat(
                dropletVertices,
                matrix,
                splat.position.subtract(cameraPosition),
                splat.normal,
                splat.size,
                splat.red * 0.7F,
                splat.green * 0.7F,
                splat.blue * 0.7F,
                alpha
            );
        }
    }

    private static boolean isVisible(Minecraft minecraft, Vec3 cameraPosition, Vec3 endpoint) {
        var hit = minecraft.level.clip(
            new ClipContext(cameraPosition, endpoint, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, minecraft.player)
        );
        return hit.getType() != HitResult.Type.BLOCK || hit.getLocation().distanceTo(cameraPosition) + 0.02D >= endpoint.distanceTo(
            cameraPosition
        );
    }

    private static void spawnImpactShards(Vec3 endpoint, boolean entityImpact, int fluidType, RandomSource random) {
        var color = entityImpact ? GunKillBloodEffect.colorFor(fluidType) : new float[] { 1.0F, 0.58F, 0.12F };
        var red = color[0];
        var green = color[1];
        var blue = color[2];
        for (int index = 0; index < 14; index++) {
            var direction = randomVector(random, 1.0D).normalize();
            var length = 0.08D + random.nextDouble() * 0.20D;
            var velocity = direction.scale(0.025D + random.nextDouble() * 0.055D);
            add(new LineSegment(endpoint, endpoint.add(direction.scale(length)), velocity, 4 + random.nextInt(4), red, green, blue));
        }
    }

    private static Vec3 randomVector(RandomSource random, double scale) {
        return new Vec3(
            (random.nextDouble() - 0.5D) * scale * 2.0D,
            (random.nextDouble() - 0.5D) * scale * 2.0D,
            (random.nextDouble() - 0.5D) * scale * 2.0D
        );
    }

    private static void addLine(
        VertexConsumer vertices,
        org.joml.Matrix4f matrix,
        Vec3 start,
        Vec3 end,
        float red,
        float green,
        float blue,
        float alpha
    ) {
        vertices.addVertex(matrix, (float) start.x, (float) start.y, (float) start.z)
            .setColor(red, green, blue, alpha)
            .setNormal(0.0F, 1.0F, 0.0F);
        vertices.addVertex(matrix, (float) end.x, (float) end.y, (float) end.z)
            .setColor(red, green, blue, alpha)
            .setNormal(0.0F, 1.0F, 0.0F);
    }

    private static void addDroplet(
        VertexConsumer vertices,
        org.joml.Matrix4f matrix,
        Vec3 center,
        float size,
        float red,
        float green,
        float blue,
        float alpha
    ) {
        // Two thin intersecting fluid sheets read as a rounded droplet from any viewing angle.
        addQuad(
            vertices,
            matrix,
            center.add(-size, -size, 0.0D),
            center.add(size, -size, 0.0D),
            center.add(size, size, 0.0D),
            center.add(-size, size, 0.0D),
            red,
            green,
            blue,
            alpha
        );
        addQuad(
            vertices,
            matrix,
            center.add(0.0D, -size, -size),
            center.add(0.0D, -size, size),
            center.add(0.0D, size, size),
            center.add(0.0D, size, -size),
            red,
            green,
            blue,
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
        float red,
        float green,
        float blue,
        float alpha
    ) {
        vertices.addVertex(matrix, (float) a.x, (float) a.y, (float) a.z).setColor(red, green, blue, alpha).setNormal(0.0F, 1.0F, 0.0F);
        vertices.addVertex(matrix, (float) b.x, (float) b.y, (float) b.z).setColor(red, green, blue, alpha).setNormal(0.0F, 1.0F, 0.0F);
        vertices.addVertex(matrix, (float) c.x, (float) c.y, (float) c.z).setColor(red, green, blue, alpha).setNormal(0.0F, 1.0F, 0.0F);
        vertices.addVertex(matrix, (float) d.x, (float) d.y, (float) d.z).setColor(red, green, blue, alpha).setNormal(0.0F, 1.0F, 0.0F);
    }

    private static void addSplat(
        VertexConsumer vertices,
        org.joml.Matrix4f matrix,
        Vec3 center,
        Vec3 normal,
        float size,
        float red,
        float green,
        float blue,
        float alpha
    ) {
        var firstAxis = Math.abs(normal.y) < 0.9D
            ? normal.cross(new Vec3(0.0D, 1.0D, 0.0D)).normalize()
            : normal.cross(new Vec3(1.0D, 0.0D, 0.0D)).normalize();
        var secondAxis = normal.cross(firstAxis).normalize();
        addQuad(
            vertices,
            matrix,
            center.add(firstAxis.scale(-size)).add(secondAxis.scale(-size * 0.70D)),
            center.add(firstAxis.scale(size)).add(secondAxis.scale(-size * 0.70D)),
            center.add(firstAxis.scale(size)).add(secondAxis.scale(size * 0.70D)),
            center.add(firstAxis.scale(-size)).add(secondAxis.scale(size * 0.70D)),
            red,
            green,
            blue,
            alpha
        );
    }

    private static void add(LineSegment segment) {
        if (SEGMENTS.size() >= MAX_SEGMENTS) {
            SEGMENTS.removeFirst();
        }
        SEGMENTS.add(segment);
    }

    private static void addKillDroplet(KillDroplet droplet) {
        if (KILL_DROPLETS.size() >= MAX_KILL_DROPLETS) {
            KILL_DROPLETS.removeFirst();
        }
        KILL_DROPLETS.add(droplet);
    }

    private static void addKillSplat(KillSplat splat) {
        if (KILL_SPLATS.size() >= MAX_KILL_SPLATS) {
            KILL_SPLATS.removeFirst();
        }
        KILL_SPLATS.add(splat);
    }

    private static void addKillJet(KillJet jet) {
        if (KILL_JETS.size() >= MAX_KILL_JETS) {
            KILL_JETS.removeFirst();
        }
        KILL_JETS.add(jet);
    }

    private static final class LineSegment {

        private Vec3 start;

        private Vec3 end;

        private final Vec3 velocity;

        private final int lifetime;

        private final float red;

        private final float green;

        private final float blue;

        private int age;

        private LineSegment(Vec3 start, Vec3 end, Vec3 velocity, int lifetime, float red, float green, float blue) {
            this.start = start;
            this.end = end;
            this.velocity = velocity;
            this.lifetime = lifetime;
            this.red = red;
            this.green = green;
            this.blue = blue;
        }

        private void tick() {
            start = start.add(velocity);
            end = end.add(velocity);
            age++;
        }
    }

    private static final class KillDroplet {

        private Vec3 position;

        private Vec3 velocity;

        private final float size;

        private final int lifetime;

        private final float red;

        private final float green;

        private final float blue;

        private int age;

        private KillDroplet(Vec3 position, Vec3 velocity, float size, int lifetime, float red, float green, float blue) {
            this.position = position;
            this.velocity = velocity;
            this.size = size;
            this.lifetime = lifetime;
            this.red = red;
            this.green = green;
            this.blue = blue;
        }

        private void tick(Minecraft minecraft) {
            var nextPosition = position.add(velocity);
            var hit = minecraft.level.clip(
                new ClipContext(position, nextPosition, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, minecraft.player)
            );
            if (hit.getType() == HitResult.Type.BLOCK) {
                addKillSplat(
                    new KillSplat(
                        // Keep the splat visibly in front of the surface to avoid depth z-fighting.
                        hit.getLocation().add(Vec3.atLowerCornerOf(hit.getDirection().getNormal()).scale(0.015D)),
                        Vec3.atLowerCornerOf(hit.getDirection().getNormal()),
                        size * 2.4F,
                        100 + (int) (size * 140.0F),
                        red,
                        green,
                        blue
                    )
                );
                age = lifetime;
                return;
            }
            position = nextPosition;
            velocity = velocity.add(0.0D, -0.012D, 0.0D).scale(0.965D);
            age++;
        }
    }

    private static final class KillSplat {

        private final Vec3 position;

        private final Vec3 normal;

        private final float size;

        private final int lifetime;

        private final float red;

        private final float green;

        private final float blue;

        private int age;

        private KillSplat(Vec3 position, Vec3 normal, float size, int lifetime, float red, float green, float blue) {
            this.position = position;
            this.normal = normal;
            this.size = size;
            this.lifetime = lifetime;
            this.red = red;
            this.green = green;
            this.blue = blue;
        }
    }

    /** A visible blood line and its collision path are deliberately the same object. */
    private static final class KillJet {

        private Vec3 position;

        private Vec3 tailPosition;

        private Vec3 velocity;

        private final float length;

        private final int lifetime;

        private final float red;

        private final float green;

        private final float blue;

        private int age;

        private KillJet(Vec3 position, Vec3 velocity, float length, int lifetime, float red, float green, float blue) {
            this.position = position;
            this.tailPosition = position;
            this.velocity = velocity;
            this.length = length;
            this.lifetime = lifetime;
            this.red = red;
            this.green = green;
            this.blue = blue;
        }

        private void tick(Minecraft minecraft) {
            var nextPosition = position.add(velocity);
            var hit = minecraft.level.clip(
                new ClipContext(position, nextPosition, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, minecraft.player)
            );
            if (hit.getType() == HitResult.Type.BLOCK) {
                position = hit.getLocation();
                var normal = Vec3.atLowerCornerOf(hit.getDirection().getNormal());
                addKillSplat(
                    new KillSplat(
                        hit.getLocation().add(normal.scale(0.015D)),
                        normal,
                        Math.min(0.26F, length * 0.13F),
                        110,
                        red,
                        green,
                        blue
                    )
                );
                age = lifetime;
                return;
            }
            position = nextPosition;
            var tailDistance = position.distanceTo(tailPosition);
            if (tailDistance > length) {
                tailPosition = position.subtract(velocity.normalize().scale(length));
            }
            velocity = velocity.add(0.0D, -0.012D, 0.0D).scale(0.975D);
            age++;
        }
    }

    private VoxelGunEffects() {}
}
