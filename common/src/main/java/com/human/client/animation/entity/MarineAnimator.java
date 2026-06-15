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
        var leftArm = boneCache.getBakedModel().getBoneOrNull("gLeftArm");
        var rightArm = boneCache.getBakedModel().getBoneOrNull("gRightArm");
        var leftLeg = boneCache.getBakedModel().getBoneOrNull("gLeftLeg");
        var rightLeg = boneCache.getBakedModel().getBoneOrNull("gRightLeg");

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
