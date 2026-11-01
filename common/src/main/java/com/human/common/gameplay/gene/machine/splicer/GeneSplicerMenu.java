package com.human.common.gameplay.gene.machine.splicer;

import com.human.common.gameplay.gene.machine.storage.NetworkTubeList;
import com.human.common.registry.init.HumanGeneSplicer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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
 * The Gene Splicer screen's menu (approved gene_splicer_ingame_scale3.png, 384x334, Oct 28). Slots at the mockup's
 * cells: the five rack holders, IN (empty syringes), OUT (take only), EMPTIES BACK (take only) and the inventory.
 * NETWORK STORAGE is the shared {@link NetworkTubeList}; the LABEL travels by its own small packets.
 */
public class GeneSplicerMenu extends AbstractContainerMenu implements NetworkTubeList.Menu {

    public static final int SCREEN_WIDTH = 384;

    public static final int SCREEN_HEIGHT = 334;

    public static final int BUTTON_START_STOP = 0;

    public static final int BUTTON_AUTO = 1;

    public static final int DATA_PROGRESS = 0;

    public static final int DATA_RUN_TICKS = 1;

    public static final int DATA_RUNNING = 2;

    public static final int DATA_SUPPLIED = 3;

    public static final int DATA_AUTO = 4;

    /** -1 ready; 0-4 a GeneSplicing refusal; 5 no syringe; 6 OUT full; 7 empties full; 8 no power. */
    public static final int DATA_BLOCKER = 5;

    private static final int DATA_COUNT = 6;

    private static final int MACHINE_SLOTS = GeneSplicerBlockEntity.SLOT_COUNT;

    private final Container items;

    private final ContainerData data;

    private final @Nullable GeneSplicerBlockEntity machine;

    private final Player player;

    private final NetworkTubeList network = new NetworkTubeList();

    private int refreshTimer;

    public GeneSplicerMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, new SimpleContainer(MACHINE_SLOTS), new SimpleContainerData(DATA_COUNT), null);
    }

    public GeneSplicerMenu(int containerId, Inventory inventory, GeneSplicerBlockEntity machine) {
        this(containerId, inventory, machine.items(), null, machine);
    }

    private GeneSplicerMenu(
        int containerId,
        Inventory inventory,
        Container items,
        @Nullable ContainerData clientData,
        @Nullable GeneSplicerBlockEntity machine
    ) {
        super(HumanGeneSplicer.MENU.get(), containerId);
        this.items = items;
        this.machine = machine;
        this.player = inventory.player;
        this.data = clientData != null ? clientData : new ServerData();

        for (int k = 0; k < GeneSplicerBlockEntity.TUBE_SLOTS; k++) {
            addSlot(new Slot(items, k, 11 + k * 22, 57) {

                @Override
                public boolean mayPlace(@NotNull ItemStack stack) {
                    return com.human.common.gameplay.gene.tube.GeneTubeItem.read(stack) != null;
                }

                @Override
                public int getMaxStackSize() {
                    return 1;
                }
            });
        }
        addSlot(new Slot(items, GeneSplicerBlockEntity.IN_SLOT, 121, 57) {

            @Override
            public boolean mayPlace(@NotNull ItemStack stack) {
                return GeneSplicerBlockEntity.isEmptySyringe(stack);
            }
        });
        addSlot(takeOnly(GeneSplicerBlockEntity.OUT_SLOT, 155, 57));
        addSlot(takeOnly(GeneSplicerBlockEntity.EMPTIES_SLOT, 165, 167));
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9, 11 + column * 18, 225 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, 11 + column * 18, 283));
        }
        addDataSlots(data);
    }

    private Slot takeOnly(int index, int x, int y) {
        return new Slot(items, index, x, y) {

            @Override
            public boolean mayPlace(@NotNull ItemStack stack) {
                return false;
            }
        };
    }

    public Container items() {
        return items;
    }

    public int data(int index) {
        return data.get(index);
    }

    public void setLabel(String label) {
        if (machine != null) {
            machine.setLabel(label);
        }
    }

    public String label() {
        return machine == null ? "" : machine.label();
    }

    private final class ServerData implements ContainerData {

        @Override
        public int get(int index) {
            if (machine == null) {
                return 0;
            }
            return switch (index) {
                case DATA_PROGRESS -> machine.progress();
                case DATA_RUN_TICKS -> machine.runTicks();
                case DATA_RUNNING -> machine.isRunning() ? 1 : 0;
                case DATA_SUPPLIED -> machine.isSupplied() ? 1 : 0;
                case DATA_AUTO -> machine.isAutoStart() ? 1 : 0;
                case DATA_BLOCKER -> {
                    var blocker = machine.blocker();
                    yield blocker == null ? -1 : switch (blocker) {
                        case "EMPTY" -> 0;
                        case "TOO_MANY" -> 1;
                        case "DUPLICATE" -> 2;
                        case "SECOND_MUTATION" -> 3;
                        case "NOT_A_TUBE" -> 4;
                        case "NO_SYRINGE" -> 5;
                        case "OUT_FULL" -> 6;
                        case "EMPTIES_FULL" -> 7;
                        default -> 8;
                    };
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
    public void broadcastChanges() {
        if (machine != null && --refreshTimer <= 0) {
            refreshTimer = 20;
            if (machine.getLevel() instanceof ServerLevel level && player instanceof ServerPlayer serverPlayer) {
                network.refresh(level, machine.getBlockPos(), serverPlayer);
            }
        }
        super.broadcastChanges();
    }

    @Override
    public void pull(int index) {
        var storage = network.storage(index);
        if (machine != null && storage != null) {
            machine.pullFrom(storage, network.slot(index));
            network.invalidate();
            refreshTimer = 0;
        }
    }

    @Override
    public boolean clickMenuButton(@NotNull Player player, int id) {
        if (machine == null) {
            return false;
        }
        switch (id) {
            case BUTTON_START_STOP -> {
                if (machine.isRunning()) {
                    machine.stop();
                } else {
                    machine.start();
                }
            }
            case BUTTON_AUTO -> machine.setAutoStart(!machine.isAutoStart());
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
        } else if (GeneSplicerBlockEntity.isEmptySyringe(stack)) {
            if (!moveItemStackTo(stack, 5, 6, false)) {
                return ItemStack.EMPTY;
            }
        } else if (com.human.common.gameplay.gene.tube.GeneTubeItem.read(stack) != null) {
            var moved = false;
            for (int target = 0; target < GeneSplicerBlockEntity.TUBE_SLOTS; target++) {
                if (!slots.get(target).hasItem()) {
                    slots.get(target).setByPlayer(stack.split(1));
                    moved = true;
                    break;
                }
            }
            if (!moved) {
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
        return machine == null || !machine.isRemoved() && player.distanceToSqr(machine.getBlockPos().getCenter()) <= 64.0D;
    }
}
