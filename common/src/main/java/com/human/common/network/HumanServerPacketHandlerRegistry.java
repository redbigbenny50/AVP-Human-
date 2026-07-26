package com.human.common.network;

import com.blib.api.common.network.v1.NetworkHandler;
import com.blib.api.common.registry.v1.impl.BLibNetworkRegistry;
import com.human.Human;
import com.human.client.network.HumanClientListener;
import com.human.common.network.packet.C2SGunFirePayload;
import com.human.common.network.packet.C2SGunReloadPayload;
import com.human.common.network.packet.C2SPlayerToggleCrawlPayload;
import com.human.common.network.packet.S2CBulletHitBlockPayload;
import com.human.common.network.packet.S2CGunKillEffectPayload;
import com.human.common.network.packet.S2CGunRecoilPayload;
import com.human.common.network.packet.S2CGunVoxelEffectPayload;
import com.human.common.network.packet.S2CNukeEffectPayload;

public class HumanServerPacketHandlerRegistry {

    private static final BLibNetworkRegistry REGISTRY = Human.MOD.registries().createNetworkRegistry();

    public static void initialize() {
        registerServerBoundPacketHandlers();
        registerClientBoundPacketHandlers();
    }

    private static void registerServerBoundPacketHandlers() {
        REGISTRY.registerPacketHandler(
            new NetworkHandler.FromClient<>(
                C2SGunFirePayload.TYPE,
                C2SGunFirePayload.CODEC,
                HumanServerListener::handleGunFirePayload
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
                S2CGunVoxelEffectPayload.TYPE,
                S2CGunVoxelEffectPayload.CODEC,
                (payload, player) -> HumanClientListener.handleGunVoxelEffect(payload)
            )
        );
        REGISTRY.registerPacketHandler(
            new NetworkHandler.FromServer<>(
                S2CGunKillEffectPayload.TYPE,
                S2CGunKillEffectPayload.CODEC,
                (payload, player) -> HumanClientListener.handleGunKillEffect(payload)
            )
        );
        REGISTRY.registerPacketHandler(
            new NetworkHandler.FromServer<>(
                S2CGunRecoilPayload.TYPE,
                S2CGunRecoilPayload.CODEC,
                HumanClientListener::handleGunRecoil
            )
        );
        REGISTRY.registerPacketHandler(
            new NetworkHandler.FromServer<>(
                S2CNukeEffectPayload.TYPE,
                S2CNukeEffectPayload.CODEC,
                (payload, player) -> HumanClientListener.handleNukeEffect(payload)
            )
        );
    }
}
