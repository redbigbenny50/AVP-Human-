package com.human.common.gameplay.item.gun.animation.dispatcher.impl;

import com.blib.api.client.animation.v1.command.AzCommand;
import com.blib.api.client.animation.v1.command.AzTarget;
import com.blib.api.client.animation.v1.command.play_behavior.AzPlayBehaviors;
import com.human.common.gameplay.item.gun.animation.dispatcher.GunAnimationDispatcher;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

public class M42A3SniperRifleAnimationDispatcher implements GunAnimationDispatcher {

    public static final M42A3SniperRifleAnimationDispatcher INSTANCE = new M42A3SniperRifleAnimationDispatcher();

    public static final String CONTROLLER_MAIN = "main";

    private static final String ANIMATION_IDLE = "animation.idle";

    private static final String ANIMATION_RECHAMBER = "animation.rechamber";

    private static final String ANIMATION_SHOOT = "animation.shoot";

    private static final AzCommand<ItemStack> IDLE = AzCommand.<ItemStack>idempotent()
        .play(AzTarget.track(CONTROLLER_MAIN), ANIMATION_IDLE, AzPlayBehaviors.LOOP)
        .build();

    private static final String ANIMATION_RELOAD = "animation.reload";

    /*
     * ⚠⚠ Oct 6 - THE GIANT ARMS AFTER A SHOT. [stated] tester: "its not actually from opening the inventory its after
     * shooting". BLib draws the player's skin arms wherever this rig's leftArm/rightArm bones are, whenever the track
     * holds an animation - and every sniper clip keys those bones. Shoot and rechamber are play-once: when rechamber
     * ended the track stopped with nothing driving the arm bones, so they slid back to the rig's rest pose, which sits
     * at the camera - each face of the arm then filled the view as one flat skin colour (the yellow/brown/purple
     * blocks). The idle was only restarted by a polling kick up to half a second later, and that gap is the bug. <p> ⭐
     * Now each one-shot is a SEQUENCE ending in the looping idle on the same track, so the moment rechamber (or reload)
     * ends the idle is already playing and the arm bones are never left unposed. The old SHOOT was also a compose of
     * two separate replays, the second of which restarted the track straight away - shoot then rechamber now actually
     * play in order. </p>
     */
    private static final AzCommand<ItemStack> SHOOT = AzCommand.<ItemStack>replay()
        .playSequence(
            AzTarget.track(CONTROLLER_MAIN),
            sequence -> sequence
                .queue(ANIMATION_SHOOT, properties -> properties.withPlayBehavior(AzPlayBehaviors.PLAY_ONCE))
                .queue(ANIMATION_RECHAMBER, properties -> properties.withPlayBehavior(AzPlayBehaviors.PLAY_ONCE))
                .queue(ANIMATION_IDLE, properties -> properties.withPlayBehavior(AzPlayBehaviors.LOOP))
        )
        .build();

    private static final AzCommand<ItemStack> RELOAD = AzCommand.<ItemStack>replay()
        .playSequence(
            AzTarget.track(CONTROLLER_MAIN),
            sequence -> sequence
                .queue(ANIMATION_RELOAD, properties -> properties.withPlayBehavior(AzPlayBehaviors.PLAY_ONCE))
                .queue(ANIMATION_IDLE, properties -> properties.withPlayBehavior(AzPlayBehaviors.LOOP))
        )
        .build();

    @Override
    public void idle(Entity entity, ItemStack itemStack) {
        if (!entity.level().isClientSide()) {
            return;
        }

        IDLE.dispatchForItem(entity, itemStack);
    }

    @Override
    public void reload(Entity entity, ItemStack itemStack) {
        if (!entity.level().isClientSide()) {
            return;
        }

        RELOAD.dispatchForItem(entity, itemStack);
    }

    @Override
    public void shoot(Entity entity, ItemStack itemStack) {
        if (!entity.level().isClientSide()) {
            return;
        }

        SHOOT.dispatchForItem(entity, itemStack);
    }

    private M42A3SniperRifleAnimationDispatcher() {}
}
