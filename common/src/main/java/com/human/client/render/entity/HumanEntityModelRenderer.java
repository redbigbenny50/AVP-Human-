package com.human.client.render.entity;

import com.blib.api.client.render.v1.AzLayerRenderer;
import com.blib.api.client.render.v1.entity.model.AzEntityModelRenderer;
import com.blib.api.client.render.v1.entity.pipeline.AzEntityRendererPipeline;
import com.human.common.gameplay.entity.living.human.AbstractHuman;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.util.Mth;

import java.util.UUID;

/**
 * The Az model renderer for humans, plus the one thing vanilla's {@code PlayerRenderer} does that BLib's does not: the
 * whole-body tilt while swimming.
 * <p>
 * This is a straight port of {@code PlayerRenderer.setupRotations}'s swim branch, applied in the SAME frame vanilla
 * applies it in - right after the {@code Axis.YP.rotationDegrees(180 - yaw)} that both renderers do first, and before
 * anything model-specific. Because both sides rotate in the entity frame, the port needs no sign conversion; the Azure
 * sign flips only matter once you are writing to a bone, and this touches no bone.
 * <p>
 * The limb stroke that goes with it (the crawl arms, the leg kick, the raised head) lives in {@code MarineAnimator},
 * because those are bone writes.
 */
public class HumanEntityModelRenderer<T extends AbstractHuman> extends AzEntityModelRenderer<T> {

    /** Vanilla: a swimmer lies flat, and in water follows its own pitch on top of that. */
    private static final float SWIM_TILT_DEGREES = -90.0F;

    /** Vanilla's offset so the flat body sits at water level rather than pivoting at the feet. */
    private static final float SWIM_OFFSET_Y = -1.0F;

    private static final float SWIM_OFFSET_Z = 0.3F;

    public HumanEntityModelRenderer(
        AzEntityRendererPipeline<T> entityRendererPipeline,
        AzLayerRenderer<UUID, T> layerRenderer
    ) {
        super(entityRendererPipeline, layerRenderer);
    }

    @Override
    protected void applyRotations(
        T animatable,
        PoseStack poseStack,
        float ageInTicks,
        float rotationYaw,
        float partialTick,
        float nativeScale
    ) {
        // ⚠⚠ Oct 5 - A PIT SCRAMBLE IS UPRIGHT, CHEST TO THE WALL. [stated] "make sure they are rotated so the front of
        // their bodies is against the wall and they arent horizontal swimming into the block". The scramble holds the
        // swimming pose for its low box and its stroke, but the flat swim tilt below would lay the body out sideways
        // into the bank. So: no tilt, no swim offset, and the yaw comes from the synced wall direction rather than the
        // body rotation the client interpolates - that can lag or drift while the body is not moving horizontally, and
        // the facing must be exact for the chest to meet the wall.
        var scrambleDirection = animatable.scrambleDirection.get();

        if (scrambleDirection >= 0) {
            var wallYaw = net.minecraft.core.Direction.from2DDataValue(scrambleDirection).toYRot();
            super.applyRotations(animatable, poseStack, ageInTicks, wallYaw, partialTick, nativeScale);
            return;
        }

        super.applyRotations(animatable, poseStack, ageInTicks, rotationYaw, partialTick, nativeScale);

        var swimAmount = animatable.getSwimAmount(partialTick);

        if (swimAmount <= 0.0F) {
            return;
        }

        var tilt = animatable.isInWater() ? SWIM_TILT_DEGREES - animatable.getXRot() : SWIM_TILT_DEGREES;

        poseStack.mulPose(Axis.XP.rotationDegrees(Mth.lerp(swimAmount, 0.0F, tilt)));

        if (animatable.isVisuallySwimming()) {
            poseStack.translate(0.0F, SWIM_OFFSET_Y, SWIM_OFFSET_Z);
        }
    }
}
