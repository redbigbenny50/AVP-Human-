package com.human.common.gameplay.gene.geneticist;

import com.human.common.registry.init.HumanGeneticist;
import com.human.common.registry.init.item.HumanItems;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * The Geneticist Station's cleanser screen ([stated] Oct 29): a syringe and a cleanser go in, five seconds later a
 * cleansed syringe comes out. No power. Like the blueprint table it keeps nothing of its own - whatever is in its slots
 * goes back to the player when the screen closes, so closing early just cancels the cleanse.
 * <p>
 * The cleanse runs on the server while the screen is open: the server calls {@link #broadcastChanges()} for the open
 * menu every tick (ServerPlayer.tick), and that is where it advances. Progress reaches the screen through
 * {@link #data}.
 * </p>
 */
public class GeneticistStationMenu extends AbstractContainerMenu {

    public static final int SYRINGE_SLOT = 0;

    public static final int CLEANSER_SLOT = 1;

    public static final int OUTPUT_SLOT = 2;

    private static final int PLAYER_START = 3;

    /** Title row above the panel ([stated] Oct 29: room for the machine's name); everything below it is moved down. */
    public static final int TOP = 11;

    public enum Status {
        EMPTY,
        NEEDS_SYRINGE,
        NEEDS_CLEANSER,
        ALREADY_CLEANSED,
        OUTPUT_FULL,
        READY
    }

    private final ContainerLevelAccess access;

    private final SimpleContainer inputs = new SimpleContainer(2) {

        @Override
        public void setChanged() {
            super.setChanged();
            slotsChanged(this);
        }
    };

    private final SimpleContainer output = new SimpleContainer(1);

    /** [0] = ticks cleansed so far, [1] = 1 once a cleanse has just finished (DONE until the inputs change). */
    private final ContainerData data;

    private long lastTick = Long.MIN_VALUE;

    public GeneticistStationMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, ContainerLevelAccess.NULL);
    }

    public GeneticistStationMenu(int containerId, Inventory inventory, ContainerLevelAccess access) {
        super(HumanGeneticist.STATION_MENU.get(), containerId);
        this.access = access;
        this.data = new SimpleContainerData(2);

        addSlot(new Slot(inputs, SYRINGE_SLOT, 26, 25 + TOP) {

            @Override
            public int getMaxStackSize() {
                return 1;
            }

            @Override
            public boolean mayPlace(@NotNull ItemStack stack) {
                return stack.is(HumanItems.SYRINGE.get());
            }
        });
        addSlot(new Slot(inputs, CLEANSER_SLOT, 26, 51 + TOP) {

            @Override
            public boolean mayPlace(@NotNull ItemStack stack) {
                return stack.is(HumanGeneticist.CLEANSER.get());
            }
        });
        addSlot(new Slot(output, 0, 134, 38 + TOP) {

            @Override
            public boolean mayPlace(@NotNull ItemStack stack) {
                return false;
            }
        });

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9, 8 + column * 18, 84 + TOP + row * 18));
            }
        }

        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, 8 + column * 18, 142 + TOP));
        }

        addDataSlots(data);
    }

    public ItemStack syringe() {
        return slots.get(SYRINGE_SLOT).getItem();
    }

    public ItemStack result() {
        return slots.get(OUTPUT_SLOT).getItem();
    }

    /** Ticks done, 0 - {@link GeneCleanser#TICKS}. */
    public int progress() {
        return data.get(0);
    }

    public boolean justFinished() {
        return data.get(1) == 1;
    }

    /** Why it is (or is not) cleansing. Read from the slots, so it is the same on both sides. */
    public Status status() {
        var syringe = slots.get(SYRINGE_SLOT).getItem();
        var cleanser = slots.get(CLEANSER_SLOT).getItem();

        if (syringe.isEmpty() && cleanser.isEmpty()) {
            return Status.EMPTY;
        }
        if (syringe.isEmpty()) {
            return Status.NEEDS_SYRINGE;
        }
        // One cleanse per syringe.
        if (GeneCleanser.isCleansed(syringe)) {
            return Status.ALREADY_CLEANSED;
        }
        if (cleanser.isEmpty()) {
            return Status.NEEDS_CLEANSER;
        }
        if (!slots.get(OUTPUT_SLOT).getItem().isEmpty()) {
            return Status.OUTPUT_FULL;
        }
        return Status.READY;
    }

    @Override
    public void broadcastChanges() {
        // Server side only: advance the cleanse once per tick while the screen is open.
        if (access != ContainerLevelAccess.NULL) {
            access.execute((level, pos) -> {
                // broadcastChanges also runs after clicks; only count it once per game tick so clicking never speeds a
                // cleanse up.
                if (!level.isClientSide && level.getGameTime() != lastTick) {
                    lastTick = level.getGameTime();
                    tickCleanse();
                }
            });
        }
        super.broadcastChanges();
    }

    private void tickCleanse() {
        if (status() != Status.READY) {
            data.set(0, 0);
            return;
        }
        data.set(1, 0);
        var done = data.get(0) + 1;

        if (done < GeneCleanser.TICKS) {
            data.set(0, done);
            return;
        }

        var syringe = inputs.getItem(SYRINGE_SLOT);
        output.setItem(0, GeneCleanser.cleanse(syringe));
        inputs.removeItem(SYRINGE_SLOT, 1);
        inputs.removeItem(CLEANSER_SLOT, 1);
        data.set(0, 0);
        data.set(1, 1);
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
        } else if (stack.is(HumanItems.SYRINGE.get())) {
            if (!moveItemStackTo(stack, SYRINGE_SLOT, SYRINGE_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (stack.is(HumanGeneticist.CLEANSER.get())) {
            if (!moveItemStackTo(stack, CLEANSER_SLOT, CLEANSER_SLOT + 1, false)) {
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

        if (stack.getCount() == original.getCount()) {
            return ItemStack.EMPTY;
        }

        slot.onTake(player, stack);
        return original;
    }

    @Override
    public void removed(@NotNull Player player) {
        super.removed(player);
        // Nothing stays in the station - everything goes back to the player, like the blueprint table.
        access.execute((level, pos) -> {
            clearContainer(player, inputs);
            clearContainer(player, output);
        });
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return stillValid(access, player, HumanGeneticist.GENETICIST_STATION.get());
    }
}
