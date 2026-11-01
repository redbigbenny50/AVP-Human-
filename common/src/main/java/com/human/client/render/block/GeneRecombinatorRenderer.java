package com.human.client.render.block;

import com.blib.api.client.model.v1.AzBone;
import com.blib.api.client.render.v1.AzRendererPipelineContext;
import com.blib.api.client.render.v1.block.AzBlockEntityRenderer;
import com.blib.api.client.render.v1.block.AzBlockEntityRendererConfig;
import com.blib.api.client.render.v1.layer.AzRenderLayer;
import com.human.HumanResources;
import com.human.client.animation.block.GeneRecombinatorAnimator;
import com.human.common.gameplay.gene.machine.recombinator.GeneRecombinatorBlock;
import com.human.common.gameplay.gene.machine.recombinator.GeneRecombinatorBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/**
 * The Gene Recombinator: the edition's texture, and the fluid in the gene's colour ([stated] Oct 29: vials, chamber
 * layers and output tinted, glass see-through). The fluid texels are clear in the skins; a layer draws each fluid cube
 * on its own bone, so the clips (arms pouring, layers filling, piston pressing) move and scale it as before.
 */
public class GeneRecombinatorRenderer extends AzBlockEntityRenderer<GeneRecombinatorBlockEntity> {

    private static final ResourceLocation MODEL = HumanResources.blockGeoModelLocation("gene_recombinator");

    private static final ResourceLocation LIGHT = HumanResources.blockTextureLocation("gene_light");

    /**
     * Bone, then the fluid cube's from/to in block-geo .bbmodel pixels (already shifted onto the 2x1), then its inflate
     * from the .bbmodel (negative: the fluid sits just inside the glass, so the two never z-fight).
     */
    private static final Object[][] FLUIDS = {
        { "gOutFluid", 6, 8, -2, 10, 11, 2, -0.003F },
        { "gLayer1", 5, 13, -3, 11, 14, 3, -0.003F },
        { "gLayer2", 5, 14, -3, 11, 15, 3, -0.003F },
        { "gLayer3", 5, 15, -3, 11, 16, 3, -0.003F },
        { "gLTubeFluid", 18, 25, -1, 20, 28, 1, -0.25F },
        { "gRTubeFluid", -4, 25, -1, -2, 28, 1, -0.25F }
    };

    public GeneRecombinatorRenderer() {
        super(
            AzBlockEntityRendererConfig.<GeneRecombinatorBlockEntity>builder($ -> MODEL, GeneRecombinatorRenderer::textureFor)
                .setAnimatorProvider(GeneRecombinatorAnimator::new)
                .addRenderLayer(new FluidLayer())
                .build()
        );
    }

    public static ResourceLocation textureFor(GeneRecombinatorBlockEntity machine) {
        var id = machine.getBlockState().getBlock() instanceof GeneRecombinatorBlock block ? block.edition().id : "gene_recombinator";
        return HumanResources.blockTextureLocation(id);
    }

    private static final class FluidLayer implements AzRenderLayer<Long, GeneRecombinatorBlockEntity> {

        @Override
        public void preRender(AzRendererPipelineContext<Long, GeneRecombinatorBlockEntity> context) {}

        @Override
        public void render(AzRendererPipelineContext<Long, GeneRecombinatorBlockEntity> context) {}

        @Override
        public void renderForBone(AzRendererPipelineContext<Long, GeneRecombinatorBlockEntity> context, AzBone bone) {
            var name = bone.getName();
            for (var fluid : FLUIDS) {
                if (!fluid[0].equals(name)) {
                    continue;
                }
                var colour = context.animatable().fluidColour();
                if (colour == 0) {
                    return;
                }
                var buffer = context.multiBufferSource().getBuffer(RenderType.entityCutoutNoCull(LIGHT));
                box(
                    context.poseStack().last(),
                    buffer,
                    colour,
                    context.packedLight(),
                    (int) fluid[1],
                    (int) fluid[2],
                    (int) fluid[3],
                    (int) fluid[4],
                    (int) fluid[5],
                    (int) fluid[6],
                    (float) fluid[7]
                );
                return;
            }
        }
    }

    private static void box(
        PoseStack.Pose pose,
        VertexConsumer buffer,
        int colour,
        int light,
        float fx,
        float fy,
        float fz,
        float tx,
        float ty,
        float tz,
        float inflate
    ) {
        float x0 = (fx - inflate) / 16, y0 = (fy - inflate) / 16, z0 = (fz - inflate) / 16;
        float x1 = (tx + inflate) / 16, y1 = (ty + inflate) / 16, z1 = (tz + inflate) / 16;
        quad(pose, buffer, colour, light, 0, 0, -1, x1, y1, z0, x0, y1, z0, x0, y0, z0, x1, y0, z0);
        quad(pose, buffer, colour, light, 0, 0, 1, x0, y1, z1, x1, y1, z1, x1, y0, z1, x0, y0, z1);
        quad(pose, buffer, colour, light, -1, 0, 0, x0, y1, z0, x0, y1, z1, x0, y0, z1, x0, y0, z0);
        quad(pose, buffer, colour, light, 1, 0, 0, x1, y1, z1, x1, y1, z0, x1, y0, z0, x1, y0, z1);
        quad(pose, buffer, colour, light, 0, 1, 0, x0, y1, z0, x1, y1, z0, x1, y1, z1, x0, y1, z1);
        quad(pose, buffer, colour, light, 0, -1, 0, x0, y0, z1, x1, y0, z1, x1, y0, z0, x0, y0, z0);
    }

    private static void quad(
        PoseStack.Pose pose,
        VertexConsumer buffer,
        int colour,
        int light,
        float nx,
        float ny,
        float nz,
        float ax,
        float ay,
        float az,
        float bx,
        float by,
        float bz,
        float cx,
        float cy,
        float cz,
        float dx,
        float dy,
        float dz
    ) {
        vertex(pose, buffer, colour, light, nx, ny, nz, ax, ay, az, 0, 0);
        vertex(pose, buffer, colour, light, nx, ny, nz, bx, by, bz, 1, 0);
        vertex(pose, buffer, colour, light, nx, ny, nz, cx, cy, cz, 1, 1);
        vertex(pose, buffer, colour, light, nx, ny, nz, dx, dy, dz, 0, 1);
    }

    private static void vertex(
        PoseStack.Pose pose,
        VertexConsumer buffer,
        int colour,
        int light,
        float nx,
        float ny,
        float nz,
        float x,
        float y,
        float z,
        float u,
        float v
    ) {
        buffer.addVertex(pose, x, y, z)
            .setColor(colour)
            .setUv(u, v)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(light)
            .setNormal(pose, nx, ny, nz);
    }
}
