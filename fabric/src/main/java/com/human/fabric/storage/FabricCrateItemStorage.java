package com.human.fabric.storage;

import com.human.common.gameplay.storage.CrateInventory;
import com.human.common.registry.init.HumanCrates;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.item.base.SingleStackStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StoragePreconditions;
import net.fabricmc.fabric.api.transfer.v1.storage.base.CombinedStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;

/**
 * A crate as Fabric item storage, for hoppers and other mods. Every block of a crate answers with its master's
 * inventory. Each slot holds 64 of anything; Fabric's own slot storage handles transactions and rollback.
 */
public final class FabricCrateItemStorage {

    private FabricCrateItemStorage() {}

    public static void register() {
        ItemStorage.SIDED.registerForBlockEntity(
            (crate, side) -> {
                var master = crate.master();

                return master == null ? null : master.<Storage<ItemVariant>>loaderView(FabricCrateItemStorage::of);
            },
            HumanCrates.CRATE_BLOCK_ENTITY.get()
        );
    }

    private static Storage<ItemVariant> of(CrateInventory inventory) {
        var slots = new ArrayList<CrateSlot>(inventory.size());

        for (int i = 0; i < inventory.size(); i++) {
            slots.add(new CrateSlot(inventory, i));
        }

        return new IndexedCrateStorage(inventory, slots);
    }

    /**
     * Fabric's combined storage walks every slot on each insert and extract - up to 1620 for a container, on every
     * hopper or modded-pipe transfer. This one goes straight to the slots that matter through the crate's item index:
     * slots already holding the item, then empty slots. Each slot still handles its own transaction and rollback, so
     * aborted transfers restore exactly.
     */
    private static final class IndexedCrateStorage extends CombinedStorage<ItemVariant, CrateSlot> {

        private final CrateInventory inventory;

        private IndexedCrateStorage(CrateInventory inventory, java.util.List<CrateSlot> slots) {
            super(slots);
            this.inventory = inventory;
        }

        @Override
        public long insert(ItemVariant resource, long maxAmount, TransactionContext transaction) {
            StoragePreconditions.notBlankNotNegative(resource, maxAmount);

            if (!CrateInventory.canStore(resource.toStack())) {
                return 0;
            }

            long inserted = 0;

            for (var slot : inventory.slotsHolding(resource.getItem())) {
                if (inserted >= maxAmount) {
                    return inserted;
                }

                inserted += parts.get(slot).insert(resource, maxAmount - inserted, transaction);
            }

            // An insert into an empty slot occupies it straight away, so the next search moves past it.
            for (int slot = inventory.nextEmpty(0); slot >= 0 && inserted < maxAmount; slot = inventory.nextEmpty(slot + 1)) {
                inserted += parts.get(slot).insert(resource, maxAmount - inserted, transaction);
            }

            return inserted;
        }

        @Override
        public long extract(ItemVariant resource, long maxAmount, TransactionContext transaction) {
            StoragePreconditions.notBlankNotNegative(resource, maxAmount);

            long extracted = 0;

            for (var slot : inventory.slotsHolding(resource.getItem())) {
                if (extracted >= maxAmount) {
                    break;
                }

                extracted += parts.get(slot).extract(resource, maxAmount - extracted, transaction);
            }

            return extracted;
        }
    }

    private static final class CrateSlot extends SingleStackStorage {

        private final CrateInventory inventory;

        private final int slot;

        private CrateSlot(CrateInventory inventory, int slot) {
            this.inventory = inventory;
            this.slot = slot;
        }

        @Override
        protected ItemStack getStack() {
            return inventory.get(slot);
        }

        @Override
        protected void setStack(ItemStack stack) {
            inventory.setSlot(slot, stack);
        }

        @Override
        protected int getCapacity(ItemVariant variant) {
            return CrateInventory.SLOT_LIMIT;
        }

        @Override
        protected boolean canInsert(ItemVariant variant) {
            return CrateInventory.canStore(variant.toStack());
        }
    }
}
