package com.human.client.animation.block;

import com.blib.api.client.animation.v1.animator.AzAnimatorConfig;
import com.blib.api.client.animation.v1.animator.AzBlockAnimator;
import com.blib.api.client.animation.v1.track.AzAnimationTrack;
import com.blib.api.client.animation.v1.track.AzAnimationTrackContainer;
import com.human.HumanResources;
import com.human.common.gameplay.fabrication.FabricatorBlockEntity;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

/** Carries fabricator.animation.json: {@code fabricating} while printing, {@code idle.loop} otherwise. */
public class FabricatorAnimator extends AzBlockAnimator<FabricatorBlockEntity> {

    private static final ResourceLocation ANIMATIONS = HumanResources.blockAnimationLocation("fabricator");

    public FabricatorAnimator() {
        super(AzAnimatorConfig.defaultConfig());
    }

    @Override
    public void registerTracks(AzAnimationTrackContainer<FabricatorBlockEntity> tracks) {
        tracks.add(AzAnimationTrack.builder(this, "base_controller").build());
    }

    @Override
    public @NotNull ResourceLocation getAnimationLocation(FabricatorBlockEntity animatable) {
        return ANIMATIONS;
    }
}
