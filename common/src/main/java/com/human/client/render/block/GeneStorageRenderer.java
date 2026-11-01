package com.human.client.render.block;

import com.blib.api.client.model.v1.AzBone;
import com.blib.api.client.render.v1.AzRendererPipelineContext;
import com.blib.api.client.render.v1.block.AzBlockEntityRenderer;
import com.blib.api.client.render.v1.block.AzBlockEntityRendererConfig;
import com.blib.api.client.render.v1.layer.AzRenderLayer;
import com.human.HumanResources;
import com.human.client.animation.block.GeneStorageAnimator;
import com.human.common.gameplay.gene.machine.storage.GeneStorageBlock;
import com.human.common.gameplay.gene.machine.storage.GeneStorageBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/**
 * The Gene Storage: the edition's texture, lit while its grid has supply and the _off skin (dark screen) without.
 * <p>
 * The 32 carousel vials are not in the model ([stated] Oct 29: drawn by the game). A layer draws them on the gCarousel
 * bone - so they ride the lift through open_1/open_2/close - at the exact cubes the .bbmodel had, tinted with the gene
 * colour of rack slots 0-31 (top ring first). An empty holder draws nothing.
 * </p>
 */
public class GeneStorageRenderer extends AzBlockEntityRenderer<GeneStorageBlockEntity> {

    private static final ResourceLocation MODEL = HumanResources.blockGeoModelLocation("gene_storage");

    /**
     * x, y, z of each vial's min corner in block-geo model space (already shifted onto the 2x2), its side texture
     * column, and its inflate in thousandths from the .bbmodel (some vials are inflated 0.003 so neighbours never
     * z-fight).
     */
    private static final int[][] VIALS = {
        { -1, 13, 7, 135, 0 },
        { 0, 13, 10, 137, 3 },
        { 1, 13, 13, 139, 0 },
        { 4, 13, 14, 141, 3 },
        { 7, 13, 15, 143, 0 },
        { 10, 13, 14, 145, 3 },
        { 13, 13, 13, 135, 0 },
        { 14, 13, 10, 137, 3 },
        { 15, 13, 7, 139, 0 },
        { 14, 13, 4, 141, 3 },
        { 13, 13, 1, 143, 0 },
        { 10, 13, 0, 145, 3 },
        { 7, 13, -1, 135, 0 },
        { 4, 13, 0, 137, 3 },
        { 1, 13, 1, 139, 0 },
        { 0, 13, 4, 141, 3 },
        { -1, 5, 7, 145, 0 },
        { 0, 5, 10, 135, 3 },
        { 1, 5, 13, 137, 0 },
        { 4, 5, 14, 139, 3 },
        { 7, 5, 15, 141, 0 },
        { 10, 5, 14, 143, 3 },
        { 13, 5, 13, 145, 0 },
        { 14, 5, 10, 135, 3 },
        { 15, 5, 7, 137, 0 },
        { 14, 5, 4, 139, 3 },
        { 13, 5, 1, 141, 0 },
        { 10, 5, 0, 143, 3 },
        { 7, 5, -1, 145, 0 },
        { 4, 5, 0, 135, 3 },
        { 1, 5, 1, 137, 0 },
        { 0, 5, 4, 139, 3 }
    };

    private static final float TEXTURE_WIDTH = 256.0F;

    private static final float TEXTURE_HEIGHT = 64.0F;

    public GeneStorageRenderer() {
        super(
            AzBlockEntityRendererConfig.<GeneStorageBlockEntity>builder($ -> MODEL, GeneStorageRenderer::textureFor)
                .setAnimatorProvider(GeneStorageAnimator::new)
                .addRenderLayer(new VialLayer())
                .build()
        );
    }

    public static ResourceLocation textureFor(GeneStorageBlockEntity storage) {
        var id = storage.getBlockState().getBlock() instanceof GeneStorageBlock block ? block.edition().id : "gene_storage";
        return HumanResources.blockTextureLocation(storage.isSupplied() ? id : id + "_off");
    }

    private static final class VialLayer implements AzRenderLayer<Long, GeneStorageBlockEntity> {

        @Override
        public void preRender(AzRendererPipelineContext<Long, GeneStorageBlockEntity> context) {}

        @Override
        public void render(AzRendererPipelineContext<Long, GeneStorageBlockEntity> context) {}

        @Override
        public void renderForBone(AzRendererPipelineContext<Long, GeneStorageBlockEntity> context, AzBone bone) {
            if ("gBase".equals(bone.getName())) {
                if (context.animatable().isSupplied()) {
                    screenBars(context);
                }
                return;
            }
            if (!"gCarousel".equals(bone.getName())) {
                return;
            }
            var colours = context.animatable().carouselColours();
            var pose = context.poseStack().last();
            var buffer = context.vertexConsumer();
            var light = context.packedLight();
            for (int slot = 0; slot < VIALS.length && slot < colours.length; slot++) {
                if (colours[slot] != 0) {
                    vial(pose, buffer, VIALS[slot], colours[slot], light);
                }
            }
        }
    }

    private static final ResourceLocation SCREEN_LIGHT = HumanResources.blockTextureLocation("gene_light");

    private static final int SCREEN_BACKGROUND = 0xFF0C1E12;

    private static final int BAR = 0xFF2C8639;

    private static final int BAR_TOP = 0xFF5AF078;

