package com.human.common.gameplay.item.gun.animation.dispatcher.impl;

import com.blib.api.client.animation.v1.command.AzCommand;
import com.blib.api.client.animation.v1.command.AzTarget;
import com.blib.api.client.animation.v1.command.play_behavior.AzPlayBehaviors;
import com.human.common.gameplay.item.gun.animation.dispatcher.GunAnimationDispatcher;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

public class OldPainlessAnimationDispatcher implements GunAnimationDispatcher {

    public static final OldPainlessAnimationDispatcher INSTANCE = new OldPainlessAnimationDispatcher();

    public static final String CONTROLLER_MAIN = "main";

    public static final String ANIMATION_SPIN_DOWN = "animation.barrelspindown";

    public static final String ANIMATION_SPIN_LOOP = "animation.barrelspinloop";

    public static final String ANIMATION_SPIN_UP = "animation.barrelspinup";

    private final AzCommand<ItemStack> SPIN_LOOP = AzCommand.<ItemStack>idempotent()
        .play(AzTarget.track(CONTROLLER_MAIN), ANIMATION_SPIN_LOOP, AzPlayBehaviors.LOOP)
        .build();

    /**
     * ⚠⚠ IDEMPOTENT, NOT replay(). {@code GunItemAnimator.runGunAnimationEvents} is called from
     * {@code setCustomAnimations}, which runs EVERY RENDER FRAME, and its only guard is {@code previousAnimationId} -
     * an instance field. Any time that field does not hold (a fresh animator for first-person, third-person, the GUI
     * icon or a dropped stack) the CURRENT animation type is dispatched again.
     * <p>
     * With {@code replay()} that RESTARTS the clip on every such dispatch, which is what produced the "hyperspinning /
     * stacked" barrels - and because the type persists on the stack after firing stops, a newly created animator
     * replayed it unprompted, so the gun span on its own with no trigger held.
     * </p>
     * <p>
     * ⭐ {@code idempotent()} is {@code PLAY_IF_NOT_PLAYING}: re-dispatching something already playing is a no-op, so
     * frame-rate no longer drives the animation. SPIN_LOOP was always idempotent, which is exactly why it was the one
     * phase that never misbehaved.
     * </p>
     */
    private final AzCommand<ItemStack> SPIN_DOWN = AzCommand.<ItemStack>idempotent()
        .play(AzTarget.track(CONTROLLER_MAIN), ANIMATION_SPIN_DOWN, AzPlayBehaviors.HOLD_ON_LAST_FRAME)
        .build();

    /** ⚠ IDEMPOTENT for the same reason as SPIN_DOWN above - see that javadoc before changing either. */
    private final AzCommand<ItemStack> SPIN_UP = AzCommand.<ItemStack>idempotent()
        .play(AzTarget.track(CONTROLLER_MAIN), ANIMATION_SPIN_UP, AzPlayBehaviors.PLAY_ONCE)
        .build();

    @Override
    public void idle(Entity entity, ItemStack itemStack) {
        DEFAULT.idle(entity, itemStack);
    }

    @Override
    public void shoot(Entity entity, ItemStack itemStack) {
        DEFAULT.shoot(entity, itemStack);
    }

    public void spinLoop(Entity entity, ItemStack itemStack) {
        if (!entity.level().isClientSide()) {
            return;
        }

        SPIN_LOOP.dispatchForItem(entity, itemStack);
    }

    public void spinDown(Entity entity, ItemStack itemStack) {
        if (!entity.level().isClientSide()) {
            return;
        }

        SPIN_DOWN.dispatchForItem(entity, itemStack);
    }

    public void spinUp(Entity entity, ItemStack itemStack) {
        if (!entity.level().isClientSide()) {
            return;
        }

        SPIN_UP.dispatchForItem(entity, itemStack);
    }

    private OldPainlessAnimationDispatcher() {}
}
