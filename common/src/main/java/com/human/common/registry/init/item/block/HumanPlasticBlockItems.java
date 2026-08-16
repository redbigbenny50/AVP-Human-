package com.human.common.registry.init.item.block;

import com.blib.api.common.registry.v1.BLibHolder;
import com.blib.api.common.registry.v1.BLibRegistry;
import com.human.Human;
import com.human.common.registry.init.block.HumanPlasticBlocks;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class HumanPlasticBlockItems {

    private static final BLibRegistry<Item> REGISTRY = Human.MOD.registries().create(BuiltInRegistries.ITEM);

    public static final BLibHolder<BlockItem> AISLE_HAZARD_PLASTIC = create(
        "aisle_hazard_plastic",
        HumanPlasticBlocks.AISLE_HAZARD_PLASTIC
    );

    public static final BLibHolder<BlockItem> ALIEN_HAZARD_PLASTIC = create(
        "alien_hazard_plastic",
        HumanPlasticBlocks.ALIEN_HAZARD_PLASTIC
    );

    public static final BLibHolder<BlockItem> FIRE_HAZARD_PLASTIC = create(
        "fire_hazard_plastic",
        HumanPlasticBlocks.FIRE_HAZARD_PLASTIC
    );

    public static final BLibHolder<BlockItem> HAZARD_PLASTIC = create(
        "hazard_plastic",
        HumanPlasticBlocks.HAZARD_PLASTIC
    );

    public static final BLibHolder<BlockItem> MACHINE_HAZARD_PLASTIC = create(
        "machine_hazard_plastic",
        HumanPlasticBlocks.MACHINE_HAZARD_PLASTIC
    );

    public static final BLibHolder<BlockItem> RADIATION_HAZARD_PLASTIC = create(
        "radiation_hazard_plastic",
        HumanPlasticBlocks.RADIATION_HAZARD_PLASTIC
    );

    public static final BLibHolder<BlockItem> SAFETY_PLASTIC = create(
        "safety_plastic",
        HumanPlasticBlocks.SAFETY_PLASTIC
    );

    public static final BLibHolder<BlockItem> TRAFFIC_HAZARD_PLASTIC = create(
        "traffic_hazard_plastic",
        HumanPlasticBlocks.TRAFFIC_HAZARD_PLASTIC
    );

    // Hazard/Safety plastic stairs
    public static final BLibHolder<BlockItem> AISLE_HAZARD_PLASTIC_STAIRS = create(
        "aisle_hazard_plastic_stairs",
        HumanPlasticBlocks.AISLE_HAZARD_PLASTIC_STAIRS
    );

    public static final BLibHolder<BlockItem> ALIEN_HAZARD_PLASTIC_STAIRS = create(
        "alien_hazard_plastic_stairs",
        HumanPlasticBlocks.ALIEN_HAZARD_PLASTIC_STAIRS
    );

    public static final BLibHolder<BlockItem> FIRE_HAZARD_PLASTIC_STAIRS = create(
        "fire_hazard_plastic_stairs",
        HumanPlasticBlocks.FIRE_HAZARD_PLASTIC_STAIRS
    );

    public static final BLibHolder<BlockItem> HAZARD_PLASTIC_STAIRS = create(
        "hazard_plastic_stairs",
        HumanPlasticBlocks.HAZARD_PLASTIC_STAIRS
    );

    public static final BLibHolder<BlockItem> MACHINE_HAZARD_PLASTIC_STAIRS = create(
        "machine_hazard_plastic_stairs",
        HumanPlasticBlocks.MACHINE_HAZARD_PLASTIC_STAIRS
    );

    public static final BLibHolder<BlockItem> RADIATION_HAZARD_PLASTIC_STAIRS = create(
        "radiation_hazard_plastic_stairs",
        HumanPlasticBlocks.RADIATION_HAZARD_PLASTIC_STAIRS
    );

    public static final BLibHolder<BlockItem> SAFETY_PLASTIC_STAIRS = create(
        "safety_plastic_stairs",
        HumanPlasticBlocks.SAFETY_PLASTIC_STAIRS
    );

    public static final BLibHolder<BlockItem> TRAFFIC_HAZARD_PLASTIC_STAIRS = create(
        "traffic_hazard_plastic_stairs",
        HumanPlasticBlocks.TRAFFIC_HAZARD_PLASTIC_STAIRS
    );

    // Hazard/Safety plastic slabs
    public static final BLibHolder<BlockItem> AISLE_HAZARD_PLASTIC_SLAB = create(
        "aisle_hazard_plastic_slab",
        HumanPlasticBlocks.AISLE_HAZARD_PLASTIC_SLAB
    );

    public static final BLibHolder<BlockItem> ALIEN_HAZARD_PLASTIC_SLAB = create(
        "alien_hazard_plastic_slab",
        HumanPlasticBlocks.ALIEN_HAZARD_PLASTIC_SLAB
    );

    public static final BLibHolder<BlockItem> FIRE_HAZARD_PLASTIC_SLAB = create(
        "fire_hazard_plastic_slab",
        HumanPlasticBlocks.FIRE_HAZARD_PLASTIC_SLAB
    );

    public static final BLibHolder<BlockItem> HAZARD_PLASTIC_SLAB = create(
        "hazard_plastic_slab",
        HumanPlasticBlocks.HAZARD_PLASTIC_SLAB
    );

    public static final BLibHolder<BlockItem> MACHINE_HAZARD_PLASTIC_SLAB = create(
        "machine_hazard_plastic_slab",
        HumanPlasticBlocks.MACHINE_HAZARD_PLASTIC_SLAB
    );

    public static final BLibHolder<BlockItem> RADIATION_HAZARD_PLASTIC_SLAB = create(
        "radiation_hazard_plastic_slab",
        HumanPlasticBlocks.RADIATION_HAZARD_PLASTIC_SLAB
    );

    public static final BLibHolder<BlockItem> SAFETY_PLASTIC_SLAB = create(
        "safety_plastic_slab",
        HumanPlasticBlocks.SAFETY_PLASTIC_SLAB
    );

    public static final BLibHolder<BlockItem> TRAFFIC_HAZARD_PLASTIC_SLAB = create(
        "traffic_hazard_plastic_slab",
        HumanPlasticBlocks.TRAFFIC_HAZARD_PLASTIC_SLAB
    );

    // Hazard/Safety plastic walls
    public static final BLibHolder<BlockItem> AISLE_HAZARD_PLASTIC_WALL = create(
        "aisle_hazard_plastic_wall",
        HumanPlasticBlocks.AISLE_HAZARD_PLASTIC_WALL
    );

    public static final BLibHolder<BlockItem> ALIEN_HAZARD_PLASTIC_WALL = create(
        "alien_hazard_plastic_wall",
        HumanPlasticBlocks.ALIEN_HAZARD_PLASTIC_WALL
    );

    public static final BLibHolder<BlockItem> FIRE_HAZARD_PLASTIC_WALL = create(
        "fire_hazard_plastic_wall",
        HumanPlasticBlocks.FIRE_HAZARD_PLASTIC_WALL
    );

    public static final BLibHolder<BlockItem> HAZARD_PLASTIC_WALL = create(
        "hazard_plastic_wall",
        HumanPlasticBlocks.HAZARD_PLASTIC_WALL
    );

    public static final BLibHolder<BlockItem> MACHINE_HAZARD_PLASTIC_WALL = create(
        "machine_hazard_plastic_wall",
        HumanPlasticBlocks.MACHINE_HAZARD_PLASTIC_WALL
    );

    public static final BLibHolder<BlockItem> RADIATION_HAZARD_PLASTIC_WALL = create(
        "radiation_hazard_plastic_wall",
        HumanPlasticBlocks.RADIATION_HAZARD_PLASTIC_WALL
    );

    public static final BLibHolder<BlockItem> SAFETY_PLASTIC_WALL = create(
        "safety_plastic_wall",
        HumanPlasticBlocks.SAFETY_PLASTIC_WALL
    );

    public static final BLibHolder<BlockItem> TRAFFIC_HAZARD_PLASTIC_WALL = create(
        "traffic_hazard_plastic_wall",
        HumanPlasticBlocks.TRAFFIC_HAZARD_PLASTIC_WALL
    );

    public static final Map<DyeColor, Supplier<BlockItem>> DYE_COLOR_TO_CUT_PLASTIC =
        HumanPlasticBlocks.DYE_COLOR_TO_CUT_PLASTIC.entrySet()
            .stream()
            .collect(
                Collectors.toMap(
                    Map.Entry::getKey,
                    entry -> create(
                        entry.getKey().getName() + "_cut_plastic",
                        entry.getValue()
                    ),
                    (a, b) -> b,
                    LinkedHashMap::new
                )
            );

    public static final Map<DyeColor, Supplier<BlockItem>> DYE_COLOR_TO_CUT_PLASTIC_SLAB =
        HumanPlasticBlocks.DYE_COLOR_TO_CUT_PLASTIC_SLAB.entrySet()
            .stream()
            .collect(
                Collectors.toMap(
                    Map.Entry::getKey,
                    entry -> create(
                        entry.getKey().getName() + "_cut_plastic_slab",
                        entry.getValue()
                    ),
                    (a, b) -> b,
                    LinkedHashMap::new
                )
            );

    public static final Map<DyeColor, Supplier<BlockItem>> DYE_COLOR_TO_CUT_PLASTIC_STAIRS =
        HumanPlasticBlocks.DYE_COLOR_TO_CUT_PLASTIC_STAIRS.entrySet()
            .stream()
            .collect(
                Collectors.toMap(
                    Map.Entry::getKey,
                    entry -> create(
                        entry.getKey().getName() + "_cut_plastic_stairs",
                        entry.getValue()
                    ),
                    (a, b) -> b,
                    LinkedHashMap::new
                )
            );

    public static final Map<DyeColor, Supplier<BlockItem>> DYE_COLOR_TO_FRAMED_PLASTIC =
        HumanPlasticBlocks.DYE_COLOR_TO_FRAMED_PLASTIC.entrySet()
            .stream()
            .collect(
                Collectors.toMap(
                    Map.Entry::getKey,
                    entry -> create(
                        entry.getKey().getName() + "_framed_plastic",
                        entry.getValue()
                    ),
                    (a, b) -> b,
                    LinkedHashMap::new
                )
            );

    public static final Map<DyeColor, Supplier<BlockItem>> DYE_COLOR_TO_PITTED_PLASTIC =
        HumanPlasticBlocks.DYE_COLOR_TO_PITTED_PLASTIC.entrySet()
            .stream()
            .collect(
                Collectors.toMap(
                    Map.Entry::getKey,
                    entry -> create(
                        entry.getKey().getName() + "_pitted_plastic",
                        entry.getValue()
                    ),
                    (a, b) -> b,
                    LinkedHashMap::new
                )
            );

    public static final Map<DyeColor, Supplier<BlockItem>> DYE_COLOR_TO_PITTED_PLASTIC_SLAB =
        HumanPlasticBlocks.DYE_COLOR_TO_PITTED_PLASTIC_SLAB.entrySet()
            .stream()
            .collect(
                Collectors.toMap(
                    Map.Entry::getKey,
                    entry -> create(
                        entry.getKey().getName() + "_pitted_plastic_slab",
                        entry.getValue()
                    ),
                    (a, b) -> b,
                    LinkedHashMap::new
                )
            );

    public static final Map<DyeColor, Supplier<BlockItem>> DYE_COLOR_TO_PITTED_PLASTIC_STAIRS =
        HumanPlasticBlocks.DYE_COLOR_TO_PITTED_PLASTIC_STAIRS.entrySet()
            .stream()
            .collect(
                Collectors.toMap(
                    Map.Entry::getKey,
                    entry -> create(
                        entry.getKey().getName() + "_pitted_plastic_stairs",
                        entry.getValue()
                    ),
                    (a, b) -> b,
                    LinkedHashMap::new
                )
            );

    public static final Map<DyeColor, Supplier<BlockItem>> DYE_COLOR_TO_PLASTIC =
        HumanPlasticBlocks.DYE_COLOR_TO_PLASTIC.entrySet()
            .stream()
            .collect(
                Collectors.toMap(
                    Map.Entry::getKey,
                    entry -> create(
                        entry.getKey().getName() + "_plastic",
                        entry.getValue()
                    ),
                    (a, b) -> b,
                    LinkedHashMap::new
                )
            );

    public static final Map<DyeColor, Supplier<BlockItem>> DYE_COLOR_TO_PLASTIC_WALL =
        HumanPlasticBlocks.DYE_COLOR_TO_PLASTIC_WALL.entrySet()
            .stream()
            .collect(
                Collectors.toMap(
                    Map.Entry::getKey,
                    entry -> create(
                        entry.getKey().getName() + "_plastic_wall",
                        entry.getValue()
                    ),
                    (a, b) -> b,
                    LinkedHashMap::new
                )
            );

    public static final Map<DyeColor, Supplier<BlockItem>> DYE_COLOR_TO_PLASTIC_SLAB =
        HumanPlasticBlocks.DYE_COLOR_TO_PLASTIC_SLAB.entrySet()
            .stream()
            .collect(
                Collectors.toMap(
                    Map.Entry::getKey,
                    entry -> create(
                        entry.getKey().getName() + "_plastic_slab",
                        entry.getValue()
                    ),
                    (a, b) -> b,
                    LinkedHashMap::new
                )
            );

    public static final Map<DyeColor, Supplier<BlockItem>> DYE_COLOR_TO_PLASTIC_STAIRS =
        HumanPlasticBlocks.DYE_COLOR_TO_PLASTIC_STAIRS.entrySet()
            .stream()
            .collect(
                Collectors.toMap(
                    Map.Entry::getKey,
                    entry -> create(
                        entry.getKey().getName() + "_plastic_stairs",
                        entry.getValue()
                    ),
                    (a, b) -> b,
                    LinkedHashMap::new
                )
            );

    public static final Map<DyeColor, Supplier<BlockItem>> DYE_COLOR_TO_PLASTIC_GRATE =
        HumanPlasticBlocks.DYE_COLOR_TO_PLASTIC_GRATE.entrySet()
            .stream()
            .collect(
                Collectors.toMap(
                    Map.Entry::getKey,
                    entry -> create(
                        entry.getKey().getName() + "_plastic_grate",
                        entry.getValue()
                    ),
                    (a, b) -> b,
                    LinkedHashMap::new
                )
            );

    public static final Map<DyeColor, Supplier<BlockItem>> DYE_COLOR_TO_PLASTIC_GRATE_SLAB =
        HumanPlasticBlocks.DYE_COLOR_TO_PLASTIC_GRATE_SLAB.entrySet()
            .stream()
            .collect(
                Collectors.toMap(
                    Map.Entry::getKey,
                    entry -> create(
                        entry.getKey().getName() + "_plastic_grate_slab",
                        entry.getValue()
                    ),
                    (a, b) -> b,
                    LinkedHashMap::new
                )
            );

    public static final Map<DyeColor, Supplier<BlockItem>> DYE_COLOR_TO_PLASTIC_GRATE_STAIRS =
        HumanPlasticBlocks.DYE_COLOR_TO_PLASTIC_GRATE_STAIRS.entrySet()
            .stream()
            .collect(
                Collectors.toMap(
                    Map.Entry::getKey,
                    entry -> create(
                        entry.getKey().getName() + "_plastic_grate_stairs",
                        entry.getValue()
                    ),
                    (a, b) -> b,
                    LinkedHashMap::new
                )
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
