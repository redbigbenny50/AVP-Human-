package com.human.client.compat.jei;

import com.human.Human;
import com.human.HumanResources;
import com.human.common.gameplay.recipe.IndustrialFurnaceRecipe;
import com.human.common.registry.init.HumanBlocks;
import com.human.common.registry.init.HumanRecipes;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IModInfoRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import org.jetbrains.annotations.NotNull;

@JeiPlugin
public class HumanJEIPlugin implements IModPlugin {

    public static final RecipeType<IndustrialFurnaceRecipe> INDUSTRIAL_FURNACE_TYPE = RecipeType.create(
        Human.MOD_ID,
        "smeltery",
        IndustrialFurnaceRecipe.class
    );

    @Override
    public @NotNull ResourceLocation getPluginUid() {
        return HumanResources.location("plugin_" + Human.MOD_ID);
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        var gui = registration.getJeiHelpers().getGuiHelper();
        var bucket = registration.getJeiHelpers().getPlatformFluidHelper().bucketVolume();

        registration.addRecipeCategories(new IndustrialCategory(gui));

        // Oct 24 - every machine with its own recipes gets a tab (see HumanMachineCategories).
        registration.addRecipeCategories(
            new HumanMachineCategories.Grinding(gui, bucket),
            new HumanMachineCategories.Pressing(gui, bucket),
            new HumanMachineCategories.Freezing(gui, bucket),
            new HumanMachineCategories.Hydrating(gui, bucket),
            new HumanMachineCategories.Fabricating(gui, bucket),
            new HumanMachineCategories.Vaporizing(gui, bucket)
        );

        // Oct 24 - where every blueprint is found (BlueprintSources).
        registration.addRecipeCategories(new BlueprintSourceCategory(gui));
        registration.addRecipeCategories(new BlueprintTableCategory(gui));
        registration.addRecipeCategories(new DryingCategory(gui, bucket));
    }

    @Override
    public void registerRecipes(@NotNull IRecipeRegistration registration) {
        if (Minecraft.getInstance().level == null) {
            return;
        }
        RecipeManager recipeManager = Minecraft.getInstance().level.getRecipeManager();
        registration.addRecipes(
            INDUSTRIAL_FURNACE_TYPE,
            recipeManager.getAllRecipesFor(HumanRecipes.INDUSTRIAL_FURNACE_RECIPE_TYPE.get())
                .stream()
                .map(RecipeHolder::value)
                .toList()
        );

        registration.addRecipes(HumanMachineCategories.GRINDING, values(recipeManager, HumanRecipes.GRINDING_TYPE.get()));
        registration.addRecipes(HumanMachineCategories.PRESSING, values(recipeManager, HumanRecipes.PRESSING_TYPE.get()));
        registration.addRecipes(HumanMachineCategories.FREEZING, values(recipeManager, HumanRecipes.FREEZING_TYPE.get()));
        registration.addRecipes(HumanMachineCategories.HYDRATING, values(recipeManager, HumanRecipes.HYDRATING_TYPE.get()));
        registration.addRecipes(
            HumanMachineCategories.FABRICATING,
            values(recipeManager, com.human.common.registry.init.HumanFabrication.FABRICATING_TYPE.get())
        );
        registration.addRecipes(HumanMachineCategories.VAPORIZING, com.human.client.compat.VaporizingRecipe.all());
        registration.addRecipes(BlueprintSourceCategory.TYPE, com.human.client.compat.BlueprintSources.all());
        registration.addRecipes(BlueprintTableCategory.TYPE, com.human.client.compat.BlueprintTableRecipe.all());
        registration.addRecipes(DryingCategory.TYPE, com.human.client.compat.DryingRecipe.all());
    }

    private static <I extends net.minecraft.world.item.crafting.RecipeInput, R extends net.minecraft.world.item.crafting.Recipe<I>> java.util.List<R> values(
        RecipeManager recipeManager,
        net.minecraft.world.item.crafting.RecipeType<R> type
    ) {
        return recipeManager.getAllRecipesFor(type).stream().map(RecipeHolder::value).toList();
    }

    @Override
    public void registerRecipeCatalysts(@NotNull IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(HumanBlocks.INDUSTRIAL_FURNACE.get()), INDUSTRIAL_FURNACE_TYPE);

