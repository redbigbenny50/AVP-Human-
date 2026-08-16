package com.human.fabric.data.recipe.impl;

import com.blib.api.common.tag.v1.BLibItemTags;
import com.blib.api.common.tag.v1.CommonItemTags;
import com.blib.fabric.data.recipe.RecipeConstants;
import com.blib.fabric.data.recipe.builder.RecipeBuilder;
import com.human.common.registry.init.HumanBlocks;
import com.human.common.registry.init.block.CoreBlocks;
import com.human.common.registry.init.item.HumanItems;
import com.human.compatibility.HumanCommonItemTags;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

public class MiscellaneousRecipeProvider {

    public static void provide(RecipeBuilder builder) {
        builder.shaped()
            .withCategory(RecipeCategory.MISC)
            .define('A', HumanCommonItemTags.INGOTS_ALUMINUM)
            .define('B', HumanItems.SYRINGE)
            .define('C', HumanItems.INTEGRATED_CIRCUIT)
            .define('D', HumanItems.LED_DISPLAY)
            .define('E', HumanItems.REDSTONE_CRYSTAL)
            .pattern("ABA")
            .pattern("CDC")
            .pattern("AEA")
            .into(1, HumanItems.GENE_READER);

        builder.shaped()
            .withCategory(RecipeCategory.MISC)
            .define('A', CommonItemTags.NUGGETS_IRON)
            .define('B', Items.GLASS_BOTTLE)
            .define('C', HumanItems.POLYMER)
            .pattern("A  ")
            .pattern(" B ")
            .pattern("  C")
            .into(1, HumanItems.SYRINGE);

        builder.blast(Blocks.REDSTONE_BLOCK)
            .withCategory(RecipeCategory.MISC)
            .withExperience(RecipeConstants.VERY_COMMON_SMELT_EXPERIENCE)
            .into(HumanItems.REDSTONE_CRYSTAL);

        builder.shaped()
            .withCategory(RecipeCategory.MISC)
            .define('A', Items.LAPIS_LAZULI)
            .define('P', HumanItems.POLYMER)
            .define('D', ItemTags.PLANKS)
            .pattern("A A")
            .pattern("P P")
            .pattern("D D")
            .into(1, HumanBlocks.BLUEPRINT_BLOCK);

        builder.shaped()
            .withCategory(RecipeCategory.MISC)
            .define('A', HumanCommonItemTags.DUSTS_AUTUNITE)
            .define('D', CommonItemTags.GEMS_DIAMOND)
            .define('G', CommonItemTags.INGOTS_GOLD)
            .pattern("GDG")
            .pattern("AAA")
            .pattern("GDG")
            .into(1, HumanItems.NUCLEAR_BATTERY);

        // builder.shaped()
        // .withCategory(RecipeCategory.MISC)
        // .define('S', HumanItems.SPEAKER)
        // .define('D', HumanItems.SERVO)
        // .define('A', HumanItems.STEEL_INGOT)
        // .pattern("SAS")
        // .pattern("SDS")
        // .pattern("AAA")
        // .into(1, AVPBlocks.RESONATOR_BLOCK);

        builder.shaped()
            .withCategory(RecipeCategory.MISC)
            .define('A', ItemTags.COALS)
            .pattern("A")
            .into(2, HumanItems.CARBON_DUST);

        // CORNBREAD. Marines spawn carrying it and it is the only thing that tames a marine dog, but until now it had
        // no survival source at all - creative menu or looting a corpse. Wheat, egg and milk are the bread of it; the
        // yellow dye is the point, because nothing in this world grows maize and the colour is out of a packet.
        //
        // ⚠ Shapeless deliberately. It is field rations, not a construction, and a fixed grid pattern for four loose
        // ingredients is busywork. The milk bucket returns as an empty bucket on its own - vanilla handles that through
        // the item's crafting remainder, so no special handling is needed here.
        builder.shapeless()
            .withCategory(RecipeCategory.FOOD)
            .requires(1, Items.WHEAT)
            .requires(1, Items.EGG)
            .requires(1, Items.MILK_BUCKET)
            .requires(1, Items.YELLOW_DYE)
            .into(2, HumanItems.CORNBREAD);

        // ASH. Four balls pack back into a layer (the snowball -> snow relationship), and a packed ash block renders
        // down into grey dye - the one thing fallout ash is actually good for.
        builder.shapeless()
            .withCategory(RecipeCategory.MISC)
            .requires(4, HumanItems.ASH_BALL)
            .into(1, CoreBlocks.ASH_BLOCK);

        builder.shapeless()
            .withCategory(RecipeCategory.MISC)
            .requires(1, CoreBlocks.ASH_BLOCK)
            .into(4, Items.GRAY_DYE);

        builder.shaped()
            .withCategory(RecipeCategory.MISC)
            .define('A', HumanCommonItemTags.INGOTS_ALUMINUM)
            .define('B', HumanItems.POLYMER)
            .define('C', Items.CHEST)
            .pattern("ABA")
            .pattern("BCB")
            .pattern("ABA")
            .into(1, HumanItems.ARMOR_CASE);

        builder.smelt(Items.SLIME_BALL)
            .withCategory(RecipeCategory.MISC)
            .withExperience(RecipeConstants.VERY_COMMON_SMELT_EXPERIENCE)
            .into(HumanItems.POLYMER);

        builder.blast(CoreBlocks.SILICA_GRAVEL)
            .withCategory(RecipeCategory.MISC)
            .withExperience(RecipeConstants.RARE_SMELT_EXPERIENCE)
            .into(HumanItems.SILICON.get());

        builder.shaped()
            .withCategory(RecipeCategory.COMBAT)
            .define('I', BLibItemTags.IRON_INGOT_LIKE)
            .define('N', CommonItemTags.NUGGETS_IRON)
            .pattern("INI")
            .pattern("NIN")
            .pattern("INI")
            .into(16, HumanBlocks.RAZOR_WIRE);

        builder.shaped()
            .withCategory(RecipeCategory.MISC)
            .define('T', HumanCommonItemTags.INGOTS_TITANIUM)
            .define('R', HumanItems.REDSTONE_CRYSTAL)
            .define('P', Items.PISTON)
            .define('B', Items.BUCKET)
            .pattern("TRT")
            .pattern("TPT")
            .pattern("TBT")
            .into(1, HumanItems.CANISTER);

        builder.shaped()
            .withCategory(RecipeCategory.MISC)
            .define('C', Items.CLOCK)
            .define('S', HumanItems.LED_DISPLAY)
            .define('P', HumanItems.CPU)
            .define('L', HumanCommonItemTags.INGOTS_LEAD)
            .define('N', HumanItems.NEODYMIUM_MAGNET)
            .define('T', Items.TNT)
            .pattern("CSP")
            .pattern("LNL")
            .pattern("TTT")
            .into(1, HumanBlocks.NUKE_BLOCK);

        builder.shaped()
            .withCategory(RecipeCategory.MISC)
            .define('S', Items.SMOOTH_STONE)
            .define('F', Items.FURNACE)
            .define('I', BLibItemTags.IRON_INGOT_LIKE)
            .pattern("SSS")
            .pattern("SFS")
            .pattern("III")
            .into(1, HumanBlocks.INDUSTRIAL_FURNACE);
    }
}
