package com.human.common.gameplay.item.gun.animation.dispatcher.impl;

import com.blib.api.client.animation.v1.command.AzCommand;
import com.blib.api.client.animation.v1.command.AzTarget;
import com.blib.api.client.animation.v1.command.play_behavior.AzPlayBehaviors;
import com.human.common.gameplay.item.gun.animation.GunAnimationNetworking;
import com.human.common.gameplay.item.gun.animation.dispatcher.GunAnimationDispatcher;
import com.human.common.network.packet.S2CGunAnimationPayload;
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

    private final AzCommand<ItemStack> SPIN_DOWN = AzCommand.<ItemStack>replay()
        .play(AzTarget.track(CONTROLLER_MAIN), ANIMATION_SPIN_DOWN, AzPlayBehaviors.HOLD_ON_LAST_FRAME)
        .build();

    private final AzCommand<ItemStack> SPIN_UP = AzCommand.<ItemStack>replay()
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
        GunAnimationNetworking.dispatch(SPIN_LOOP, entity, itemStack, S2CGunAnimationPayload.Animation.OLD_PAINLESS_SPIN_LOOP);
    }

    public void spinDown(Entity entity, ItemStack itemStack) {
        GunAnimationNetworking.dispatch(SPIN_DOWN, entity, itemStack, S2CGunAnimationPayload.Animation.OLD_PAINLESS_SPIN_DOWN);
    }

    public void spinUp(Entity entity, ItemStack itemStack) {
        GunAnimationNetworking.dispatch(SPIN_UP, entity, itemStack, S2CGunAnimationPayload.Animation.OLD_PAINLESS_SPIN_UP);
    }

    private OldPainlessAnimationDispatcher() {}
}
