package com.human.client.render.block;

import com.blib.api.client.model.v1.AzBone;
import com.blib.api.client.render.v1.block.AzBlockEntityRenderer;
import com.blib.api.client.render.v1.block.AzBlockEntityRendererConfig;
import com.human.HumanResources;
import com.human.client.animation.block.FabricatorAnimator;
import com.human.client.render.util.ClippingVertexConsumer;
import com.human.common.gameplay.fabrication.FabricatorBlockEntity;
import com.human.common.registry.init.HumanFabrication;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;

/**
 * The fabricator_v2 model on the core block, in its edition's skin, plus what the model's two marker bones are for
 * ([stated] Oct 13):
 * <ul>
 * <li>{@code gTip} - welding sparks (white, yellow and some orange) where the print head is, as it moves.</li>
 * <li>{@code gPrintPlate} - the item being printed, on the bed, built up LAYER BY LAYER as the print progresses. Guns
 * and other items lie on their side; blocks stand upright.</li>
 * </ul>
 * Both bones are TRACKED, so BLib fills their matrices as it draws this fabricator's own animated pose; they are read
 * straight after, before any other fabricator is drawn (the baked bones are shared, the Old Painless lesson).
 * <h2>COST</h2> Nothing extra unless printing. Sparks: at most once per game tick per fabricator, and only while it is
 * actually being drawn - so a distant or hidden one spawns none.
 */
public class FabricatorRenderer extends AzBlockEntityRenderer<FabricatorBlockEntity> implements com.human.client.render.OversizedRender<FabricatorBlockEntity> {

    private static final ResourceLocation MODEL = HumanResources.blockGeoModelLocation("fabricator");

    private static final ResourceLocation STANDARD = HumanResources.blockTextureLocation("fabricator");

    private static final ResourceLocation WY = HumanResources.blockTextureLocation("wy_fabricator");

    private static final ResourceLocation USCM = HumanResources.blockTextureLocation("uscm_fabricator");

    private static final String TIP = "gTip";

    private static final String PLATE = "gPrintPlate";

    /** Size on the bed, in blocks: a block stands this tall, an item this long. */
    private static final float BLOCK_SCALE = 0.4F;

    private static final float ITEM_SCALE = 0.55F;

    /**
     * The two marker bones of the baked model being drawn right now, captured by the pre-render hook (which runs inside
     * {@code super.render}). Static is fine: rendering is single-threaded, and each fabricator reads them straight
     * after its own draw.
     */
    private static @Nullable AzBone tip;

    private static @Nullable AzBone plate;

    public FabricatorRenderer() {
        super(
            AzBlockEntityRendererConfig.<FabricatorBlockEntity>builder($ -> MODEL, FabricatorRenderer::textureFor)
                .setAnimatorProvider(FabricatorAnimator::new)
                .setPrerenderEntry(context -> {
                    // Tracking must be on BEFORE the bones are drawn, so BLib fills their matrices this frame.
                    var model = context.bakedModel();

                    tip = model == null ? null : model.getBoneOrNull(TIP);
                    plate = model == null ? null : model.getBoneOrNull(PLATE);

                    if (tip != null) {
                        tip.setTrackingMatrices(true);
                    }

                    if (plate != null) {
                        plate.setTrackingMatrices(true);
                    }

                    return context;
                })
                .build()
        );
    }

    public static ResourceLocation textureFor(FabricatorBlockEntity fabricator) {
        var block = fabricator.getBlockState().getBlock();

        if (block == HumanFabrication.WY_FABRICATOR.get()) {
            return WY;
        }

        return block == HumanFabrication.USCM_FABRICATOR.get() ? USCM : STANDARD;
    }

