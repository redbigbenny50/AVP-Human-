package com.human.fabric.data.recipe.impl;

import com.blib.fabric.data.recipe.builder.RecipeBuilder;
import com.human.common.registry.init.block.HumanPlasticBlocks;
import com.human.common.registry.tag.HumanItemTags;
import com.human.fabric.data.recipe.builder.IndustrialFurnaceRecipeBuilder;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;

public class IndustrialFurnaceRecipeProvider {

    public static void provide(RecipeBuilder builder) {
        // Stone Recipes
        industrialFurnaceSmelting(Items.COBBLESTONE)
            .withCategory(RecipeCategory.BUILDING_BLOCKS)
            .withExperience(0.1f)
            .withCookingTime(100)
            .into(Items.STONE);

        industrialFurnaceSmelting(Items.COBBLED_DEEPSLATE)
            .withCategory(RecipeCategory.BUILDING_BLOCKS)
            .withExperience(0.1f)
            .withCookingTime(100)
            .into(Items.DEEPSLATE);

        industrialFurnaceSmelting(Items.QUARTZ_BLOCK)
            .withCategory(RecipeCategory.BUILDING_BLOCKS)
            .withExperience(0.1f)
            .withCookingTime(100)
            .into(Items.SMOOTH_QUARTZ);

        industrialFurnaceSmelting(Items.STONE)
            .withCategory(RecipeCategory.BUILDING_BLOCKS)
            .withExperience(0.1f)
            .withCookingTime(100)
            .into(Items.SMOOTH_STONE);

        industrialFurnaceSmelting(Items.BASALT)
            .withCategory(RecipeCategory.BUILDING_BLOCKS)
            .withExperience(0.1f)
            .withCookingTime(100)
            .into(Items.SMOOTH_BASALT);

        // Sandstone Recipes
        industrialFurnaceSmelting(Items.SANDSTONE)
            .withCategory(RecipeCategory.BUILDING_BLOCKS)
            .withExperience(0.1f)
            .withCookingTime(100)
            .into(Items.SMOOTH_SANDSTONE);

        industrialFurnaceSmelting(Items.RED_SANDSTONE)
            .withCategory(RecipeCategory.BUILDING_BLOCKS)
            .withExperience(0.1f)
            .withCookingTime(100)
            .into(Items.SMOOTH_RED_SANDSTONE);

        // Cracked Bricks Recipes
        industrialFurnaceSmelting(Items.STONE_BRICKS)
            .withCategory(RecipeCategory.BUILDING_BLOCKS)
            .withExperience(0.1f)
            .withCookingTime(100)
            .into(Items.CRACKED_STONE_BRICKS);

        industrialFurnaceSmelting(Items.DEEPSLATE_BRICKS)
            .withCategory(RecipeCategory.BUILDING_BLOCKS)
            .withExperience(0.1f)
            .withCookingTime(100)
            .into(Items.CRACKED_DEEPSLATE_BRICKS);

        industrialFurnaceSmelting(Items.DEEPSLATE_TILES)
            .withCategory(RecipeCategory.BUILDING_BLOCKS)
            .withExperience(0.1f)
            .withCookingTime(100)
            .into(Items.CRACKED_DEEPSLATE_TILES);

        industrialFurnaceSmelting(Items.POLISHED_BLACKSTONE_BRICKS)
            .withCategory(RecipeCategory.BUILDING_BLOCKS)
            .withExperience(0.1f)
            .withCookingTime(100)
            .into(Items.CRACKED_POLISHED_BLACKSTONE_BRICKS);

        industrialFurnaceSmelting(Items.NETHER_BRICKS)
            .withCategory(RecipeCategory.BUILDING_BLOCKS)
            .withExperience(0.1f)
            .withCookingTime(100)
            .into(Items.CRACKED_NETHER_BRICKS);

        // Terracotta Recipes - All colors
        industrialFurnaceSmelting(Items.WHITE_TERRACOTTA)
            .withCategory(RecipeCategory.BUILDING_BLOCKS)
            .withExperience(0.1f)
            .withCookingTime(100)
            .into(Items.WHITE_GLAZED_TERRACOTTA);

        industrialFurnaceSmelting(Items.ORANGE_TERRACOTTA)
            .withCategory(RecipeCategory.BUILDING_BLOCKS)
            .withExperience(0.1f)
            .withCookingTime(100)
            .into(Items.ORANGE_GLAZED_TERRACOTTA);

        industrialFurnaceSmelting(Items.MAGENTA_TERRACOTTA)
            .withCategory(RecipeCategory.BUILDING_BLOCKS)
            .withExperience(0.1f)
            .withCookingTime(100)
            .into(Items.MAGENTA_GLAZED_TERRACOTTA);

        industrialFurnaceSmelting(Items.LIGHT_BLUE_TERRACOTTA)
            .withCategory(RecipeCategory.BUILDING_BLOCKS)
            .withExperience(0.1f)
            .withCookingTime(100)
            .into(Items.LIGHT_BLUE_GLAZED_TERRACOTTA);

        industrialFurnaceSmelting(Items.YELLOW_TERRACOTTA)
            .withCategory(RecipeCategory.BUILDING_BLOCKS)
            .withExperience(0.1f)
            .withCookingTime(100)
            .into(Items.YELLOW_GLAZED_TERRACOTTA);

        industrialFurnaceSmelting(Items.LIME_TERRACOTTA)
            .withCategory(RecipeCategory.BUILDING_BLOCKS)
            .withExperience(0.1f)
            .withCookingTime(100)
            .into(Items.LIME_GLAZED_TERRACOTTA);

        industrialFurnaceSmelting(Items.PINK_TERRACOTTA)
            .withCategory(RecipeCategory.BUILDING_BLOCKS)
            .withExperience(0.1f)
            .withCookingTime(100)
            .into(Items.PINK_GLAZED_TERRACOTTA);

        industrialFurnaceSmelting(Items.GRAY_TERRACOTTA)
            .withCategory(RecipeCategory.BUILDING_BLOCKS)
            .withExperience(0.1f)
            .withCookingTime(100)
            .into(Items.GRAY_GLAZED_TERRACOTTA);

        industrialFurnaceSmelting(Items.LIGHT_GRAY_TERRACOTTA)
            .withCategory(RecipeCategory.BUILDING_BLOCKS)
            .withExperience(0.1f)
            .withCookingTime(100)
            .into(Items.LIGHT_GRAY_GLAZED_TERRACOTTA);

        industrialFurnaceSmelting(Items.CYAN_TERRACOTTA)
            .withCategory(RecipeCategory.BUILDING_BLOCKS)
            .withExperience(0.1f)
            .withCookingTime(100)
            .into(Items.CYAN_GLAZED_TERRACOTTA);

        industrialFurnaceSmelting(Items.PURPLE_TERRACOTTA)
            .withCategory(RecipeCategory.BUILDING_BLOCKS)
            .withExperience(0.1f)
            .withCookingTime(100)
            .into(Items.PURPLE_GLAZED_TERRACOTTA);

        industrialFurnaceSmelting(Items.BLUE_TERRACOTTA)
            .withCategory(RecipeCategory.BUILDING_BLOCKS)
            .withExperience(0.1f)
            .withCookingTime(100)
            .into(Items.BLUE_GLAZED_TERRACOTTA);

        industrialFurnaceSmelting(Items.BROWN_TERRACOTTA)
            .withCategory(RecipeCategory.BUILDING_BLOCKS)
            .withExperience(0.1f)
            .withCookingTime(100)
            .into(Items.BROWN_GLAZED_TERRACOTTA);

        industrialFurnaceSmelting(Items.GREEN_TERRACOTTA)
            .withCategory(RecipeCategory.BUILDING_BLOCKS)
            .withExperience(0.1f)
            .withCookingTime(100)
            .into(Items.GREEN_GLAZED_TERRACOTTA);

        industrialFurnaceSmelting(Items.RED_TERRACOTTA)
            .withCategory(RecipeCategory.BUILDING_BLOCKS)
            .withExperience(0.1f)
            .withCookingTime(100)
            .into(Items.RED_GLAZED_TERRACOTTA);

        industrialFurnaceSmelting(Items.BLACK_TERRACOTTA)
            .withCategory(RecipeCategory.BUILDING_BLOCKS)
            .withExperience(0.1f)
            .withCookingTime(100)
            .into(Items.BLACK_GLAZED_TERRACOTTA);

        // Miscellaneous Recipes
        industrialFurnaceSmelting(Items.WET_SPONGE)
            .withCategory(RecipeCategory.BUILDING_BLOCKS)
            .withExperience(0.15f)
            .withCookingTime(100)
            .into(Items.SPONGE);

        industrialFurnaceSmelting(Items.SAND)
            .withCategory(RecipeCategory.BUILDING_BLOCKS)
            .withExperience(0.1f)
            .withCookingTime(100)
            .into(Items.GLASS);

        industrialFurnaceSmelting(Items.RED_SAND)
            .withCategory(RecipeCategory.BUILDING_BLOCKS)
            .withExperience(0.1f)
            .withCookingTime(100)
            .into(Items.GLASS);

        industrialFurnaceSmelting(Items.MUD)
            .withCategory(RecipeCategory.BUILDING_BLOCKS)
            .withExperience(0.1f)
            .withCookingTime(100)
            .into(Items.CLAY);

        industrialFurnaceSmelting(Items.CLAY)
            .withCategory(RecipeCategory.BUILDING_BLOCKS)
            .withExperience(0.35f)
            .withCookingTime(100)
            .into(Items.TERRACOTTA);

        // Alien Recipes
        //
        // The long way round is four resin balls smelted to slime balls, smelted to polymer, then crafted into a
        // plastic block. Since four resin balls are also one resin block, cooking the block whole lands on the same
        // plastic block for the same resin -- it buys steps, not material. Every strain's base resin qualifies.
        industrialFurnaceSmelting(HumanItemTags.RESIN_BLOCKS, "resin_blocks")
            .withCategory(RecipeCategory.BUILDING_BLOCKS)
            .withExperience(0.1f)
            .withCookingTime(100)
            .into(HumanPlasticBlocks.DYE_COLOR_TO_PLASTIC.get(DyeColor.GREEN).get());
    }

    /**
     * Tag-input variant. The item overload below derives its unlock criterion and recipe id from the input item, and a
     * tag has neither, so both are passed in: the recipe unlocks on the RESULT being obtained, and the caller names the
     * recipe explicitly.
     */
    private static IndustrialFurnaceRecipeBuilder industrialFurnaceSmelting(TagKey<Item> tag, String name) {
        IndustrialFurnaceRecipeBuilder builder = IndustrialFurnaceRecipeBuilder.smelting(
            Ingredient.of(tag),
            RecipeCategory.MISC,
            HumanPlasticBlocks.DYE_COLOR_TO_PLASTIC.get(DyeColor.GREEN).get(),
            0.1f,
            100
        );

        builder.group(name);

        return builder;
    }

    private static IndustrialFurnaceRecipeBuilder industrialFurnaceSmelting(ItemLike item) {
        IndustrialFurnaceRecipeBuilder builder = IndustrialFurnaceRecipeBuilder.smelting(
            Ingredient.of(item),
            RecipeCategory.MISC,
            item,
            0.1f,
            100
        );

        builder.unlockedBy(RecipeProvider.getHasName(item), RecipeProvider.has(item));

        return builder;
    }
}
