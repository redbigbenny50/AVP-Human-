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
    }

    @Override
    public void registerDisplays(DisplayRegistry registry) {
        registry.registerRecipeFiller(
            IndustrialFurnaceRecipe.class,
            HumanRecipes.INDUSTRIAL_FURNACE_RECIPE_TYPE.get(),
            IndustrialDisplay::new
        );
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
