package com.human.common.network;

import com.blib.api.common.network.v1.NetworkHandler;
import com.blib.api.common.registry.v1.impl.BLibNetworkRegistry;
import com.human.Human;
import com.human.client.network.HumanClientListener;
import com.human.common.network.packet.C2SGunHitResultsPayload;
import com.human.common.network.packet.C2SGunReloadPayload;
import com.human.common.network.packet.C2SPlayerToggleCrawlPayload;
import com.human.common.network.packet.S2CBulletHitBlockPayload;
import com.human.common.network.packet.S2CGunRecoilPayload;

public class HumanServerPacketHandlerRegistry {

    private static final BLibNetworkRegistry REGISTRY = Human.MOD.registries().createNetworkRegistry();

    public static void initialize() {
        registerServerBoundPacketHandlers();
        registerClientBoundPacketHandlers();
    }

    private static void registerServerBoundPacketHandlers() {
        REGISTRY.registerPacketHandler(
            new NetworkHandler.FromClient<>(
                C2SGunHitResultsPayload.TYPE,
                C2SGunHitResultsPayload.CODEC,
                HumanServerListener::handleGunHitResultsPayload
            )
        );
        REGISTRY.registerPacketHandler(
            new NetworkHandler.FromClient<>(
                C2SGunReloadPayload.TYPE,
                C2SGunReloadPayload.CODEC,
                HumanServerListener::handleGunReloadPayload
            )
        );
        REGISTRY.registerPacketHandler(
            new NetworkHandler.FromClient<>(
                C2SPlayerToggleCrawlPayload.TYPE,
                C2SPlayerToggleCrawlPayload.CODEC,
                HumanServerListener::handlePlayerToggleCrawlPayload
            )
        );
    }

    private static void registerClientBoundPacketHandlers() {
        REGISTRY.registerPacketHandler(
            new NetworkHandler.FromServer<>(
                S2CBulletHitBlockPayload.TYPE,
                S2CBulletHitBlockPayload.CODEC,
                (payload, player) -> HumanClientListener.handleBulletHitBlockPayload(payload)
            )
        );
        REGISTRY.registerPacketHandler(
            new NetworkHandler.FromServer<>(
                S2CGunRecoilPayload.TYPE,
                S2CGunRecoilPayload.CODEC,
                HumanClientListener::handleGunRecoil
            )
        );
    }
}
