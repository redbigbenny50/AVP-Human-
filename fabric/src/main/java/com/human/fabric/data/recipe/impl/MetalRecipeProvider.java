package com.human.fabric.data.recipe.impl;

import com.blib.api.common.tag.v1.CommonItemTags;
import com.blib.fabric.data.recipe.RecipeConstants;
import com.blib.fabric.data.recipe.RecipeTemplates;
import com.blib.fabric.data.recipe.builder.RecipeBuilder;
import com.blib.fabric.data.recipe.util.RecipeUtil;
import com.human.common.registry.init.block.CoreBlocks;
import com.human.common.registry.init.block.HumanCompositeBlocks;
import com.human.common.registry.init.block.HumanFerroaluminumBlocks;
import com.human.common.registry.init.block.HumanSteelBlocks;
import com.human.common.registry.init.block.HumanTitaniumBlocks;
import com.human.common.registry.init.item.HumanItems;
import com.human.compatibility.HumanCommonItemTags;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;

import java.util.function.Supplier;

public class MetalRecipeProvider {

    public static void provide(RecipeBuilder builder) {
        builder.shapeless()
            .withCategory(RecipeCategory.MISC)
            .requires(1, CommonItemTags.DUSTS_COAL)
            .requires(1, CommonItemTags.RAW_MATERIALS_IRON)
            .into(1, HumanItems.RAW_CRUDE_IRON.get());

        builder.shapeless()
            .withCategory(RecipeCategory.MISC)
            .requires(1, HumanCommonItemTags.RAW_MATERIALS_ALUMINUM)
            .requires(1, CommonItemTags.DUSTS_COAL)
            .requires(1, CommonItemTags.RAW_MATERIALS_IRON)
            .into(2, HumanItems.RAW_FERROBAUXITE.get());

        builder.shapeless()
            .withCategory(RecipeCategory.MISC)
            .withCustomName(name -> "raw_ferrobauxite_using_raw_crude_iron")
            .requires(1, HumanCommonItemTags.RAW_MATERIALS_ALUMINUM)
            .requires(1, HumanCommonItemTags.RAW_MATERIALS_STEEL)
            .into(2, HumanItems.RAW_FERROBAUXITE.get());

        // Steel can only be blasted.
        builder.blast(HumanItems.RAW_CRUDE_IRON.get())
            .withCategory(RecipeCategory.MISC)
            .withExperience(RecipeConstants.RARE_SMELT_EXPERIENCE)
            .into(HumanItems.STEEL_INGOT.get());

        builder.shapeless()
            .withCategory(RecipeCategory.MISC)
            .requires(1, HumanCommonItemTags.RAW_MATERIALS_ZINC)
            .requires(1, CommonItemTags.RAW_MATERIALS_COPPER)
            .into(2, HumanItems.RAW_BRASS.get());

        builder.shaped()
            .withCustomName(name -> "uranium_ingot_normal")
            .withCategory(RecipeCategory.MISC)
            .define('A', HumanCommonItemTags.DUSTS_AUTUNITE)
            .define('T', HumanCommonItemTags.INGOTS_TITANIUM)
            .pattern(" A ")
            .pattern("ATA")
            .pattern(" A ")
            .into(1, HumanItems.URANIUM_INGOT.get());

        createSmeltAndBlastRecipes(builder, CoreBlocks.BAUXITE_ORE.get(), HumanItems.ALUMINUM_INGOT.get());
        createSmeltAndBlastRecipes(builder, CoreBlocks.GALENA_ORE.get(), HumanItems.LEAD_INGOT.get());
        createSmeltAndBlastRecipes(builder, CoreBlocks.MONAZITE_ORE.get(), HumanItems.NEODYMIUM_MAGNET.get());
        createSmeltAndBlastRecipes(builder, CoreBlocks.DEEPSLATE_TITANIUM_ORE.get(), HumanItems.TITANIUM_INGOT.get());
        createSmeltAndBlastRecipes(builder, CoreBlocks.ZINC_ORE.get(), HumanItems.ZINC_INGOT.get());
        createSmeltAndBlastRecipes(builder, CoreBlocks.DEEPSLATE_ZINC_ORE.get(), HumanItems.ZINC_INGOT.get());

        createSmeltAndBlastRecipes(builder, HumanItems.RAW_BAUXITE.get(), HumanItems.ALUMINUM_INGOT.get());
        createSmeltAndBlastRecipes(builder, HumanItems.RAW_BRASS.get(), HumanItems.BRASS_INGOT.get());
        createSmeltAndBlastRecipes(builder, HumanItems.RAW_FERROBAUXITE.get(), HumanItems.FERROALUMINUM_INGOT.get());
        createSmeltAndBlastRecipes(builder, HumanItems.RAW_GALENA.get(), HumanItems.LEAD_INGOT.get());
        createSmeltAndBlastRecipes(builder, HumanItems.RAW_MONAZITE.get(), HumanItems.NEODYMIUM_MAGNET.get());
        createSmeltAndBlastRecipes(builder, HumanItems.RAW_TITANIUM.get(), HumanItems.TITANIUM_INGOT.get());
        createSmeltAndBlastRecipes(builder, HumanItems.RAW_ZINC.get(), HumanItems.ZINC_INGOT.get());

        RecipeUtil.createCompressedBlockRecipes3x3TagFriendlyWithDiscriminator(
            builder,
            HumanItems.ALUMINUM_INGOT.get(),
            HumanCommonItemTags.INGOTS_ALUMINUM,
            CoreBlocks.ALUMINUM_BLOCK.get()
        );
        RecipeUtil.createCompressedBlockRecipes3x3TagFriendlyWithDiscriminator(
            builder,
            HumanItems.BRASS_INGOT.get(),
            HumanCommonItemTags.INGOTS_BRASS,
            CoreBlocks.BRASS_BLOCK.get()
        );
        RecipeUtil.createCompressedBlockRecipes3x3TagFriendlyWithDiscriminator(
            builder,
            HumanItems.FERROALUMINUM_INGOT.get(),
            HumanCommonItemTags.INGOTS_FERROALUMINUM,
            HumanFerroaluminumBlocks.FERROALUMINUM_BLOCK.get()
        );
        RecipeUtil.createCompressedBlockRecipes3x3TagFriendlyWithDiscriminator(
            builder,
            HumanItems.LEAD_INGOT.get(),
            HumanCommonItemTags.INGOTS_LEAD,
            CoreBlocks.LEAD_BLOCK.get()
        );
        RecipeUtil.createCompressedBlockRecipes3x3TagFriendlyWithDiscriminator(
            builder,
            HumanItems.STEEL_INGOT.get(),
            HumanCommonItemTags.INGOTS_STEEL,
            HumanSteelBlocks.STEEL_BLOCK.get()
        );

        // [stated] Oct 3: composite - 2 polymer and a steel ingot make a composite ingot; 9 make the block (and back).
        builder.shapeless()
            .withCategory(RecipeCategory.MISC)
            .requires(2, HumanItems.POLYMER)
            .requires(1, HumanCommonItemTags.INGOTS_STEEL)
            .into(1, HumanItems.COMPOSITE_INGOT);
        RecipeUtil.createCompressedBlockRecipes3x3TagFriendlyWithDiscriminator(
            builder,
            HumanItems.COMPOSITE_INGOT.get(),
            HumanCommonItemTags.INGOTS_COMPOSITE,
            HumanCompositeBlocks.COMPOSITE_BLOCK.get()
        );
        RecipeUtil.createCompressedBlockRecipes3x3TagFriendlyWithDiscriminator(
            builder,
            HumanItems.TITANIUM_INGOT.get(),
            HumanCommonItemTags.INGOTS_TITANIUM,
            HumanTitaniumBlocks.TITANIUM_BLOCK.get()
        );
        RecipeUtil.createCompressedBlockRecipes3x3TagFriendlyWithDiscriminator(
            builder,
            HumanItems.URANIUM_INGOT.get(),
            HumanCommonItemTags.INGOTS_URANIUM,
            CoreBlocks.URANIUM_BLOCK.get()
        );
        RecipeUtil.createCompressedBlockRecipes3x3TagFriendlyWithDiscriminator(
            builder,
            HumanItems.ZINC_INGOT.get(),
            HumanCommonItemTags.INGOTS_ZINC,
            CoreBlocks.ZINC_BLOCK.get()
        );

        RecipeUtil.createCompressedBlockRecipes3x3TagFriendlyWithDiscriminator(
            builder,
            HumanItems.AUTUNITE_DUST.get(),
            HumanCommonItemTags.DUSTS_AUTUNITE,
            CoreBlocks.AUTUNITE_BLOCK.get()
        );
        RecipeUtil.createCompressedBlockRecipes3x3TagFriendlyWithDiscriminator(
            builder,
            HumanItems.LITHIUM_DUST.get(),
            HumanCommonItemTags.DUSTS_LITHIUM,
            CoreBlocks.LITHIUM_BLOCK.get()
        );
        RecipeUtil.createCompressedBlockRecipes3x3TagFriendlyWithDiscriminator(
            builder,
            HumanItems.RAW_BAUXITE.get(),
            HumanCommonItemTags.RAW_MATERIALS_ALUMINUM,
            CoreBlocks.RAW_BAUXITE_BLOCK.get()
        );
        RecipeUtil.createCompressedBlockRecipes3x3TagFriendlyWithDiscriminator(
            builder,
            HumanItems.RAW_GALENA.get(),
            HumanCommonItemTags.RAW_MATERIALS_LEAD,
            CoreBlocks.RAW_GALENA_BLOCK.get()
        );
        RecipeUtil.createCompressedBlockRecipes3x3TagFriendlyWithDiscriminator(
            builder,
            HumanItems.RAW_MONAZITE.get(),
            HumanCommonItemTags.RAW_MATERIALS_MONAZITE,
            CoreBlocks.RAW_MONAZITE_BLOCK.get()
        );
        RecipeUtil.createCompressedBlockRecipes3x3TagFriendlyWithDiscriminator(
            builder,
            HumanItems.RAW_TITANIUM.get(),
            HumanCommonItemTags.RAW_MATERIALS_TITANIUM,
            CoreBlocks.RAW_TITANIUM_BLOCK.get()
        );
        RecipeUtil.createCompressedBlockRecipes3x3TagFriendlyWithDiscriminator(
            builder,
            HumanItems.RAW_ZINC.get(),
            HumanCommonItemTags.RAW_MATERIALS_ZINC,
            CoreBlocks.RAW_ZINC_BLOCK.get()
        );
        RecipeUtil.createCompressedBlockRecipes3x3TagFriendlyWithDiscriminator(
            builder,
            HumanItems.SILICON.get(),
            HumanCommonItemTags.SILICON,
            CoreBlocks.SILICON_BLOCK.get()
        );

        builder.shaped()
            .withCategory(RecipeCategory.BUILDING_BLOCKS)
            .apply(RecipeTemplates.BARS_BLOCK.apply(HumanCommonItemTags.INGOTS_STEEL))
            .into(16, HumanSteelBlocks.STEEL_BARS);

        builder.shaped()
            .withCategory(RecipeCategory.BUILDING_BLOCKS)
            .apply(RecipeTemplates.BUTTON_BLOCK.apply(HumanCommonItemTags.INGOTS_FERROALUMINUM))
            .into(2, HumanFerroaluminumBlocks.FERROALUMINUM_BUTTON);
        builder.shaped()
            .withCategory(RecipeCategory.BUILDING_BLOCKS)
            .apply(RecipeTemplates.BUTTON_BLOCK.apply(HumanCommonItemTags.INGOTS_STEEL))
            .into(2, HumanSteelBlocks.STEEL_BUTTON);
        builder.shaped()
            .withCategory(RecipeCategory.BUILDING_BLOCKS)
            .apply(RecipeTemplates.BUTTON_BLOCK.apply(HumanCommonItemTags.INGOTS_COMPOSITE))
            .into(2, HumanCompositeBlocks.COMPOSITE_BUTTON);
        builder.shaped()
            .withCategory(RecipeCategory.BUILDING_BLOCKS)
            .apply(RecipeTemplates.BUTTON_BLOCK.apply(HumanCommonItemTags.INGOTS_TITANIUM))
            .into(2, HumanTitaniumBlocks.TITANIUM_BUTTON);
        builder.shaped()
            .withCategory(RecipeCategory.BUILDING_BLOCKS)
            .apply(RecipeTemplates.DOOR_BLOCK_TAG_FRIENDLY.apply(HumanCommonItemTags.INGOTS_FERROALUMINUM))
            .into(3, HumanFerroaluminumBlocks.FERROALUMINUM_DOOR);
        builder.shaped()
            .withCategory(RecipeCategory.BUILDING_BLOCKS)
            .apply(RecipeTemplates.DOOR_BLOCK_TAG_FRIENDLY.apply(HumanCommonItemTags.INGOTS_STEEL))
            .into(3, HumanSteelBlocks.STEEL_DOOR);
        builder.shaped()
            .withCategory(RecipeCategory.BUILDING_BLOCKS)
            .apply(RecipeTemplates.DOOR_BLOCK_TAG_FRIENDLY.apply(HumanCommonItemTags.INGOTS_COMPOSITE))
            .into(3, HumanCompositeBlocks.COMPOSITE_DOOR);
        builder.shaped()
            .withCategory(RecipeCategory.BUILDING_BLOCKS)
            .apply(RecipeTemplates.DOOR_BLOCK_TAG_FRIENDLY.apply(HumanCommonItemTags.INGOTS_TITANIUM))
            .into(3, HumanTitaniumBlocks.TITANIUM_DOOR);

        builder.shaped()
            .withCategory(RecipeCategory.BUILDING_BLOCKS)
            .apply(RecipeTemplates.TRAP_DOOR_BLOCK_TAG_FRIENDLY.apply(HumanCommonItemTags.INGOTS_FERROALUMINUM))
            .into(2, HumanFerroaluminumBlocks.FERROALUMINUM_TRAP_DOOR);
        builder.shaped()
            .withCategory(RecipeCategory.BUILDING_BLOCKS)
            .apply(RecipeTemplates.TRAP_DOOR_BLOCK_TAG_FRIENDLY.apply(HumanCommonItemTags.INGOTS_STEEL))
            .into(2, HumanSteelBlocks.STEEL_TRAP_DOOR);
        builder.shaped()
            .withCategory(RecipeCategory.BUILDING_BLOCKS)
            .apply(RecipeTemplates.TRAP_DOOR_BLOCK_TAG_FRIENDLY.apply(HumanCommonItemTags.INGOTS_COMPOSITE))
            .into(2, HumanCompositeBlocks.COMPOSITE_TRAP_DOOR);
        builder.shaped()
            .withCategory(RecipeCategory.BUILDING_BLOCKS)
            .apply(RecipeTemplates.TRAP_DOOR_BLOCK_TAG_FRIENDLY.apply(HumanCommonItemTags.INGOTS_TITANIUM))
            .into(2, HumanTitaniumBlocks.TITANIUM_TRAP_DOOR);

        builder.shaped()
            .withCategory(RecipeCategory.BUILDING_BLOCKS)
            .apply(RecipeTemplates.PRESSURE_PLATE_BLOCK.apply(HumanCommonItemTags.INGOTS_FERROALUMINUM))
            .into(1, HumanFerroaluminumBlocks.FERROALUMINUM_PRESSURE_PLATE);
        builder.shaped()
            .withCategory(RecipeCategory.BUILDING_BLOCKS)
            .apply(RecipeTemplates.PRESSURE_PLATE_BLOCK.apply(HumanCommonItemTags.INGOTS_STEEL))
            .into(1, HumanSteelBlocks.STEEL_PRESSURE_PLATE);
        builder.shaped()
            .withCategory(RecipeCategory.BUILDING_BLOCKS)
            .apply(RecipeTemplates.PRESSURE_PLATE_BLOCK.apply(HumanCommonItemTags.INGOTS_COMPOSITE))
            .into(1, HumanCompositeBlocks.COMPOSITE_PRESSURE_PLATE);
        builder.shaped()
            .withCategory(RecipeCategory.BUILDING_BLOCKS)
            .apply(RecipeTemplates.PRESSURE_PLATE_BLOCK.apply(HumanCommonItemTags.INGOTS_TITANIUM))
            .into(1, HumanTitaniumBlocks.TITANIUM_PRESSURE_PLATE);

        // Add standard slab and stair crafting recipes
        createStandardSlabRecipe(
            builder,
            HumanFerroaluminumBlocks.FERROALUMINUM_BLOCK.get(),
            HumanFerroaluminumBlocks.FERROALUMINUM_SLAB.get()
        );
        createStandardStairRecipe(
            builder,
            HumanFerroaluminumBlocks.FERROALUMINUM_BLOCK.get(),
            HumanFerroaluminumBlocks.FERROALUMINUM_STAIRS.get()
        );

        createStandardSlabRecipe(builder, HumanSteelBlocks.STEEL_BLOCK.get(), HumanSteelBlocks.STEEL_SLAB.get());
        createStandardStairRecipe(builder, HumanSteelBlocks.STEEL_BLOCK.get(), HumanSteelBlocks.STEEL_STAIRS.get());

        createStandardSlabRecipe(builder, HumanCompositeBlocks.COMPOSITE_BLOCK.get(), HumanCompositeBlocks.COMPOSITE_SLAB.get());
        createStandardStairRecipe(builder, HumanCompositeBlocks.COMPOSITE_BLOCK.get(), HumanCompositeBlocks.COMPOSITE_STAIRS.get());

        createStandardSlabRecipe(builder, HumanTitaniumBlocks.TITANIUM_BLOCK.get(), HumanTitaniumBlocks.TITANIUM_SLAB.get());
        createStandardStairRecipe(builder, HumanTitaniumBlocks.TITANIUM_BLOCK.get(), HumanTitaniumBlocks.TITANIUM_STAIRS.get());

        createFerroaluminumBlockVariantRecipes(builder);
        createSteelBlockVariantRecipes(builder);
        createCompositeBlockVariantRecipes(builder);
        createTitaniumBlockVariantRecipes(builder);

        // Add variant slab and stair recipes
        createVariantSlabAndStairRecipes(builder);

        // Nugget to ingot recipes
        nuggetToIngot(builder, HumanItems.ALUMINUM_NUGGET.get(), HumanCommonItemTags.NUGGETS_ALUMINUM, HumanItems.ALUMINUM_INGOT.get());
        nuggetToIngot(builder, HumanItems.BRASS_NUGGET.get(), HumanCommonItemTags.NUGGETS_BRASS, HumanItems.BRASS_INGOT.get());
        nuggetToIngot(
            builder,
            HumanItems.FERROALUMINUM_NUGGET.get(),
            HumanCommonItemTags.NUGGETS_FERROALUMINUM,
            HumanItems.FERROALUMINUM_INGOT.get()
        );
        nuggetToIngot(builder, HumanItems.LEAD_NUGGET.get(), HumanCommonItemTags.NUGGETS_LEAD, HumanItems.LEAD_INGOT.get());
        nuggetToIngot(builder, HumanItems.STEEL_NUGGET.get(), HumanCommonItemTags.NUGGETS_STEEL, HumanItems.STEEL_INGOT.get());
        nuggetToIngot(builder, HumanItems.TITANIUM_NUGGET.get(), HumanCommonItemTags.NUGGETS_TITANIUM, HumanItems.TITANIUM_INGOT.get());
        nuggetToIngot(builder, HumanItems.URANIUM_NUGGET.get(), HumanCommonItemTags.NUGGETS_URANIUM, HumanItems.URANIUM_INGOT.get());
        nuggetToIngot(builder, HumanItems.ZINC_NUGGET.get(), HumanCommonItemTags.NUGGETS_ZINC, HumanItems.ZINC_INGOT.get());
        nuggetToIngot(
            builder,
            HumanItems.COPPER_NUGGET.get(),
            HumanCommonItemTags.NUGGETS_COPPER,
            net.minecraft.world.item.Items.COPPER_INGOT
        );

        // Ingot to nugget recipes
        ingotToNugget(builder, HumanItems.ALUMINUM_INGOT.get(), HumanItems.ALUMINUM_NUGGET.get());
        ingotToNugget(builder, HumanItems.BRASS_INGOT.get(), HumanItems.BRASS_NUGGET.get());
        ingotToNugget(builder, HumanItems.FERROALUMINUM_INGOT.get(), HumanItems.FERROALUMINUM_NUGGET.get());
        ingotToNugget(builder, HumanItems.LEAD_INGOT.get(), HumanItems.LEAD_NUGGET.get());
        ingotToNugget(builder, HumanItems.STEEL_INGOT.get(), HumanItems.STEEL_NUGGET.get());
        ingotToNugget(builder, HumanItems.TITANIUM_INGOT.get(), HumanItems.TITANIUM_NUGGET.get());
        ingotToNugget(builder, HumanItems.URANIUM_INGOT.get(), HumanItems.URANIUM_NUGGET.get());
        ingotToNugget(builder, HumanItems.ZINC_INGOT.get(), HumanItems.ZINC_NUGGET.get());
        ingotToNugget(builder, net.minecraft.world.item.Items.COPPER_INGOT, HumanItems.COPPER_NUGGET.get());

        metalFixtures(builder);
        metalSigns(builder);
    }

