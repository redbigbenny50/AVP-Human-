package com.human.fabric.fluid;

import com.human.common.gameplay.fluid.FluidContent;
import com.human.common.gameplay.item.canister.UniversalCanisterItem;
import com.human.common.registry.init.item.HumanItems;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleVariantItemStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;

/**
 * The universal canister as Fabric Transfer API fluid storage, so any mod's tank, pipe or machine can fill and drain
 * it.
 * <h2>⚠ UNITS: 81 DROPLETS TO THE MILLIBUCKET</h2> Fabric counts 81,000 droplets per bucket; the canister stores
 * millibuckets. Every transfer is rounded DOWN to a whole number of millibuckets before it happens. Without that, a
 * pipe offering, say, 100 droplets would move them and the canister would store one millibucket (81), silently deleting
 * the other 19.
 * <h2>HOW THE STACK CHANGES</h2> Fabric never edits the stack directly: {@code getUpdatedVariant} describes the new
 * canister, and the context swaps it in - one item at a time, so a stack of empties is handled by Fabric splitting one
 * off.
 */
public final class FabricCanisterFluidStorage extends SingleVariantItemStorage<FluidVariant> {

    private static final long DROPLETS_PER_MB = FluidConstants.BUCKET / FluidContent.BUCKET;

    public FabricCanisterFluidStorage(ContainerItemContext context) {
        super(context);
    }

    public static void register() {
        FluidStorage.ITEM.registerForItems(
            (stack, context) -> new FabricCanisterFluidStorage(context),
            HumanItems.CANISTER.get()
        );
    }

    @Override
    protected FluidVariant getBlankResource() {
        return FluidVariant.blank();
    }

    @Override
    protected FluidVariant getResource(ItemVariant variant) {
        var content = UniversalCanisterItem.getContent(variant.toStack());

        return content.isEmpty() ? FluidVariant.blank() : FluidVariant.of(content.fluid());
    }

    @Override
    protected long getAmount(ItemVariant variant) {
        return UniversalCanisterItem.getContent(variant.toStack()).amount() * DROPLETS_PER_MB;
    }

    @Override
    protected long getCapacity(FluidVariant variant) {
        return UniversalCanisterItem.CAPACITY * DROPLETS_PER_MB;
    }

    @Override
    protected ItemVariant getUpdatedVariant(ItemVariant currentVariant, FluidVariant newResource, long newAmount) {
        var stack = currentVariant.toStack();
        var millibuckets = (int) (newAmount / DROPLETS_PER_MB);

        UniversalCanisterItem.setContent(
            stack,
            newResource.isBlank() || millibuckets <= 0 ? FluidContent.EMPTY : FluidContent.of(newResource.getFluid(), millibuckets)
        );

        return ItemVariant.of(stack);
    }

    /** No fluid data: the canister stores fluid and amount only, and would silently drop anything else. */
    @Override
    protected boolean canInsert(FluidVariant resource) {
        return !resource.isBlank() && resource.getComponents().isEmpty();
    }

    @Override
    public long insert(FluidVariant resource, long maxAmount, TransactionContext transaction) {
        return super.insert(resource, maxAmount - maxAmount % DROPLETS_PER_MB, transaction);
    }

    @Override
    public long extract(FluidVariant resource, long maxAmount, TransactionContext transaction) {
        return super.extract(resource, maxAmount - maxAmount % DROPLETS_PER_MB, transaction);
    }
}
