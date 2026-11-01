package com.human.client.render.block;

import com.blib.api.client.model.v1.AzBone;
import com.blib.api.client.render.v1.AzRendererPipelineContext;
import com.blib.api.client.render.v1.block.AzBlockEntityRenderer;
import com.blib.api.client.render.v1.block.AzBlockEntityRendererConfig;
import com.blib.api.client.render.v1.layer.AzRenderLayer;
import com.human.HumanResources;
import com.human.client.animation.block.GeneSplicerAnimator;
import com.human.common.gameplay.gene.machine.splicer.GeneSplicerBlock;
import com.human.common.gameplay.gene.machine.splicer.GeneSplicerBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/**
 * The Gene Splicer: the edition's texture, and fluid in gene colours ([stated] Oct 29). Each rack holder's fluid is its
 * own tube's colour; the cradle syringe shows the finished syringe's blend, or the planned blend while running. The
 * fluid texels are clear in the skins; a layer draws each fluid cube (inflate -0.25 from the .bbmodel) on its bone.
 */
public class GeneSplicerRenderer extends AzBlockEntityRenderer<GeneSplicerBlockEntity> {

    private static final ResourceLocation MODEL = HumanResources.blockGeoModelLocation("gene_splicer");

    private static final ResourceLocation LIGHT = HumanResources.blockTextureLocation("gene_light");

    /**
     * Bone, then the fluid cube's from/to in block-geo .bbmodel pixels (already shifted onto the 2x1), then its inflate
     * from the .bbmodel (negative: the fluid sits just inside the glass, so the two never z-fight).
     */
    private static final Object[][] FLUIDS = {
        { "gSyringeFluid", -4, 11, -3, 4, 14, -1, -0.25F },
        { "gTube1Fluid", 9, 12, -3, 11, 15, -1, -0.25F },
        { "gTube2Fluid", 12, 12, -3, 14, 15, -1, -0.25F },
        { "gTube3Fluid", 15, 12, -3, 17, 15, -1, -0.25F },
        { "gTube4Fluid", 18, 12, -3, 20, 15, -1, -0.25F },
        { "gTube5Fluid", 21, 12, -3, 23, 15, -1, -0.25F }
    };

    public GeneSplicerRenderer() {
        super(
            AzBlockEntityRendererConfig.<GeneSplicerBlockEntity>builder($ -> MODEL, GeneSplicerRenderer::textureFor)
                .setAnimatorProvider(GeneSplicerAnimator::new)
                .addRenderLayer(new FluidLayer())
                .build()
        );
    }

    public static ResourceLocation textureFor(GeneSplicerBlockEntity machine) {
        var id = machine.getBlockState().getBlock() instanceof GeneSplicerBlock block ? block.edition().id : "gene_splicer";
        return HumanResources.blockTextureLocation(id);
    }

    private static final class FluidLayer implements AzRenderLayer<Long, GeneSplicerBlockEntity> {

        @Override
        public void preRender(AzRendererPipelineContext<Long, GeneSplicerBlockEntity> context) {}

        @Override
        public void render(AzRendererPipelineContext<Long, GeneSplicerBlockEntity> context) {}

        @Override
        public void renderForBone(AzRendererPipelineContext<Long, GeneSplicerBlockEntity> context, AzBone bone) {
            var name = bone.getName();
            for (var fluid : FLUIDS) {
                if (!fluid[0].equals(name)) {
                    continue;
                }
                var machine = context.animatable();
                // gSyringeFluid, or gTube1Fluid..gTube5Fluid: the digit is the holder.
                var colour = name.equals("gSyringeFluid") ? machine.syringeColour() : machine.tubeColour(name.charAt(5) - '1');
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
