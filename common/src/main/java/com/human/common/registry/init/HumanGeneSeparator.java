package com.human.common.registry.init;

import com.blib.api.common.registry.v1.BLibHolder;
import com.blib.api.common.registry.v1.BLibRegistry;
import com.human.Human;
import com.human.common.gameplay.block_item.StackableModelBlockItem;
import com.human.common.gameplay.gene.machine.separator.GeneSeparatorBlock;
import com.human.common.gameplay.gene.machine.separator.GeneSeparatorBlockEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

/** The Gene Separator in its three editions ([stated] Oct 29), their items and the one block entity type. */
public final class HumanGeneSeparator {

    private static final BLibRegistry<Block> BLOCKS = Human.MOD.registries().create(BuiltInRegistries.BLOCK);

    private static final BLibRegistry<Item> ITEMS = Human.MOD.registries().create(BuiltInRegistries.ITEM);

    private static final BLibRegistry<BlockEntityType<?>> BLOCK_ENTITY_TYPES = Human.MOD.registries()
        .create(BuiltInRegistries.BLOCK_ENTITY_TYPE);

    public static final BLibHolder<Block> GENE_SEPARATOR = block(GeneSeparatorBlock.Edition.BARE);

    public static final BLibHolder<Block> WY_GENE_SEPARATOR = block(GeneSeparatorBlock.Edition.WY);

    public static final BLibHolder<Block> USCM_GENE_SEPARATOR = block(GeneSeparatorBlock.Edition.USCM);

    public static final BLibHolder<Item> GENE_SEPARATOR_ITEM = item(GENE_SEPARATOR, GeneSeparatorBlock.Edition.BARE);

    public static final BLibHolder<Item> WY_GENE_SEPARATOR_ITEM = item(WY_GENE_SEPARATOR, GeneSeparatorBlock.Edition.WY);

    public static final BLibHolder<Item> USCM_GENE_SEPARATOR_ITEM = item(USCM_GENE_SEPARATOR, GeneSeparatorBlock.Edition.USCM);

    public static final BLibHolder<BlockEntityType<GeneSeparatorBlockEntity>> BLOCK_ENTITY = BLOCK_ENTITY_TYPES.createHolder(
        "gene_separator",
        () -> BlockEntityType.Builder.of(
            GeneSeparatorBlockEntity::new,
            GENE_SEPARATOR.get(),
            WY_GENE_SEPARATOR.get(),
            USCM_GENE_SEPARATOR.get()
        ).build(null)
    );

    private static final BLibRegistry<net.minecraft.world.inventory.MenuType<?>> MENUS = Human.MOD.registries()
        .create(BuiltInRegistries.MENU);

    public static final BLibHolder<net.minecraft.world.inventory.MenuType<com.human.common.gameplay.gene.machine.separator.GeneSeparatorMenu>> MENU =
        MENUS.createHolder(
            "gene_separator",
            () -> new net.minecraft.world.inventory.MenuType<>(
                com.human.common.gameplay.gene.machine.separator.GeneSeparatorMenu::new,
                net.minecraft.world.flag.FeatureFlagSet.of()
            )
        );

    private HumanGeneSeparator() {}

    private static BLibHolder<Block> block(GeneSeparatorBlock.Edition edition) {
        return BLOCKS.createHolder(
            edition.id,
            () -> new GeneSeparatorBlock(
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

    private static BLibHolder<Item> item(BLibHolder<Block> block, GeneSeparatorBlock.Edition edition) {
        return ITEMS.createHolder(edition.id, () -> new StackableModelBlockItem(block.get(), new Item.Properties().stacksTo(1)));
    }

    public static void initialize() {
        BLOCKS.registerAll();
        ITEMS.registerAll();
        BLOCK_ENTITY_TYPES.registerAll();
        MENUS.registerAll();
    }
}
