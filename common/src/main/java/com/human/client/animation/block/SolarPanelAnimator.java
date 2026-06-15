package com.human.client.animation.block;

import com.blib.api.client.animation.v1.animator.AzAnimatorConfig;
import com.blib.api.client.animation.v1.animator.AzBlockAnimator;
import com.blib.api.client.animation.v1.track.AzAnimationTrack;
import com.blib.api.client.animation.v1.track.AzAnimationTrackContainer;
import com.human.HumanResources;
import com.human.common.gameplay.block.entity.power.impl.SolarPanelBlockEntity;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class SolarPanelAnimator extends AzBlockAnimator<SolarPanelBlockEntity> {

    private static final ResourceLocation ANIMATIONS = HumanResources.blockAnimationLocation("solar_panel");

    public SolarPanelAnimator() {
        super(AzAnimatorConfig.defaultConfig());
    }

    @Override
    public void registerTracks(AzAnimationTrackContainer<SolarPanelBlockEntity> animationControllerContainer) {
        animationControllerContainer.add(
            AzAnimationTrack.builder(this, "base_controller")
                .build()
        );
    }

    @Override
    public @NotNull ResourceLocation getAnimationLocation(SolarPanelBlockEntity animatable) {
        return ANIMATIONS;
    }
}
