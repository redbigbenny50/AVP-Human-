package com.human.common.registry.init.item.block;

import com.blib.api.common.registry.v1.BLibHolder;
import com.blib.api.common.registry.v1.BLibRegistry;
import com.human.Human;
import com.human.common.registry.init.block.HumanCompositeBlocks;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import java.util.function.Supplier;

public class HumanCompositeBlockItems {

    private static final BLibRegistry<Item> REGISTRY = Human.MOD.registries().create(BuiltInRegistries.ITEM);

    public static final BLibHolder<BlockItem> CHISELED_COMPOSITE = create(
        "chiseled_composite",
        HumanCompositeBlocks.CHISELED_COMPOSITE
    );

    public static final BLibHolder<BlockItem> CUT_COMPOSITE = create("cut_composite", HumanCompositeBlocks.CUT_COMPOSITE);

    public static final BLibHolder<BlockItem> CUT_COMPOSITE_SLAB = create(
        "cut_composite_slab",
        HumanCompositeBlocks.CUT_COMPOSITE_SLAB
    );

    public static final BLibHolder<BlockItem> CUT_COMPOSITE_STAIRS = create(
        "cut_composite_stairs",
        HumanCompositeBlocks.CUT_COMPOSITE_STAIRS
    );

    public static final BLibHolder<BlockItem> COMPOSITE_BLOCK = create("composite_block", HumanCompositeBlocks.COMPOSITE_BLOCK);

    public static final BLibHolder<BlockItem> COMPOSITE_BUTTON = create("composite_button", HumanCompositeBlocks.COMPOSITE_BUTTON);

    public static final BLibHolder<BlockItem> COMPOSITE_CHAIN_FENCE = create(
        "composite_chain_fence",
        HumanCompositeBlocks.COMPOSITE_CHAIN_FENCE
    );

    public static final BLibHolder<BlockItem> COMPOSITE_COLUMN = create("composite_column", HumanCompositeBlocks.COMPOSITE_COLUMN);

    public static final BLibHolder<BlockItem> COMPOSITE_DOOR = create("composite_door", HumanCompositeBlocks.COMPOSITE_DOOR);

    public static final BLibHolder<BlockItem> COMPOSITE_FASTENED_SIDING = create(
        "composite_fastened_siding",
        HumanCompositeBlocks.COMPOSITE_FASTENED_SIDING
    );

    public static final BLibHolder<BlockItem> COMPOSITE_FASTENED_SIDING_SLAB = create(
        "composite_fastened_siding_slab",
        HumanCompositeBlocks.COMPOSITE_FASTENED_SIDING_SLAB
    );

    public static final BLibHolder<BlockItem> COMPOSITE_FASTENED_SIDING_STAIRS = create(
        "composite_fastened_siding_stairs",
        HumanCompositeBlocks.COMPOSITE_FASTENED_SIDING_STAIRS
    );

    public static final BLibHolder<BlockItem> COMPOSITE_FASTENED_STANDING = create(
        "composite_fastened_standing",
        HumanCompositeBlocks.COMPOSITE_FASTENED_STANDING
    );

    public static final BLibHolder<BlockItem> COMPOSITE_FASTENED_STANDING_SLAB = create(
        "composite_fastened_standing_slab",
        HumanCompositeBlocks.COMPOSITE_FASTENED_STANDING_SLAB
    );

    public static final BLibHolder<BlockItem> COMPOSITE_FASTENED_STANDING_STAIRS = create(
        "composite_fastened_standing_stairs",
        HumanCompositeBlocks.COMPOSITE_FASTENED_STANDING_STAIRS
    );

    public static final BLibHolder<BlockItem> COMPOSITE_GRATE = create("composite_grate", HumanCompositeBlocks.COMPOSITE_GRATE);

    public static final BLibHolder<BlockItem> COMPOSITE_GRATE_SLAB = create(
        "composite_grate_slab",
        HumanCompositeBlocks.COMPOSITE_GRATE_SLAB
    );

