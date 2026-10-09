package com.human.common.registry.init.creative_mode_tab;

import com.blib.api.common.registry.v1.BLibHolder;
import com.blib.api.common.registry.v1.BLibRegistry;
import com.human.Human;
import com.human.common.registry.init.HumanBlocks;
import com.human.common.registry.init.block.HumanPlasticBlocks;
import com.human.common.registry.init.creative_mode_tab.initializer.BlocksCreativeModeTabInitializer;
import com.human.common.registry.init.creative_mode_tab.initializer.ColoredBlocksCreativeModeTabInitializer;
import com.human.common.registry.init.creative_mode_tab.initializer.CombatCreativeModeTabInitializer;
import com.human.common.registry.init.creative_mode_tab.initializer.FoodAndDrinksCreativeModeTabInitializer;
import com.human.common.registry.init.creative_mode_tab.initializer.IngredientsCreativeModeTabInitializer;
import com.human.common.registry.init.creative_mode_tab.initializer.PowerCreativeModeTabInitializer;
import com.human.common.registry.init.creative_mode_tab.initializer.SpawnEggsCreativeModeTabInitializer;
import com.human.common.registry.init.creative_mode_tab.initializer.ToolsAndUtilitiesCreativeModeTabInitializer;
import com.human.common.registry.init.item.HumanGunItems;
import com.human.common.registry.init.item.HumanItems;
import com.human.common.registry.init.item.HumanSpawnEggItems;
import com.human.common.registry.key.HumanCreativeModeTabKeys;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class HumanCreativeModeTabs {

    private static final BLibRegistry<CreativeModeTab> REGISTRY = Human.MOD.registries().create(BuiltInRegistries.CREATIVE_MODE_TAB);

    private static final String BASE_PATH = "creativeModeTab";

    public static final BLibHolder<CreativeModeTab> BLOCKS = create(
        HumanCreativeModeTabKeys.BLOCKS_KEY,
        () -> new ItemStack(HumanBlocks.BLUEPRINT_BLOCK.get()),
        BlocksCreativeModeTabInitializer.OUTPUT_CONSUMER
    );

    public static final BLibHolder<CreativeModeTab> COLORED_BLOCKS = create(
        HumanCreativeModeTabKeys.COLORED_BLOCKS_KEY,
        () -> new ItemStack(HumanPlasticBlocks.DYE_COLOR_TO_PLASTIC.get(DyeColor.WHITE).get()),
        ColoredBlocksCreativeModeTabInitializer.OUTPUT_CONSUMER
    );

    public static final BLibHolder<CreativeModeTab> COMBAT = create(
        HumanCreativeModeTabKeys.COMBAT_KEY,
        () -> new ItemStack(HumanGunItems.M41A_PULSE_RIFLE.get()),
        CombatCreativeModeTabInitializer.OUTPUT_CONSUMER
    );

    public static final BLibHolder<CreativeModeTab> FOOD_AND_DRINKS = create(
        HumanCreativeModeTabKeys.FOOD_AND_DRINKS_KEY,
        () -> new ItemStack(HumanItems.CORNBREAD.get()),
        FoodAndDrinksCreativeModeTabInitializer.OUTPUT_CONSUMER
    );

    public static final BLibHolder<CreativeModeTab> INGREDIENTS = create(
        HumanCreativeModeTabKeys.INGREDIENTS_KEY,
        () -> new ItemStack(HumanItems.BLUEPRINT_M41A_PULSE_RIFLE.get()),
        IngredientsCreativeModeTabInitializer.OUTPUT_CONSUMER
    );

    public static final BLibHolder<CreativeModeTab> POWER = create(
        HumanCreativeModeTabKeys.POWER_KEY,
        () -> new ItemStack(HumanBlocks.BATTERY.get()),
        PowerCreativeModeTabInitializer.OUTPUT_CONSUMER
    );

    public static final BLibHolder<CreativeModeTab> SPAWN_EGGS = create(
        HumanCreativeModeTabKeys.SPAWN_EGGS_KEY,
        () -> new ItemStack(HumanSpawnEggItems.MARINE_SPAWN_EGG.get()),
        SpawnEggsCreativeModeTabInitializer.OUTPUT_CONSUMER
    );

    public static final BLibHolder<CreativeModeTab> TOOLS_AND_UTILITIES = create(
        HumanCreativeModeTabKeys.TOOLS_AND_UTILITIES_KEY,
        () -> new ItemStack(HumanItems.CANISTER.get()),
        ToolsAndUtilitiesCreativeModeTabInitializer.OUTPUT_CONSUMER
    );

    public static BLibHolder<CreativeModeTab> create(
        ResourceKey<CreativeModeTab> resourceKey,
        Supplier<ItemStack> iconSupplier,
        Consumer<CreativeModeTab.Output> outputConsumer
    ) {
        var path = resourceKey.location().getPath();

        return REGISTRY.createHolder(
            path,
            () -> CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
                .icon(iconSupplier)
                .title(Component.translatable(BASE_PATH + "." + Human.MOD_ID + "." + path))
                .displayItems((itemDisplayParameters, output) -> outputConsumer.accept(output))
                .build()
        );
    }

    public static void initialize() {
        REGISTRY.registerAll();
    }
}
