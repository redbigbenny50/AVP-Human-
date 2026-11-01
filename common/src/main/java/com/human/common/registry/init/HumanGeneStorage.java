package com.human.common.registry.init;

import com.blib.api.common.registry.v1.BLibHolder;
import com.blib.api.common.registry.v1.BLibRegistry;
import com.human.Human;
import com.human.common.gameplay.block_item.StackableModelBlockItem;
import com.human.common.gameplay.gene.machine.storage.GeneStorageBlock;
import com.human.common.gameplay.gene.machine.storage.GeneStorageBlockEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

/** The Gene Storage in its three editions ([stated] Oct 29), their items and the one block entity type. */
public final class HumanGeneStorage {

    private static final BLibRegistry<Block> BLOCKS = Human.MOD.registries().create(BuiltInRegistries.BLOCK);

    private static final BLibRegistry<Item> ITEMS = Human.MOD.registries().create(BuiltInRegistries.ITEM);

    private static final BLibRegistry<BlockEntityType<?>> BLOCK_ENTITY_TYPES = Human.MOD.registries()
        .create(BuiltInRegistries.BLOCK_ENTITY_TYPE);

    public static final BLibHolder<Block> GENE_STORAGE = block(GeneStorageBlock.Edition.BARE);

    public static final BLibHolder<Block> WY_GENE_STORAGE = block(GeneStorageBlock.Edition.WY);

    public static final BLibHolder<Block> USCM_GENE_STORAGE = block(GeneStorageBlock.Edition.USCM);

    public static final BLibHolder<Item> GENE_STORAGE_ITEM = item(GENE_STORAGE, GeneStorageBlock.Edition.BARE);

    public static final BLibHolder<Item> WY_GENE_STORAGE_ITEM = item(WY_GENE_STORAGE, GeneStorageBlock.Edition.WY);

    public static final BLibHolder<Item> USCM_GENE_STORAGE_ITEM = item(USCM_GENE_STORAGE, GeneStorageBlock.Edition.USCM);

    public static final BLibHolder<BlockEntityType<GeneStorageBlockEntity>> BLOCK_ENTITY = BLOCK_ENTITY_TYPES.createHolder(
        "gene_storage",
        () -> BlockEntityType.Builder.of(GeneStorageBlockEntity::new, GENE_STORAGE.get(), WY_GENE_STORAGE.get(), USCM_GENE_STORAGE.get())
            .build(null)
    );

    private static final BLibRegistry<net.minecraft.world.inventory.MenuType<?>> MENUS = Human.MOD.registries()
        .create(BuiltInRegistries.MENU);

    public static final BLibHolder<net.minecraft.world.inventory.MenuType<com.human.common.gameplay.gene.machine.storage.GeneStorageMenu>> MENU =
        MENUS.createHolder(
            "gene_storage",
            () -> new net.minecraft.world.inventory.MenuType<>(
                com.human.common.gameplay.gene.machine.storage.GeneStorageMenu::new,
                net.minecraft.world.flag.FeatureFlagSet.of()
            )
        );

    private HumanGeneStorage() {}

    private static BLibHolder<Block> block(GeneStorageBlock.Edition edition) {
        return BLOCKS.createHolder(
            edition.id,
            () -> new GeneStorageBlock(
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

    private static BLibHolder<Item> item(BLibHolder<Block> block, GeneStorageBlock.Edition edition) {
        return ITEMS.createHolder(edition.id, () -> new StackableModelBlockItem(block.get(), new Item.Properties().stacksTo(1)));
    }

    public static void initialize() {
        BLOCKS.registerAll();
        ITEMS.registerAll();
        BLOCK_ENTITY_TYPES.registerAll();
        MENUS.registerAll();
    }
}
