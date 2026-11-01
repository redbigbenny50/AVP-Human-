package com.human.common.gameplay.cosmetic;

import com.human.common.gameplay.fabrication.FabricatingRecipe;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * The Cosmetic Crafter's prebuilt catalogue - [stated] Oct 31: "raw materials, no pattern ... think of it as a prebuilt
 * catalog", with faction licenses unlocking the faction additions.
 * <h2>COSTS</h2> Each design has a base (Industrial) list of raw materials. A faction edition is the same list plus
 * that faction's materials - the same rule the fabricator's faction recipes follow: W-Y adds white plastic and yellow
 * dye, USCM adds composite and green dye.
 * <h2>ORDER</h2> Designs in {@link CosmeticDesign} order, each in {@link CosmeticStyle} order. The order is the same on
 * both sides, so the screen and the server agree on an entry by its index alone.
 */
public final class CosmeticCatalog {

    /** One thing the crafter can make. */
    public record Entry(
        int index,
        CosmeticDesign design,
        CosmeticStyle style,
        List<FabricatingRecipe.Part> parts,
        Item result
    ) {

        public ItemStack resultStack() {
            return new ItemStack(result);
        }
    }

    /** Most ingredient lines one entry may have - the screen shows them three at a time with a scroll bar. */
    public static final int MAX_PARTS = 8;

    private static @Nullable List<Entry> entries;

    private CosmeticCatalog() {}

    /** Every registered piece whose design matches, in every style - for the recipe viewers' info pages. */
    public static java.util.List<net.minecraft.world.item.ItemStack> stacksWhere(java.util.function.Predicate<CosmeticDesign> test) {
        var out = new java.util.ArrayList<net.minecraft.world.item.ItemStack>();

        for (var item : com.human.common.registry.init.HumanCosmetics.DECOR_ITEMS.values()) {
            if (item.get() instanceof net.minecraft.world.item.BlockItem blockItem && blockItem.getBlock() instanceof CosmeticBlock block && test.test(
                block.design()
            )) {
                out.add(new net.minecraft.world.item.ItemStack(blockItem));
            }
        }

        return out;
    }

    /** Every registered piece with a swappable interior, in every style - for the recipe viewers' info pages. */
    public static java.util.List<net.minecraft.world.item.ItemStack> specimenStacks() {
        var out = new java.util.ArrayList<net.minecraft.world.item.ItemStack>();

        for (var item : com.human.common.registry.init.HumanCosmetics.DECOR_ITEMS.values()) {
            if (item.get() instanceof net.minecraft.world.item.BlockItem blockItem && blockItem.getBlock() instanceof CosmeticSpecimenBlock) {
                out.add(new net.minecraft.world.item.ItemStack(blockItem));
            }
        }

        return out;
    }

