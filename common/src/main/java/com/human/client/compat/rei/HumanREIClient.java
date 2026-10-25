package com.human.client.compat.rei;

import com.human.client.screen.IndustrialFurnaceScreen;
import com.human.common.gameplay.recipe.IndustrialFurnaceRecipe;
import com.human.common.registry.init.HumanBlocks;
import com.human.common.registry.init.HumanRecipes;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.plugins.REIClientPlugin;
import me.shedaniel.rei.api.client.registry.category.CategoryRegistry;
import me.shedaniel.rei.api.client.registry.display.DisplayRegistry;
import me.shedaniel.rei.api.client.registry.screen.ScreenRegistry;
import me.shedaniel.rei.api.common.util.EntryStacks;

public class HumanREIClient implements REIClientPlugin {

    @Override
    public void registerCategories(CategoryRegistry registry) {
        registry.add(new IndustrialCategory());

        registry.addWorkstations(IndustrialCategory.INDUSTRIAL_FURNACE, EntryStacks.of(HumanBlocks.INDUSTRIAL_FURNACE.get()));

        // Oct 24 - where every blueprint is found (BlueprintSources), and the blueprint table's scan and copy.
        registry.add(new BlueprintSourceDisplays.SourceCategory());
        registry.add(
            new HumanMachineDisplays.MachineCategory(
                HumanMachineDisplays.BLUEPRINT_TABLE,
                com.human.common.registry.init.HumanBlocks.BLUEPRINT_BLOCK.get()
            )
        );
        registry.add(
            new HumanMachineDisplays.MachineCategory(HumanMachineDisplays.DRYING, net.minecraft.world.level.block.Blocks.HONEY_BLOCK)
        );
        registry.addWorkstations(HumanMachineDisplays.DRYING, EntryStacks.of(net.minecraft.world.level.block.Blocks.CAULDRON));
        registry.addWorkstations(
            HumanMachineDisplays.BLUEPRINT_TABLE,
            EntryStacks.of(com.human.common.registry.init.HumanBlocks.BLUEPRINT_BLOCK.get())
        );

        // Oct 24 - every machine with its own recipes gets a tab (see HumanMachineDisplays).
        registry.add(new HumanMachineDisplays.MachineCategory(HumanMachineDisplays.GRINDING, HumanMachineDisplays.grinder()));
        registry.add(new HumanMachineDisplays.MachineCategory(HumanMachineDisplays.PRESSING, HumanMachineDisplays.press()));
        registry.add(new HumanMachineDisplays.MachineCategory(HumanMachineDisplays.FREEZING, HumanMachineDisplays.freezer()));
        registry.add(new HumanMachineDisplays.MachineCategory(HumanMachineDisplays.HYDRATING, HumanMachineDisplays.hydrator()));
        registry.add(
            new HumanMachineDisplays.MachineCategory(HumanMachineDisplays.FABRICATING, HumanMachineDisplays.fabricators().getFirst())
        );
        registry.add(
            new HumanMachineDisplays.MachineCategory(HumanMachineDisplays.VAPORIZING, HumanMachineDisplays.vaporizers().getFirst())
        );

        registry.addWorkstations(HumanMachineDisplays.GRINDING, EntryStacks.of(HumanMachineDisplays.grinder()));
        registry.addWorkstations(HumanMachineDisplays.PRESSING, EntryStacks.of(HumanMachineDisplays.press()));
        registry.addWorkstations(HumanMachineDisplays.FREEZING, EntryStacks.of(HumanMachineDisplays.freezer()));
        registry.addWorkstations(HumanMachineDisplays.HYDRATING, EntryStacks.of(HumanMachineDisplays.hydrator()));
        for (var fabricator : HumanMachineDisplays.fabricators()) {
            registry.addWorkstations(HumanMachineDisplays.FABRICATING, EntryStacks.of(fabricator));
        }
        for (var vaporizer : HumanMachineDisplays.vaporizers()) {
            registry.addWorkstations(HumanMachineDisplays.VAPORIZING, EntryStacks.of(vaporizer));
        }

        // The electric machines that run vanilla (or our industrial) recipes, as workstations on those tabs.
        registry.addWorkstations(IndustrialCategory.INDUSTRIAL_FURNACE, EntryStacks.of(HumanBlocks.ELECTRIC_INDUSTRIAL_FURNACE.get()));
        registry.addWorkstations(
            me.shedaniel.rei.plugin.common.BuiltinPlugin.SMELTING,
            EntryStacks.of(HumanBlocks.ELECTRIC_FURNACE.get())
        );
        registry.addWorkstations(
            me.shedaniel.rei.plugin.common.BuiltinPlugin.SMOKING,
            EntryStacks.of(HumanBlocks.ELECTRIC_SMOKER.get())
        );
        registry.addWorkstations(
            me.shedaniel.rei.plugin.common.BuiltinPlugin.BLASTING,
            EntryStacks.of(HumanBlocks.ELECTRIC_BLAST_FURNACE.get())
        );
    }

