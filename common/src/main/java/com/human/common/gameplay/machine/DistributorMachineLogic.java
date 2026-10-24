package com.human.common.gameplay.machine;

import com.human.common.gameplay.block.entity.machine.ElectricMachineBlockEntity;
import com.human.common.gameplay.distributor.DistributorBlockEntity;
import com.human.common.gameplay.vapor.VaporKind;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;

/**
 * The distributor as the shared machine loop sees it: a one-tick job, repeated. [stated] Oct 17: "pours 1 vapor per
 * tick, 30 FE/t, only while pouring". So {@link #work} answers 1 on every tick it should pour - the machine then asks
 * the grid for its 30 FE - and {@link #finish} runs on the tick the grid paid, pouring that tick's vapor. With no
 * power, nothing is poured and nothing is lost.
 * <p>
 * ⚠ Speed cards do nothing here: a one-tick job cannot get shorter. Efficiency cards still cut the draw.
 * </p>
 */
public final class DistributorMachineLogic implements MachineLogic {

    @Override
    public int work(ElectricMachineBlockEntity machine, Level level) {
        return machine instanceof DistributorBlockEntity distributor && distributor.wantsToPour(level) ? 1 : 0;
    }

    @Override
    public void finish(ElectricMachineBlockEntity machine, Level level) {
        if (machine instanceof DistributorBlockEntity distributor) {
            distributor.pourOne();
        }
    }

    /** [stated] tanks in from the side: only the five filled vapor tanks - never the empty aluminum tank. */
    @Override
    public boolean accepts(ElectricMachineBlockEntity machine, Level level, ItemStack stack) {
        return VaporKind.fromTank(stack) != null;
    }

    /** [stated] liquid in on top: only the liquids that make a vapor - anything else would sit in the tank forever. */
    @Override
    public boolean acceptsFluid(ElectricMachineBlockEntity machine, Level level, Fluid fluid) {
        return VaporKind.fromFluid(fluid) != null;
    }
}