    /** GENERATED from the design master table: each design's Industrial raw materials. */
    private static String[][] base(CosmeticDesign design) {
        return switch (design) {
            case LAB_BENCH -> new String[][] { { "#c:ingots/steel", "6" }, { "#c:ingots/aluminum", "2" }, { "avp_human:polymer", "1" }, { "minecraft:glass_bottle", "2" } };
            case LAB_CHAIR -> new String[][] { { "#c:ingots/steel", "3" }, { "avp_human:white_padding", "2" }, { "avp_human:rubber_globule", "1" } };
            case STORAGE_CABINET -> new String[][] { { "#c:ingots/steel", "8" }, { "#c:ingots/aluminum", "2" } };
            case LAB_TERMINAL -> new String[][] { { "#c:ingots/steel", "4" }, { "avp_human:integrated_circuit", "1" }, { "avp_human:industrial_glass", "1" }, { "avp_human:polymer", "2" } };
            case EQUIPMENT_CART -> new String[][] { { "#c:ingots/steel", "5" }, { "#c:ingots/aluminum", "2" }, { "avp_human:rubber_globule", "4" } };
            case MICROSCOPE -> new String[][] { { "#c:ingots/steel", "2" }, { "minecraft:glass_pane", "1" }, { "avp_human:polymer", "1" } };
            case CENTRIFUGE -> new String[][] { { "#c:ingots/steel", "3" }, { "#c:ingots/aluminum", "1" }, { "avp_human:integrated_circuit", "1" } };
            case SPECIMEN_JAR -> new String[][] { { "avp_human:industrial_glass", "2" }, { "#c:ingots/steel", "1" }, { "avp_human:polymer", "1" } };
            case SAMPLE_ANALYZER -> new String[][] { { "#c:ingots/steel", "3" }, { "avp_human:integrated_circuit", "1" }, { "avp_human:industrial_glass", "1" } };
            case SAMPLE_RACK -> new String[][] { { "#c:ingots/steel", "1" }, { "minecraft:glass_bottle", "3" } };
            case EXAM_BED -> new String[][] { { "#c:ingots/steel", "1" }, { "#c:ingots/aluminum", "2" }, { "avp_human:white_padding", "1" }, { "avp_human:rubber_globule", "1" } };
            case SURGICAL_LIGHT -> new String[][] { { "#c:ingots/steel", "2" }, { "#c:ingots/aluminum", "2" }, { "avp_human:rubber_globule", "1" }, { "minecraft:redstone", "1" } };
            case PRIVACY_SCREEN -> new String[][] { { "#c:ingots/steel", "1" }, { "#c:ingots/aluminum", "3" }, { "avp_human:white_padding", "2" }, { "avp_human:rubber_globule", "1" } };
            case WALL_CABINET -> new String[][] { { "#c:ingots/steel", "9" }, { "#c:ingots/aluminum", "2" }, { "avp_human:industrial_glass", "1" } };
            case MEDICAL_STATION -> new String[][] { { "#c:ingots/steel", "3" }, { "avp_human:rubber_globule", "1" }, { "avp_human:integrated_circuit", "1" } };
            case CRYO_SUPPORT_UNIT -> new String[][] { { "#c:ingots/steel", "2" }, { "#c:ingots/aluminum", "2" }, { "avp_human:industrial_glass", "3" }, { "minecraft:redstone", "1" } };
            case BUNK_BED -> new String[][] { { "#c:ingots/steel", "8" }, { "avp_human:white_padding", "4" } };
            case DINING_SET -> new String[][] { { "#c:ingots/steel", "6" }, { "#c:ingots/aluminum", "2" } };
            case HABITAT_LOCKER -> new String[][] { { "#c:ingots/steel", "7" }, { "#c:ingots/aluminum", "1" } };
            case SOFA -> new String[][] { { "#c:ingots/steel", "4" }, { "avp_human:white_padding", "6" } };
            case REFRIGERATOR -> new String[][] { { "#c:ingots/steel", "6" }, { "#c:ingots/aluminum", "2" }, { "avp_human:integrated_circuit", "1" }, { "minecraft:ice", "1" } };
            case CONTROL_DESK -> new String[][] { { "#c:ingots/steel", "8" }, { "#c:ingots/aluminum", "3" }, { "avp_human:rubber_globule", "1" }, { "avp_human:integrated_circuit", "1" } };
            case OFFICE_CHAIR -> new String[][] { { "#c:ingots/steel", "1" }, { "avp_human:white_padding", "1" } };
            case SHELVING_UNIT -> new String[][] { { "#c:ingots/steel", "3" }, { "#c:ingots/aluminum", "2" }, { "avp_human:industrial_glass", "1" } };
            case VENDING_MACHINE -> new String[][] { { "#c:ingots/steel", "8" }, { "#c:ingots/aluminum", "2" }, { "avp_human:industrial_glass", "1" }, { "avp_human:rubber_globule", "1" }, { "avp_human:integrated_circuit", "1" } };
            case SMALL_TABLE -> new String[][] { { "#c:ingots/steel", "1" } };
            case LONG_TABLE -> new String[][] { { "#c:ingots/steel", "2" }, { "#c:ingots/aluminum", "2" } };
            case LARGE_TABLE -> new String[][] { { "#c:ingots/steel", "4" }, { "#c:ingots/aluminum", "3" } };
            case COUNTER_UNIT -> new String[][] { { "#c:ingots/steel", "5" }, { "#c:ingots/aluminum", "1" } };
            case PLAIN_DESK -> new String[][] { { "#c:ingots/steel", "5" }, { "#c:ingots/aluminum", "2" } };
            case BARE_LAB_BENCH -> new String[][] { { "#c:ingots/steel", "6" }, { "#c:ingots/aluminum", "2" } };
            case SIDE_TABLE -> new String[][] { { "#c:ingots/steel", "1" } };
            case JUNCTION_BOX -> new String[][] { { "#c:ingots/steel", "1" } };
            case PIPE_SECTION -> new String[][] { { "#c:ingots/steel", "1" } };
            case VENT_PANEL -> new String[][] { { "#c:ingots/steel", "1" } };
            case CABLE_TRAY -> new String[][] { { "#c:ingots/steel", "1" } };
            case DATA_PANEL -> new String[][] { { "#c:ingots/steel", "1" }, { "avp_human:rubber_globule", "1" }, { "avp_human:integrated_circuit", "1" } };
            case CONDUIT_SECTION -> new String[][] { { "#c:ingots/steel", "1" } };
            case CEILING_RAIL -> new String[][] { { "#c:ingots/steel", "1" } };
            case ARMORED_PANEL -> new String[][] { { "#c:ingots/steel", "1" }, { "minecraft:redstone", "1" } };
            case VENT_FAN -> new String[][] { { "#c:ingots/steel", "1" } };
            case SUPPORT_BRACKET -> new String[][] { { "#c:ingots/steel", "1" } };
            case CABLE_PANEL -> new String[][] { { "#c:ingots/steel", "1" }, { "avp_human:rubber_globule", "1" } };
            case SPLIT_PANEL -> new String[][] { { "#c:ingots/steel", "2" }, { "#c:ingots/aluminum", "2" } };
            case PIPE_CLUSTER -> new String[][] { { "#c:ingots/steel", "1" }, { "#c:ingots/aluminum", "2" } };
            case CABLE_BUNDLE -> new String[][] { { "#c:ingots/steel", "1" }, { "avp_human:rubber_globule", "1" } };
            case BEAM_SECTION -> new String[][] { { "#c:ingots/steel", "1" }, { "#c:ingots/aluminum", "2" } };
            case HANGING_CONDUITS -> new String[][] { { "#c:ingots/steel", "1" }, { "#c:ingots/aluminum", "2" } };
            case JUNCTION_HUB -> new String[][] { { "#c:ingots/steel", "1" }, { "minecraft:redstone", "1" } };
            case FILTER_BOX -> new String[][] { { "#c:ingots/steel", "1" } };
            case ACCESS_PANEL -> new String[][] { { "#c:ingots/steel", "1" }, { "minecraft:redstone", "1" } };
            case STOWAGE_RACK -> new String[][] { { "#c:ingots/steel", "1" } };
            case TOOL_MOUNT -> new String[][] { { "#c:ingots/steel", "1" }, { "minecraft:oak_planks", "1" } };
            case TRASH_BIN -> new String[][] { { "#c:ingots/steel", "2" } };
            case RECYCLING_BIN -> new String[][] { { "#c:ingots/steel", "1" }, { "avp_human:industrial_glass", "1" } };
            case BIOHAZARD_BIN -> new String[][] { { "#c:ingots/steel", "2" } };
            case JANITOR_CART -> new String[][] { { "#c:ingots/steel", "1" }, { "avp_human:rubber_globule", "2" }, { "minecraft:oak_planks", "1" } };
            case MAINTENANCE_KIT -> new String[][] { { "#c:ingots/steel", "1" } };
            case UTILITY_CART -> new String[][] { { "#c:ingots/steel", "2" }, { "#c:ingots/aluminum", "2" }, { "avp_human:rubber_globule", "1" } };
            case WORK_LIGHT -> new String[][] { { "#c:ingots/steel", "1" }, { "avp_human:rubber_globule", "1" }, { "minecraft:redstone", "1" } };
            case STOVE -> new String[][] { { "#c:ingots/steel", "4" }, { "avp_human:rubber_globule", "1" }, { "minecraft:redstone", "1" } };
            case SINK_UNIT -> new String[][] { { "#c:ingots/steel", "4" } };
            case TOILET -> new String[][] { { "#c:ingots/steel", "2" } };
            case SHOWER -> new String[][] { { "#c:ingots/steel", "2" }, { "#c:ingots/aluminum", "2" }, { "avp_human:industrial_glass", "1" }, { "minecraft:redstone", "1" } };
            case FOOD_DISPENSER -> new String[][] { { "#c:ingots/steel", "4" }, { "avp_human:rubber_globule", "1" }, { "avp_human:integrated_circuit", "1" } };
            case COFFEE_MACHINE -> new String[][] { { "#c:ingots/steel", "2" }, { "avp_human:industrial_glass", "1" }, { "minecraft:paper", "1" }, { "minecraft:redstone", "1" } };
            case WATER_COOLER -> new String[][] { { "#c:ingots/steel", "2" }, { "avp_human:industrial_glass", "1" }, { "minecraft:redstone", "1" } };
            case MICROWAVE -> new String[][] { { "#c:ingots/steel", "2" }, { "avp_human:rubber_globule", "1" }, { "minecraft:redstone", "1" } };
            case WATER_DISPENSER -> new String[][] { { "#c:ingots/steel", "4" }, { "#c:ingots/aluminum", "2" }, { "avp_human:industrial_glass", "1" }, { "avp_human:rubber_globule", "1" }, { "minecraft:redstone", "1" } };
            case HEATER_UNIT -> new String[][] { { "#c:ingots/steel", "2" }, { "avp_human:rubber_globule", "1" }, { "minecraft:redstone", "1" } };
            case RATION_WARMER -> new String[][] { { "#c:ingots/steel", "2" }, { "avp_human:rubber_globule", "1" }, { "minecraft:redstone", "1" } };
            case CARGO_CRATE -> new String[][] { { "#c:ingots/steel", "5" }, { "#c:ingots/aluminum", "1" } };
            case PALLET -> new String[][] { { "#c:ingots/steel", "1" }, { "minecraft:oak_planks", "1" } };
            case MAINTENANCE_WORKBENCH -> new String[][] { { "#c:ingots/steel", "3" }, { "#c:ingots/aluminum", "2" } };
            case TOOL_CHEST -> new String[][] { { "#c:ingots/steel", "3" }, { "avp_human:rubber_globule", "1" } };
            case SECURITY_DESK -> new String[][] { { "#c:ingots/steel", "4" }, { "#c:ingots/aluminum", "2" }, { "avp_human:rubber_globule", "1" }, { "avp_human:integrated_circuit", "1" } };
            case CHECKPOINT_SCANNER -> new String[][] { { "#c:ingots/steel", "2" }, { "avp_human:rubber_globule", "1" }, { "avp_human:integrated_circuit", "1" } };
            case TOOL_CART -> new String[][] { { "#c:ingots/steel", "2" }, { "avp_human:rubber_globule", "1" }, { "minecraft:oak_planks", "1" } };
            case AMMO_RACK -> new String[][] { { "#c:ingots/steel", "2" } };
            case ARMORY_LOCKER -> new String[][] { { "#c:ingots/steel", "3" }, { "#c:ingots/aluminum", "2" } };
            case SECURITY_CHECKPOINT -> new String[][] { { "#c:ingots/steel", "2" }, { "avp_human:rubber_globule", "1" }, { "minecraft:redstone", "1" } };
            case SECURITY_GATE -> new String[][] { { "#c:ingots/steel", "3" }, { "#c:ingots/aluminum", "3" }, { "avp_human:rubber_globule", "1" }, { "minecraft:redstone", "1" } };
            case TURNSTILE -> new String[][] { { "#c:ingots/steel", "1" }, { "minecraft:redstone", "1" } };
            case FIELD_SUPPLY_CASE -> new String[][] { { "#c:ingots/steel", "5" }, { "#c:ingots/aluminum", "2" } };
            case HYDRO_TRAY -> new String[][] { { "#c:ingots/steel", "1" }, { "minecraft:oak_sapling", "1" }, { "minecraft:dirt", "1" }, { "minecraft:redstone", "1" } };
            case GROW_RACK -> new String[][] { { "#c:ingots/steel", "1" }, { "avp_human:rubber_globule", "1" }, { "minecraft:oak_sapling", "1" }, { "minecraft:dirt", "1" }, { "minecraft:redstone", "1" } };
            case ARCADE_MACHINE -> new String[][] { { "#c:ingots/steel", "6" }, { "#c:ingots/aluminum", "2" }, { "avp_human:rubber_globule", "1" }, { "avp_human:integrated_circuit", "1" } };
            case EXERCISE_BENCH -> new String[][] { { "#c:ingots/steel", "1" }, { "#c:ingots/aluminum", "2" }, { "avp_human:white_padding", "1" }, { "avp_human:rubber_globule", "1" } };
            case HYDROPONIC_UNIT -> new String[][] { { "#c:ingots/steel", "3" }, { "#c:ingots/aluminum", "2" }, { "avp_human:industrial_glass", "1" }, { "avp_human:rubber_globule", "1" }, { "minecraft:oak_sapling", "1" }, { "minecraft:dirt", "1" } };
            case PLANT_CABINET -> new String[][] { { "#c:ingots/steel", "8" }, { "#c:ingots/aluminum", "2" }, { "minecraft:oak_sapling", "1" }, { "minecraft:redstone", "1" } };
            case ENTERTAINMENT_CONSOLE -> new String[][] { { "#c:ingots/steel", "1" }, { "avp_human:rubber_globule", "4" }, { "avp_human:integrated_circuit", "1" } };
            case ARMOR_STAND -> new String[][] { { "#c:ingots/steel", "1" }, { "#c:ingots/aluminum", "2" } };
            case BOTANICAL_TABLE -> new String[][] { { "#c:ingots/steel", "1" }, { "minecraft:oak_planks", "1" }, { "minecraft:oak_sapling", "1" }, { "minecraft:dirt", "1" } };
            case RECREATION_TABLE -> new String[][] { { "#c:ingots/steel", "1" }, { "#c:ingots/aluminum", "2" }, { "avp_human:white_padding", "1" }, { "minecraft:oak_planks", "1" }, { "minecraft:paper", "1" } };
            case DESKTOP_TERMINAL -> new String[][] { { "#c:ingots/steel", "1" }, { "avp_human:rubber_globule", "1" }, { "avp_human:integrated_circuit", "1" } };
            case KEYBOARD -> new String[][] { { "#c:ingots/steel", "1" }, { "avp_human:rubber_globule", "1" }, { "minecraft:redstone", "1" } };
            case PAPERWORK_STACK -> new String[][] { { "#c:ingots/steel", "1" }, { "minecraft:paper", "2" } };
            case CLIPBOARD -> new String[][] { { "#c:ingots/steel", "1" }, { "minecraft:oak_planks", "1" }, { "minecraft:paper", "1" } };
            case BOOK_STACK -> new String[][] { { "#c:ingots/steel", "1" } };
            case COFFEE_MUG -> new String[][] { { "#c:ingots/steel", "1" }, { "minecraft:dirt", "1" } };
            case FOOD_TRAY -> new String[][] { { "#c:ingots/steel", "1" }, { "minecraft:oak_planks", "1" }, { "minecraft:dirt", "1" } };
            case TOOL_TRAY -> new String[][] { { "#c:ingots/steel", "1" } };
            case BOTTLES -> new String[][] { { "#c:ingots/steel", "1" }, { "avp_human:industrial_glass", "1" } };
            case TEST_TUBES -> new String[][] { { "#c:ingots/steel", "1" }, { "avp_human:industrial_glass", "1" } };
            case SAMPLE_CASE -> new String[][] { { "#c:ingots/steel", "1" } };
            case FIRST_AID_KIT -> new String[][] { { "#c:ingots/steel", "1" } };
            case FIRE_EXTINGUISHER -> new String[][] { { "#c:ingots/steel", "1" } };
            case SPARE_HARD_HAT -> new String[][] { { "#c:ingots/steel", "1" }, { "minecraft:redstone", "1" } };
            case BOOT_PAIR -> new String[][] { { "#c:ingots/steel", "1" }, { "avp_human:rubber_globule", "1" }, { "minecraft:paper", "1" } };
            case DUFFEL_BAG -> new String[][] { { "#c:ingots/steel", "1" } };
            case TOOLBOX -> new String[][] { { "#c:ingots/steel", "1" } };
            case TAPE_ROLL -> new String[][] { { "#c:ingots/steel", "1" } };
            case FACILITY_SIGN -> new String[][] { { "#c:ingots/steel", "1" } };
            case HANDHELD_DEVICE -> new String[][] { { "#c:ingots/steel", "1" }, { "avp_human:integrated_circuit", "1" } };
            case COMMS_RADIO -> new String[][] { { "#c:ingots/steel", "1" }, { "minecraft:redstone", "1" } };
            case FILE_STACK -> new String[][] { { "#c:ingots/steel", "1" }, { "minecraft:paper", "1" } };
            case ALARM_SIREN -> new String[][] { { "#c:ingots/steel", "1" }, { "minecraft:redstone", "1" } };
            case INTERCOM -> new String[][] { { "#c:ingots/steel", "1" }, { "minecraft:redstone", "1" } };
            case WALL_SPEAKER -> new String[][] { { "#c:ingots/steel", "1" }, { "avp_human:rubber_globule", "1" } };
            case SECURITY_CAMERA -> new String[][] { { "#c:ingots/steel", "1" }, { "minecraft:redstone", "1" } };
            case WALL_CLOCK -> new String[][] { { "#c:ingots/steel", "1" }, { "minecraft:paper", "1" } };
            case WARNING_SIGN -> new String[][] { { "#c:ingots/steel", "1" } };
            case MAP_PANEL -> new String[][] { { "#c:ingots/steel", "1" }, { "avp_human:industrial_glass", "1" } };
            case ELECTRICAL_BOX -> new String[][] { { "#c:ingots/steel", "1" } };
            case SWITCH_PANEL -> new String[][] { { "#c:ingots/steel", "1" }, { "minecraft:redstone", "1" } };
            case KEYPAD -> new String[][] { { "#c:ingots/steel", "1" }, { "minecraft:redstone", "1" } };
            case WALL_HANDSET -> new String[][] { { "#c:ingots/steel", "1" } };
            case SCREEN_PANEL -> new String[][] { { "#c:ingots/steel", "1" }, { "avp_human:rubber_globule", "1" }, { "avp_human:integrated_circuit", "1" } };
            case SCREEN_BOARD -> new String[][] { { "#c:ingots/steel", "1" }, { "avp_human:rubber_globule", "1" }, { "avp_human:integrated_circuit", "1" } };
            case PIPE_MOUNT -> new String[][] { { "#c:ingots/steel", "1" } };
            case CABLE_HOOK -> new String[][] { { "#c:ingots/steel", "1" }, { "avp_human:rubber_globule", "1" } };
            case TOOL_RACK -> new String[][] { { "#c:ingots/steel", "1" }, { "minecraft:oak_planks", "1" } };
            case COAT_HOOK -> new String[][] { { "#c:ingots/steel", "1" }, { "avp_human:white_padding", "1" } };
            case WALL_SHELF -> new String[][] { { "#c:ingots/steel", "1" } };
            case PETRI_DISHES -> new String[][] { { "#c:ingots/steel", "1" }, { "avp_human:industrial_glass", "1" } };
            case INSTRUMENT_TRAY -> new String[][] { { "#c:ingots/steel", "1" }, { "minecraft:paper", "1" } };
            case DATA_PAD -> new String[][] { { "#c:ingots/steel", "1" }, { "avp_human:rubber_globule", "1" }, { "avp_human:integrated_circuit", "1" } };
            case DESK_LAMP -> new String[][] { { "#c:ingots/steel", "1" }, { "minecraft:redstone", "1" } };
            case WALL_MONITOR -> new String[][] { { "#c:ingots/steel", "1" }, { "avp_human:rubber_globule", "1" }, { "avp_human:integrated_circuit", "1" } };
            case SAMPLE_CAROUSEL -> new String[][] { { "#c:ingots/steel", "1" }, { "avp_human:industrial_glass", "1" } };
            case PORTABLE_LIGHT -> new String[][] { { "#c:ingots/steel", "1" }, { "minecraft:redstone", "1" } };
            case DESKTOP_TERRARIUM -> new String[][] { { "#c:ingots/steel", "1" }, { "avp_human:industrial_glass", "1" }, { "minecraft:oak_sapling", "1" }, { "minecraft:dirt", "1" }, { "minecraft:redstone", "1" } };
            case TALL_GROWTH_CHAMBER -> new String[][] { { "#c:ingots/steel", "2" }, { "#c:ingots/aluminum", "2" }, { "avp_human:industrial_glass", "1" }, { "avp_human:rubber_globule", "1" }, { "minecraft:oak_sapling", "1" }, { "minecraft:dirt", "1" } };
            case WIDE_GROW_RACK -> new String[][] { { "#c:ingots/steel", "4" }, { "#c:ingots/aluminum", "3" }, { "avp_human:rubber_globule", "1" }, { "minecraft:oak_sapling", "1" }, { "minecraft:dirt", "1" }, { "minecraft:redstone", "1" } };
            case HYDROPONIC_CABINET -> new String[][] { { "#c:ingots/steel", "6" }, { "#c:ingots/aluminum", "3" }, { "avp_human:industrial_glass", "1" }, { "minecraft:oak_sapling", "1" }, { "minecraft:dirt", "1" }, { "avp_human:integrated_circuit", "1" } };
            case CLIMATE_GROW_CHAMBER -> new String[][] { { "#c:ingots/steel", "10" }, { "#c:ingots/aluminum", "3" }, { "avp_human:industrial_glass", "3" }, { "minecraft:oak_sapling", "1" }, { "minecraft:dirt", "1" }, { "avp_human:integrated_circuit", "1" } };
            case AUTOMATED_PLANTER -> new String[][] { { "#c:ingots/steel", "5" }, { "#c:ingots/aluminum", "2" }, { "avp_human:rubber_globule", "1" }, { "minecraft:oak_sapling", "1" }, { "minecraft:dirt", "1" }, { "avp_human:integrated_circuit", "1" } };
            case HANGING_BOTANICAL_POD -> new String[][] { { "#c:ingots/steel", "1" }, { "avp_human:industrial_glass", "1" }, { "minecraft:oak_sapling", "1" }, { "minecraft:dirt", "1" } };
            case WALL_PLANT_CHAMBER -> new String[][] { { "#c:ingots/steel", "1" }, { "avp_human:industrial_glass", "1" }, { "minecraft:oak_sapling", "1" }, { "minecraft:dirt", "1" }, { "avp_human:integrated_circuit", "1" } };
            case AQUATIC_TERRARIUM -> new String[][] { { "#c:ingots/steel", "2" }, { "#c:ingots/aluminum", "2" }, { "avp_human:industrial_glass", "1" }, { "minecraft:oak_sapling", "1" }, { "minecraft:dirt", "1" }, { "avp_human:integrated_circuit", "1" } };
            case FUNGAL_GROW_CHAMBER -> new String[][] { { "#c:ingots/steel", "2" }, { "#c:ingots/aluminum", "2" }, { "avp_human:industrial_glass", "1" }, { "minecraft:oak_planks", "1" }, { "minecraft:paper", "1" }, { "avp_human:integrated_circuit", "1" } };
            case VERTICAL_BIO_DOME -> new String[][] { { "#c:ingots/steel", "10" }, { "#c:ingots/aluminum", "3" }, { "avp_human:industrial_glass", "3" }, { "minecraft:oak_planks", "1" }, { "minecraft:oak_sapling", "1" }, { "minecraft:dirt", "1" } };
            case ROOT_OBSERVATION_TANK -> new String[][] { { "#c:ingots/steel", "1" }, { "avp_human:industrial_glass", "1" }, { "minecraft:oak_planks", "1" }, { "minecraft:oak_sapling", "1" }, { "minecraft:dirt", "1" } };
            case SEED_GERMINATION_INCUBATOR -> new String[][] { { "#c:ingots/steel", "1" }, { "minecraft:redstone", "1" } };
            case BOTANICAL_EXAMINATION_BENCH -> new String[][] { { "#c:ingots/steel", "3" }, { "#c:ingots/aluminum", "2" }, { "avp_human:rubber_globule", "1" }, { "minecraft:oak_sapling", "1" }, { "avp_human:integrated_circuit", "1" } };
            case NUTRIENT_RESERVOIR -> new String[][] { { "#c:ingots/steel", "1" }, { "avp_human:industrial_glass", "1" }, { "minecraft:redstone", "1" } };
            case GENETICS_WORKBENCH -> new String[][] { { "#c:ingots/steel", "5" }, { "#c:ingots/aluminum", "2" }, { "avp_human:industrial_glass", "1" }, { "avp_human:rubber_globule", "1" }, { "avp_human:integrated_circuit", "1" } };
            case DNA_SEQUENCER -> new String[][] { { "#c:ingots/steel", "7" }, { "#c:ingots/aluminum", "2" }, { "avp_human:rubber_globule", "1" }, { "avp_human:integrated_circuit", "1" } };
            case SPLICING_WORKSTATION -> new String[][] { { "#c:ingots/steel", "8" }, { "#c:ingots/aluminum", "2" }, { "avp_human:rubber_globule", "1" }, { "avp_human:integrated_circuit", "1" } };
            case PCR_MACHINE -> new String[][] { { "#c:ingots/steel", "1" }, { "minecraft:redstone", "1" } };
            case GEL_ELECTROPHORESIS_UNIT -> new String[][] { { "#c:ingots/steel", "1" }, { "avp_human:industrial_glass", "1" }, { "minecraft:redstone", "1" } };
            case TISSUE_CULTURE_STATION -> new String[][] { { "#c:ingots/steel", "3" }, { "avp_human:industrial_glass", "1" }, { "avp_human:rubber_globule", "1" }, { "avp_human:integrated_circuit", "1" } };
            case CRYOGENIC_SAMPLE_STORAGE -> new String[][] { { "#c:ingots/steel", "8" }, { "#c:ingots/aluminum", "2" }, { "minecraft:redstone", "1" } };
            case CLONE_SAMPLE_POD -> new String[][] { { "#c:ingots/steel", "2" }, { "#c:ingots/aluminum", "2" }, { "avp_human:industrial_glass", "1" }, { "minecraft:redstone", "1" } };
            case MICROARRAY_STATION -> new String[][] { { "#c:ingots/steel", "1" }, { "avp_human:integrated_circuit", "1" } };
            case GENE_ANALYSIS_TERMINAL -> new String[][] { { "#c:ingots/steel", "2" }, { "avp_human:rubber_globule", "1" }, { "avp_human:integrated_circuit", "1" } };
            case SPECIMEN_FREEZER -> new String[][] { { "#c:ingots/steel", "8" }, { "#c:ingots/aluminum", "2" }, { "minecraft:redstone", "1" } };
            case REAGENT_MIXER -> new String[][] { { "#c:ingots/steel", "1" }, { "avp_human:industrial_glass", "1" }, { "minecraft:paper", "1" }, { "minecraft:redstone", "1" } };
            case BIOSAFETY_CABINET -> new String[][] { { "#c:ingots/steel", "10" }, { "#c:ingots/aluminum", "3" }, { "avp_human:industrial_glass", "1" }, { "avp_human:rubber_globule", "1" }, { "avp_human:integrated_circuit", "1" } };
            case BIOHAZARD_WORKBENCH -> new String[][] { { "#c:ingots/steel", "8" }, { "#c:ingots/aluminum", "2" }, { "avp_human:industrial_glass", "1" }, { "avp_human:rubber_globule", "1" }, { "avp_human:integrated_circuit", "1" } };
            case VIRUS_INCUBATOR -> new String[][] { { "#c:ingots/steel", "8" }, { "#c:ingots/aluminum", "2" }, { "avp_human:industrial_glass", "1" }, { "minecraft:redstone", "1" } };
            case CONTAINMENT_GLOVEBOX -> new String[][] { { "#c:ingots/steel", "8" }, { "#c:ingots/aluminum", "2" }, { "avp_human:industrial_glass", "1" }, { "avp_human:rubber_globule", "1" }, { "minecraft:redstone", "1" } };
            case ISOLATION_CHAMBER -> new String[][] { { "#c:ingots/steel", "8" }, { "#c:ingots/aluminum", "3" }, { "avp_human:industrial_glass", "2" }, { "avp_human:white_padding", "1" }, { "minecraft:redstone", "1" } };
            case DECONTAMINATION_SHOWER -> new String[][] { { "#c:ingots/steel", "2" }, { "#c:ingots/aluminum", "2" }, { "avp_human:industrial_glass", "1" }, { "minecraft:redstone", "1" } };
            case AUTOCLAVE -> new String[][] { { "#c:ingots/steel", "4" }, { "minecraft:redstone", "1" } };
            case HAZMAT_STORAGE -> new String[][] { { "#c:ingots/steel", "4" } };
            case FLUID_STORAGE_TANK -> new String[][] { { "#c:ingots/steel", "1" }, { "avp_human:industrial_glass", "1" }, { "avp_human:rubber_globule", "1" }, { "minecraft:redstone", "1" } };
            case SAMPLE_FREEZER -> new String[][] { { "#c:ingots/steel", "4" }, { "minecraft:redstone", "1" } };
            case PATHOGEN_ANALYZER -> new String[][] { { "#c:ingots/steel", "1" }, { "avp_human:rubber_globule", "1" }, { "avp_human:integrated_circuit", "1" } };
            case WASTE_DISPOSAL_UNIT -> new String[][] { { "#c:ingots/steel", "3" }, { "avp_human:rubber_globule", "1" }, { "minecraft:redstone", "1" } };
            case QUARANTINE_SPECIMEN_CASE -> new String[][] { { "#c:ingots/steel", "1" }, { "avp_human:industrial_glass", "1" }, { "minecraft:redstone", "1" } };
            case CRYSTAL_GROWTH_CHAMBER -> new String[][] { { "#c:ingots/steel", "7" }, { "#c:ingots/aluminum", "3" }, { "avp_human:industrial_glass", "1" }, { "avp_human:rubber_globule", "1" }, { "minecraft:amethyst_shard", "4" }, { "avp_human:integrated_circuit", "1" } };
            case CRYSTAL_SAMPLE -> new String[][] { { "#c:ingots/steel", "1" }, { "minecraft:amethyst_shard", "1" }, { "minecraft:paper", "1" } };
            case GEOLOGY_WORKBENCH -> new String[][] { { "#c:ingots/steel", "4" }, { "#c:ingots/aluminum", "2" }, { "avp_human:industrial_glass", "1" }, { "avp_human:rubber_globule", "1" }, { "avp_human:integrated_circuit", "1" } };
            case ROCK_ANALYZER -> new String[][] { { "#c:ingots/steel", "1" }, { "avp_human:rubber_globule", "1" }, { "avp_human:integrated_circuit", "1" } };
            case CRYSTAL_DISPLAY_CASE -> new String[][] { { "#c:ingots/steel", "2" }, { "#c:ingots/aluminum", "2" }, { "avp_human:industrial_glass", "1" }, { "minecraft:amethyst_shard", "1" }, { "minecraft:redstone", "1" } };
            case GEODE_CUTTER -> new String[][] { { "#c:ingots/steel", "2" }, { "minecraft:amethyst_shard", "1" }, { "minecraft:redstone", "1" } };
            case MINERAL_PRESS -> new String[][] { { "#c:ingots/steel", "1" }, { "minecraft:redstone", "1" } };
            case SPECTROMETER -> new String[][] { { "#c:ingots/steel", "1" }, { "avp_human:industrial_glass", "1" }, { "avp_human:integrated_circuit", "1" } };
            case THIN_SECTION_STATION -> new String[][] { { "#c:ingots/steel", "1" }, { "avp_human:industrial_glass", "1" }, { "avp_human:integrated_circuit", "1" } };
            case ROCK_SAW -> new String[][] { { "#c:ingots/steel", "1" }, { "minecraft:redstone", "1" } };
            case CORE_SAMPLE_RACK -> new String[][] { { "#c:ingots/steel", "2" }, { "#c:ingots/aluminum", "2" }, { "minecraft:dirt", "1" } };
            case SEISMIC_MONITOR -> new String[][] { { "#c:ingots/steel", "3" }, { "avp_human:rubber_globule", "1" }, { "minecraft:paper", "1" }, { "avp_human:integrated_circuit", "1" } };
            case CRYSTAL_CLUSTER_SMALL -> new String[][] { { "#c:ingots/steel", "1" }, { "minecraft:amethyst_shard", "1" } };
            case GEODE_FORMATION -> new String[][] { { "#c:ingots/steel", "1" }, { "minecraft:amethyst_shard", "1" } };
            case MINERAL_PEDESTAL -> new String[][] { { "#c:ingots/steel", "1" }, { "minecraft:amethyst_shard", "1" }, { "minecraft:redstone", "1" } };
            case WALL_MINERAL_COLLECTION -> new String[][] { { "#c:ingots/steel", "1" }, { "#c:ingots/aluminum", "2" }, { "minecraft:oak_planks", "1" }, { "minecraft:amethyst_shard", "1" }, { "minecraft:dirt", "1" } };
            case FOSSIL_DISPLAY -> new String[][] { { "#c:ingots/steel", "2" }, { "#c:ingots/aluminum", "3" }, { "minecraft:paper", "1" } };
            case SPECIMEN_TANK -> new String[][] { { "#c:ingots/steel", "3" }, { "#c:ingots/aluminum", "2" }, { "avp_human:industrial_glass", "1" }, { "avp_human:integrated_circuit", "1" } };
            case VERTICAL_SPECIMEN_CHAMBER -> new String[][] { { "#c:ingots/steel", "3" }, { "#c:ingots/aluminum", "2" }, { "avp_human:industrial_glass", "1" }, { "avp_human:integrated_circuit", "1" } };
            case SKELETON_DISPLAY -> new String[][] { { "#c:ingots/steel", "1" }, { "#c:ingots/aluminum", "2" }, { "minecraft:paper", "1" } };
            case ORGAN_DISPLAY_CASE -> new String[][] { { "#c:ingots/steel", "1" }, { "avp_human:industrial_glass", "1" }, { "minecraft:redstone", "1" } };
            case XENOMORPH_SPECIMEN_TANK -> new String[][] { { "#c:ingots/steel", "10" }, { "#c:ingots/aluminum", "3" }, { "avp_human:industrial_glass", "3" }, { "avp_human:rubber_globule", "1" }, { "avp_human:integrated_circuit", "1" } };
            case ANATOMY_TABLE -> new String[][] { { "#c:ingots/steel", "2" }, { "#c:ingots/aluminum", "2" }, { "avp_human:rubber_globule", "1" }, { "minecraft:paper", "1" }, { "avp_human:integrated_circuit", "1" } };
            case PRESERVATION_POD -> new String[][] { { "#c:ingots/steel", "1" }, { "avp_human:industrial_glass", "1" }, { "minecraft:redstone", "1" } };
            case SPECIMEN_SHELF -> new String[][] { { "#c:ingots/steel", "3" }, { "#c:ingots/aluminum", "3" }, { "avp_human:industrial_glass", "1" } };
            case JAR_RACK -> new String[][] { { "#c:ingots/steel", "1" }, { "avp_human:industrial_glass", "1" }, { "minecraft:oak_planks", "1" } };
            case TISSUE_STORAGE -> new String[][] { { "#c:ingots/steel", "4" }, { "minecraft:redstone", "1" } };
            case SPECIMEN_LOCKER -> new String[][] { { "#c:ingots/steel", "8" }, { "#c:ingots/aluminum", "2" }, { "avp_human:industrial_glass", "1" }, { "minecraft:redstone", "1" } };
            case POTTED_PLANT_SET -> new String[][] { { "#c:ingots/steel", "1" }, { "minecraft:oak_sapling", "1" }, { "minecraft:dirt", "1" } };
            case HANGING_VINE_CLUSTER -> new String[][] { { "#c:ingots/steel", "1" }, { "minecraft:oak_sapling", "1" }, { "minecraft:dirt", "1" } };
            case SEED_TRAY -> new String[][] { { "#c:ingots/steel", "1" }, { "minecraft:oak_sapling", "1" }, { "minecraft:dirt", "1" }, { "minecraft:paper", "1" } };
            case PLANT_SAMPLE -> new String[][] { { "#c:ingots/steel", "1" }, { "avp_human:industrial_glass", "1" }, { "minecraft:oak_sapling", "1" }, { "minecraft:dirt", "1" }, { "minecraft:paper", "1" } };
            case FUNGUS_CULTURE_JAR -> new String[][] { { "#c:ingots/steel", "1" }, { "avp_human:industrial_glass", "1" }, { "minecraft:dirt", "1" }, { "minecraft:paper", "1" } };
            case GROWTH_MEDIA_JAR -> new String[][] { { "#c:ingots/steel", "1" }, { "avp_human:industrial_glass", "1" } };
            case SAMPLE_VIAL_RACK -> new String[][] { { "#c:ingots/steel", "1" }, { "avp_human:industrial_glass", "1" } };
            case DNA_SAMPLE_CANISTER -> new String[][] { { "#c:ingots/steel", "1" }, { "avp_human:industrial_glass", "1" } };
            case VIRUS_SAMPLE_CANISTER -> new String[][] { { "#c:ingots/steel", "1" }, { "avp_human:industrial_glass", "1" }, { "minecraft:redstone", "1" } };
            case ROCK_SAMPLE -> new String[][] { { "#c:ingots/steel", "1" }, { "minecraft:amethyst_shard", "1" }, { "minecraft:dirt", "1" }, { "minecraft:paper", "1" } };
            case CORE_SAMPLE -> new String[][] { { "#c:ingots/steel", "1" }, { "minecraft:oak_planks", "1" }, { "minecraft:dirt", "1" }, { "minecraft:paper", "1" } };
            case PIPETTE_RACK -> new String[][] { { "#c:ingots/steel", "1" }, { "avp_human:industrial_glass", "1" } };
            case SCALPEL_TRAY -> new String[][] { { "#c:ingots/steel", "1" }, { "avp_human:white_padding", "1" } };
            case SPECIMEN_NOTEBOOK -> new String[][] { { "#c:ingots/steel", "1" }, { "minecraft:paper", "1" } };
            case RESEARCH_WHITEBOARD -> new String[][] { { "#c:ingots/steel", "1" }, { "#c:ingots/aluminum", "2" }, { "minecraft:amethyst_shard", "1" }, { "minecraft:paper", "3" } };
            case BIOHAZARD_SIGN -> new String[][] { { "#c:ingots/steel", "1" }, { "avp_human:rubber_globule", "1" } };
            case GEOLOGICAL_FIELD_CASE -> new String[][] { { "#c:ingots/steel", "1" }, { "minecraft:amethyst_shard", "1" } };
            case VERTICAL_WEAPON_RACK -> new String[][] { { "#c:ingots/steel", "2" }, { "#c:ingots/aluminum", "2" }, { "avp_human:rubber_globule", "1" }, { "minecraft:redstone", "1" } };
            case DUAL_WEAPON_RACK -> new String[][] { { "#c:ingots/steel", "4" }, { "#c:ingots/aluminum", "3" }, { "avp_human:rubber_globule", "1" }, { "minecraft:redstone", "1" } };
            case WALL_WEAPON_RACK -> new String[][] { { "#c:ingots/steel", "1" }, { "#c:ingots/aluminum", "2" }, { "avp_human:rubber_globule", "1" }, { "minecraft:redstone", "1" } };
            case MODULAR_WEAPON_RACK -> new String[][] { { "#c:ingots/steel", "1" }, { "#c:ingots/aluminum", "2" } };
            case ARMORY_DISPLAY_CASE -> new String[][] { { "#c:ingots/steel", "8" }, { "#c:ingots/aluminum", "3" }, { "avp_human:industrial_glass", "1" }, { "avp_human:rubber_globule", "1" }, { "minecraft:redstone", "1" } };
            case GEAR_CAGE -> new String[][] { { "#c:ingots/steel", "7" }, { "#c:ingots/aluminum", "3" } };
            case AMMO_GEAR_SHELF -> new String[][] { { "#c:ingots/steel", "8" }, { "#c:ingots/aluminum", "3" }, { "avp_human:rubber_globule", "1" }, { "minecraft:redstone", "1" } };
            case WEAPON_MAINTENANCE_BENCH -> new String[][] { { "#c:ingots/steel", "10" }, { "#c:ingots/aluminum", "3" }, { "avp_human:rubber_globule", "1" }, { "minecraft:oak_planks", "1" }, { "avp_human:integrated_circuit", "1" } };
            case MAPPING_TABLE -> new String[][] { { "#c:ingots/steel", "10" }, { "#c:ingots/aluminum", "3" }, { "avp_human:rubber_globule", "4" }, { "avp_human:integrated_circuit", "1" } };
            case OPS_CONSOLE -> new String[][] { { "#c:ingots/steel", "9" }, { "#c:ingots/aluminum", "2" }, { "avp_human:rubber_globule", "1" }, { "avp_human:integrated_circuit", "1" } };
            case MULTI_MONITOR_STATION -> new String[][] { { "#c:ingots/steel", "10" }, { "#c:ingots/aluminum", "2" }, { "avp_human:rubber_globule", "2" }, { "avp_human:integrated_circuit", "1" } };
            case TACTICAL_CONSOLE -> new String[][] { { "#c:ingots/steel", "9" }, { "#c:ingots/aluminum", "2" }, { "avp_human:rubber_globule", "1" }, { "avp_human:integrated_circuit", "1" } };
            case SECURITY_CONSOLE -> new String[][] { { "#c:ingots/steel", "4" }, { "avp_human:rubber_globule", "1" }, { "avp_human:integrated_circuit", "1" } };
            case COMMS_CONSOLE -> new String[][] { { "#c:ingots/steel", "9" }, { "#c:ingots/aluminum", "2" }, { "avp_human:rubber_globule", "1" }, { "avp_human:integrated_circuit", "1" } };
            case SURVEILLANCE_STATION -> new String[][] { { "#c:ingots/steel", "10" }, { "#c:ingots/aluminum", "2" }, { "avp_human:rubber_globule", "2" }, { "avp_human:integrated_circuit", "1" } };
            case HOLO_PROJECTOR -> new String[][] { { "#c:ingots/steel", "1" }, { "avp_human:rubber_globule", "1" }, { "minecraft:redstone", "1" } };
            case COMMAND_TERMINAL -> new String[][] { { "#c:ingots/steel", "2" }, { "avp_human:rubber_globule", "1" }, { "avp_human:integrated_circuit", "1" } };
            case BRIEFING_TABLE -> new String[][] { { "#c:ingots/steel", "10" }, { "#c:ingots/aluminum", "3" }, { "avp_human:rubber_globule", "4" }, { "minecraft:redstone", "1" } };
            case WALL_DISPLAY_ARRAY -> new String[][] { { "#c:ingots/steel", "6" }, { "#c:ingots/aluminum", "3" }, { "avp_human:rubber_globule", "4" }, { "minecraft:redstone", "1" } };
            case PLANNING_BOARD -> new String[][] { { "#c:ingots/steel", "1" }, { "#c:ingots/aluminum", "2" }, { "minecraft:amethyst_shard", "1" }, { "minecraft:paper", "3" }, { "minecraft:redstone", "1" } };
            case DATA_SERVER_RACK -> new String[][] { { "#c:ingots/steel", "8" }, { "#c:ingots/aluminum", "2" }, { "avp_human:rubber_globule", "1" }, { "minecraft:redstone", "1" } };
            case COMMS_RACK -> new String[][] { { "#c:ingots/steel", "8" }, { "#c:ingots/aluminum", "2" }, { "avp_human:rubber_globule", "1" }, { "minecraft:redstone", "1" } };
            case LATHE -> new String[][] { { "#c:ingots/steel", "3" }, { "#c:ingots/aluminum", "2" }, { "avp_human:rubber_globule", "1" }, { "minecraft:redstone", "1" } };
            case CNC_MILL -> new String[][] { { "#c:ingots/steel", "10" }, { "#c:ingots/aluminum", "3" }, { "avp_human:industrial_glass", "1" }, { "avp_human:rubber_globule", "1" }, { "avp_human:integrated_circuit", "1" } };
            case DRILL_PRESS -> new String[][] { { "#c:ingots/steel", "2" }, { "#c:ingots/aluminum", "2" }, { "minecraft:redstone", "1" } };
            case WELDER -> new String[][] { { "#c:ingots/steel", "2" }, { "#c:ingots/aluminum", "2" }, { "avp_human:rubber_globule", "1" }, { "minecraft:redstone", "1" } };
            case PARTS_WASHER -> new String[][] { { "#c:ingots/steel", "3" }, { "minecraft:redstone", "1" } };
            case HYDRAULIC_PRESS -> new String[][] { { "#c:ingots/steel", "9" }, { "#c:ingots/aluminum", "3" }, { "minecraft:redstone", "1" } };
            case ASSEMBLY_RIG -> new String[][] { { "#c:ingots/steel", "9" }, { "#c:ingots/aluminum", "3" }, { "minecraft:redstone", "1" } };
            case FRAME_WELDER -> new String[][] { { "#c:ingots/steel", "10" }, { "#c:ingots/aluminum", "3" }, { "avp_human:rubber_globule", "1" }, { "avp_human:integrated_circuit", "1" } };
            case PARTS_FABRICATOR -> new String[][] { { "#c:ingots/steel", "10" }, { "#c:ingots/aluminum", "3" }, { "avp_human:rubber_globule", "1" }, { "avp_human:integrated_circuit", "1" } };
            case TABLE_SAW -> new String[][] { { "#c:ingots/steel", "3" }, { "#c:ingots/aluminum", "2" }, { "minecraft:oak_planks", "1" }, { "minecraft:redstone", "1" } };
            case BAND_SAW -> new String[][] { { "#c:ingots/steel", "3" }, { "#c:ingots/aluminum", "2" }, { "avp_human:rubber_globule", "1" }, { "minecraft:redstone", "1" } };
            case COLD_SAW -> new String[][] { { "#c:ingots/steel", "2" } };
            case BENCH_GRINDER -> new String[][] { { "#c:ingots/steel", "1" }, { "minecraft:redstone", "1" } };
            case CYBERNETICS_BENCH -> new String[][] { { "#c:ingots/steel", "4" }, { "#c:ingots/aluminum", "2" }, { "avp_human:rubber_globule", "1" }, { "avp_human:integrated_circuit", "1" } };
            case PROSTHETIC_ASSEMBLY -> new String[][] { { "#c:ingots/steel", "4" }, { "#c:ingots/aluminum", "2" }, { "minecraft:redstone", "1" } };
            case NEURAL_INTERFACE -> new String[][] { { "#c:ingots/steel", "2" }, { "#c:ingots/aluminum", "2" }, { "avp_human:white_padding", "2" }, { "avp_human:rubber_globule", "1" }, { "avp_human:integrated_circuit", "1" } };
            case LIMB_FABRICATOR -> new String[][] { { "#c:ingots/steel", "10" }, { "#c:ingots/aluminum", "3" }, { "avp_human:industrial_glass", "3" }, { "minecraft:paper", "1" }, { "avp_human:integrated_circuit", "1" } };
            case EXOSKELETON_RIG -> new String[][] { { "#c:ingots/steel", "9" }, { "#c:ingots/aluminum", "3" }, { "avp_human:rubber_globule", "1" }, { "avp_human:integrated_circuit", "1" } };
            case SYNTH_REPAIR_STATION -> new String[][] { { "#c:ingots/steel", "2" }, { "#c:ingots/aluminum", "2" }, { "avp_human:rubber_globule", "1" }, { "minecraft:paper", "3" }, { "avp_human:integrated_circuit", "1" } };
            case ROBOT_ASSEMBLY_BAY -> new String[][] { { "#c:ingots/steel", "9" }, { "#c:ingots/aluminum", "3" }, { "minecraft:redstone", "1" } };
            case DRONE_WORKSTATION -> new String[][] { { "#c:ingots/steel", "4" }, { "#c:ingots/aluminum", "2" }, { "avp_human:rubber_globule", "1" }, { "avp_human:integrated_circuit", "1" } };
            case SENTRY_TURRET_BUILD -> new String[][] { { "#c:ingots/steel", "4" }, { "#c:ingots/aluminum", "2" }, { "minecraft:redstone", "1" } };
            case UTILITY_DRONE_PAD -> new String[][] { { "#c:ingots/steel", "3" }, { "#c:ingots/aluminum", "3" }, { "minecraft:paper", "1" }, { "minecraft:redstone", "1" } };
            case ROBOT_FRAME_RACK -> new String[][] { { "#c:ingots/steel", "5" }, { "#c:ingots/aluminum", "3" }, { "minecraft:redstone", "1" } };
            case CHARGING_STATION -> new String[][] { { "#c:ingots/steel", "6" }, { "#c:ingots/aluminum", "2" }, { "avp_human:rubber_globule", "1" }, { "minecraft:redstone", "1" } };
        };
    }

