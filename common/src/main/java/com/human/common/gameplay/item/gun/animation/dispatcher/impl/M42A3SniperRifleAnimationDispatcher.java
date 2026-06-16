package com.human.common.gameplay.item.gun.animation.dispatcher.impl;

import com.blib.api.client.animation.v1.command.AzCommand;
import com.blib.api.client.animation.v1.command.AzTarget;
import com.blib.api.client.animation.v1.command.play_behavior.AzPlayBehaviors;
import com.human.common.gameplay.item.gun.animation.GunAnimationNetworking;
import com.human.common.gameplay.item.gun.animation.dispatcher.GunAnimationDispatcher;
import com.human.common.network.packet.S2CGunAnimationPayload;
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

    private static final AzCommand<ItemStack> RECHAMBER = AzCommand.<ItemStack>replay()
        .play(AzTarget.track(CONTROLLER_MAIN), ANIMATION_RECHAMBER, AzPlayBehaviors.PLAY_ONCE)
        .build();

    private static final AzCommand<ItemStack> SHOOT = AzCommand.compose(
        AzCommand.<ItemStack>replay()
            .play(AzTarget.track(CONTROLLER_MAIN), ANIMATION_SHOOT, AzPlayBehaviors.PLAY_ONCE)
            .build(),
        RECHAMBER
    );

    @Override
    public void idle(Entity entity, ItemStack itemStack) {
        if (!entity.level().isClientSide()) {
            return;
        }

        IDLE.dispatchForItem(entity, itemStack);
    }

    @Override
    public void reload(Entity entity, ItemStack itemStack) {
        DEFAULT.reload(entity, itemStack);
    }

    @Override
    public void shoot(Entity entity, ItemStack itemStack) {
        GunAnimationNetworking.dispatch(SHOOT, entity, itemStack, S2CGunAnimationPayload.Animation.SHOOT);
    }

    private M42A3SniperRifleAnimationDispatcher() {}
}
