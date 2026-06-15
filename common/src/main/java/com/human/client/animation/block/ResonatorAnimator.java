package com.human.client.animation.block;

import com.blib.api.client.animation.v1.animator.AzAnimatorConfig;
import com.blib.api.client.animation.v1.animator.AzBlockAnimator;
import com.blib.api.client.animation.v1.track.AzAnimationTrack;
import com.blib.api.client.animation.v1.track.AzAnimationTrackContainer;
import com.human.HumanResources;
import com.human.common.gameplay.block.entity.power.impl.ResonatorBlockEntity;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class ResonatorAnimator extends AzBlockAnimator<ResonatorBlockEntity> {

    private static final ResourceLocation ANIMATIONS = HumanResources.blockAnimationLocation("resonator");

    public ResonatorAnimator() {
        super(AzAnimatorConfig.defaultConfig());
    }

    @Override
    public void registerTracks(AzAnimationTrackContainer<ResonatorBlockEntity> animationControllerContainer) {
        animationControllerContainer.add(
            AzAnimationTrack.builder(this, "base_controller")
                .build()
        );
    }

    @Override
    public @NotNull ResourceLocation getAnimationLocation(ResonatorBlockEntity animatable) {
        return ANIMATIONS;
    }
}
