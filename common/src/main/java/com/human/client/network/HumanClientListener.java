package com.human.client.network;

import com.human.client.effect.NukeClientEffects;
import com.human.client.effect.VoxelGunEffects;
import com.human.common.network.packet.S2CBulletHitBlockPayload;
import com.human.common.network.packet.S2CGunKillEffectPayload;
import com.human.common.network.packet.S2CGunRecoilPayload;
import com.human.common.network.packet.S2CGunVoxelEffectPayload;
import com.human.common.network.packet.S2CNukeEffectPayload;
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
        player.turn(gunRecoilPayload.horizontalKick() * 2.0F, -gunRecoilPayload.verticalKick() * 2.0F);
    }

    public static void handleGunVoxelEffect(S2CGunVoxelEffectPayload payload) {
        VoxelGunEffects.trigger(payload);
    }

    public static void handleGunKillEffect(S2CGunKillEffectPayload payload) {
        VoxelGunEffects.triggerKillEffect(payload);
    }

    public static void handleNukeEffect(S2CNukeEffectPayload payload) {
        NukeClientEffects.trigger(payload);
    }

    private HumanClientListener() {
        throw new UnsupportedOperationException();
    }
}
