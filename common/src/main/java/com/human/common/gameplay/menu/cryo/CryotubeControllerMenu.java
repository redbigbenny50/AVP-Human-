package com.human.common.gameplay.menu.cryo;

import com.human.common.gameplay.block.cryo.CryotubeControllerBlockEntity;
import com.human.common.gameplay.item.power.HubChipItem;
import com.human.common.registry.init.HumanMenuTypes;
import net.minecraft.core.BlockPos;
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
 * The CRYOLINK screen's container. [stated] Oct 2: four tabs - OVERVIEW, CRYOTUBES, SPECIMENS, DIAGNOSTICS - and only
 * DIAGNOSTICS holds item slots (backup batteries, hub chip, inventory).
 * <p>
 * The tabs are client-side; the slots exist all the time but report themselves inactive on the other tabs, which is how
 * vanilla hides a slot (no drawing, no clicking). Everything the tabs show comes from the controller's snapshot, sent
 * to whoever has this screen open; the only thing carried here is WHICH controller, as its position.
 * </p>
 */
public class CryotubeControllerMenu extends AbstractContainerMenu {

    /** Actions, as vanilla menu-button ids: base + tube index. */
    public static final int BUTTON_TOGGLE_DEACTIVATE = 100;

    public static final int BUTTON_PRIORITY_UP = 200;

    public static final int BUTTON_PRIORITY_DOWN = 300;

    public static final int GUI_WIDTH = 296;

    public static final int GUI_HEIGHT = 236;

    public static final int BATTERY_X = 14;

    public static final int BATTERY_Y = 158;

    public static final int HUB_X = 14;

    public static final int HUB_Y = 192;

    public static final int INVENTORY_X = 118;

    public static final int INVENTORY_Y = 146;

    public static final int HOTBAR_Y = 204;

    /** The controller's position, split into 16-bit halves: container data travels as shorts. */
    private static final int DATA_COUNT = 6;

    private final @Nullable CryotubeControllerBlockEntity controller;

    private final ContainerData data;

    /** Client side: set by the screen. Slots are live only on the DIAGNOSTICS tab. */
    private boolean slotsShown;

    /** Client side. */
    public CryotubeControllerMenu(int containerId, Inventory inventory) {
        this(
            containerId,
            inventory,
            null,
            new SimpleContainer(1),
            new SimpleContainer(CryotubeControllerBlockEntity.BATTERY_SLOTS),
            new SimpleContainerData(DATA_COUNT)
        );
    }

    /** Server side. */
    public CryotubeControllerMenu(int containerId, Inventory inventory, CryotubeControllerBlockEntity controller) {
        this(containerId, inventory, controller, controller.getHub(), controller.getBatteries(), null);
    }

    private CryotubeControllerMenu(
        int containerId,
        Inventory inventory,
        @Nullable CryotubeControllerBlockEntity controller,
        Container hub,
        Container batteries,
        @Nullable ContainerData clientData
    ) {
        super(HumanMenuTypes.CRYOTUBE_CONTROLLER.get(), containerId);

        this.controller = controller;
        this.data = clientData != null ? clientData : positionData(controller.getBlockPos());

        addSlot(new TabSlot(hub, 0, HUB_X, HUB_Y) {

            @Override
            public boolean mayPlace(@NotNull ItemStack stack) {
                return stack.getItem() instanceof HubChipItem;
            }
        });

        for (int slot = 0; slot < CryotubeControllerBlockEntity.BATTERY_SLOTS; slot++) {
            addSlot(new TabSlot(batteries, slot, BATTERY_X + slot * 19, BATTERY_Y) {

                @Override
                public boolean mayPlace(@NotNull ItemStack stack) {
                    return com.human.common.gameplay.power.ItemEnergyBridge.canDischarge(stack);
                }
            });
        }

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new TabSlot(inventory, column + row * 9 + 9, INVENTORY_X + column * 18, INVENTORY_Y + row * 18));
            }
        }

        for (int column = 0; column < 9; column++) {
            addSlot(new TabSlot(inventory, column, INVENTORY_X + column * 18, HOTBAR_Y));
        }

        addDataSlots(data);
    }

    /** A slot that only exists on the DIAGNOSTICS tab (always live on the server, which has no tabs). */
    private class TabSlot extends Slot {

        TabSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean isActive() {
            return controller != null || slotsShown;
        }

        @Override
        public int getMaxStackSize() {
            return container instanceof Inventory ? super.getMaxStackSize() : 1;
        }
    }

    private static ContainerData positionData(BlockPos pos) {
        return new ContainerData() {

            @Override
            public int get(int index) {
                var value = switch (index / 2) {
                    case 0 -> pos.getX();
                    case 1 -> pos.getY();
                    default -> pos.getZ();
                };

                return index % 2 == 0 ? (value >>> 16) & 0xFFFF : value & 0xFFFF;
            }

            @Override
            public void set(int index, int value) {}

            @Override
            public int getCount() {
                return DATA_COUNT;
            }
        };
    }

    /** Client: the controller's position, put back together from its halves. */
    public BlockPos controllerPos() {
        int[] parts = new int[3];

        for (int axis = 0; axis < 3; axis++) {
            parts[axis] = ((data.get(axis * 2) & 0xFFFF) << 16) | (data.get(axis * 2 + 1) & 0xFFFF);
        }

        return new BlockPos(parts[0], parts[1], parts[2]);
    }

    public void setSlotsShown(boolean shown) {
        this.slotsShown = shown;
    }

    public boolean isFor(CryotubeControllerBlockEntity controller) {
        return this.controller == controller;
    }

    @Override
    public boolean clickMenuButton(@NotNull Player player, int id) {
        if (controller == null) {
            return false;
        }

        if (id >= BUTTON_TOGGLE_DEACTIVATE && id < BUTTON_TOGGLE_DEACTIVATE + 100) {
            controller.toggleDeactivated(id - BUTTON_TOGGLE_DEACTIVATE);
            return true;
        }

        if (id >= BUTTON_PRIORITY_UP && id < BUTTON_PRIORITY_UP + 100) {
            // "Up" means more important, which is the LOWER number (P1 is kept longest).
            controller.changePriority(id - BUTTON_PRIORITY_UP, -1);
            return true;
        }

        if (id >= BUTTON_PRIORITY_DOWN && id < BUTTON_PRIORITY_DOWN + 100) {
            controller.changePriority(id - BUTTON_PRIORITY_DOWN, 1);
            return true;
        }

        return false;
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return controller == null
            || !controller.isRemoved() && player.distanceToSqr(controller.getBlockPos().getCenter()) <= 64.0;
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
        var slot = slots.get(index);

        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        var stack = slot.getItem();
        var original = stack.copy();
        var machineSlots = 1 + CryotubeControllerBlockEntity.BATTERY_SLOTS;

        if (index < machineSlots) {
            if (!moveItemStackTo(stack, machineSlots, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (stack.getItem() instanceof HubChipItem) {
            if (!moveItemStackTo(stack, 0, 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (com.human.common.gameplay.power.ItemEnergyBridge.canDischarge(stack)) {
            if (!moveItemStackTo(stack, 1, machineSlots, false)) {
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

        return original;
    }
}
