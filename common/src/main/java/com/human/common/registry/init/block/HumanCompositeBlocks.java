package com.human.common.registry.init.block;

import com.blib.api.common.block.v1.BlockPropertyBuilder;
import com.blib.api.common.registry.v1.BLibHolder;
import com.blib.api.common.registry.v1.BLibRegistry;
import com.human.Human;
import com.human.common.gameplay.block.HumanBlockSetTypes;
import com.human.common.gameplay.block.property.HumanBlockProperties;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.PressurePlateBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.WaterloggedTransparentBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockSetType;

import java.util.function.Supplier;

/**
 * [stated] Oct 3: COMPOSITE - steel and plastic combined (2 polymer and a steel ingot make a composite ingot; 9 make
 * the block). As strong as steel - it uses steel's own block properties - and acid resistant: it stands up to xenomorph
 * acid three times longer than steel (tagged avp_human:acid_resistant_3x for avp_alien to read). The same family as
 * steel, minus the bars.
 */
public class HumanCompositeBlocks {

    public static final BLibRegistry<Block> REGISTRY = Human.MOD.registries().create(BuiltInRegistries.BLOCK);

    public static final BLibHolder<Block> CHISELED_COMPOSITE = create("chiseled_composite", HumanBlockProperties.STEEL);

    public static final BLibHolder<Block> CUT_COMPOSITE = create("cut_composite", HumanBlockProperties.STEEL);

    public static final BLibHolder<Block> CUT_COMPOSITE_STAIRS = create(
        "cut_composite_stairs",
        () -> new StairBlock(
            CUT_COMPOSITE.get().defaultBlockState(),
            HumanBlockProperties.STEEL.build()
        )
    );

    public static final BLibHolder<Block> CUT_COMPOSITE_SLAB = create(
        "cut_composite_slab",
        () -> new SlabBlock(HumanBlockProperties.STEEL.build())
    );

    public static final BLibHolder<Block> COMPOSITE_BLOCK = create("composite_block", HumanBlockProperties.STEEL);

    public static final BLibHolder<Block> COMPOSITE_STAIRS = create(
        "composite_stairs",
        () -> new StairBlock(COMPOSITE_BLOCK.get().defaultBlockState(), BlockBehaviour.Properties.ofFullCopy(COMPOSITE_BLOCK.get()))
    );

    public static final BLibHolder<Block> COMPOSITE_SLAB = create(
        "composite_slab",
        () -> new SlabBlock(BlockBehaviour.Properties.ofFullCopy(COMPOSITE_BLOCK.get()))
    );

    public static final BLibHolder<Block> COMPOSITE_BUTTON = create(
        "composite_button",
        () -> new ButtonBlock(HumanBlockSetTypes.STEEL, 20, HumanBlockProperties.STEEL.build())
    );

    public static final BLibHolder<Block> COMPOSITE_CHAIN_FENCE = create(
        "composite_chain_fence",
        () -> new IronBarsBlock(HumanBlockProperties.STEEL_BARS.build().sound(SoundType.CHAIN))
    );

    public static final BLibHolder<Block> COMPOSITE_COLUMN = create(
        "composite_column",
        () -> new RotatedPillarBlock(HumanBlockProperties.STEEL.build())
    );

    public static final BLibHolder<Block> COMPOSITE_DOOR = create(
        "composite_door",
        () -> new DoorBlock(BlockSetType.COPPER, HumanBlockProperties.STEEL.build().noOcclusion())
    );

    public static final BLibHolder<Block> COMPOSITE_FASTENED_SIDING = create("composite_fastened_siding", HumanBlockProperties.STEEL);

    public static final BLibHolder<Block> COMPOSITE_FASTENED_SIDING_STAIRS = create(
        "composite_fastened_siding_stairs",
        () -> new StairBlock(COMPOSITE_FASTENED_SIDING.get().defaultBlockState(), HumanBlockProperties.STEEL.build())
    );

    public static final BLibHolder<Block> COMPOSITE_FASTENED_SIDING_SLAB = create(
        "composite_fastened_siding_slab",
        () -> new SlabBlock(HumanBlockProperties.STEEL.build())
    );

