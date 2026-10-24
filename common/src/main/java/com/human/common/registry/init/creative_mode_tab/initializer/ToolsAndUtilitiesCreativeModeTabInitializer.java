package com.human.common.registry.init.creative_mode_tab.initializer;

import com.human.common.gameplay.item.canister.UniversalCanisterItem;
import com.human.common.registry.init.item.HumanItems;
import net.minecraft.world.item.CreativeModeTab;

import java.util.function.Consumer;

public class ToolsAndUtilitiesCreativeModeTabInitializer {

    public static final Consumer<CreativeModeTab.Output> OUTPUT_CONSUMER = output -> {
        CreativeModeTabUtil.accept(output, HumanItems.DOG_TAG);
        CreativeModeTabUtil.accept(output, HumanItems.WY_MUSIC_DISC_1);
        CreativeModeTabUtil.accept(output, HumanItems.WY_MUSIC_DISC_1_FRAGMENT);
        CreativeModeTabUtil.accept(output, HumanItems.ARMOR_CASE);
        CreativeModeTabUtil.accept(output, HumanItems.GENE_READER);
        CreativeModeTabUtil.accept(output, HumanItems.SYRINGE);
        CreativeModeTabUtil.accept(output, HumanItems.CANISTER);

        // One full canister for EVERY fluid that can go in one - vanilla's, ours and any other mod's - found in the
        // fluid registry when the tab is built, so a new mod's fluids appear here with no code change. The old water,
        // lava and latex canisters are legacy and deliberately not listed.
        for (var fluid : UniversalCanisterItem.canisterFluids()) {
            CreativeModeTabUtil.accept(output, UniversalCanisterItem.filled(fluid, UniversalCanisterItem.CAPACITY));
        }

        CreativeModeTabUtil.accept(output, HumanItems.HONEY_BUCKET);
        CreativeModeTabUtil.accept(output, HumanItems.CRYOFLUID_BUCKET);

        // [stated] Oct 17: the xeno acids come from avp_alien's chitin - without avp_alien they are hidden.
        if (com.human.common.gameplay.acid.AcidAvailability.available()) {
            CreativeModeTabUtil.accept(output, com.human.common.registry.init.HumanChemicals.XENO_ACID_BUCKET);
            CreativeModeTabUtil.accept(output, com.human.common.registry.init.HumanChemicals.NETHER_XENO_ACID_BUCKET);
            CreativeModeTabUtil.accept(output, com.human.common.registry.init.HumanChemicals.IRRADIATED_XENO_ACID_BUCKET);
        }
        CreativeModeTabUtil.accept(output, HumanItems.TITANIUM_CAPTURE_CORD);
        CreativeModeTabUtil.accept(output, HumanItems.MILK_CANISTER);
        CreativeModeTabUtil.accept(output, HumanItems.POWDER_SNOW_CANISTER);
        CreativeModeTabUtil.accept(output, HumanItems.STEEL_AXE);
        CreativeModeTabUtil.accept(output, HumanItems.STEEL_HOE);
        CreativeModeTabUtil.accept(output, HumanItems.STEEL_PICKAXE);
        CreativeModeTabUtil.accept(output, HumanItems.STEEL_SHOVEL);
        CreativeModeTabUtil.accept(output, HumanItems.STEEL_SWORD);
        CreativeModeTabUtil.accept(output, HumanItems.TACTICAL_KNIFE);
        CreativeModeTabUtil.accept(output, HumanItems.TITANIUM_AXE);
        CreativeModeTabUtil.accept(output, HumanItems.TITANIUM_HOE);
        CreativeModeTabUtil.accept(output, HumanItems.TITANIUM_PICKAXE);
        CreativeModeTabUtil.accept(output, HumanItems.TITANIUM_SHOVEL);
        CreativeModeTabUtil.accept(output, HumanItems.TITANIUM_SWORD);
    };
}
