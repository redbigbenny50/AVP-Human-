package com.human.common.gameplay.menu.marine;

import com.human.common.gameplay.entity.living.human.marine.Marine;
import com.human.common.gameplay.entity.living.human.marine.MarineMode;
import com.human.common.registry.init.HumanMenuTypes;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The screen behind right-clicking a hired marine.
 * <p>
 * Backed by the marine's real inventory on the server and by a throwaway {@link SimpleContainer} on the client, which
 * is all the client needs: the menu system streams slot contents to it, and the {@code MARINE_OWNED} component rides
 * along on those stacks so the client can tell locked slots from free ones without ever holding a reference to the
 * marine.
 * <p>
 * The command row at the top is deliberately open-ended. Follow and hold are the two commands that exist today; the
 * button ids and the synced mode value are structured so more can be added without touching the wire format.
 */
public class MarineInventoryMenu extends AbstractContainerMenu {

    /**
     * Rows in the marine's pack. Twelve is two double chests; a hired marine is meant to be worth guarding.
     * <p>
     * Independent of the panel size now that the window scrolls, so this is the one number to change if the pack should
     * be bigger. ⚠ Only ever raise it: {@code readAdditionalSaveData} re-adds saved stacks into a fresh inventory and
     * does not check the result, so lowering it would silently eat anything past the new end.
     */
    public static final int MARINE_ROW_COUNT = 12;

    /**
     * Rows visible at once. This IS bounded by the panel: five rows plus the command strip and the player's own
     * inventory comes to 228 of the 240 GUI pixels Minecraft guarantees. Trading a row here buys eighteen more pixels
     * of command strip without costing a single slot of storage.
     */
    public static final int VISIBLE_MARINE_ROW_COUNT = 5;

    public static final int COLUMN_COUNT = 9;

    public static final int MARINE_SLOT_COUNT = MARINE_ROW_COUNT * COLUMN_COUNT;

    public static final int SLOT_SIZE = 18;

    /**
     * Height of the command strip spliced into the panel above the marine's slots.
     * <p>
     * Minecraft only ever guarantees 240 effective GUI pixels of height - it steps the automatic scale down until the
     * window is at least 320x240 - so the whole panel has to fit inside that. At five visible rows this leaves room to
     * spare, and any of it can be spent here.
     */
    public static final int COMMAND_STRIP_HEIGHT = 24;

    public static final int BUTTON_TOGGLE_MODE = 0;

    public static final int BUTTON_END_CONTRACT = 1;

    public static final int BUTTON_TOGGLE_SENTRY = 2;

    public static final int MODE_FOLLOW = 0;

    public static final int MODE_HOLD = 1;

    public static final int MODE_SENTRY = 2;

    public static final int MARINE_SLOTS_Y = 18 + COMMAND_STRIP_HEIGHT;

    private static final int PLAYER_INVENTORY_Y = 121 + COMMAND_STRIP_HEIGHT;

    private static final int PLAYER_HOTBAR_Y = 179 + COMMAND_STRIP_HEIGHT;

    public static final int SLOTS_X = 8;

    private final Container container;

    /**
     * Server side only. The client half of a menu never has an entity reference and does not need one.
     */
    private final @Nullable Marine marine;

    /**
     * Mirror of the marine's mode for the client. On the server the data slot reads straight through to the marine, so
     * this field is only ever written by the incoming sync.
     */
    private int mode;

    private final DataSlot modeDataSlot;

