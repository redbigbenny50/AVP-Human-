package com.human.common.registry.init;

import com.blib.api.common.registry.v1.BLibHolder;
import com.blib.api.common.registry.v1.BLibRegistry;
import com.human.Human;
import com.human.common.gameplay.gene.tube.GeneTubeItem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;

/**
 * Gene tubes. [stated] Oct 27 - Empty Gene Tube = a copper ingot over a glass V (glass-bottle shape), makes 8. [stated]
 * Oct 28 - the Separator fills one tube per gene holding all of that gene's doses, and filled tubes stack to 16 when
 * gene, value and donor match. Registered from Genes.initialize.
 */
public final class HumanGeneTubes {

    private static final BLibRegistry<Item> ITEMS = Human.MOD.registries().create(BuiltInRegistries.ITEM);

    public static final BLibHolder<Item> EMPTY_GENE_TUBE = ITEMS.createHolder(
        "empty_gene_tube",
        () -> new Item(new Item.Properties().stacksTo(64))
    );

    public static final BLibHolder<Item> GENE_TUBE = ITEMS.createHolder(
        "gene_tube",
        () -> new GeneTubeItem(new Item.Properties().stacksTo(16))
    );

    private HumanGeneTubes() {}

    public static void initialize() {
        ITEMS.registerAll();
    }
}
