package com.human.fabric.data.model;

import com.blib.fabric.data.model.BLibModelTemplates;
import com.blib.fabric.data.model.generator.BarsGenerator;
import com.human.common.gameplay.block.property.HumanBlockProperties;
import com.human.common.registry.init.HumanBlocks;
import com.human.common.registry.init.block.CoreBlocks;
import com.human.common.registry.init.block.HumanFerroaluminumBlocks;
import com.human.common.registry.init.block.HumanIndustrialConcreteBlocks;
import com.human.common.registry.init.block.HumanIndustrialGlassBlocks;
import com.human.common.registry.init.block.HumanPaddingBlocks;
import com.human.common.registry.init.block.HumanPlasticBlocks;
import com.human.common.registry.init.block.HumanSteelBlocks;
import com.human.common.registry.init.block.HumanTitaniumBlocks;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricModelProvider;
import net.minecraft.data.models.BlockModelGenerators;
import net.minecraft.data.models.ItemModelGenerators;
import net.minecraft.data.models.blockstates.Condition;
import net.minecraft.data.models.blockstates.MultiPartGenerator;
import net.minecraft.data.models.blockstates.Variant;
import net.minecraft.data.models.blockstates.VariantProperties;
import net.minecraft.data.models.model.ModelLocationUtils;
import net.minecraft.data.models.model.ModelTemplates;
import net.minecraft.data.models.model.TextureMapping;
import net.minecraft.data.models.model.TextureSlot;
import net.minecraft.data.models.model.TexturedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.jetbrains.annotations.NotNull;

public class BlockModelProvider extends FabricModelProvider {

    public BlockModelProvider(FabricDataOutput output) {
        super(output);
    }

