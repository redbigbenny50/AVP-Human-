package com.human.fabric.data.recipe.impl;

import com.blib.api.common.tag.v1.CommonItemTags;
import com.blib.fabric.data.recipe.builder.RecipeBuilder;
import com.human.common.registry.init.item.HumanItems;
import com.human.common.registry.tag.HumanItemTags;
import com.human.compatibility.HumanCommonItemTags;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.world.item.Items;

public class ElectronicItemRecipeProvider {

    public static void provide(RecipeBuilder builder) {
        createElectronicItemRecipes(builder);
    }

    private static void createElectronicItemRecipes(RecipeBuilder builder) {
        builder.shaped()
            .withCategory(RecipeCategory.MISC)
            .define('A', HumanCommonItemTags.INGOTS_ALUMINUM)
            .define('N', HumanItems.NEODYMIUM_MAGNET)
            .define('R', HumanItems.REGULATOR)
            .define('P', HumanItems.POLYMER)
            .pattern("PAP")
            .pattern("ANA")
            .pattern("PRP")
            .into(8, HumanItems.SPEAKER);

        builder.shaped()
            .withCategory(RecipeCategory.MISC)
            .define('B', HumanCommonItemTags.NUGGETS_BRASS)
            .define('C', CommonItemTags.INGOTS_COPPER)
            .define('N', HumanItems.NEODYMIUM_MAGNET)
            .define('I', HumanItems.INTEGRATED_CIRCUIT)
            .pattern("BCB")
            .pattern("NIN")
            .pattern("BCB")
            .into(8, HumanItems.SERVO);

        builder.shaped()
            .withCategory(RecipeCategory.MISC)
            .define('A', CommonItemTags.NUGGETS_GOLD)
            .define('B', CommonItemTags.DUSTS_REDSTONE)
            .define('C', HumanCommonItemTags.SILICON)
            .pattern("AB")
            .pattern(" C")
            .pattern("AB")
            .into(8, HumanItems.RESISTOR);

        builder.shaped()
            .withCategory(RecipeCategory.MISC)
            .define('A', CommonItemTags.NUGGETS_GOLD)
            .define('B', CommonItemTags.DUSTS_REDSTONE)
            .define('C', HumanCommonItemTags.SILICON)
            .pattern(" A ")
            .pattern("BCB")
            .pattern(" A ")
            .into(8, HumanItems.DIODE);

        builder.shaped()
            .withCategory(RecipeCategory.MISC)
            .define('G', CommonItemTags.NUGGETS_GOLD)
            .define('L', Items.LEVER)
            .define('R', CommonItemTags.DUSTS_REDSTONE)
            .define('S', HumanCommonItemTags.SILICON)
            .define('I', HumanItemTags.INDUSTRIAL_GLASS_PANE)
            .pattern("SSS")
            .pattern("ILI")
            .pattern("RGR")
            .into(2, HumanItems.TRANSISTOR);

        builder.shaped()
            .withCategory(RecipeCategory.MISC)
            .define('D', HumanItems.DIODE)
            .define('L', HumanCommonItemTags.INGOTS_LEAD)
            .define('R', HumanItems.RESISTOR)
            .define('S', HumanCommonItemTags.SILICON)
            .pattern(" D ")
            .pattern("SLS")
            .pattern(" R ")
            .into(2, HumanItems.REGULATOR);

        builder.shaped()
            .withCategory(RecipeCategory.MISC)
            .define('G', CommonItemTags.NUGGETS_GOLD)
            .define('L', HumanCommonItemTags.DUSTS_LITHIUM)
            .define('R', CommonItemTags.DUSTS_REDSTONE)
            .define('S', HumanCommonItemTags.SILICON)
            .pattern("GSS")
            .pattern("RLL")
            .pattern("GSS")
            .into(1, HumanItems.CAPACITOR);

        builder.shaped()
            .withCategory(RecipeCategory.MISC)
            .define('D', HumanItems.DIODE)
            .define('L', HumanCommonItemTags.INGOTS_LEAD)
            .define('G', HumanItems.REGULATOR)
            .define('O', CommonItemTags.DUSTS_REDSTONE)
            .define('R', HumanItems.RESISTOR)
            .define('S', HumanCommonItemTags.SILICON)
            .define('T', HumanItems.TRANSISTOR)
            .pattern("TSR")
            .pattern("LOL")
            .pattern("GSD")
            .into(1, HumanItems.INTEGRATED_CIRCUIT);

        builder.shaped()
            .withCategory(RecipeCategory.MISC)
            .define('D', HumanItems.DIODE)
            .define('G', CommonItemTags.NUGGETS_GOLD)
            .define('I', HumanItemTags.INDUSTRIAL_GLASS_PANE)
            .define('R', CommonItemTags.DUSTS_REDSTONE)
            .pattern("G  ")
            .pattern("RDI")
            .pattern("G  ")
            .into(2, HumanItems.LED);

        builder.shaped()
            .withCategory(RecipeCategory.MISC)
            .define('C', HumanItems.INTEGRATED_CIRCUIT)
            .define('I', HumanItemTags.INDUSTRIAL_GLASS_PANE)
            .define('L', HumanItems.LED)
            .define('T', HumanCommonItemTags.DUSTS_LITHIUM)
            .pattern("LTL")
            .pattern("LIL")
            .pattern("LCL")
            .into(1, HumanItems.LED_DISPLAY);

        builder.shaped()
            .withCategory(RecipeCategory.MISC)
            .define('C', HumanItems.INTEGRATED_CIRCUIT)
            .define('R', CommonItemTags.DUSTS_REDSTONE)
            .define('S', HumanCommonItemTags.SILICON)
            .pattern("SCS")
            .pattern("CRC")
            .pattern("SCS")
            .into(1, HumanItems.CPU);

        // [stated] Oct 2: computer parts, made at the electronics workstation.
        builder.shaped()
            .withCategory(RecipeCategory.MISC)
            .define('I', HumanItems.INTEGRATED_CIRCUIT)
            .define('S', HumanCommonItemTags.SILICON)
            .define('T', HumanItems.TRANSISTOR)
            .define('G', CommonItemTags.NUGGETS_GOLD)
            .define('R', CommonItemTags.DUSTS_REDSTONE)
            .pattern("III")
            .pattern("STS")
            .pattern("GRG")
            .into(1, HumanItems.RAM);

        builder.shaped()
            .withCategory(RecipeCategory.MISC)
            .define('R', HumanItems.RAM)
            .define('T', HumanItems.TRANSISTOR)
            .define('I', HumanItems.INTEGRATED_CIRCUIT)
            .define('G', CommonItemTags.NUGGETS_GOLD)
            .define('C', HumanItems.CAPACITOR)
            .pattern("RRR")
            .pattern("TIT")
            .pattern("GCG")
            .into(1, HumanItems.SSD);

        // His layout: I R D / Re S T / C S L - "Re" (resistor) is X here, since a key is one character.
        builder.shaped()
            .withCategory(RecipeCategory.MISC)
            .define('I', HumanItems.INTEGRATED_CIRCUIT)
            .define('R', HumanItems.REGULATOR)
            .define('D', HumanItems.DIODE)
            .define('X', HumanItems.RESISTOR)
            .define('S', HumanCommonItemTags.SILICON)
            .define('T', HumanItems.TRANSISTOR)
            .define('C', HumanItems.CAPACITOR)
            .define('L', HumanItems.LED)
            .pattern("IRD")
            .pattern("XST")
            .pattern("CSL")
            .into(1, HumanItems.MOTHERBOARD);

        // His layout: A F A / Ca M Ca / A C A - "Ca" (capacitor) is P here, C is the copper coil.
        builder.shaped()
            .withCategory(RecipeCategory.MISC)
            .define('A', HumanCommonItemTags.INGOTS_ALUMINUM)
            .define('F', HumanItems.FAN)
            .define('P', HumanItems.CAPACITOR)
            .define('M', HumanItems.NEODYMIUM_MAGNET)
            .define('C', HumanItems.COPPER_COIL)
            .pattern("AFA")
            .pattern("PMP")
            .pattern("ACA")
            .into(1, HumanItems.POWER_SUPPLY);

        builder.shaped()
            .withCategory(RecipeCategory.MISC)
            .define('A', HumanCommonItemTags.INGOTS_ALUMINUM)
            .define('C', HumanItems.CAPACITOR)
            .define('L', HumanCommonItemTags.DUSTS_LITHIUM)
            .pattern("ACA")
            .pattern("LLL")
            .pattern("ACL")
            .into(1, HumanItems.BATTERY_PACK);
    }
}
