package com.human.common.registry.init;

import com.blib.api.common.registry.v1.BLibHolder;
import com.blib.api.common.registry.v1.BLibRegistry;
import com.human.Human;
import com.human.common.gameplay.block.metal.MetalLadderBlock;
import com.human.common.gameplay.block.property.HumanBlockProperties;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChainBlock;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

import java.util.List;
import java.util.function.Supplier;

/**
 * [stated] Oct 21 - bars, chains and ladders for the five metals (steel, titanium, ferroaluminum, aluminum, composite),
 * plus the composite nugget the chain recipe needs ("all those metals should have nuggets").
 * <ul>
 * <li>Bars - like iron bars. Steel bars already existed (HumanSteelBlocks) and are not repeated here.</li>
 * <li>Chains - like vanilla chains, upright or on their side.</li>
 * <li>Ladders - {@link MetalLadderBlock}: hang like vines, break like vines, build downwards on a right-click.</li>
 * </ul>
 */
public final class HumanMetalFixtures {

    private static final BLibRegistry<Block> BLOCKS = Human.MOD.registries().create(BuiltInRegistries.BLOCK);

    private static final BLibRegistry<Item> ITEMS = Human.MOD.registries().create(BuiltInRegistries.ITEM);

    // --- Bars (steel bars already exist) --------------------------------------------------------------------------

    public static final BLibHolder<Block> TITANIUM_BARS = block(
        "titanium_bars",
        () -> new IronBarsBlock(HumanBlockProperties.TITANIUM_BARS.build())
    );

    public static final BLibHolder<Block> FERROALUMINUM_BARS = block(
        "ferroaluminum_bars",
        () -> new IronBarsBlock(HumanBlockProperties.FERROALUMINUM_BARS.build())
    );

    public static final BLibHolder<Block> ALUMINUM_BARS = block(
        "aluminum_bars",
        () -> new IronBarsBlock(HumanBlockProperties.STEEL_BARS.build().mapColor(MapColor.METAL))
    );

    public static final BLibHolder<Block> COMPOSITE_BARS = block(
        "composite_bars",
        () -> new IronBarsBlock(HumanBlockProperties.STEEL_BARS.build().mapColor(MapColor.COLOR_GREEN))
    );

    // --- Chains -----------------------------------------------------------------------------------------------------

    public static final BLibHolder<Block> STEEL_CHAIN = chain("steel_chain", MapColor.COLOR_GRAY);

    public static final BLibHolder<Block> TITANIUM_CHAIN = chain("titanium_chain", MapColor.SAND);

    public static final BLibHolder<Block> FERROALUMINUM_CHAIN = chain("ferroaluminum_chain", MapColor.COLOR_BLACK);

    public static final BLibHolder<Block> ALUMINUM_CHAIN = chain("aluminum_chain", MapColor.METAL);

    public static final BLibHolder<Block> COMPOSITE_CHAIN = chain("composite_chain", MapColor.COLOR_GREEN);

    // --- Ladders ----------------------------------------------------------------------------------------------------

    public static final BLibHolder<Block> STEEL_LADDER = ladder("steel_ladder", MapColor.COLOR_GRAY);

    public static final BLibHolder<Block> TITANIUM_LADDER = ladder("titanium_ladder", MapColor.SAND);

    public static final BLibHolder<Block> FERROALUMINUM_LADDER = ladder("ferroaluminum_ladder", MapColor.COLOR_BLACK);

    public static final BLibHolder<Block> ALUMINUM_LADDER = ladder("aluminum_ladder", MapColor.METAL);

    public static final BLibHolder<Block> COMPOSITE_LADDER = ladder("composite_ladder", MapColor.COLOR_GREEN);

    // --- Items ------------------------------------------------------------------------------------------------------

    public static final BLibHolder<Item> TITANIUM_BARS_ITEM = blockItem("titanium_bars", TITANIUM_BARS);