    /**
     * [stated] Oct 23 - metal signs, on vanilla's patterns with ingots for planks: six ingots over a stick make three
     * signs; two of the metal's own chains over six ingots make six hanging signs.
     */
    private static void metalSigns(RecipeBuilder builder) {
        signRecipes(
            builder,
            com.human.common.registry.init.HumanSigns.STEEL,
            HumanCommonItemTags.INGOTS_STEEL,
            com.human.common.registry.init.HumanMetalFixtures.STEEL_CHAIN
        );
        signRecipes(
            builder,
            com.human.common.registry.init.HumanSigns.TITANIUM,
            HumanCommonItemTags.INGOTS_TITANIUM,
            com.human.common.registry.init.HumanMetalFixtures.TITANIUM_CHAIN
        );
        signRecipes(
            builder,
            com.human.common.registry.init.HumanSigns.FERROALUMINUM,
            HumanCommonItemTags.INGOTS_FERROALUMINUM,
            com.human.common.registry.init.HumanMetalFixtures.FERROALUMINUM_CHAIN
        );
        signRecipes(
            builder,
            com.human.common.registry.init.HumanSigns.ALUMINUM,
            HumanCommonItemTags.INGOTS_ALUMINUM,
            com.human.common.registry.init.HumanMetalFixtures.ALUMINUM_CHAIN
        );
        signRecipes(
            builder,
            com.human.common.registry.init.HumanSigns.COMPOSITE,
            HumanCommonItemTags.INGOTS_COMPOSITE,
            com.human.common.registry.init.HumanMetalFixtures.COMPOSITE_CHAIN
        );
    }

