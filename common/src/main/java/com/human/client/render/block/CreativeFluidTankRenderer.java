package com.human.client.render.block;

import com.human.common.gameplay.block.creative.CreativeFluidTankBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.NotNull;

/**
 * [stated] Oct 20 - the Creative Fluid Tank's window shows the fluid it is set to: the fluid's own still texture and
 * colour, drawn over the glass window on all four sides (pixels 2-14 across, 2-9 down, as the side texture has it).
 * Empty glass while no fluid is set.
 */
public class CreativeFluidTankRenderer implements BlockEntityRenderer<CreativeFluidTankBlockEntity> {

    private static final float X0 = 2.0F / 16.0F;

    private static final float X1 = 14.0F / 16.0F;

    /** Window rows 2..9 of the texture, measured from the block's top. */
    private static final float Y0 = 1.0F - 9.0F / 16.0F;

    private static final float Y1 = 1.0F - 2.0F / 16.0F;

    private static final float OUT = 0.002F;

    public CreativeFluidTankRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(
        @NotNull CreativeFluidTankBlockEntity tank,
        float partialTick,
        @NotNull PoseStack poseStack,
        @NotNull MultiBufferSource buffers,
        int packedLight,
        int packedOverlay
    ) {
        var fluid = tank.fluid();

        if (fluid == Fluids.EMPTY) {
            return;
        }

        var info = com.human.client.fluid.FluidClientInfo.Holder.get();
        var texture = info.stillTexture(fluid);

        if (texture == null) {
            return;
        }

        var sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(texture);
        var tint = info.tint(fluid);
        int r = (tint >> 16) & 0xFF;
        int g = (tint >> 8) & 0xFF;
        int b = tint & 0xFF;
        var light = fluid.defaultFluidState().createLegacyBlock().getLightEmission() > 0 ? LightTexture.FULL_BRIGHT : packedLight;
        var consumer = buffers.getBuffer(RenderType.entityTranslucentCull(InventoryMenu.BLOCK_ATLAS));
        var pose = poseStack.last();

        for (var side : Direction.Plane.HORIZONTAL) {
            face(consumer, pose, sprite, side, r, g, b, light);
        }
    }

    private static void face(
        VertexConsumer consumer,
        PoseStack.Pose pose,
        TextureAtlasSprite sprite,
        Direction side,
        int r,
        int g,
        int b,
        int light
    ) {
        float u0 = sprite.getU(X0);
        float u1 = sprite.getU(X1);
        float v0 = sprite.getV(1.0F - Y1);
        float v1 = sprite.getV(1.0F - Y0);
        float nx = side.getStepX();
        float nz = side.getStepZ();

        // Corners of the window on this face, wound to face outwards.
        switch (side) {
            case NORTH -> {
                vertex(consumer, pose, 1 - X0, Y1, -OUT, u0, v0, r, g, b, light, nx, nz);
                vertex(consumer, pose, 1 - X0, Y0, -OUT, u0, v1, r, g, b, light, nx, nz);
                vertex(consumer, pose, 1 - X1, Y0, -OUT, u1, v1, r, g, b, light, nx, nz);
                vertex(consumer, pose, 1 - X1, Y1, -OUT, u1, v0, r, g, b, light, nx, nz);
            }
            case SOUTH -> {
                vertex(consumer, pose, X0, Y1, 1 + OUT, u0, v0, r, g, b, light, nx, nz);
                vertex(consumer, pose, X0, Y0, 1 + OUT, u0, v1, r, g, b, light, nx, nz);
                vertex(consumer, pose, X1, Y0, 1 + OUT, u1, v1, r, g, b, light, nx, nz);
                vertex(consumer, pose, X1, Y1, 1 + OUT, u1, v0, r, g, b, light, nx, nz);
            }
            case WEST -> {
                vertex(consumer, pose, -OUT, Y1, X0, u0, v0, r, g, b, light, nx, nz);
                vertex(consumer, pose, -OUT, Y0, X0, u0, v1, r, g, b, light, nx, nz);
                vertex(consumer, pose, -OUT, Y0, X1, u1, v1, r, g, b, light, nx, nz);
                vertex(consumer, pose, -OUT, Y1, X1, u1, v0, r, g, b, light, nx, nz);
            }
            default -> {
                vertex(consumer, pose, 1 + OUT, Y1, 1 - X0, u0, v0, r, g, b, light, nx, nz);
                vertex(consumer, pose, 1 + OUT, Y0, 1 - X0, u0, v1, r, g, b, light, nx, nz);
                vertex(consumer, pose, 1 + OUT, Y0, 1 - X1, u1, v1, r, g, b, light, nx, nz);
                vertex(consumer, pose, 1 + OUT, Y1, 1 - X1, u1, v0, r, g, b, light, nx, nz);
            }
        }
    }

    private static void vertex(
        VertexConsumer consumer,
        PoseStack.Pose pose,
        float x,
        float y,
        float z,
        float u,
        float v,
        int r,
        int g,
        int b,
        int light,
        float nx,
        float nz
    ) {
        consumer.addVertex(pose, x, y, z)
            .setColor(r, g, b, 230)
            .setUv(u, v)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(light)
            .setNormal(pose, nx, 0.0F, nz);
    }
}
