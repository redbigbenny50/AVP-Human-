package com.human.common.gameplay.machine;

import com.human.common.gameplay.block.entity.machine.ElectricMachineBlockEntity;
import com.human.common.gameplay.recipe.PressRecipe;
import com.human.common.registry.init.HumanRecipes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * The press: one input, an item into the output slot and/or fluid into its tank ({@link PressRecipe}).
 * <h2>THE KEEP / DISCARD SWITCHES</h2> [stated] readme: "you can disable liquid extraction to destroy liquids and just
 * get blocks; you can disable block extraction to destroy blocks and just get liquids". A switched-off output is thrown
 * away every time, so it never needs room - a press making latex with its item output switched off never stops for a
 * full slot.
 */
public final class PressMachineLogic implements MachineLogic {

    private final RecipeManager.CachedCheck<SingleRecipeInput, PressRecipe> quickCheck =
        RecipeManager.createCheck(HumanRecipes.PRESSING_TYPE.get());

    @Override
    public int work(ElectricMachineBlockEntity machine, Level level) {
        var recipe = recipe(machine, level);

        return recipe != null && hasRoom(machine, recipe.value()) ? recipe.value().getTime() : 0;
    }

    @Override
    public void finish(ElectricMachineBlockEntity machine, Level level) {
        var recipe = recipe(machine, level);

        // ⚠ Room re-checked: a pipe may have filled the tank's last space since work() said yes.
        if (recipe == null || !hasRoom(machine, recipe.value())) {
            return;
        }

        var press = recipe.value();

        machine.getInput().shrink(press.getInputCount());

        if (machine.keepsItemOutput() && !press.getResult().isEmpty()) {
            machine.insertOutputs(List.of(press.getResult().copy()));
        }

        if (machine.keepsFluidOutput() && !press.getFluid().isEmpty()) {
            machine.addFluid(press.getFluid().fluid(), press.getFluid().amount());
        }
    }

    @Override
    public boolean accepts(ElectricMachineBlockEntity machine, Level level, ItemStack stack) {
        return !stack.isEmpty()
            && level.getRecipeManager().getRecipeFor(HumanRecipes.PRESSING_TYPE.get(), new SingleRecipeInput(stack), level).isPresent();
    }

    /** Room for every output that is being KEPT. */
    private static boolean hasRoom(ElectricMachineBlockEntity machine, PressRecipe press) {
        var item = press.getResult();
        var fluid = press.getFluid();

        var itemFits = item.isEmpty() || !machine.keepsItemOutput() || machine.canInsertOutputs(List.of(item.copy()));
        var fluidFits = fluid.isEmpty() || !machine.keepsFluidOutput() || machine.fluidRoom(fluid.fluid()) >= fluid.amount();

        return itemFits && fluidFits;
    }

    private @Nullable RecipeHolder<PressRecipe> recipe(ElectricMachineBlockEntity machine, Level level) {
        var input = machine.getInput();

        return input.isEmpty() ? null : quickCheck.getRecipeFor(new SingleRecipeInput(input), level).orElse(null);
    }
}