    private static void signRecipes(
        RecipeBuilder builder,
        com.human.common.registry.init.HumanSigns.SignSet set,
        TagKey<Item> ingots,
        com.blib.api.common.registry.v1.BLibHolder<net.minecraft.world.level.block.Block> chain
    ) {
        builder.shaped()
            .withCategory(RecipeCategory.DECORATIONS)
            .define('I', ingots)
            .define('S', net.minecraft.world.item.Items.STICK)
            .pattern("III")
            .pattern("III")
            .pattern(" S ")
            .into(3, set.signItem());

        builder.shaped()
            .withCategory(RecipeCategory.DECORATIONS)
            .define('C', chain.get())
            .define('I', ingots)
            .pattern("C C")
            .pattern("III")
            .pattern("III")
            .into(6, set.hangingSignItem());
    }

    /**
     * [stated] Oct 21 - bars, chains and ladders for the five metals, and the composite nugget. Bars: six ingots -> 16,
     * like iron bars (no clash: the chain fences are stonecut from blocks). Chain: nugget, ingot, nugget, like vanilla.
     * Ladder: an H of seven ingots -> 8.
     */
    private static void metalFixtures(RecipeBuilder builder) {
        TagKey<Item> COMPOSITE_NUGGETS = TagKey.create(
            net.minecraft.core.registries.Registries.ITEM,
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("c", "nuggets/composite")
        );

        builder.shaped()
            .withCategory(RecipeCategory.DECORATIONS)
            .define('N', HumanCommonItemTags.NUGGETS_STEEL)
            .define('I', HumanCommonItemTags.INGOTS_STEEL)
            .pattern("N")
            .pattern("I")
            .pattern("N")
            .into(1, com.human.common.registry.init.HumanMetalFixtures.STEEL_CHAIN);

        builder.shaped()
            .withCategory(RecipeCategory.DECORATIONS)
            .define('I', HumanCommonItemTags.INGOTS_STEEL)
            .pattern("I I")
            .pattern("III")
            .pattern("I I")
            .into(8, com.human.common.registry.init.HumanMetalFixtures.STEEL_LADDER);

        builder.shaped()
            .withCategory(RecipeCategory.BUILDING_BLOCKS)
            .apply(RecipeTemplates.BARS_BLOCK.apply(HumanCommonItemTags.INGOTS_TITANIUM))
            .into(16, com.human.common.registry.init.HumanMetalFixtures.TITANIUM_BARS);

        builder.shaped()
            .withCategory(RecipeCategory.DECORATIONS)
            .define('N', HumanCommonItemTags.NUGGETS_TITANIUM)
            .define('I', HumanCommonItemTags.INGOTS_TITANIUM)
            .pattern("N")
            .pattern("I")
            .pattern("N")
            .into(1, com.human.common.registry.init.HumanMetalFixtures.TITANIUM_CHAIN);

        builder.shaped()
            .withCategory(RecipeCategory.DECORATIONS)
            .define('I', HumanCommonItemTags.INGOTS_TITANIUM)
            .pattern("I I")
            .pattern("III")
            .pattern("I I")
            .into(8, com.human.common.registry.init.HumanMetalFixtures.TITANIUM_LADDER);

        builder.shaped()
            .withCategory(RecipeCategory.BUILDING_BLOCKS)
            .apply(RecipeTemplates.BARS_BLOCK.apply(HumanCommonItemTags.INGOTS_FERROALUMINUM))
            .into(16, com.human.common.registry.init.HumanMetalFixtures.FERROALUMINUM_BARS);

        builder.shaped()
            .withCategory(RecipeCategory.DECORATIONS)
            .define('N', HumanCommonItemTags.NUGGETS_FERROALUMINUM)
            .define('I', HumanCommonItemTags.INGOTS_FERROALUMINUM)
            .pattern("N")
            .pattern("I")
            .pattern("N")
            .into(1, com.human.common.registry.init.HumanMetalFixtures.FERROALUMINUM_CHAIN);

        builder.shaped()
            .withCategory(RecipeCategory.DECORATIONS)
            .define('I', HumanCommonItemTags.INGOTS_FERROALUMINUM)
            .pattern("I I")
            .pattern("III")
            .pattern("I I")
            .into(8, com.human.common.registry.init.HumanMetalFixtures.FERROALUMINUM_LADDER);

        builder.shaped()
            .withCategory(RecipeCategory.BUILDING_BLOCKS)
            .apply(RecipeTemplates.BARS_BLOCK.apply(HumanCommonItemTags.INGOTS_ALUMINUM))
            .into(16, com.human.common.registry.init.HumanMetalFixtures.ALUMINUM_BARS);

        builder.shaped()
            .withCategory(RecipeCategory.DECORATIONS)
            .define('N', HumanCommonItemTags.NUGGETS_ALUMINUM)
            .define('I', HumanCommonItemTags.INGOTS_ALUMINUM)
            .pattern("N")
            .pattern("I")
            .pattern("N")
            .into(1, com.human.common.registry.init.HumanMetalFixtures.ALUMINUM_CHAIN);

        builder.shaped()
            .withCategory(RecipeCategory.DECORATIONS)
            .define('I', HumanCommonItemTags.INGOTS_ALUMINUM)
            .pattern("I I")
            .pattern("III")
            .pattern("I I")
            .into(8, com.human.common.registry.init.HumanMetalFixtures.ALUMINUM_LADDER);

        builder.shaped()
            .withCategory(RecipeCategory.BUILDING_BLOCKS)
            .apply(RecipeTemplates.BARS_BLOCK.apply(HumanCommonItemTags.INGOTS_COMPOSITE))
            .into(16, com.human.common.registry.init.HumanMetalFixtures.COMPOSITE_BARS);

        builder.shaped()
            .withCategory(RecipeCategory.DECORATIONS)
            .define('N', COMPOSITE_NUGGETS)
            .define('I', HumanCommonItemTags.INGOTS_COMPOSITE)
            .pattern("N")
            .pattern("I")
            .pattern("N")
            .into(1, com.human.common.registry.init.HumanMetalFixtures.COMPOSITE_CHAIN);

        builder.shaped()
            .withCategory(RecipeCategory.DECORATIONS)
            .define('I', HumanCommonItemTags.INGOTS_COMPOSITE)
            .pattern("I I")
            .pattern("III")
            .pattern("I I")
            .into(8, com.human.common.registry.init.HumanMetalFixtures.COMPOSITE_LADDER);

        // ⚠ Oct 22 - NOT nuggetToIngot: that names the recipe after its output, and "avp_human:composite_ingot" is
        // already the recipe that MAKES composite ingots - datagen stopped with "Duplicate recipe". Same 3x3 of
        // nuggets,
        // under its own name.
        builder.shaped()
            .withCategory(RecipeCategory.BUILDING_BLOCKS)
            .withCustomName(name -> name + "_from_nuggets")
            .define('A', com.human.common.registry.init.HumanMetalFixtures.COMPOSITE_NUGGET.get())
            .define('B', com.human.common.registry.init.HumanMetalFixtures.COMPOSITE_NUGGET.get())
            .pattern("BBB")
            .pattern("BAB")
            .pattern("BBB")
            .into(1, HumanItems.COMPOSITE_INGOT.get());
        ingotToNugget(builder, HumanItems.COMPOSITE_INGOT.get(), com.human.common.registry.init.HumanMetalFixtures.COMPOSITE_NUGGET.get());
    }

