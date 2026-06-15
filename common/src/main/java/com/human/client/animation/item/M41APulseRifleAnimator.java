package com.human.client.animation.item;

import com.blib.api.client.animation.v1.animator.AzAnimatorConfig;
import com.blib.api.client.animation.v1.animator.AzItemAnimator;
import com.blib.api.client.animation.v1.track.AzAnimationTrack;
import com.blib.api.client.animation.v1.track.AzAnimationTrackContainer;
import com.human.HumanResources;
import com.human.common.gameplay.item.gun.animation.dispatcher.impl.DefaultGunAnimationDispatcher;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class M41APulseRifleAnimator extends AzItemAnimator {

    private static final String NAME = "m41a_pulse_rifle";

    private static final ResourceLocation ANIMATION = HumanResources.itemAnimationLocation(NAME);

    public M41APulseRifleAnimator() {
        super(AzAnimatorConfig.defaultConfig());
    }

    @Override
    public void registerTracks(AzAnimationTrackContainer<ItemStack> animationControllerContainer) {
        animationControllerContainer.add(
            AzAnimationTrack.builder(this, DefaultGunAnimationDispatcher.CONTROLLER_MAIN)
                .setTransitionLength(1)
                .build()
        );
    }

    @Override
    public @NotNull ResourceLocation getAnimationLocation(ItemStack animatable) {
        return ANIMATION;
    }
}
