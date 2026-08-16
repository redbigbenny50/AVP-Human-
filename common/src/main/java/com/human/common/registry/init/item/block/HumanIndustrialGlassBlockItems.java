package com.human.common.registry.init.item.block;

import com.blib.api.common.registry.v1.BLibHolder;
import com.blib.api.common.registry.v1.BLibRegistry;
import com.human.Human;
import com.human.common.registry.init.block.HumanIndustrialGlassBlocks;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class HumanIndustrialGlassBlockItems {

    private static final BLibRegistry<Item> REGISTRY = Human.MOD.registries().create(BuiltInRegistries.ITEM);

    public static final BLibHolder<BlockItem> INDUSTRIAL_GLASS = create(
        "industrial_glass",
        HumanIndustrialGlassBlocks.INDUSTRIAL_GLASS
    );

    public static final BLibHolder<BlockItem> INDUSTRIAL_GLASS_DOOR = create(
        "industrial_glass_door",
        HumanIndustrialGlassBlocks.INDUSTRIAL_GLASS_DOOR
    );

    public static final BLibHolder<BlockItem> INDUSTRIAL_GLASS_WALL = create(
        "industrial_glass_wall",
        HumanIndustrialGlassBlocks.INDUSTRIAL_GLASS_WALL
    );

    public static final BLibHolder<BlockItem> INDUSTRIAL_GLASS_PANE = create(
        "industrial_glass_pane",
        HumanIndustrialGlassBlocks.INDUSTRIAL_GLASS_PANE
    );

    public static final BLibHolder<BlockItem> INDUSTRIAL_GLASS_SLAB = create(
        "industrial_glass_slab",
        HumanIndustrialGlassBlocks.INDUSTRIAL_GLASS_SLAB
    );

    public static final BLibHolder<BlockItem> INDUSTRIAL_GLASS_STAIRS = create(
        "industrial_glass_stairs",
        HumanIndustrialGlassBlocks.INDUSTRIAL_GLASS_STAIRS
    );

    public static final BLibHolder<BlockItem> INDUSTRIAL_GLASS_TRAP_DOOR = create(
        "industrial_glass_trapdoor",
        HumanIndustrialGlassBlocks.INDUSTRIAL_GLASS_TRAP_DOOR
    );

    public static final Map<DyeColor, Supplier<BlockItem>> DYE_COLOR_TO_INDUSTRIAL_GLASS =
        HumanIndustrialGlassBlocks.DYE_COLOR_TO_INDUSTRIAL_GLASS.entrySet()
            .stream()
            .collect(
                Collectors.toMap(
                    Map.Entry::getKey,
                    entry -> create(
                        entry.getKey().getName() + "_industrial_glass",
                        entry.getValue()
                    ),
                    (a, b) -> b,
                    LinkedHashMap::new
                )
            );

    public static final Map<DyeColor, Supplier<BlockItem>> DYE_COLOR_TO_INDUSTRIAL_GLASS_PANE =
        HumanIndustrialGlassBlocks.DYE_COLOR_TO_INDUSTRIAL_GLASS_PANE.entrySet()
            .stream()
            .collect(
                Collectors.toMap(
                    Map.Entry::getKey,
                    entry -> create(
                        entry.getKey().getName() + "_industrial_glass_pane",
                        entry.getValue()
                    ),
                    (a, b) -> b,
                    LinkedHashMap::new
                )
            );

    public static final Map<DyeColor, Supplier<BlockItem>> DYE_COLOR_TO_INDUSTRIAL_GLASS_STAIRS =
        HumanIndustrialGlassBlocks.DYE_COLOR_TO_INDUSTRIAL_GLASS_STAIRS.entrySet()
            .stream()
            .collect(
                Collectors.toMap(
                    Map.Entry::getKey,
                    entry -> create(
                        entry.getKey().getName() + "_industrial_glass_stairs",
                        entry.getValue()
                    ),
                    (a, b) -> b,
                    LinkedHashMap::new
                )
            );

    public static final Map<DyeColor, Supplier<BlockItem>> DYE_COLOR_TO_INDUSTRIAL_GLASS_SLAB =
        HumanIndustrialGlassBlocks.DYE_COLOR_TO_INDUSTRIAL_GLASS_SLAB.entrySet()
            .stream()
            .collect(
                Collectors.toMap(
                    Map.Entry::getKey,
                    entry -> create(
                        entry.getKey().getName() + "_industrial_glass_slab",
                        entry.getValue()
                    ),
                    (a, b) -> b,
                    LinkedHashMap::new
                )
            );

    public static final Map<DyeColor, Supplier<BlockItem>> DYE_COLOR_TO_INDUSTRIAL_GLASS_DOOR =
        HumanIndustrialGlassBlocks.DYE_COLOR_TO_INDUSTRIAL_GLASS_DOOR.entrySet()
            .stream()
            .collect(
                Collectors.toMap(
                    Map.Entry::getKey,
                    entry -> create(
                        entry.getKey().getName() + "_industrial_glass_door",
                        entry.getValue()
                    ),
                    (a, b) -> b,
                    LinkedHashMap::new
                )
            );

    public static final Map<DyeColor, Supplier<BlockItem>> DYE_COLOR_TO_INDUSTRIAL_GLASS_TRAP_DOOR =
        HumanIndustrialGlassBlocks.DYE_COLOR_TO_INDUSTRIAL_GLASS_TRAP_DOOR.entrySet()
            .stream()
            .collect(
                Collectors.toMap(
                    Map.Entry::getKey,
                    entry -> create(
                        entry.getKey().getName() + "_industrial_glass_trapdoor",
                        entry.getValue()
                    ),
                    (a, b) -> b,
                    LinkedHashMap::new
                )
            );

    public static final Map<DyeColor, Supplier<BlockItem>> DYE_COLOR_TO_INDUSTRIAL_GLASS_WALL =
        HumanIndustrialGlassBlocks.DYE_COLOR_TO_INDUSTRIAL_GLASS_WALL.entrySet()
            .stream()
            .collect(
                Collectors.toMap(
                    Map.Entry::getKey,
                    entry -> create(
                        entry.getKey().getName() + "_industrial_glass_wall",
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