    private static void createFerroaluminumBlockVariantRecipes(RecipeBuilder builder) {
        var ferroaluminumBaseBuilder = builder.stonecut(HumanFerroaluminumBlocks.FERROALUMINUM_BLOCK)
            .withCategory(RecipeCategory.BUILDING_BLOCKS);

        ferroaluminumBaseBuilder.into(4, HumanFerroaluminumBlocks.CHISELED_FERROALUMINUM);
        ferroaluminumBaseBuilder.into(4, HumanFerroaluminumBlocks.CUT_FERROALUMINUM);
        ferroaluminumBaseBuilder.into(8, HumanFerroaluminumBlocks.CUT_FERROALUMINUM_SLAB);
        ferroaluminumBaseBuilder.into(4, HumanFerroaluminumBlocks.CUT_FERROALUMINUM_STAIRS);
        ferroaluminumBaseBuilder.into(16, HumanFerroaluminumBlocks.FERROALUMINUM_CHAIN_FENCE);
        ferroaluminumBaseBuilder.into(4, HumanFerroaluminumBlocks.FERROALUMINUM_COLUMN);
        ferroaluminumBaseBuilder.into(4, HumanFerroaluminumBlocks.FERROALUMINUM_FASTENED_SIDING);
        ferroaluminumBaseBuilder.into(4, HumanFerroaluminumBlocks.FERROALUMINUM_FASTENED_STANDING);
        ferroaluminumBaseBuilder.into(4, HumanFerroaluminumBlocks.FERROALUMINUM_GRATE);
        ferroaluminumBaseBuilder.into(4, HumanFerroaluminumBlocks.FERROALUMINUM_PLATING);
        ferroaluminumBaseBuilder.into(4, HumanFerroaluminumBlocks.FERROALUMINUM_SIDING);
        ferroaluminumBaseBuilder.into(4, HumanFerroaluminumBlocks.FERROALUMINUM_STANDING);
        ferroaluminumBaseBuilder.into(4, HumanFerroaluminumBlocks.FERROALUMINUM_TREAD);
        ferroaluminumBaseBuilder.into(2, HumanFerroaluminumBlocks.FERROALUMINUM_SLAB);
        ferroaluminumBaseBuilder.into(1, HumanFerroaluminumBlocks.FERROALUMINUM_STAIRS);

        var cutFerroaluminumBuilder = builder.stonecut(HumanFerroaluminumBlocks.CUT_FERROALUMINUM)
            .withCategory(RecipeCategory.BUILDING_BLOCKS);

        cutFerroaluminumBuilder.into(2, HumanFerroaluminumBlocks.CUT_FERROALUMINUM_SLAB);
        cutFerroaluminumBuilder.into(1, HumanFerroaluminumBlocks.CUT_FERROALUMINUM_STAIRS);
    }

