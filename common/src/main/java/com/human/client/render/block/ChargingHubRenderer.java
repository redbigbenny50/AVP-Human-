package com.human.client.render.block;

import com.blib.api.client.render.v1.block.AzBlockEntityRenderer;
import com.blib.api.client.render.v1.block.AzBlockEntityRendererConfig;
import com.human.HumanResources;
import com.human.client.render.layer.AzOverlayTextureLayer;
import com.human.common.gameplay.block.entity.power.impl.ChargingHubBlockEntity;
import net.minecraft.resources.ResourceLocation;

/**
 * The charging hub (formerly the ESU), drawn in three passes over one model.
 * <h2>⚠⚠ THREE PASSES, ONE MODEL, AND THAT IS FORCED</h2> A render layer cannot swap the baked model -
 * {@code AzRendererPipelineContext} exposes {@code bakedModel()} as a getter only. So every pass redraws the SAME
 * geometry with the SAME UVs, and only the texture changes. That is why the animated plate sheet had to be rebuilt into
 * this model's 64-wide UV space: each overlay is transparent everywhere except the part it is meant to paint.
 * <ol>
 * <li>base - {@code charging_hub.png}, the whole block</li>
 * <li>plate - the animated screen on the angled face, translucent elsewhere</li>
 * <li>plate glow - the same screen, emissive, so it reads at night</li>
 * </ol>
 * ⚠ The body's own glow mask goes through {@code AzAutoGlowingLayer} as usual; only the animated plate needs the
 * overlay layer, because its texture is a separate animated file rather than a {@code _glowmask} sibling.
 */
public class ChargingHubRenderer extends AzBlockEntityRenderer<ChargingHubBlockEntity> {

    public static final String NAME = "charging_hub";

    private static final ResourceLocation MODEL_LOCATION = HumanResources.blockGeoModelLocation(NAME);

    private static final ResourceLocation TEXTURE = HumanResources.blockTextureLocation(NAME);

    /**
     * ⚠ Oct 16 - the animated plate is SIX SEPARATE FRAME TEXTURES ({@code _plate_0} to {@code _plate_5}, and the same
     * for the glow), stepped here by game time, 3 ticks a frame - not one strip with a {@code .mcmeta}. A strip is only
     * cropped and advanced when BLib's own animated-texture wrapper owns it and the renderer calls it each frame; this
     * overlay layer never did, and with another animated-texture mod installed (GeckoLib wraps the same lookup) the
     * strip could be drawn whole, squashed onto the plate. The quarry terminal's working screen showed exactly that.
     */
    private static final int PLATE_FRAMES = 6;

    private static final int PLATE_FRAME_TICKS = 3;

    private static final ResourceLocation[] PLATE = frames(NAME + "_plate_");

    private static final ResourceLocation[] PLATE_GLOW = frames(NAME + "_plate_glow_");

    private static ResourceLocation[] frames(String prefix) {
        var frames = new ResourceLocation[PLATE_FRAMES];

        for (int frame = 0; frame < PLATE_FRAMES; frame++) {
            frames[frame] = HumanResources.blockTextureLocation(prefix + frame);
        }

        return frames;
    }

    /** The plate frame showing now, the same for every charging hub (as the strip's animation was). */
    private static int plateFrame() {
        var level = net.minecraft.client.Minecraft.getInstance().level;

        return level == null ? 0 : (int) (level.getGameTime() / PLATE_FRAME_TICKS % PLATE_FRAMES);
    }

    /**
     * Where the pad item sits, taken from the {@code item_mount} bone in charging_hub.geo.json: pivot [0, 12.5, -4.5]
     * with a -45 degree tilt, converted to block space (bedrock units / 16, Z negated back out of the mirror).
     * <p>
     * ⚠ READ FROM THE MODEL FILE, NOT FROM THE POSED BONE AT RUNTIME. The renderer has no public accessor for its baked
     * model, and the bone is static anyway - it carries no animation - so resolving it per frame would buy nothing and
     * depend on internals. If the artist moves that bone, these three numbers move with it.
     * </p>
     */
    private static final float MOUNT_X = 0.0F;

