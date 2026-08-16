package com.human.common.registry.init;

import com.blib.api.common.registry.v1.BLibHolder;
import com.blib.api.common.registry.v1.BLibRegistry;
import com.human.Human;
import com.human.common.gameplay.recipe.GunUnloadRecipe;
import com.human.common.gameplay.recipe.IndustrialFurnaceRecipe;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SimpleCookingSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import org.jetbrains.annotations.NotNull;

public class HumanRecipes {

    private static final BLibRegistry<RecipeSerializer<?>> SERIALIZER_REGISTRY = Human.MOD.registries()
        .create(
            BuiltInRegistries.RECIPE_SERIALIZER
        );

    private static final BLibRegistry<RecipeType<?>> TYPE_REGISTRY = Human.MOD.registries().create(BuiltInRegistries.RECIPE_TYPE);

    public static final BLibHolder<? extends RecipeSerializer<IndustrialFurnaceRecipe>> INDUSTRIAL_FURNACE_RECIPE_SERIALIZER =
        create(
            "industrial_furnace",
            IndustrialFurnaceRecipe::new,
            100
        );

    /** Empties any gun placed alone in a crafting grid, keeping every other component. See {@link GunUnloadRecipe}. */
    public static final BLibHolder<SimpleCraftingRecipeSerializer<GunUnloadRecipe>> GUN_UNLOAD_RECIPE_SERIALIZER =
        SERIALIZER_REGISTRY.createHolder(
            "gun_unload",
            () -> new SimpleCraftingRecipeSerializer<>(GunUnloadRecipe::new)
        );

    private static @NotNull <T extends AbstractCookingRecipe> BLibHolder<SimpleCookingSerializer<T>> create(
        String path,
        AbstractCookingRecipe.Factory<T> factory,
        int cookingTime
    ) {
        return SERIALIZER_REGISTRY.createHolder(
            path,
            () -> new SimpleCookingSerializer<>(factory, cookingTime)
        );
    }

    public static final BLibHolder<RecipeType<IndustrialFurnaceRecipe>> INDUSTRIAL_FURNACE_RECIPE_TYPE = create(
        "industrial_furnace"
    );

    private static @NotNull <T extends Recipe<?>> BLibHolder<RecipeType<T>> create(String path) {
        return TYPE_REGISTRY.createHolder(path, () -> new RecipeType<>() {

            @Override
            public String toString() {
                return path;
            }
        });
    }

    private HumanRecipes() {}

    public static void initialize() {
        SERIALIZER_REGISTRY.registerAll();
        TYPE_REGISTRY.registerAll();
    }
}
