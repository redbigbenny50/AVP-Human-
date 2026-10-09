package com.human.common.gameplay.machine;

import com.human.common.gameplay.block.entity.machine.ElectricMachineBlockEntity;
import com.human.common.gameplay.recipe.GrinderRecipe;
import com.human.common.registry.init.HumanRecipes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * The grinder: one input, up to nine outputs ({@link GrinderRecipe}). [stated] Sep 29 - ores to two dusts, coal to
 * carbon, the sediment chain.
 * <p>
 * [stated] Sep 30: it is also a DECRAFTING table - anything with no grinding recipe of its own goes back to its
 * crafting ingredients, scaled by durability ({@link GrinderDecrafting}). Grinding recipes always win.
 * </p>
 * <p>
 * ⚠ A job only starts when EVERY output it could make fits, chance outputs included - otherwise a lucky flint roll with
 * the slots nearly full would have nowhere to go and be lost.
 * </p>
 */
public final class GrinderMachineLogic implements MachineLogic {

    private final RecipeManager.CachedCheck<SingleRecipeInput, GrinderRecipe> quickCheck =
        RecipeManager.createCheck(HumanRecipes.GRINDING_TYPE.get());

    @Override
    public int work(ElectricMachineBlockEntity machine, Level level) {
        var recipe = recipe(machine, level);

        if (recipe != null) {
            return machine.canInsertOutputs(recipe.value().allPossibleResults()) ? recipe.value().getTime() : 0;
        }

        // Decrafting: only whole batches, and only when the full-durability yield would fit.
        var input = machine.getInput();
        var plan = GrinderDecrafting.plan(level, input);

        return plan != null && input.getCount() >= plan.inputCount() && machine.canInsertOutputs(plan.outputs())
            ? GrinderRecipe.DEFAULT_TIME
            : 0;
    }

    @Override
    public void finish(ElectricMachineBlockEntity machine, Level level) {
        var recipe = recipe(machine, level);

        if (recipe != null) {
            machine.getInput().shrink(recipe.value().getInputCount());
            machine.insertOutputs(recipe.value().roll(level.random));
            return;
        }

        var input = machine.getInput();
        var plan = GrinderDecrafting.plan(level, input);

        if (plan == null || input.getCount() < plan.inputCount()) {
            return;
        }

        // Read the durability BEFORE the item is used up.
        var outputs = GrinderDecrafting.roll(plan, input.copy(), level.random);

        input.shrink(plan.inputCount());
        machine.insertOutputs(outputs);
    }

    @Override
    public boolean accepts(ElectricMachineBlockEntity machine, Level level, ItemStack stack) {
        return !stack.isEmpty()
            && (level.getRecipeManager().getRecipeFor(HumanRecipes.GRINDING_TYPE.get(), new SingleRecipeInput(stack), level).isPresent()
                || GrinderDecrafting.plan(level, stack) != null);
    }

    private @Nullable RecipeHolder<GrinderRecipe> recipe(ElectricMachineBlockEntity machine, Level level) {
        var input = machine.getInput();

        return input.isEmpty() ? null : quickCheck.getRecipeFor(new SingleRecipeInput(input), level).orElse(null);
    }
}
