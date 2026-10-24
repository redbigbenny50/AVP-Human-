package com.human.client.render.block;

import com.blib.api.client.render.v1.block.AzBlockEntityRenderer;
import com.blib.api.client.render.v1.block.AzBlockEntityRendererConfig;
import com.human.common.gameplay.storage.CrateBlockEntity;
import com.human.common.gameplay.storage.CrateFootprint;
import com.human.common.gameplay.storage.CrateSize;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import org.jetbrains.annotations.NotNull;

/**
 * Draws a crate once, from its master block. The geo models are centred on the crate's footprint, and the geo renderer
 * centres on the block it is called for and then turns by FACING - so this first moves the origin from the master block
 * to the footprint centre. One renderer serves every family, size and colour: the model and texture come from the
 * crate's own type.
 */
public class CrateRenderer implements BlockEntityRenderer<CrateBlockEntity>, com.human.client.render.OversizedRender<CrateBlockEntity> {

    private final AzBlockEntityRenderer<CrateBlockEntity> geo = new AzBlockEntityRenderer<>(
        AzBlockEntityRendererConfig.<CrateBlockEntity>builder(
            crate -> crate.type().modelLocation(),
            crate -> crate.type().textureLocation()
        ).build()
    ) {};

    @Override
    public void render(
        @NotNull CrateBlockEntity crate,
        float partialTick,
        @NotNull PoseStack poseStack,
        @NotNull MultiBufferSource buffers,
        int packedLight,
        int packedOverlay
    ) {
        if (!crate.isMaster()) {
            return;
        }

        var centre = CrateFootprint.centre(crate.facing(), crate.type().size());

        poseStack.pushPose();
        poseStack.translate(centre.x - 0.5, 0.0, centre.z - 0.5);
        geo.render(crate, partialTick, poseStack, buffers, packedLight, packedOverlay);
        poseStack.popPose();
    }

    /** Big crates reach past their master block, so they must still draw when that one block is off screen. */
    @Override
    public boolean shouldRenderOffScreen(@NotNull CrateBlockEntity crate) {
        return crate.type().size() != CrateSize.ONE;
    }

    @Override
    public int getViewDistance() {
        return 96;
    }

    /** Big crates reach up to five blocks from their master block (the 3 x 5 x 3 container). */
    @Override
    public net.minecraft.world.phys.AABB renderBox(@NotNull CrateBlockEntity crate) {
        // Only the master draws; a big crate's other blocks keep their own one-block box.
        if (!crate.isMaster()) {
            return new net.minecraft.world.phys.AABB(crate.getBlockPos());
        }

        return new net.minecraft.world.phys.AABB(crate.getBlockPos()).inflate(5.0D);
    }
}
