package com.human.fabric.data.recipe.impl;

import com.blib.api.common.tag.v1.CommonItemTags;
import com.blib.fabric.data.recipe.builder.RecipeBuilder;
import com.human.common.registry.init.HumanBlocks;
import com.human.common.registry.init.block.CoreBlocks;
import com.human.common.registry.init.block.HumanIndustrialGlassBlocks;
import com.human.common.registry.init.block.HumanTitaniumBlocks;
import com.human.common.registry.init.item.HumanItems;
import com.human.compatibility.HumanCommonItemTags;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.world.level.block.Blocks;

/**
 * The power system's components and the tier-1 generators, exactly as [stated] Sep 24. Every ingredient already
 * existed; the seven components are the only new items. "Silicon Block" is the block registered as
 * {@code raw_silica_block} (CoreBlocks.SILICON_BLOCK). Copper nuggets come from the common tag - the mod has none of
 * its own.
 */
public class PowerRecipeProvider {

    /**
     * BLib's CommonItemTags carries no copper nugget tag; vanilla has no copper nugget either, so this is the common
     * one.
     */
    private static final net.minecraft.tags.TagKey<net.minecraft.world.item.Item> NUGGETS_COPPER = net.minecraft.tags.TagKey.create(
        net.minecraft.core.registries.Registries.ITEM,
        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("c", "nuggets/copper")
    );

