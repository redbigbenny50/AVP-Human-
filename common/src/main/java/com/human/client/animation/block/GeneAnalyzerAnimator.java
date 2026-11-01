package com.human.client.animation.block;

import com.blib.api.client.animation.v1.animator.AzAnimatorConfig;
import com.blib.api.client.animation.v1.animator.AzBlockAnimator;
import com.blib.api.client.animation.v1.track.AzAnimationTrack;
import com.blib.api.client.animation.v1.track.AzAnimationTrackContainer;
import com.human.HumanResources;
import com.human.common.gameplay.gene.machine.analyzer.GeneAnalyzerBlockEntity;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

/** Two tracks: the machine (off / power_on / idle / load / scan / finish), and the DNA screens over the top. */
public class GeneAnalyzerAnimator extends AzBlockAnimator<GeneAnalyzerBlockEntity> {

    private static final ResourceLocation ANIMATIONS = HumanResources.blockAnimationLocation("gene_analyzer");

    public GeneAnalyzerAnimator() {
        super(AzAnimatorConfig.defaultConfig());
    }

    @Override
    public void registerTracks(AzAnimationTrackContainer<GeneAnalyzerBlockEntity> container) {
        container.add(AzAnimationTrack.builder(this, "base_controller").build());
        // Added second, so the DNA screens' scale keys win over the base track's while a progress clip holds.
        container.add(AzAnimationTrack.builder(this, GeneAnalyzerBlockEntity.DNA_TRACK).build());
    }

    @Override
    public @NotNull ResourceLocation getAnimationLocation(GeneAnalyzerBlockEntity animatable) {
        return ANIMATIONS;
    }
}
