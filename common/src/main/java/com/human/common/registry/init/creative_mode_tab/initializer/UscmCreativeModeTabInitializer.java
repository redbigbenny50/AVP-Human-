package com.human.common.registry.init.creative_mode_tab.initializer;

import com.human.common.gameplay.storage.CrateFamily;
import com.human.common.registry.init.HumanBlocks;
import com.human.common.registry.init.HumanCrates;
import com.human.common.registry.init.HumanDistributor;
import com.human.common.registry.init.HumanFabrication;
import com.human.common.registry.init.HumanQuarry;
import com.human.common.registry.init.HumanVaporizer;
import com.human.common.registry.init.block.HumanUscmBlocks;
import net.minecraft.world.item.CreativeModeTab;

import java.util.function.Consumer;

/**
 * [stated] Sep 28 - the USCM faction tab: everything USCM in one place, the USCM LICENSE in the first slot.
 * <p>
 * ⚠ These entries are MOVED here, not copied (the machines came out of the Power tab, the license out of Ingredients,
 * the crates out of Storage) - a stack in two tabs looks like a bug and lists twice in search.
 */
public class UscmCreativeModeTabInitializer {

    public static final Consumer<CreativeModeTab.Output> OUTPUT_CONSUMER = output -> {
        // The license unlocks everything below it in a fabricator.
        CreativeModeTabUtil.accept(output, HumanFabrication.USCM_FACTION_LICENSE);

        // The base building set, in its build order: walls, lights, floors, ceiling, structure, lockers, window.
        HumanUscmBlocks.ALL.forEach(block -> CreativeModeTabUtil.accept(output, block));

        // Machines
        CreativeModeTabUtil.accept(output, HumanFabrication.USCM_FABRICATOR);
        CreativeModeTabUtil.accept(output, HumanFabrication.USCM_DRIVE_BAY);
        CreativeModeTabUtil.accept(output, HumanBlocks.USCM_CRYOTUBE_CONTROLLER);
        CreativeModeTabUtil.accept(output, HumanVaporizer.USCM_VAPORIZER);
        CreativeModeTabUtil.accept(output, HumanDistributor.USCM_DISTRIBUTOR);
        CreativeModeTabUtil.accept(output, HumanQuarry.USCM_QUARRY_TERMINAL);
        CreativeModeTabUtil.accept(output, HumanBlocks.USCM_FLUID_TANK);

        // Storage
        HumanCrates.ITEM_BY_TYPE.forEach((type, item) -> {
            if (type.family() == CrateFamily.USCM) {
                CreativeModeTabUtil.accept(output, item);
            }
        });
    };
}
