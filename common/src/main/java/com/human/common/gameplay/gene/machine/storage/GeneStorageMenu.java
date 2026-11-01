package com.human.common.gameplay.gene.machine.storage;

import com.human.common.registry.init.HumanGeneStorage;
import net.minecraft.server.level.ServerLevel;
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
 * The Gene Storage screen's menu (approved mockup gene_storage_ingame_scale3.png).
 * <p>
 * The 90 rack slots exist only to sync the rack to the client: they sit far off-screen and refuse clicks, because the
 * screen draws the rack itself (tube glyphs, selection, search dimming). Everything you do to the rack goes through
 * {@link #clickMenuButton}: store all, take a tube, sort, AUTO SORT, ISOLATED. Shift-clicking a filled tube in your
 * inventory stores it.
 * </p>
 * <p>
 * Network figures (storages on this grid, their tubes and room, this one's fill rank) are worked out once a second
 * while the screen is open - never per tick, never while nobody is looking.
 * </p>
 */
public class GeneStorageMenu extends AbstractContainerMenu {

    public static final int RACK_SLOTS = GeneStorageInventory.SIZE;

    public static final int SCREEN_WIDTH = 384;

    public static final int SCREEN_HEIGHT = 342;

    /**
     * Player inventory, matching the mockup's lower-left panel (cells at 10 + c * 18, 246 + r * 18; hotbar row at 304).
     */
    public static final int INVENTORY_X = 11;

    public static final int INVENTORY_Y = 247;

    public static final int HOTBAR_Y = 305;

    private static final int OFF_SCREEN = -10000;

    public static final int BUTTON_STORE_ALL = 0;

    public static final int BUTTON_SORT_AZ = 1;

    public static final int BUTTON_SORT_ZA = 2;

    public static final int BUTTON_SORT_DOSE = 3;

    public static final int BUTTON_AUTO_SORT = 4;

    public static final int BUTTON_ISOLATED = 5;

    /** BUTTON_TAKE + slot takes that rack slot's tubes. */
    public static final int BUTTON_TAKE = 1000;

    // Synced data indices.
    public static final int DATA_SUPPLIED = 0;

    public static final int DATA_ISOLATED = 1;

    public static final int DATA_AUTO_SORT = 2;

    public static final int DATA_SORT_MODE = 3;

    public static final int DATA_ON_GRID = 4;

    public static final int DATA_STORAGES = 5;

    public static final int DATA_NETWORK_TUBES = 6;

    public static final int DATA_NETWORK_ROOM = 7;

    public static final int DATA_FILL_RANK = 8;

    private static final int DATA_COUNT = 9;

    private static final int REFRESH_TICKS = 20;

    private final Container rack;

    private final ContainerData data;

    private final @Nullable GeneStorageBlockEntity storage;

    private final int[] network = new int[DATA_COUNT];

    private int refreshTimer;

    /** Client side. */
    public GeneStorageMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, new SimpleContainer(RACK_SLOTS), new SimpleContainerData(DATA_COUNT), null);
    }

    public GeneStorageMenu(int containerId, Inventory inventory, GeneStorageBlockEntity storage) {
        this(containerId, inventory, storage.rack(), null, storage);
    }

    private GeneStorageMenu(
        int containerId,
        Inventory inventory,
        Container rack,
        @Nullable ContainerData clientData,
        @Nullable GeneStorageBlockEntity storage
    ) {
        super(HumanGeneStorage.MENU.get(), containerId);
        this.rack = rack;
        this.storage = storage;
        this.data = clientData != null ? clientData : new ServerData();

        for (int slot = 0; slot < RACK_SLOTS; slot++) {
            addSlot(new RackSlot(rack, slot));
        }
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9, INVENTORY_X + column * 18, INVENTORY_Y + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, INVENTORY_X + column * 18, HOTBAR_Y));
        }
        addDataSlots(data);

        if (storage != null) {
            storage.startViewing();
            refreshNetwork();
        }
    }

    public Container rack() {
        return rack;
    }

    public int data(int index) {
        return data.get(index);
    }

    // ------------------------------------------------------------------------------------------------------------
    // Server
    // ------------------------------------------------------------------------------------------------------------

    @Override
    public void broadcastChanges() {
        if (storage != null && --refreshTimer <= 0) {
            refreshTimer = REFRESH_TICKS;
            refreshNetwork();
        }
        super.broadcastChanges();
    }

    private void refreshNetwork() {
        if (storage == null || !(storage.getLevel() instanceof ServerLevel level)) {
            return;
        }
        var grid = com.human.common.gameplay.power.PowerSystem.get(level).gridAt(storage.gridPos());
        network[DATA_ON_GRID] = grid != null ? 1 : 0;

        // Everything on this grid, isolated or not, so the figures describe the whole rack room.
        var storages = 0;
        var tubes = 0;
        var room = 0;
        var rank = 1;
        if (grid != null) {
            for (var member : GeneStorageNetwork.allOnGrid(level, storage.storagePos(), storage.gridPos())) {
                storages++;
                tubes += member.rack().tubeCount();
                room += GeneStorageInventory.SIZE;
                // Fill rank: the non-isolated storages placed before this one fill ahead of it on ties in distance.
                if (member != storage && !member.isIsolated() && member.fillOrder() < storage.fillOrder()) {
                    rank++;
                }
            }
        }
        network[DATA_STORAGES] = storages;
        network[DATA_NETWORK_TUBES] = tubes;
        network[DATA_NETWORK_ROOM] = room;
        // 0 = never fills (isolated, or on no grid).
        network[DATA_FILL_RANK] = storage.isIsolated() || grid == null ? 0 : rank;
    }

    private final class ServerData implements ContainerData {

        @Override
        public int get(int index) {
            if (storage == null) {
                return 0;
            }
            return switch (index) {
                case DATA_SUPPLIED -> storage.isSupplied() ? 1 : 0;
                case DATA_ISOLATED -> storage.isIsolated() ? 1 : 0;
                case DATA_AUTO_SORT -> storage.rack().isAutoSort() ? 1 : 0;
                case DATA_SORT_MODE -> storage.rack().sortMode().ordinal();
                default -> network[index];
            };
        }

        @Override
        public void set(int index, int value) {}

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    }

    @Override
    public boolean clickMenuButton(@NotNull Player player, int id) {
        if (storage == null) {
            return false;
        }
        var store = storage.rack();
        if (id >= BUTTON_TAKE && id < BUTTON_TAKE + RACK_SLOTS) {
            var slot = id - BUTTON_TAKE;
            var stack = store.getItem(slot);
            if (stack.isEmpty()) {
                return false;
            }
            var moving = stack.copy();
            // add() moves what fits and leaves the rest in 'moving'; no room at all leaves the rack untouched.
            player.getInventory().add(moving);
            if (moving.getCount() == stack.getCount()) {
                return false;
            }
            store.setItem(slot, moving.isEmpty() ? ItemStack.EMPTY : moving);
            storage.rackChanged();
            return true;
        }
        switch (id) {
            case BUTTON_STORE_ALL -> {
                var inventory = player.getInventory();
                for (int slot = 0; slot < inventory.items.size(); slot++) {
                    var stack = inventory.items.get(slot);
                    if (GeneStorageInventory.accepts(stack)) {
                        inventory.items.set(slot, store.insert(stack));
                    }
                }
                inventory.setChanged();
                storage.rackChanged();
            }
            case BUTTON_SORT_AZ -> sort(GeneStorageInventory.SortMode.A_Z);
            case BUTTON_SORT_ZA -> sort(GeneStorageInventory.SortMode.Z_A);
            case BUTTON_SORT_DOSE -> sort(GeneStorageInventory.SortMode.DOSE);
            case BUTTON_AUTO_SORT -> {
                store.setAutoSort(!store.isAutoSort(), store.sortMode());
                storage.rackChanged();
            }
            case BUTTON_ISOLATED -> {
                storage.setIsolated(!storage.isIsolated());
                refreshNetwork();
            }
            default -> {
                return false;
            }
        }
        return true;
    }

    private void sort(GeneStorageInventory.SortMode mode) {
        if (storage == null) {
            return;
        }
        var store = storage.rack();
        if (store.isAutoSort()) {
            store.setAutoSort(true, mode);
        } else {
            store.sort(mode);
        }
        storage.rackChanged();
    }

    /** Shift-click: a filled tube in your inventory goes into the rack. Rack slots are never clicked directly. */
    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
        if (index < RACK_SLOTS || storage == null) {
            return ItemStack.EMPTY;
        }
        var slot = slots.get(index);
        var stack = slot.getItem();
        if (!GeneStorageInventory.accepts(stack)) {
            return ItemStack.EMPTY;
        }
        var left = storage.rack().insert(stack);
        if (left.getCount() == stack.getCount()) {
            return ItemStack.EMPTY;
        }
        slot.set(left);
        storage.rackChanged();
        // Nothing more to move from this slot on the next loop pass.
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return storage == null
            || !storage.isRemoved()
                && storage.isSupplied()
                && player.distanceToSqr(storage.modelCentre().add(0.0D, 1.0D, 0.0D)) <= 64.0D;
    }

    @Override
    public void removed(@NotNull Player player) {
        super.removed(player);
        if (storage != null) {
            storage.stopViewing();
        }
    }

    /** Sync-only: off-screen, takes and gives nothing by hand. */
    private static final class RackSlot extends Slot {

        RackSlot(Container container, int slot) {
            super(container, slot, OFF_SCREEN, OFF_SCREEN);
        }

        @Override
        public boolean mayPlace(@NotNull ItemStack stack) {
            return false;
        }

        @Override
        public boolean mayPickup(@NotNull Player player) {
            return false;
        }

        @Override
        public boolean isActive() {
            return false;
        }
    }
}
