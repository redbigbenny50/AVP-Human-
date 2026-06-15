package com.human.client.animation.item;

import com.blib.api.client.animation.v1.animator.AzAnimatorConfig;
import com.blib.api.client.animation.v1.animator.AzItemAnimator;
import com.blib.api.client.animation.v1.track.AzAnimationTrack;
import com.blib.api.client.animation.v1.track.AzAnimationTrackContainer;
import com.human.HumanResources;
import com.human.common.gameplay.item.gun.animation.dispatcher.impl.OldPainlessAnimationDispatcher;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class OldPainlessAnimator extends AzItemAnimator {

    private static final String NAME = "old_painless";

    private static final ResourceLocation ANIMATION = HumanResources.itemAnimationLocation(NAME);

    public OldPainlessAnimator() {
        super(AzAnimatorConfig.defaultConfig());
    }

    @Override
    public void registerTracks(AzAnimationTrackContainer<ItemStack> animationControllerContainer) {
        animationControllerContainer.add(
            AzAnimationTrack.builder(this, OldPainlessAnimationDispatcher.CONTROLLER_MAIN)
                .setTransitionLength(5)
                .build()
        );
    }

    @Override
    public @NotNull ResourceLocation getAnimationLocation(ItemStack animatable) {
        return ANIMATION;
    }
}