    private static String[][] extra(CosmeticStyle style) {
        return switch (style) {
            case WY -> new String[][] { { "avp_human:white_plastic", "2" }, { "minecraft:yellow_dye", "1" } };
            case USCM -> new String[][] { { "#c:ingots/composite", "2" }, { "minecraft:green_dye", "1" } };
            default -> new String[0][];
        };
    }

    /** Every entry. Built on first use, after the registries are filled. */
    public static List<Entry> entries() {
        if (entries == null) {
            var built = new ArrayList<Entry>();

            for (var design : CosmeticDesign.values()) {
                for (var style : CosmeticStyle.values()) {
                    var result = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("avp_human", design.blockName(style)));

                    if (result == net.minecraft.world.item.Items.AIR) {
                        continue;
                    }

                    var parts = new ArrayList<FabricatingRecipe.Part>();

                    for (var line : base(design)) {
                        parts.add(part(line));
                    }

                    for (var line : extra(style)) {
                        parts.add(part(line));
                    }

                    built.add(new Entry(built.size(), design, style, List.copyOf(parts), result));
                }
            }

            entries = List.copyOf(built);
        }

        return entries;
    }

    public static @Nullable Entry get(int index) {
        var all = entries();

        return index >= 0 && index < all.size() ? all.get(index) : null;
    }

    /** The entry for {@code design} in {@code style}, or null when that edition does not exist. */
    public static @Nullable Entry find(CosmeticDesign design, CosmeticStyle style) {
        for (var entry : entries()) {
            if (entry.design() == design && entry.style() == style) {
                return entry;
            }
        }

        return null;
    }

    private static FabricatingRecipe.Part part(String[] line) {
        var id = line[0];
        var count = Integer.parseInt(line[1]);

        if (id.startsWith("#")) {
            return new FabricatingRecipe.Part(Ingredient.of(TagKey.create(Registries.ITEM, ResourceLocation.parse(id.substring(1)))), count);
        }

        return new FabricatingRecipe.Part(Ingredient.of(BuiltInRegistries.ITEM.get(ResourceLocation.parse(id))), count);
    }
}
