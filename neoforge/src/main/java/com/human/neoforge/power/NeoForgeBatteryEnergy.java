package com.human.neoforge.power;

import com.human.common.gameplay.item.power.BatteryItem;
import com.human.common.registry.init.item.HumanItems;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.energy.IEnergyStorage;

/**
 * The battery pack and nuclear battery as NeoForge energy items - so our charging hub (through
 * {@link NeoForgeItemEnergyBridge}) and any other mod's charger or machine can fill and drain them.
 * <p>
 * ⚠ The capability speaks {@code int}; every value is clamped. A nuclear cell drained to zero shrinks its stack IN
 * PLACE ({@link BatteryItem#extract}), so the holder sees it consumed without any callback.
 * </p>
 */
public final class NeoForgeBatteryEnergy implements IEnergyStorage {

    private final ItemStack stack;

    private final BatteryItem battery;

    private NeoForgeBatteryEnergy(ItemStack stack, BatteryItem battery) {
        this.stack = stack;
        this.battery = battery;
    }

    public static void register(RegisterCapabilitiesEvent event) {
        event.registerItem(
            Capabilities.EnergyStorage.ITEM,
            (stack, context) -> stack.getItem() instanceof BatteryItem battery ? new NeoForgeBatteryEnergy(stack, battery) : null,
            HumanItems.BATTERY_PACK.get(),
            HumanItems.NUCLEAR_BATTERY.get()
        );
    }

    @Override
    public int receiveEnergy(int toReceive, boolean simulate) {
        return (int) battery.receive(stack, toReceive, simulate);
    }

    @Override
    public int extractEnergy(int toExtract, boolean simulate) {
        return (int) battery.extract(stack, toExtract, simulate);
    }

    @Override
    public int getEnergyStored() {
        return (int) Math.min(Integer.MAX_VALUE, battery.getStored(stack));
    }

    @Override
    public int getMaxEnergyStored() {
        return (int) Math.min(Integer.MAX_VALUE, battery.getCapacity());
    }

    @Override
    public boolean canExtract() {
        return true;
    }

    @Override
    public boolean canReceive() {
        return battery.isRechargeable();
    }
}
