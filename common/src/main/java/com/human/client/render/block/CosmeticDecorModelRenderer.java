package com.human.client.render.block;

import com.blib.api.client.render.v1.AzRendererPipelineContext;
import com.blib.api.client.render.v1.block.model.AzBlockEntityModelRenderer;
import com.blib.api.client.render.v1.block.pipeline.AzBlockEntityRendererPipeline;
import com.blib.api.client.render.v1.AzLayerRenderer;
import com.human.common.gameplay.cosmetic.CosmeticBlock;
import com.human.common.gameplay.cosmetic.CosmeticBlockEntity;
import com.human.common.gameplay.cosmetic.CosmeticModelTables;
import com.human.common.gameplay.cosmetic.CosmeticMount;
import com.human.common.gameplay.cosmetic.CosmeticMotion;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;

import java.util.List;

/**
 * The cosmetic decor's model renderer: BLib's block renderer plus two things -
 * <ul>
 * <li><b>Floor / ceiling mounts</b> ([stated] Oct 31): after BLib's facing turn, a wall piece lying on a floor turns
 * +90 degrees about X through the block centre (front up), on a ceiling -90 (front down). BLib's own UP/DOWN handling
 * cannot also keep the facing, which is why the turn is added here. Matches the baked models' blockstate x 270 / 90.</li>
 * <li><b>Doors, lids and spinners</b>: their bones are turned from the block state each frame - doors ease open and
 * shut, spinners wind up and coast down ([stated] Oct 31: right-click toggles them). The baked model is shared by
 * every block and item of the design, so the bones are put back to rest straight after drawing.</li>
 * </ul>
 */
public class CosmeticDecorModelRenderer extends AzBlockEntityModelRenderer<CosmeticBlockEntity> {

    /** Door / lid easing: fraction of the full swing per tick. */
    private static final float SWING_PER_TICK = 0.16F;

    /** Spinner wind-up / coast-down: fraction of full speed per tick. */
    private static final float SPIN_RAMP_PER_TICK = 0.05F;

    private CosmeticMount mount = CosmeticMount.WALL;

    public CosmeticDecorModelRenderer(
        AzBlockEntityRendererPipeline<CosmeticBlockEntity> pipeline,
        AzLayerRenderer<Long, CosmeticBlockEntity> layerRenderer
    ) {
        super(pipeline, layerRenderer);
    }

    @Override
    protected Direction getFacing(CosmeticBlockEntity block) {
        var state = block.getBlockState();
        mount = state.hasProperty(CosmeticBlock.MOUNT) ? state.getValue(CosmeticBlock.MOUNT) : CosmeticMount.WALL;

        return super.getFacing(block);
    }

    @Override
    protected void rotateBlock(Direction facing, PoseStack poseStack) {
        super.rotateBlock(facing, poseStack);

        if (mount == CosmeticMount.WALL) {
            return;
        }

        // BLib has already moved to the bottom centre of the block; the X turn is about the block's middle.
        poseStack.translate(0, 0.5, 0);
        poseStack.mulPose(Axis.XP.rotationDegrees(mount == CosmeticMount.FLOOR ? 90 : -90));
        poseStack.translate(0, -0.5, 0);
    }

    @Override
    public void render(AzRendererPipelineContext<Long, CosmeticBlockEntity> context, boolean isReRender) {
        var decor = context.animatable();
        var motions = CosmeticModelTables.MOTIONS.getOrDefault(decor.block().design().id(), List.of());
        var model = context.bakedModel();

        if (motions.isEmpty() || model == null) {
            super.render(context, isReRender);
            return;
        }

        if (!isReRender) {
            advance(decor, motions, context.partialTick());
        }

        for (var motion : motions) {
            var bone = model.getBoneOrNull(motion.bone());

            if (bone != null) {
                var degrees = motion.spin() ? decor.spinAngle : motion.amount() * ease(decor.openProgress);
                set(bone, motion.axis(), (float) Math.toRadians(degrees));
            }
        }

        try {
            super.render(context, isReRender);
        } finally {
            // The baked model is shared by every block and item of this design: leave it at rest.
            for (var motion : motions) {
                var bone = model.getBoneOrNull(motion.bone());

                if (bone != null) {
                    set(bone, motion.axis(), 0);
                }
            }
        }
    }

    /** Moves the door easing and spinner angle on by the time since this block was last drawn. */
    private static void advance(CosmeticBlockEntity decor, List<CosmeticMotion> motions, float partialTick) {
        var state = decor.getBlockState();
        var level = decor.getLevel();
        var now = (level == null ? 0 : level.getGameTime()) + (double) partialTick;
        var ticks = Double.isNaN(decor.lastDrawn) ? 0 : Mth.clamp(now - decor.lastDrawn, 0, 10);
        decor.lastDrawn = now;
        var open = state.hasProperty(CosmeticBlock.OPEN) && state.getValue(CosmeticBlock.OPEN);

        // First frame (chunk just loaded): start where the state says, no swing.
        if (Float.isNaN(decor.openProgress)) {
            decor.openProgress = open ? 1 : 0;
        }

        var target = open ? 1F : 0F;
        var step = (float) (SWING_PER_TICK * ticks);
        decor.openProgress = decor.openProgress < target
            ? Math.min(target, decor.openProgress + step)
            : Math.max(target, decor.openProgress - step);

        var spinning = state.hasProperty(CosmeticBlock.SPINNING) && state.getValue(CosmeticBlock.SPINNING);
        var ramp = (float) (SPIN_RAMP_PER_TICK * ticks);
        decor.spinSpeed = spinning ? Math.min(1, decor.spinSpeed + ramp) : Math.max(0, decor.spinSpeed - ramp);

        for (var motion : motions) {
            if (motion.spin()) {
                decor.spinAngle = (float) ((decor.spinAngle + motion.amount() * decor.spinSpeed * ticks) % 360);
                break;
            }
        }
    }

    /** Smoothstep, so doors start and finish their swing gently. */
    private static float ease(float t) {
        return t * t * (3 - 2 * t);
    }

    private static void set(com.blib.api.client.model.v1.AzBone bone, char axis, float radians) {
        switch (axis) {
            case 'x' -> bone.setRotX(radians);
            case 'z' -> bone.setRotZ(radians);
            default -> bone.setRotY(radians);
        }
    }
}
