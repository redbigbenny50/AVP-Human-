package com.human.client.animation.entity;

import com.blib.api.client.animation.v1.animator.AzEntityAnimator;
import com.blib.api.client.animation.v1.track.AzAnimationTrack;
import com.blib.api.client.animation.v1.track.AzAnimationTrackContainer;
import com.human.HumanResources;
import com.human.common.gameplay.entity.nuke.MushroomCloudEntity;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class MushroomCloudAnimator extends AzEntityAnimator<MushroomCloudEntity> {

    private static final ResourceLocation ANIMATIONS = HumanResources.entityAnimationLocation("mushroom_cloud");

    @Override
    public void registerTracks(AzAnimationTrackContainer<MushroomCloudEntity> animationControllerContainer) {
        animationControllerContainer.add(
            AzAnimationTrack.builder(this, "base_controller")
                .build()
        );
    }

    @Override
    public @NotNull ResourceLocation getAnimationLocation(MushroomCloudEntity animatable) {
        return ANIMATIONS;
    }
}
