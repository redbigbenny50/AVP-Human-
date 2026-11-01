package com.human.common.gameplay.gene.machine.separator;

import com.human.common.registry.init.HumanGeneSeparator;
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
 * The Gene Separator screen's menu (approved gene_separator_ingame_scale3.png, 384x324, Oct 28). Slots sit at the
 * mockup's cells: the sample, the 9-slot tube bay, the 27-slot output (take only) and the player's inventory.
 */
public class GeneSeparatorMenu extends AbstractContainerMenu {

    public static final int SCREEN_WIDTH = 384;

    public static final int SCREEN_HEIGHT = 324;

    public static final int BUTTON_NETWORK = 0;

    public static final int BUTTON_AUTO = 1;

    public static final int BUTTON_START_STOP = 2;

    public static final int DATA_PROGRESS = 0;

    public static final int DATA_RUN_TICKS = 1;

    public static final int DATA_RUNNING = 2;

    public static final int DATA_SUPPLIED = 3;

    public static final int DATA_NETWORK = 4;

    public static final int DATA_AUTO = 5;

    public static final int DATA_NETWORK_FULL = 6;

    /** -1 when a run can start, else the {@link GeneSeparatorBlockEntity.Blocker} ordinal. */
    public static final int DATA_BLOCKER = 7;

    private static final int DATA_COUNT = 8;

    private static final int MACHINE_SLOTS = GeneSeparatorBlockEntity.SLOT_COUNT;

    private final Container items;

    private final ContainerData data;

    private final @Nullable GeneSeparatorBlockEntity separator;

    public GeneSeparatorMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, new SimpleContainer(MACHINE_SLOTS), new SimpleContainerData(DATA_COUNT), null);
    }

    public GeneSeparatorMenu(int containerId, Inventory inventory, GeneSeparatorBlockEntity separator) {
        this(containerId, inventory, separator.items(), null, separator);
    }

    private GeneSeparatorMenu(
        int containerId,
        Inventory inventory,
        Container items,
        @Nullable ContainerData clientData,
        @Nullable GeneSeparatorBlockEntity separator
    ) {
        super(HumanGeneSeparator.MENU.get(), containerId);
        this.items = items;
        this.separator = separator;
        this.data = clientData != null ? clientData : new ServerData();

        addSlot(new Slot(items, GeneSeparatorBlockEntity.SAMPLE_SLOT, 13, 59) {

            @Override
            public boolean mayPlace(@NotNull ItemStack stack) {
                return GeneSeparatorBlockEntity.acceptsSample(stack);
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });
        for (int i = 0; i < GeneSeparatorBlockEntity.BAY_SLOTS; i++) {
            addSlot(new Slot(items, GeneSeparatorBlockEntity.BAY_START + i, 11 + i * 18, 93) {

                @Override
                public boolean mayPlace(@NotNull ItemStack stack) {
                    return GeneSeparatorBlockEntity.isEmptyTube(stack);
                }
            });
        }
        for (int i = 0; i < GeneSeparatorBlockEntity.OUTPUT_SLOTS; i++) {
            addSlot(new Slot(items, GeneSeparatorBlockEntity.OUTPUT_START + i, 195 + (i % 9) * 18, 57 + (i / 9) * 18) {

                @Override
                public boolean mayPlace(@NotNull ItemStack stack) {
                    return false;
                }
            });
        }
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9, 11 + column * 18, 227 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, 11 + column * 18, 285));
        }
        addDataSlots(data);
        if (separator != null) {
            separator.startViewing();
        }
    }

    public Container items() {
        return items;
    }

    public int data(int index) {
        return data.get(index);
    }

    private final class ServerData implements ContainerData {

        @Override
        public int get(int index) {
            if (separator == null) {
                return 0;
            }
            return switch (index) {
                case DATA_PROGRESS -> separator.progress();
                case DATA_RUN_TICKS -> separator.runTicks();
                case DATA_RUNNING -> separator.isRunning() ? 1 : 0;
                case DATA_SUPPLIED -> separator.isSupplied() ? 1 : 0;
                case DATA_NETWORK -> separator.isToNetwork() ? 1 : 0;
                case DATA_AUTO -> separator.isAutoStart() ? 1 : 0;
                case DATA_NETWORK_FULL -> separator.isNetworkFull() ? 1 : 0;
                case DATA_BLOCKER -> {
                    var blocker = separator.blocker();
                    yield blocker == null ? -1 : blocker.ordinal();
                }
                default -> 0;
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
        if (separator == null) {
            return false;
        }
        switch (id) {
            case BUTTON_NETWORK -> separator.setToNetwork(!separator.isToNetwork());
            case BUTTON_AUTO -> separator.setAutoStart(!separator.isAutoStart());
            case BUTTON_START_STOP -> {
                if (separator.isRunning()) {
                    separator.stop();
                } else {
                    separator.start();
                }
            }
            default -> {
                return false;
            }
        }
        return true;
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
        var slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        var stack = slot.getItem();
        var copy = stack.copy();
        if (index < MACHINE_SLOTS) {
            if (!moveItemStackTo(stack, MACHINE_SLOTS, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (GeneSeparatorBlockEntity.acceptsSample(stack)) {
            if (!moveItemStackTo(stack, GeneSeparatorBlockEntity.SAMPLE_SLOT, GeneSeparatorBlockEntity.SAMPLE_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (GeneSeparatorBlockEntity.isEmptyTube(stack)) {
            if (
                !moveItemStackTo(
                    stack,
                    GeneSeparatorBlockEntity.BAY_START,
                    GeneSeparatorBlockEntity.BAY_START + GeneSeparatorBlockEntity.BAY_SLOTS,
                    false
                )
            ) {
                return ItemStack.EMPTY;
            }
        } else {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return copy;
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return separator == null || !separator.isRemoved() && player.distanceToSqr(separator.getBlockPos().getCenter()) <= 64.0D;
    }

    @Override
    public void removed(@NotNull Player player) {
        super.removed(player);
        if (separator != null) {
            separator.stopViewing();
        }
    }
}