    public static void provide(RecipeBuilder builder) {
        provideConsumers(builder);

        // Latex Tap - cheap on purpose; the cost of latex is the 120 s per cauldron level, not the tap.
        builder.shaped()
            .withCategory(RecipeCategory.MISC)
            .define('I', net.minecraft.world.item.Items.IRON_INGOT)
            .define('S', net.minecraft.world.item.Items.STICK)
            .pattern("II ")
            .pattern("  S")
            .into(1, CoreBlocks.LATEX_TAP);

        // Rubber Block <-> Rubber Globule
        builder.shaped()
            .withCategory(RecipeCategory.BUILDING_BLOCKS)
            .define('R', HumanCommonItemTags.RUBBER)
            .pattern("RRR")
            .pattern("RRR")
            .pattern("RRR")
            .into(1, CoreBlocks.RUBBER_BLOCK);

        builder.shapeless()
            .withCategory(RecipeCategory.MISC)
            .requires(1, CoreBlocks.RUBBER_BLOCK)
            .into(9, HumanItems.RUBBER_GLOBULE);

        // Latex Block <-> Latex Sheet - [stated] Sep 24: "latex blocks make 8 latex sheets and vice versa".
        builder.shapeless()
            .withCategory(RecipeCategory.MISC)
            .requires(1, CoreBlocks.LATEX_BLOCK)
            .into(8, HumanItems.LATEX_SHEET);

        builder.shaped()
            .withCategory(RecipeCategory.BUILDING_BLOCKS)
            .define('L', HumanCommonItemTags.LATEX)
            .pattern("LLL")
            .pattern("L L")
            .pattern("LLL")
            .into(1, CoreBlocks.LATEX_BLOCK);

        // Photovoltaic Array
        builder.shaped()
            .withCategory(RecipeCategory.MISC)
            .define('I', HumanIndustrialGlassBlocks.INDUSTRIAL_GLASS)
            .define('S', HumanCommonItemTags.SILICON)
            .define('Z', HumanCommonItemTags.INGOTS_ZINC)
            .define('C', CommonItemTags.INGOTS_COPPER)
            .pattern("III")
            .pattern("SZS")
            .pattern("SCS")
            .into(1, HumanItems.PHOTOVOLTAIC_ARRAY);

        // Turbine
        builder.shaped()
            .withCategory(RecipeCategory.MISC)
            .define('F', HumanCommonItemTags.INGOTS_FERROALUMINUM)
            .define('T', HumanCommonItemTags.INGOTS_TITANIUM)
            .pattern("TFT")
            .pattern("T T")
            .pattern("TFT")
            .into(1, HumanItems.TURBINE);

        // Electric Motor
        builder.shaped()
            .withCategory(RecipeCategory.MISC)
            .define('A', HumanCommonItemTags.INGOTS_ALUMINUM)
            .define('C', HumanItems.COPPER_COIL)
            .define('N', HumanItems.NEODYMIUM_MAGNET)
            .define('S', HumanCommonItemTags.INGOTS_STEEL)
            .pattern("ANA")
            .pattern("SCA")
            .pattern("ANA")
            .into(1, HumanItems.ELECTRIC_MOTOR);

        // Copper Coil - [stated] Sep 24: "copper coils are made from copper thats it". The rubber moved to the
        // cable, where the jacket actually belongs.
        builder.shaped()
            .withCategory(RecipeCategory.MISC)
            .define('C', CommonItemTags.INGOTS_COPPER)
            .pattern(" C ")
            .pattern(" C ")
            .pattern(" C ")
            .into(1, HumanItems.COPPER_COIL);

        // Fan (makes 2)
        builder.shaped()
            .withCategory(RecipeCategory.MISC)
            .define('A', HumanCommonItemTags.INGOTS_ALUMINUM)
            .define('S', HumanItems.SERVO)
            .pattern(" A ")
            .pattern("ASA")
            .pattern(" A ")
            .into(2, HumanItems.FAN);

        // Heat Sink (makes 2)
        builder.shaped()
            .withCategory(RecipeCategory.MISC)
            .define('C', CommonItemTags.INGOTS_COPPER)
            .define('N', NUGGETS_COPPER)
            .pattern("CNC")
            .pattern("NCN")
            .pattern("CNC")
            .into(2, HumanItems.HEAT_SINK);

        // Thermal Plate (makes 6)
        builder.shaped()
            .withCategory(RecipeCategory.MISC)
            .define('T', HumanTitaniumBlocks.TITANIUM_BLOCK)
            .define('E', Blocks.TERRACOTTA)
            .define('H', HumanItems.HEAT_SINK)
            .define('C', CommonItemTags.INGOTS_COPPER)
            .define('S', CoreBlocks.SILICON_BLOCK)
            .pattern(" EC")
            .pattern("TSH")
            .pattern(" EC")
            .into(6, HumanItems.THERMAL_PLATE);

        // Charging hub - the former Energy Storage Unit, renamed Sep 29. [stated] Oct 2: aluminium either side of a
        // machine block, magnets either side of a copper coil, ferroaluminium either side of a lithium block. The
        // machine block and the lithium block are what keep a 500,000 FE store out of early game.
        builder.shaped()
            .withCategory(RecipeCategory.MISC)
            .define('A', HumanCommonItemTags.INGOTS_ALUMINUM)
            .define('X', HumanBlocks.MACHINE_BLOCK)
            .define('M', HumanItems.NEODYMIUM_MAGNET)
            .define('C', HumanItems.COPPER_COIL)
            .define('F', HumanCommonItemTags.INGOTS_FERROALUMINUM)
            .define('L', CoreBlocks.LITHIUM_BLOCK)
            .pattern("AXA")
            .pattern("MCM")
            .pattern("FLF")
            .into(1, HumanBlocks.CHARGING_HUB);

        // [stated] Oct 2: the battery block - costly but easy: steel on top, lithium blocks either side of a magnet,
        // aluminium either side of a machine block.
        builder.shaped()
            .withCategory(RecipeCategory.MISC)
            .define('S', HumanCommonItemTags.INGOTS_STEEL)
            .define('L', CoreBlocks.LITHIUM_BLOCK)
            .define('M', com.human.common.registry.init.item.HumanItems.NEODYMIUM_MAGNET)
            .define('A', HumanCommonItemTags.INGOTS_ALUMINUM)
            .define('X', HumanBlocks.MACHINE_BLOCK)
            .pattern("SSS")
            .pattern("LML")
            .pattern("AXA")
            .into(1, HumanBlocks.BATTERY);

        // [stated] Oct 2: cryo. The W-Y and USCM controllers have no recipe - they come from the fabricator.
        builder.shaped()
            .withCategory(RecipeCategory.MISC)
            .define('A', HumanCommonItemTags.INGOTS_ALUMINUM)
            .define('G', HumanIndustrialGlassBlocks.INDUSTRIAL_GLASS)
            .define('S', HumanCommonItemTags.INGOTS_STEEL)
            .pattern("AAA")
            .pattern("GGG")
            .pattern("SSS")
            .into(8, HumanBlocks.CRYOTANK);

        builder.shaped()
            .withCategory(RecipeCategory.MISC)
            .define('T', HumanCommonItemTags.INGOTS_TITANIUM)
            .define('R', com.human.common.registry.init.item.HumanItems.RUBBER_GLOBULE)
            .define('G', HumanIndustrialGlassBlocks.INDUSTRIAL_GLASS)
            .pattern("TRT")
            .pattern("RGR")
            .pattern("TRT")
            .into(16, HumanBlocks.CRYOCONDUIT);

        // [stated] Oct 2: The steel cryotube controller - metal on top, heat sinks either side of a desk terminal,
        // metal
        // either side of a machine block.
        builder.shaped()
            .withCategory(RecipeCategory.MISC)
            .define('S', HumanCommonItemTags.INGOTS_STEEL)
            .define('H', com.human.common.registry.init.item.HumanItems.HEAT_SINK)
            .define('T', HumanBlocks.DESK_TERMINAL_BLOCK)
            .define('M', HumanBlocks.MACHINE_BLOCK)
            .pattern("SSS")
            .pattern("HTH")
            .pattern("SMS")
            .into(1, HumanBlocks.STEEL_CRYOTUBE_CONTROLLER);

        // [stated] Oct 2: The standard (ferroaluminium) cryotube controller - metal on top, heat sinks either side of a
        // desk terminal, metal
        // either side of a machine block.
        builder.shaped()
            .withCategory(RecipeCategory.MISC)
            .define('S', HumanCommonItemTags.INGOTS_FERROALUMINUM)
            .define('H', com.human.common.registry.init.item.HumanItems.HEAT_SINK)
            .define('T', HumanBlocks.DESK_TERMINAL_BLOCK)
            .define('M', HumanBlocks.MACHINE_BLOCK)
            .pattern("SSS")
            .pattern("HTH")
            .pattern("SMS")
            .into(1, HumanBlocks.CRYOTUBE_CONTROLLER);

        // Cable, rubber jacket - [stated] Sep 24: "coil in the middle and a cross of rubber around it", FOUR globules
        // not eight, 16 cables per craft.
        builder.shaped()
            .withCategory(RecipeCategory.MISC)
            .define('C', HumanItems.COPPER_COIL)
            .define('R', HumanCommonItemTags.RUBBER)
            .pattern(" R ")
            .pattern("RCR")
            .pattern(" R ")
            .into(16, HumanBlocks.CABLE);

        // Cable, polymer jacket - [stated] Sep 24: "have polymer and coil also make wires but at a 1/4 ratio so 4
        // wires but 2 polymer". ⭐ This is the route for a player with no jungle: the coil is copper only, so nothing
        // in this chain needs rubber at all. Worse ratio, always available.
        builder.shapeless()
            .withCategory(RecipeCategory.MISC)
            // ⚠⚠ A SECOND RECIPE FOR THE SAME OUTPUT NEEDS ITS OWN ID. Recipe ids default to the RESULT's name, so
            // two cable recipes both tried to be avp_human:cable and datagen refused: "Duplicate recipe
            // avp_human:cable". The rubber one keeps the plain id; this alternate path is suffixed.
            .withCustomName(name -> name + "_from_polymer")
            .requires(1, HumanItems.COPPER_COIL)
            .requires(2, HumanItems.POLYMER)
            .into(4, HumanBlocks.CABLE);

        // Solar Panel
        builder.shaped()
            .withCategory(RecipeCategory.MISC)
            .define('S', HumanCommonItemTags.INGOTS_STEEL)
            .define('P', HumanItems.PHOTOVOLTAIC_ARRAY)
            .define('E', HumanItems.SERVO)
            .define('C', CommonItemTags.INGOTS_COPPER)
            .pattern("SPS")
            .pattern("SES")
            .pattern("SCS")
            .into(1, HumanBlocks.SOLAR_PANEL);

        // AeroKinetic Generator
        builder.shaped()
            .withCategory(RecipeCategory.MISC)
            .define('F', HumanCommonItemTags.INGOTS_FERROALUMINUM)
            .define('S', HumanCommonItemTags.INGOTS_STEEL)
            .define('M', HumanItems.ELECTRIC_MOTOR)
            .define('T', HumanItems.TURBINE)
            .define('C', CommonItemTags.INGOTS_COPPER)
            .pattern("STS")
            .pattern("FMF")
            .pattern("FCF")
            .into(1, HumanBlocks.AEROKINETIC_GENERATOR);

        // Hydroelectric Generator
        builder.shaped()
            .withCategory(RecipeCategory.MISC)
            .define('F', HumanCommonItemTags.INGOTS_FERROALUMINUM)
            .define('S', HumanCommonItemTags.INGOTS_STEEL)
            .define('M', HumanItems.ELECTRIC_MOTOR)
            .define('T', HumanItems.TURBINE)
            .define('I', HumanCommonItemTags.INGOTS_TITANIUM)
            .define('C', CommonItemTags.INGOTS_COPPER)
            .pattern("STS")
            .pattern("CIF")
            .pattern("SMS")
            .into(1, HumanBlocks.HYDROELECTRIC_GENERATOR);

        // Thermoelectric Generator
        // ⚠ PLACEHOLDER CENTRE. [stated] Sep 24: "the recipe is going to need a new material and a way to get it. ill
        // go into details later for now have it be a copper coil in the center of a crafting table." The copper coil
        // is holding that slot until the new material and its acquisition are designed - swap ONE define when it is.
        builder.shaped()
            .withCategory(RecipeCategory.MISC)
            .define('F', HumanItems.FAN)
            .define('H', HumanItems.HEAT_SINK)
            .define('L', HumanItems.COPPER_COIL)
            .define('P', HumanItems.THERMAL_PLATE)
            .define('S', HumanCommonItemTags.INGOTS_STEEL)
            .pattern("SFS")
            .pattern("PLP")
            .pattern("PHP")
            .into(1, HumanBlocks.THERMOELECTRIC_GENERATOR);
    }

