package com.human.client.animation.block;

import com.blib.api.client.animation.v1.animator.AzAnimatorConfig;
import com.blib.api.client.animation.v1.animator.AzBlockAnimator;
import com.blib.api.client.animation.v1.track.AzAnimationTrack;
import com.blib.api.client.animation.v1.track.AzAnimationTrackContainer;
import com.human.HumanResources;
import com.human.common.gameplay.gene.machine.separator.GeneSeparatorBlockEntity;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

/** Three tracks whose bones never overlap: the lid and clamp, the rotor and buckets, and the screen's power bars. */
public class GeneSeparatorAnimator extends AzBlockAnimator<GeneSeparatorBlockEntity> {

    private static final ResourceLocation ANIMATIONS = HumanResources.blockAnimationLocation("gene_separator");

    public GeneSeparatorAnimator() {
        super(AzAnimatorConfig.defaultConfig());
    }

    @Override
    public void registerTracks(AzAnimationTrackContainer<GeneSeparatorBlockEntity> container) {
        container.add(AzAnimationTrack.builder(this, GeneSeparatorBlockEntity.LID_TRACK).build());
        container.add(AzAnimationTrack.builder(this, GeneSeparatorBlockEntity.ROTOR_TRACK).build());
        container.add(AzAnimationTrack.builder(this, GeneSeparatorBlockEntity.SCREEN_TRACK).build());
    }

    @Override
    public @NotNull ResourceLocation getAnimationLocation(GeneSeparatorBlockEntity animatable) {
        return ANIMATIONS;
    }
}
