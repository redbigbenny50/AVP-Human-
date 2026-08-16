package com.human.client.animation.entity;

import com.blib.api.client.animation.v1.BLibEntityAnimationUtils;
import com.blib.api.client.animation.v1.animator.AzEntityAnimator;
import com.blib.api.client.animation.v1.track.AzAnimationTrack;
import com.blib.api.client.animation.v1.track.AzAnimationTrackContainer;
import com.human.HumanResources;
import com.human.common.gameplay.entity.living.human.marine.Marine;
import com.human.common.gameplay.entity.living.human.marine.MarineAnimationRefs;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;

public class MarineAnimator extends AzEntityAnimator<Marine> {

    private static final String NAME = "marine";

    private static final ResourceLocation ANIMATION = HumanResources.entityAnimationLocation(NAME);

    /**
     * Vanilla's crossbow hold, from {@code AnimationUtils.animateCrossbowHold}, WITH EVERY CONSTANT NEGATED.
     * <p>
     * ⚠⚠ THE SIGNS ARE FLIPPED ON PURPOSE AND THAT IS THE WHOLE FIX. These numbers were lifted from vanilla
     * {@code HumanoidModel}, which drives {@code ModelPart}. Azure bones turn the OPPOSITE WAY on both axes - BLib's
     * own {@code applyHeadRotations} is the proof, since it negates the head's pitch AND its yaw before writing them to
     * a bone. Copying vanilla's constants across without flipping them pointed the arms the wrong way, and for the
     * pitch that meant {@code -PI/2} swung the rifle up over the shoulder and BACKWARDS rather than forwards - a
     * constant error of almost exactly 180 degrees, no matter where the target was or whether the marine was moving.
     * <p>
     * Converting properly: vanilla asks for {@code armV = -PI/2 + headV}; an Azure bone needs {@code armA = -armV}, and
     * {@code headA = -headV} already, so it works out to {@code armA = +PI/2 + headA}. The constant flips, the head
     * term stays added. Same derivation for every offset below.
     */
    private static final float FIRING_ARM_YAW_OFFSET = 0.3F;

    private static final float SUPPORT_ARM_YAW_OFFSET = -0.6F;

    private static final float FIRING_ARM_PITCH_OFFSET = -0.1F;

    private static final float SUPPORT_ARM_PITCH = 1.5F;

    @Override
    public void registerTracks(AzAnimationTrackContainer<Marine> animationControllerContainer) {
        animationControllerContainer.add(
            AzAnimationTrack.builder(this, MarineAnimationRefs.FULL_BODY_CONTROLLER_NAME)
                .setTransitionLength(5)
                .build()
        );
    }

    @Override
    public @NotNull ResourceLocation getAnimationLocation(Marine animatable) {
        return ANIMATION;
    }

    @Override
    public void setCustomAnimations(Marine animatable, float partialTicks) {
        super.setCustomAnimations(animatable, partialTicks);

        BLibEntityAnimationUtils.applyHeadRotations(animatable, context(), partialTicks, "gHead", 0F);

        var boneCache = this.context().boneCache();
        var head = boneCache.getBakedModel().getBoneOrNull("gHead");
        var leftArm = boneCache.getBakedModel().getBoneOrNull("gLeftArm");
        var rightArm = boneCache.getBakedModel().getBoneOrNull("gRightArm");
        var leftLeg = boneCache.getBakedModel().getBoneOrNull("gLeftLeg");
        var rightLeg = boneCache.getBakedModel().getBoneOrNull("gRightLeg");

        // The weapon hangs off rightHand_Item, which is a child of gRightArm, so the gun points wherever that bone
        // points and nowhere else. Nothing was aiming it: the walk swing below is skipped while aggressive, and the
        // only animation that touches the arm is a fixed -85 degree pose barely one tick long, so the arm sat in its
        // bind pose -- straight down -- for the whole engagement.
        //
        // These marines are posed the way a vanilla player is, so the aim pose is vanilla's two-handed ranged hold
        // rather than anything bespoke. It is driven off the head bone instead of the entity's own pitch and yaw,
        // because applyHeadRotations has already resolved the aim above; reading it back keeps the muzzle exactly on
        // the line the marine is looking along, elevation included, and inherits its sign conventions for free.
        if (head != null && animatable.isAggressive()) {
            if (rightArm != null) {
                rightArm.setRotY(FIRING_ARM_YAW_OFFSET + head.getRotY());
                rightArm.setRotX(Mth.HALF_PI + head.getRotX() + FIRING_ARM_PITCH_OFFSET);
            }

            if (leftArm != null) {
                leftArm.setRotY(SUPPORT_ARM_YAW_OFFSET + head.getRotY());
                leftArm.setRotX(SUPPORT_ARM_PITCH + head.getRotX());
            }
        }

        if (leftArm != null && !animatable.isAggressive()) {
            leftArm.setRotX(
                Mth.cos(
                    animatable.walkAnimation.position(
                        partialTicks
                    ) * 0.6662F
                ) * 1.0F * animatable.walkAnimation.speed() * 0.9F
            );
        }

        if (rightArm != null && !animatable.isAggressive()) {
            rightArm.setRotX(
                Mth.cos(
                    animatable.walkAnimation.position(
                        partialTicks
                    ) * 0.6662F + ((float) Math.PI)
                ) * 1.0F * animatable.walkAnimation.speed() * 0.9F
            );
        }

        if (leftLeg != null) {
            leftLeg.setRotX(
                Mth.cos(
                    animatable.walkAnimation.position(
                        partialTicks
                    ) * 0.6662F + ((float) Math.PI)
                ) * 1.4F * animatable.walkAnimation.speed() * 0.9F
            );
        }

        if (rightLeg != null) {
            rightLeg.setRotX(
                Mth.cos(
                    animatable.walkAnimation.position(
                        partialTicks
                    ) * 0.6662F
                ) * 1.4F * animatable.walkAnimation.speed() * 0.9F
            );
        }
    }
}