    @Override
    public void registerDisplays(DisplayRegistry registry) {
        registry.registerRecipeFiller(
            IndustrialFurnaceRecipe.class,
            HumanRecipes.INDUSTRIAL_FURNACE_RECIPE_TYPE.get(),
            IndustrialDisplay::new
        );

        registry.registerRecipeFiller(
            com.human.common.gameplay.recipe.GrinderRecipe.class,
            HumanRecipes.GRINDING_TYPE.get(),
            HumanMachineDisplays::grinding
        );
        registry.registerRecipeFiller(
            com.human.common.gameplay.recipe.PressRecipe.class,
            HumanRecipes.PRESSING_TYPE.get(),
            HumanMachineDisplays::pressing
        );
        registry.registerRecipeFiller(
            com.human.common.gameplay.recipe.FreezerRecipe.class,
            HumanRecipes.FREEZING_TYPE.get(),
            HumanMachineDisplays::freezing
        );
        registry.registerRecipeFiller(
            com.human.common.gameplay.recipe.HydratorRecipe.class,
            HumanRecipes.HYDRATING_TYPE.get(),
            HumanMachineDisplays::hydrating
        );
        registry.registerRecipeFiller(
            com.human.common.gameplay.fabrication.FabricatingRecipe.class,
            com.human.common.registry.init.HumanFabrication.FABRICATING_TYPE.get(),
            HumanMachineDisplays::fabricating
        );
        for (var recipe : com.human.client.compat.VaporizingRecipe.all()) {
            registry.add(HumanMachineDisplays.vaporizing(recipe));
        }
        for (var display : BlueprintSourceDisplays.all()) {
            registry.add(display);
        }
        for (var recipe : com.human.client.compat.BlueprintTableRecipe.all()) {
            registry.add(HumanMachineDisplays.blueprintTable(recipe));
        }
        for (var recipe : com.human.client.compat.DryingRecipe.all()) {
            registry.add(HumanMachineDisplays.drying(recipe));
        }
    }

    /**
     * [stated] Oct 17: without avp_alien, everything made from xeno blood - the acid tanks, buckets and fluids - is
     * hidden, so nobody looks up an item they could never make. Matched by id, items and fluids alike.
     */
    @Override
    public void registerEntries(me.shedaniel.rei.api.client.registry.entry.EntryRegistry registry) {
        if (com.human.common.gameplay.acid.AcidAvailability.available()) {
            return;
        }

        var locked = com.human.common.gameplay.acid.AcidAvailability.lockedIds();

        registry.removeEntryIf(stack -> locked.contains(stack.getIdentifier()));
    }

    @Override
    public void registerScreens(ScreenRegistry registry) {
        registry.registerClickArea(
            screen -> new Rectangle(
                ((screen.width - 176) / 2) + 78,
                ((screen.height - 166) / 2) + 30,
                20,
                25
            ),
            IndustrialFurnaceScreen.class,
            IndustrialCategory.INDUSTRIAL_FURNACE
        );
    }
}
