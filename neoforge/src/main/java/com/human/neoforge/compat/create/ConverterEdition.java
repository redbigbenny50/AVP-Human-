package com.human.neoforge.compat.create;

import com.human.common.registry.key.HumanCreativeModeTabKeys;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;

/**
 * The three editions of each Create converter, named like the other edition machines (steel_, wy_, uscm_ - as the
 * cryotube controller). Steel is crafted; W-Y and USCM are made from it in the fabricator.
 */
public enum ConverterEdition {

    STEEL("steel", HumanCreativeModeTabKeys.POWER_KEY),
    WY("wy", HumanCreativeModeTabKeys.WY_KEY),
    USCM("uscm", HumanCreativeModeTabKeys.USCM_KEY);

    private final String prefix;

    private final ResourceKey<CreativeModeTab> tab;

    ConverterEdition(String prefix, ResourceKey<CreativeModeTab> tab) {
        this.prefix = prefix;
        this.tab = tab;
    }

    public String id(String machine) {
        return prefix + "_" + machine;
    }

    public ResourceKey<CreativeModeTab> tab() {
        return tab;
    }
}
