package com.human.common.network;

import com.blib.api.common.network.v1.PacketDirection;
import com.blib.api.common.registry.v1.impl.BLibNetworkRegistry;
import com.human.Human;
import com.human.common.network.packet.C2SGunHitResultsPayload;
import com.human.common.network.packet.C2SGunReloadPayload;
import com.human.common.network.packet.C2SPlayerToggleCrawlPayload;
import com.human.common.network.packet.S2CBulletHitBlockPayload;
import com.human.common.network.packet.S2CGunRecoilPayload;

public class HumanPacketDirectionRegistry {

    private static final BLibNetworkRegistry REGISTRY = Human.MOD.registries().createNetworkRegistry();

    public static void initialize() {
        REGISTRY.registerPacketDirection(new PacketDirection.C2S<>(C2SGunHitResultsPayload.TYPE, C2SGunHitResultsPayload.CODEC));
        REGISTRY.registerPacketDirection(new PacketDirection.C2S<>(C2SGunReloadPayload.TYPE, C2SGunReloadPayload.CODEC));
        REGISTRY.registerPacketDirection(
            new PacketDirection.C2S<>(C2SPlayerToggleCrawlPayload.TYPE, C2SPlayerToggleCrawlPayload.CODEC)
        );

        REGISTRY.registerPacketDirection(
            new PacketDirection.S2C<>(S2CBulletHitBlockPayload.TYPE, S2CBulletHitBlockPayload.CODEC)
        );
        REGISTRY.registerPacketDirection(new PacketDirection.S2C<>(S2CGunRecoilPayload.TYPE, S2CGunRecoilPayload.CODEC));
    }
}
