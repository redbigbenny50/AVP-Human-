package com.human.common.gameplay.power;

import net.minecraft.world.item.ItemStack;

/**
 * Pushes FE into an item held by one of our machines - ours or any other mod's.
 * <h2>⚠⚠ WHY THIS IS A LOADER HOOK AND NOT JUST A METHOD</h2> There is no common way to ask an ItemStack about its
 * energy. NeoForge answers through its own capability system ({@code Capabilities.EnergyStorage.ITEM}), which ships
 * with NeoForge; Fabric answers through Team Reborn's Energy API, which is a THIRD-PARTY library and is not a
 * dependency of this project today. Neither type exists in {@code :common}, so common asks this bridge and each loader
 * installs its own.
 * <p>
 * ⚠ THE DEFAULT IS A DELIBERATE NO-OP, NOT AN OVERSIGHT. With no bridge installed the pad accepts an item, holds it and
 * renders it, and simply transfers nothing. That is the correct behaviour for a loader whose energy API is absent: it
 * fails visibly and harmlessly rather than crashing on a missing class.
 * </p>
 * <p>
 * ⚠ To enable this on FABRIC, Team Reborn Energy has to be added to {@code fabric/build.gradle} and a bridge installed
 * in the Fabric entrypoint the way {@code NeoForgeItemEnergyBridge} does. Until then Fabric charges nothing.
 * </p>
 */
public final class ItemEnergyBridge {

    private static Bridge bridge = new Bridge() {};

    private ItemEnergyBridge() {}

    public static void setBridge(Bridge newBridge) {
        bridge = newBridge == null ? new Bridge() {} : newBridge;
    }

    /** True when this stack can accept any energy at all - used to decide whether the pad should take it. */
    public static boolean canCharge(ItemStack stack) {
        return !stack.isEmpty() && bridge.canCharge(StackSlot.readOnly(stack));
    }

    /**
     * ⚠ TAKES A SLOT, NOT A STACK - see {@link StackSlot}. Fabric needs somewhere to put the modified stack; passing a
     * bare stack silently loses the charge there while working perfectly on NeoForge.
     *
     * @return how much was ACTUALLY accepted, so the machine only ever bills itself for that. Never more than offered.
     */
    public static long charge(StackSlot slot, long amount) {
        if (slot == null || slot.get().isEmpty() || amount <= 0L) {
            return 0L;
        }

        return Math.max(0L, Math.min(amount, bridge.charge(slot, amount)));
    }

    /** 0-1 for a display bar, or -1 when the item does not report a capacity. */
    /**
     * Whether {@code stack} is an energy item that can GIVE power - a battery the cryotube controller (or any machine)
     * can run from. True for an empty one too: whether it can be placed in a battery slot does not depend on its
     * charge.
     */
    public static boolean canDischarge(ItemStack stack) {
        return !stack.isEmpty() && bridge.canDischarge(StackSlot.readOnly(stack));
    }

    /**
     * Draws up to {@code amount} from the item in {@code slot}, writing the drained stack back through the slot.
     * Returns what was actually drawn - never more than asked, never negative.
     */
    public static long discharge(StackSlot slot, long amount) {
        if (slot == null || slot.get().isEmpty() || amount <= 0L) {
            return 0L;
        }

        return Math.max(0L, Math.min(amount, bridge.discharge(slot, amount)));
    }

    public static float chargeFraction(ItemStack stack) {
        return stack.isEmpty() ? -1.0F : bridge.chargeFraction(StackSlot.readOnly(stack));
    }

    public interface Bridge {

        default boolean canCharge(StackSlot slot) {
            return false;
        }

        default long charge(StackSlot slot, long amount) {
            return 0L;
        }

        default boolean canDischarge(StackSlot slot) {
            return false;
        }

        default long discharge(StackSlot slot, long amount) {
            return 0L;
        }

        default float chargeFraction(StackSlot slot) {
            return -1.0F;
        }
    }
}
