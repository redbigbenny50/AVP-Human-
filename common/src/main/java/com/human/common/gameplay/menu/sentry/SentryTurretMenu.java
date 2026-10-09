package com.human.common.gameplay.menu.sentry;

import com.human.common.gameplay.entity.machine.SentryLoadout;
import com.human.common.gameplay.entity.machine.SentryTurret;
import com.human.common.gameplay.item.power.BatteryItem;
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
 * The sentry turret's screen. [stated] Oct 2 layout: four upgrade-card slots down the left; skin, network card, sensor
 * lens and battery down the right; the turret's model and power in the middle; nine ammo slots along the bottom.
 * <p>
 * ⚠ No duplication through a screen left open: {@link #stillValid} turns false the moment the turret is picked up or
 * destroyed, and the server checks it before applying any click.
 * </p>
 */
public class SentryTurretMenu extends AbstractContainerMenu {

    public static final int CARD_X = 8;

    public static final int RIGHT_X = 152;

    public static final int TOP_Y = 18;

    public static final int AMMO_Y = 92;

    public static final int PLAYER_Y = 114;

    public static final int HOTBAR_Y = 172;

    public static final int IMAGE_HEIGHT = 196;

    /** Menu slot index of the battery - after the loadout. */
    public static final int BATTERY_SLOT = SentryLoadout.SIZE;

    private static final int PLAYER_FIRST = BATTERY_SLOT + 1;

    private static final int PLAYER_END = PLAYER_FIRST + 36;

    private static final int DATA_ENERGY_LOW = 0;

    private static final int DATA_ENERGY_HIGH = 1;

    private static final int DATA_BOUND = 2;

    private static final int DATA_STATUS = 3;

    /** Heat, 0..100. */
    private static final int DATA_HEAT = 4;

    /** The turret's entity id in two halves - the screen finds the turret by it, to draw its model. */
    private static final int DATA_ENTITY_LOW = 5;

    private static final int DATA_ENTITY_HIGH = 6;

    private static final int DATA_REDSTONE = 7;

    private static final int DATA_COUNT = 8;

    /** [stated] Oct 2: the Redstone side button - BUTTON_SET_REDSTONE + mode ordinal. Owner only. */
    public static final int BUTTON_SET_REDSTONE = 20;

    /**
     * CLIENT ONLY: true while the Targeting page is shown. A slot's position is fixed, so the page switches every slot
     * off rather than moving them out from under it.
     */
    private boolean slotsHidden;

    private final Container loadout;

    private final ContainerData data;

    private final @Nullable SentryTurret turret;

    private int status;

    public SentryTurretMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, new SimpleContainer(SentryLoadout.SIZE), new SimpleContainer(1), null, null);
    }

    public SentryTurretMenu(int containerId, Inventory inventory, SentryTurret turret) {
        this(containerId, inventory, turret.getLoadout(), turret.getBattery(), null, turret);
    }

    private SentryTurretMenu(
        int containerId,
        Inventory inventory,
        Container loadout,
        Container battery,
        @Nullable ContainerData data,
        @Nullable SentryTurret turret
    ) {
        super(HumanMenuTypes.SENTRY_TURRET.get(), containerId);

        this.loadout = loadout;
        this.turret = turret;
        this.data = data != null ? data : turret != null ? serverData() : new SimpleContainerData(DATA_COUNT);

        // Ammo along the bottom.
        for (int index = 0; index < SentryLoadout.AMMO_SLOTS; index++) {
            addSlot(new LoadoutSlot(loadout, SentryLoadout.AMMO_FIRST + index, 8 + index * 18, AMMO_Y, () -> slotsHidden));
        }

        // Cards down the left.
        for (int index = 0; index < SentryLoadout.CARD_SLOTS; index++) {
            addSlot(new LoadoutSlot(loadout, SentryLoadout.CARD_FIRST + index, CARD_X, TOP_Y + index * 18, () -> slotsHidden));
        }

        // Skin, network card and lens down the right; the battery below them.
        addSlot(new LoadoutSlot(loadout, SentryLoadout.SKIN, RIGHT_X, TOP_Y, () -> slotsHidden));
        addSlot(new LoadoutSlot(loadout, SentryLoadout.NETWORK, RIGHT_X, TOP_Y + 18, () -> slotsHidden));
        addSlot(new LoadoutSlot(loadout, SentryLoadout.SENSOR, RIGHT_X, TOP_Y + 36, () -> slotsHidden));

        addSlot(new Slot(battery, 0, RIGHT_X, TOP_Y + 54) {

            @Override
            public boolean mayPlace(@NotNull ItemStack stack) {
                return stack.getItem() instanceof BatteryItem && !isBound();
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }

            @Override
            public boolean isActive() {
                return !slotsHidden && (!isBound() || hasItem());
            }
        });

        for (var row = 0; row < 3; row++) {
            for (var column = 0; column < 9; column++) {
                addSlot(new PlayerSlot(inventory, column + row * 9 + 9, 8 + column * 18, PLAYER_Y + row * 18));
            }
        }

        for (var column = 0; column < 9; column++) {
            addSlot(new PlayerSlot(inventory, column, 8 + column * 18, HOTBAR_Y));
        }

        addDataSlots(this.data);
    }

    /**
     * A loadout slot - its rules (what fits, one card each, one item in all but the ammo) are {@link SentryLoadout}.
     */
    private static class LoadoutSlot extends Slot {

        private final java.util.function.BooleanSupplier hidden;

        LoadoutSlot(Container container, int slot, int x, int y, java.util.function.BooleanSupplier hidden) {
            super(container, slot, x, y);
            this.hidden = hidden;
        }

        @Override
        public boolean mayPlace(@NotNull ItemStack stack) {
            return SentryLoadout.mayPlace(container, getContainerSlot(), stack);
        }

        @Override
        public int getMaxStackSize() {
            return SentryLoadout.maxStack(getContainerSlot());
        }

        @Override
        public boolean isActive() {
            return !hidden.getAsBoolean();
        }
    }

    /** A player inventory slot that hides with the page. */
    private class PlayerSlot extends Slot {

        PlayerSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean isActive() {
            return !slotsHidden;
        }
    }

    private ContainerData serverData() {
        return new ContainerData() {

            @Override
            public int get(int index) {
                if (turret == null) {
                    return 0;
                }

                var energy = turret.getEnergy();

                return switch (index) {
                    case DATA_ENERGY_LOW -> (int) (energy & 0xFFFF);
                    case DATA_ENERGY_HIGH -> (int) (energy >>> 16 & 0xFFFF);
                    case DATA_BOUND -> turret.getFusePanel() != null ? 1 : 0;
                    case DATA_STATUS -> status;
                    case DATA_HEAT -> Math.round(turret.getHeatFraction() * 100.0F);
                    case DATA_ENTITY_LOW -> turret.getId() & 0xFFFF;
                    case DATA_ENTITY_HIGH -> turret.getId() >>> 16 & 0xFFFF;
                    case DATA_REDSTONE -> turret.getRedstoneMode().ordinal();
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {}

            @Override
            public int getCount() {
                return DATA_COUNT;
            }
        };
    }

    /** ⚡ The status flags are re-read once a second, not every tick. */
    @Override
    public void broadcastChanges() {
        if (turret != null && turret.tickCount % 20 == 0) {
            status = turret.getStatusFlags();
        }

        super.broadcastChanges();
    }

    public long getEnergy() {
        return (data.get(DATA_ENERGY_LOW) & 0xFFFFL) | (data.get(DATA_ENERGY_HIGH) & 0xFFFFL) << 16;
    }

    public boolean isBound() {
        return data.get(DATA_BOUND) != 0;
    }

    public int getStatus() {
        return data.get(DATA_STATUS);
    }

    /** The turret's entity id, as synced. */
    public int getTurretId() {
        return (data.get(DATA_ENTITY_LOW) & 0xFFFF) | (data.get(DATA_ENTITY_HIGH) & 0xFFFF) << 16;
    }

    public com.human.common.gameplay.machine.MachineUpgrades.RedstoneMode getRedstoneMode() {
        return com.human.common.gameplay.machine.MachineUpgrades.RedstoneMode.byOrdinal(data.get(DATA_REDSTONE));
    }

    /** SERVER: the turret this screen is for - the Targeting page's packet applies to it. Null on the client. */
    public @Nullable SentryTurret getTurret() {
        return turret;
    }

    public void setSlotsHidden(boolean hidden) {
        slotsHidden = hidden;
    }

    @Override
    public boolean clickMenuButton(@NotNull Player player, int id) {
        if (
            id >= BUTTON_SET_REDSTONE && id < BUTTON_SET_REDSTONE + com.human.common.gameplay.machine.MachineUpgrades.RedstoneMode
                .values().length
        ) {
            if (turret != null) {
                if (!turret.canConfigure(player)) {
                    return false;
                }

                turret.setRedstoneMode(com.human.common.gameplay.machine.MachineUpgrades.RedstoneMode.byOrdinal(id - BUTTON_SET_REDSTONE));
            }

            return true;
        }

        return false;
    }

    public float getHeat() {
        return data.get(DATA_HEAT) / 100.0F;
    }

    public boolean isOverheated() {
        return (getStatus() & SentryTurret.STATUS_OVERHEATED) != 0;
    }

    /** Which loadout slot a menu slot is, or -1. */
    public static int loadoutIndex(Slot slot) {
        return slot.index < SentryLoadout.SIZE ? slot.getContainerSlot() : -1;
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
        var slot = slots.get(index);

        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        var stack = slot.getItem();
        var original = stack.copy();

        if (index < PLAYER_FIRST) {
            // Out of the turret, into the player's inventory.
            if (!moveItemStackTo(stack, PLAYER_FIRST, PLAYER_END, true)) {
                return ItemStack.EMPTY;
            }
        } else if (stack.getItem() instanceof BatteryItem) {
            if (isBound() || !moveItemStackTo(stack, BATTERY_SLOT, BATTERY_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, 0, SentryLoadout.SIZE, false)) {
            // Each loadout slot's own rules decide where it can go - ammo to the ammo row, a card to a free card slot.
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }

        return stack.getCount() == original.getCount() ? ItemStack.EMPTY : original;
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return turret == null || turret.isAlive() && player.distanceToSqr(turret) <= 64.0D;
    }
}
