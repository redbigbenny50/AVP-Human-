package com.human.common.registry.init;

import com.blib.api.common.registry.v1.BLibHolder;
import com.blib.api.common.registry.v1.BLibRegistry;
import com.human.Human;
import com.human.common.gameplay.block_item.StackableModelBlockItem;
import com.human.common.gameplay.gene.machine.analyzer.GeneAnalyzerBlock;
import com.human.common.gameplay.gene.machine.analyzer.GeneAnalyzerBlockEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

/** The Gene Analyzer in its three editions ([stated] Oct 29), their items and the one block entity type. */
public final class HumanGeneAnalyzer {

    private static final BLibRegistry<Block> BLOCKS = Human.MOD.registries().create(BuiltInRegistries.BLOCK);

    private static final BLibRegistry<Item> ITEMS = Human.MOD.registries().create(BuiltInRegistries.ITEM);

    private static final BLibRegistry<BlockEntityType<?>> BLOCK_ENTITY_TYPES = Human.MOD.registries()
        .create(BuiltInRegistries.BLOCK_ENTITY_TYPE);

    public static final BLibHolder<Block> GENE_ANALYZER = block(GeneAnalyzerBlock.Edition.BARE);

    public static final BLibHolder<Block> WY_GENE_ANALYZER = block(GeneAnalyzerBlock.Edition.WY);

    public static final BLibHolder<Block> USCM_GENE_ANALYZER = block(GeneAnalyzerBlock.Edition.USCM);

    public static final BLibHolder<Item> GENE_ANALYZER_ITEM = item(GENE_ANALYZER, GeneAnalyzerBlock.Edition.BARE);

    public static final BLibHolder<Item> WY_GENE_ANALYZER_ITEM = item(WY_GENE_ANALYZER, GeneAnalyzerBlock.Edition.WY);

    public static final BLibHolder<Item> USCM_GENE_ANALYZER_ITEM = item(USCM_GENE_ANALYZER, GeneAnalyzerBlock.Edition.USCM);

    public static final BLibHolder<BlockEntityType<GeneAnalyzerBlockEntity>> BLOCK_ENTITY = BLOCK_ENTITY_TYPES.createHolder(
        "gene_analyzer",
        () -> BlockEntityType.Builder.of(
            GeneAnalyzerBlockEntity::new,
            GENE_ANALYZER.get(),
            WY_GENE_ANALYZER.get(),
            USCM_GENE_ANALYZER.get()
        ).build(null)
    );

    private static final BLibRegistry<net.minecraft.world.inventory.MenuType<?>> MENUS = Human.MOD.registries()
        .create(BuiltInRegistries.MENU);

    private static final com.blib.api.common.registry.v1.impl.BLibNetworkRegistry DIRECTIONS = Human.MOD.registries()
        .createNetworkRegistry();

    private static final com.blib.api.common.registry.v1.impl.BLibNetworkRegistry HANDLERS = Human.MOD.registries().createNetworkRegistry();

    public static final BLibHolder<net.minecraft.world.inventory.MenuType<com.human.common.gameplay.gene.machine.analyzer.GeneAnalyzerMenu>> MENU =
        MENUS.createHolder(
            "gene_analyzer",
            () -> new net.minecraft.world.inventory.MenuType<>(
                com.human.common.gameplay.gene.machine.analyzer.GeneAnalyzerMenu::new,
                net.minecraft.world.flag.FeatureFlagSet.of()
            )
        );

    private HumanGeneAnalyzer() {}

    private static BLibHolder<Block> block(GeneAnalyzerBlock.Edition edition) {
        return BLOCKS.createHolder(
            edition.id,
            () -> new GeneAnalyzerBlock(
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

    private static BLibHolder<Item> item(BLibHolder<Block> block, GeneAnalyzerBlock.Edition edition) {
        return ITEMS.createHolder(edition.id, () -> new StackableModelBlockItem(block.get(), new Item.Properties().stacksTo(1)));
    }

    public static void initialize() {
        BLOCKS.registerAll();
        ITEMS.registerAll();
        BLOCK_ENTITY_TYPES.registerAll();
        MENUS.registerAll();

        DIRECTIONS.registerPacketDirection(
            new com.blib.api.common.network.v1.PacketDirection.S2C<>(
                com.human.common.network.packet.S2CAnalyzerDatabasePayload.TYPE,
                com.human.common.network.packet.S2CAnalyzerDatabasePayload.CODEC
            )
        );
        DIRECTIONS.registerPacketDirection(
            new com.blib.api.common.network.v1.PacketDirection.C2S<>(
                com.human.common.network.packet.C2SAnalyzerPrintPayload.TYPE,
                com.human.common.network.packet.C2SAnalyzerPrintPayload.CODEC
            )
        );
        HANDLERS.registerPacketHandler(
            new com.blib.api.common.network.v1.NetworkHandler.FromServer<>(
                com.human.common.network.packet.S2CAnalyzerDatabasePayload.TYPE,
                com.human.common.network.packet.S2CAnalyzerDatabasePayload.CODEC,
                (payload, player) -> com.human.common.gameplay.gene.machine.analyzer.ClientAnalyzerDatabase.accept(
                    payload.part(),
                    payload.total(),
                    payload.text()
                )
            )
        );
        HANDLERS.registerPacketHandler(
            new com.blib.api.common.network.v1.NetworkHandler.FromClient<>(
                com.human.common.network.packet.C2SAnalyzerPrintPayload.TYPE,
                com.human.common.network.packet.C2SAnalyzerPrintPayload.CODEC,
                (payload, player) -> {
                    if (
                        player.containerMenu instanceof com.human.common.gameplay.gene.machine.analyzer.GeneAnalyzerMenu menu
                            && player instanceof net.minecraft.server.level.ServerPlayer serverPlayer
                    ) {
                        menu.print(serverPlayer, payload.genes(), payload.species());
                    }
                }
            )
        );
    }
}
