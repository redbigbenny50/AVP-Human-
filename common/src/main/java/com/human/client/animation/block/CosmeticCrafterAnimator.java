package com.human.client.animation.block;

import com.blib.api.client.animation.v1.animator.AzAnimatorConfig;
import com.blib.api.client.animation.v1.animator.AzBlockAnimator;
import com.blib.api.client.animation.v1.track.AzAnimationTrack;
import com.blib.api.client.animation.v1.track.AzAnimationTrackContainer;
import com.human.HumanResources;
import com.human.common.gameplay.cosmetic.CosmeticCrafterBlockEntity;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

/** Carries cosmetic_crafter.animation.json: {@code idle.loop}, {@code crafting.loop} and {@code complete}. */
public class CosmeticCrafterAnimator extends AzBlockAnimator<CosmeticCrafterBlockEntity> {

    private static final ResourceLocation ANIMATIONS = HumanResources.blockAnimationLocation("cosmetic_crafter");

    public CosmeticCrafterAnimator() {
        super(AzAnimatorConfig.defaultConfig());
    }

    @Override
    public void registerTracks(AzAnimationTrackContainer<CosmeticCrafterBlockEntity> tracks) {
        tracks.add(AzAnimationTrack.builder(this, "base_controller").build());
    }

    @Override
    public @NotNull ResourceLocation getAnimationLocation(CosmeticCrafterBlockEntity animatable) {
        return ANIMATIONS;
    }
}
