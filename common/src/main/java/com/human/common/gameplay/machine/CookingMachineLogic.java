package com.human.common.gameplay.machine;

import com.human.common.gameplay.block.entity.machine.ElectricMachineBlockEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Supplier;

/**
 * The furnace-type machines: the same recipes as their fuel-burning versions, one input, one output, experience kept
 * for whoever takes the output. This is the cooking code that lived in the machine block entity, moved here unchanged.
 */
public final class CookingMachineLogic implements MachineLogic {

    private final Supplier<? extends RecipeType<? extends AbstractCookingRecipe>> recipeType;

    private final RecipeManager.CachedCheck<SingleRecipeInput, AbstractCookingRecipe> quickCheck;

    @SuppressWarnings("unchecked")
    public CookingMachineLogic(Supplier<? extends RecipeType<? extends AbstractCookingRecipe>> recipeType) {
        this.recipeType = recipeType;
        this.quickCheck = RecipeManager.createCheck((RecipeType<AbstractCookingRecipe>) (RecipeType<?>) recipeType.get());
    }

    @Override
    public int work(ElectricMachineBlockEntity machine, Level level) {
        var recipe = recipe(machine, level);

        if (recipe == null) {
            return 0;
        }

        var result = recipe.value().assemble(new SingleRecipeInput(machine.getInput()), level.registryAccess());

        return !result.isEmpty() && machine.canInsertOutputs(List.of(result)) ? recipe.value().getCookingTime() : 0;
    }

    @Override
    public void finish(ElectricMachineBlockEntity machine, Level level) {
        var recipe = recipe(machine, level);

        if (recipe == null) {
            return;
        }

        var input = machine.getInput();
        var result = recipe.value().assemble(new SingleRecipeInput(input), level.registryAccess());

        machine.insertOutputs(List.of(result));
        input.shrink(1);
        machine.addExperience(recipe.value().getExperience());
    }

    @Override
    public boolean accepts(ElectricMachineBlockEntity machine, Level level, ItemStack stack) {
        return !stack.isEmpty() && level.getRecipeManager().getRecipeFor(recipeType.get(), new SingleRecipeInput(stack), level).isPresent();
    }

    private @Nullable RecipeHolder<AbstractCookingRecipe> recipe(ElectricMachineBlockEntity machine, Level level) {
        var input = machine.getInput();

        return input.isEmpty() ? null : quickCheck.getRecipeFor(new SingleRecipeInput(input), level).orElse(null);
    }
}
