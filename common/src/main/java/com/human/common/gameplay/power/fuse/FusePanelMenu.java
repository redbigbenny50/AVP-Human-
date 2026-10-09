package com.human.common.gameplay.power.fuse;

import com.human.common.gameplay.block.entity.power.PowerConsumerBlockEntity;
import com.human.common.gameplay.machine.MachinePriority;
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
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The fuse panel's screen: its bound devices (each can be unbound here), its four upgrade slots for CAPACITY cards, and
 * its priority on the grid.
 * <h2>HOW THE CLIENT SEES THE DEVICE LIST</h2> The list lives on the panel's block entity, which publishes its tag to
 * clients on every change (as every power node does). The menu syncs only the panel's POSITION - split into 16-bit
 * halves, because menu data travels as shorts - and the screen reads the list from the client's copy of the panel.
 */
public class FusePanelMenu extends AbstractContainerMenu {

    public static final int BUTTON_CYCLE_PRIORITY = 1000;

    /**
     * [stated] Oct 2: the Priority side button sets a value directly - BUTTON_SET_PRIORITY + priority ordinal. Far
     * above the unbind ids (0 up to the device count), so the two can never meet.
     */
    public static final int BUTTON_SET_PRIORITY = 2000;

    private static boolean isSetPriority(int id) {
        return id >= BUTTON_SET_PRIORITY && id < BUTTON_SET_PRIORITY + com.human.common.gameplay.machine.MachinePriority.values().length;
    }

    public static final int UPGRADE_X = 8;

    public static final int UPGRADE_Y = 116;

    public static final int PLAYER_Y = 140;

    public static final int HOTBAR_Y = 198;

    private static final int DATA_PRIORITY = 0;

    private static final int DATA_CAPACITY = 1;

    private static final int DATA_POS = 2;

    private static final int DATA_COUNT = DATA_POS + 6;

    private static final int PLAYER_FIRST = FusePanelBlockEntity.UPGRADE_SLOTS;

    private static final int PLAYER_END = PLAYER_FIRST + 36;

    private final Container upgrades;

    private final ContainerData data;

    private final @Nullable FusePanelBlockEntity panel;

    public FusePanelMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, new SimpleContainer(FusePanelBlockEntity.UPGRADE_SLOTS), new SimpleContainerData(DATA_COUNT), null);
    }

    public FusePanelMenu(int containerId, Inventory inventory, FusePanelBlockEntity panel) {
        this(containerId, inventory, panel.getUpgrades(), serverData(panel), panel);
    }

    private FusePanelMenu(
        int containerId,
        Inventory inventory,
        Container upgrades,
        ContainerData data,
        @Nullable FusePanelBlockEntity panel
    ) {
        super(HumanMenuTypes.FUSE_PANEL.get(), containerId);
        checkContainerSize(upgrades, FusePanelBlockEntity.UPGRADE_SLOTS);
        checkContainerDataCount(data, DATA_COUNT);

        this.upgrades = upgrades;
        this.data = data;
        this.panel = panel;

        for (var slot = 0; slot < FusePanelBlockEntity.UPGRADE_SLOTS; slot++) {
            addSlot(new Slot(upgrades, slot, UPGRADE_X + slot * 18, UPGRADE_Y) {

                @Override
                public boolean mayPlace(@NotNull ItemStack stack) {
                    return FusePanelBlockEntity.isCapacityCard(stack);
                }

                @Override
                public int getMaxStackSize() {
                    return 1;
                }
            });
        }

        for (var row = 0; row < 3; row++) {
            for (var column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9, 8 + column * 18, PLAYER_Y + row * 18));
            }
        }

        for (var column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, 8 + column * 18, HOTBAR_Y));
        }

        addDataSlots(data);
    }

    private static ContainerData serverData(FusePanelBlockEntity panel) {
        return new ContainerData() {

            @Override
            public int get(int index) {
                var pos = panel.getBlockPos();

                return switch (index) {
                    case DATA_PRIORITY -> panel.getPriority().ordinal();
                    case DATA_CAPACITY -> panel.getDeviceCapacity();
                    case DATA_POS -> pos.getX() & 0xFFFF;
                    case DATA_POS + 1 -> pos.getX() >>> 16;
                    case DATA_POS + 2 -> pos.getY() & 0xFFFF;
                    case DATA_POS + 3 -> pos.getY() >>> 16;
                    case DATA_POS + 4 -> pos.getZ() & 0xFFFF;
                    case DATA_POS + 5 -> pos.getZ() >>> 16;
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {}

            @Override
            public int getCount() {
                return DATA_COUNT;
            }
        };
    }

    public MachinePriority getPriority() {
        return MachinePriority.byOrdinal(data.get(DATA_PRIORITY));
    }

    public int getCapacity() {
        return data.get(DATA_CAPACITY);
    }

    /** Rebuilt from the synced 16-bit halves. */
    public BlockPos getPanelPos() {
        return new BlockPos(join(DATA_POS), join(DATA_POS + 2), join(DATA_POS + 4));
    }

    private int join(int index) {
        return (data.get(index) & 0xFFFF) | (data.get(index + 1) & 0xFFFF) << 16;
    }

    /** The panel as the client sees it - its device list comes with its block update. */
    public @Nullable FusePanelBlockEntity clientPanel(Level level) {
        return level.getBlockEntity(getPanelPos()) instanceof FusePanelBlockEntity found ? found : null;
    }

    /** Button ids 0..n unbind that device; {@link #BUTTON_CYCLE_PRIORITY} cycles the priority. */
    @Override
    public boolean clickMenuButton(@NotNull Player player, int id) {
        if (panel == null) {
            return id == BUTTON_CYCLE_PRIORITY || isSetPriority(id) || id >= 0 && id < FusePanelBlockEntity.MAX_DEVICES_EVER;
        }

        if (id == BUTTON_CYCLE_PRIORITY) {
            panel.cyclePriority();
            return true;
        }

        if (isSetPriority(id)) {
            panel.setPriority(com.human.common.gameplay.machine.MachinePriority.values()[id - BUTTON_SET_PRIORITY]);
            return true;
        }

        if (id < 0 || id >= panel.getBoundCount()) {
            return false;
        }

        // Past the block devices come the turrets.
        if (id >= panel.getDevices().size()) {
            var bound = panel.getSentries().get(id - panel.getDevices().size());

            if (
                player.level() instanceof net.minecraft.server.level.ServerLevel serverLevel
                    && serverLevel.getEntity(bound.id()) instanceof com.human.common.gameplay.entity.machine.SentryTurret turret
            ) {
                turret.setFusePanel(null);
            }

            panel.unbindSentry(bound.id());
            return true;
        }

        var pos = panel.getDevices().get(id);
        var level = player.level();

        if (level.isLoaded(pos) && level.getBlockEntity(pos) instanceof PowerConsumerBlockEntity device) {
            device.setFusePanel(null);
        }

        panel.unbind(pos);

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

        if (index < PLAYER_FIRST) {
            if (!moveItemStackTo(stack, PLAYER_FIRST, PLAYER_END, true)) {
                return ItemStack.EMPTY;
            }
        } else if (!FusePanelBlockEntity.isCapacityCard(stack) || !moveItemStackTo(stack, 0, PLAYER_FIRST, false)) {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }

        return stack.getCount() == original.getCount() ? ItemStack.EMPTY : original;
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return panel == null || !panel.isRemoved() && player.distanceToSqr(panel.getBlockPos().getCenter()) <= 64.0D;
    }
}
