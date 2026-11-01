package com.human.common.registry.init;

import com.blib.api.common.network.v1.NetworkHandler;
import com.blib.api.common.network.v1.PacketDirection;
import com.blib.api.common.registry.v1.BLibHolder;
import com.blib.api.common.registry.v1.BLibRegistry;
import com.blib.api.common.registry.v1.impl.BLibNetworkRegistry;
import com.human.Human;
import com.human.common.gameplay.gene.reader.GeneReaderMenu;
import com.human.common.network.packet.C2SSyringeLabelPayload;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.inventory.MenuType;

/** The gene reader screen: its menu type and the syringe-label packet. Registered from Genes.initialize. */
public final class HumanGeneReader {

    private static final BLibRegistry<MenuType<?>> MENUS = Human.MOD.registries().create(BuiltInRegistries.MENU);

    private static final BLibNetworkRegistry DIRECTIONS = Human.MOD.registries().createNetworkRegistry();

    private static final BLibNetworkRegistry HANDLERS = Human.MOD.registries().createNetworkRegistry();

    public static final BLibHolder<MenuType<GeneReaderMenu>> MENU = MENUS.createHolder(
        "gene_reader",
        () -> new MenuType<>(GeneReaderMenu::new, FeatureFlagSet.of())
    );

    private HumanGeneReader() {}

    public static void initialize() {
        MENUS.registerAll();

        DIRECTIONS.registerPacketDirection(new PacketDirection.C2S<>(C2SSyringeLabelPayload.TYPE, C2SSyringeLabelPayload.CODEC));
        DIRECTIONS.registerPacketDirection(
            new PacketDirection.S2C<>(
                com.human.common.network.packet.S2CGeneDiscoveriesPayload.TYPE,
                com.human.common.network.packet.S2CGeneDiscoveriesPayload.CODEC
            )
        );
        HANDLERS.registerPacketHandler(
            new NetworkHandler.FromServer<>(
                com.human.common.network.packet.S2CGeneDiscoveriesPayload.TYPE,
                com.human.common.network.packet.S2CGeneDiscoveriesPayload.CODEC,
                (payload, player) -> com.human.common.gameplay.gene.discovery.ClientGeneDiscoveries.accept(
                    payload.genes(),
                    payload.species()
                )
            )
        );
        HANDLERS.registerPacketHandler(
            new NetworkHandler.FromClient<>(
                C2SSyringeLabelPayload.TYPE,
                C2SSyringeLabelPayload.CODEC,
                (payload, player) -> {
                    if (player.containerMenu instanceof GeneReaderMenu menu) {
                        menu.setLabel(payload.label());
                    }
                }
            )
        );
    }
}
