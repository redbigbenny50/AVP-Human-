package com.human.common.gameplay.machine;

import com.human.common.gameplay.block.entity.machine.ElectricMachineBlockEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;

/**
 * What a machine actually DOES - the one part that differs between machines. Everything else (power, the off/on/working
 * states, priority, the input filter, the working grace period, the animation clock) lives in
 * {@link ElectricMachineBlockEntity} and is shared.
 * <p>
 * One instance per machine block entity: implementations may keep a per-machine recipe cache.
 * </p>
 */
public interface MachineLogic {

    /**
     * Ticks the current job needs, or 0 when there is nothing to do right now - no input, no recipe, or no room for
     * what it would make. Called every tick; it must be cheap (use a cached recipe check).
     */
    int work(ElectricMachineBlockEntity machine, Level level);

    /** The job completed: consume the input and produce the output. Only called right after {@link #work} said yes. */
    void finish(ElectricMachineBlockEntity machine, Level level);

    /** Whether automation may insert this stack - there is a recipe for it. Hand insertion is always allowed. */
    boolean accepts(ElectricMachineBlockEntity machine, Level level, ItemStack stack);

    /**
     * Whether this machine's tank may take this fluid IN, from a pipe or a container. Only machines that use fluid say
     * yes, and only to fluids they have a recipe for - a freezer full of a fluid it cannot freeze would be stuck.
     */
    default boolean acceptsFluid(ElectricMachineBlockEntity machine, Level level, Fluid fluid) {
        return false;
    }
}