    private static void createSteelBlockVariantRecipes(RecipeBuilder builder) {
        var steelBaseBuilder = builder.stonecut(HumanSteelBlocks.STEEL_BLOCK.get())
            .withCategory(RecipeCategory.BUILDING_BLOCKS);

        steelBaseBuilder.into(4, HumanSteelBlocks.CHISELED_STEEL);
        steelBaseBuilder.into(4, HumanSteelBlocks.CUT_STEEL);
        steelBaseBuilder.into(8, HumanSteelBlocks.CUT_STEEL_SLAB);
        steelBaseBuilder.into(4, HumanSteelBlocks.CUT_STEEL_STAIRS);
        steelBaseBuilder.into(16, HumanSteelBlocks.STEEL_CHAIN_FENCE);
        steelBaseBuilder.into(4, HumanSteelBlocks.STEEL_COLUMN);
        steelBaseBuilder.into(4, HumanSteelBlocks.STEEL_FASTENED_SIDING);
        steelBaseBuilder.into(4, HumanSteelBlocks.STEEL_FASTENED_STANDING);
        steelBaseBuilder.into(4, HumanSteelBlocks.STEEL_GRATE);
        steelBaseBuilder.into(4, HumanSteelBlocks.STEEL_PLATING);
        steelBaseBuilder.into(4, HumanSteelBlocks.STEEL_SIDING);
        steelBaseBuilder.into(4, HumanSteelBlocks.STEEL_STANDING);
        steelBaseBuilder.into(4, HumanSteelBlocks.STEEL_TREAD);
        steelBaseBuilder.into(2, HumanSteelBlocks.STEEL_SLAB.get());
        steelBaseBuilder.into(1, HumanSteelBlocks.STEEL_STAIRS.get());

        var cutSteelBuilder = builder.stonecut(HumanSteelBlocks.CUT_STEEL)
            .withCategory(RecipeCategory.BUILDING_BLOCKS);

        cutSteelBuilder.into(2, HumanSteelBlocks.CUT_STEEL_SLAB);
        cutSteelBuilder.into(1, HumanSteelBlocks.CUT_STEEL_STAIRS);
    }

