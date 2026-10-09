package com.human.neoforge.power;

import com.human.common.gameplay.power.ItemEnergyBridge;
import com.human.common.gameplay.power.StackSlot;
import net.neoforged.neoforge.capabilities.Capabilities;

/**
 * The NeoForge half of item charging: FE straight through NeoForge's own energy capability, so ANY mod's chargeable
 * item works with no per-mod code.
 * <p>
 * ⚠ Install from the mod entrypoint. Unlike the fluid factory this has NO ordering constraint - it is consulted at
 * interaction time, long after registration - but it must be installed at all, or the pad silently charges nothing.
 * </p>
 * <p>
 * ⚠ The capability speaks in {@code int}. Our grid speaks in {@code long}, so every offer is clamped to
 * {@link Integer#MAX_VALUE} before the call. Without that a large transfer would overflow negative and the item would
 * appear to DRAIN.
 * </p>
 */
public final class NeoForgeItemEnergyBridge implements ItemEnergyBridge.Bridge {

    private NeoForgeItemEnergyBridge() {}

    public static void install() {
        ItemEnergyBridge.setBridge(new NeoForgeItemEnergyBridge());
    }

    @Override
    public boolean canCharge(StackSlot slot) {
        var energy = slot.get().getCapability(Capabilities.EnergyStorage.ITEM);

        return energy != null && energy.canReceive() && energy.getEnergyStored() < energy.getMaxEnergyStored();
    }

    @Override
    public long charge(StackSlot slot, long amount) {
        var energy = slot.get().getCapability(Capabilities.EnergyStorage.ITEM);

        if (energy == null || !energy.canReceive()) {
            return 0L;
        }

        var offer = (int) Math.min(amount, Integer.MAX_VALUE);
        var accepted = energy.receiveEnergy(offer, false);

        // ⚠ The capability mutated the stack IN PLACE, so there is nothing new to write back - but the slot is told
        // anyway, so a machine that needs to mark itself changed gets the same signal on both loaders.
        if (accepted > 0) {
            slot.set(slot.get());
        }

        return accepted;
    }

    @Override
    public boolean canDischarge(StackSlot slot) {
        var energy = slot.get().getCapability(Capabilities.EnergyStorage.ITEM);
        return energy != null && energy.canExtract();
    }

    /** The mirror of {@link #charge}, with the same int clamp. */
    @Override
    public long discharge(StackSlot slot, long amount) {
        var energy = slot.get().getCapability(Capabilities.EnergyStorage.ITEM);

        if (energy == null || !energy.canExtract()) {
            return 0L;
        }

        var request = (int) Math.min(amount, Integer.MAX_VALUE);
        var drawn = energy.extractEnergy(request, false);

        if (drawn > 0) {
            slot.set(slot.get());
        }

        return drawn;
    }

    @Override
    public float chargeFraction(StackSlot slot) {
        var energy = slot.get().getCapability(Capabilities.EnergyStorage.ITEM);

        if (energy == null || energy.getMaxEnergyStored() <= 0) {
            return -1.0F;
        }

        return (float) energy.getEnergyStored() / energy.getMaxEnergyStored();
    }
}
