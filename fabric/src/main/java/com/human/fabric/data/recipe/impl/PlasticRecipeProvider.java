package com.human.fabric.data.recipe.impl;

import com.blib.fabric.data.recipe.RecipeTemplates;
import com.blib.fabric.data.recipe.builder.RecipeBuilder;
import com.blib.fabric.data.recipe.util.RecipeUtil;
import com.human.common.registry.init.block.HumanPlasticBlocks;
import com.human.common.registry.init.item.HumanItems;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.function.Supplier;

public class PlasticRecipeProvider {

    public static void provide(RecipeBuilder builder) {
        createPlasticBlockRecipes(builder);
        createUniquePlasticBlockRecipes(builder);
    }

    private static void createUniquePlasticBlockRecipes(RecipeBuilder builder) {
        var aisleHazardPlastic = HumanPlasticBlocks.AISLE_HAZARD_PLASTIC.get();
        builder.shaped()
            .withCategory(RecipeCategory.BUILDING_BLOCKS)
            .define('A', HumanPlasticBlocks.DYE_COLOR_TO_PLASTIC.get(DyeColor.WHITE))
            .define('B', HumanPlasticBlocks.DYE_COLOR_TO_PLASTIC.get(DyeColor.BLACK))
            .pattern("ABA")
            .pattern("BAB")
            .pattern("ABA")
            .into(9, aisleHazardPlastic);
        RecipeUtil.createSlabBlockManualAndStonecutterRecipes(
            builder,
            aisleHazardPlastic,
            HumanPlasticBlocks.AISLE_HAZARD_PLASTIC_SLAB.get()
        );
        RecipeUtil.createStairBlockManualAndStonecutterRecipes(
            builder,
            aisleHazardPlastic,
            HumanPlasticBlocks.AISLE_HAZARD_PLASTIC_STAIRS.get()
        );
        RecipeUtil.createWallBlockManualAndStonecutterRecipes(
            builder,
            aisleHazardPlastic,
            HumanPlasticBlocks.AISLE_HAZARD_PLASTIC_WALL.get()
        );

        var alienHazardPlastic = HumanPlasticBlocks.ALIEN_HAZARD_PLASTIC.get();
        builder.shaped()
            .withCategory(RecipeCategory.BUILDING_BLOCKS)
            .define('A', HumanPlasticBlocks.DYE_COLOR_TO_PLASTIC.get(DyeColor.GREEN))
            .define('B', HumanPlasticBlocks.DYE_COLOR_TO_PLASTIC.get(DyeColor.BLACK))
            .pattern("ABA")
            .pattern("BAB")
            .pattern("ABA")
            .into(9, alienHazardPlastic);
        RecipeUtil.createSlabBlockManualAndStonecutterRecipes(
            builder,
            alienHazardPlastic,
            HumanPlasticBlocks.ALIEN_HAZARD_PLASTIC_SLAB.get()
        );
        RecipeUtil.createStairBlockManualAndStonecutterRecipes(
            builder,
            alienHazardPlastic,
            HumanPlasticBlocks.ALIEN_HAZARD_PLASTIC_STAIRS.get()
        );
        RecipeUtil.createWallBlockManualAndStonecutterRecipes(
            builder,
            alienHazardPlastic,
            HumanPlasticBlocks.ALIEN_HAZARD_PLASTIC_WALL.get()
        );

        var fireHazardPlastic = HumanPlasticBlocks.FIRE_HAZARD_PLASTIC.get();
        builder.shaped()
            .withCategory(RecipeCategory.BUILDING_BLOCKS)
            .define('A', HumanPlasticBlocks.DYE_COLOR_TO_PLASTIC.get(DyeColor.RED))
            .define('B', HumanPlasticBlocks.DYE_COLOR_TO_PLASTIC.get(DyeColor.WHITE))
            .pattern("ABA")
            .pattern("BAB")
            .pattern("ABA")
            .into(9, fireHazardPlastic);
        RecipeUtil.createSlabBlockManualAndStonecutterRecipes(
            builder,
            fireHazardPlastic,
            HumanPlasticBlocks.FIRE_HAZARD_PLASTIC_SLAB.get()
        );
        RecipeUtil.createStairBlockManualAndStonecutterRecipes(
            builder,
            fireHazardPlastic,
            HumanPlasticBlocks.FIRE_HAZARD_PLASTIC_STAIRS.get()
        );
        RecipeUtil.createWallBlockManualAndStonecutterRecipes(
            builder,
            fireHazardPlastic,
            HumanPlasticBlocks.FIRE_HAZARD_PLASTIC_WALL.get()
        );

        var hazardPlastic = HumanPlasticBlocks.HAZARD_PLASTIC.get();
        builder.shaped()
            .withCategory(RecipeCategory.BUILDING_BLOCKS)
            .define('A', HumanPlasticBlocks.DYE_COLOR_TO_PLASTIC.get(DyeColor.YELLOW))
            .define('B', HumanPlasticBlocks.DYE_COLOR_TO_PLASTIC.get(DyeColor.BLACK))
            .pattern("ABA")
            .pattern("BAB")
            .pattern("ABA")
            .into(9, hazardPlastic);
        RecipeUtil.createSlabBlockManualAndStonecutterRecipes(builder, hazardPlastic, HumanPlasticBlocks.HAZARD_PLASTIC_SLAB.get());
        RecipeUtil.createStairBlockManualAndStonecutterRecipes(builder, hazardPlastic, HumanPlasticBlocks.HAZARD_PLASTIC_STAIRS.get());
        RecipeUtil.createWallBlockManualAndStonecutterRecipes(builder, hazardPlastic, HumanPlasticBlocks.HAZARD_PLASTIC_WALL.get());

        var machineHazardPlastic = HumanPlasticBlocks.MACHINE_HAZARD_PLASTIC.get();
        builder.shaped()
            .withCategory(RecipeCategory.BUILDING_BLOCKS)
            .define('A', HumanPlasticBlocks.DYE_COLOR_TO_PLASTIC.get(DyeColor.BLUE))
            .define('B', HumanPlasticBlocks.DYE_COLOR_TO_PLASTIC.get(DyeColor.WHITE))
            .pattern("ABA")
            .pattern("BAB")
            .pattern("ABA")
            .into(9, machineHazardPlastic);
        RecipeUtil.createSlabBlockManualAndStonecutterRecipes(
            builder,
            machineHazardPlastic,
            HumanPlasticBlocks.MACHINE_HAZARD_PLASTIC_SLAB.get()
        );
        RecipeUtil.createStairBlockManualAndStonecutterRecipes(
            builder,
            machineHazardPlastic,
            HumanPlasticBlocks.MACHINE_HAZARD_PLASTIC_STAIRS.get()
        );
        RecipeUtil.createWallBlockManualAndStonecutterRecipes(
            builder,
            machineHazardPlastic,
            HumanPlasticBlocks.MACHINE_HAZARD_PLASTIC_WALL.get()
        );

        var radiationHazardPlastic = HumanPlasticBlocks.RADIATION_HAZARD_PLASTIC.get();
        builder.shaped()
            .withCategory(RecipeCategory.BUILDING_BLOCKS)
            .define('A', HumanPlasticBlocks.DYE_COLOR_TO_PLASTIC.get(DyeColor.YELLOW))
            .define('B', HumanPlasticBlocks.DYE_COLOR_TO_PLASTIC.get(DyeColor.MAGENTA))
            .pattern("ABA")
            .pattern("BAB")
            .pattern("ABA")
            .into(9, radiationHazardPlastic);
        RecipeUtil.createSlabBlockManualAndStonecutterRecipes(
            builder,
            radiationHazardPlastic,
            HumanPlasticBlocks.RADIATION_HAZARD_PLASTIC_SLAB.get()
        );
        RecipeUtil.createStairBlockManualAndStonecutterRecipes(
            builder,
            radiationHazardPlastic,
            HumanPlasticBlocks.RADIATION_HAZARD_PLASTIC_STAIRS.get()
        );
        RecipeUtil.createWallBlockManualAndStonecutterRecipes(
            builder,
            radiationHazardPlastic,
            HumanPlasticBlocks.RADIATION_HAZARD_PLASTIC_WALL.get()
        );

        var safetyPlastic = HumanPlasticBlocks.SAFETY_PLASTIC.get();
        builder.shaped()
            .withCategory(RecipeCategory.BUILDING_BLOCKS)
            .define('A', HumanPlasticBlocks.DYE_COLOR_TO_PLASTIC.get(DyeColor.LIME))
            .define('B', HumanPlasticBlocks.DYE_COLOR_TO_PLASTIC.get(DyeColor.WHITE))
            .pattern("ABA")
            .pattern("BAB")
            .pattern("ABA")
            .into(9, safetyPlastic);
        RecipeUtil.createSlabBlockManualAndStonecutterRecipes(builder, safetyPlastic, HumanPlasticBlocks.SAFETY_PLASTIC_SLAB.get());
        RecipeUtil.createStairBlockManualAndStonecutterRecipes(builder, safetyPlastic, HumanPlasticBlocks.SAFETY_PLASTIC_STAIRS.get());
        RecipeUtil.createWallBlockManualAndStonecutterRecipes(builder, safetyPlastic, HumanPlasticBlocks.SAFETY_PLASTIC_WALL.get());

        var trafficHazardPlastic = HumanPlasticBlocks.TRAFFIC_HAZARD_PLASTIC.get();
        builder.shaped()
            .withCategory(RecipeCategory.BUILDING_BLOCKS)
            .define('A', HumanPlasticBlocks.DYE_COLOR_TO_PLASTIC.get(DyeColor.ORANGE))
            .define('B', HumanPlasticBlocks.DYE_COLOR_TO_PLASTIC.get(DyeColor.WHITE))
            .pattern("ABA")
            .pattern("BAB")
            .pattern("ABA")
            .into(9, trafficHazardPlastic);
        RecipeUtil.createSlabBlockManualAndStonecutterRecipes(
            builder,
            trafficHazardPlastic,
            HumanPlasticBlocks.TRAFFIC_HAZARD_PLASTIC_SLAB.get()
        );
        RecipeUtil.createStairBlockManualAndStonecutterRecipes(
            builder,
            trafficHazardPlastic,
            HumanPlasticBlocks.TRAFFIC_HAZARD_PLASTIC_STAIRS.get()
        );
        RecipeUtil.createWallBlockManualAndStonecutterRecipes(
            builder,
            trafficHazardPlastic,
            HumanPlasticBlocks.TRAFFIC_HAZARD_PLASTIC_WALL.get()
        );
    }

