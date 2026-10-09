package com.human.client.animation.block;

import com.blib.api.client.animation.v1.animator.AzAnimatorConfig;
import com.blib.api.client.animation.v1.animator.AzBlockAnimator;
import com.blib.api.client.animation.v1.track.AzAnimationTrack;
import com.blib.api.client.animation.v1.track.AzAnimationTrackContainer;
import com.human.HumanResources;
import com.human.common.gameplay.block.entity.power.impl.AerokineticGeneratorBlockEntity;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

/**
 * Carries {@code akg.spin} and {@code akg.hold}. ⚠ Neither is dispatched - the rotor angle is computed from world time
 * in the renderer so neighbouring turbines turn together and the 50/100/150% speeds are exact. The clips remain the
 * source of the motion; if you ever switch to playing them, delete the rotY code in the renderer FIRST.
 */
public class AerokineticGeneratorAnimator extends AzBlockAnimator<AerokineticGeneratorBlockEntity> {

    private static final ResourceLocation ANIMATIONS = HumanResources.blockAnimationLocation("aerokinetic_generator");

    public AerokineticGeneratorAnimator() {
        super(AzAnimatorConfig.defaultConfig());
    }

    @Override
    public void registerTracks(AzAnimationTrackContainer<AerokineticGeneratorBlockEntity> animationControllerContainer) {
        animationControllerContainer.add(
            AzAnimationTrack.builder(this, "base_controller")
                .build()
        );
    }

    @Override
    public @NotNull ResourceLocation getAnimationLocation(AerokineticGeneratorBlockEntity animatable) {
        return ANIMATIONS;
    }
}
