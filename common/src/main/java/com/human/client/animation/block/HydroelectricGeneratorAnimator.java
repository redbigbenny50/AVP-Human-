package com.human.client.animation.block;

import com.blib.api.client.animation.v1.animator.AzAnimatorConfig;
import com.blib.api.client.animation.v1.animator.AzBlockAnimator;
import com.blib.api.client.animation.v1.track.AzAnimationTrack;
import com.blib.api.client.animation.v1.track.AzAnimationTrackContainer;
import com.human.HumanResources;
import com.human.common.gameplay.block.entity.power.impl.HydroelectricGeneratorBlockEntity;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

/**
 * Carries the wheel's clips ({@code heg.forwardspin}, {@code heg.backspin}, {@code heg.hold}). ⚠ NOTHING DISPATCHES
 * THEM - the spin is driven from world time in {@link com.human.client.render.block.HydroelectricGeneratorRenderer} so
 * neighbouring sections can never drift out of phase. The clips stay registered as the source of the motion and as the
 * route back to clip-driven spinning if that is ever wanted; if you switch, delete the rotY code in the renderer FIRST.
 */
public class HydroelectricGeneratorAnimator extends AzBlockAnimator<HydroelectricGeneratorBlockEntity> {

    private static final ResourceLocation ANIMATIONS = HumanResources.blockAnimationLocation("hydroelectric_generator");

    public HydroelectricGeneratorAnimator() {
        super(AzAnimatorConfig.defaultConfig());
    }

    @Override
    public void registerTracks(
        AzAnimationTrackContainer<HydroelectricGeneratorBlockEntity> animationControllerContainer
    ) {
        animationControllerContainer.add(
            AzAnimationTrack.builder(this, "base_controller")
                .build()
        );
    }

    @Override
    public @NotNull ResourceLocation getAnimationLocation(HydroelectricGeneratorBlockEntity animatable) {
        return ANIMATIONS;
    }
}
