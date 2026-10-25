package com.human.neoforge.compat.create;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

/** The electric gearbox's reading screen. No slots - its speed is set on the block's front with Create's value box. */
public class ElectricGearboxMenu extends AbstractContainerMenu {

    /** Set RPM, stored +256 so a reversed (negative) setting survives the unsigned trip. */
    public static final int RPM = 0;

    public static final int DRAW = 1;

    public static final int SUPPLY = 3;

    public static final int STORED = 5;

    public static final int CAPACITY = 7;

    public static final int STATUS = 9;

    public static final int DATA_SIZE = 10;

    public static final int STATUS_OFF = 0;

    public static final int STATUS_RUNNING = 1;

    public static final int STATUS_NO_POWER = 2;

    public static final int STATUS_OVERSTRESSED = 3;

    private final ContainerData data;

    private final BlockPos pos;

    public ElectricGearboxMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(id, new SimpleContainerData(DATA_SIZE), buf.readBlockPos());
    }

    public ElectricGearboxMenu(int id, Inventory inventory, ElectricGearboxBlockEntity gearbox) {
        this(id, gearbox.menuData(), gearbox.getBlockPos());
    }

    private ElectricGearboxMenu(int id, ContainerData data, BlockPos pos) {
        super(HumanCreate.GEARBOX_MENU.get(), id);
        this.data = data;
        this.pos = pos;
        addDataSlots(data);
    }

    public long get(int index) {
        return switch (index) {
            case RPM -> data.get(RPM) - 256L;
            case DRAW, SUPPLY, STORED, CAPACITY -> ConverterData.read(data, index);
            default -> data.get(index);
        };
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return player.level().getBlockEntity(pos) instanceof ElectricGearboxBlockEntity
            && player.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) <= 64.0D;
    }
}
