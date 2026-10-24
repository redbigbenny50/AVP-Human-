package com.human.common.registry.init.creative_mode_tab.initializer;

import com.human.common.registry.init.HumanBlocks;
import com.human.common.registry.init.block.CoreBlocks;
import com.human.common.registry.init.item.HumanItems;
import net.minecraft.world.item.CreativeModeTab;

import java.util.function.Consumer;

/**
 * Everything that generates, carries, stores or is built into a power machine.
 * <p>
 * ⚠ These entries are MOVED here, not copied - a stack that appears in two tabs looks like a bug to the player and
 * makes the search tab list it twice. Latex and rubber BLOCKS deliberately stay in the Blocks tab: they are building
 * blocks that happen to be craftable from the latex chain, not machine parts.
 * </p>
 * <p>
 * Ordered the way a player builds: make power, move it, store it, then the parts the machines are made of.
 * </p>
 */
public class PowerCreativeModeTabInitializer {

    public static final Consumer<CreativeModeTab.Output> OUTPUT_CONSUMER = output -> {
        // Generators
        CreativeModeTabUtil.accept(output, HumanBlocks.SOLAR_PANEL);
        CreativeModeTabUtil.accept(output, HumanBlocks.AEROKINETIC_GENERATOR);
        CreativeModeTabUtil.accept(output, HumanBlocks.HYDROELECTRIC_GENERATOR);
        CreativeModeTabUtil.accept(output, HumanBlocks.THERMOELECTRIC_GENERATOR);
        // [stated] Oct 20 - creative blocks at the end of their own sections.
        CreativeModeTabUtil.accept(output, HumanBlocks.INFINITE_POWER_GENERATOR);

        // Distribution and storage
        CreativeModeTabUtil.accept(output, HumanBlocks.CABLE);
        CreativeModeTabUtil.accept(output, HumanBlocks.CRYOCONDUIT);
        CreativeModeTabUtil.accept(output, HumanBlocks.CRYOTANK);
        CreativeModeTabUtil.accept(output, HumanBlocks.CRYOTUBE_CONTROLLER);
        CreativeModeTabUtil.accept(output, HumanBlocks.STEEL_CRYOTUBE_CONTROLLER);
        CreativeModeTabUtil.accept(output, HumanBlocks.BATTERY);
        CreativeModeTabUtil.accept(output, HumanBlocks.CHARGING_HUB);
        CreativeModeTabUtil.accept(output, com.human.common.registry.init.HumanCreativeBlocks.CREATIVE_BATTERY);
        CreativeModeTabUtil.accept(output, HumanItems.WIRING_TOOL);
        CreativeModeTabUtil.accept(output, HumanItems.CAPACITY_UPGRADE);
        CreativeModeTabUtil.accept(output, HumanItems.TRANSFER_UPGRADE);
        CreativeModeTabUtil.accept(output, HumanItems.EMERGENCY_UPGRADE);
        CreativeModeTabUtil.accept(output, HumanItems.REDSTONE_UPGRADE);
        CreativeModeTabUtil.accept(output, HumanItems.ALARM_UPGRADE);
        CreativeModeTabUtil.accept(output, HumanItems.ARMORED_UPGRADE);
        CreativeModeTabUtil.accept(output, HumanItems.SPEED_UPGRADE);
        CreativeModeTabUtil.accept(output, HumanItems.EFFICIENCY_UPGRADE);
        CreativeModeTabUtil.accept(output, HumanItems.RANGE_UPGRADE);
        CreativeModeTabUtil.accept(output, HumanItems.WIDE_CONE_UPGRADE);
        CreativeModeTabUtil.accept(output, HumanItems.COOLING_UPGRADE);
        CreativeModeTabUtil.accept(output, HumanItems.DEFENSE_UPGRADE);
        // [stated] Oct 2: the built-in skins, as kits.
        for (var skin : new String[] { "weyland_yutani", "gold", "xeno" }) {
            output.accept(
                com.human.common.gameplay.item.SkinKitItem.of(HumanItems.SKIN_KIT.get(), com.human.HumanResources.location(skin))
            );
        }
        CreativeModeTabUtil.accept(output, HumanItems.MOTION_SENSOR);
        CreativeModeTabUtil.accept(output, HumanItems.EM_SENSOR);
        CreativeModeTabUtil.accept(output, HumanItems.CAMERA_SENSOR);
        CreativeModeTabUtil.accept(output, HumanItems.NETWORK_CARD);
        CreativeModeTabUtil.accept(output, HumanItems.CASELESS_AMMO_UPGRADE);
        CreativeModeTabUtil.accept(output, HumanItems.ROCKET_AMMO_UPGRADE);

        // Tier-1 machines
        CreativeModeTabUtil.accept(output, HumanBlocks.MACHINE_BLOCK);
        CreativeModeTabUtil.accept(output, HumanBlocks.ELECTRIC_FURNACE);
        CreativeModeTabUtil.accept(output, HumanBlocks.ELECTRIC_SMOKER);
        CreativeModeTabUtil.accept(output, HumanBlocks.ELECTRIC_BLAST_FURNACE);
        CreativeModeTabUtil.accept(output, HumanBlocks.ELECTRIC_INDUSTRIAL_FURNACE);
        CreativeModeTabUtil.accept(output, HumanBlocks.ELECTRIC_GRINDER);
        CreativeModeTabUtil.accept(output, HumanBlocks.FUSE_PANEL);
        CreativeModeTabUtil.accept(output, HumanBlocks.FLUID_PIPE);
        CreativeModeTabUtil.accept(output, HumanBlocks.ITEM_PIPE);
        CreativeModeTabUtil.accept(output, HumanItems.HUB_UPGRADE);
        CreativeModeTabUtil.accept(output, HumanBlocks.ELECTRIC_PRESS);
        CreativeModeTabUtil.accept(output, HumanBlocks.ELECTRIC_HYDRATOR);
        CreativeModeTabUtil.accept(output, HumanBlocks.ELECTRIC_FREEZER);
        CreativeModeTabUtil.accept(output, HumanBlocks.ELECTRONICS_WORKSTATION);
        CreativeModeTabUtil.accept(output, com.human.common.registry.init.HumanFabrication.DRIVE_BAY);
        CreativeModeTabUtil.accept(output, com.human.common.registry.init.HumanFabrication.FABRICATOR);
        // [stated] Oct 17 - the vaporizer. Its W-Y and USCM editions (and every other faction machine) live in their
        // faction tabs since Sep 28.
        CreativeModeTabUtil.accept(output, com.human.common.registry.init.HumanVaporizer.VAPORIZER);
        CreativeModeTabUtil.accept(output, com.human.common.registry.init.HumanDistributor.DISTRIBUTOR);
        CreativeModeTabUtil.accept(output, com.human.common.registry.init.HumanQuarry.QUARRY_TERMINAL);
        CreativeModeTabUtil.accept(output, com.human.common.registry.init.HumanQuarry.QUARRY_FRAME);
        CreativeModeTabUtil.accept(output, HumanBlocks.STEEL_FLUID_TANK);
        CreativeModeTabUtil.accept(output, HumanBlocks.FERROALUMINUM_FLUID_TANK);
        CreativeModeTabUtil.accept(output, HumanBlocks.ALUMINUM_FLUID_TANK);
        CreativeModeTabUtil.accept(output, HumanBlocks.TITANIUM_FLUID_TANK);
        CreativeModeTabUtil.accept(output, com.human.common.registry.init.HumanCreativeBlocks.CREATIVE_FLUID_TANK);

        // Latex production - the rubber for cable insulation starts here
        CreativeModeTabUtil.accept(output, CoreBlocks.LATEX_TAP);

        // Machine components
        CreativeModeTabUtil.accept(output, HumanItems.COPPER_COIL);
        CreativeModeTabUtil.accept(output, HumanItems.ELECTRIC_MOTOR);
        CreativeModeTabUtil.accept(output, HumanItems.TURBINE);
        CreativeModeTabUtil.accept(output, HumanItems.FAN);
        CreativeModeTabUtil.accept(output, HumanItems.PHOTOVOLTAIC_ARRAY);
        CreativeModeTabUtil.accept(output, HumanItems.HEAT_SINK);
        CreativeModeTabUtil.accept(output, HumanItems.THERMAL_PLATE);
    };
}
