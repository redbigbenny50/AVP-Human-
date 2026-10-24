package com.human.common.gameplay.item.gun.animation.dispatcher.impl;

import com.blib.api.client.animation.v1.command.AzCommand;
import com.blib.api.client.animation.v1.command.AzTarget;
import com.blib.api.client.animation.v1.command.play_behavior.AzPlayBehaviors;
import com.human.common.gameplay.item.gun.animation.dispatcher.GunAnimationDispatcher;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

/**
 * The chem thrower's clips - his {@code chem_thrower.animation.json}: {@code idle.loop}, {@code fire},
 * {@code reload.remove}, {@code reload.purge}, {@code reload.insert} ({@code reload.oldunused} is left unused, as its
 * name says).
 * <h2>[stated] Oct 18 - THE RELOAD IS TWO HALVES</h2> The selection wheel sits between them, so they are two events:
 * <ul>
 * <li>UNLOAD - remove, then purge (the old vapor jets out of the right side - the jet itself is sent by the server),
 * then the idle while the gun waits, empty, for its next tank. ⚠ {@code reload.purge} is authored as a LOOP, which
 * would never let a sequence move on - so it is queued as three play-once passes (1.25 s).</li>
 * <li>INSERT - insert, then the idle.</li>
 * </ul>
 * <h2>⚠ THE ARMS NEED A CLIP ON THE TRACK AT ALL TIMES</h2> This rig carries {@code leftArm}/{@code rightArm}, and BLib
 * only draws the player's skin arms while the track holds an animation - the sniper's "giant arms" bug (Oct 6) was a
 * play-once clip ending with nothing after it. So every one-shot here is a SEQUENCE that ends in the looping idle. ⚠
 * remove and insert are authored HOLD_ON_LAST_FRAME; inside a sequence they are played once so it can continue.
 * <h2>FIRE</h2> {@code fire} loops while the trigger is held (a stream weapon, one shot a tick); idempotent, so it runs
 * on instead of restarting every shot. The animator puts the idle back when the shots stop.
 */
public class ChemThrowerAnimationDispatcher implements GunAnimationDispatcher {

    public static final ChemThrowerAnimationDispatcher INSTANCE = new ChemThrowerAnimationDispatcher();

    public static final String CONTROLLER_MAIN = "main";

    public static final String ANIMATION_IDLE = "idle.loop";

    public static final String ANIMATION_FIRE = "fire";

    public static final String ANIMATION_REMOVE = "reload.remove";

    private static final String ANIMATION_PURGE = "reload.purge";

    public static final String ANIMATION_INSERT = "reload.insert";

    private static final AzCommand<ItemStack> IDLE = AzCommand.<ItemStack>idempotent()
        .play(AzTarget.track(CONTROLLER_MAIN), ANIMATION_IDLE, AzPlayBehaviors.LOOP)
        .build();

    private static final AzCommand<ItemStack> SHOOT = AzCommand.<ItemStack>idempotent()
        .play(AzTarget.track(CONTROLLER_MAIN), ANIMATION_FIRE, AzPlayBehaviors.LOOP)
        .build();

    private static final AzCommand<ItemStack> UNLOAD = AzCommand.<ItemStack>replay()
        .playSequence(
            AzTarget.track(CONTROLLER_MAIN),
            sequence -> sequence
                .queue(ANIMATION_REMOVE, properties -> properties.withPlayBehavior(AzPlayBehaviors.PLAY_ONCE))
                .queue(ANIMATION_PURGE, properties -> properties.withPlayBehavior(AzPlayBehaviors.PLAY_ONCE))
                .queue(ANIMATION_PURGE, properties -> properties.withPlayBehavior(AzPlayBehaviors.PLAY_ONCE))
                .queue(ANIMATION_PURGE, properties -> properties.withPlayBehavior(AzPlayBehaviors.PLAY_ONCE))
                .queue(ANIMATION_IDLE, properties -> properties.withPlayBehavior(AzPlayBehaviors.LOOP))
        )
        .build();

    private static final AzCommand<ItemStack> INSERT = AzCommand.<ItemStack>replay()
        .playSequence(
            AzTarget.track(CONTROLLER_MAIN),
            sequence -> sequence
                .queue(ANIMATION_INSERT, properties -> properties.withPlayBehavior(AzPlayBehaviors.PLAY_ONCE))
                .queue(ANIMATION_IDLE, properties -> properties.withPlayBehavior(AzPlayBehaviors.LOOP))
        )
        .build();

    private ChemThrowerAnimationDispatcher() {}

    @Override
    public void idle(Entity entity, ItemStack itemStack) {
        if (entity.level().isClientSide()) {
            IDLE.dispatchForItem(entity, itemStack);
        }
    }

    @Override
    public void shoot(Entity entity, ItemStack itemStack) {
        if (entity.level().isClientSide()) {
            SHOOT.dispatchForItem(entity, itemStack);
        }
    }

    public void unload(Entity entity, ItemStack itemStack) {
        if (entity.level().isClientSide()) {
            UNLOAD.dispatchForItem(entity, itemStack);
        }
    }

    public void insert(Entity entity, ItemStack itemStack) {
        if (entity.level().isClientSide()) {
            INSERT.dispatchForItem(entity, itemStack);
        }
    }
}
