package com.human.common.registry.init;

import com.blib.api.common.registry.v1.BLibHolder;
import com.blib.api.common.registry.v1.BLibRegistry;
import com.human.Human;
import com.human.common.gameplay.block_item.StackableModelBlockItem;
import com.human.common.gameplay.gene.machine.splicer.GeneSplicerBlock;
import com.human.common.gameplay.gene.machine.splicer.GeneSplicerBlockEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

/** The Gene Splicer in its three editions ([stated] Oct 29), their items and the one block entity type. */
public final class HumanGeneSplicer {

    private static final BLibRegistry<Block> BLOCKS = Human.MOD.registries().create(BuiltInRegistries.BLOCK);

    private static final BLibRegistry<Item> ITEMS = Human.MOD.registries().create(BuiltInRegistries.ITEM);

    private static final BLibRegistry<BlockEntityType<?>> BLOCK_ENTITY_TYPES = Human.MOD.registries()
        .create(BuiltInRegistries.BLOCK_ENTITY_TYPE);

    public static final BLibHolder<Block> GENE_SPLICER = block(GeneSplicerBlock.Edition.BARE);

    public static final BLibHolder<Block> WY_GENE_SPLICER = block(GeneSplicerBlock.Edition.WY);

    public static final BLibHolder<Block> USCM_GENE_SPLICER = block(GeneSplicerBlock.Edition.USCM);

    public static final BLibHolder<Item> GENE_SPLICER_ITEM = item(GENE_SPLICER, GeneSplicerBlock.Edition.BARE);

    public static final BLibHolder<Item> WY_GENE_SPLICER_ITEM = item(WY_GENE_SPLICER, GeneSplicerBlock.Edition.WY);

    public static final BLibHolder<Item> USCM_GENE_SPLICER_ITEM = item(USCM_GENE_SPLICER, GeneSplicerBlock.Edition.USCM);

    public static final BLibHolder<BlockEntityType<GeneSplicerBlockEntity>> BLOCK_ENTITY = BLOCK_ENTITY_TYPES.createHolder(
        "gene_splicer",
        () -> BlockEntityType.Builder.of(GeneSplicerBlockEntity::new, GENE_SPLICER.get(), WY_GENE_SPLICER.get(), USCM_GENE_SPLICER.get())
            .build(null)
    );

    private static final BLibRegistry<net.minecraft.world.inventory.MenuType<?>> MENUS = Human.MOD.registries()
        .create(BuiltInRegistries.MENU);

    public static final BLibHolder<net.minecraft.world.inventory.MenuType<com.human.common.gameplay.gene.machine.splicer.GeneSplicerMenu>> MENU =
        MENUS.createHolder(
            "gene_splicer",
            () -> new net.minecraft.world.inventory.MenuType<>(
                com.human.common.gameplay.gene.machine.splicer.GeneSplicerMenu::new,
                net.minecraft.world.flag.FeatureFlagSet.of()
            )
        );

    private static final com.blib.api.common.registry.v1.impl.BLibNetworkRegistry DIRECTIONS = Human.MOD.registries()
        .createNetworkRegistry();

    private static final com.blib.api.common.registry.v1.impl.BLibNetworkRegistry HANDLERS = Human.MOD.registries().createNetworkRegistry();

    private HumanGeneSplicer() {}

    private static BLibHolder<Block> block(GeneSplicerBlock.Edition edition) {
        return BLOCKS.createHolder(
            edition.id,
            () -> new GeneSplicerBlock(
                edition,
                BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(3.5F, 6.0F)
                    .sound(SoundType.METAL)
                    .noOcclusion()
                    .pushReaction(net.minecraft.world.level.material.PushReaction.BLOCK)
            )
        );
    }

    private static BLibHolder<Item> item(BLibHolder<Block> block, GeneSplicerBlock.Edition edition) {
        return ITEMS.createHolder(edition.id, () -> new StackableModelBlockItem(block.get(), new Item.Properties().stacksTo(1)));
    }

    public static void initialize() {
        BLOCKS.registerAll();
        ITEMS.registerAll();
        BLOCK_ENTITY_TYPES.registerAll();
        MENUS.registerAll();

        // The NETWORK STORAGE packets are registered once, by HumanGeneRecombinator, and serve both machines.
        DIRECTIONS.registerPacketDirection(
            new com.blib.api.common.network.v1.PacketDirection.S2C<>(
                com.human.common.network.packet.S2CSplicerLabelPayload.TYPE,
                com.human.common.network.packet.S2CSplicerLabelPayload.CODEC
            )
        );
        DIRECTIONS.registerPacketDirection(
            new com.blib.api.common.network.v1.PacketDirection.C2S<>(
                com.human.common.network.packet.C2SSplicerLabelPayload.TYPE,
                com.human.common.network.packet.C2SSplicerLabelPayload.CODEC
            )
        );
        HANDLERS.registerPacketHandler(
            new com.blib.api.common.network.v1.NetworkHandler.FromServer<>(
                com.human.common.network.packet.S2CSplicerLabelPayload.TYPE,
                com.human.common.network.packet.S2CSplicerLabelPayload.CODEC,
                (payload, player) -> com.human.common.gameplay.gene.machine.splicer.ClientSplicerLabel.accept(payload.label())
            )
        );
        HANDLERS.registerPacketHandler(
            new com.blib.api.common.network.v1.NetworkHandler.FromClient<>(
                com.human.common.network.packet.C2SSplicerLabelPayload.TYPE,
                com.human.common.network.packet.C2SSplicerLabelPayload.CODEC,
                (payload, player) -> {
                    if (player.containerMenu instanceof com.human.common.gameplay.gene.machine.splicer.GeneSplicerMenu menu) {
                        menu.setLabel(payload.label());
                    }
                }
            )
        );
    }
}
