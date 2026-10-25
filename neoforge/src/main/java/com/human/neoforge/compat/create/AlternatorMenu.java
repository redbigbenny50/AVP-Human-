package com.human.neoforge.compat.create;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
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

/** The alternator's reading screen: its numbers, and the four card slots. */
public class AlternatorMenu extends AbstractContainerMenu {

    public static final int SPEED = 0;

    public static final int LOAD = 1;

    public static final int OUTPUT = 3;

    public static final int STORED = 5;

    public static final int CAPACITY = 7;

    public static final int STATUS = 9;

    public static final int MODE = 10;

    public static final int HAS_REDSTONE = 11;

    public static final int DATA_SIZE = 12;

    public static final int STATUS_STOPPED = 0;

    public static final int STATUS_GENERATING = 1;

    public static final int STATUS_OVERSTRESSED = 2;

    public static final int STATUS_REDSTONE = 3;

    /** clickMenuButton id: cycle the redstone card's mode. */
    public static final int BUTTON_REDSTONE = 0;

    /** Where the 2x2 card slots sit (item corner), matching the screen texture. */
    public static final int CARDS_X = 127;

    public static final int CARDS_Y = 23;

    private final Container upgrades;

    private final ContainerData data;

    private final BlockPos pos;

    /** Client side, from the open packet. */
    public AlternatorMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(
            id,
            inventory,
            new SimpleContainer(AlternatorBlockEntity.UPGRADE_SLOTS),
            new SimpleContainerData(DATA_SIZE),
            buf.readBlockPos()
        );
    }

    /** Server side. */
    public AlternatorMenu(int id, Inventory inventory, AlternatorBlockEntity alternator) {
        this(id, inventory, alternator.getUpgrades(), alternator.menuData(), alternator.getBlockPos());
    }

    private AlternatorMenu(int id, Inventory inventory, Container upgrades, ContainerData data, BlockPos pos) {
        super(HumanCreate.ALTERNATOR_MENU.get(), id);
        this.upgrades = upgrades;
        this.data = data;
        this.pos = pos;

        for (int slot = 0; slot < AlternatorBlockEntity.UPGRADE_SLOTS; slot++) {
            addSlot(new Slot(upgrades, slot, CARDS_X + (slot % 2) * 18, CARDS_Y + (slot / 2) * 18) {

                @Override
                public boolean mayPlace(@NotNull ItemStack stack) {
                    return AlternatorBlockEntity.acceptsUpgrade(stack);
                }

                @Override
                public int getMaxStackSize() {
                    return 1;
                }
            });
        }

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9, 8 + column * 18, 84 + row * 18));
            }
        }

        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, 8 + column * 18, 142));
        }

        addDataSlots(data);
    }

    public long get(int index) {
        return switch (index) {
            case LOAD, OUTPUT, STORED, CAPACITY -> ConverterData.read(data, index);
            default -> data.get(index);
        };
    }

    @Override
    public boolean clickMenuButton(@NotNull Player player, int id) {
        if (id == BUTTON_REDSTONE && player.level().getBlockEntity(pos) instanceof AlternatorBlockEntity alternator) {
            alternator.cycleRedstoneMode();
            return true;
        }

        return false;
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
        var slot = slots.get(index);

        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        var stack = slot.getItem();
        var copy = stack.copy();
        var cards = AlternatorBlockEntity.UPGRADE_SLOTS;

        if (index < cards) {
            if (!moveItemStackTo(stack, cards, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (AlternatorBlockEntity.acceptsUpgrade(stack)) {
            if (!moveItemStackTo(stack, 0, cards, false)) {
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
        return player.level().getBlockEntity(pos) instanceof AlternatorBlockEntity
            && player.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) <= 64.0D;
    }
}
