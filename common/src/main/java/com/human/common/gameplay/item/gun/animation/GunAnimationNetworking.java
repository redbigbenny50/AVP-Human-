package com.human.common.gameplay.item.gun.animation;

import com.blib.api.client.animation.v1.command.AzCommand;
import com.human.Human;
import com.human.common.network.packet.S2CGunAnimationPayload;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public final class GunAnimationNetworking {

    public static void dispatch(
        AzCommand<ItemStack> command,
        Entity entity,
        ItemStack itemStack,
        S2CGunAnimationPayload.Animation animation
    ) {
        if (entity.level().isClientSide()) {
            command.dispatchForItem(entity, itemStack);
            return;
        }

        Human.MOD.networking()
            .sendToAllClientsTrackingEntity(entity, new S2CGunAnimationPayload(entity.getId(), resolveHand(entity, itemStack), animation));
    }

    private static InteractionHand resolveHand(Entity entity, ItemStack itemStack) {
        if (entity instanceof LivingEntity livingEntity && livingEntity.getOffhandItem() == itemStack) {
            return InteractionHand.OFF_HAND;
        }

        return InteractionHand.MAIN_HAND;
    }

    private GunAnimationNetworking() {
        throw new UnsupportedOperationException();
    }
}
