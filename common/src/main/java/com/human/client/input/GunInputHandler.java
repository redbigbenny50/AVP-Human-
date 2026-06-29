package com.human.client.input;

import com.human.Human;
import com.human.common.gameplay.item.GunItem;
import com.human.common.network.packet.C2SGunFirePayload;
import net.minecraft.client.Minecraft;

public class GunInputHandler {

    private static int firingTicks;

    private static boolean wasFiring;

    public static void tick(Minecraft minecraft) {
        if (!isTryingToFireGun(minecraft)) {
            reset();
            return;
        }

        var player = minecraft.player;
        var itemStack = player.getMainHandItem();
        var tickProgress = wasFiring ? firingTicks : 0;

        wasFiring = true;
        firingTicks++;

        if (itemStack.getItem() instanceof GunItem gunItem) {
            gunItem.fire(player.level(), player, itemStack, tickProgress);
            Human.MOD.networking().sendToServer(new C2SGunFirePayload(tickProgress));
        }
    }

    public static boolean isHoldingGun(Minecraft minecraft) {
        return minecraft.player != null && minecraft.player.getMainHandItem().getItem() instanceof GunItem;
    }

    private static boolean isTryingToFireGun(Minecraft minecraft) {
        return minecraft.screen == null
            && minecraft.player != null
            && minecraft.mouseHandler.isMouseGrabbed()
            && minecraft.options.keyAttack.isDown()
            && isHoldingGun(minecraft);
    }

    private static void reset() {
        firingTicks = 0;
        wasFiring = false;
    }

    private GunInputHandler() {
        throw new UnsupportedOperationException();
    }
}