    /**
     * The approved powered screen (gene_storage_screen_bars.gif): six level bars on the glass that rise and fall, the
     * wave moving sideways; the status LEDs on the left stay as painted. The front screen cube is block-geo x 0..16, y
     * 4..13, front face at z -7, its texture 16x9 with the glass at columns 1-14, rows 1-7; a north face runs u from +x
     * to -x, so face column c spans x 15-c..16-c and row r spans y 12-r..13-r. Drawn fullbright, just in front of the
     * face. Each storage runs its own clock, so a row of them never moves in lockstep.
     */
    private static void screenBars(AzRendererPipelineContext<Long, GeneStorageBlockEntity> context) {
        var storage = context.animatable();
        var level = storage.getLevel();
        if (level == null) {
            return;
        }
        var pose = context.poseStack().last();
        var buffer = context.multiBufferSource().getBuffer(net.minecraft.client.renderer.RenderType.entityCutoutNoCull(SCREEN_LIGHT));
        var light = net.minecraft.client.renderer.LightTexture.FULL_BRIGHT;
        // Clear the painted readout lines right of the LEDs (face columns 4-14, rows 1-7).
        texelRect(pose, buffer, 4, 1, 14, 7, -7.01F, SCREEN_BACKGROUND, light);
        var seed = storage.getBlockPos().hashCode() * 0.37F;
        var t = (level.getGameTime() + context.partialTick()) / 20.0F + seed;
        for (int bar = 0; bar < 6; bar++) {
            var phase = t * 2.2F + bar * 0.85F;
            var height = Math.max(
                1,
                Math.min(
                    6,
                    Math.round(3.5F + 2.6F * net.minecraft.util.Mth.sin(phase) + 0.6F * net.minecraft.util.Mth.sin(phase * 2.3F + bar))
                )
            );
            var column = 4 + bar * 2;
            // Bottom glass row is face row 7; the bar grows upward.
            texelRect(pose, buffer, column, 8 - height + 1, column, 7, -7.02F, BAR, light);
            texelRect(pose, buffer, column, 8 - height, column, 8 - height, -7.02F, BAR_TOP, light);
        }
    }

    /** A rectangle of whole face texels (columns c0..c1, rows r0..r1 inclusive) on the front screen, facing -z. */
    private static void texelRect(
        PoseStack.Pose pose,
        VertexConsumer buffer,
        int c0,
        int r0,
        int c1,
        int r1,
        float z,
        int colour,
        int light
    ) {
        if (r1 < r0) {
            return;
        }
        float x0 = (15 - c1) / 16.0F, x1 = (16 - c0) / 16.0F;
        float y0 = (12 - r1) / 16.0F, y1 = (13 - r0) / 16.0F;
        float zz = z / 16.0F;
        quad(pose, buffer, colour, light, 0, 0, -1, x1, y1, zz, x0, y1, zz, x0, y0, zz, x1, y0, zz, 0, 0, 1, 1);
    }

    /** One 2x6x2 vial: sides from its texture column, cap and base from the shared cap texels - as in the .bbmodel. */
    private static void vial(PoseStack.Pose pose, VertexConsumer buffer, int[] vial, int colour, int light) {
        var inflate = vial[4] / 1000.0F;
        float x0 = (vial[0] - inflate) / 16.0F, y0 = (vial[1] - inflate) / 16.0F, z0 = (vial[2] - inflate) / 16.0F;
        float x1 = (vial[0] + 2 + inflate) / 16.0F, y1 = (vial[1] + 6 + inflate) / 16.0F, z1 = (vial[2] + 2 + inflate) / 16.0F;
        float su0 = vial[3] / TEXTURE_WIDTH, su1 = (vial[3] + 2) / TEXTURE_WIDTH, sv0 = 32 / TEXTURE_HEIGHT, sv1 = 38 / TEXTURE_HEIGHT;
        // north (-z), south (+z), west (-x), east (+x)
        quad(pose, buffer, colour, light, 0, 0, -1, x1, y1, z0, x0, y1, z0, x0, y0, z0, x1, y0, z0, su0, sv0, su1, sv1);
        quad(pose, buffer, colour, light, 0, 0, 1, x0, y1, z1, x1, y1, z1, x1, y0, z1, x0, y0, z1, su0, sv0, su1, sv1);
        quad(pose, buffer, colour, light, -1, 0, 0, x0, y1, z0, x0, y1, z1, x0, y0, z1, x0, y0, z0, su0, sv0, su1, sv1);
        quad(pose, buffer, colour, light, 1, 0, 0, x1, y1, z1, x1, y1, z0, x1, y0, z0, x1, y0, z1, su0, sv0, su1, sv1);
        // up, down
        quad(
            pose,
            buffer,
            colour,
            light,
            0,
            1,
            0,
            x0,
            y1,
            z0,
            x1,
            y1,
            z0,
            x1,
            y1,
            z1,
            x0,
            y1,
            z1,
            85 / TEXTURE_WIDTH,
            46 / TEXTURE_HEIGHT,
            87 / TEXTURE_WIDTH,
            48 / TEXTURE_HEIGHT
        );
        quad(
            pose,
            buffer,
            colour,
            light,
            0,
            -1,
            0,
            x0,
            y0,
            z1,
            x1,
            y0,
            z1,
            x1,
            y0,
            z0,
            x0,
            y0,
            z0,
            87 / TEXTURE_WIDTH,
            46 / TEXTURE_HEIGHT,
            89 / TEXTURE_WIDTH,
            48 / TEXTURE_HEIGHT
        );
    }

    /** Corners in order top-left, top-right, bottom-right, bottom-left as seen from outside. */
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
        float dz,
        float u0,
        float v0,
        float u1,
        float v1
    ) {
        vertex(pose, buffer, colour, light, nx, ny, nz, ax, ay, az, u0, v0);
        vertex(pose, buffer, colour, light, nx, ny, nz, bx, by, bz, u1, v0);
        vertex(pose, buffer, colour, light, nx, ny, nz, cx, cy, cz, u1, v1);
        vertex(pose, buffer, colour, light, nx, ny, nz, dx, dy, dz, u0, v1);
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