    @Override
    public void render(
        @NotNull FabricatorBlockEntity fabricator,
        float partialTick,
        @NotNull PoseStack poseStack,
        @NotNull MultiBufferSource buffers,
        int packedLight,
        int packedOverlay
    ) {
        tip = null;
        plate = null;
        super.render(fabricator, partialTick, poseStack, buffers, packedLight, packedOverlay);

        var level = fabricator.getLevel();
        var target = fabricator.target();

        if (level == null || target == null || !fabricator.isPrinting()) {
            tip = null;
            plate = null;
            return;
        }

        // Sparks at the head, once per tick at most - and ONLY while power is actually reaching the print. [stated]
        // Oct 23 an unpowered fabricator "appears to be printing": a paused print keeps its half-built item on the bed
        // (drawn below), but the head goes quiet.
        if (tip != null && fabricator.isRunning() && fabricator.claimSparkTick(level.getGameTime())) {
            var at = tip.getWorldPosition();

            sparks(fabricator, at.x, at.y, at.z);
        }

        if (plate != null) {
            drawPrint(fabricator, new ItemStack(target), plate, poseStack, buffers, packedLight, partialTick);
        }

        // Never keep the bones past this fabricator's draw: after a resource reload they would hold the old model
        // alive.
        tip = null;
        plate = null;
    }

    private static void sparks(FabricatorBlockEntity fabricator, double x, double y, double z) {
        var level = fabricator.getLevel();

        if (level == null) {
            return;
        }

        var random = level.random;

        for (int i = 0; i < 2; i++) {
            level.addParticle(
                ParticleTypes.ELECTRIC_SPARK,
                x,
                y,
                z,
                (random.nextDouble() - 0.5D) * 0.12D,
                random.nextDouble() * 0.06D,
                (random.nextDouble() - 0.5D) * 0.12D
            );
        }

        if (random.nextInt(2) == 0) {
            level.addParticle(
                ParticleTypes.WAX_ON,
                x,
                y,
                z,
                (random.nextDouble() - 0.5D) * 0.4D,
                0.1D,
                (random.nextDouble() - 0.5D) * 0.4D
            );
        }

        if (random.nextInt(5) == 0) {
            level.addParticle(ParticleTypes.SMALL_FLAME, x, y, z, 0.0D, 0.005D, 0.0D);
        }
    }

    /**
     * The item being printed, on the bed at {@code gPrintPlate}, cut off at a height that rises with the print. Drawn
     * through the game's own item rendering, so a gun shows its full 3D model and a block its block model.
     */
    private static void drawPrint(
        FabricatorBlockEntity fabricator,
        ItemStack stack,
        AzBone plate,
        PoseStack poseStack,
        MultiBufferSource buffers,
        int packedLight,
        float partialTick
    ) {
        var minecraft = Minecraft.getInstance();
        var block = stack.getItem() instanceof BlockItem;
        var scale = block ? BLOCK_SCALE : ITEM_SCALE;
        var height = block ? BLOCK_SCALE : ITEM_SCALE * 0.25F;

        // The cut plane, in the same camera-relative space the vertices arrive in.
        var bed = plate.getWorldPosition();
        var cameraY = minecraft.gameRenderer.getMainCamera().getPosition().y;
        var progress = fabricator.clientProgress(partialTick);
        var clipY = (float) (bed.y - cameraY - 0.01D + progress * (height + 0.02D));

        poseStack.pushPose();
        poseStack.mulPose(plate.getLocalSpaceMatrix());

        if (block) {
            poseStack.translate(0.0F, BLOCK_SCALE / 2.0F, 0.0F);
        } else {
            // On its side, flat on the bed.
            poseStack.translate(0.0F, 0.03F, 0.0F);
            poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
        }

        poseStack.scale(scale, scale, scale);

        var opened = new ArrayList<ClippingVertexConsumer>();

        minecraft.getItemRenderer()
            .renderStatic(
                stack,
                ItemDisplayContext.FIXED,
                packedLight,
                OverlayTexture.NO_OVERLAY,
                poseStack,
                ClippingVertexConsumer.wrap(buffers, clipY, opened),
                fabricator.getLevel(),
                (int) fabricator.getBlockPos().asLong()
            );

        opened.forEach(ClippingVertexConsumer::flush);
        poseStack.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen(@NotNull FabricatorBlockEntity blockEntity) {
        return true;
    }

    /** The fabricator is 2 wide and 2 tall around its core. */
    @Override
    public net.minecraft.world.phys.AABB renderBox(@NotNull FabricatorBlockEntity fabricator) {
        return new net.minecraft.world.phys.AABB(fabricator.getBlockPos()).inflate(2.0D);
    }
}