        // Oct 24 - the electric machines that run vanilla (or our industrial) recipes, shown as "also made in" on those
        // tabs, and every edition of each machine on its own tab.
        registration.addRecipeCatalyst(new ItemStack(HumanBlocks.ELECTRIC_INDUSTRIAL_FURNACE.get()), INDUSTRIAL_FURNACE_TYPE);
        registration.addRecipeCatalyst(new ItemStack(HumanBlocks.ELECTRIC_FURNACE.get()), mezz.jei.api.constants.RecipeTypes.SMELTING);
        registration.addRecipeCatalyst(new ItemStack(HumanBlocks.ELECTRIC_SMOKER.get()), mezz.jei.api.constants.RecipeTypes.SMOKING);
        registration.addRecipeCatalyst(
            new ItemStack(HumanBlocks.ELECTRIC_BLAST_FURNACE.get()),
            mezz.jei.api.constants.RecipeTypes.BLASTING
        );

        registration.addRecipeCatalyst(new ItemStack(HumanBlocks.ELECTRIC_GRINDER.get()), HumanMachineCategories.GRINDING);
        registration.addRecipeCatalyst(new ItemStack(HumanBlocks.ELECTRIC_PRESS.get()), HumanMachineCategories.PRESSING);
        registration.addRecipeCatalyst(new ItemStack(HumanBlocks.ELECTRIC_FREEZER.get()), HumanMachineCategories.FREEZING);
        registration.addRecipeCatalyst(new ItemStack(HumanBlocks.ELECTRIC_HYDRATOR.get()), HumanMachineCategories.HYDRATING);

        for (
            var fabricator : java.util.List.of(
                com.human.common.registry.init.HumanFabrication.FABRICATOR.get(),
                com.human.common.registry.init.HumanFabrication.WY_FABRICATOR.get(),
                com.human.common.registry.init.HumanFabrication.USCM_FABRICATOR.get()
            )
        ) {
            registration.addRecipeCatalyst(new ItemStack(fabricator), HumanMachineCategories.FABRICATING);
        }

        registration.addRecipeCatalyst(com.human.client.compat.BlueprintTableRecipe.table(), BlueprintTableCategory.TYPE);
        registration.addRecipeCatalyst(new ItemStack(net.minecraft.world.item.Items.CAULDRON), DryingCategory.TYPE);

        for (
            var vaporizer : java.util.List.of(
                com.human.common.registry.init.HumanVaporizer.VAPORIZER.get(),
                com.human.common.registry.init.HumanVaporizer.WY_VAPORIZER.get(),
                com.human.common.registry.init.HumanVaporizer.USCM_VAPORIZER.get()
            )
        ) {
            registration.addRecipeCatalyst(new ItemStack(vaporizer), HumanMachineCategories.VAPORIZING);
        }
    }

    @Override
    public void registerModInfo(IModInfoRegistration modAliasRegistration) {
        modAliasRegistration.addModAliases(Human.MOD_ID, "avp", "avp_human");
    }

    /**
     * [stated] Oct 17: without avp_alien, everything made from xeno blood - the acid tanks, the acid buckets and the
     * acid fluids - is hidden, so nobody looks up an item they could never make. Matched by id, so items and fluids go
     * through the same check.
     */
    @Override
    public void onRuntimeAvailable(mezz.jei.api.runtime.IJeiRuntime runtime) {
        if (com.human.common.gameplay.acid.AcidAvailability.available()) {
            return;
        }

        var locked = com.human.common.gameplay.acid.AcidAvailability.lockedIds();
        var manager = runtime.getIngredientManager();

        hide(manager, mezz.jei.api.constants.VanillaTypes.ITEM_STACK, locked);
        hide(manager, runtime.getJeiHelpers().getPlatformFluidHelper().getFluidIngredientType(), locked);
    }

    private static <V> void hide(
        mezz.jei.api.runtime.IIngredientManager manager,
        mezz.jei.api.ingredients.IIngredientType<V> type,
        java.util.Set<ResourceLocation> locked
    ) {
        var helper = manager.getIngredientHelper(type);
        var hidden = manager.getAllIngredients(type)
            .stream()
            .filter(ingredient -> locked.contains(helper.getResourceLocation(ingredient)))
            .toList();

        if (!hidden.isEmpty()) {
            manager.removeIngredientsAtRuntime(type, hidden);
        }
    }
}
