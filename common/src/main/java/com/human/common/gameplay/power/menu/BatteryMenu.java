package com.human.common.gameplay.power.menu;

import com.human.common.gameplay.block.entity.power.impl.BatteryBlockEntity;
import com.human.common.gameplay.power.StoreMode;
import com.human.common.gameplay.power.upgrade.BatteryUpgrade;
import com.human.common.gameplay.power.upgrade.BatteryUpgradeItem;
import com.human.common.gameplay.power.upgrade.RedstoneControl;
import com.human.common.registry.init.HumanMenuTypes;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The battery's screen: charge, rate, store mode, and four upgrade slots with the settings the installed modules add.
 * <p>
 * Buttons go through vanilla's menu-button click ({@link #clickMenuButton}) - no packets of our own. The numbers come
 * through {@link ContainerData}, which vanilla sends as 16-bit values, so every long is four pieces
 * ({@link BatteryBlockEntity#unchunk}).
 * </p>
 */
public class BatteryMenu extends AbstractContainerMenu {

    public static final int BUTTON_MODE = 0;

    public static final int BUTTON_RESERVE = 1;

    public static final int BUTTON_REDSTONE = 2;

    public static final int BUTTON_ALARM = 3;

    /** [stated] Oct 2: side-button pop-ups set a value directly. BUTTON_SET_MODE + mode ordinal. */
    public static final int BUTTON_SET_MODE = 10;

    /** BUTTON_SET_RESERVE + index into RESERVE_PERCENT - needs the emergency card. */
    public static final int BUTTON_SET_RESERVE = 20;

    /** BUTTON_SET_REDSTONE + control ordinal - needs the redstone card. */
    public static final int BUTTON_SET_REDSTONE = 30;

    /** BUTTON_SET_ALARM + index into ALARM_PERCENT - needs the alarm card. */
    public static final int BUTTON_SET_ALARM = 40;

    /** Where the upgrade slots sit: a 2 x 2 block on the right of the panel. */
    public static final int UPGRADE_X = 125;

    public static final int UPGRADE_Y = 18;

    private static final int PLAYER_FIRST = BatteryBlockEntity.UPGRADE_SLOTS;

    private static final int PLAYER_END = PLAYER_FIRST + 36;

    private final Container upgrades;

    private final ContainerData data;

    @Nullable
    private final BatteryBlockEntity blockEntity;

    /** Client constructor - the menu type's factory. */
    public BatteryMenu(int containerId, Inventory inventory) {
        this(
            containerId,
            inventory,
            new SimpleContainer(BatteryBlockEntity.UPGRADE_SLOTS),
            new SimpleContainerData(BatteryBlockEntity.DATA_COUNT),
            null
        );
    }

    /** Server constructor. */
    public BatteryMenu(int containerId, Inventory inventory, BatteryBlockEntity blockEntity) {
        this(containerId, inventory, blockEntity.getUpgradeContainer(), blockEntity.getData(), blockEntity);
    }

    private BatteryMenu(
        int containerId,
        Inventory inventory,
        Container upgrades,
        ContainerData data,
        @Nullable BatteryBlockEntity blockEntity
    ) {
        super(HumanMenuTypes.BATTERY.get(), containerId);
        checkContainerSize(upgrades, BatteryBlockEntity.UPGRADE_SLOTS);
        checkContainerDataCount(data, BatteryBlockEntity.DATA_COUNT);

        this.upgrades = upgrades;
        this.data = data;
        this.blockEntity = blockEntity;

        for (var index = 0; index < BatteryBlockEntity.UPGRADE_SLOTS; index++) {
            addSlot(new UpgradeSlot(upgrades, index, UPGRADE_X + (index % 2) * 18, UPGRADE_Y + (index / 2) * 18));
        }

        for (var row = 0; row < 3; row++) {
            for (var column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9, 8 + column * 18, 84 + row * 18));
            }
        }

        for (var column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, 8 + column * 18, 142));
        }

        addDataSlots(data);
    }

    // ------------------------------------------------------------------------------------------------------------
    // What the screen reads
    // ------------------------------------------------------------------------------------------------------------

    public long stored() {
        return BatteryBlockEntity.unchunk(data, BatteryBlockEntity.DATA_STORED);
    }

    public long capacity() {
        return BatteryBlockEntity.unchunk(data, BatteryBlockEntity.DATA_CAPACITY);
    }

    public long rate() {
        return BatteryBlockEntity.unchunk(data, BatteryBlockEntity.DATA_RATE);
    }

    public StoreMode mode() {
        return BatteryBlockEntity.modeOf(data);
    }

    public int reservePercent() {
        return BatteryBlockEntity.RESERVE_PERCENT[Math.floorMod(
            data.get(BatteryBlockEntity.DATA_RESERVE),
            BatteryBlockEntity.RESERVE_PERCENT.length
        )];
    }

    public RedstoneControl redstone() {
        return RedstoneControl.byOrdinal(data.get(BatteryBlockEntity.DATA_REDSTONE));
    }

    public int alarmPercent() {
        return BatteryBlockEntity.ALARM_PERCENT[Math.floorMod(
            data.get(BatteryBlockEntity.DATA_ALARM),
            BatteryBlockEntity.ALARM_PERCENT.length
        )];
    }

    public boolean powered() {
        return data.get(BatteryBlockEntity.DATA_POWERED) != 0;
    }

    /** Whether a module is in the slots - read from the slots themselves, which are synced to the client too. */
    public boolean has(BatteryUpgrade upgrade) {
        for (var index = 0; index < upgrades.getContainerSize(); index++) {
            if (upgrades.getItem(index).getItem() instanceof BatteryUpgradeItem item && item.getUpgrade() == upgrade) {
                return true;
            }
        }

        return false;
    }

    // ------------------------------------------------------------------------------------------------------------
    // Buttons
    // ------------------------------------------------------------------------------------------------------------

    @Override
    public boolean clickMenuButton(@NotNull Player player, int id) {
        if (blockEntity == null) {
            return false;
        }

        // The direct setters - each range-checked, and each module's own card checked, exactly as the cycle buttons
        // are.
        if (id >= BUTTON_SET_MODE && id < BUTTON_SET_MODE + StoreMode.values().length) {
            blockEntity.setMode(StoreMode.values()[id - BUTTON_SET_MODE]);
            return true;
        }

        if (id >= BUTTON_SET_RESERVE && id < BUTTON_SET_RESERVE + BatteryBlockEntity.RESERVE_PERCENT.length) {
            if (!blockEntity.has(BatteryUpgrade.EMERGENCY)) {
                return false;
            }

            blockEntity.setReserveIndex(id - BUTTON_SET_RESERVE);
            return true;
        }

        if (id >= BUTTON_SET_REDSTONE && id < BUTTON_SET_REDSTONE + RedstoneControl.values().length) {
            if (!blockEntity.has(BatteryUpgrade.REDSTONE)) {
                return false;
            }

            blockEntity.setRedstoneControl(RedstoneControl.byOrdinal(id - BUTTON_SET_REDSTONE));
            return true;
        }

        if (id >= BUTTON_SET_ALARM && id < BUTTON_SET_ALARM + BatteryBlockEntity.ALARM_PERCENT.length) {
            if (!blockEntity.has(BatteryUpgrade.ALARM)) {
                return false;
            }

            blockEntity.setAlarmIndex(id - BUTTON_SET_ALARM);
            return true;
        }

        switch (id) {
            case BUTTON_MODE -> blockEntity.cycleMode();
            case BUTTON_RESERVE -> {
                if (!blockEntity.has(BatteryUpgrade.EMERGENCY)) {
                    return false;
                }

                blockEntity.cycleReserve();
            }
            case BUTTON_REDSTONE -> {
                if (!blockEntity.has(BatteryUpgrade.REDSTONE)) {
                    return false;
                }

                blockEntity.cycleRedstone();
            }
            case BUTTON_ALARM -> {
                if (!blockEntity.has(BatteryUpgrade.ALARM)) {
                    return false;
                }

                blockEntity.cycleAlarm();
            }
            default -> {
                return false;
            }
        }

        return true;
    }

    // ------------------------------------------------------------------------------------------------------------
    // Slots
    // ------------------------------------------------------------------------------------------------------------

    @Override
    public boolean stillValid(@NotNull Player player) {
        return blockEntity == null || blockEntity.stillValid(player);
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
        var slot = slots.get(index);

        if (!slot.hasItem() || !slot.mayPickup(player)) {
            return ItemStack.EMPTY;
        }

        var stack = slot.getItem();
        var copy = stack.copy();

        if (index < PLAYER_FIRST) {
            if (!moveItemStackTo(stack, PLAYER_FIRST, PLAYER_END, true)) {
                return ItemStack.EMPTY;
            }
        } else if (!(stack.getItem() instanceof BatteryUpgradeItem) || !moveItemStackTo(stack, 0, PLAYER_FIRST, false)) {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }

        return copy;
    }

    private final class UpgradeSlot extends Slot {

        private UpgradeSlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        /**
         * Upgrade cards only, one per slot. A MODULE (anything but capacity and transfer) goes in once: a second copy
         * would do nothing, so it is refused rather than silently wasted.
         */
        @Override
        public boolean mayPlace(@NotNull ItemStack stack) {
            if (!(stack.getItem() instanceof BatteryUpgradeItem item)) {
                return false;
            }

            if (item.getUpgrade().stackable()) {
                return true;
            }

            for (var other = 0; other < container.getContainerSize(); other++) {
                if (
                    other != getContainerSlot()
                        && container.getItem(other).getItem() instanceof BatteryUpgradeItem installed
                        && installed.getUpgrade() == item.getUpgrade()
                ) {
                    return false;
                }
            }

            return true;
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }

        /**
         * ⚠ A capacity upgrade stays put while the battery holds more than it could hold without it - taking it out
         * would delete the difference. The screen's tooltip says why. Works on both sides: the numbers are menu data.
         */
        @Override
        public boolean mayPickup(@NotNull Player player) {
            if (getItem().getItem() instanceof BatteryUpgradeItem item && item.getUpgrade() == BatteryUpgrade.CAPACITY) {
                return stored() <= capacity() - BatteryBlockEntity.CAPACITY;
            }

            return true;
        }
    }

    /** For the screen's tooltip: whether this upgrade slot is currently locked by the rule in mayPickup. */
    public boolean isLockedCapacitySlot(Slot slot) {
        return slot instanceof UpgradeSlot && !slot.mayPickup(null);
    }
}
