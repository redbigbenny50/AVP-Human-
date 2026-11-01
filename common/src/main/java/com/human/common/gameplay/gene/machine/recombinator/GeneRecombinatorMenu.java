package com.human.common.gameplay.gene.machine.recombinator;

import com.human.common.registry.init.HumanGeneRecombinator;
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
 * The Gene Recombinator screen's menu (approved gene_recombinator_ingame_scale3.png, 384x330, Oct 28). Slots at the
 * mockup's cells: the LEFT arm, the SAMPLE cradle, the RIGHT arm, EMPTIES BACK (take only) and the inventory.
 * <p>
 * NETWORK STORAGE: once a second while open, the tubes in Gene Storage on this grid (ISOLATED ones excluded) are sent
 * to the player if they changed; clicking one sends back its index and the tube is loaded, re-checked server side.
 * </p>
 */
public class GeneRecombinatorMenu extends AbstractContainerMenu implements com.human.common.gameplay.gene.machine.storage.NetworkTubeList.Menu {

    public static final int SCREEN_WIDTH = 384;

    public static final int SCREEN_HEIGHT = 330;

    public static final int BUTTON_START_STOP = 0;

    public static final int BUTTON_AUTO = 1;

    public static final int BUTTON_PULL = 2;

    public static final int DATA_PROGRESS = 0;

    public static final int DATA_RUNNING = 1;

    public static final int DATA_SUPPLIED = 2;

    public static final int DATA_AUTO = 3;

    public static final int DATA_PULL = 4;

    /** -1 ready, 0-3 a GeneRecombination refusal, 4 empties full, 5 no power. */
    public static final int DATA_BLOCKER = 5;

    private static final int DATA_COUNT = 6;

    private static final int MACHINE_SLOTS = GeneRecombinatorBlockEntity.SLOT_COUNT;

    private final Container items;

    private final ContainerData data;

    private final @Nullable GeneRecombinatorBlockEntity machine;

    private final Player player;

    private final com.human.common.gameplay.gene.machine.storage.NetworkTubeList network =
        new com.human.common.gameplay.gene.machine.storage.NetworkTubeList();

    private int refreshTimer;

    public GeneRecombinatorMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, new SimpleContainer(MACHINE_SLOTS), new SimpleContainerData(DATA_COUNT), null);
    }

    public GeneRecombinatorMenu(int containerId, Inventory inventory, GeneRecombinatorBlockEntity machine) {
        this(containerId, inventory, machine.items(), null, machine);
    }

    private GeneRecombinatorMenu(
        int containerId,
        Inventory inventory,
        Container items,
        @Nullable ContainerData clientData,
        @Nullable GeneRecombinatorBlockEntity machine
    ) {
        super(HumanGeneRecombinator.MENU.get(), containerId);
        this.items = items;
        this.machine = machine;
        this.player = inventory.player;
        this.data = clientData != null ? clientData : new ServerData();

        addSlot(tubeSlot(GeneRecombinatorBlockEntity.LEFT_SLOT, 15, 61));
        addSlot(tubeSlot(GeneRecombinatorBlockEntity.SAMPLE_SLOT, 85, 61));
        addSlot(tubeSlot(GeneRecombinatorBlockEntity.RIGHT_SLOT, 155, 61));
        addSlot(new Slot(items, GeneRecombinatorBlockEntity.EMPTIES_SLOT, 165, 164) {

            @Override
            public boolean mayPlace(@NotNull ItemStack stack) {
                return false;
            }
        });
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9, 11 + column * 18, 219 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, 11 + column * 18, 277));
        }
        addDataSlots(data);
    }

    private Slot tubeSlot(int index, int x, int y) {
        return new Slot(items, index, x, y) {

            @Override
            public boolean mayPlace(@NotNull ItemStack stack) {
                return GeneRecombinatorBlockEntity.isFilledTube(stack);
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        };
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
            if (machine == null) {
                return 0;
            }
            return switch (index) {
                case DATA_PROGRESS -> machine.progress();
                case DATA_RUNNING -> machine.isRunning() ? 1 : 0;
                case DATA_SUPPLIED -> machine.isSupplied() ? 1 : 0;
                case DATA_AUTO -> machine.isAutoStart() ? 1 : 0;
                case DATA_PULL -> machine.isPull() ? 1 : 0;
                case DATA_BLOCKER -> {
                    var blocker = machine.blocker();
                    yield blocker == null ? -1 : switch (blocker) {
                        case "MISSING" -> 0;
                        case "WRONG_GENE" -> 1;
                        case "DOES_NOT_STACK" -> 2;
                        case "AT_CAP" -> 3;
                        case "EMPTIES_FULL" -> 4;
                        default -> 5;
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
            sendNetwork();
        }
        super.broadcastChanges();
    }

    private void sendNetwork() {
        if (machine != null && machine.getLevel() instanceof ServerLevel level && player instanceof ServerPlayer serverPlayer) {
            network.refresh(level, machine.getBlockPos(), serverPlayer);
        }
    }

    /** A click on a listed tube; the tube is re-checked by the machine. */
    @Override
    public void pull(int index) {
        var storage = network.storage(index);
        if (machine == null || storage == null) {
            return;
        }
        machine.pullFrom(storage, network.slot(index));
        network.invalidate();
        refreshTimer = 0;
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
            case BUTTON_PULL -> machine.setPull(!machine.isPull());
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
        } else if (GeneRecombinatorBlockEntity.isFilledTube(stack)) {
            // Sample first (menu slot 1), then the arms (0 and 2) - one tube each.
            var moved = false;
            for (var target : new int[] { 1, 0, 2 }) {
                var targetSlot = slots.get(target);
                if (!targetSlot.hasItem()) {
                    targetSlot.setByPlayer(stack.split(1));
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
