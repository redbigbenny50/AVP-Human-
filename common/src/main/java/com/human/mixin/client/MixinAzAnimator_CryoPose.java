package com.human.mixin.client;

import com.blib.api.client.animation.v1.animator.AzAnimator;
import com.human.client.render.cryo.CryoDisplay;
import com.human.client.render.cryo.CryoPose;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Holds a frozen xenomorph in its molt pose. [stated] Oct 2.
 * <p>
 * Every avp_alien animator decides its clips in {@code setCustomAnimations}, from the mob's live state, every frame -
 * so dispatching the pose from outside would be overwritten by the idle on the very next frame. {@code animate} is the
 * one place every animator's override is called from, so the call is redirected there: a cryo display copy gets
 * {@link CryoPose} INSTEAD of its normal logic, and everything else is untouched.
 * </p>
 */
@Mixin(value = AzAnimator.class, remap = false)
public abstract class MixinAzAnimator_CryoPose {

    @Redirect(
        method = "animate(Ljava/lang/Object;FZ)V",
        at = @At(
            value = "INVOKE",
            target = "Lcom/blib/api/client/animation/v1/animator/AzAnimator;setCustomAnimations(Ljava/lang/Object;F)V"
        )
    )
    private void avp_human$cryoPose(AzAnimator<?, ?> self, Object animatable, float partialTicks) {
        if (animatable instanceof Entity entity) {
            if (CryoDisplay.isDisplay(entity) && CryoPose.apply(self, entity)) {
                return;
            }

            if (com.human.client.render.cryo.CryoIcon.isIcon(entity) && CryoPose.applyIcon(self, entity)) {
                return;
            }

            // [stated] Oct 19 - a mob frozen solid starts no new clips (no attack, no new idle): its pose is held where
            // it froze (MixinAzModelRenderer_FrozenSolid).
            if (
                com.human.client.freeze.FrozenSolidClient.isFrozen(entity)
                    && !com.human.compatibility.avp_predator.HumanPredatorNet.isNettedOnClient(entity)
            ) {
                return;
            }
        }

        @SuppressWarnings("unchecked")
        var typed = (AzAnimator<Object, Object>) self;
        typed.setCustomAnimations(animatable, partialTicks);
    }
}
