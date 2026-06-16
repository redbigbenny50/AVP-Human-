package com.human.common.gameplay.item.gun.animation.dispatcher.impl;

import com.blib.api.client.animation.v1.command.AzCommand;
import com.blib.api.client.animation.v1.command.AzTarget;
import com.blib.api.client.animation.v1.command.play_behavior.AzPlayBehaviors;
import com.human.common.gameplay.item.gun.animation.dispatcher.GunAnimationDispatcher;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

public class DefaultGunAnimationDispatcher implements GunAnimationDispatcher {

    public static final DefaultGunAnimationDispatcher INSTANCE = new DefaultGunAnimationDispatcher();

    public static final String CONTROLLER_MAIN = "main";

    private static final String ANIMATION_IDLE = "animation.idle";

    private static final String ANIMATION_RELOAD = "animation.reload";

    private static final String ANIMATION_SHOOT = "animation.shoot";

    private static final AzCommand<ItemStack> IDLE = AzCommand.<ItemStack>replay()
        .play(AzTarget.track(CONTROLLER_MAIN), ANIMATION_IDLE, AzPlayBehaviors.PLAY_ONCE)
        .build();

    private static final AzCommand<ItemStack> RELOAD = AzCommand.<ItemStack>replay()
        .play(AzTarget.track(CONTROLLER_MAIN), ANIMATION_RELOAD, AzPlayBehaviors.PLAY_ONCE)
        .build();

    private static final AzCommand<ItemStack> SHOOT = AzCommand.<ItemStack>replay()
        .play(AzTarget.track(CONTROLLER_MAIN), ANIMATION_SHOOT, AzPlayBehaviors.PLAY_ONCE)
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

    private DefaultGunAnimationDispatcher() {}
}
