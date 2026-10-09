package com.human.common.gameplay.machine;

import com.human.common.gameplay.block.entity.machine.ElectricMachineBlockEntity;
import com.human.common.gameplay.recipe.HydratorRecipe;
import com.human.common.registry.init.HumanRecipes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * The hydrator: [stated] readme "generates water when powered" and "floods blocks with liquid".
 * <h2>TWO JOBS, ONE AT A TIME</h2>
 * <ol>
 * <li>FLOOD an input ({@link HydratorRecipe}) when there is one, enough water for it, and room for the result.</li>
 * <li>Otherwise MAKE WATER - {@value #WATER_PER_JOB} mB every {@value #WATER_TICKS} ticks - until the tank is
 * full.</li>
 * </ol>
 * Each is an ordinary job, so the power rule holds: it draws only while doing one, and a full tank with nothing to
 * flood is idle and draws nothing. The water is also free to leave by pipe (the bottom) or the fill slot, which makes a
 * hydrator a powered water source.
 * <p>
 * ⚠ finish() decides again rather than remembering which job started: an item dropped in halfway through a water job is
 * simply flooded when that job ends, if the water is there - never both, never neither.
 * </p>
 */
public final class HydratorMachineLogic implements MachineLogic {

    /** A quarter bucket per job. */
    static final int WATER_PER_JOB = 250;

    /** One second per quarter bucket: 12.5 mB a tick, a bucket every four seconds at 40 FE/t. */
    static final int WATER_TICKS = 20;

    private final RecipeManager.CachedCheck<SingleRecipeInput, HydratorRecipe> quickCheck =
        RecipeManager.createCheck(HumanRecipes.HYDRATING_TYPE.get());

    @Override
    public int work(ElectricMachineBlockEntity machine, Level level) {
        var recipe = floodable(machine, level);

        if (recipe != null) {
            return recipe.value().getTime();
        }

        return machine.fluidRoom(Fluids.WATER) > 0 ? WATER_TICKS : 0;
    }

    @Override
    public void finish(ElectricMachineBlockEntity machine, Level level) {
        var recipe = floodable(machine, level);

        if (recipe != null) {
            var flood = recipe.value();

            machine.getInput().shrink(flood.getInputCount());
            machine.removeFluid(flood.getWater());
            machine.insertOutputs(List.of(flood.getResult().copy()));
            return;
        }

        var room = machine.fluidRoom(Fluids.WATER);

        if (room > 0) {
            machine.addFluid(Fluids.WATER, Math.min(WATER_PER_JOB, room));
        }
    }

    @Override
    public boolean accepts(ElectricMachineBlockEntity machine, Level level, ItemStack stack) {
        return !stack.isEmpty()
            && level.getRecipeManager().getRecipeFor(HumanRecipes.HYDRATING_TYPE.get(), new SingleRecipeInput(stack), level).isPresent();
    }

    /** The recipe for the input - but only when there is the water for it and room for what it makes. */
    private @Nullable RecipeHolder<HydratorRecipe> floodable(ElectricMachineBlockEntity machine, Level level) {
        var input = machine.getInput();

        if (input.isEmpty()) {
            return null;
        }

        var recipe = quickCheck.getRecipeFor(new SingleRecipeInput(input), level).orElse(null);

        if (recipe == null) {
            return null;
        }

        var water = machine.getFluid();
        var enoughWater = recipe.value().getWater() <= 0
            || water.fluid().isSame(Fluids.WATER) && water.amount() >= recipe.value().getWater();

        return enoughWater && machine.canInsertOutputs(List.of(recipe.value().getResult().copy())) ? recipe : null;
    }
}
