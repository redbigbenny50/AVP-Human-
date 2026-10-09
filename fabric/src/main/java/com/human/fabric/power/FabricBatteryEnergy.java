package com.human.fabric.power;

import com.human.common.gameplay.item.power.BatteryItem;
import com.human.common.registry.init.item.HumanItems;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import team.reborn.energy.api.EnergyStorage;

/**
 * The battery pack and nuclear battery as Team Reborn Energy items on Fabric - so our charging hub (through
 * {@link FabricItemEnergyBridge}) and other mods' chargers and machines can fill and drain them.
 * <h2>⚠ THE CHANGE GOES BACK THROUGH THE CONTEXT</h2> An item storage on Fabric must never edit a stack in place: the
 * stack it reads is a copy. Each transfer works on a copy, then EXCHANGES the context's item for the updated one inside
 * the caller's transaction - so an aborted transfer leaves the cell untouched. A nuclear cell drained to zero is
 * EXTRACTED from the context instead: consumed.
 */
public final class FabricBatteryEnergy implements EnergyStorage {

    private final ContainerItemContext context;

    private final BatteryItem battery;

    private FabricBatteryEnergy(ContainerItemContext context, BatteryItem battery) {
        this.context = context;
        this.battery = battery;
    }

    public static void register() {
        EnergyStorage.ITEM.registerForItems(
            (stack, context) -> stack.getItem() instanceof BatteryItem battery ? new FabricBatteryEnergy(context, battery) : null,
            HumanItems.BATTERY_PACK.get(),
            HumanItems.NUCLEAR_BATTERY.get()
        );
    }

    @Override
    public boolean supportsInsertion() {
        return battery.isRechargeable();
    }

    @Override
    public long insert(long maxAmount, TransactionContext transaction) {
        if (!battery.isRechargeable() || context.getAmount() != 1) {
            return 0L;
        }

        var stack = context.getItemVariant().toStack();
        var moved = battery.receive(stack, maxAmount, false);

        return moved > 0L && context.exchange(ItemVariant.of(stack), 1, transaction) == 1 ? moved : 0L;
    }

    @Override
    public long extract(long maxAmount, TransactionContext transaction) {
        if (context.getAmount() != 1) {
            return 0L;
        }

        var variant = context.getItemVariant();
        var stack = variant.toStack();
        var moved = battery.extract(stack, maxAmount, false);

        if (moved <= 0L) {
            return 0L;
        }

        // Drained nuclear cell: the stack is now empty - it is taken out, not swapped.
        if (stack.isEmpty()) {
            return context.extract(variant, 1, transaction) == 1 ? moved : 0L;
        }

        return context.exchange(ItemVariant.of(stack), 1, transaction) == 1 ? moved : 0L;
    }

    @Override
    public long getAmount() {
        return battery.getStored(context.getItemVariant().toStack());
    }

    @Override
    public long getCapacity() {
        return battery.getCapacity();
    }
}
