package com.human.mixin.client;

import com.blib.internal.client.animation.AzAnimationTimer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Lets a frozen-solid mob's animation clock be held still without a jump when it thaws: while frozen its timer is not
 * ticked, and its "last ticked" time is kept at the present, so the first tick after thawing advances by one frame, not
 * by the whole time it spent frozen (see {@link MixinAzModelRenderer_FrozenSolid}).
 */
@Mixin(value = AzAnimationTimer.class, remap = false)
public interface AzAnimationTimerAccessor {

    @Accessor("lastGameTickTime")
    void avp_human$setLastGameTickTime(double lastGameTickTime);
}
