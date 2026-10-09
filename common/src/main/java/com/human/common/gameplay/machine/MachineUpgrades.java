package com.human.common.gameplay.machine;

import com.human.common.gameplay.item.power.HubChipItem;
import com.human.common.gameplay.power.upgrade.BatteryUpgrade;
import com.human.common.gameplay.power.upgrade.BatteryUpgradeItem;
import com.human.common.gameplay.power.upgrade.UpgradeCard;
import com.human.common.gameplay.power.upgrade.UpgradeCardItem;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

/**
 * What a machine's four upgrade slots accept, and what they add up to. [stated] Oct 2: one card per slot; a stackable
 * card (speed, efficiency) adds up across slots, a module (the hub chip, the redstone card) counts once and a second
 * copy is refused.
 */
public final class MachineUpgrades {

    private MachineUpgrades() {}

    /** True for a card a machine can use at all. */
    public static boolean fits(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        if (stack.getItem() instanceof UpgradeCardItem card) {
            return card.getCard().fits(UpgradeCard.Target.MACHINE);
        }

        return stack.getItem() instanceof HubChipItem
            || stack.getItem() instanceof BatteryUpgradeItem battery && battery.getUpgrade() == BatteryUpgrade.REDSTONE;
    }

    /** Whether {@code stack} may go into {@code slot}: it fits, and a module is not already fitted in another slot. */
    public static boolean mayPlace(Container upgrades, int slot, ItemStack stack) {
        if (!fits(stack)) {
            return false;
        }

        if (stack.getItem() instanceof UpgradeCardItem card && card.getCard().stackable()) {
            return true;
        }

        var key = key(stack);

        for (int other = 0; other < upgrades.getContainerSize(); other++) {
            if (other != slot && key.equals(key(upgrades.getItem(other)))) {
                return false;
            }
        }

        return true;
    }

    /** How many {@code card} cards are fitted. */
    public static int count(Container upgrades, UpgradeCard card) {
        int count = 0;

        for (int slot = 0; slot < upgrades.getContainerSize(); slot++) {
            if (upgrades.getItem(slot).getItem() instanceof UpgradeCardItem item && item.getCard() == card) {
                count++;
            }
        }

        return count;
    }

    public static boolean hasHub(Container upgrades) {
        for (int slot = 0; slot < upgrades.getContainerSize(); slot++) {
            if (upgrades.getItem(slot).getItem() instanceof HubChipItem) {
                return true;
            }
        }

        return false;
    }

    public static boolean hasRedstone(Container upgrades) {
        for (int slot = 0; slot < upgrades.getContainerSize(); slot++) {
            if (upgrades.getItem(slot).getItem() instanceof BatteryUpgradeItem battery && battery.getUpgrade() == BatteryUpgrade.REDSTONE) {
                return true;
            }
        }

        return false;
    }

    /** The machine's draw multiplier for the speed and efficiency cards fitted. */
    public static double drawMultiplier(Container upgrades) {
        return UpgradeCard.drawMultiplier(count(upgrades, UpgradeCard.SPEED), count(upgrades, UpgradeCard.EFFICIENCY));
    }

    /** A recipe's time after speed cards: never below one tick. */
    public static int effectiveTicks(Container upgrades, int recipeTicks) {
        if (recipeTicks <= 0) {
            return recipeTicks;
        }

        return Math.max(1, (int) Math.ceil(recipeTicks / UpgradeCard.speedMultiplier(count(upgrades, UpgradeCard.SPEED))));
    }

    private static String key(ItemStack stack) {
        if (stack.getItem() instanceof UpgradeCardItem card) {
            return card.getCard().id();
        }

        if (stack.getItem() instanceof HubChipItem) {
            return "hub";
        }

        if (stack.getItem() instanceof BatteryUpgradeItem battery) {
            return "battery:" + battery.getUpgrade().id();
        }

        return "";
    }

    /**
     * [stated] Oct 2: with a redstone card fitted, the machine can follow a redstone signal. Without the card it
     * ignores redstone whatever this is set to.
     */
    public enum RedstoneMode {

        /** Runs whatever the signal. */
        IGNORE,
        /** Runs only while it has a signal. */
        HIGH,
        /** Runs only while it has none. */
        LOW;

        public boolean allows(boolean powered) {
            return switch (this) {
                case IGNORE -> true;
                case HIGH -> powered;
                case LOW -> !powered;
            };
        }

        public RedstoneMode next() {
            return values()[(ordinal() + 1) % values().length];
        }

        public static RedstoneMode byOrdinal(int ordinal) {
            return ordinal >= 0 && ordinal < values().length ? values()[ordinal] : IGNORE;
        }
    }
}
