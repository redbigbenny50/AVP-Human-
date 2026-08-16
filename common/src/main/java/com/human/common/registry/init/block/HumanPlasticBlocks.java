package com.human.common.registry.init.block;

import com.blib.api.common.block.v1.BlockPropertyBuilder;
import com.blib.api.common.registry.v1.BLibHolder;
import com.blib.api.common.registry.v1.BLibRegistry;
import com.human.Human;
import com.human.common.gameplay.block.property.HumanBlockProperties;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.WaterloggedTransparentBlock;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class HumanPlasticBlocks {

    public static final BLibRegistry<Block> REGISTRY = Human.MOD.registries().create(BuiltInRegistries.BLOCK);

    public static final BLibHolder<Block> AISLE_HAZARD_PLASTIC = create(
        "aisle_hazard_plastic",
        HumanBlockProperties.PLASTIC
    );

    public static final BLibHolder<Block> ALIEN_HAZARD_PLASTIC = create(
        "alien_hazard_plastic",
        HumanBlockProperties.PLASTIC
    );

    public static final BLibHolder<Block> FIRE_HAZARD_PLASTIC = create(
        "fire_hazard_plastic",
        HumanBlockProperties.PLASTIC
    );

    public static final BLibHolder<Block> HAZARD_PLASTIC = create(
        "hazard_plastic",
        HumanBlockProperties.PLASTIC
    );

    public static final BLibHolder<Block> MACHINE_HAZARD_PLASTIC = create(
        "machine_hazard_plastic",
        HumanBlockProperties.PLASTIC
    );

    public static final BLibHolder<Block> RADIATION_HAZARD_PLASTIC = create(
        "radiation_hazard_plastic",
        HumanBlockProperties.PLASTIC
    );

    public static final BLibHolder<Block> SAFETY_PLASTIC = create(
        "safety_plastic",
        HumanBlockProperties.PLASTIC
    );

    public static final BLibHolder<Block> TRAFFIC_HAZARD_PLASTIC = create(
        "traffic_hazard_plastic",
        HumanBlockProperties.PLASTIC
    );

    // Hazard/Safety plastic stairs
    public static final BLibHolder<Block> AISLE_HAZARD_PLASTIC_STAIRS = create(
        "aisle_hazard_plastic_stairs",
        () -> new StairBlock(AISLE_HAZARD_PLASTIC.get().defaultBlockState(), HumanBlockProperties.PLASTIC.build())
    );

    public static final BLibHolder<Block> ALIEN_HAZARD_PLASTIC_STAIRS = create(
        "alien_hazard_plastic_stairs",
        () -> new StairBlock(ALIEN_HAZARD_PLASTIC.get().defaultBlockState(), HumanBlockProperties.PLASTIC.build())
    );

    public static final BLibHolder<Block> FIRE_HAZARD_PLASTIC_STAIRS = create(
        "fire_hazard_plastic_stairs",
        () -> new StairBlock(FIRE_HAZARD_PLASTIC.get().defaultBlockState(), HumanBlockProperties.PLASTIC.build())
    );

    public static final BLibHolder<Block> HAZARD_PLASTIC_STAIRS = create(
        "hazard_plastic_stairs",
        () -> new StairBlock(HAZARD_PLASTIC.get().defaultBlockState(), HumanBlockProperties.PLASTIC.build())
    );

    public static final BLibHolder<Block> MACHINE_HAZARD_PLASTIC_STAIRS = create(
        "machine_hazard_plastic_stairs",
        () -> new StairBlock(MACHINE_HAZARD_PLASTIC.get().defaultBlockState(), HumanBlockProperties.PLASTIC.build())
    );

    public static final BLibHolder<Block> RADIATION_HAZARD_PLASTIC_STAIRS = create(
        "radiation_hazard_plastic_stairs",
        () -> new StairBlock(RADIATION_HAZARD_PLASTIC.get().defaultBlockState(), HumanBlockProperties.PLASTIC.build())
    );

    public static final BLibHolder<Block> SAFETY_PLASTIC_STAIRS = create(
        "safety_plastic_stairs",
        () -> new StairBlock(SAFETY_PLASTIC.get().defaultBlockState(), HumanBlockProperties.PLASTIC.build())
    );

    public static final BLibHolder<Block> TRAFFIC_HAZARD_PLASTIC_STAIRS = create(
        "traffic_hazard_plastic_stairs",
        () -> new StairBlock(TRAFFIC_HAZARD_PLASTIC.get().defaultBlockState(), HumanBlockProperties.PLASTIC.build())
    );

    public static final BLibHolder<Block> AISLE_HAZARD_PLASTIC_SLAB = create(
        "aisle_hazard_plastic_slab",
        () -> new SlabBlock(HumanBlockProperties.PLASTIC.build())
    );

    public static final BLibHolder<Block> ALIEN_HAZARD_PLASTIC_SLAB = create(
        "alien_hazard_plastic_slab",
        () -> new SlabBlock(HumanBlockProperties.PLASTIC.build())
    );

    public static final BLibHolder<Block> FIRE_HAZARD_PLASTIC_SLAB = create(
        "fire_hazard_plastic_slab",
        () -> new SlabBlock(HumanBlockProperties.PLASTIC.build())
    );

    public static final BLibHolder<Block> HAZARD_PLASTIC_SLAB = create(
        "hazard_plastic_slab",
        () -> new SlabBlock(HumanBlockProperties.PLASTIC.build())
    );

    public static final BLibHolder<Block> MACHINE_HAZARD_PLASTIC_SLAB = create(
        "machine_hazard_plastic_slab",
        () -> new SlabBlock(HumanBlockProperties.PLASTIC.build())
    );

    public static final BLibHolder<Block> RADIATION_HAZARD_PLASTIC_SLAB = create(
        "radiation_hazard_plastic_slab",
        () -> new SlabBlock(HumanBlockProperties.PLASTIC.build())
    );

    public static final BLibHolder<Block> SAFETY_PLASTIC_SLAB = create(
        "safety_plastic_slab",
        () -> new SlabBlock(HumanBlockProperties.PLASTIC.build())
    );

    public static final BLibHolder<Block> TRAFFIC_HAZARD_PLASTIC_SLAB = create(
        "traffic_hazard_plastic_slab",
        () -> new SlabBlock(HumanBlockProperties.PLASTIC.build())
    );

    // Hazard/Safety plastic walls
    public static final BLibHolder<Block> AISLE_HAZARD_PLASTIC_WALL = create(
        "aisle_hazard_plastic_wall",
        () -> new WallBlock(HumanBlockProperties.PLASTIC.build())
    );

    public static final BLibHolder<Block> ALIEN_HAZARD_PLASTIC_WALL = create(
        "alien_hazard_plastic_wall",
        () -> new WallBlock(HumanBlockProperties.PLASTIC.build())
    );

    public static final BLibHolder<Block> FIRE_HAZARD_PLASTIC_WALL = create(
        "fire_hazard_plastic_wall",
        () -> new WallBlock(HumanBlockProperties.PLASTIC.build())
    );

    public static final BLibHolder<Block> HAZARD_PLASTIC_WALL = create(
        "hazard_plastic_wall",
        () -> new WallBlock(HumanBlockProperties.PLASTIC.build())
    );

    public static final BLibHolder<Block> MACHINE_HAZARD_PLASTIC_WALL = create(
        "machine_hazard_plastic_wall",
        () -> new WallBlock(HumanBlockProperties.PLASTIC.build())
    );

    public static final BLibHolder<Block> RADIATION_HAZARD_PLASTIC_WALL = create(
        "radiation_hazard_plastic_wall",
        () -> new WallBlock(HumanBlockProperties.PLASTIC.build())
    );

    public static final BLibHolder<Block> SAFETY_PLASTIC_WALL = create(
        "safety_plastic_wall",
        () -> new WallBlock(HumanBlockProperties.PLASTIC.build())
    );

    public static final BLibHolder<Block> TRAFFIC_HAZARD_PLASTIC_WALL = create(
        "traffic_hazard_plastic_wall",
        () -> new WallBlock(HumanBlockProperties.PLASTIC.build())
    );

    public static final Map<DyeColor, BLibHolder<Block>> DYE_COLOR_TO_CUT_PLASTIC =
        Collections.unmodifiableMap(
            Arrays.stream(DyeColor.values())
                .collect(
                    Collectors.toMap(
                        Function.identity(),
                        dyeColor -> create(
                            dyeColor.getName() + "_cut_plastic",
                            () -> new Block(HumanBlockProperties.DYE_COLOR_TO_PLASTIC_PROPERTIES.get(dyeColor).build())
                        ),
                        (a, b) -> b,
                        LinkedHashMap::new
                    )
                )
        );

    public static final Map<DyeColor, BLibHolder<Block>> DYE_COLOR_TO_CUT_PLASTIC_STAIRS =
        Collections.unmodifiableMap(
            Arrays.stream(DyeColor.values())
                .collect(
                    Collectors.toMap(
                        Function.identity(),
                        dyeColor -> create(
                            dyeColor.getName() + "_cut_plastic_stairs",
                            () -> new StairBlock(
                                DYE_COLOR_TO_CUT_PLASTIC.get(dyeColor).get().defaultBlockState(),
                                HumanBlockProperties.DYE_COLOR_TO_PLASTIC_PROPERTIES.get(dyeColor).build()
                            )
                        ),
                        (a, b) -> b,
                        LinkedHashMap::new
                    )
                )
        );

    public static final Map<DyeColor, BLibHolder<Block>> DYE_COLOR_TO_CUT_PLASTIC_SLAB =
        Collections.unmodifiableMap(
            Arrays.stream(DyeColor.values())
                .collect(
                    Collectors.toMap(
                        Function.identity(),
                        dyeColor -> create(
                            dyeColor.getName() + "_cut_plastic_slab",
                            () -> new SlabBlock(HumanBlockProperties.DYE_COLOR_TO_PLASTIC_PROPERTIES.get(dyeColor).build())
                        ),
                        (a, b) -> b,
                        LinkedHashMap::new
                    )
                )
        );

    public static final Map<DyeColor, BLibHolder<Block>> DYE_COLOR_TO_FRAMED_PLASTIC =
        Collections.unmodifiableMap(
            Arrays.stream(DyeColor.values())
                .collect(
                    Collectors.toMap(
                        Function.identity(),
                        dyeColor -> create(
                            dyeColor.getName() + "_framed_plastic",
                            () -> new Block(HumanBlockProperties.DYE_COLOR_TO_PLASTIC_PROPERTIES.get(dyeColor).build())
                        ),
                        (a, b) -> b,
                        LinkedHashMap::new
                    )
                )
        );

    public static final Map<DyeColor, BLibHolder<Block>> DYE_COLOR_TO_PITTED_PLASTIC =
        Collections.unmodifiableMap(
            Arrays.stream(DyeColor.values())
                .collect(
                    Collectors.toMap(
                        Function.identity(),
                        dyeColor -> create(
                            dyeColor.getName() + "_pitted_plastic",
                            () -> new Block(HumanBlockProperties.DYE_COLOR_TO_PLASTIC_PROPERTIES.get(dyeColor).build())
                        ),
                        (a, b) -> b,
                        LinkedHashMap::new
                    )
                )
        );

    public static final Map<DyeColor, BLibHolder<Block>> DYE_COLOR_TO_PITTED_PLASTIC_STAIRS =
        Collections.unmodifiableMap(
            Arrays.stream(DyeColor.values())
                .collect(
                    Collectors.toMap(
                        Function.identity(),
                        dyeColor -> create(
                            dyeColor.getName() + "_pitted_plastic_stairs",
                            () -> new StairBlock(
                                DYE_COLOR_TO_PITTED_PLASTIC.get(dyeColor).get().defaultBlockState(),
                                HumanBlockProperties.DYE_COLOR_TO_PLASTIC_PROPERTIES.get(dyeColor).build()
                            )
                        ),
                        (a, b) -> b,
                        LinkedHashMap::new
                    )
                )
        );

    public static final Map<DyeColor, BLibHolder<Block>> DYE_COLOR_TO_PITTED_PLASTIC_SLAB =
        Collections.unmodifiableMap(
            Arrays.stream(DyeColor.values())
                .collect(
                    Collectors.toMap(
                        Function.identity(),
                        dyeColor -> create(
                            dyeColor.getName() + "_pitted_plastic_slab",
                            () -> new SlabBlock(HumanBlockProperties.DYE_COLOR_TO_PLASTIC_PROPERTIES.get(dyeColor).build())
                        ),
                        (a, b) -> b,
                        LinkedHashMap::new
                    )
                )
        );

    public static final Map<DyeColor, BLibHolder<Block>> DYE_COLOR_TO_PLASTIC =
        Collections.unmodifiableMap(
            Arrays.stream(DyeColor.values())
                .collect(
                    Collectors.toMap(
                        Function.identity(),
                        dyeColor -> create(
                            dyeColor.getName() + "_plastic",
                            () -> new Block(HumanBlockProperties.DYE_COLOR_TO_PLASTIC_PROPERTIES.get(dyeColor).build())
                        ),
                        (a, b) -> b,
                        LinkedHashMap::new
                    )
                )
        );

    public static final Map<DyeColor, BLibHolder<Block>> DYE_COLOR_TO_PLASTIC_WALL =
        Collections.unmodifiableMap(
            Arrays.stream(DyeColor.values())
                .collect(
                    Collectors.toMap(
                        Function.identity(),
                        dyeColor -> create(
                            dyeColor.getName() + "_plastic_wall",
                            () -> new WallBlock(HumanBlockProperties.DYE_COLOR_TO_PLASTIC_PROPERTIES.get(dyeColor).build())
                        ),
                        (a, b) -> b,
                        LinkedHashMap::new
                    )
                )
        );

    public static final Map<DyeColor, BLibHolder<Block>> DYE_COLOR_TO_PLASTIC_STAIRS =
        Collections.unmodifiableMap(
            Arrays.stream(DyeColor.values())
                .collect(
                    Collectors.toMap(
                        Function.identity(),
                        dyeColor -> create(
                            dyeColor.getName() + "_plastic_stairs",
                            () -> new StairBlock(
                                DYE_COLOR_TO_PLASTIC.get(dyeColor).get().defaultBlockState(),
                                HumanBlockProperties.DYE_COLOR_TO_PLASTIC_PROPERTIES.get(dyeColor).build()
                            )
                        ),
                        (a, b) -> b,
                        LinkedHashMap::new
                    )
                )
        );

    public static final Map<DyeColor, BLibHolder<Block>> DYE_COLOR_TO_PLASTIC_SLAB =
        Collections.unmodifiableMap(
            Arrays.stream(DyeColor.values())
                .collect(
                    Collectors.toMap(
                        Function.identity(),
                        dyeColor -> create(
                            dyeColor.getName() + "_plastic_slab",
                            () -> new SlabBlock(HumanBlockProperties.DYE_COLOR_TO_PLASTIC_PROPERTIES.get(dyeColor).build())
                        ),
                        (a, b) -> b,
                        LinkedHashMap::new
                    )
                )
        );

    public static final Map<DyeColor, BLibHolder<Block>> DYE_COLOR_TO_PLASTIC_GRATE =
        Collections.unmodifiableMap(
            Arrays.stream(DyeColor.values())
                .collect(
                    Collectors.toMap(
                        Function.identity(),
                        dyeColor -> create(
                            dyeColor.getName() + "_plastic_grate",
                            () -> new WaterloggedTransparentBlock(
                                HumanBlockProperties.DYE_COLOR_TO_PLASTIC_PROPERTIES.get(dyeColor).noOcclusion().build()
                            )
                        ),
                        (a, b) -> b,
                        LinkedHashMap::new
                    )
                )
        );

    public static final Map<DyeColor, BLibHolder<Block>> DYE_COLOR_TO_PLASTIC_GRATE_STAIRS =
        Collections.unmodifiableMap(
            Arrays.stream(DyeColor.values())
                .collect(
                    Collectors.toMap(
                        Function.identity(),
                        dyeColor -> create(
                            dyeColor.getName() + "_plastic_grate_stairs",
                            () -> new StairBlock(
                                DYE_COLOR_TO_PLASTIC_GRATE.get(dyeColor).get().defaultBlockState(),
                                HumanBlockProperties.DYE_COLOR_TO_PLASTIC_PROPERTIES.get(dyeColor).noOcclusion().build()
                            )
                        ),
                        (a, b) -> b,
                        LinkedHashMap::new
                    )
                )
        );

    public static final Map<DyeColor, BLibHolder<Block>> DYE_COLOR_TO_PLASTIC_GRATE_SLAB =
        Collections.unmodifiableMap(
            Arrays.stream(DyeColor.values())
                .collect(
                    Collectors.toMap(
                        Function.identity(),
                        dyeColor -> create(
                            dyeColor.getName() + "_plastic_grate_slab",
                            () -> new SlabBlock(HumanBlockProperties.DYE_COLOR_TO_PLASTIC_PROPERTIES.get(dyeColor).noOcclusion().build())
                        ),
                        (a, b) -> b,
                        LinkedHashMap::new
                    )
                )
        );

    private static BLibHolder<Block> create(String path, BlockPropertyBuilder blockPropertyBuilder) {
        return create(path, () -> new Block(blockPropertyBuilder.build()));
    }

    private static <T extends Block> BLibHolder<T> create(String path, Supplier<T> blockSupplier) {
        return REGISTRY.createHolder(path, blockSupplier);
    }

    public static void initialize() {
        REGISTRY.registerAll();
    }
}
