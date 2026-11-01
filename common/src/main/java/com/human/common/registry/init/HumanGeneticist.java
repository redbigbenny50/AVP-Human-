package com.human.common.registry.init;

import com.blib.api.common.registry.v1.BLibHolder;
import com.blib.api.common.registry.v1.BLibRegistry;
import com.human.Human;
import com.human.common.gameplay.entity.living.human.colonist.ColonistProfessions;
import com.human.common.gameplay.gene.geneticist.GeneticistProfession;
import com.human.common.gameplay.gene.geneticist.GeneticistStationBlock;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

/** The Geneticist: his station, his outfit, his profession. Registered from Genes.initialize. */
public final class HumanGeneticist {

    private static final BLibRegistry<Block> BLOCKS = Human.MOD.registries().create(BuiltInRegistries.BLOCK);

    private static final BLibRegistry<Item> ITEMS = Human.MOD.registries().create(BuiltInRegistries.ITEM);

    public static final BLibHolder<Block> GENETICIST_STATION = BLOCKS.createHolder(
        "geneticist_station",
        () -> new GeneticistStationBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.5F, 6.0F).sound(SoundType.METAL).noOcclusion()
        )
    );

    public static final BLibHolder<Item> GENETICIST_STATION_ITEM = ITEMS.createHolder(
        "geneticist_station",
        () -> new BlockItem(GENETICIST_STATION.get(), new Item.Properties())
    );

    /** The outfit - work clothes like the electrician's: leather-grade, worn by the colonist, never dropped. */
    public static final BLibHolder<Item> GENETICIST_GOGGLES = ITEMS.createHolder(
        "geneticist_goggles",
        () -> new ArmorItem(
            ArmorMaterials.LEATHER,
            ArmorItem.Type.HELMET,
            new Item.Properties().durability(ArmorItem.Type.HELMET.getDurability(5))
        )
    );

    public static final BLibHolder<Item> GENETICIST_LAB_COAT = ITEMS.createHolder(
        "geneticist_lab_coat",
        () -> new ArmorItem(
            ArmorMaterials.LEATHER,
            ArmorItem.Type.CHESTPLATE,
            new Item.Properties().durability(ArmorItem.Type.CHESTPLATE.getDurability(5))
        )
    );

    public static final BLibHolder<Item> GENETICIST_COAT_TAILS = ITEMS.createHolder(
        "geneticist_coat_tails",
        () -> new ArmorItem(
            ArmorMaterials.LEATHER,
            ArmorItem.Type.LEGGINGS,
            new Item.Properties().durability(ArmorItem.Type.LEGGINGS.getDurability(5))
        )
    );

    /** The Analyzer's printout - teaches its genes and species to whoever uses it. */
    public static final BLibHolder<Item> GENETIC_RESEARCH = ITEMS.createHolder(
        "genetic_research",
        () -> new com.human.common.gameplay.gene.discovery.GeneticResearchItem(new Item.Properties().stacksTo(16))
    );

    /** [stated] Oct 29: the cleanser - an empty gene tube and a diamond; cleanses one syringe at the station. */
    public static final BLibHolder<Item> CLEANSER = ITEMS.createHolder(
        "cleanser",
        () -> new com.human.common.gameplay.gene.geneticist.CleanserItem(new Item.Properties().stacksTo(16))
    );

    private static final BLibRegistry<net.minecraft.world.inventory.MenuType<?>> MENUS = Human.MOD.registries()
        .create(BuiltInRegistries.MENU);

    public static final BLibHolder<net.minecraft.world.inventory.MenuType<com.human.common.gameplay.gene.geneticist.GeneticistStationMenu>> STATION_MENU =
        MENUS.createHolder(
            "geneticist_station",
            () -> new net.minecraft.world.inventory.MenuType<>(
                com.human.common.gameplay.gene.geneticist.GeneticistStationMenu::new,
                net.minecraft.world.flag.FeatureFlagSet.of()
            )
        );

    private HumanGeneticist() {}

    public static void initialize() {
        BLOCKS.registerAll();
        ITEMS.registerAll();
        MENUS.registerAll();
        ColonistProfessions.register(GeneticistProfession.INSTANCE);
    }
}