    public static final BLibHolder<Block> COMPOSITE_FASTENED_STANDING = create(
        "composite_fastened_standing",
        HumanBlockProperties.STEEL
    );

    public static final BLibHolder<Block> COMPOSITE_FASTENED_STANDING_STAIRS = create(
        "composite_fastened_standing_stairs",
        () -> new StairBlock(COMPOSITE_FASTENED_STANDING.get().defaultBlockState(), HumanBlockProperties.STEEL.build())
    );

    public static final BLibHolder<Block> COMPOSITE_FASTENED_STANDING_SLAB = create(
        "composite_fastened_standing_slab",
        () -> new SlabBlock(HumanBlockProperties.STEEL.build())
    );

    public static final BLibHolder<Block> COMPOSITE_GRATE = create(
        "composite_grate",
        () -> new WaterloggedTransparentBlock(HumanBlockProperties.STEEL_GRATE.build())
    );

    public static final BLibHolder<Block> COMPOSITE_GRATE_STAIRS = create(
        "composite_grate_stairs",
        () -> new StairBlock(COMPOSITE_GRATE.get().defaultBlockState(), HumanBlockProperties.STEEL.build().noOcclusion())
    );

    public static final BLibHolder<Block> COMPOSITE_GRATE_SLAB = create(
        "composite_grate_slab",
        () -> new SlabBlock(HumanBlockProperties.STEEL.build().noOcclusion())
    );

    public static final BLibHolder<Block> COMPOSITE_PLATING = create("composite_plating", HumanBlockProperties.STEEL);

    public static final BLibHolder<Block> COMPOSITE_PLATING_STAIRS = create(
        "composite_plating_stairs",
        () -> new StairBlock(COMPOSITE_PLATING.get().defaultBlockState(), HumanBlockProperties.STEEL.build())
    );

    public static final BLibHolder<Block> COMPOSITE_PLATING_SLAB = create(
        "composite_plating_slab",
        () -> new SlabBlock(HumanBlockProperties.STEEL.build())
    );

    public static final BLibHolder<Block> COMPOSITE_PRESSURE_PLATE = create(
        "composite_pressure_plate",
        () -> new PressurePlateBlock(BlockSetType.COPPER, HumanBlockProperties.STEEL.build())
    );

    public static final BLibHolder<Block> COMPOSITE_SIDING = create("composite_siding", HumanBlockProperties.STEEL);

    public static final BLibHolder<Block> COMPOSITE_SIDING_STAIRS = create(
        "composite_siding_stairs",
        () -> new StairBlock(COMPOSITE_SIDING.get().defaultBlockState(), HumanBlockProperties.STEEL.build())
    );

    public static final BLibHolder<Block> COMPOSITE_SIDING_SLAB = create(
        "composite_siding_slab",
        () -> new SlabBlock(HumanBlockProperties.STEEL.build())
    );

    public static final BLibHolder<Block> COMPOSITE_STANDING = create("composite_standing", HumanBlockProperties.STEEL);

    public static final BLibHolder<Block> COMPOSITE_STANDING_STAIRS = create(
        "composite_standing_stairs",
        () -> new StairBlock(COMPOSITE_STANDING.get().defaultBlockState(), HumanBlockProperties.STEEL.build())
    );

    public static final BLibHolder<Block> COMPOSITE_STANDING_SLAB = create(
        "composite_standing_slab",
        () -> new SlabBlock(HumanBlockProperties.STEEL.build())
    );

    public static final BLibHolder<Block> COMPOSITE_TRAP_DOOR = create(
        "composite_trapdoor",
        () -> new TrapDoorBlock(BlockSetType.COPPER, HumanBlockProperties.STEEL.build())
    );

    public static final BLibHolder<Block> COMPOSITE_TREAD = create("composite_tread", HumanBlockProperties.STEEL);

    public static final BLibHolder<Block> COMPOSITE_TREAD_STAIRS = create(
        "composite_tread_stairs",
        () -> new StairBlock(COMPOSITE_TREAD.get().defaultBlockState(), HumanBlockProperties.STEEL.build())
    );

    public static final BLibHolder<Block> COMPOSITE_TREAD_SLAB = create(
        "composite_tread_slab",
        () -> new SlabBlock(HumanBlockProperties.STEEL.build())
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