    private static void createPlasticBlockRecipes(RecipeBuilder builder) {
        var base = HumanPlasticBlocks.DYE_COLOR_TO_PLASTIC.get(DyeColor.GREEN).get();

        builder.shaped()
            .withCategory(RecipeCategory.BUILDING_BLOCKS)
            .define('A', HumanItems.POLYMER)
            .pattern("AA")
            .pattern("AA")
            .into(1, base);

        HumanPlasticBlocks.DYE_COLOR_TO_PLASTIC.forEach(((dyeColor, blockSupplier) -> {
            var block = blockSupplier.get();
            var dyeItem = DyeItem.byColor(dyeColor);

            var ingredient = Ingredient.of(
                HumanPlasticBlocks.DYE_COLOR_TO_PLASTIC.values()
                    .stream()
                    .map(Supplier::get)
                    .filter(plasticBlock -> !plasticBlock.equals(block))
                    .map(ItemStack::new)
            );

            builder.shaped()
                .withCategory(RecipeCategory.BUILDING_BLOCKS)
                .define('A', dyeItem)
                .define('B', ingredient)
                .pattern("BBB")
                .pattern("BAB")
                .pattern("BBB")
                .withCustomName((outputItem) -> "dye_" + outputItem)
                .into(8, block);

            var slabBlock = HumanPlasticBlocks.DYE_COLOR_TO_PLASTIC_SLAB.get(dyeColor).get();
            RecipeUtil.createSlabBlockManualAndStonecutterRecipes(builder, block, slabBlock);

            var stairBlock = HumanPlasticBlocks.DYE_COLOR_TO_PLASTIC_STAIRS.get(dyeColor).get();
            RecipeUtil.createStairBlockManualAndStonecutterRecipes(builder, block, stairBlock);

            var wallBlock = HumanPlasticBlocks.DYE_COLOR_TO_PLASTIC_WALL.get(dyeColor).get();
            RecipeUtil.createWallBlockManualAndStonecutterRecipes(builder, block, wallBlock);

            var stonecut = builder.stonecut(block);

            var cutBlock = HumanPlasticBlocks.DYE_COLOR_TO_CUT_PLASTIC.get(dyeColor).get();
            stonecut.into(4, cutBlock);

            builder.shaped()
                .withCategory(RecipeCategory.BUILDING_BLOCKS)
                .apply(RecipeTemplates.COMPRESSED_BLOCK_2x2.apply(block))
                .into(4, cutBlock);

            var cutSlabBlock = HumanPlasticBlocks.DYE_COLOR_TO_CUT_PLASTIC_SLAB.get(dyeColor).get();
            RecipeUtil.createSlabBlockManualAndStonecutterRecipes(builder, cutBlock, cutSlabBlock);
            stonecut.into(8, cutSlabBlock);

            var cutStairBlock = HumanPlasticBlocks.DYE_COLOR_TO_CUT_PLASTIC_STAIRS.get(dyeColor).get();
            RecipeUtil.createStairBlockManualAndStonecutterRecipes(builder, cutBlock, cutStairBlock);
            stonecut.into(4, cutStairBlock);

            var framedBlock = HumanPlasticBlocks.DYE_COLOR_TO_FRAMED_PLASTIC.get(dyeColor).get();
            stonecut.into(2, framedBlock);

            var pittedBlock = HumanPlasticBlocks.DYE_COLOR_TO_PITTED_PLASTIC.get(dyeColor).get();
            stonecut.into(4, pittedBlock);

            var pittedSlabBlock = HumanPlasticBlocks.DYE_COLOR_TO_PITTED_PLASTIC_SLAB.get(dyeColor).get();
            RecipeUtil.createSlabBlockManualAndStonecutterRecipes(builder, pittedBlock, pittedSlabBlock);
            stonecut.into(8, pittedSlabBlock);

            var pittedStairBlock = HumanPlasticBlocks.DYE_COLOR_TO_PITTED_PLASTIC_STAIRS.get(dyeColor).get();
            RecipeUtil.createStairBlockManualAndStonecutterRecipes(builder, pittedBlock, pittedStairBlock);
            stonecut.into(4, pittedStairBlock);

            var grateBlock = HumanPlasticBlocks.DYE_COLOR_TO_PLASTIC_GRATE.get(dyeColor).get();
            stonecut.into(4, grateBlock);

            var grateSlabBlock = HumanPlasticBlocks.DYE_COLOR_TO_PLASTIC_GRATE_SLAB.get(dyeColor).get();
            RecipeUtil.createSlabBlockManualAndStonecutterRecipes(builder, grateBlock, grateSlabBlock);
            stonecut.into(8, grateSlabBlock);

            var grateStairBlock = HumanPlasticBlocks.DYE_COLOR_TO_PLASTIC_GRATE_STAIRS.get(dyeColor).get();
            RecipeUtil.createStairBlockManualAndStonecutterRecipes(builder, grateBlock, grateStairBlock);
            stonecut.into(4, grateStairBlock);
        }));
    }
}
