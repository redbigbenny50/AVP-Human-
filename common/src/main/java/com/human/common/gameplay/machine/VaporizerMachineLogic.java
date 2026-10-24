package com.human.common.gameplay.machine;

import com.human.common.gameplay.acid.AcidKind;
import com.human.common.gameplay.block.entity.machine.ElectricMachineBlockEntity;
import com.human.common.gameplay.fluid.FluidContent;
import com.human.common.registry.init.HumanChemicals;
import com.human.common.registry.init.HumanFluids;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * The vaporizer. [stated] Oct 17: one bucket of liquid and one empty aluminum tank make one filled tank in 10 seconds:
 * <ul>
 * <li>cryofluid - Cryo Tank</li>
 * <li>lava - Infernium Tank</li>
 * <li>xeno acid - Acidium Tank; nether xeno acid - Nether Acidium Tank; irradiated xeno acid - Irradium Tank</li>
 * </ul>
 * The tank holds ONE liquid at a time (8 buckets) and only takes the liquids above - anything else is refused at the
 * intake, so it can never fill up with something it cannot use. A filled tank goes into the next free spot of the 3 x 3
 * grid; with all nine full it stops, drawing nothing, until one is taken.
 * <p>
 * A bucket of one of those liquids put in the empty-tank slot by hand is poured in, and the empty bucket goes to the
 * grid - the machine's shared container handling, the same as the freezer's.
 * </p>
 */
public final class VaporizerMachineLogic implements MachineLogic {

    /** [stated] 1 bucket per tank. */
    public static final int FLUID_PER_TANK = FluidContent.BUCKET;

    /** [stated] 10 seconds per tank. */
    public static final int TICKS_PER_TANK = 200;

    @Override
    public int work(ElectricMachineBlockEntity machine, Level level) {
        var result = resultFor(machine);

        return result != null && machine.canInsertOutputs(List.of(new ItemStack(result))) ? TICKS_PER_TANK : 0;
    }

    @Override
    public void finish(ElectricMachineBlockEntity machine, Level level) {
        var result = resultFor(machine);

        // ⚠ Re-checked, not assumed: a hopper may have filled the grid since work() said yes.
        if (result == null || !machine.canInsertOutputs(List.of(new ItemStack(result)))) {
            return;
        }

        machine.getInput().shrink(1);
        machine.removeFluid(FLUID_PER_TANK);
        machine.insertOutputs(List.of(new ItemStack(result)));
    }

    /** Only empty aluminum tanks go in by automation - or a bucket of a liquid it takes, which it pours itself. */
    @Override
    public boolean accepts(ElectricMachineBlockEntity machine, Level level, ItemStack stack) {
        if (stack.is(HumanChemicals.ALUMINUM_TANK.get())) {
            return true;
        }

        var held = MachineFluidItems.contained(stack);

        return !held.isEmpty() && acceptsFluid(machine, level, held.fluid());
    }

    @Override
    public boolean acceptsFluid(ElectricMachineBlockEntity machine, Level level, Fluid fluid) {
        return tankFor(fluid) != null;
    }

    /** What the liquid in the tank makes, when there is an empty tank in and a full bucket's worth of liquid. */
    private static @Nullable Item resultFor(ElectricMachineBlockEntity machine) {
        var fluid = machine.getFluid();

        if (!machine.getInput().is(HumanChemicals.ALUMINUM_TANK.get()) || fluid.isEmpty() || fluid.amount() < FLUID_PER_TANK) {
            return null;
        }

        return tankFor(fluid.fluid());
    }

    /** The filled tank for a liquid, or null for a liquid the vaporizer does not take. */
    public static @Nullable Item tankFor(Fluid fluid) {
        var source = FluidContent.sourceOf(fluid);

        if (source.isSame(HumanFluids.CRYOFLUID.get())) {
            return HumanChemicals.CRYO_TANK.get();
        }

        if (source.isSame(Fluids.LAVA)) {
            return HumanChemicals.INFERNIUM_TANK.get();
        }

        if (source.isSame(HumanChemicals.acid(AcidKind.XENO))) {
            return HumanChemicals.ACIDIUM_TANK.get();
        }

        if (source.isSame(HumanChemicals.acid(AcidKind.NETHER))) {
            return HumanChemicals.NETHER_ACIDIUM_TANK.get();
        }

        if (source.isSame(HumanChemicals.acid(AcidKind.IRRADIATED))) {
            return HumanChemicals.IRRADIUM_TANK.get();
        }

        return null;
    }
}