    /**
     * [stated] Sep 30: crafting recipes for the power consumers - machine block, workstation, wiring tool, terminal,
     * the eight electric machines, the six battery upgrade cards and four fluid tank editions (4 per craft). The W-Y
     * and USCM tanks have NONE here: [stated] they are made in the fabricator, from their own plating blocks.
     */
    private static void provideConsumers(RecipeBuilder builder) {
        // [stated] Oct 2: the fluid pipe is BRASS PLUMBING - brass across the top and bottom rows, the middle row
        // hollow.
        // Sixteen pipes.
        builder.shaped()
            .withCategory(RecipeCategory.MISC)
            .define('B', HumanCommonItemTags.INGOTS_BRASS)
            .pattern("BBB")
            .pattern("   ")
            .pattern("BBB")
            .into(16, com.human.common.registry.init.HumanBlocks.FLUID_PIPE);

        // [stated] Oct 2: the item pipe is GALVANIZED STEEL - steel in the four corners, zinc between them top and
        // bottom,
        // the middle row hollow. Sixteen pipes.
        builder.shaped()
            .withCategory(RecipeCategory.MISC)
            .define('S', HumanCommonItemTags.INGOTS_STEEL)
            .define('Z', HumanCommonItemTags.INGOTS_ZINC)
            .pattern("SZS")
            .pattern("   ")
            .pattern("SZS")
            .into(16, com.human.common.registry.init.HumanBlocks.ITEM_PIPE);

        // [stated] Oct 2: the fuse panel is NOT wireless - its devices are fed by small in-wall wiring, too small to
        // see. So it is mostly copper: a steel case, two copper coils down the middle, a power supply at the bottom.
        builder.shaped()
            .withCategory(RecipeCategory.MISC)
            .define('S', HumanCommonItemTags.INGOTS_STEEL)
            .define('C', com.human.common.registry.init.item.HumanItems.COPPER_COIL)
            .define('P', com.human.common.registry.init.item.HumanItems.POWER_SUPPLY)
            .pattern("SCS")
            .pattern("SCS")
            .pattern("SPS")
            .into(1, com.human.common.registry.init.HumanBlocks.FUSE_PANEL);

        // Hub chip: the card pattern, with ender pearls for the wireless link.
        // [stated] Oct 2: the hub chip is a machine card - in-wall wiring, not wireless - so the machine-card pattern,
        // with
        // a copper coil (the ender pearl is gone).
        builder.shaped()
            .withCategory(RecipeCategory.MISC)
            .define('N', net.minecraft.world.item.Items.GOLD_NUGGET)
            .define('X', com.human.common.registry.init.item.HumanItems.COPPER_COIL)
            .define('S', HumanCommonItemTags.SILICON)
            .define('I', com.human.common.registry.init.item.HumanItems.INTEGRATED_CIRCUIT)
            .define('G', net.minecraft.world.item.Items.DIAMOND)
            .pattern("NXN")
            .pattern("SIS")
            .pattern("NGN")
            .into(1, com.human.common.registry.init.item.HumanItems.HUB_UPGRADE);

        // Machine block - the casing every electric machine is built on
        builder.shaped()
            .withCategory(RecipeCategory.MISC)
            .define('S', HumanCommonItemTags.INGOTS_STEEL)
            .define('L', com.human.common.registry.init.item.HumanItems.LED)
            .define('F', com.human.common.registry.init.item.HumanItems.FAN)
            .define('H', com.human.common.registry.init.item.HumanItems.HEAT_SINK)
            .define('C', com.human.common.registry.init.item.HumanItems.COPPER_COIL)
            .define('P', com.human.common.registry.init.item.HumanItems.POWER_SUPPLY)
            .pattern("SLS")
            .pattern("FHC")
            .pattern("SPS")
            .into(1, com.human.common.registry.init.HumanBlocks.MACHINE_BLOCK);

        // Electronics workstation
        builder.shaped()
            .withCategory(RecipeCategory.MISC)
            .define('G', com.human.common.registry.init.block.HumanIndustrialGlassBlocks.INDUSTRIAL_GLASS_PANE)
            .define('I', com.human.common.registry.init.item.HumanItems.INTEGRATED_CIRCUIT)
            .define('T', net.minecraft.world.item.Items.CRAFTING_TABLE)
            .define('S', HumanCommonItemTags.INGOTS_STEEL)
            .pattern("GGG")
            .pattern("ITI")
            .pattern("SSS")
            .into(1, com.human.common.registry.init.HumanBlocks.ELECTRONICS_WORKSTATION);

        // Wiring tool: steel handle, diode, copper tip
        builder.shaped()
            .withCategory(RecipeCategory.MISC)
            .define('C', CommonItemTags.INGOTS_COPPER)
            .define('D', com.human.common.registry.init.item.HumanItems.DIODE)
            .define('S', HumanCommonItemTags.INGOTS_STEEL)
            .pattern("  C")
            .pattern(" D ")
            .pattern("S  ")
            .into(1, com.human.common.registry.init.item.HumanItems.WIRING_TOOL);

        // Desk terminal
        // [stated] Oct 2: the desk terminal is a real computer - polymer case and an LED, then fan, motherboard and
        // CPU,
        // then RAM, power supply and SSD.
        builder.shaped()
            .withCategory(RecipeCategory.MISC)
            .define('P', com.human.common.registry.init.item.HumanItems.POLYMER)
            .define('L', com.human.common.registry.init.item.HumanItems.LED)
            .define('F', com.human.common.registry.init.item.HumanItems.FAN)
            .define('M', com.human.common.registry.init.item.HumanItems.MOTHERBOARD)
            .define('C', com.human.common.registry.init.item.HumanItems.CPU)
            .define('R', com.human.common.registry.init.item.HumanItems.RAM)
            .define('W', com.human.common.registry.init.item.HumanItems.POWER_SUPPLY)
            .define('D', com.human.common.registry.init.item.HumanItems.SSD)
            .pattern("PLP")
            .pattern("FMC")
            .pattern("RWD")
            .into(1, com.human.common.registry.init.HumanBlocks.DESK_TERMINAL_BLOCK);

        // [stated] Oct 2: every machine sits on a machine block, with aluminium nuggets filling the empty spots.
        builder.shaped()
            .withCategory(RecipeCategory.MISC)
            .define('N', HumanCommonItemTags.NUGGETS_ALUMINUM)
            .define('F', net.minecraft.world.item.Items.FURNACE)
            .define('M', com.human.common.registry.init.HumanBlocks.MACHINE_BLOCK)
            .pattern("NNN")
            .pattern("NFN")
            .pattern("NMN")
            .into(1, com.human.common.registry.init.HumanBlocks.ELECTRIC_FURNACE);

        builder.shaped()
            .withCategory(RecipeCategory.MISC)
            .define('N', HumanCommonItemTags.NUGGETS_ALUMINUM)
            .define('F', net.minecraft.world.item.Items.SMOKER)
            .define('M', com.human.common.registry.init.HumanBlocks.MACHINE_BLOCK)
            .pattern("NNN")
            .pattern("NFN")
            .pattern("NMN")
            .into(1, com.human.common.registry.init.HumanBlocks.ELECTRIC_SMOKER);

        builder.shaped()
            .withCategory(RecipeCategory.MISC)
            .define('N', HumanCommonItemTags.NUGGETS_ALUMINUM)
            .define('F', net.minecraft.world.item.Items.BLAST_FURNACE)
            .define('M', com.human.common.registry.init.HumanBlocks.MACHINE_BLOCK)
            .pattern("NNN")
            .pattern("NFN")
            .pattern("NMN")
            .into(1, com.human.common.registry.init.HumanBlocks.ELECTRIC_BLAST_FURNACE);

        builder.shaped()
            .withCategory(RecipeCategory.MISC)
            .define('N', HumanCommonItemTags.NUGGETS_ALUMINUM)
            .define('F', com.human.common.registry.init.HumanBlocks.INDUSTRIAL_FURNACE)
            .define('M', com.human.common.registry.init.HumanBlocks.MACHINE_BLOCK)
            .pattern("NNN")
            .pattern("NFN")
            .pattern("NMN")
            .into(1, com.human.common.registry.init.HumanBlocks.ELECTRIC_INDUSTRIAL_FURNACE);

        // Grinder: three titanium ingots on top, a grindstone in the centre.
        builder.shaped()
            .withCategory(RecipeCategory.MISC)
            .define('T', HumanCommonItemTags.INGOTS_TITANIUM)
            .define('N', HumanCommonItemTags.NUGGETS_ALUMINUM)
            .define('G', net.minecraft.world.item.Items.GRINDSTONE)
            .define('M', com.human.common.registry.init.HumanBlocks.MACHINE_BLOCK)
            .pattern("TTT")
            .pattern("NGN")
            .pattern("NMN")
            .into(1, com.human.common.registry.init.HumanBlocks.ELECTRIC_GRINDER);

        // Press: a cauldron on top, a piston in the centre.
        builder.shaped()
            .withCategory(RecipeCategory.MISC)
            .define('N', HumanCommonItemTags.NUGGETS_ALUMINUM)
            .define('C', net.minecraft.world.item.Items.CAULDRON)
            .define('P', net.minecraft.world.item.Items.PISTON)
            .define('M', com.human.common.registry.init.HumanBlocks.MACHINE_BLOCK)
            .pattern("NCN")
            .pattern("NPN")
            .pattern("NMN")
            .into(1, com.human.common.registry.init.HumanBlocks.ELECTRIC_PRESS);

        // Hydrator: a bucket on top, a dispenser in the centre.
        builder.shaped()
            .withCategory(RecipeCategory.MISC)
            .define('N', HumanCommonItemTags.NUGGETS_ALUMINUM)
            .define('B', net.minecraft.world.item.Items.BUCKET)
            .define('D', net.minecraft.world.item.Items.DISPENSER)
            .define('M', com.human.common.registry.init.HumanBlocks.MACHINE_BLOCK)
            .pattern("NBN")
            .pattern("NDN")
            .pattern("NMN")
            .into(1, com.human.common.registry.init.HumanBlocks.ELECTRIC_HYDRATOR);

        // Freezer: a fan on top, copper coil / heat sink / copper coil across the centre.
        builder.shaped()
            .withCategory(RecipeCategory.MISC)
            .define('N', HumanCommonItemTags.NUGGETS_ALUMINUM)
            .define('F', com.human.common.registry.init.item.HumanItems.FAN)
            .define('C', com.human.common.registry.init.item.HumanItems.COPPER_COIL)
            .define('H', com.human.common.registry.init.item.HumanItems.HEAT_SINK)
            .define('M', com.human.common.registry.init.HumanBlocks.MACHINE_BLOCK)
            .pattern("NFN")
            .pattern("CHC")
            .pattern("NMN")
            .into(1, com.human.common.registry.init.HumanBlocks.ELECTRIC_FREEZER);

        // [stated] Oct 2: battery cards follow the card pattern too - two silicon and an integrated circuit, with
        // LITHIUM DUST where a machine card has its diamond. The themed ingredient on top says what the card does.
        builder.shaped()
            .withCategory(RecipeCategory.MISC)
            .define('N', net.minecraft.world.item.Items.GOLD_NUGGET)
            .define('X', com.human.common.registry.init.item.HumanItems.CAPACITOR)
            .define('S', HumanCommonItemTags.SILICON)
            .define('I', com.human.common.registry.init.item.HumanItems.INTEGRATED_CIRCUIT)
            .define('L', HumanCommonItemTags.DUSTS_LITHIUM)
            .pattern("NXN")
            .pattern("SIS")
            .pattern("NLN")
            .into(1, com.human.common.registry.init.item.HumanItems.CAPACITY_UPGRADE);

        builder.shaped()
            .withCategory(RecipeCategory.MISC)
            .define('N', net.minecraft.world.item.Items.GOLD_NUGGET)
            .define('X', CommonItemTags.INGOTS_COPPER)
            .define('S', HumanCommonItemTags.SILICON)
            .define('I', com.human.common.registry.init.item.HumanItems.INTEGRATED_CIRCUIT)
            .define('L', HumanCommonItemTags.DUSTS_LITHIUM)
            .pattern("NXN")
            .pattern("SIS")
            .pattern("NLN")
            .into(1, com.human.common.registry.init.item.HumanItems.TRANSFER_UPGRADE);

        builder.shaped()
            .withCategory(RecipeCategory.MISC)
            .define('N', net.minecraft.world.item.Items.GOLD_NUGGET)
            .define('X', net.minecraft.world.item.Items.REDSTONE)
            .define('S', HumanCommonItemTags.SILICON)
            .define('I', com.human.common.registry.init.item.HumanItems.INTEGRATED_CIRCUIT)
            .define('L', HumanCommonItemTags.DUSTS_LITHIUM)
            .pattern("NXN")
            .pattern("SIS")
            .pattern("NLN")
            .into(1, com.human.common.registry.init.item.HumanItems.EMERGENCY_UPGRADE);

        builder.shaped()
            .withCategory(RecipeCategory.MISC)
            .define('N', net.minecraft.world.item.Items.GOLD_NUGGET)
            .define('X', net.minecraft.world.item.Items.REDSTONE_TORCH)
            .define('S', HumanCommonItemTags.SILICON)
            .define('I', com.human.common.registry.init.item.HumanItems.INTEGRATED_CIRCUIT)
            .define('L', HumanCommonItemTags.DUSTS_LITHIUM)
            .pattern("NXN")
            .pattern("SIS")
            .pattern("NLN")
            .into(1, com.human.common.registry.init.item.HumanItems.REDSTONE_UPGRADE);

        builder.shaped()
            .withCategory(RecipeCategory.MISC)
            .define('N', net.minecraft.world.item.Items.GOLD_NUGGET)
            .define('X', net.minecraft.world.item.Items.OBSIDIAN)
            .define('S', HumanCommonItemTags.SILICON)
            .define('I', com.human.common.registry.init.item.HumanItems.INTEGRATED_CIRCUIT)
            .define('L', HumanCommonItemTags.DUSTS_LITHIUM)
            .pattern("NXN")
            .pattern("SIS")
            .pattern("NLN")
            .into(1, com.human.common.registry.init.item.HumanItems.ARMORED_UPGRADE);

        builder.shaped()
            .withCategory(RecipeCategory.MISC)
            .define('N', net.minecraft.world.item.Items.GOLD_NUGGET)
            .define('X', net.minecraft.world.item.Items.BELL)
            .define('S', HumanCommonItemTags.SILICON)
            .define('I', com.human.common.registry.init.item.HumanItems.INTEGRATED_CIRCUIT)
            .define('L', HumanCommonItemTags.DUSTS_LITHIUM)
            .pattern("NXN")
            .pattern("SIS")
            .pattern("NLN")
            .into(1, com.human.common.registry.init.item.HumanItems.ALARM_UPGRADE);

        // [stated] Oct 2: machine and sentry cards cost more - two silicon and an integrated circuit, plus a DIAMOND
        // for
        // a machine card and an EMERALD for a sentry card. One themed ingredient says what the card does. The sentry
        // cards lie on their side, so their recipe is the machine pattern turned 90 degrees clockwise.

        builder.shaped()
            .withCategory(RecipeCategory.MISC)
            .define('N', net.minecraft.world.item.Items.GOLD_NUGGET)
            .define('X', com.human.common.registry.init.item.HumanItems.SERVO)
            .define('S', HumanCommonItemTags.SILICON)
            .define('I', com.human.common.registry.init.item.HumanItems.INTEGRATED_CIRCUIT)
            .define('G', net.minecraft.world.item.Items.DIAMOND)
            .pattern("NXN")
            .pattern("SIS")
            .pattern("NGN")
            .into(1, com.human.common.registry.init.item.HumanItems.SPEED_UPGRADE);

        builder.shaped()
            .withCategory(RecipeCategory.MISC)
            .define('N', net.minecraft.world.item.Items.GOLD_NUGGET)
            .define('X', com.human.common.registry.init.item.HumanItems.REGULATOR)
            .define('S', HumanCommonItemTags.SILICON)
            .define('I', com.human.common.registry.init.item.HumanItems.INTEGRATED_CIRCUIT)
            .define('G', net.minecraft.world.item.Items.DIAMOND)
            .pattern("NXN")
            .pattern("SIS")
            .pattern("NGN")
            .into(1, com.human.common.registry.init.item.HumanItems.EFFICIENCY_UPGRADE);

        builder.shaped()
            .withCategory(RecipeCategory.MISC)
            .define('N', net.minecraft.world.item.Items.GOLD_NUGGET)
            .define('X', net.minecraft.world.item.Items.AMETHYST_SHARD)
            .define('S', HumanCommonItemTags.SILICON)
            .define('I', com.human.common.registry.init.item.HumanItems.INTEGRATED_CIRCUIT)
            .define('G', net.minecraft.world.item.Items.EMERALD)
            .pattern("NSN")
            .pattern("GIX")
            .pattern("NSN")
            .into(1, com.human.common.registry.init.item.HumanItems.RANGE_UPGRADE);

        builder.shaped()
            .withCategory(RecipeCategory.MISC)
            .define('N', net.minecraft.world.item.Items.GOLD_NUGGET)
            .define('X', net.minecraft.world.item.Items.GLASS_PANE)
            .define('S', HumanCommonItemTags.SILICON)
            .define('I', com.human.common.registry.init.item.HumanItems.INTEGRATED_CIRCUIT)
            .define('G', net.minecraft.world.item.Items.EMERALD)
            .pattern("NSN")
            .pattern("GIX")
            .pattern("NSN")
            .into(1, com.human.common.registry.init.item.HumanItems.WIDE_CONE_UPGRADE);

        builder.shaped()
            .withCategory(RecipeCategory.MISC)
            .define('N', net.minecraft.world.item.Items.GOLD_NUGGET)
            .define('X', net.minecraft.world.item.Items.PACKED_ICE)
            .define('S', HumanCommonItemTags.SILICON)
            .define('I', com.human.common.registry.init.item.HumanItems.INTEGRATED_CIRCUIT)
            .define('G', net.minecraft.world.item.Items.EMERALD)
            .pattern("NSN")
            .pattern("GIX")
            .pattern("NSN")
            .into(1, com.human.common.registry.init.item.HumanItems.COOLING_UPGRADE);

        builder.shaped()
            .withCategory(RecipeCategory.MISC)
            .define('N', net.minecraft.world.item.Items.GOLD_NUGGET)
            .define('X', net.minecraft.world.item.Items.OBSIDIAN)
            .define('S', HumanCommonItemTags.SILICON)
            .define('I', com.human.common.registry.init.item.HumanItems.INTEGRATED_CIRCUIT)
            .define('G', net.minecraft.world.item.Items.EMERALD)
            .pattern("NSN")
            .pattern("GIX")
            .pattern("NSN")
            .into(1, com.human.common.registry.init.item.HumanItems.DEFENSE_UPGRADE);

        // [stated] Oct 2: lenses - a diamond in the centre, industrial glass above and below, aluminium corners, the
        // dye
        // for the lens colour on the left and the part that makes it work on the right.
        builder.shaped()
            .withCategory(RecipeCategory.MISC)
            .define('A', HumanCommonItemTags.INGOTS_ALUMINUM)
            .define('G', HumanIndustrialGlassBlocks.INDUSTRIAL_GLASS)
            .define('D', net.minecraft.world.item.Items.GREEN_DYE)
            .define('X', net.minecraft.world.item.Items.DIAMOND)
            .define('P', net.minecraft.world.item.Items.SCULK_SENSOR)
            .pattern("AGA")
            .pattern("DXP")
            .pattern("AGA")
            .into(1, com.human.common.registry.init.item.HumanItems.MOTION_SENSOR);

        builder.shaped()
            .withCategory(RecipeCategory.MISC)
            .define('A', HumanCommonItemTags.INGOTS_ALUMINUM)
            .define('G', HumanIndustrialGlassBlocks.INDUSTRIAL_GLASS)
            .define('D', net.minecraft.world.item.Items.BLUE_DYE)
            .define('X', net.minecraft.world.item.Items.DIAMOND)
            .define('P', com.human.common.registry.init.item.HumanItems.NEODYMIUM_MAGNET)
            .pattern("AGA")
            .pattern("DXP")
            .pattern("AGA")
            .into(1, com.human.common.registry.init.item.HumanItems.EM_SENSOR);

        builder.shaped()
            .withCategory(RecipeCategory.MISC)
            .define('A', HumanCommonItemTags.INGOTS_ALUMINUM)
            .define('G', HumanIndustrialGlassBlocks.INDUSTRIAL_GLASS)
            .define('D', net.minecraft.world.item.Items.BLACK_DYE)
            .define('X', net.minecraft.world.item.Items.DIAMOND)
            .define('P', net.minecraft.world.item.Items.OBSERVER)
            .pattern("AGA")
            .pattern("DXP")
            .pattern("AGA")
            .into(1, com.human.common.registry.init.item.HumanItems.CAMERA_SENSOR);

        // [stated] Oct 2: the network card - a sentry card, so the sideways emerald pattern, with a CPU.
        builder.shaped()
            .withCategory(RecipeCategory.MISC)
            .define('N', net.minecraft.world.item.Items.GOLD_NUGGET)
            .define('X', com.human.common.registry.init.item.HumanItems.CPU)
            .define('S', HumanCommonItemTags.SILICON)
            .define('I', com.human.common.registry.init.item.HumanItems.INTEGRATED_CIRCUIT)
            .define('G', net.minecraft.world.item.Items.EMERALD)
            .pattern("NSN")
            .pattern("GIX")
            .pattern("NSN")
            .into(1, com.human.common.registry.init.item.HumanItems.NETWORK_CARD);

        builder.shaped()
            .withCategory(RecipeCategory.MISC)
            .define('N', net.minecraft.world.item.Items.GOLD_NUGGET)
            .define('X', com.human.common.registry.init.item.HumanItems.CASELESS_BULLET)
            .define('S', HumanCommonItemTags.SILICON)
            .define('I', com.human.common.registry.init.item.HumanItems.INTEGRATED_CIRCUIT)
            .define('G', net.minecraft.world.item.Items.EMERALD)
            .pattern("NSN")
            .pattern("GIX")
            .pattern("NSN")
            .into(1, com.human.common.registry.init.item.HumanItems.CASELESS_AMMO_UPGRADE);

        builder.shaped()
            .withCategory(RecipeCategory.MISC)
            .define('N', net.minecraft.world.item.Items.GOLD_NUGGET)
            .define('X', com.human.common.registry.init.item.HumanItems.ROCKET)
            .define('S', HumanCommonItemTags.SILICON)
            .define('I', com.human.common.registry.init.item.HumanItems.INTEGRATED_CIRCUIT)
            .define('G', net.minecraft.world.item.Items.EMERALD)
            .pattern("NSN")
            .pattern("GIX")
            .pattern("NSN")
            .into(1, com.human.common.registry.init.item.HumanItems.ROCKET_AMMO_UPGRADE);

        builder.shaped()
            .withCategory(RecipeCategory.MISC)
            .define('M', HumanCommonItemTags.INGOTS_STEEL)
            .define('G', com.human.common.registry.init.block.HumanIndustrialGlassBlocks.INDUSTRIAL_GLASS)
            .pattern("MMM")
            .pattern("MGM")
            .pattern("MMM")
            .into(4, com.human.common.registry.init.HumanBlocks.STEEL_FLUID_TANK);

        builder.shaped()
            .withCategory(RecipeCategory.MISC)
            .define('M', HumanCommonItemTags.INGOTS_FERROALUMINUM)
            .define('G', com.human.common.registry.init.block.HumanIndustrialGlassBlocks.INDUSTRIAL_GLASS)
            .pattern("MMM")
            .pattern("MGM")
            .pattern("MMM")
            .into(4, com.human.common.registry.init.HumanBlocks.FERROALUMINUM_FLUID_TANK);

        builder.shaped()
            .withCategory(RecipeCategory.MISC)
            .define('M', HumanCommonItemTags.INGOTS_ALUMINUM)
            .define('G', com.human.common.registry.init.block.HumanIndustrialGlassBlocks.INDUSTRIAL_GLASS)
            .pattern("MMM")
            .pattern("MGM")
            .pattern("MMM")
            .into(4, com.human.common.registry.init.HumanBlocks.ALUMINUM_FLUID_TANK);

        builder.shaped()
            .withCategory(RecipeCategory.MISC)
            .define('M', HumanCommonItemTags.INGOTS_TITANIUM)
            .define('G', com.human.common.registry.init.block.HumanIndustrialGlassBlocks.INDUSTRIAL_GLASS)
            .pattern("MMM")
            .pattern("MGM")
            .pattern("MMM")
            .into(4, com.human.common.registry.init.HumanBlocks.TITANIUM_FLUID_TANK);
    }
}