    private static final float MOUNT_Y = 12.5F / 16.0F;

    private static final float MOUNT_Z = 4.5F / 16.0F;

    /**
     * ⚠ MUST MATCH the plate/item_mount rotation in charging_hub.geo.json. [stated] Sep 25 the authored -45 leaned the
     * ramp the wrong way and both were flipped to +45; a pad item posed at the old sign would sit through the plate.
     */
    private static final float MOUNT_TILT_DEGREES = 45.0F;

    /** Shrunk to sit on the plate rather than dominate it. */
    private static final float PAD_ITEM_SCALE = 0.4F;

    /**
     * Draws whatever is on the charging pad, at the {@code item_mount} bone.
     * <p>
     * ⚠ AFTER super.render, not before: the bone matrices are only meaningful once the model has been posed for this
     * frame. Reading them earlier gives the previous frame's pose, which shows up as the item lagging a tick behind the
     * block whenever anything animates.
     * </p>
     * <p>
     * ⚠ The item is drawn in its own pushPose/popPose. Leaving the matrix stack dirty here would corrupt every block
     * entity drawn after this one, which is the kind of fault that looks like an unrelated mod misbehaving.
     * </p>
     */
    @Override
    public void render(
        ChargingHubBlockEntity blockEntity,
        float partialTick,
        com.mojang.blaze3d.vertex.@org.jetbrains.annotations.NotNull PoseStack poseStack,
        net.minecraft.client.renderer.@org.jetbrains.annotations.NotNull MultiBufferSource bufferSource,
        int packedLight,
        int packedOverlay
    ) {
        super.render(blockEntity, partialTick, poseStack, bufferSource, packedLight, packedOverlay);

        var stack = blockEntity.getPadItem();

        if (stack.isEmpty()) {
            return;
        }

        var minecraft = net.minecraft.client.Minecraft.getInstance();
        var facing = blockEntity.getBlockState().hasProperty(com.human.common.gameplay.block.power.ChargingHubBlock.FACING)
            ? blockEntity.getBlockState().getValue(com.human.common.gameplay.block.power.ChargingHubBlock.FACING)
            : net.minecraft.core.Direction.NORTH;

        poseStack.pushPose();
        poseStack.translate(0.5D, 0.5D, 0.5D);
        // Turn with the block, so the pad stays on the angled FACE whichever way it was placed.
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-facing.toYRot()));
        poseStack.translate(MOUNT_X, MOUNT_Y, MOUNT_Z);
        poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(MOUNT_TILT_DEGREES));
        poseStack.scale(PAD_ITEM_SCALE, PAD_ITEM_SCALE, PAD_ITEM_SCALE);

        minecraft.getItemRenderer()
            .renderStatic(
                stack,
                net.minecraft.world.item.ItemDisplayContext.FIXED,
                packedLight,
                packedOverlay,
                poseStack,
                bufferSource,
                blockEntity.getLevel(),
                0
            );

        poseStack.popPose();
    }

    public ChargingHubRenderer() {
        super(
            AzBlockEntityRendererConfig.<ChargingHubBlockEntity>builder($ -> MODEL_LOCATION, $ -> TEXTURE)
                .addRenderLayer(new com.blib.api.client.render.v1.layer.AzAutoGlowingLayer<>())
                // ⚠ Restored after the layer was fixed: the holes came from the layer using a TRANSLUCENT render type
                // (which writes depth for transparent pixels), not from the passes themselves.
                .addRenderLayer(new AzOverlayTextureLayer<>($ -> PLATE[plateFrame()], false))
                .addRenderLayer(new AzOverlayTextureLayer<>($ -> PLATE_GLOW[plateFrame()], true))
                .build()
        );
    }
}
