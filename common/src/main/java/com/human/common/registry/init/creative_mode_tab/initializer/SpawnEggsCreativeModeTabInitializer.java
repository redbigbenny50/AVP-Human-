package com.human.common.registry.init.creative_mode_tab.initializer;

import com.human.common.registry.init.item.HumanSpawnEggItems;
import net.minecraft.world.item.CreativeModeTab;

import java.util.function.Consumer;

public class SpawnEggsCreativeModeTabInitializer {

    public static final Consumer<CreativeModeTab.Output> OUTPUT_CONSUMER = output -> {
        CreativeModeTabUtil.accept(output, HumanSpawnEggItems.MARINE_DOG_SPAWN_EGG);
        CreativeModeTabUtil.accept(output, HumanSpawnEggItems.COLONIST_SPAWN_EGG);
        CreativeModeTabUtil.accept(output, HumanSpawnEggItems.MARINE_SPAWN_EGG);
        CreativeModeTabUtil.accept(output, HumanSpawnEggItems.TACTICAL_MARINE_SPAWN_EGG);
        CreativeModeTabUtil.accept(output, HumanSpawnEggItems.WY_COMMANDO_SPAWN_EGG);
        CreativeModeTabUtil.accept(output, HumanSpawnEggItems.WY_ELITE_SPAWN_EGG);
        CreativeModeTabUtil.accept(output, HumanSpawnEggItems.WY_SPEC_OPS_COMMANDO_SPAWN_EGG);
        CreativeModeTabUtil.accept(output, HumanSpawnEggItems.WY_SPEC_OPS_ELITE_SPAWN_EGG);
        CreativeModeTabUtil.accept(output, HumanSpawnEggItems.WY_APE_TROOPER_SPAWN_EGG);
    };
}
