package com.human.client.network;

import com.human.common.gameplay.item.GunItem;
import com.human.common.gameplay.item.gun.animation.dispatcher.impl.OldPainlessAnimationDispatcher;
import com.human.common.network.packet.S2CBulletHitBlockPayload;
import com.human.common.network.packet.S2CGunAnimationPayload;
import com.human.common.network.packet.S2CGunRecoilPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public class HumanClientListener {

    public static void handleBulletHitBlockPayload(S2CBulletHitBlockPayload bulletHitBlockPayload) {
        var blockPos = bulletHitBlockPayload.blockPos();
        var direction = bulletHitBlockPayload.direction();

        for (int i = 0; i < 16; i++) {
            Minecraft.getInstance().particleEngine.crack(blockPos, direction);
        }
    }

    public static void handleGunRecoil(S2CGunRecoilPayload gunRecoilPayload, Player player) {
        var level = player.level();
        var baseRecoilX = level.getRandom().nextBoolean() ? 1f : -1f;

        player.turn(baseRecoilX * 2, -gunRecoilPayload.recoil() * 2);
    }

    public static void handleGunAnimation(S2CGunAnimationPayload gunAnimationPayload) {
        var level = Minecraft.getInstance().level;

        if (level == null) {
            return;
        }

        if (!(level.getEntity(gunAnimationPayload.entityId()) instanceof LivingEntity livingEntity)) {
            return;
        }

        var itemStack = livingEntity.getItemInHand(gunAnimationPayload.hand());

        if (!(itemStack.getItem() instanceof GunItem gunItem)) {
            return;
        }

        var dispatcher = gunItem.getGunConfig().animationDispatcher();

        switch (gunAnimationPayload.animation()) {
            case SHOOT -> dispatcher.shoot(livingEntity, itemStack);
            case RELOAD -> dispatcher.reload(livingEntity, itemStack);
            case OLD_PAINLESS_SPIN_LOOP -> OldPainlessAnimationDispatcher.INSTANCE.spinLoop(livingEntity, itemStack);
            case OLD_PAINLESS_SPIN_DOWN -> OldPainlessAnimationDispatcher.INSTANCE.spinDown(livingEntity, itemStack);
            case OLD_PAINLESS_SPIN_UP -> OldPainlessAnimationDispatcher.INSTANCE.spinUp(livingEntity, itemStack);
        }
    }

    private HumanClientListener() {
        throw new UnsupportedOperationException();
    }
}