    @Override
    public void generateBlockStateModels(BlockModelGenerators generators) {
        HumanBlockProperties.DYE_COLOR_TO_CONCRETE_BLOCKS.forEach(
            (dyeColor, block) -> {
                createSlab(generators, block, HumanBlocks.DYE_COLOR_TO_CONCRETE_SLAB.get(dyeColor).get());
                createStairs(generators, block, HumanBlocks.DYE_COLOR_TO_CONCRETE_STAIRS.get(dyeColor).get());
            }
        );

        HumanIndustrialConcreteBlocks.DYE_COLOR_TO_INDUSTRIAL_CONCRETE.forEach(
            (dyeColor, blockSupplier) -> {
                var block = blockSupplier.get();
                var topResourceLocation = TextureMapping.getBlockTexture(block, "_top");

                var baseTextureMapping = TextureMapping.cube(block)
                    .put(TextureSlot.END, topResourceLocation);

                generators.createTrivialBlock(block, baseTextureMapping, ModelTemplates.CUBE_COLUMN);

                createSlab(generators, block, HumanIndustrialConcreteBlocks.DYE_COLOR_TO_INDUSTRIAL_CONCRETE_SLAB.get(dyeColor).get());

                createStairs(generators, block, HumanIndustrialConcreteBlocks.DYE_COLOR_TO_INDUSTRIAL_CONCRETE_STAIRS.get(dyeColor).get());

                createWallCustomTop(
                    generators,
                    block,
                    HumanIndustrialConcreteBlocks.DYE_COLOR_TO_INDUSTRIAL_CONCRETE_WALL.get(dyeColor).get(),
                    topResourceLocation
                );
            }
        );

        createIndustrialGlassSlab(generators);
        generators.family(HumanIndustrialGlassBlocks.INDUSTRIAL_GLASS.get())
            .door(HumanIndustrialGlassBlocks.INDUSTRIAL_GLASS_DOOR.get())
            .stairs(HumanIndustrialGlassBlocks.INDUSTRIAL_GLASS_STAIRS.get())
            .trapdoor(HumanIndustrialGlassBlocks.INDUSTRIAL_GLASS_TRAP_DOOR.get());
        // ⚠ NO createTrivialCube HERE. generators.family(block) calls fullBlock() internally, which emits the block's
        // own CUBE_ALL model and blockstate - so generating the cube separately as well is a DUPLICATE MODEL and
        // datagen dies on it. The uncoloured glass has always relied on family() for the same reason.
        HumanIndustrialGlassBlocks.DYE_COLOR_TO_INDUSTRIAL_GLASS.forEach((dyeColor, blockSupplier) -> {
            createIndustrialGlassSlab(
                generators,
                blockSupplier.get(),
                HumanIndustrialGlassBlocks.DYE_COLOR_TO_INDUSTRIAL_GLASS_SLAB.get(dyeColor).get()
            );
            generators.family(blockSupplier.get())
                .door(HumanIndustrialGlassBlocks.DYE_COLOR_TO_INDUSTRIAL_GLASS_DOOR.get(dyeColor).get())
                .stairs(HumanIndustrialGlassBlocks.DYE_COLOR_TO_INDUSTRIAL_GLASS_STAIRS.get(dyeColor).get())
                .trapdoor(HumanIndustrialGlassBlocks.DYE_COLOR_TO_INDUSTRIAL_GLASS_TRAP_DOOR.get(dyeColor).get());
            createWall(
                generators,
                blockSupplier.get(),
                HumanIndustrialGlassBlocks.DYE_COLOR_TO_INDUSTRIAL_GLASS_WALL.get(dyeColor).get()
            );
        });
        createWall(
            generators,
            HumanIndustrialGlassBlocks.INDUSTRIAL_GLASS.get(),
            HumanIndustrialGlassBlocks.INDUSTRIAL_GLASS_WALL.get()
        );
        createGlassBlocks(
            generators,
            HumanIndustrialGlassBlocks.INDUSTRIAL_GLASS.get(),
            HumanIndustrialGlassBlocks.INDUSTRIAL_GLASS_PANE.get()
        );
        HumanIndustrialGlassBlocks.DYE_COLOR_TO_INDUSTRIAL_GLASS_PANE.forEach(
            (dyeColor, blockSupplier) -> createGlassBlocks(
                generators,
                HumanIndustrialGlassBlocks.DYE_COLOR_TO_INDUSTRIAL_GLASS.get(dyeColor).get(),
                blockSupplier.get()
            )
        );

        HumanPaddingBlocks.DYE_COLOR_TO_PADDING.forEach(
            (dyeColor, blockSupplier) -> {
                var block = blockSupplier.get();
                var slabBlock = HumanPaddingBlocks.DYE_COLOR_TO_PADDING_SLAB.get(dyeColor).get();
                var stairBlock = HumanPaddingBlocks.DYE_COLOR_TO_PADDING_STAIRS.get(dyeColor).get();

                generators.family(block)
                    .slab(slabBlock)
                    .stairs(stairBlock);
            }
        );

        HumanPaddingBlocks.DYE_COLOR_TO_PANEL_PADDING.forEach(
            (dyeColor, blockSupplier) -> {
                var block = blockSupplier.get();
                var slabBlock = HumanPaddingBlocks.DYE_COLOR_TO_PANEL_PADDING_SLAB.get(dyeColor).get();
                var stairBlock = HumanPaddingBlocks.DYE_COLOR_TO_PANEL_PADDING_STAIRS.get(dyeColor).get();

                generators.family(block)
                    .slab(slabBlock)
                    .stairs(stairBlock);
            }
        );

        HumanPaddingBlocks.DYE_COLOR_TO_PIPE_PADDING.forEach(
            (dyeColor, blockSupplier) -> {
                var block = blockSupplier.get();
                createRotatedPillar(generators, block, TexturedModel.CUBE);

                createSlab(generators, block, HumanPaddingBlocks.DYE_COLOR_TO_PIPE_PADDING_SLAB.get(dyeColor).get());
                createStairs(generators, block, HumanPaddingBlocks.DYE_COLOR_TO_PIPE_PADDING_STAIRS.get(dyeColor).get());
            }
        );

        HumanPaddingBlocks.DYE_COLOR_TO_TILE_PADDING.forEach(
            (dyeColor, blockSupplier) -> {
                var block = blockSupplier.get();
                var slabBlock = HumanPaddingBlocks.DYE_COLOR_TO_TILE_PADDING_SLAB.get(dyeColor).get();
                var stairBlock = HumanPaddingBlocks.DYE_COLOR_TO_TILE_PADDING_STAIRS.get(dyeColor).get();

                generators.family(block)
                    .slab(slabBlock)
                    .stairs(stairBlock);
            }
        );

        HumanPlasticBlocks.DYE_COLOR_TO_CUT_PLASTIC.forEach(
            (dyeColor, blockSupplier) -> {
                var block = blockSupplier.get();
                var slabBlock = HumanPlasticBlocks.DYE_COLOR_TO_CUT_PLASTIC_SLAB.get(dyeColor).get();
                var stairBlock = HumanPlasticBlocks.DYE_COLOR_TO_CUT_PLASTIC_STAIRS.get(dyeColor).get();

                generators.family(block)
                    .slab(slabBlock)
                    .stairs(stairBlock);
            }
        );

        HumanPlasticBlocks.DYE_COLOR_TO_FRAMED_PLASTIC.forEach((dyeColor, blockSupplier) -> generators.family(blockSupplier.get()));

        HumanPlasticBlocks.DYE_COLOR_TO_PITTED_PLASTIC.forEach(
            (dyeColor, blockSupplier) -> {
                var block = blockSupplier.get();
                var slabBlock = HumanPlasticBlocks.DYE_COLOR_TO_PITTED_PLASTIC_SLAB.get(dyeColor).get();
                var stairBlock = HumanPlasticBlocks.DYE_COLOR_TO_PITTED_PLASTIC_STAIRS.get(dyeColor).get();

                generators.family(block)
                    .slab(slabBlock)
                    .stairs(stairBlock);
            }
        );

        HumanPlasticBlocks.DYE_COLOR_TO_PLASTIC.forEach(
            (dyeColor, blockSupplier) -> {
                var block = blockSupplier.get();
                var slabBlock = HumanPlasticBlocks.DYE_COLOR_TO_PLASTIC_SLAB.get(dyeColor).get();
                var stairBlock = HumanPlasticBlocks.DYE_COLOR_TO_PLASTIC_STAIRS.get(dyeColor).get();

                generators.family(block)
                    .slab(slabBlock)
                    .stairs(stairBlock);
                createWall(generators, block, HumanPlasticBlocks.DYE_COLOR_TO_PLASTIC_WALL.get(dyeColor).get());
            }
        );

        HumanPlasticBlocks.DYE_COLOR_TO_PLASTIC_GRATE.forEach(
            (dyeColor, blockSupplier) -> {
                var block = blockSupplier.get();
                var slabBlock = HumanPlasticBlocks.DYE_COLOR_TO_PLASTIC_GRATE_SLAB.get(dyeColor).get();
                var stairBlock = HumanPlasticBlocks.DYE_COLOR_TO_PLASTIC_GRATE_STAIRS.get(dyeColor).get();

                generators.family(block)
                    .slab(slabBlock)
                    .stairs(stairBlock);
            }
        );

        generators.family(HumanPlasticBlocks.AISLE_HAZARD_PLASTIC.get())
            .slab(HumanPlasticBlocks.AISLE_HAZARD_PLASTIC_SLAB.get())
            .stairs(HumanPlasticBlocks.AISLE_HAZARD_PLASTIC_STAIRS.get())
            .wall(HumanPlasticBlocks.AISLE_HAZARD_PLASTIC_WALL.get());

        generators.family(HumanPlasticBlocks.ALIEN_HAZARD_PLASTIC.get())
            .slab(HumanPlasticBlocks.ALIEN_HAZARD_PLASTIC_SLAB.get())
            .stairs(HumanPlasticBlocks.ALIEN_HAZARD_PLASTIC_STAIRS.get())
            .wall(HumanPlasticBlocks.ALIEN_HAZARD_PLASTIC_WALL.get());

        generators.family(HumanPlasticBlocks.FIRE_HAZARD_PLASTIC.get())
            .slab(HumanPlasticBlocks.FIRE_HAZARD_PLASTIC_SLAB.get())
            .stairs(HumanPlasticBlocks.FIRE_HAZARD_PLASTIC_STAIRS.get())
            .wall(HumanPlasticBlocks.FIRE_HAZARD_PLASTIC_WALL.get());

        generators.family(HumanPlasticBlocks.HAZARD_PLASTIC.get())
            .slab(HumanPlasticBlocks.HAZARD_PLASTIC_SLAB.get())
            .stairs(HumanPlasticBlocks.HAZARD_PLASTIC_STAIRS.get())
            .wall(HumanPlasticBlocks.HAZARD_PLASTIC_WALL.get());

        generators.family(HumanPlasticBlocks.MACHINE_HAZARD_PLASTIC.get())
            .slab(HumanPlasticBlocks.MACHINE_HAZARD_PLASTIC_SLAB.get())
            .stairs(HumanPlasticBlocks.MACHINE_HAZARD_PLASTIC_STAIRS.get())
            .wall(HumanPlasticBlocks.MACHINE_HAZARD_PLASTIC_WALL.get());

        generators.family(HumanPlasticBlocks.RADIATION_HAZARD_PLASTIC.get())
            .slab(HumanPlasticBlocks.RADIATION_HAZARD_PLASTIC_SLAB.get())
            .stairs(HumanPlasticBlocks.RADIATION_HAZARD_PLASTIC_STAIRS.get())
            .wall(HumanPlasticBlocks.RADIATION_HAZARD_PLASTIC_WALL.get());

        generators.family(HumanPlasticBlocks.SAFETY_PLASTIC.get())
            .slab(HumanPlasticBlocks.SAFETY_PLASTIC_SLAB.get())
            .stairs(HumanPlasticBlocks.SAFETY_PLASTIC_STAIRS.get())
            .wall(HumanPlasticBlocks.SAFETY_PLASTIC_WALL.get());

        generators.family(HumanPlasticBlocks.TRAFFIC_HAZARD_PLASTIC.get())
            .slab(HumanPlasticBlocks.TRAFFIC_HAZARD_PLASTIC_SLAB.get())
            .stairs(HumanPlasticBlocks.TRAFFIC_HAZARD_PLASTIC_STAIRS.get())
            .wall(HumanPlasticBlocks.TRAFFIC_HAZARD_PLASTIC_WALL.get());

        generators.createTrivialCube(CoreBlocks.ALUMINUM_BLOCK.get());
        generators.createTrivialCube(CoreBlocks.TRINITITE_BLOCK.get());
        generators.createTrivialCube(CoreBlocks.AUTUNITE_BLOCK.get());
        generators.createTrivialCube(CoreBlocks.AUTUNITE_ORE.get());
        generators.createTrivialCube(CoreBlocks.BAUXITE_ORE.get());
        generators.createTrivialCube(CoreBlocks.BRASS_BLOCK.get());
        generators.createTrivialCube(HumanFerroaluminumBlocks.CHISELED_FERROALUMINUM.get());
        generators.createTrivialCube(HumanSteelBlocks.CHISELED_STEEL.get());
        generators.createTrivialCube(HumanTitaniumBlocks.CHISELED_TITANIUM.get());

        generators.family(HumanFerroaluminumBlocks.CUT_FERROALUMINUM.get())
            .slab(HumanFerroaluminumBlocks.CUT_FERROALUMINUM_SLAB.get())
            .stairs(HumanFerroaluminumBlocks.CUT_FERROALUMINUM_STAIRS.get());

        generators.family(HumanSteelBlocks.CUT_STEEL.get())
            .slab(HumanSteelBlocks.CUT_STEEL_SLAB.get())
            .stairs(HumanSteelBlocks.CUT_STEEL_STAIRS.get());

        generators.family(HumanTitaniumBlocks.CUT_TITANIUM.get())
            .slab(HumanTitaniumBlocks.CUT_TITANIUM_SLAB.get())
            .stairs(HumanTitaniumBlocks.CUT_TITANIUM_STAIRS.get());

        generators.createTrivialCube(CoreBlocks.DEEPSLATE_TITANIUM_ORE.get());
        generators.createTrivialCube(CoreBlocks.DEEPSLATE_ZINC_ORE.get());
        BarsGenerator.generate(generators, HumanFerroaluminumBlocks.FERROALUMINUM_CHAIN_FENCE.get());
        generators.createRotatedPillarWithHorizontalVariant(
            HumanFerroaluminumBlocks.FERROALUMINUM_COLUMN.get(),
            TexturedModel.COLUMN_ALT,
            TexturedModel.COLUMN_HORIZONTAL_ALT
        );
        // generators.createTrivialCube(HumanBlocks.FERROALUMINUM_FASTENED_SIDING);
        // generators.createTrivialCube(HumanBlocks.FERROALUMINUM_FASTENED_STANDING);
        // generators.createTrivialCube(HumanBlocks.FERROALUMINUM_GRATE);
        // generators.createTrivialCube(HumanBlocks.FERROALUMINUM_PLATING);
        // generators.createTrivialCube(HumanBlocks.FERROALUMINUM_SIDING);
        // generators.createTrivialCube(HumanBlocks.FERROALUMINUM_STANDING);
        // generators.createTrivialCube(HumanBlocks.FERROALUMINUM_TREAD);
        generators.createTrivialCube(CoreBlocks.GALENA_ORE.get());
        generators.createTrivialCube(CoreBlocks.LEAD_BLOCK.get());
        generators.createTrivialCube(CoreBlocks.LITHIUM_BLOCK.get());
        generators.createTrivialCube(CoreBlocks.LITHIUM_ORE.get());
        generators.createTrivialCube(CoreBlocks.MONAZITE_ORE.get());
        generators.createTrivialCube(CoreBlocks.RAW_BAUXITE_BLOCK.get());
        generators.createTrivialCube(CoreBlocks.RAW_GALENA_BLOCK.get());
        generators.createTrivialCube(CoreBlocks.RAW_MONAZITE_BLOCK.get());
        generators.createTrivialCube(CoreBlocks.RAW_TITANIUM_BLOCK.get());
        generators.createTrivialCube(CoreBlocks.RAW_ZINC_BLOCK.get());
        generators.createCrossBlock(HumanBlocks.RAZOR_WIRE.get(), BlockModelGenerators.TintState.NOT_TINTED);
        generators.createTrivialCube(CoreBlocks.SILICA_GRAVEL.get());
        generators.createTrivialCube(CoreBlocks.SILICON_BLOCK.get());
        BarsGenerator.generate(generators, HumanSteelBlocks.STEEL_BARS.get());
        BarsGenerator.generate(generators, HumanSteelBlocks.STEEL_CHAIN_FENCE.get());
        generators.createRotatedPillarWithHorizontalVariant(
            HumanSteelBlocks.STEEL_COLUMN.get(),
            TexturedModel.COLUMN_ALT,
            TexturedModel.COLUMN_HORIZONTAL_ALT
        );
        // generators.createTrivialCube(HumanBlocks.STEEL_FASTENED_SIDING);
        // generators.createTrivialCube(HumanBlocks.STEEL_FASTENED_STANDING);
        // generators.createTrivialCube(HumanBlocks.STEEL_GRATE);
        // generators.createTrivialCube(HumanBlocks.STEEL_PLATING);
        // generators.createTrivialCube(HumanBlocks.STEEL_SIDING);
        // generators.createTrivialCube(HumanBlocks.STEEL_STANDING);
        // generators.createTrivialCube(HumanBlocks.STEEL_TREAD);
        BarsGenerator.generate(generators, HumanTitaniumBlocks.TITANIUM_CHAIN_FENCE.get());
        generators.createRotatedPillarWithHorizontalVariant(
            HumanTitaniumBlocks.TITANIUM_COLUMN.get(),
            TexturedModel.COLUMN_ALT,
            TexturedModel.COLUMN_HORIZONTAL_ALT
        );
        // generators.createTrivialCube(HumanBlocks.TITANIUM_FASTENED_SIDING);
        // generators.createTrivialCube(HumanBlocks.TITANIUM_FASTENED_STANDING);
        // generators.createTrivialCube(HumanBlocks.TITANIUM_GRATE);
        // generators.createTrivialCube(HumanBlocks.TITANIUM_PLATING);
        // generators.createTrivialCube(HumanBlocks.TITANIUM_SIDING);
        // generators.createTrivialCube(HumanBlocks.TITANIUM_STANDING);
        // generators.createTrivialCube(HumanBlocks.TITANIUM_TREAD);
        generators.createTrivialCube(CoreBlocks.URANIUM_BLOCK.get());
        generators.createTrivialCube(CoreBlocks.ZINC_BLOCK.get());
        generators.createTrivialCube(CoreBlocks.ZINC_ORE.get());

        generators.family(HumanFerroaluminumBlocks.FERROALUMINUM_BLOCK.get())
            .slab(HumanFerroaluminumBlocks.FERROALUMINUM_SLAB.get())
            .stairs(HumanFerroaluminumBlocks.FERROALUMINUM_STAIRS.get())
            .pressurePlate(HumanFerroaluminumBlocks.FERROALUMINUM_PRESSURE_PLATE.get())
            .button(HumanFerroaluminumBlocks.FERROALUMINUM_BUTTON.get())
            .door(HumanFerroaluminumBlocks.FERROALUMINUM_DOOR.get())
            .trapdoor(HumanFerroaluminumBlocks.FERROALUMINUM_TRAP_DOOR.get());

        generators.family(HumanSteelBlocks.STEEL_BLOCK.get())
            .slab(HumanSteelBlocks.STEEL_SLAB.get())
            .stairs(HumanSteelBlocks.STEEL_STAIRS.get())
            .pressurePlate(HumanSteelBlocks.STEEL_PRESSURE_PLATE.get())
            .button(HumanSteelBlocks.STEEL_BUTTON.get())
            .door(HumanSteelBlocks.STEEL_DOOR.get())
            .trapdoor(HumanSteelBlocks.STEEL_TRAP_DOOR.get());
        generators.family(HumanTitaniumBlocks.TITANIUM_BLOCK.get())
            .slab(HumanTitaniumBlocks.TITANIUM_SLAB.get())
            .stairs(HumanTitaniumBlocks.TITANIUM_STAIRS.get())
            .pressurePlate(HumanTitaniumBlocks.TITANIUM_PRESSURE_PLATE.get())
            .button(HumanTitaniumBlocks.TITANIUM_BUTTON.get())
            .door(HumanTitaniumBlocks.TITANIUM_DOOR.get())
            .trapdoor(HumanTitaniumBlocks.TITANIUM_TRAP_DOOR.get());

        generators.family(HumanFerroaluminumBlocks.FERROALUMINUM_SIDING.get())
            .slab(HumanFerroaluminumBlocks.FERROALUMINUM_SIDING_SLAB.get())
            .stairs(HumanFerroaluminumBlocks.FERROALUMINUM_SIDING_STAIRS.get());

        generators.family(HumanSteelBlocks.STEEL_SIDING.get())
            .slab(HumanSteelBlocks.STEEL_SIDING_SLAB.get())
            .stairs(HumanSteelBlocks.STEEL_SIDING_STAIRS.get());

        generators.family(HumanTitaniumBlocks.TITANIUM_SIDING.get())
            .slab(HumanTitaniumBlocks.TITANIUM_SIDING_SLAB.get())
            .stairs(HumanTitaniumBlocks.TITANIUM_SIDING_STAIRS.get());

        generators.family(HumanFerroaluminumBlocks.FERROALUMINUM_STANDING.get())
            .slab(HumanFerroaluminumBlocks.FERROALUMINUM_STANDING_SLAB.get())
            .stairs(HumanFerroaluminumBlocks.FERROALUMINUM_STANDING_STAIRS.get());

        generators.family(HumanSteelBlocks.STEEL_STANDING.get())
            .slab(HumanSteelBlocks.STEEL_STANDING_SLAB.get())
            .stairs(HumanSteelBlocks.STEEL_STANDING_STAIRS.get());

        generators.family(HumanTitaniumBlocks.TITANIUM_STANDING.get())
            .slab(HumanTitaniumBlocks.TITANIUM_STANDING_SLAB.get())
            .stairs(HumanTitaniumBlocks.TITANIUM_STANDING_STAIRS.get());

        generators.family(HumanFerroaluminumBlocks.FERROALUMINUM_FASTENED_SIDING.get())
            .slab(HumanFerroaluminumBlocks.FERROALUMINUM_FASTENED_SIDING_SLAB.get())
            .stairs(HumanFerroaluminumBlocks.FERROALUMINUM_FASTENED_SIDING_STAIRS.get());

        generators.family(HumanSteelBlocks.STEEL_FASTENED_SIDING.get())
            .slab(HumanSteelBlocks.STEEL_FASTENED_SIDING_SLAB.get())
            .stairs(HumanSteelBlocks.STEEL_FASTENED_SIDING_STAIRS.get());

        generators.family(HumanTitaniumBlocks.TITANIUM_FASTENED_SIDING.get())
            .slab(HumanTitaniumBlocks.TITANIUM_FASTENED_SIDING_SLAB.get())
            .stairs(HumanTitaniumBlocks.TITANIUM_FASTENED_SIDING_STAIRS.get());

        generators.family(HumanFerroaluminumBlocks.FERROALUMINUM_FASTENED_STANDING.get())
            .slab(HumanFerroaluminumBlocks.FERROALUMINUM_FASTENED_STANDING_SLAB.get())
            .stairs(HumanFerroaluminumBlocks.FERROALUMINUM_FASTENED_STANDING_STAIRS.get());

        generators.family(HumanSteelBlocks.STEEL_FASTENED_STANDING.get())
            .slab(HumanSteelBlocks.STEEL_FASTENED_STANDING_SLAB.get())
            .stairs(HumanSteelBlocks.STEEL_FASTENED_STANDING_STAIRS.get());

        generators.family(HumanTitaniumBlocks.TITANIUM_FASTENED_STANDING.get())
            .slab(HumanTitaniumBlocks.TITANIUM_FASTENED_STANDING_SLAB.get())
            .stairs(HumanTitaniumBlocks.TITANIUM_FASTENED_STANDING_STAIRS.get());

        generators.family(HumanFerroaluminumBlocks.FERROALUMINUM_PLATING.get())
            .slab(HumanFerroaluminumBlocks.FERROALUMINUM_PLATING_SLAB.get())
            .stairs(HumanFerroaluminumBlocks.FERROALUMINUM_PLATING_STAIRS.get());

        generators.family(HumanSteelBlocks.STEEL_PLATING.get())
            .slab(HumanSteelBlocks.STEEL_PLATING_SLAB.get())
            .stairs(HumanSteelBlocks.STEEL_PLATING_STAIRS.get());

        generators.family(HumanTitaniumBlocks.TITANIUM_PLATING.get())
            .slab(HumanTitaniumBlocks.TITANIUM_PLATING_SLAB.get())
            .stairs(HumanTitaniumBlocks.TITANIUM_PLATING_STAIRS.get());

        generators.family(HumanFerroaluminumBlocks.FERROALUMINUM_TREAD.get())
            .slab(HumanFerroaluminumBlocks.FERROALUMINUM_TREAD_SLAB.get())
            .stairs(HumanFerroaluminumBlocks.FERROALUMINUM_TREAD_STAIRS.get());

        generators.family(HumanSteelBlocks.STEEL_TREAD.get())
            .slab(HumanSteelBlocks.STEEL_TREAD_SLAB.get())
            .stairs(HumanSteelBlocks.STEEL_TREAD_STAIRS.get());

        generators.family(HumanTitaniumBlocks.TITANIUM_TREAD.get())
            .slab(HumanTitaniumBlocks.TITANIUM_TREAD_SLAB.get())
            .stairs(HumanTitaniumBlocks.TITANIUM_TREAD_STAIRS.get());

        generators.family(HumanFerroaluminumBlocks.FERROALUMINUM_GRATE.get())
            .slab(HumanFerroaluminumBlocks.FERROALUMINUM_GRATE_SLAB.get())
            .stairs(HumanFerroaluminumBlocks.FERROALUMINUM_GRATE_STAIRS.get());

        generators.family(HumanSteelBlocks.STEEL_GRATE.get())
            .slab(HumanSteelBlocks.STEEL_GRATE_SLAB.get())
            .stairs(HumanSteelBlocks.STEEL_GRATE_STAIRS.get());

        generators.family(HumanTitaniumBlocks.TITANIUM_GRATE.get())
            .slab(HumanTitaniumBlocks.TITANIUM_GRATE_SLAB.get())
            .stairs(HumanTitaniumBlocks.TITANIUM_GRATE_STAIRS.get());
    }