    public static final BLibHolder<BlockItem> COMPOSITE_GRATE_STAIRS = create(
        "composite_grate_stairs",
        HumanCompositeBlocks.COMPOSITE_GRATE_STAIRS
    );

    public static final BLibHolder<BlockItem> COMPOSITE_PLATING = create(
        "composite_plating",
        HumanCompositeBlocks.COMPOSITE_PLATING
    );

    public static final BLibHolder<BlockItem> COMPOSITE_PLATING_SLAB = create(
        "composite_plating_slab",
        HumanCompositeBlocks.COMPOSITE_PLATING_SLAB
    );

    public static final BLibHolder<BlockItem> COMPOSITE_PLATING_STAIRS = create(
        "composite_plating_stairs",
        HumanCompositeBlocks.COMPOSITE_PLATING_STAIRS
    );

    public static final BLibHolder<BlockItem> COMPOSITE_PRESSURE_PLATE = create(
        "composite_pressure_plate",
        HumanCompositeBlocks.COMPOSITE_PRESSURE_PLATE
    );

    public static final BLibHolder<BlockItem> COMPOSITE_SIDING = create("composite_siding", HumanCompositeBlocks.COMPOSITE_SIDING);

    public static final BLibHolder<BlockItem> COMPOSITE_SIDING_SLAB = create(
        "composite_siding_slab",
        HumanCompositeBlocks.COMPOSITE_SIDING_SLAB
    );

    public static final BLibHolder<BlockItem> COMPOSITE_SIDING_STAIRS = create(
        "composite_siding_stairs",
        HumanCompositeBlocks.COMPOSITE_SIDING_STAIRS
    );

    public static final BLibHolder<BlockItem> COMPOSITE_SLAB = create("composite_slab", HumanCompositeBlocks.COMPOSITE_SLAB);

    public static final BLibHolder<BlockItem> COMPOSITE_STAIRS = create("composite_stairs", HumanCompositeBlocks.COMPOSITE_STAIRS);

    public static final BLibHolder<BlockItem> COMPOSITE_STANDING = create(
        "composite_standing",
        HumanCompositeBlocks.COMPOSITE_STANDING
    );

    public static final BLibHolder<BlockItem> COMPOSITE_STANDING_SLAB = create(
        "composite_standing_slab",
        HumanCompositeBlocks.COMPOSITE_STANDING_SLAB
    );

    public static final BLibHolder<BlockItem> COMPOSITE_STANDING_STAIRS = create(
        "composite_standing_stairs",
        HumanCompositeBlocks.COMPOSITE_STANDING_STAIRS
    );

    public static final BLibHolder<BlockItem> COMPOSITE_TRAP_DOOR = create(
        "composite_trapdoor",
        HumanCompositeBlocks.COMPOSITE_TRAP_DOOR
    );

    public static final BLibHolder<BlockItem> COMPOSITE_TREAD = create("composite_tread", HumanCompositeBlocks.COMPOSITE_TREAD);

    public static final BLibHolder<BlockItem> COMPOSITE_TREAD_SLAB = create(
        "composite_tread_slab",
        HumanCompositeBlocks.COMPOSITE_TREAD_SLAB
    );

    public static final BLibHolder<BlockItem> COMPOSITE_TREAD_STAIRS = create(
        "composite_tread_stairs",
        HumanCompositeBlocks.COMPOSITE_TREAD_STAIRS
    );

    private static BLibHolder<BlockItem> create(String path, Supplier<? extends Block> blockSupplier) {
        return create(path, blockSupplier, new Item.Properties());
    }

    private static BLibHolder<BlockItem> create(String path, Supplier<? extends Block> blockSupplier, Item.Properties properties) {
        return createBlockItem(path, () -> new BlockItem(blockSupplier.get(), properties));
    }

    private static BLibHolder<BlockItem> createBlockItem(String path, Supplier<BlockItem> blockItemSupplier) {
        return REGISTRY.createHolder(path, blockItemSupplier);
    }

    public static void initialize() {
        REGISTRY.registerAll();
    }
}
