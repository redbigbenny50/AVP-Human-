package com.human.common.network;

import com.blib.api.common.network.v1.PacketDirection;
import com.blib.api.common.registry.v1.impl.BLibNetworkRegistry;
import com.human.Human;
import com.human.common.network.packet.C2SDaggerOffHandAttackPayload;
import com.human.common.network.packet.C2SGunFirePayload;
import com.human.common.network.packet.C2SGunReloadPayload;
import com.human.common.network.packet.C2SMarineSentryFilterPayload;
import com.human.common.network.packet.C2SPlayerToggleCrawlPayload;
import com.human.common.network.packet.S2CBulletHitBlockPayload;
import com.human.common.network.packet.S2CGunKillEffectPayload;
import com.human.common.network.packet.S2CGunRecoilPayload;
import com.human.common.network.packet.S2CGunVoxelEffectPayload;
import com.human.common.network.packet.S2CMarineSentryFilterPayload;
import com.human.common.network.packet.S2CNukeEffectPayload;

public class HumanPacketDirectionRegistry {

    private static final BLibNetworkRegistry REGISTRY = Human.MOD.registries().createNetworkRegistry();

    public static void initialize() {
        REGISTRY.registerPacketDirection(new PacketDirection.C2S<>(C2SGunFirePayload.TYPE, C2SGunFirePayload.CODEC));
        REGISTRY.registerPacketDirection(
            new PacketDirection.C2S<>(C2SDaggerOffHandAttackPayload.TYPE, C2SDaggerOffHandAttackPayload.CODEC)
        );
        REGISTRY.registerPacketDirection(new PacketDirection.C2S<>(C2SGunReloadPayload.TYPE, C2SGunReloadPayload.CODEC));
        REGISTRY.registerPacketDirection(
            new PacketDirection.C2S<>(C2SPlayerToggleCrawlPayload.TYPE, C2SPlayerToggleCrawlPayload.CODEC)
        );
        REGISTRY.registerPacketDirection(
            new PacketDirection.C2S<>(C2SMarineSentryFilterPayload.TYPE, C2SMarineSentryFilterPayload.CODEC)
        );

        REGISTRY.registerPacketDirection(
            new PacketDirection.S2C<>(S2CBulletHitBlockPayload.TYPE, S2CBulletHitBlockPayload.CODEC)
        );
        REGISTRY.registerPacketDirection(new PacketDirection.S2C<>(S2CGunRecoilPayload.TYPE, S2CGunRecoilPayload.CODEC));
        REGISTRY.registerPacketDirection(new PacketDirection.S2C<>(S2CGunVoxelEffectPayload.TYPE, S2CGunVoxelEffectPayload.CODEC));
        REGISTRY.registerPacketDirection(new PacketDirection.S2C<>(S2CGunKillEffectPayload.TYPE, S2CGunKillEffectPayload.CODEC));
        REGISTRY.registerPacketDirection(new PacketDirection.S2C<>(S2CNukeEffectPayload.TYPE, S2CNukeEffectPayload.CODEC));
        REGISTRY.registerPacketDirection(
            new PacketDirection.S2C<>(S2CMarineSentryFilterPayload.TYPE, S2CMarineSentryFilterPayload.CODEC)
        );
    }
}
