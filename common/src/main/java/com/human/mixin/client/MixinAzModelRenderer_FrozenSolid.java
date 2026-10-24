package com.human.mixin.client;

import com.blib.api.client.animation.v1.animator.AzAnimator;
import com.blib.api.client.render.v1.AzModelRenderer;
import com.blib.internal.client.render.util.RenderUtil;
import com.human.client.freeze.FrozenSolidClient;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * [stated] Oct 19 (tester): "When frozen ... the xenos continue their idle animations". A mob frozen solid is a statue:
 * its animation stops on whatever frame it froze in.
 * <p>
 * Every BLib model is posed through {@code AzModelRenderer.handleAnimation}. For a frozen mob it is posed WITHOUT
 * ticking its animation clock ({@code animate(..., false)}): every track reads that clock, so the pose holds exactly
 * where it was. It is still posed every frame - skipping the pose entirely would leave the model's shared bones in
 * whatever pose the last OTHER mob of that kind was drawn in. The clock's "last ticked" time is kept at the present, so
 * on thawing it carries on from the same frame instead of jumping ahead by the time it spent frozen.
 * </p>
 * New clips are not started while frozen either - {@code MixinAzAnimator_CryoPose} skips the animator's own clip choice
 * for a frozen mob, so it cannot begin an attack or a new idle.
 */
@Mixin(value = AzModelRenderer.class, remap = false)
public abstract class MixinAzModelRenderer_FrozenSolid {

    @Inject(method = "handleAnimation", at = @At("HEAD"), cancellable = true)
    private void avp_human$holdFrozenPose(AzAnimator<?, ?> animator, Object animatable, float partialTick, CallbackInfo callback) {
        if (!(animatable instanceof Entity entity) || !FrozenSolidClient.isFrozen(entity)) {
            return;
        }

        // [stated] Oct 19 - "The net pose takes priority if it has to choose how its posed": a netted mob is posed by
        // its own animator (the net pose), frozen or not. The frost and the net are both still drawn over it.
        if (com.human.compatibility.avp_predator.HumanPredatorNet.isNettedOnClient(entity)) {
            return;
        }

        @SuppressWarnings("unchecked")
        var typed = (AzAnimator<Object, Object>) animator;

        typed.animate(animatable, partialTick, false);
        ((AzAnimationTimerAccessor) typed.context().timer()).avp_human$setLastGameTickTime(RenderUtil.getCurrentTick());
        callback.cancel();
    }
}
