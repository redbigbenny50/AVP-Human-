package com.human.neoforge.fluid;

import com.human.common.gameplay.fluid.FluidEndpoint;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Any {@link FluidEndpoint} - placed canister, fluid tank - as a NeoForge fluid handler for one face.
 * <p>
 * The face's rule is asked on every fill and drain ({@link FluidEndpoint#canFill}), never captured here: NeoForge and
 * other mods cache this handler, and a tank side toggled to output must take effect for a pipe that already holds it.
 * </p>
 */
public final class NeoForgeFluidEndpointHandler implements IFluidHandler {

    private final FluidEndpoint endpoint;

    @Nullable
    private final Direction side;

    private NeoForgeFluidEndpointHandler(FluidEndpoint endpoint, @Nullable Direction side) {
        this.endpoint = endpoint;
        this.side = side;
    }

    /** The handler for one face, or null when that face is not a port at all. */
    public static @Nullable IFluidHandler forSide(FluidEndpoint endpoint, @Nullable Direction side) {
        return endpoint.isPort(side) ? new NeoForgeFluidEndpointHandler(endpoint, side) : null;
    }

    @Override
    public int getTanks() {
        return 1;
    }

    @Override
    public @NotNull FluidStack getFluidInTank(int tank) {
        var content = endpoint.getContent();

        return content.isEmpty() ? FluidStack.EMPTY : new FluidStack(content.fluid(), content.amount());
    }

    @Override
    public int getTankCapacity(int tank) {
        return endpoint.getCapacity();
    }

    @Override
    public boolean isFluidValid(int tank, @NotNull FluidStack stack) {
        return endpoint.canFill(side) && !stack.isEmpty() && stack.isComponentsPatchEmpty();
    }

    @Override
    public int fill(@NotNull FluidStack resource, @NotNull FluidAction action) {
        if (!isFluidValid(0, resource)) {
            return 0;
        }

        return endpoint.insert(resource.getFluid(), resource.getAmount(), action.simulate());
    }

    @Override
    public @NotNull FluidStack drain(@NotNull FluidStack resource, @NotNull FluidAction action) {
        if (!endpoint.canDrain(side) || resource.isEmpty()) {
            return FluidStack.EMPTY;
        }

        var fluid = endpoint.getContent().fluid();
        var drained = endpoint.extract(resource.getFluid(), resource.getAmount(), action.simulate());

        return drained <= 0 ? FluidStack.EMPTY : new FluidStack(fluid, drained);
    }

    @Override
    public @NotNull FluidStack drain(int maxDrain, @NotNull FluidAction action) {
        if (!endpoint.canDrain(side)) {
            return FluidStack.EMPTY;
        }

        var fluid = endpoint.getContent().fluid();
        var drained = endpoint.extract(null, maxDrain, action.simulate());

        return drained <= 0 ? FluidStack.EMPTY : new FluidStack(fluid, drained);
    }
}
