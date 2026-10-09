package com.human.fabric.power;

import com.human.common.gameplay.power.ItemEnergyBridge;
import com.human.common.gameplay.power.StackSlot;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleVariantStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.world.item.ItemStack;
import team.reborn.energy.api.EnergyStorage;
import team.reborn.energy.api.EnergyStorageUtil;

/**
 * The Fabric half of item charging, through Team Reborn Energy.
 * <h2>⚠⚠ WHY {@code ContainerItemContext.withConstant} IS NOT USED</h2> It is the obvious call and it is WRONG here.
 * {@code ConstantContainerItemContext} backs itself with its own {@code SingleVariantStorage}, disconnected from the
 * real slot - the insert succeeds, reports a number, and the charge is discarded on commit. The pad would appear to
 * charge and forget on reload, with no error anywhere.
 * <p>
 * {@code ofSingleSlot} over a storage that actually reads and writes OUR slot is the correct route, which is the same
 * shape Oritech uses (its {@code StackContext} is a stack plus an updater, and Fabric's context type never leaves the
 * loader boundary).
 * </p>
 * <h2>⚠ Transactions</h2> Energy's insert takes a {@link TransactionContext} and only becomes real on commit. Each
 * transfer gets its own short transaction: commit on success, and let it close unconmitted when nothing was accepted,
 * so a refusing item leaves no trace.
 * <h2>⚠ No int clamping</h2> Unlike NeoForge's capability, this API speaks {@code long} throughout - so the overflow
 * guard the NeoForge bridge needs has no counterpart here, and adding one would silently cap large transfers.
 */
public final class FabricItemEnergyBridge implements ItemEnergyBridge.Bridge {

    private FabricItemEnergyBridge() {}

    public static void install() {
        ItemEnergyBridge.setBridge(new FabricItemEnergyBridge());
    }

    @Override
    public boolean canCharge(StackSlot slot) {
        // ⭐ Takes a bare stack - the one part of this API that needs no context at all.
        return EnergyStorageUtil.isEnergyStorage(slot.get());
    }

    @Override
    public long charge(StackSlot slot, long amount) {
        var storage = find(slot);

        if (storage == null || !storage.supportsInsertion()) {
            return 0L;
        }

        try (var transaction = Transaction.openOuter()) {
            var inserted = storage.insert(amount, transaction);

            if (inserted <= 0L) {
                return 0L;
            }

            transaction.commit();

            return inserted;
        }
    }

    @Override
    public boolean canDischarge(StackSlot slot) {
        var storage = find(slot);
        return storage != null && storage.supportsExtraction();
    }

    /** The mirror of {@link #charge}: one short transaction, committed only when something was actually drawn. */
    @Override
    public long discharge(StackSlot slot, long amount) {
        var storage = find(slot);

        if (storage == null || !storage.supportsExtraction()) {
            return 0L;
        }

        try (var transaction = Transaction.openOuter()) {
            var extracted = storage.extract(amount, transaction);

            if (extracted <= 0L) {
                return 0L;
            }

            transaction.commit();

            return extracted;
        }
    }

    @Override
    public float chargeFraction(StackSlot slot) {
        var storage = find(slot);

        if (storage == null || storage.getCapacity() <= 0L) {
            return -1.0F;
        }

        return (float) storage.getAmount() / storage.getCapacity();
    }

    /** Builds a context whose main slot IS our slot, so an insert lands back where it came from. */
    private static EnergyStorage find(StackSlot slot) {
        var stack = slot.get();

        if (stack.isEmpty()) {
            return null;
        }

        return EnergyStorage.ITEM.find(stack, ContainerItemContext.ofSingleSlot(new SlotStorage(slot)));
    }

    /**
     * A one-slot storage view over {@link StackSlot}.
     * <p>
     * ⚠ {@code SingleVariantStorage} is what carries the modified variant back: when the energy API changes the stack's
     * components it exchanges the variant here, and {@link #onFinalCommit} is where that becomes our stack again.
     * Without this method the transfer still "works" and the charge still vanishes.
     * </p>
     */
    private static final class SlotStorage extends SingleVariantStorage<ItemVariant> {

        private final StackSlot slot;

        private SlotStorage(StackSlot slot) {
            this.slot = slot;
            this.variant = ItemVariant.of(slot.get());
            this.amount = slot.get().getCount();
        }

        // ⚠ getBlankVARIANT, not getBlankResource. The class is SingleVariantStorage and its abstract method is
        // named for the variant; getBlankResource does not exist and the class silently stays abstract.
        @Override
        protected ItemVariant getBlankVariant() {
            return ItemVariant.blank();
        }

        @Override
        protected long getCapacity(ItemVariant variant) {
            return variant.getItem().getDefaultMaxStackSize();
        }

        @Override
        protected void onFinalCommit() {
            slot.set(variant.isBlank() || amount <= 0L ? ItemStack.EMPTY : variant.toStack((int) amount));
        }
    }
}
