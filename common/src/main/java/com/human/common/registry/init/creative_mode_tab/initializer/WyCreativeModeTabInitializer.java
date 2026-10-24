package com.human.common.registry.init.creative_mode_tab.initializer;

import com.human.common.gameplay.storage.CrateFamily;
import com.human.common.registry.init.HumanBlocks;
import com.human.common.registry.init.HumanCrates;
import com.human.common.registry.init.HumanDistributor;
import com.human.common.registry.init.HumanFabrication;
import com.human.common.registry.init.HumanQuarry;
import com.human.common.registry.init.HumanVaporizer;
import com.human.common.registry.init.block.HumanWyBlocks;
import net.minecraft.world.item.CreativeModeTab;

import java.util.function.Consumer;

/**
 * [stated] Sep 28 - the Weyland-Yutani faction tab, the same pattern as the USCM one: the W-Y LICENSE in the first
 * slot, then the W-Y building set, then its machines and crates.
 */
public class WyCreativeModeTabInitializer {

    public static final Consumer<CreativeModeTab.Output> OUTPUT_CONSUMER = output -> {
        CreativeModeTabUtil.accept(output, HumanFabrication.WY_FACTION_LICENSE);

        // [stated] Oct 20 - the W-Y building set, in its build order: walls, patterns, panels, screens, lights, vents,
        // structure, floors, ceiling, locker, window.
        HumanWyBlocks.ALL.forEach(block -> CreativeModeTabUtil.accept(output, block));

        // Machines
        CreativeModeTabUtil.accept(output, HumanFabrication.WY_FABRICATOR);
        CreativeModeTabUtil.accept(output, HumanFabrication.WY_DRIVE_BAY);
        CreativeModeTabUtil.accept(output, HumanBlocks.WY_CRYOTUBE_CONTROLLER);
        CreativeModeTabUtil.accept(output, HumanVaporizer.WY_VAPORIZER);
        CreativeModeTabUtil.accept(output, HumanDistributor.WY_DISTRIBUTOR);
        CreativeModeTabUtil.accept(output, HumanQuarry.WY_QUARRY_TERMINAL);
        CreativeModeTabUtil.accept(output, HumanBlocks.WY_FLUID_TANK);

        // Storage
        HumanCrates.ITEM_BY_TYPE.forEach((type, item) -> {
            if (type.family() == CrateFamily.WY) {
                CreativeModeTabUtil.accept(output, item);
            }
        });
    };
}
