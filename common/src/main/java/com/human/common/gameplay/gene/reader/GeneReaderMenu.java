package com.human.common.gameplay.gene.reader;

import com.human.common.gameplay.item.SyringeItem;
import com.human.common.registry.init.HumanGeneReader;
import com.human.common.registry.init.item.HumanItems;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * The gene reader screen's container: the player's inventory and one syringe slot. Slot positions are the approved
 * true-scale mockup's (gene_reader_ingame_scale3.png): inventory 3x9 at (11, 218), hotbar at (11, 276), syringe at
 * (187, 220). The reader itself, in the hotbar, is locked in place while the screen is open. The syringe slot is the
 * screen's own; a syringe left in it goes back to the player when the screen closes.
 */
public class GeneReaderMenu extends AbstractContainerMenu {

    public static final int SYRINGE_SLOT = 36;

    public static final int BUTTON_CLEAR_SAMPLE = 0;

    public static final int MAX_LABEL = 20;

    private final Container syringe = new SimpleContainer(1);

    private final Player player;

    /** Hotbar index of the reader the screen was opened with, or -1 (off hand). */
    private final int lockedHotbarSlot;

    public GeneReaderMenu(int containerId, Inventory inventory) {
        super(HumanGeneReader.MENU.get(), containerId);
        this.player = inventory.player;
        this.lockedHotbarSlot = inventory.getSelected().is(HumanItems.GENE_READER.get()) ? inventory.selected : -1;

        for (var row = 0; row < 3; row++) {
            for (var column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, 9 + column + row * 9, 11 + column * 18, 218 + row * 18));
            }
        }

        for (var column = 0; column < 9; column++) {
            var locked = column == lockedHotbarSlot;
            addSlot(new Slot(inventory, column, 11 + column * 18, 276) {

                @Override
                public boolean mayPickup(@NotNull Player player) {
                    return !locked && super.mayPickup(player);
                }

                @Override
                public boolean mayPlace(@NotNull ItemStack stack) {
                    return !locked && super.mayPlace(stack);
                }
            });
        }

        addSlot(new Slot(syringe, 0, 187, 220) {

            @Override
            public boolean mayPlace(@NotNull ItemStack stack) {
                return stack.is(HumanItems.SYRINGE.get());
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });
    }

    public int lockedHotbarSlot() {
        return lockedHotbarSlot;
    }

    public ItemStack syringe() {
        return syringe.getItem(0);
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return player.getMainHandItem().is(HumanItems.GENE_READER.get())
            || player.getOffhandItem().is(HumanItems.GENE_READER.get());
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
        var slot = slots.get(index);

        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        var stack = slot.getItem();
        var copy = stack.copy();

        if (index == SYRINGE_SLOT) {
            if (!moveItemStackTo(stack, 0, SYRINGE_SLOT, true)) {
                return ItemStack.EMPTY;
            }
        } else if (stack.is(HumanItems.SYRINGE.get()) && !slots.get(SYRINGE_SLOT).hasItem()) {
            if (!moveItemStackTo(stack, SYRINGE_SLOT, SYRINGE_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (index < 27) {
            if (!moveItemStackTo(stack, 27, 36, false)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, 0, 27, false)) {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }

        return copy;
    }

    /** CLEAR SAMPLE: empties the syringe in the slot. */
    @Override
    public boolean clickMenuButton(@NotNull Player player, int id) {
        if (id == BUTTON_CLEAR_SAMPLE && !syringe().isEmpty()) {
            SyringeItem.emptySyringe(syringe());
            syringe.setChanged();
            broadcastChanges();
            return true;
        }
        return false;
    }

    /** The LABEL field: names the syringe in the slot (up to {@value #MAX_LABEL} characters; blank clears the name). */
    public void setLabel(String label) {
        var stack = syringe();

        if (stack.isEmpty()) {
            return;
        }

        var text = label == null ? "" : label.strip();
        if (text.length() > MAX_LABEL) {
            text = text.substring(0, MAX_LABEL);
        }

        if (text.isEmpty()) {
            stack.remove(DataComponents.CUSTOM_NAME);
        } else {
            stack.set(DataComponents.CUSTOM_NAME, Component.literal(text));
        }

        syringe.setChanged();
        broadcastChanges();
    }

    @Override
    public void removed(@NotNull Player player) {
        super.removed(player);
        clearContainer(player, syringe);
    }
}
