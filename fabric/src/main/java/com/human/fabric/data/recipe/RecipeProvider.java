package com.human.fabric.data.recipe;

import com.blib.fabric.data.recipe.builder.RecipeBuilder;
import com.human.Human;
import com.human.HumanResources;
import com.human.common.gameplay.recipe.GunUnloadRecipe;
import com.human.fabric.data.recipe.builder.IndustrialFurnaceRecipeBuilder;
import com.human.fabric.data.recipe.impl.ArmorRecipeProvider;
import com.human.fabric.data.recipe.impl.ElectronicItemRecipeProvider;
import com.human.fabric.data.recipe.impl.GlassRecipeProvider;
import com.human.fabric.data.recipe.impl.GunRecipeProvider;
import com.human.fabric.data.recipe.impl.IndustrialConcreteRecipeProvider;
import com.human.fabric.data.recipe.impl.MetalRecipeProvider;
import com.human.fabric.data.recipe.impl.MiscellaneousRecipeProvider;
import com.human.fabric.data.recipe.impl.PaddingRecipeProvider;
import com.human.fabric.data.recipe.impl.PlasticRecipeProvider;
import com.human.fabric.data.recipe.impl.ToolRecipeProvider;
import com.human.fabric.data.recipe.impl.vanilla.VanillaChestRecipeProvider;
import com.human.fabric.data.recipe.impl.vanilla.VanillaConcreteRecipeProvider;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.SpecialRecipeBuilder;

import java.util.concurrent.CompletableFuture;

public class RecipeProvider extends FabricRecipeProvider {

    public RecipeProvider(FabricDataOutput output, CompletableFuture<HolderLookup.Provider> registriesFuture) {
        super(output, registriesFuture);
    }

    @Override
    public void buildRecipes(RecipeOutput recipeOutput) {
        var builder = RecipeBuilder.with(Human.MOD, recipeOutput, this::withConditions);
        IndustrialFurnaceRecipeBuilder.ensureRegistration(recipeOutput);

        // Emptying a gun keeps its components, which a JSON recipe cannot express - hence a special recipe.
        SpecialRecipeBuilder.special(GunUnloadRecipe::new)
            .save(recipeOutput, HumanResources.location("gun_unload").toString());

        ArmorRecipeProvider.provide(builder);
        ElectronicItemRecipeProvider.provide(builder);
        GlassRecipeProvider.provide(builder);
        GunRecipeProvider.provide(builder);
        IndustrialConcreteRecipeProvider.provide(builder);
        MetalRecipeProvider.provide(builder);
        MiscellaneousRecipeProvider.provide(builder);
        PaddingRecipeProvider.provide(builder);
        PlasticRecipeProvider.provide(builder);
        ToolRecipeProvider.provide(builder);

        VanillaChestRecipeProvider.provide(builder);
        VanillaConcreteRecipeProvider.provide(builder);
    }

}