    private static void createCompositeBlockVariantRecipes(RecipeBuilder builder) {
        var compositeBaseBuilder = builder.stonecut(HumanCompositeBlocks.COMPOSITE_BLOCK.get())
            .withCategory(RecipeCategory.BUILDING_BLOCKS);

        compositeBaseBuilder.into(4, HumanCompositeBlocks.CHISELED_COMPOSITE);
        compositeBaseBuilder.into(4, HumanCompositeBlocks.CUT_COMPOSITE);
        compositeBaseBuilder.into(8, HumanCompositeBlocks.CUT_COMPOSITE_SLAB);
        compositeBaseBuilder.into(4, HumanCompositeBlocks.CUT_COMPOSITE_STAIRS);
        compositeBaseBuilder.into(16, HumanCompositeBlocks.COMPOSITE_CHAIN_FENCE);
        compositeBaseBuilder.into(4, HumanCompositeBlocks.COMPOSITE_COLUMN);
        compositeBaseBuilder.into(4, HumanCompositeBlocks.COMPOSITE_FASTENED_SIDING);
        compositeBaseBuilder.into(4, HumanCompositeBlocks.COMPOSITE_FASTENED_STANDING);
        compositeBaseBuilder.into(4, HumanCompositeBlocks.COMPOSITE_GRATE);
        compositeBaseBuilder.into(4, HumanCompositeBlocks.COMPOSITE_PLATING);
        compositeBaseBuilder.into(4, HumanCompositeBlocks.COMPOSITE_SIDING);
        compositeBaseBuilder.into(4, HumanCompositeBlocks.COMPOSITE_STANDING);
        compositeBaseBuilder.into(4, HumanCompositeBlocks.COMPOSITE_TREAD);
        compositeBaseBuilder.into(2, HumanCompositeBlocks.COMPOSITE_SLAB.get());
        compositeBaseBuilder.into(1, HumanCompositeBlocks.COMPOSITE_STAIRS.get());

        var cutCompositeBuilder = builder.stonecut(HumanCompositeBlocks.CUT_COMPOSITE)
            .withCategory(RecipeCategory.BUILDING_BLOCKS);

        cutCompositeBuilder.into(2, HumanCompositeBlocks.CUT_COMPOSITE_SLAB);
        cutCompositeBuilder.into(1, HumanCompositeBlocks.CUT_COMPOSITE_STAIRS);
    }

