package com.human.neoforge.fluid;

import com.human.common.gameplay.fluid.FluidContent;
import com.human.common.gameplay.fluid.pipe.PipeFluidAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;

/**
 * Other mods' fluid blocks for our pipes, on NeoForge: the fluid-handler capability. A foreign face's colour comes from
 * what it can do NOW - only give (red), only take (green), both (yellow) - since mods do not say which way a face
 * works.
 */
public final class NeoForgePipeFluidAccess implements PipeFluidAccess {

    private NeoForgePipeFluidAccess() {}

    public static void install() {
        PipeFluidAccess.Holder.install(new NeoForgePipeFluidAccess());
    }

    @Override
    public @Nullable Port openForeign(ServerLevel level, BlockPos pos, Direction side) {
        var handler = level.getCapability(Capabilities.FluidHandler.BLOCK, pos, side);

        if (handler == null || handler.getTanks() <= 0) {
            return null;
        }

        return new Port() {

            @Override
            public boolean present() {
                return true;
            }

            @Override
            public boolean canFill() {
                return handler.fill(new FluidStack(probeFluid(handler), 1), IFluidHandler.FluidAction.SIMULATE) > 0;
            }

            @Override
            public boolean canDrain() {
                return !handler.drain(1, IFluidHandler.FluidAction.SIMULATE).isEmpty();
            }

            @Override
            public FluidContent peek(int max) {
                var stack = handler.drain(max, IFluidHandler.FluidAction.SIMULATE);

                return stack.isEmpty() ? FluidContent.EMPTY : FluidContent.of(stack.getFluid(), stack.getAmount());
            }

            @Override
            public int fill(Fluid fluid, int amount, boolean simulate) {
                return amount <= 0 ? 0 : handler.fill(new FluidStack(fluid, amount), action(simulate));
            }

            @Override
            public int drain(Fluid fluid, int amount, boolean simulate) {
                return amount <= 0 ? 0 : handler.drain(new FluidStack(fluid, amount), action(simulate)).getAmount();
            }

            @Override
            public Object storage() {
                return pos.immutable();
            }
        };
    }

    /** What to test a fill with: what it already holds, or water when it is empty. */
    private static Fluid probeFluid(IFluidHandler handler) {
        for (var tank = 0; tank < handler.getTanks(); tank++) {
            var held = handler.getFluidInTank(tank);

            if (!held.isEmpty()) {
                return held.getFluid();
            }
        }

        return Fluids.WATER;
    }

    private static IFluidHandler.FluidAction action(boolean simulate) {
        return simulate ? IFluidHandler.FluidAction.SIMULATE : IFluidHandler.FluidAction.EXECUTE;
    }
}
