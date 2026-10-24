package com.human.neoforge.fluid;

import com.human.common.gameplay.fluid.FluidContent;
import com.human.common.gameplay.item.canister.UniversalCanisterItem;
import com.human.common.registry.init.item.HumanItems;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import org.jetbrains.annotations.NotNull;

/**
 * The universal canister as a NeoForge fluid container, so any mod's tank, pipe, machine or {@code FluidUtil} call can
 * fill and drain it - and so NeoForge itself sees what is inside.
 * <h2>RULES</h2>
 * <ul>
 * <li>ONE canister at a time: a stack of empties is refused, as NeoForge's own container handlers do. Callers such as
 * {@code FluidUtil} split one off first.</li>
 * <li>One fluid: fills only when empty or holding the same fluid.</li>
 * <li>No fluid data: a fluid stack carrying extra components is refused, because the canister stores fluid and amount
 * only and would silently drop the rest.</li>
 * </ul>
 */
public final class NeoForgeCanisterFluidHandler implements IFluidHandlerItem {

    private ItemStack container;

    public NeoForgeCanisterFluidHandler(ItemStack container) {
        this.container = container;
    }

    public static void register(RegisterCapabilitiesEvent event) {
        event.registerItem(
            Capabilities.FluidHandler.ITEM,
            (stack, context) -> new NeoForgeCanisterFluidHandler(stack),
            HumanItems.CANISTER.get()
        );

        // Fluid ENDPOINTS as blocks - each face's rule comes from the endpoint itself (FluidEndpoint.canFill/canDrain):
        // a placed canister fills through its top and drains through its bottom; a tank takes fluid in at the top,
        // gives
        // it out at the bottom, and each side is input or output as set with the wiring tool.
        event.registerBlockEntity(
            Capabilities.FluidHandler.BLOCK,
            com.human.common.registry.init.HumanBlockEntityTypes.CANISTER.get(),
            NeoForgeFluidEndpointHandler::forSide
        );
        event.registerBlockEntity(
            Capabilities.FluidHandler.BLOCK,
            com.human.common.registry.init.HumanBlockEntityTypes.FLUID_TANK.get(),
            NeoForgeFluidEndpointHandler::forSide
        );

        // [stated] Oct 3: the cryotube controller takes cryofluid in through the port on its TOP. It was a fluid
        // endpoint all along but was never registered, so pipes could not see it and would not connect.
        event.registerBlockEntity(
            Capabilities.FluidHandler.BLOCK,
            com.human.common.registry.init.HumanBlockEntityTypes.CRYOTUBE_CONTROLLER.get(),
            NeoForgeFluidEndpointHandler::forSide
        );

        // [stated] readme: the press and hydrator give fluid out of their bottom, the freezer takes it in on its left.
        for (
            var type : java.util.List.of(
                com.human.common.registry.init.HumanBlockEntityTypes.ELECTRIC_PRESS.get(),
                com.human.common.registry.init.HumanBlockEntityTypes.ELECTRIC_HYDRATOR.get(),
                com.human.common.registry.init.HumanBlockEntityTypes.ELECTRIC_FREEZER.get(),
                // [stated] Oct 17 - the vaporizer takes liquid in through the front of its bottom block.
                com.human.common.registry.init.HumanVaporizer.VAPORIZER_BLOCK_ENTITY.get(),
                // [stated] Oct 17 - the distributor takes liquid in through its TOP.
                com.human.common.registry.init.HumanDistributor.DISTRIBUTOR_BLOCK_ENTITY.get(),
                // ⚠ Oct 21 quality pass - the Creative Fluid Tank was never registered, so no pipe could see it.
                com.human.common.registry.init.HumanCreativeBlocks.CREATIVE_FLUID_TANK_BLOCK_ENTITY.get()
            )
        ) {
            event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, type, NeoForgeFluidEndpointHandler::forSide);
        }

        // Oct 15 - the quarry terminal's LIQUID port: the top of its back-row counter-clockwise block, which has no
        // block entity of its own, so it is registered on the BLOCKS and resolved to the terminal's tank.
        event.registerBlock(
            Capabilities.FluidHandler.BLOCK,
            (level, pos, state, blockEntity, side) -> {
                var port = com.human.common.gameplay.quarry.QuarryTerminalBlock.liquidPort(level, pos, state, side);

                return port == null ? null : NeoForgeFluidEndpointHandler.forSide(port, side);
            },
            com.human.common.registry.init.HumanQuarry.QUARRY_TERMINAL.get(),
            com.human.common.registry.init.HumanQuarry.WY_QUARRY_TERMINAL.get(),
            com.human.common.registry.init.HumanQuarry.USCM_QUARRY_TERMINAL.get()
        );
    }

    @Override
    public @NotNull ItemStack getContainer() {
        return container;
    }

    @Override
    public int getTanks() {
        return 1;
    }

    @Override
    public @NotNull FluidStack getFluidInTank(int tank) {
        var content = UniversalCanisterItem.getContent(container);

        return content.isEmpty() ? FluidStack.EMPTY : new FluidStack(content.fluid(), content.amount());
    }

    @Override
    public int getTankCapacity(int tank) {
        return UniversalCanisterItem.CAPACITY;
    }

    @Override
    public boolean isFluidValid(int tank, @NotNull FluidStack stack) {
        return !stack.isEmpty() && stack.isComponentsPatchEmpty();
    }

    @Override
    public int fill(@NotNull FluidStack resource, @NotNull FluidAction action) {
        if (container.getCount() != 1 || resource.isEmpty() || !isFluidValid(0, resource)) {
            return 0;
        }

        var content = UniversalCanisterItem.getContent(container);

        if (!content.accepts(resource.getFluid())) {
            return 0;
        }

        var filled = Math.min(UniversalCanisterItem.CAPACITY - content.amount(), resource.getAmount());

        if (filled > 0 && action.execute()) {
            UniversalCanisterItem.setContent(container, FluidContent.of(resource.getFluid(), content.amount() + filled));
        }

        return Math.max(0, filled);
    }

    @Override
    public @NotNull FluidStack drain(@NotNull FluidStack resource, @NotNull FluidAction action) {
        var content = UniversalCanisterItem.getContent(container);

        if (resource.isEmpty() || content.isEmpty() || !content.fluid().isSame(resource.getFluid())) {
            return FluidStack.EMPTY;
        }

        return drain(resource.getAmount(), action);
    }

    @Override
    public @NotNull FluidStack drain(int maxDrain, @NotNull FluidAction action) {
        if (container.getCount() != 1 || maxDrain <= 0) {
            return FluidStack.EMPTY;
        }

        var content = UniversalCanisterItem.getContent(container);

        if (content.isEmpty()) {
            return FluidStack.EMPTY;
        }

        var drained = Math.min(maxDrain, content.amount());
        var result = new FluidStack(content.fluid(), drained);

        if (action.execute()) {
            UniversalCanisterItem.setContent(container, content.withAmount(content.amount() - drained));
        }

        return result;
    }
}