    private static void createTitaniumBlockVariantRecipes(RecipeBuilder builder) {
        var titaniumBaseBuilder = builder.stonecut(HumanTitaniumBlocks.TITANIUM_BLOCK)
            .withCategory(RecipeCategory.BUILDING_BLOCKS);

        titaniumBaseBuilder.into(4, HumanTitaniumBlocks.CHISELED_TITANIUM);
        titaniumBaseBuilder.into(4, HumanTitaniumBlocks.CUT_TITANIUM);
        titaniumBaseBuilder.into(8, HumanTitaniumBlocks.CUT_TITANIUM_SLAB);
        titaniumBaseBuilder.into(4, HumanTitaniumBlocks.CUT_TITANIUM_STAIRS);
        titaniumBaseBuilder.into(16, HumanTitaniumBlocks.TITANIUM_CHAIN_FENCE);
        titaniumBaseBuilder.into(4, HumanTitaniumBlocks.TITANIUM_COLUMN);
        titaniumBaseBuilder.into(4, HumanTitaniumBlocks.TITANIUM_FASTENED_SIDING);
        titaniumBaseBuilder.into(4, HumanTitaniumBlocks.TITANIUM_FASTENED_STANDING);
        titaniumBaseBuilder.into(4, HumanTitaniumBlocks.TITANIUM_GRATE);
        titaniumBaseBuilder.into(4, HumanTitaniumBlocks.TITANIUM_PLATING);
        titaniumBaseBuilder.into(4, HumanTitaniumBlocks.TITANIUM_SIDING);
        titaniumBaseBuilder.into(4, HumanTitaniumBlocks.TITANIUM_STANDING);
        titaniumBaseBuilder.into(4, HumanTitaniumBlocks.TITANIUM_TREAD);
        titaniumBaseBuilder.into(2, HumanTitaniumBlocks.TITANIUM_SLAB);
        titaniumBaseBuilder.into(1, HumanTitaniumBlocks.TITANIUM_STAIRS);

        var cutTitaniumBuilder = builder.stonecut(HumanTitaniumBlocks.CUT_TITANIUM)
            .withCategory(RecipeCategory.BUILDING_BLOCKS);

        cutTitaniumBuilder.into(2, HumanTitaniumBlocks.CUT_TITANIUM_SLAB);
        cutTitaniumBuilder.into(1, HumanTitaniumBlocks.CUT_TITANIUM_STAIRS);
    }

    private static void createVariantSlabAndStairRecipes(RecipeBuilder builder) {
        // Ferroaluminum variant slabs and stairs
        addVariantSlabAndStairRecipes(
            builder,
            HumanFerroaluminumBlocks.FERROALUMINUM_SIDING,
            HumanFerroaluminumBlocks.FERROALUMINUM_SIDING_SLAB,
            HumanFerroaluminumBlocks.FERROALUMINUM_SIDING_STAIRS
        );

        addVariantSlabAndStairRecipes(
            builder,
            HumanFerroaluminumBlocks.FERROALUMINUM_STANDING,
            HumanFerroaluminumBlocks.FERROALUMINUM_STANDING_SLAB,
            HumanFerroaluminumBlocks.FERROALUMINUM_STANDING_STAIRS
        );

        addVariantSlabAndStairRecipes(
            builder,
            HumanFerroaluminumBlocks.FERROALUMINUM_FASTENED_SIDING,
            HumanFerroaluminumBlocks.FERROALUMINUM_FASTENED_SIDING_SLAB,
            HumanFerroaluminumBlocks.FERROALUMINUM_FASTENED_SIDING_STAIRS
        );

        addVariantSlabAndStairRecipes(
            builder,
            HumanFerroaluminumBlocks.FERROALUMINUM_FASTENED_STANDING,
            HumanFerroaluminumBlocks.FERROALUMINUM_FASTENED_STANDING_SLAB,
            HumanFerroaluminumBlocks.FERROALUMINUM_FASTENED_STANDING_STAIRS
        );

        addVariantSlabAndStairRecipes(
            builder,
            HumanFerroaluminumBlocks.FERROALUMINUM_PLATING,
            HumanFerroaluminumBlocks.FERROALUMINUM_PLATING_SLAB,
            HumanFerroaluminumBlocks.FERROALUMINUM_PLATING_STAIRS
        );

        addVariantSlabAndStairRecipes(
            builder,
            HumanFerroaluminumBlocks.FERROALUMINUM_TREAD,
            HumanFerroaluminumBlocks.FERROALUMINUM_TREAD_SLAB,
            HumanFerroaluminumBlocks.FERROALUMINUM_TREAD_STAIRS
        );

        addVariantSlabAndStairRecipes(
            builder,
            HumanFerroaluminumBlocks.FERROALUMINUM_GRATE,
            HumanFerroaluminumBlocks.FERROALUMINUM_GRATE_SLAB,
            HumanFerroaluminumBlocks.FERROALUMINUM_GRATE_STAIRS
        );

        // Steel variant slabs and stairs
        addVariantSlabAndStairRecipes(
            builder,
            HumanSteelBlocks.STEEL_SIDING,
            HumanSteelBlocks.STEEL_SIDING_SLAB,
            HumanSteelBlocks.STEEL_SIDING_STAIRS
        );

        addVariantSlabAndStairRecipes(
            builder,
            HumanSteelBlocks.STEEL_STANDING,
            HumanSteelBlocks.STEEL_STANDING_SLAB,
            HumanSteelBlocks.STEEL_STANDING_STAIRS
        );

        addVariantSlabAndStairRecipes(
            builder,
            HumanSteelBlocks.STEEL_FASTENED_SIDING,
            HumanSteelBlocks.STEEL_FASTENED_SIDING_SLAB,
            HumanSteelBlocks.STEEL_FASTENED_SIDING_STAIRS
        );

        addVariantSlabAndStairRecipes(
            builder,
            HumanSteelBlocks.STEEL_FASTENED_STANDING,
            HumanSteelBlocks.STEEL_FASTENED_STANDING_SLAB,
            HumanSteelBlocks.STEEL_FASTENED_STANDING_STAIRS
        );

        addVariantSlabAndStairRecipes(
            builder,
            HumanSteelBlocks.STEEL_PLATING,
            HumanSteelBlocks.STEEL_PLATING_SLAB,
            HumanSteelBlocks.STEEL_PLATING_STAIRS
        );

        addVariantSlabAndStairRecipes(
            builder,
            HumanSteelBlocks.STEEL_TREAD,
            HumanSteelBlocks.STEEL_TREAD_SLAB,
            HumanSteelBlocks.STEEL_TREAD_STAIRS
        );

        addVariantSlabAndStairRecipes(
            builder,
            HumanSteelBlocks.STEEL_GRATE,
            HumanSteelBlocks.STEEL_GRATE_SLAB,
            HumanSteelBlocks.STEEL_GRATE_STAIRS
        );

        // Composite variant slabs and stairs
        addVariantSlabAndStairRecipes(
            builder,
            HumanCompositeBlocks.COMPOSITE_SIDING,
            HumanCompositeBlocks.COMPOSITE_SIDING_SLAB,
            HumanCompositeBlocks.COMPOSITE_SIDING_STAIRS
        );
        addVariantSlabAndStairRecipes(
            builder,
            HumanCompositeBlocks.COMPOSITE_STANDING,
            HumanCompositeBlocks.COMPOSITE_STANDING_SLAB,
            HumanCompositeBlocks.COMPOSITE_STANDING_STAIRS
        );
        addVariantSlabAndStairRecipes(
            builder,
            HumanCompositeBlocks.COMPOSITE_FASTENED_SIDING,
            HumanCompositeBlocks.COMPOSITE_FASTENED_SIDING_SLAB,
            HumanCompositeBlocks.COMPOSITE_FASTENED_SIDING_STAIRS
        );
        addVariantSlabAndStairRecipes(
            builder,
            HumanCompositeBlocks.COMPOSITE_FASTENED_STANDING,
            HumanCompositeBlocks.COMPOSITE_FASTENED_STANDING_SLAB,
            HumanCompositeBlocks.COMPOSITE_FASTENED_STANDING_STAIRS
        );
        addVariantSlabAndStairRecipes(
            builder,
            HumanCompositeBlocks.COMPOSITE_PLATING,
            HumanCompositeBlocks.COMPOSITE_PLATING_SLAB,
            HumanCompositeBlocks.COMPOSITE_PLATING_STAIRS
        );
        addVariantSlabAndStairRecipes(
            builder,
            HumanCompositeBlocks.COMPOSITE_TREAD,
            HumanCompositeBlocks.COMPOSITE_TREAD_SLAB,
            HumanCompositeBlocks.COMPOSITE_TREAD_STAIRS
        );
        addVariantSlabAndStairRecipes(
            builder,
            HumanCompositeBlocks.COMPOSITE_GRATE,
            HumanCompositeBlocks.COMPOSITE_GRATE_SLAB,
            HumanCompositeBlocks.COMPOSITE_GRATE_STAIRS
        );

        // Titanium variant slabs and stairs
        addVariantSlabAndStairRecipes(
            builder,
            HumanTitaniumBlocks.TITANIUM_SIDING,
            HumanTitaniumBlocks.TITANIUM_SIDING_SLAB,
            HumanTitaniumBlocks.TITANIUM_SIDING_STAIRS
        );

        addVariantSlabAndStairRecipes(
            builder,
            HumanTitaniumBlocks.TITANIUM_STANDING,
            HumanTitaniumBlocks.TITANIUM_STANDING_SLAB,
            HumanTitaniumBlocks.TITANIUM_STANDING_STAIRS
        );

        addVariantSlabAndStairRecipes(
            builder,
            HumanTitaniumBlocks.TITANIUM_FASTENED_SIDING,
            HumanTitaniumBlocks.TITANIUM_FASTENED_SIDING_SLAB,
            HumanTitaniumBlocks.TITANIUM_FASTENED_SIDING_STAIRS
        );

        addVariantSlabAndStairRecipes(
            builder,
            HumanTitaniumBlocks.TITANIUM_FASTENED_STANDING,
            HumanTitaniumBlocks.TITANIUM_FASTENED_STANDING_SLAB,
            HumanTitaniumBlocks.TITANIUM_FASTENED_STANDING_STAIRS
        );

        addVariantSlabAndStairRecipes(
            builder,
            HumanTitaniumBlocks.TITANIUM_PLATING,
            HumanTitaniumBlocks.TITANIUM_PLATING_SLAB,
            HumanTitaniumBlocks.TITANIUM_PLATING_STAIRS
        );

        addVariantSlabAndStairRecipes(
            builder,
            HumanTitaniumBlocks.TITANIUM_TREAD,
            HumanTitaniumBlocks.TITANIUM_TREAD_SLAB,
            HumanTitaniumBlocks.TITANIUM_TREAD_STAIRS
        );

        addVariantSlabAndStairRecipes(
            builder,
            HumanTitaniumBlocks.TITANIUM_GRATE,
            HumanTitaniumBlocks.TITANIUM_GRATE_SLAB,
            HumanTitaniumBlocks.TITANIUM_GRATE_STAIRS
        );
    }

