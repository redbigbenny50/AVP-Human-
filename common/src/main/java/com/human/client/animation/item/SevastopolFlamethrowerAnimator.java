package com.human.client.animation.item;

import com.blib.api.client.animation.v1.track.AzAnimationTrack;
import com.blib.api.client.animation.v1.track.AzAnimationTrackContainer;
import com.human.HumanResources;
import com.human.common.gameplay.item.gun.animation.dispatcher.impl.DefaultGunAnimationDispatcher;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class SevastopolFlamethrowerAnimator extends GunItemAnimator {

    private static final String NAME = "flamethrower_sevastopol";

    private static final ResourceLocation ANIMATION = HumanResources.itemAnimationLocation(NAME);

    @Override
    public void registerTracks(AzAnimationTrackContainer<ItemStack> animationControllerContainer) {
        animationControllerContainer.add(
            // TODO: Fix this OldPainlessAnimationRefs reference.
            AzAnimationTrack.builder(this, DefaultGunAnimationDispatcher.CONTROLLER_MAIN)
                .setTransitionLength(1)
                .build()
        );
    }

    @Override
    public @NotNull ResourceLocation getAnimationLocation(ItemStack animatable) {
        return ANIMATION;
    }

    @Override
    public void setCustomAnimations(ItemStack animatable, float partialTicks) {
        super.setCustomAnimations(animatable, partialTicks);
        runGunAnimationEvents(animatable, DefaultGunAnimationDispatcher.INSTANCE);
    }
}
