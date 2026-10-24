package com.human.client.animation.item;

import com.blib.api.client.animation.v1.animator.AzAnimatorConfig;
import com.blib.api.client.animation.v1.animator.AzItemAnimator;
import com.human.common.gameplay.item.gun.animation.GunAnimationEvents;
import com.human.common.gameplay.item.gun.animation.dispatcher.GunAnimationDispatcher;
import com.human.common.gameplay.item.gun.animation.dispatcher.impl.OldPainlessAnimationDispatcher;
import com.human.common.registry.init.HumanDataComponents;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;

public abstract class GunItemAnimator extends AzItemAnimator {

    private int previousAnimationId = Integer.MIN_VALUE;

    /** The game time idle was last restarted (see restartIdleIfStopped). */
    // Oct 14 FIX - was Long.MIN_VALUE: "now - Long.MIN_VALUE" OVERFLOWS to a huge negative number, so the time check
    // never passed. Half of it leaves room to subtract any real game time.
    private long lastIdleKick = Long.MIN_VALUE / 2;

    /** At most twice a second while the track sits stopped. */
    private static final long IDLE_KICK_INTERVAL_TICKS = 10L;

    protected GunItemAnimator() {
        super(AzAnimatorConfig.defaultConfig());
    }

    /**
     * [stated] Oct 5: after zooming out of the sniper or a reload, its arms vanished. Nothing ever started a gun's idle
     * - only shoot, reload and spin events from the server are dispatched - so once a play-once clip ended the track
     * sat STOPPED, and BLib draws the player's skin arms only while a track is playing. Restart the idle whenever the
     * track has stopped. It never interrupts a shot or a reload (those are playing, not stopped).
     */
    private void restartIdleIfStopped(ItemStack itemStack, GunAnimationDispatcher dispatcher) {
        var minecraft = Minecraft.getInstance();

        if (minecraft.player == null || minecraft.level == null) {
            return;
        }

        var stopped = true;

        for (var track : getAnimationTrackContainer().getAll()) {
            if (track.currentAnimation() != null && !track.stateMachine().isStopped()) {
                stopped = false;
                break;
            }
        }

        var now = minecraft.level.getGameTime();

        // Oct 14 audit: this animator outlives a world, but game time is per world - a world whose clock is BEHIND the
        // last kick (a newer world, opened after an older one) would wait until it caught up, possibly hours.
        if (stopped && (now - lastIdleKick >= IDLE_KICK_INTERVAL_TICKS || now < lastIdleKick)) {
            lastIdleKick = now;
            dispatcher.idle(minecraft.player, itemStack);
        }
    }

    @Override
    public void setCustomAnimations(ItemStack animatable, float partialTicks) {
        super.setCustomAnimations(animatable, partialTicks);
    }

    protected void runGunAnimationEvents(ItemStack itemStack, GunAnimationDispatcher dispatcher) {
        restartIdleIfStopped(itemStack, dispatcher);

        var animationId = itemStack.getOrDefault(HumanDataComponents.GUN_ANIMATION_ID.get(), 0);

        if (animationId == previousAnimationId) {
            return;
        }

        previousAnimationId = animationId;

        if (animationId == 0) {
            return;
        }

        var player = Minecraft.getInstance().player;

        if (player == null) {
            return;
        }

        switch (itemStack.getOrDefault(HumanDataComponents.GUN_ANIMATION_TYPE.get(), GunAnimationEvents.NONE)) {
            case GunAnimationEvents.SHOOT -> dispatcher.shoot(player, itemStack);
            case GunAnimationEvents.RELOAD -> dispatcher.reload(player, itemStack);
            case GunAnimationEvents.OLD_PAINLESS_SPIN_LOOP -> OldPainlessAnimationDispatcher.INSTANCE.spinLoop(player, itemStack);
            case GunAnimationEvents.OLD_PAINLESS_SPIN_DOWN -> OldPainlessAnimationDispatcher.INSTANCE.spinDown(player, itemStack);
            case GunAnimationEvents.OLD_PAINLESS_SPIN_UP -> OldPainlessAnimationDispatcher.INSTANCE.spinUp(player, itemStack);
            default -> {
                // No animation event to run.
            }
        }
    }
}
