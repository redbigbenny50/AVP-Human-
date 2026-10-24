package com.human.common.registry.key;

import com.human.HumanResources;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;

public class HumanCreativeModeTabKeys {

    public static final ResourceKey<CreativeModeTab> BLOCKS_KEY = create("human_blocks");

    public static final ResourceKey<CreativeModeTab> COLORED_BLOCKS_KEY = create("human_colored_blocks");

    public static final ResourceKey<CreativeModeTab> COMBAT_KEY = create("human_combat");

    public static final ResourceKey<CreativeModeTab> FOOD_AND_DRINKS_KEY = create("human_food_and_drinks");

    public static final ResourceKey<CreativeModeTab> INGREDIENTS_KEY = create("human_ingredients");

    public static final ResourceKey<CreativeModeTab> POWER_KEY = create("human_power");

    public static final ResourceKey<CreativeModeTab> SPAWN_EGGS_KEY = create("human_spawn_eggs");

    public static final ResourceKey<CreativeModeTab> TOOLS_AND_UTILITIES_KEY = create("human_tools_and_utilities");

    /** [stated] Sep 28 - each faction has its own tab for its blocks and machines, its license in the first slot. */
    public static final ResourceKey<CreativeModeTab> USCM_KEY = create("human_uscm");

    public static final ResourceKey<CreativeModeTab> WY_KEY = create("human_wy");

    public static ResourceKey<CreativeModeTab> create(String name) {
        return ResourceKey.create(
            BuiltInRegistries.CREATIVE_MODE_TAB.key(),
            HumanResources.location(name)
        );
    }
}