    private void createRotatedPillar(BlockModelGenerators generators, Block rotatedPillarBlock, TexturedModel.Provider modelProvider) {
        var resourceLocation = modelProvider.create(rotatedPillarBlock, generators.modelOutput);
        generators.blockStateOutput.accept(
            BlockModelGenerators.createRotatedPillarWithHorizontalVariant(rotatedPillarBlock, resourceLocation, resourceLocation)
        );
    }

    private void createGlassBlocks(BlockModelGenerators generators, Block block, Block block2) {
        TextureMapping textureMapping = TextureMapping.pane(block, block2);
        ResourceLocation resourceLocation = ModelTemplates.STAINED_GLASS_PANE_POST.create(block2, textureMapping, generators.modelOutput);
        ResourceLocation resourceLocation2 = ModelTemplates.STAINED_GLASS_PANE_SIDE.create(block2, textureMapping, generators.modelOutput);
        ResourceLocation resourceLocation3 = ModelTemplates.STAINED_GLASS_PANE_SIDE_ALT.create(
            block2,
            textureMapping,
            generators.modelOutput
        );
        ResourceLocation resourceLocation4 = ModelTemplates.STAINED_GLASS_PANE_NOSIDE.create(
            block2,
            textureMapping,
            generators.modelOutput
        );
        ResourceLocation resourceLocation5 = ModelTemplates.STAINED_GLASS_PANE_NOSIDE_ALT.create(
            block2,
            textureMapping,
            generators.modelOutput
        );
        Item item = block2.asItem();
        ModelTemplates.FLAT_ITEM.create(ModelLocationUtils.getModelLocation(item), TextureMapping.layer0(block), generators.modelOutput);
        generators.blockStateOutput
            .accept(
                MultiPartGenerator.multiPart(block2)
                    .with(Variant.variant().with(VariantProperties.MODEL, resourceLocation))
                    .with(
                        Condition.condition().term(BlockStateProperties.NORTH, true),
                        Variant.variant().with(VariantProperties.MODEL, resourceLocation2)
                    )
                    .with(
                        Condition.condition().term(BlockStateProperties.EAST, true),
                        Variant.variant()
                            .with(VariantProperties.MODEL, resourceLocation2)
                            .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)
                    )
                    .with(
                        Condition.condition().term(BlockStateProperties.SOUTH, true),
                        Variant.variant().with(VariantProperties.MODEL, resourceLocation3)
                    )
                    .with(
                        Condition.condition().term(BlockStateProperties.WEST, true),
                        Variant.variant()
                            .with(VariantProperties.MODEL, resourceLocation3)
                            .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)
                    )
                    .with(
                        Condition.condition().term(BlockStateProperties.NORTH, false),
                        Variant.variant().with(VariantProperties.MODEL, resourceLocation4)
                    )
                    .with(
                        Condition.condition().term(BlockStateProperties.EAST, false),
                        Variant.variant().with(VariantProperties.MODEL, resourceLocation5)
                    )
                    .with(
                        Condition.condition().term(BlockStateProperties.SOUTH, false),
                        Variant.variant()
                            .with(VariantProperties.MODEL, resourceLocation5)
                            .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)
                    )
                    .with(
                        Condition.condition().term(BlockStateProperties.WEST, false),
                        Variant.variant()
                            .with(VariantProperties.MODEL, resourceLocation4)
                            .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270)
                    )
            );
    }

    private void createSlab(
        BlockModelGenerators generators,
        Block baseBlock,
        Block slabBlock
    ) {
        var resourceLocation = ModelLocationUtils.getModelLocation(baseBlock);
        var textureMapping = TextureMapping.cube(baseBlock)
            .put(TextureSlot.BOTTOM, resourceLocation)
            .put(TextureSlot.TOP, resourceLocation);

        var bottom = ModelTemplates.SLAB_BOTTOM.create(slabBlock, textureMapping, generators.modelOutput);
        var top = ModelTemplates.SLAB_TOP.create(slabBlock, textureMapping, generators.modelOutput);

        generators.blockStateOutput.accept(
            BlockModelGenerators.createSlab(slabBlock, bottom, top, resourceLocation)
        );
    }

    private void createStairs(BlockModelGenerators generators, Block baseBlock, Block stairsBlock) {
        var resourceLocation = ModelLocationUtils.getModelLocation(baseBlock);

        var textureMapping = TextureMapping.cube(baseBlock)
            .put(TextureSlot.BOTTOM, resourceLocation)
            .put(TextureSlot.TOP, resourceLocation);

        var innerResourceLocation = ModelTemplates.STAIRS_INNER.create(stairsBlock, textureMapping, generators.modelOutput);
        var straightResourceLocation = ModelTemplates.STAIRS_STRAIGHT.create(stairsBlock, textureMapping, generators.modelOutput);
        var outerResourceLocation = ModelTemplates.STAIRS_OUTER.create(stairsBlock, textureMapping, generators.modelOutput);

        generators.blockStateOutput.accept(
            BlockModelGenerators.createStairs(stairsBlock, innerResourceLocation, straightResourceLocation, outerResourceLocation)
        );
    }

    private void createWall(
        BlockModelGenerators generators,
        Block block,
        Block wallBlock
    ) {
        createWallCustomTop(generators, block, wallBlock, TextureMapping.getBlockTexture(block));
    }

    private void createWallCustomTop(
        BlockModelGenerators generators,
        Block block,
        Block wallBlock,
        ResourceLocation topResourceLocation
    ) {
        var wallTextureMapping = TextureMapping.cube(block)
            .put(TextureSlot.TOP, topResourceLocation);
        var wallTopTextureMapping = TextureMapping.cube(block)
            .put(TextureSlot.WALL, topResourceLocation);
        var postResourceLocation = ModelTemplates.WALL_POST.create(
            wallBlock,
            wallTopTextureMapping,
            generators.modelOutput
        );
        var lowSideResourceLocation = BLibModelTemplates.WALL_LOW_SIDE.create(
            wallBlock,
            wallTextureMapping,
            generators.modelOutput
        );
        var tallSideResourceLocation = BLibModelTemplates.WALL_TALL_SIDE.create(
            wallBlock,
            wallTextureMapping,
            generators.modelOutput
        );

        generators.blockStateOutput.accept(
            BlockModelGenerators.createWall(wallBlock, postResourceLocation, lowSideResourceLocation, tallSideResourceLocation)
        );

        var inventoryResourceLocation = BLibModelTemplates.WALL_INVENTORY.create(
            wallBlock,
            wallTextureMapping,
            generators.modelOutput
        );
        generators.delegateItemModel(wallBlock, inventoryResourceLocation);
    }

    private void createIndustrialGlassSlab(BlockModelGenerators generators) {
        createIndustrialGlassSlab(
            generators,
            HumanIndustrialGlassBlocks.INDUSTRIAL_GLASS.get(),
            HumanIndustrialGlassBlocks.INDUSTRIAL_GLASS_SLAB.get()
        );
    }

    /**
     * ⚠ The slab takes its SIDE from a dedicated {@code _slab_side} texture rather than the block face - the seam sits
     * in the middle of the sheet instead of at the top - so every coloured slab needs its own
     * {@code <colour>_industrial_glass_slab_side.png}. The top and bottom come from the parent block.
     */
    private void createIndustrialGlassSlab(BlockModelGenerators generators, Block block, Block slabBlock) {
        var textureMapping = TextureMapping.cube(block);
        var textureMapping2 = TextureMapping.column(
            TextureMapping.getBlockTexture(slabBlock, "_side"),
            textureMapping.get(TextureSlot.TOP)
        );
        var bottomResourceLocation = ModelTemplates.SLAB_BOTTOM.create(slabBlock, textureMapping2, generators.modelOutput);
        var topResourceLocation = ModelTemplates.SLAB_TOP.create(slabBlock, textureMapping2, generators.modelOutput);
        var columnResourceLocation = ModelTemplates.CUBE_COLUMN.createWithOverride(
            slabBlock,
            "_double",
            textureMapping2,
            generators.modelOutput
        );

        generators.blockStateOutput.accept(
            BlockModelGenerators.createSlab(slabBlock, bottomResourceLocation, topResourceLocation, columnResourceLocation)
        );
    }

    @Override
    public void generateItemModels(ItemModelGenerators generators) {}

    @Override
    public @NotNull String getName() {
        return "Block Model Definitions";
    }
}
