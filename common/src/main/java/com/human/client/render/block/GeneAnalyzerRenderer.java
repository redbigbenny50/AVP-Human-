package com.human.client.render.block;

import com.blib.api.client.model.v1.AzBone;
import com.blib.api.client.render.v1.AzRendererPipelineContext;
import com.blib.api.client.render.v1.block.AzBlockEntityRenderer;
import com.blib.api.client.render.v1.block.AzBlockEntityRendererConfig;
import com.blib.api.client.render.v1.layer.AzRenderLayer;
import com.human.HumanResources;
import com.human.client.animation.block.GeneAnalyzerAnimator;
import com.human.common.gameplay.gene.machine.analyzer.GeneAnalyzerBlock;
import com.human.common.gameplay.gene.machine.analyzer.GeneAnalyzerBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/**
 * The Gene Analyzer: the edition's texture; the screens go dark through the clips (off scales them away).
 * <p>
 * The tray sample's fluid shows the loaded syringe's colour blend ([stated] Oct 29). Its texels are clear in the skins;
 * a layer draws the fluid cube (.bbmodel sample_fluid, x -17..-15, y 12..15, z -1..1, inflate -0.25) on the
 * gSampleFluid bone in the blend colour, so it rides the tray and vanishes whenever the clips scale the sample away.
 * </p>
 */
public class GeneAnalyzerRenderer extends AzBlockEntityRenderer<GeneAnalyzerBlockEntity> {

    private static final ResourceLocation MODEL = HumanResources.blockGeoModelLocation("gene_analyzer");

    private static final ResourceLocation LIGHT = HumanResources.blockTextureLocation("gene_light");

    public GeneAnalyzerRenderer() {
        super(
            AzBlockEntityRendererConfig.<GeneAnalyzerBlockEntity>builder($ -> MODEL, GeneAnalyzerRenderer::textureFor)
                .setAnimatorProvider(GeneAnalyzerAnimator::new)
                .addRenderLayer(new FluidLayer())
                .build()
        );
    }

    public static ResourceLocation textureFor(GeneAnalyzerBlockEntity analyzer) {
        var id = analyzer.getBlockState().getBlock() instanceof GeneAnalyzerBlock block ? block.edition().id : "gene_analyzer";
        return HumanResources.blockTextureLocation(id);
    }

    private static final class FluidLayer implements AzRenderLayer<Long, GeneAnalyzerBlockEntity> {

        @Override
        public void preRender(AzRendererPipelineContext<Long, GeneAnalyzerBlockEntity> context) {}

        @Override
        public void render(AzRendererPipelineContext<Long, GeneAnalyzerBlockEntity> context) {}

        @Override
        public void renderForBone(AzRendererPipelineContext<Long, GeneAnalyzerBlockEntity> context, AzBone bone) {
            if (!"gSampleFluid".equals(bone.getName())) {
                return;
            }
            var colour = context.animatable().sampleColour();
            if (colour == 0) {
                return;
            }
            var pose = context.poseStack().last();
            var buffer = context.multiBufferSource().getBuffer(RenderType.entityCutoutNoCull(LIGHT));
            // The .bbmodel cube inflated -0.25: the fluid sits inside the glass, never z-fighting it.
            box(pose, buffer, colour | 0xFF000000, context.packedLight(), -16.75F, 12.25F, -0.75F, -15.25F, 14.75F, 0.75F);
        }
    }

    /** A box in .bbmodel pixel coordinates (BLib bakes cubes in these), all six faces. */
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
        float tz
    ) {
        float x0 = fx / 16, y0 = fy / 16, z0 = fz / 16, x1 = tx / 16, y1 = ty / 16, z1 = tz / 16;
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