    private static void addVariantSlabAndStairRecipes(
        RecipeBuilder builder,
        Supplier<? extends ItemLike> baseBlockSupplier,
        Supplier<? extends ItemLike> slabSupplier,
        Supplier<? extends ItemLike> stairsSupplier
    ) {
        // Add stonecut recipes for the block variants
        builder.stonecut(baseBlockSupplier)
            .withCategory(RecipeCategory.BUILDING_BLOCKS)
            .into(2, slabSupplier);

        builder.stonecut(baseBlockSupplier)
            .withCategory(RecipeCategory.BUILDING_BLOCKS)
            .into(1, stairsSupplier);

        // Add shaped crafting recipes
        var baseBlock = baseBlockSupplier.get();
        var slab = slabSupplier.get();
        var stairs = stairsSupplier.get();
        createStandardSlabRecipe(builder, baseBlock, slab);
        createStandardStairRecipe(builder, baseBlock, stairs);
    }

    private static void createStandardSlabRecipe(RecipeBuilder builder, ItemLike input, ItemLike output) {
        builder.shaped()
            .withCategory(RecipeCategory.BUILDING_BLOCKS)
            .apply(RecipeTemplates.SLAB_BLOCK.apply(input))
            .into(6, output);
    }

    private static void createStandardStairRecipe(RecipeBuilder builder, ItemLike input, ItemLike output) {
        builder
            .shaped()
            .withCategory(RecipeCategory.BUILDING_BLOCKS)
            .apply(RecipeTemplates.STAIR_BLOCK.apply(input))
            .into(4, output);
    }

    private static void createSmeltAndBlastRecipes(RecipeBuilder builder, ItemLike input, ItemLike output) {
        builder.smelt(input)
            .withCategory(RecipeCategory.MISC)
            .withExperience(RecipeConstants.RARE_SMELT_EXPERIENCE)
            .into(output);

        builder.blast(input)
            .withCategory(RecipeCategory.MISC)
            .withExperience(RecipeConstants.RARE_SMELT_EXPERIENCE)
            .into(output);
    }

    private static void nuggetToIngot(RecipeBuilder builder, ItemLike input, TagKey<Item> itemTagKey, ItemLike output) {
        builder.shaped()
            .withCategory(RecipeCategory.BUILDING_BLOCKS)
            .define('A', input)
            .define('B', input)
            .pattern("BBB")
            .pattern("BAB")
            .pattern("BBB")
            .into(1, output);
    }

    private static void ingotToNugget(RecipeBuilder builder, ItemLike input, ItemLike output) {
        builder.shapeless()
            .withCategory(RecipeCategory.BUILDING_BLOCKS)
            .requires(1, input)
            .into(9, output);
    }
}
