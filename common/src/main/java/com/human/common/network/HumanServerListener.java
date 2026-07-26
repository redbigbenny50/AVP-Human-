package com.human.common.network;

import com.human.common.gameplay.item.GunItem;
import com.human.common.gameplay.item.gun.GunReloading;
import com.human.common.model.Crawler;
import com.human.common.network.packet.C2SGunFirePayload;
import com.human.common.network.packet.C2SGunReloadPayload;
import com.human.common.network.packet.C2SPlayerToggleCrawlPayload;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;

public class HumanServerListener {

    public static void handleGunFirePayload(C2SGunFirePayload gunFirePayload, Player serverPlayer) {
        if (
            Math.abs(net.minecraft.util.Mth.wrapDegrees(gunFirePayload.yaw() - serverPlayer.getYRot())) > 35.0F
                || Math.abs(gunFirePayload.pitch() - serverPlayer.getXRot()) > 35.0F
        ) {
            return;
        }

        var itemStack = serverPlayer.getItemInHand(InteractionHand.MAIN_HAND);

        if (itemStack.getItem() instanceof GunItem gunItem) {
            gunItem.fire(serverPlayer.level(), serverPlayer, itemStack, gunFirePayload.tickProgress());
        }
    }

    public static void handleGunReloadPayload(C2SGunReloadPayload gunReloadPayload, Player serverPlayer) {
        GunReloading.reload(serverPlayer);
    }

    public static void handlePlayerToggleCrawlPayload(C2SPlayerToggleCrawlPayload playerToggleCrawlPayload, Player serverPlayer) {
        var crawler = (Crawler) serverPlayer;
        crawler.setCrawling(playerToggleCrawlPayload.shouldCrawl());
    }

    private HumanServerListener() {
        throw new UnsupportedOperationException();
    }
}
