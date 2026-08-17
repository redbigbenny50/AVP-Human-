package com.human.client.animation.item;

import com.blib.api.client.animation.v1.track.AzAnimationTrack;
import com.blib.api.client.animation.v1.track.AzAnimationTrackContainer;
import com.human.HumanResources;
import com.human.common.gameplay.item.gun.animation.dispatcher.impl.OldPainlessAnimationDispatcher;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class OldPainlessAnimator extends GunItemAnimator {

    private static final String NAME = "old_painless";

    private static final ResourceLocation ANIMATION = HumanResources.itemAnimationLocation(NAME);

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

    /**
     * ⚠⚠ DELIBERATELY DOES NOT CALL {@code runGunAnimationEvents}, UNLIKE EVERY OTHER GUN ANIMATOR.
     * <p>
     * Old Painless's barrels are turned PROCEDURALLY by {@code OldPainlessItemRenderer}, which sets gBarrel's rotZ
     * every frame. Wiring the animation clips up here as well made two things rotate the same bone, which renders as a
     * spinning model ghosting over a still one - most obvious as red/grey checkering once the heat layer tints it.
     * </p>
     * <p>
     * ⚠ animation.barrelspinup / barrelspinloop / barrelspindown still exist in old_painless.animation.json and are
     * still wired through OldPainlessAnimationDispatcher, but NOTHING dispatches them and nothing should. If you want
     * to move to clip-driven barrels, delete the rotZ code in the renderer FIRST.
     * </p>
     */
    @Override
    public void setCustomAnimations(ItemStack animatable, float partialTicks) {
        super.setCustomAnimations(animatable, partialTicks);
    }
}
