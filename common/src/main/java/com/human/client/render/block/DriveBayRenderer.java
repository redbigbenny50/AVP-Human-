package com.human.client.render.block;

import com.human.HumanResources;
import com.human.common.gameplay.fabrication.DriveBayBlock;
import com.human.common.gameplay.fabrication.DriveBayBlockEntity;
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

/**
 * Draws a drive bay's SSDs and lights on its front face. [stated] Oct 13: the power LED and each SSD's read light are
 * GLOW LAYERS - drawn at full brightness so they read at night on both loaders, the same way the machines' LEDs are.
 * <p>
 * The SSDs are drawn here rather than in the block model so the model needs no state per slot. An empty, unpowered bay
 * returns before drawing anything.
 */
public class DriveBayRenderer implements BlockEntityRenderer<DriveBayBlockEntity> {

    private static final int VIEW_DISTANCE = 32;

    private static final float FACE_OFFSET = 0.002F;

    private static final ResourceLocation POWER_LED = HumanResources.location("textures/block/machine_led_glow.png");

    private static final ResourceLocation[] SSD = new ResourceLocation[DriveBayBlockEntity.SSD_SLOTS];

    private static final ResourceLocation[] READ_GLOW = new ResourceLocation[DriveBayBlockEntity.SSD_SLOTS];

    static {
        for (int position = 0; position < DriveBayBlockEntity.SSD_SLOTS; position++) {
            SSD[position] = HumanResources.location("textures/block/drive_bay_ssd_" + position + ".png");
            READ_GLOW[position] = HumanResources.location("textures/block/drive_bay_read_glow_" + position + ".png");
        }
    }

    public DriveBayRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(
        @NotNull DriveBayBlockEntity bay,
        float partialTick,
        @NotNull PoseStack poseStack,
        @NotNull MultiBufferSource buffers,
        int packedLight,
        int packedOverlay
    ) {
        var level = bay.getLevel();
        var state = bay.getBlockState();

        if (level == null || !state.hasProperty(DriveBayBlock.FACING) || (bay.isEmpty() && !bay.isSupplied())) {
            return;
        }

        var facing = state.getValue(DriveBayBlock.FACING);

        poseStack.pushPose();
        poseStack.translate(0.5D, 0.5D, 0.5D);
        // Front is local -Z on every facing: north 0, east -90, south 180, west 90.
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - facing.toYRot()));

        var pose = poseStack.last();
        // The bay itself is solid, so its own light is dark - light the front by the block it faces.
        var frontLight = LevelRenderer.getLightColor(level, bay.getBlockPos().relative(facing));

        for (int position = 0; position < DriveBayBlockEntity.SSD_SLOTS; position++) {
            if (!bay.getSsd(position).isEmpty()) {
                quad(buffers.getBuffer(RenderType.entityCutoutNoCull(SSD[position])), pose, FACE_OFFSET, frontLight);
            }
        }

        if (bay.isSupplied()) {
            quad(buffers.getBuffer(RenderType.entityCutoutNoCull(POWER_LED)), pose, FACE_OFFSET * 2.0F, LightTexture.FULL_BRIGHT);
        }

        var reading = bay.readingMask();

        for (int position = 0; position < DriveBayBlockEntity.SSD_SLOTS; position++) {
            if ((reading & (1 << position)) != 0) {
                quad(
                    buffers.getBuffer(RenderType.entityCutoutNoCull(READ_GLOW[position])),
                    pose,
                    FACE_OFFSET * 2.0F,
                    LightTexture.FULL_BRIGHT
                );
            }
        }

        poseStack.popPose();
    }

    @Override
    public int getViewDistance() {
        return VIEW_DISTANCE;
    }

    private static void quad(VertexConsumer consumer, PoseStack.Pose pose, float offset, int light) {
        var z = -0.5F - offset;

        vertex(consumer, pose, 0.5F, 0.5F, z, 0.0F, 0.0F, light);
        vertex(consumer, pose, 0.5F, -0.5F, z, 0.0F, 1.0F, light);
        vertex(consumer, pose, -0.5F, -0.5F, z, 1.0F, 1.0F, light);
        vertex(consumer, pose, -0.5F, 0.5F, z, 1.0F, 0.0F, light);
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, float x, float y, float z, float u, float v, int light) {
        consumer.addVertex(pose, x, y, z)
            .setColor(255, 255, 255, 255)
            .setUv(u, v)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(light)
            .setNormal(pose, 0.0F, 0.0F, -1.0F);
    }
}