    public static final BLibHolder<Item> FERROALUMINUM_BARS_ITEM = blockItem("ferroaluminum_bars", FERROALUMINUM_BARS);

    public static final BLibHolder<Item> ALUMINUM_BARS_ITEM = blockItem("aluminum_bars", ALUMINUM_BARS);

    public static final BLibHolder<Item> COMPOSITE_BARS_ITEM = blockItem("composite_bars", COMPOSITE_BARS);

    public static final BLibHolder<Item> STEEL_CHAIN_ITEM = blockItem("steel_chain", STEEL_CHAIN);

    public static final BLibHolder<Item> TITANIUM_CHAIN_ITEM = blockItem("titanium_chain", TITANIUM_CHAIN);

    public static final BLibHolder<Item> FERROALUMINUM_CHAIN_ITEM = blockItem("ferroaluminum_chain", FERROALUMINUM_CHAIN);

    public static final BLibHolder<Item> ALUMINUM_CHAIN_ITEM = blockItem("aluminum_chain", ALUMINUM_CHAIN);

    public static final BLibHolder<Item> COMPOSITE_CHAIN_ITEM = blockItem("composite_chain", COMPOSITE_CHAIN);

    public static final BLibHolder<Item> STEEL_LADDER_ITEM = blockItem("steel_ladder", STEEL_LADDER);

    public static final BLibHolder<Item> TITANIUM_LADDER_ITEM = blockItem("titanium_ladder", TITANIUM_LADDER);

    public static final BLibHolder<Item> FERROALUMINUM_LADDER_ITEM = blockItem("ferroaluminum_ladder", FERROALUMINUM_LADDER);

    public static final BLibHolder<Item> ALUMINUM_LADDER_ITEM = blockItem("aluminum_ladder", ALUMINUM_LADDER);

    public static final BLibHolder<Item> COMPOSITE_LADDER_ITEM = blockItem("composite_ladder", COMPOSITE_LADDER);

    /** The only one of the five metals that had no nugget. */
    public static final BLibHolder<Item> COMPOSITE_NUGGET = ITEMS.createHolder("composite_nugget", () -> new Item(new Item.Properties()));

    /** In metal order (steel, titanium, ferroaluminum, aluminum, composite) - for tags, tabs and datagen. */
    public static List<BLibHolder<Block>> chains() {
        return List.of(STEEL_CHAIN, TITANIUM_CHAIN, FERROALUMINUM_CHAIN, ALUMINUM_CHAIN, COMPOSITE_CHAIN);
    }

    public static List<BLibHolder<Block>> ladders() {
        return List.of(STEEL_LADDER, TITANIUM_LADDER, FERROALUMINUM_LADDER, ALUMINUM_LADDER, COMPOSITE_LADDER);
    }

    /** The NEW bars only - steel bars live in HumanSteelBlocks. */
    public static List<BLibHolder<Block>> bars() {
        return List.of(TITANIUM_BARS, FERROALUMINUM_BARS, ALUMINUM_BARS, COMPOSITE_BARS);
    }

    private static BLibHolder<Block> block(String name, Supplier<Block> supplier) {
        return BLOCKS.createHolder(name, supplier);
    }

    private static BLibHolder<Block> chain(String name, MapColor color) {
        return block(name, () -> new ChainBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.CHAIN).mapColor(color)));
    }

    private static BLibHolder<Block> ladder(String name, MapColor color) {
        return block(
            name,
            () -> new MetalLadderBlock(
                BlockBehaviour.Properties.ofFullCopy(Blocks.LADDER)
                    .mapColor(color)
                    .strength(1.5F, 6.0F)
                    .sound(SoundType.METAL)
            )
        );
    }

    private static BLibHolder<Item> blockItem(String name, BLibHolder<Block> block) {
        return ITEMS.createHolder(name, () -> new BlockItem(block.get(), new Item.Properties()));
    }

    private HumanMetalFixtures() {}

    public static void initialize() {
        BLOCKS.registerAll();
        ITEMS.registerAll();
    }
}
