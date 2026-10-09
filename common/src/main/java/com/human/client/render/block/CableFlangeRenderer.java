package com.human.client.render.block;

import com.human.HumanResources;
import com.human.common.gameplay.block.entity.power.CableBlockEntity;
import com.human.common.gameplay.block.power.CableBlock;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import org.jetbrains.annotations.NotNull;

/**
 * His cable flange - a 6x6 plate, 1 pixel thick, on the face where a cable meets a block - in the colour of what that
 * block does to the grid: GREEN power comes in, RED power goes out, YELLOW both. [stated] Sep 30: cables have no
 * joiner. The cable is centred on every axis, so one box turned to each face fits exactly.
 */
public class CableFlangeRenderer implements BlockEntityRenderer<CableBlockEntity> {

    private static final ResourceLocation GREEN = HumanResources.location("block/cable_flange_green");

    private static final ResourceLocation RED = HumanResources.location("block/cable_flange_red");

    private static final ResourceLocation YELLOW = HumanResources.location("block/cable_flange_yellow");

    public CableFlangeRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(
        @NotNull CableBlockEntity cable,
        float partialTick,
        @NotNull PoseStack poseStack,
        @NotNull MultiBufferSource bufferSource,
        int packedLight,
        int packedOverlay
    ) {
        var state = cable.getBlockState();

        if (!(state.getBlock() instanceof CableBlock)) {
            return;
        }

        VertexConsumer consumer = null;
        var atlas = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS);

        for (var face : Direction.values()) {
            if (!state.getValue(CableBlock.PROPERTY_BY_DIRECTION.get(face))) {
                continue;
            }

            var texture = switch (cable.getMode(face)) {
                case CableBlockEntity.IN -> GREEN;
                case CableBlockEntity.OUT -> RED;
                case CableBlockEntity.BOTH -> YELLOW;
                default -> null;
            };

            if (texture == null) {
                continue;
            }

            if (consumer == null) {
                consumer = bufferSource.getBuffer(RenderType.entityCutout(InventoryMenu.BLOCK_ATLAS));
            }

            flange(consumer, poseStack.last(), face, atlas.apply(texture), packedLight);
        }
    }

    private static void flange(VertexConsumer consumer, PoseStack.Pose pose, Direction face, TextureAtlasSprite sprite, int light) {
        float[] box = switch (face) {
            case NORTH -> new float[] { 5, 5, 0, 11, 11, 1 };
            case SOUTH -> new float[] { 5, 5, 15, 11, 11, 16 };
            case WEST -> new float[] { 0, 5, 5, 1, 11, 11 };
            case EAST -> new float[] { 15, 5, 5, 16, 11, 11 };
            case UP -> new float[] { 5, 15, 5, 11, 16, 11 };
            case DOWN -> new float[] { 5, 0, 5, 11, 1, 11 };
        };

        var x0 = box[0] / 16F;
        var y0 = box[1] / 16F;
        var z0 = box[2] / 16F;
        var x1 = box[3] / 16F;
        var y1 = box[4] / 16F;
        var z1 = box[5] / 16F;

        for (var side : Direction.values()) {
            // His UVs: the ring (pixels 0-6 x 10-16) on the faces across the cable, the thin rim strip on its edges.
            float[] uv = side.getAxis() == face.getAxis()
                ? new float[] { 0, 10, 6, 16 }
                : side.getAxis() == Direction.Axis.Y ? new float[] { 7, 13, 13, 14 } : new float[] { 14, 9, 15, 15 };
            var u0 = sprite.getU(uv[0] / 16F);
            var v0 = sprite.getV(uv[1] / 16F);
            var u1 = sprite.getU(uv[2] / 16F);
            var v1 = sprite.getV(uv[3] / 16F);

            float[][] corners = switch (side) {
                case DOWN -> new float[][] { { x0, y0, z1 }, { x0, y0, z0 }, { x1, y0, z0 }, { x1, y0, z1 } };
                case UP -> new float[][] { { x0, y1, z0 }, { x0, y1, z1 }, { x1, y1, z1 }, { x1, y1, z0 } };
                case NORTH -> new float[][] { { x1, y1, z0 }, { x1, y0, z0 }, { x0, y0, z0 }, { x0, y1, z0 } };
                case SOUTH -> new float[][] { { x0, y1, z1 }, { x0, y0, z1 }, { x1, y0, z1 }, { x1, y1, z1 } };
                case WEST -> new float[][] { { x0, y1, z0 }, { x0, y0, z0 }, { x0, y0, z1 }, { x0, y1, z1 } };
                case EAST -> new float[][] { { x1, y1, z1 }, { x1, y0, z1 }, { x1, y0, z0 }, { x1, y1, z0 } };
            };
            float[][] uvs = { { u0, v0 }, { u0, v1 }, { u1, v1 }, { u1, v0 } };

            for (var index = 0; index < 4; index++) {
                consumer.addVertex(pose, corners[index][0], corners[index][1], corners[index][2])
                    .setColor(255, 255, 255, 255)
                    .setUv(uvs[index][0], uvs[index][1])
                    .setOverlay(OverlayTexture.NO_OVERLAY)
                    .setLight(light)
                    .setNormal(pose, side.getStepX(), side.getStepY(), side.getStepZ());
            }
        }
    }
}
