package com.human.common.gameplay.storage;

import com.human.Human;
import com.human.common.network.packet.S2CCrateContentsPayload;
import com.human.common.registry.init.HumanCrates;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * The crate screen's menu. It holds only the player's own inventory as real slots; the crate's contents are not slots
 * on the screen but a condensed list (each distinct item once, with its total), sent by the server whenever the crate
 * changes. Taking and inserting go through {@link #take} and {@link #insertCarried}.
 */
public class CrateMenu extends AbstractContainerMenu {

    public static final int PLAYER_INVENTORY_X = 17;

    public static final int PLAYER_INVENTORY_Y = 140;

    public static final byte TAKE_STACK = 0;

    public static final byte TAKE_HALF = 1;

    public static final byte TAKE_TO_INVENTORY = 2;

    private final @Nullable CrateBlockEntity crate;

    private final Player player;

    private int sentVersion = -1;

    private int sentTick = -RESEND_INTERVAL_START;

    /** While pipes are filling a crate the list changes every tick; resend at most this often. */
    private static final int RESEND_INTERVAL = 4;

    /** Far enough back that the first list always goes out immediately, without overflowing the subtraction. */
    private static final int RESEND_INTERVAL_START = 1_000_000;

    // Client-side copy of the crate's list, filled by S2CCrateContentsPayload.
    private List<CrateInventory.Entry> clientEntries = List.of();

    private int clientUsedSlots;

    private int clientTotalSlots;

    private int clientRevision;

    /** Client constructor (from the menu type). */
    public CrateMenu(int id, Inventory playerInventory) {
        this(id, playerInventory, null);
    }

    public CrateMenu(int id, Inventory playerInventory, @Nullable CrateBlockEntity crate) {
        super(HumanCrates.CRATE_MENU.get(), id);
        this.crate = crate;
        this.player = playerInventory.player;

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(playerInventory, column + row * 9 + 9, PLAYER_INVENTORY_X + column * 18, PLAYER_INVENTORY_Y + row * 18));
            }
        }

        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(playerInventory, column, PLAYER_INVENTORY_X + column * 18, PLAYER_INVENTORY_Y + 58));
        }
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();

        if (crate != null && player instanceof ServerPlayer serverPlayer) {
            var inventory = crate.inventory();

            var now = serverPlayer.tickCount;

            if (inventory.version() != sentVersion && now - sentTick >= RESEND_INTERVAL) {
                sentVersion = inventory.version();
                sentTick = now;
                Human.MOD.networking()
                    .sendToClient(
                        serverPlayer,
                        new S2CCrateContentsPayload(containerId, inventory.usedSlots(), inventory.size(), inventory.summarize())
                    );
            }
        }
    }

    /** Shift-clicking a stack in the player's inventory sends it into the crate. */
    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
        if (crate == null || index < 0 || index >= slots.size()) {
            return ItemStack.EMPTY;
        }

        var slot = slots.get(index);
        var stack = slot.getItem();

        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }

        var moved = crate.inventory().insert(stack, false);

        if (moved > 0) {
            stack.shrink(moved);
            slot.setChanged();
        }

        // Always EMPTY: the vanilla quick-move loop repeats while this returns the same item, and one pass is all
        // a crate needs.
        return ItemStack.EMPTY;
    }

    /** Server: takes from the crate - a stack or half a stack onto the cursor, or a stack into the inventory. */
    public void take(ItemStack prototype, byte action) {
        if (crate == null || prototype.isEmpty()) {
            return;
        }

        var inventory = crate.inventory();
        var stackSize = prototype.getMaxStackSize();

        if (action == TAKE_TO_INVENTORY) {
            var taken = inventory.extract(prototype, stackSize, false);

            if (!taken.isEmpty()) {
                player.getInventory().add(taken);

                if (!taken.isEmpty()) {
                    inventory.insert(taken, false);
                }
            }

            return;
        }

        var carried = getCarried();

        if (!carried.isEmpty() && !ItemStack.isSameItemSameComponents(carried, prototype)) {
            return;
        }

        var room = stackSize - carried.getCount();
        var available = (int) Math.min(Integer.MAX_VALUE, inventory.count(prototype));
        var wanted = action == TAKE_HALF ? (Math.min(stackSize, available) + 1) / 2 : stackSize;
        var amount = Math.min(room, wanted);

        if (amount <= 0) {
            return;
        }

        var taken = inventory.extract(prototype, amount, false);

        if (taken.isEmpty()) {
            return;
        }

        if (carried.isEmpty()) {
            setCarried(taken);
        } else {
            carried.grow(taken.getCount());
        }
    }

    /** Server: puts the cursor stack (or one of it) into the crate. */
    public void insertCarried(boolean single) {
        if (crate == null) {
            return;
        }

        var carried = getCarried();

        if (carried.isEmpty()) {
            return;
        }

        var offered = single ? carried.copyWithCount(1) : carried;
        var moved = crate.inventory().insert(offered, false);

        carried.shrink(moved);
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        if (crate == null) {
            return true;
        }

        if (crate.isRemoved()) {
            return false;
        }

        var reach = 8.0 + crate.type().size().largestSide();

        return player.distanceToSqr(crate.centre()) <= reach * reach;
    }

    // ---- client side ----

    public void setClientContents(int usedSlots, int totalSlots, List<CrateInventory.Entry> entries) {
        clientUsedSlots = usedSlots;
        clientTotalSlots = totalSlots;
        clientEntries = entries;
        clientRevision++;
    }

    public List<CrateInventory.Entry> clientEntries() {
        return clientEntries;
    }

    public int clientUsedSlots() {
        return clientUsedSlots;
    }

    public int clientTotalSlots() {
        return clientTotalSlots;
    }

    /** Changes whenever a new list arrives, so the screen knows to rebuild its view. */
    public int clientRevision() {
        return clientRevision;
    }
}
