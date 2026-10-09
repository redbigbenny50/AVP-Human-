package com.human.client.render.block;

import com.human.HumanResources;
import com.human.common.gameplay.block.pipe.PipeBlock;
import com.human.common.gameplay.block.pipe.PipeBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import org.jetbrains.annotations.NotNull;

/**
 * The coloured flange where a pipe meets a block - his 8x8 flange, 1 pixel thick, on the pipe's face: GREEN where
 * things go into the block, RED where they come out, YELLOW both. One renderer for the fluid and the item pipe, each
 * with its own three flange textures. Drawn here rather than in the block model because the colour depends on the
 * neighbour and the end's setting, and changes live. Joiners are in the block model and cost nothing here.
 */
public class PipeFlangeRenderer<T extends PipeBlockEntity> implements BlockEntityRenderer<T> {

    private final ResourceLocation green;

    private final ResourceLocation red;

    private final ResourceLocation yellow;

    /**
     * @param textures the pipe's flange textures, e.g. {@code block/fluid_pipe_flange} for {@code _green},
     *                 {@code _red}, {@code _yellow}
     */
    public PipeFlangeRenderer(String textures) {
        this.green = HumanResources.location(textures + "_green");
        this.red = HumanResources.location(textures + "_red");
        this.yellow = HumanResources.location(textures + "_yellow");
    }

    @Override
    public void render(
        @NotNull T pipe,
        float partialTick,
        @NotNull PoseStack poseStack,
        @NotNull MultiBufferSource bufferSource,
        int packedLight,
        int packedOverlay
    ) {
        var state = pipe.getBlockState();

        if (!(state.getBlock() instanceof PipeBlock)) {
            return;
        }

        VertexConsumer consumer = null;
        var atlas = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS);
        var pose = poseStack.last();

        for (var face : Direction.values()) {
            if (PipeBlock.side(state, face) != PipeBlock.Side.PORT) {
                continue;
            }

            var texture = switch (pipe.getMode(face)) {
                case PipeBlockEntity.IN -> green;
                case PipeBlockEntity.OUT -> red;
                case PipeBlockEntity.BOTH -> yellow;
                default -> null;
            };

            if (texture == null) {
                continue;
            }

            if (consumer == null) {
                consumer = bufferSource.getBuffer(RenderType.entityCutout(InventoryMenu.BLOCK_ATLAS));
            }

            flange(consumer, pose, face, atlas.apply(texture), packedLight);
        }
    }

    /** The flange box for one face, in pixels - centred on the pipe (x 5-11, y 6-12 across its run). */
    private static void flange(VertexConsumer consumer, PoseStack.Pose pose, Direction face, TextureAtlasSprite sprite, int light) {
        float[] box = switch (face) {
            case NORTH -> new float[] { 4, 5, 0, 12, 13, 1 };
            case SOUTH -> new float[] { 4, 5, 15, 12, 13, 16 };
            case WEST -> new float[] { 0, 5, 4, 1, 13, 12 };
            case EAST -> new float[] { 15, 5, 4, 16, 13, 12 };
            case UP -> new float[] { 4, 15, 4, 12, 16, 12 };
            case DOWN -> new float[] { 4, 0, 4, 12, 1, 12 };
        };

        var x0 = box[0] / 16F;
        var y0 = box[1] / 16F;
        var z0 = box[2] / 16F;
        var x1 = box[3] / 16F;
        var y1 = box[4] / 16F;
        var z1 = box[5] / 16F;

        // His flange texture's ring: pixels 0-6 across, 8-14 down.
        var u0 = sprite.getU(0F);
        var u1 = sprite.getU(6F / 16F);
        var v0 = sprite.getV(8F / 16F);
        var v1 = sprite.getV(14F / 16F);

        for (var side : Direction.values()) {
            float[][] corners = switch (side) {
                case DOWN -> new float[][] { { x0, y0, z1 }, { x0, y0, z0 }, { x1, y0, z0 }, { x1, y0, z1 } };
                case UP -> new float[][] { { x0, y1, z0 }, { x0, y1, z1 }, { x1, y1, z1 }, { x1, y1, z0 } };
                case NORTH -> new float[][] { { x1, y1, z0 }, { x1, y0, z0 }, { x0, y0, z0 }, { x0, y1, z0 } };
                case SOUTH -> new float[][] { { x0, y1, z1 }, { x0, y0, z1 }, { x1, y0, z1 }, { x1, y1, z1 } };
                case WEST -> new float[][] { { x0, y1, z0 }, { x0, y0, z0 }, { x0, y0, z1 }, { x0, y1, z1 } };
                case EAST -> new float[][] { { x1, y1, z1 }, { x1, y0, z1 }, { x1, y0, z0 }, { x1, y1, z0 } };
            };
            float[][] uv = { { u0, v0 }, { u0, v1 }, { u1, v1 }, { u1, v0 } };

            for (var index = 0; index < 4; index++) {
                consumer.addVertex(pose, corners[index][0], corners[index][1], corners[index][2])
                    .setColor(255, 255, 255, 255)
                    .setUv(uv[index][0], uv[index][1])
                    .setOverlay(OverlayTexture.NO_OVERLAY)
                    .setLight(light)
                    .setNormal(pose, side.getStepX(), side.getStepY(), side.getStepZ());
            }
        }
    }
}
