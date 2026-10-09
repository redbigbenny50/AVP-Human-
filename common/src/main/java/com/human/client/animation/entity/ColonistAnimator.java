package com.human.client.animation.entity;

import com.blib.api.client.animation.v1.BLibEntityAnimationUtils;
import com.blib.api.client.animation.v1.animator.AzEntityAnimator;
import com.blib.api.client.animation.v1.track.AzAnimationTrack;
import com.blib.api.client.animation.v1.track.AzAnimationTrackContainer;
import com.human.HumanResources;
import com.human.common.gameplay.entity.living.human.colonist.ColonistEntity;
import com.human.common.gameplay.entity.living.human.marine.MarineAnimationRefs;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;

/** The marines' animation file and the same player-like walk: arms and legs swing from the walk animation. */
public class ColonistAnimator extends AzEntityAnimator<ColonistEntity> {

    private static final ResourceLocation ANIMATION = HumanResources.location("animations/entity/marine.animation.json");

    @Override
    public void registerTracks(AzAnimationTrackContainer<ColonistEntity> container) {
        container.add(AzAnimationTrack.builder(this, MarineAnimationRefs.FULL_BODY_CONTROLLER_NAME).setTransitionLength(5).build());
    }

    @Override
    public @NotNull ResourceLocation getAnimationLocation(ColonistEntity animatable) {
        return ANIMATION;
    }

    @Override
    public void setCustomAnimations(ColonistEntity animatable, float partialTicks) {
        super.setCustomAnimations(animatable, partialTicks);
        BLibEntityAnimationUtils.applyHeadRotations(animatable, context(), partialTicks, "gHead", 0F);

        var model = context().boneCache().getBakedModel();
        var leftArm = model.getBoneOrNull("gLeftArm");
        var rightArm = model.getBoneOrNull("gRightArm");
        var leftLeg = model.getBoneOrNull("gLeftLeg");
        var rightLeg = model.getBoneOrNull("gRightLeg");
        var position = animatable.walkAnimation.position(partialTicks) * 0.6662F;
        var speed = animatable.walkAnimation.speed() * 0.9F;

        if (leftArm != null) {
            leftArm.setRotX(Mth.cos(position) * speed);
        }

        if (rightArm != null) {
            rightArm.setRotX(Mth.cos(position + Mth.PI) * speed);
        }

        if (leftLeg != null) {
            leftLeg.setRotX(Mth.cos(position + Mth.PI) * 1.4F * speed);
        }

        if (rightLeg != null) {
            rightLeg.setRotX(Mth.cos(position) * 1.4F * speed);
        }
    }
}
