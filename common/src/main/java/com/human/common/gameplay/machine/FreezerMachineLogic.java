package com.human.common.gameplay.machine;

import com.human.common.gameplay.block.entity.machine.ElectricMachineBlockEntity;
import com.human.common.gameplay.recipe.FreezerRecipe;
import com.human.common.registry.init.HumanRecipes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * The freezer: [stated] readme "freezes blocks, items, or liquid". An ITEM in the input slot is frozen first (ice to
 * packed ice); with no item recipe to run, the fluid in its TANK is frozen (a bucket of water to ice).
 * <p>
 * Fluid reaches the tank through the left face by pipe, or from a bucket or canister put in the input slot - the
 * machine pours those in itself and passes the empty container to the output (see {@link ElectricMachineBlockEntity}).
 * Either way, only a fluid it has a recipe for is let in.
 * </p>
 */
public final class FreezerMachineLogic implements MachineLogic {

    private final RecipeManager.CachedCheck<SingleRecipeInput, FreezerRecipe> quickCheck =
        RecipeManager.createCheck(HumanRecipes.FREEZING_TYPE.get());

    @Override
    public int work(ElectricMachineBlockEntity machine, Level level) {
        var item = itemRecipe(machine, level);

        if (item != null) {
            return item.value().getTime();
        }

        var fluid = fluidRecipe(machine, level);

        return fluid != null ? fluid.getTime() : 0;
    }

    @Override
    public void finish(ElectricMachineBlockEntity machine, Level level) {
        var item = itemRecipe(machine, level);

        if (item != null) {
            machine.getInput().shrink(item.value().getInputCount());
            machine.insertOutputs(List.of(item.value().getResult().copy()));
            return;
        }

        var fluid = fluidRecipe(machine, level);

        if (fluid != null) {
            machine.removeFluid(fluid.getFluid().amount());
            machine.insertOutputs(List.of(fluid.getResult().copy()));
        }
    }

    @Override
    public boolean accepts(ElectricMachineBlockEntity machine, Level level, ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        if (level.getRecipeManager().getRecipeFor(HumanRecipes.FREEZING_TYPE.get(), new SingleRecipeInput(stack), level).isPresent()) {
            return true;
        }

        // A bucket or canister of something it can freeze: the machine pours it into the tank.
        var held = MachineFluidItems.contained(stack);

        return !held.isEmpty() && acceptsFluid(machine, level, held.fluid());
    }

    @Override
    public boolean acceptsFluid(ElectricMachineBlockEntity machine, Level level, Fluid fluid) {
        for (var holder : level.getRecipeManager().getAllRecipesFor(HumanRecipes.FREEZING_TYPE.get())) {
            if (holder.value().usesFluid(fluid)) {
                return true;
            }
        }

        return false;
    }

    private @Nullable RecipeHolder<FreezerRecipe> itemRecipe(ElectricMachineBlockEntity machine, Level level) {
        var input = machine.getInput();

        if (input.isEmpty()) {
            return null;
        }

        var recipe = quickCheck.getRecipeFor(new SingleRecipeInput(input), level).orElse(null);

        return recipe != null && machine.canInsertOutputs(List.of(recipe.value().getResult().copy())) ? recipe : null;
    }

    /** The recipe for the fluid in the tank - when there is enough of it and room for the result. */
    private static @Nullable FreezerRecipe fluidRecipe(ElectricMachineBlockEntity machine, Level level) {
        var tank = machine.getFluid();

        if (tank.isEmpty()) {
            return null;
        }

        for (var holder : level.getRecipeManager().getAllRecipesFor(HumanRecipes.FREEZING_TYPE.get())) {
            var recipe = holder.value();

            if (
                recipe.usesFluid(tank.fluid())
                    && tank.amount() >= recipe.getFluid().amount()
                    && machine.canInsertOutputs(List.of(recipe.getResult().copy()))
            ) {
                return recipe;
            }
        }

        return null;
    }
}
