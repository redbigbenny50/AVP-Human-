package com.human.mixin;

import com.human.common.gameplay.freeze.FrozenSolid;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * A mob frozen solid thinks nothing: its whole server AI step — goals, targets, navigation, look, BLib's GOAP — is
 * skipped while the Frozen Solid effect is on it. It picks up exactly where it left off when it thaws. It can also be
 * put on a lead while frozen, hostiles included, so it can be dragged away.
 */
@Mixin(Mob.class)
public abstract class MixinMob_FrozenSolid {

    @Inject(method = "serverAiStep", at = @At("HEAD"), cancellable = true)
    private void avp_human$frozenSolid(CallbackInfo callback) {
        var self = (Mob) (Object) this;

        if (FrozenSolid.isFrozen(self)) {
            callback.cancel();
            return;
        }

        // Thawed: a hostile that was only on a lead because it was frozen breaks free of it, and the lead drops. The
        // cord
        // and the capture chain are made to hold hostiles, so they stay on.
        if (self.getTags().contains(FrozenSolid.FROZEN_LEASH_TAG)) {
            self.removeTag(FrozenSolid.FROZEN_LEASH_TAG);

            if (self.isLeashed() && !self.canBeLeashed()) {
                self.dropLeash(true, true);
            }
        }
    }

    /**
     * [stated] Oct 4: "you should be able to attach a lead ... to a frozen mob and drag it around". Vanilla refuses a
     * lead on anything hostile ({@code Mob.canBeLeashed} is false for every Enemy) — a frozen one takes one.
     * <p>
     * ⚠ This ADDS Mob's own copy of {@code Leashable.canHaveALeashAttachedToIt}, which vanilla only has as the
     * interface's default — the one check {@code Entity.interact} makes before a lead is tied. Verified in the 1.21.1
     * jar: nothing else overrides it and nothing else calls it. It keeps vanilla's rule exactly (can be leashed and is
     * not already) and only adds "or is frozen solid".
     * </p>
     */
    public boolean canHaveALeashAttachedToIt() {
        var self = (Mob) (Object) this;

        return (self.canBeLeashed() || FrozenSolid.isFrozen(self)) && !self.isLeashed();
    }
}
