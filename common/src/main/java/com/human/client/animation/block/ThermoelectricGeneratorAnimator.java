package com.human.client.animation.block;

import com.blib.api.client.animation.v1.animator.AzAnimatorConfig;
import com.blib.api.client.animation.v1.animator.AzBlockAnimator;
import com.blib.api.client.animation.v1.track.AzAnimationTrack;
import com.blib.api.client.animation.v1.track.AzAnimationTrackContainer;
import com.human.HumanResources;
import com.human.common.gameplay.block.entity.power.impl.ThermoelectricGeneratorBlockEntity;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

/**
 * Carries {@code fan_spin} and {@code spinhold}. ⚠ Neither is dispatched - the fan angle is computed from world time in
 * the renderer so every generator spins in step and the 50/100/200% speeds are exact.
 */
public class ThermoelectricGeneratorAnimator extends AzBlockAnimator<ThermoelectricGeneratorBlockEntity> {

    private static final ResourceLocation ANIMATIONS = HumanResources.blockAnimationLocation("thermoelectric_generator");

    public ThermoelectricGeneratorAnimator() {
        super(AzAnimatorConfig.defaultConfig());
    }

    @Override
    public void registerTracks(
        AzAnimationTrackContainer<ThermoelectricGeneratorBlockEntity> animationControllerContainer
    ) {
        animationControllerContainer.add(
            AzAnimationTrack.builder(this, "base_controller")
                .build()
        );
    }

    @Override
    public @NotNull ResourceLocation getAnimationLocation(ThermoelectricGeneratorBlockEntity animatable) {
        return ANIMATIONS;
    }
}
