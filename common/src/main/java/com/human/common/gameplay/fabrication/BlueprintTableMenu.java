package com.human.common.gameplay.fabrication;

import com.human.common.registry.init.HumanBlocks;
import com.human.common.registry.init.HumanFabrication;
import com.human.common.registry.init.item.HumanItems;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * The blueprint table's screen: the item to scan, the raw silicon, and the chip that comes out.
 * <p>
 * The scan happens only on {@link #SCAN_BUTTON}, which the screen sends after its two-press confirm. The server checks
 * everything again here and never trusts the client: an item that may not be scanned is never touched.
 */
public class BlueprintTableMenu extends AbstractContainerMenu {

    public static final int SCAN_BUTTON = 0;

    public static final int ITEM_SLOT = 0;

    public static final int SILICON_SLOT = 1;

    public static final int OUTPUT_SLOT = 2;

    private static final int PLAYER_START = 3;

    private final ContainerLevelAccess access;

    private final SimpleContainer inputs = new SimpleContainer(2) {

        @Override
        public void setChanged() {
            super.setChanged();
            slotsChanged(this);
        }
    };

    private final SimpleContainer output = new SimpleContainer(1);

    private final Player player;

    public BlueprintTableMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, ContainerLevelAccess.NULL);
    }

    public BlueprintTableMenu(int containerId, Inventory inventory, ContainerLevelAccess access) {
        super(HumanFabrication.BLUEPRINT_TABLE.get(), containerId);
        this.access = access;
        this.player = inventory.player;

        addSlot(new Slot(inputs, ITEM_SLOT, 26, 25) {

            @Override
            public int getMaxStackSize() {
                return 1;
            }

            // [stated] Oct 16: raw silicon only in the silicon slot; faction licenses never go in.
            @Override
            public boolean mayPlace(@NotNull ItemStack stack) {
                return BlueprintScanning.mayInsert(stack);
            }
        });
        addSlot(new Slot(inputs, SILICON_SLOT, 26, 51) {

            @Override
            public boolean mayPlace(@NotNull ItemStack stack) {
                return stack.is(HumanItems.SILICON.get());
            }
        });
        addSlot(new Slot(output, 0, 134, 38) {

            @Override
            public boolean mayPlace(@NotNull ItemStack stack) {
                return false;
            }
        });

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9, 8 + column * 18, 84 + row * 18));
            }
        }

        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, 8 + column * 18, 142));
        }
    }

    /** Everything the scan needs, checked the same way on both sides. */
    public BlueprintScanning.Result status() {
        var item = inputs.getItem(ITEM_SLOT);
        var result = BlueprintScanning.check(player.level(), item);

        if (result != BlueprintScanning.Result.OK) {
            return result;
        }

        var silicon = inputs.getItem(SILICON_SLOT);
        var have = silicon.is(HumanItems.SILICON.get()) ? silicon.getCount() : 0;

        // [stated] Oct 16: a chip without its two silicon says NOT ENOUGH SILICON (even with none in).
        if (isCopy() && have < BlueprintScanning.COPY_SILICON) {
            return BlueprintScanning.Result.NOT_ENOUGH_SILICON;
        }

        if (have == 0) {
            return BlueprintScanning.Result.NO_SILICON;
        }

        var made = made();
        var existing = output.getItem(0);

        if (
            !existing.isEmpty() && (!ItemStack.isSameItemSameComponents(existing, made) || existing.getCount() + made.getCount() > existing
                .getMaxStackSize())
        ) {
            return BlueprintScanning.Result.OUTPUT_FULL;
        }

        return BlueprintScanning.Result.OK;
    }

    /** A written chip in the item slot: the button copies it rather than scanning. Checked the same on both sides. */
    public boolean isCopy() {
        return BlueprintScanning.isCopy(inputs.getItem(ITEM_SLOT));
    }

    /** What one press puts in the output: one new chip from a scan, or two of the chip from a copy. */
    private ItemStack made() {
        var item = inputs.getItem(ITEM_SLOT);

        return isCopy() ? item.copyWithCount(BlueprintScanning.COPY_COUNT) : BlueprintScanning.chipFor(item.getItem());
    }

    @Override
    public boolean clickMenuButton(@NotNull Player player, int id) {
        if (id != SCAN_BUTTON || status() != BlueprintScanning.Result.OK) {
            return false;
        }

        var copy = isCopy();
        var made = made();
        var existing = output.getItem(0);

        // A scan: one item and one silicon make one chip. A copy: the chip and two silicon make two of that chip.
        inputs.removeItem(ITEM_SLOT, 1);
        inputs.removeItem(SILICON_SLOT, copy ? BlueprintScanning.COPY_SILICON : 1);

        if (existing.isEmpty()) {
            output.setItem(0, made);
        } else {
            existing.grow(made.getCount());
            output.setChanged();
        }

        broadcastChanges();

        return true;
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
        var slot = slots.get(index);

        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        var stack = slot.getItem();
        var original = stack.copy();

        if (index < PLAYER_START) {
            if (!moveItemStackTo(stack, PLAYER_START, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (stack.is(HumanItems.SILICON.get())) {
            if (!moveItemStackTo(stack, SILICON_SLOT, SILICON_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (!BlueprintScanning.mayInsert(stack) || !moveItemStackTo(stack, ITEM_SLOT, ITEM_SLOT + 1, false)) {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }

        if (stack.getCount() == original.getCount()) {
            return ItemStack.EMPTY;
        }

        slot.onTake(player, stack);

        return original;
    }

    @Override
    public void removed(@NotNull Player player) {
        super.removed(player);
        // Nothing stays in the table - everything goes back to the player, like a stonecutter.
        access.execute((level, pos) -> {
            clearContainer(player, inputs);
            clearContainer(player, output);
        });
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return stillValid(access, player, HumanBlocks.BLUEPRINT_BLOCK.get());
    }
}
