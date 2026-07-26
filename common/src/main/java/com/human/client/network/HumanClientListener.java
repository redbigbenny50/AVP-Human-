package com.human.client.network;

import com.human.common.network.packet.S2CBulletHitBlockPayload;
import com.human.common.network.packet.S2CGunRecoilPayload;
import net.minecraft.client.Minecraft;
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

    private HumanClientListener() {
        throw new UnsupportedOperationException();
    }
}