    public MarineInventoryMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, new SimpleContainer(MARINE_SLOT_COUNT), null);
    }

    public MarineInventoryMenu(int containerId, Inventory playerInventory, Container container, @Nullable Marine marine) {
        super(HumanMenuTypes.MARINE_INVENTORY.get(), containerId);

        checkContainerSize(container, MARINE_SLOT_COUNT);

        this.container = container;
        this.marine = marine;

        container.startOpen(playerInventory.player);

        addMarineSlots(container);
        addPlayerInventorySlots(playerInventory);
        addPlayerHotbarSlots(playerInventory);

        this.modeDataSlot = addDataSlot(new DataSlot() {

            @Override
            public int get() {
                return marine != null
                    ? modeToIndex(marine.getMode())
                    : mode;
            }

            @Override
            public void set(int value) {
                mode = value;
            }
        });
    }

    public static int modeToIndex(MarineMode marineMode) {
        if (marineMode == MarineMode.SENTRY) {
            return MODE_SENTRY;
        }

        return marineMode == MarineMode.HOLD
            ? MODE_HOLD
            : MODE_FOLLOW;
    }

    /**
     * The mode as the client currently understands it, for labelling the command button.
     */
    /**
     * The marine this menu is for, or null on the client - which never has one.
     */
    public @Nullable Marine getMarine() {
        return marine;
    }

    public int getModeIndex() {
        return modeDataSlot.get();
    }

    /**
     * Whether ending the contract is currently allowed. Mirrors the server's own check so the button can be greyed out
     * rather than silently doing nothing when pressed.
     */
    public boolean canEndContract() {
        for (var slotIndex = 0; slotIndex < MARINE_SLOT_COUNT; slotIndex++) {
            var itemStack = slots.get(slotIndex).getItem();

            if (!itemStack.isEmpty() && !MarineInventorySlot.isIssuedGear(itemStack)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Handles a press in the command row.
     * <p>
     * Routed through vanilla's own container-button packet rather than a bespoke one. That packet already carries the
     * container id, is already validated against the menu the player actually has open, and costs no new registration -
     * which also means a future command is one more case in this switch and nothing else.
     */
    @Override
    public boolean clickMenuButton(@NotNull Player player, int buttonId) {
        if (marine == null || !marine.isLeader(player)) {
            return false;
        }

        if (buttonId == BUTTON_TOGGLE_MODE) {
            // Toggling follow/hold stands a sentry down: the three are one setting, not a mode plus a flag.
            marine.setMode(marine.getMode() == MarineMode.FOLLOW ? MarineMode.HOLD : MarineMode.FOLLOW);

            return true;
        }

        if (buttonId == BUTTON_TOGGLE_SENTRY) {
            if (marine.isSentry()) {
                marine.endSentry();
            } else {
                // Posted where it stands, so a player positions a sentry by walking it there.
                marine.beginSentry();
            }

            return true;
        }

        if (buttonId == BUTTON_END_CONTRACT) {
            // Refused while the pack holds anything the player put there, which would otherwise walk away with the
            // marine. Re-checked here rather than trusting the client's greyed-out button.
            if (marine.carriesPlayerItems()) {
                return false;
            }

            marine.endContract();

            return true;
        }

        return false;
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int slotIndex) {
        var slot = slots.get(slotIndex);

        // mayPickup is what makes shift-click respect the lock. Without this check the transfer would happily empty a
        // slot the player is not allowed to touch, because moveItemStackTo only consults the DESTINATION slots.
        if (!slot.hasItem() || !slot.mayPickup(player)) {
            return ItemStack.EMPTY;
        }

        var itemStack = slot.getItem();
        var originalItemStack = itemStack.copy();

        if (slotIndex < MARINE_SLOT_COUNT) {
            if (!moveItemStackTo(itemStack, MARINE_SLOT_COUNT, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(itemStack, 0, MARINE_SLOT_COUNT, false)) {
            return ItemStack.EMPTY;
        }

        if (itemStack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }

        return originalItemStack;
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return container.stillValid(player);
    }

    @Override
    public void removed(@NotNull Player player) {
        super.removed(player);

        container.stopOpen(player);

        if (marine != null) {
            marine.setContainerOpen(false);
        }
    }

    /**
     * Registers EVERY pack slot, including the rows that start out scrolled off the window.
     * <p>
     * That is the whole point of the design: a menu slot index is bound to one inventory index for the life of the
     * screen, so a click can never be resolved against a scroll position the server has not caught up with.
     */
    private void addMarineSlots(Container container) {
        for (var row = 0; row < MARINE_ROW_COUNT; row++) {
            for (var column = 0; column < COLUMN_COUNT; column++) {
                var slot = new MarineInventorySlot(
                    container,
                    column + row * COLUMN_COUNT,
                    row,
                    SLOTS_X + column * SLOT_SIZE,
                    MARINE_SLOTS_Y + row * SLOT_SIZE
                );

                addSlot(slot);
            }
        }
    }

    private void addPlayerInventorySlots(Inventory playerInventory) {
        for (var row = 0; row < 3; row++) {
            for (var column = 0; column < COLUMN_COUNT; column++) {
                addSlot(
                    new Slot(
                        playerInventory,
                        column + row * COLUMN_COUNT + COLUMN_COUNT,
                        SLOTS_X + column * SLOT_SIZE,
                        PLAYER_INVENTORY_Y + row * SLOT_SIZE
                    )
                );
            }
        }
    }

    private void addPlayerHotbarSlots(Inventory playerInventory) {
        for (var column = 0; column < COLUMN_COUNT; column++) {
            addSlot(new Slot(playerInventory, column, SLOTS_X + column * SLOT_SIZE, PLAYER_HOTBAR_Y));
        }
    }
}
