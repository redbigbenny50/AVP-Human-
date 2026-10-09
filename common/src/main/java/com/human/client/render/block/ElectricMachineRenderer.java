package com.human.client.render.block;

import com.human.HumanResources;
import com.human.common.gameplay.block.entity.machine.ElectricMachineBlockEntity;
import com.human.common.gameplay.block.power.machine.ElectricMachineBlock;
import com.human.common.gameplay.machine.ElectricMachineKind;
import com.human.common.gameplay.machine.MachineState;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.EnumMap;
import java.util.Map;

/**
 * Draws a machine's WORKING front on the machine's own clock, and its power LED full-bright.
 * <h2>⭐ WHY THE FRONT IS DRAWN HERE AND NOT BY THE BLOCK MODEL</h2> An animated block texture runs on one clock for
 * every copy of the block in the world. [stated] Sep 29: machines must not animate in lockstep. So the block model
 * always shows the still front, and while the machine works this draws the animation over it, one flat square a hair in
 * front of the face, choosing the frame from the tick THIS machine started ({@code clientWorkStart}).
 * <h2>COST</h2> Idle and switched-off machines return before drawing anything. A working machine draws one square, two
 * for the furnace fronts that blend between frames, plus the LED - less than a chest. Nothing is allocated per frame.
 * And it only draws within {@value #VIEW_DISTANCE} blocks; beyond that the model's still front shows, which at that
 * distance looks the same.
 * <h2>THE LED</h2> Drawn at full brightness whenever the machine is ON or WORKING, on BOTH loaders. The block model's
 * own emissive setting only works on NeoForge; this makes the light read at night on Fabric too.
 */
public class ElectricMachineRenderer implements BlockEntityRenderer<ElectricMachineBlockEntity> {

    private static final int VIEW_DISTANCE = 32;

    /** How far in front of the face the overlay sits - enough to never z-fight, too little to ever see. */
    private static final float FACE_OFFSET = 0.002F;

    private static final ResourceLocation LED = HumanResources.location("textures/block/machine_led_glow.png");

    private static final Map<ElectricMachineKind, ResourceLocation> STRIPS =
        new EnumMap<>(ElectricMachineKind.class);

    public ElectricMachineRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(
        @NotNull ElectricMachineBlockEntity machine,
        float partialTick,
        @NotNull PoseStack poseStack,
        @NotNull MultiBufferSource buffers,
        int packedLight,
        int packedOverlay
    ) {
        var state = machine.getBlockState();

        if (!state.hasProperty(ElectricMachineBlock.STATE)) {
            return;
        }

        var machineState = state.getValue(ElectricMachineBlock.STATE);

        if (machineState == MachineState.OFF) {
            return;
        }

        var level = machine.getLevel();

        if (level == null) {
            return;
        }

        var facing = state.getValue(ElectricMachineBlock.FACING);

        poseStack.pushPose();
        poseStack.translate(0.5D, 0.5D, 0.5D);
        // Rotate so the machine's front is always local -Z: north 0, east -90, south 180, west 90.
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - facing.toYRot()));

        var pose = poseStack.last();

        if (machineState == MachineState.WORKING) {
            // ⚠ The machine's own block is solid, so its packed light is dark. Light the front by the block it faces.
            var light = LevelRenderer.getLightColor(level, machine.getBlockPos().relative(facing));

            drawWorkingFront(machine, level.getGameTime(), partialTick, pose, buffers, light);
        }

        quad(buffers.getBuffer(RenderType.entityCutoutNoCull(LED)), pose, 0.0F, 1.0F, FACE_OFFSET * 2.0F, LightTexture.FULL_BRIGHT, 255);

        poseStack.popPose();
    }

    private static void drawWorkingFront(
        ElectricMachineBlockEntity machine,
        long gameTime,
        float partialTick,
        PoseStack.Pose pose,
        MultiBufferSource buffers,
        int light
    ) {
        var animation = machine.getKind().animation();
        var strip = STRIPS.computeIfAbsent(
            machine.getKind(),
            kind -> HumanResources.location("textures/machine/" + kind.id() + "_working.png")
        );

        // Ticks since THIS machine started, into its own cycle. Before the client has seen it start (a frame drawn in
        // the same tick the chunk arrived), clientWorkStart is 0 and this is simply some frame - harmless for a tick.
        var elapsed = (gameTime - machine.clientWorkStart) + partialTick;
        var cycle = animation.cycleTicks();
        var inCycle = (float) (elapsed % cycle);

        if (inCycle < 0.0F) {
            inCycle += cycle;
        }

        var frame = 0;
        var frameStart = 0.0F;
        var frames = animation.frameTicks();

        while (frame < frames.length - 1 && inCycle >= frameStart + frames[frame]) {
            frameStart += frames[frame];
            frame++;
        }

        var count = animation.frameCount();
        var v0 = frame / (float) count;
        var v1 = (frame + 1) / (float) count;

        quad(buffers.getBuffer(RenderType.entityCutoutNoCull(strip)), pose, v0, v1, FACE_OFFSET, light, 255);

        if (animation.interpolate()) {
            var fraction = (inCycle - frameStart) / frames[frame];
            var next = (frame + 1) % count;
            var alpha = Math.round(Math.max(0.0F, Math.min(1.0F, fraction)) * 255.0F);

            if (alpha > 0) {
                quad(
                    buffers.getBuffer(RenderType.entityTranslucent(strip)),
                    pose,
                    next / (float) count,
                    (next + 1) / (float) count,
                    FACE_OFFSET * 1.5F,
                    light,
                    alpha
                );
            }
        }
    }

    /**
     * The front face as one square, in the rotated frame where the front is -Z. Texture left sits on the viewer's left,
     * as on any block face.
     */
    private static void quad(VertexConsumer consumer, PoseStack.Pose pose, float v0, float v1, float offset, int light, int alpha) {
        var z = -0.5F - offset;

        vertex(consumer, pose, 0.5F, 0.5F, z, 0.0F, v0, light, alpha);
        vertex(consumer, pose, 0.5F, -0.5F, z, 0.0F, v1, light, alpha);
        vertex(consumer, pose, -0.5F, -0.5F, z, 1.0F, v1, light, alpha);
        vertex(consumer, pose, -0.5F, 0.5F, z, 1.0F, v0, light, alpha);
    }

    private static void vertex(
        VertexConsumer consumer,
        PoseStack.Pose pose,
        float x,
        float y,
        float z,
        float u,
        float v,
        int light,
        int alpha
    ) {
        consumer.addVertex(pose, x, y, z)
            .setColor(255, 255, 255, alpha)
            .setUv(u, v)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(light)
            .setNormal(pose, 0.0F, 0.0F, -1.0F);
    }

    @Override
    public int getViewDistance() {
        return VIEW_DISTANCE;
    }
}
