package com.human.common.gameplay.entity.living.human.marine;

import com.human.common.gameplay.item.GunItem;
import com.human.common.registry.init.HumanDataComponents;
import net.minecraft.world.item.ItemStack;

/**
 * Restocks a marine that has shot itself dry.
 * <p>
 * Without this a gunner is a one-fight asset: the magazine and the two issued spares run out, the weapon strategies
 * stop accepting the gun, and a trained marine spends the rest of its existence as a knife. Resupply is the offscreen
 * fiction of a squad drawing from its own stores, so it only runs when the marine is genuinely empty and only after a
 * wait long enough that it can never rescue anyone mid-firefight.
 * <p>
 * The timer starts when the marine runs dry rather than running continuously, so five minutes of fighting does not bank
 * a resupply that lands the instant the last round is spent.
 */
public class MarineResupply {

    /**
     * Wait between running dry and the restock arriving.
     */
    public static final int RESUPPLY_DELAY_IN_TICKS = 6000;

    /**
     * Reloads' worth of ammunition delivered. Matches the spares a marine is issued with at spawn, so a resupply puts
     * it back where it started rather than topping it up indefinitely.
     */
    private static final int RESUPPLY_RELOADS = 2;

    /**
     * How often the check runs. Ammunition state cannot change without the marine firing, so sampling every second is
     * ample and keeps a scan of the pack off the per-tick path - it is 108 slots now that marines carry a full pack.
     */
    private static final int CHECK_INTERVAL_IN_TICKS = 20;

    /**
     * Tick at which the pending resupply lands, or {@link #NO_RESUPPLY_PENDING} when the marine is not waiting on one.
     */
    private int resupplyTick = NO_RESUPPLY_PENDING;

    public static final int NO_RESUPPLY_PENDING = Integer.MIN_VALUE;

    public void tick(Marine marine) {
        if (marine.tickCount % CHECK_INTERVAL_IN_TICKS != 0) {
            return;
        }

        if (!isOutOfAmmunition(marine)) {
            // Picked something up, was handed a drum, or is carrying a gun that was never empty. Any of those cancels
            // a pending resupply: the timer is for a marine that has nothing, not one that is merely low.
            resupplyTick = NO_RESUPPLY_PENDING;

            return;
        }

        if (resupplyTick == NO_RESUPPLY_PENDING) {
            resupplyTick = marine.tickCount + RESUPPLY_DELAY_IN_TICKS;

            return;
        }

        if (marine.tickCount >= resupplyTick) {
            resupplyTick = NO_RESUPPLY_PENDING;

            resupply(marine);
        }
    }

    /**
     * Whether every gun the marine carries is empty and there is nothing left to reload from.
     * <p>
     * Both halves matter. A marine holding an empty rifle and a full drum is not out - it is one reload away, and the
     * reload will happen on its own. Only when neither the guns nor the pack hold anything is the marine actually
     * reduced to its knife.
     */
    public static boolean isOutOfAmmunition(Marine marine) {
        var inventory = marine.getInventory();
        var carriesAGun = false;

        for (var slotIndex = 0; slotIndex < inventory.getSize(); slotIndex++) {
            var itemStack = inventory.getItemStack(slotIndex);

            if (!(itemStack.getItem() instanceof GunItem gunItem)) {
                continue;
            }

            carriesAGun = true;

            if (itemStack.getOrDefault(HumanDataComponents.AMMUNITION.get(), 0) > 0) {
                return false;
            }

            var ammunitionItemSupplier = gunItem.getGunConfig().ammunitionItemSupplier();

            if (ammunitionItemSupplier != null && inventory.hasItem(ammunitionItemSupplier.get().asItem())) {
                return false;
            }
        }

        // A knife-only marine was never issued a gun and has nothing to resupply.
        return carriesAGun;
    }

    /**
     * Delivers spare ammunition for every gun the marine carries.
     */
    private static void resupply(Marine marine) {
        var inventory = marine.getInventory();

        for (var slotIndex = 0; slotIndex < inventory.getSize(); slotIndex++) {
            var itemStack = inventory.getItemStack(slotIndex);

            if (!(itemStack.getItem() instanceof GunItem gunItem)) {
                continue;
            }

            var gunConfig = gunItem.getGunConfig();
            var ammunitionItemSupplier = gunConfig.ammunitionItemSupplier();

            if (ammunitionItemSupplier == null) {
                continue;
            }

            var reloadAmount = Math.max(1, gunConfig.reloadAmount());
            // Items per reload, worked out exactly as the reload itself does: a drum is one item, loose rounds many.
            var itemsPerReload = (int) Math.ceil(gunConfig.maximumAmmunition() / (float) reloadAmount);
            var resupplyItems = itemsPerReload * RESUPPLY_RELOADS;

            if (resupplyItems <= 0) {
                continue;
            }

            var resupplyStack = new ItemStack(ammunitionItemSupplier.get(), resupplyItems);

            // Marine-owned, matching the issued spares - so a resupplied marine is not a renewable source of drums for
            // a player who parks one somewhere safe and comes back every five minutes.
            resupplyStack.set(HumanDataComponents.MARINE_OWNED.get(), true);

            marine.getInventory().addItemStack(resupplyStack);
        }
    }

    public int getResupplyTick() {
        return resupplyTick;
    }

    public void setResupplyTick(int resupplyTick) {
        this.resupplyTick = resupplyTick;
    }
}
