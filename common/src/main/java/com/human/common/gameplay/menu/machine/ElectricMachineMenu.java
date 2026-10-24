package com.human.common.gameplay.menu.machine;

import com.human.common.gameplay.block.entity.machine.ElectricMachineBlockEntity;
import com.human.common.gameplay.fluid.FluidContent;
import com.human.common.gameplay.machine.ElectricMachineKind;
import com.human.common.gameplay.machine.MachineFluidItems;
import com.human.common.gameplay.machine.MachinePriority;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The shared menu for every tier-1 cooking machine.
 * <h2>LAYOUT - MEASURED FROM HIS GUI ART</h2> The input and output boxes are 24px frames at (40, 31) and (112, 31), so
 * the 16px items sit at (44, 35) and (116, 35). The player inventory is at the standard (8, 84) with the hotbar at (8,
 * 142). The FILTER slots sit in the panel drawn to the LEFT of the art ([stated] Sep 29: "all machines should have a
 * filter option on the left of the gui"), at negative x - slots outside the texture are legal, the screen just draws
 * their frame itself.
 * <h2>⚠ FILTER SLOTS ARE GHOST SLOTS</h2> Clicking one COPIES a single item from the cursor into it (or clears it with
 * an empty cursor) and never moves a real item. That is handled in {@link #clicked} before vanilla's slot logic runs,
 * on both sides, so the client's prediction and the server agree without a custom packet.
 */
public class ElectricMachineMenu extends AbstractContainerMenu {

    public static final int BUTTON_CYCLE_PRIORITY = 0;

    /** [stated] readme - the press: keep or throw away the item output. */
    public static final int BUTTON_TOGGLE_KEEP_ITEMS = 1;

    /** [stated] readme - the press: keep or throw away the fluid output. */
    public static final int BUTTON_TOGGLE_KEEP_FLUID = 2;

    /** [stated] Oct 2: cycles the redstone mode - only does anything with a redstone card fitted. */
    public static final int BUTTON_CYCLE_REDSTONE = 3;

    /** [stated] Oct 2: the side-button pop-ups pick a value directly. BUTTON_SET_PRIORITY + priority ordinal. */
    public static final int BUTTON_SET_PRIORITY = 10;

    /** BUTTON_SET_REDSTONE + mode ordinal. */
    public static final int BUTTON_SET_REDSTONE = 20;

    /** BUTTON_SET_FILTER + entry: the entry becomes one copy of what the player is holding (server-side cursor). */
    public static final int BUTTON_SET_FILTER = 100;

    /** BUTTON_CLEAR_FILTER + entry. */
    public static final int BUTTON_CLEAR_FILTER = 110;

    /** [stated] Oct 2: BUTTON_SET_PORT + face * 8 + (fluid ? 4 : 0) + mode - 200 to 247. */
    public static final int BUTTON_SET_PORT = 200;

    public static int portButton(
        com.human.common.gameplay.machine.MachinePorts.Face face,
        boolean fluid,
        com.human.common.gameplay.machine.MachinePorts.Mode mode
    ) {
        return BUTTON_SET_PORT + face.ordinal() * 8 + (fluid ? 4 : 0) + mode.ordinal();
    }

    // Slot index ranges, from the machine's layout: its own slots, then the filter, then the player's inventory.
    private final int machineSlots;

    private final int filterFirst;

    private final int filterEnd;

    private final int playerFirst;

    /** [stated] Oct 2: the four upgrade slots, in the left panel below the filter (and below the press's switches). */
    private final int upgradeFirst;

    private final int upgradeEnd;

    private final int playerEnd;

    public static final int FILTER_X = -20;

    public static final int FILTER_Y = 36;

    private final ElectricMachineKind kind;

    private final Container machine;

    private final Container filter;

    private final ContainerData data;

    /** Server only: the real block entity, for the settings buttons and experience. Null on the client. */
    private final @Nullable ElectricMachineBlockEntity blockEntity;

    /** Client constructor - the menu type's factory. */
    public ElectricMachineMenu(ElectricMachineKind kind, int containerId, Inventory inventory) {
        this(
            kind,
            containerId,
            inventory,
            new SimpleContainer(kind.layout().slotCount()),
            new SimpleContainer(ElectricMachineBlockEntity.FILTER_SIZE),
            new SimpleContainer(ElectricMachineBlockEntity.UPGRADE_SLOTS),
            new SimpleContainerData(ElectricMachineBlockEntity.DATA_COUNT + kind.extraDataCount()),
            null
        );
    }

    /** Server constructor. */
    public ElectricMachineMenu(ElectricMachineKind kind, int containerId, Inventory inventory, ElectricMachineBlockEntity blockEntity) {
        this(
            kind,
            containerId,
            inventory,
            blockEntity,
            blockEntity.getFilter(),
            blockEntity.getUpgrades(),
            blockEntity.getData(),
            blockEntity
        );
    }

    private ElectricMachineMenu(
        ElectricMachineKind kind,
        int containerId,
        Inventory inventory,
        Container machine,
        Container filter,
        Container upgrades,
        ContainerData data,
        @Nullable ElectricMachineBlockEntity blockEntity
    ) {
        super(kind.menuType().get(), containerId);
        this.machineSlots = kind.layout().slotCount();
        this.filterFirst = machineSlots;
        this.filterEnd = filterFirst + ElectricMachineBlockEntity.FILTER_SIZE;
        this.upgradeFirst = filterEnd;
        this.upgradeEnd = upgradeFirst + ElectricMachineBlockEntity.UPGRADE_SLOTS;
        this.playerFirst = upgradeEnd;
        this.playerEnd = playerFirst + 36;

        checkContainerSize(machine, machineSlots);
        checkContainerSize(filter, ElectricMachineBlockEntity.FILTER_SIZE);
        checkContainerDataCount(data, ElectricMachineBlockEntity.DATA_COUNT + kind.extraDataCount());

        this.kind = kind;
        this.machine = machine;
        this.filter = filter;
        this.data = data;
        this.blockEntity = blockEntity;

        machine.startOpen(inventory.player);

        // Positions come from the machine's layout - measured from each machine's GUI art.
        var layout = kind.layout();

        // [stated] Oct 17 - the distributor's slot takes only a filled vapor tank, one at a time (tanks never stack).
        addSlot(
            kind == ElectricMachineKind.DISTRIBUTOR
                ? new VaporTankSlot(machine, ElectricMachineBlockEntity.SLOT_INPUT, layout.inputX(), layout.inputY())
                : new Slot(machine, ElectricMachineBlockEntity.SLOT_INPUT, layout.inputX(), layout.inputY())
        );

        for (var output = 0; output < layout.outputs().length; output++) {
            addSlot(
                new OutputSlot(
                    inventory.player,
                    machine,
                    ElectricMachineBlockEntity.FIRST_OUTPUT + output,
                    layout.outputs()[output][0],
                    layout.outputs()[output][1]
                )
            );
        }

        // [stated] Sep 30: the press and hydrator's fill slot, under the bucket icon. Menu index = container index.
        if (layout.hasFillSlot()) {
            addSlot(new FillSlot(machine, layout.fillSlot(), layout.fillX(), layout.fillY()));
        }

        for (var index = 0; index < ElectricMachineBlockEntity.FILTER_SIZE; index++) {
            addSlot(new GhostSlot(filter, index, FILTER_X, FILTER_Y + index * 18));
        }

        for (var index = 0; index < ElectricMachineBlockEntity.UPGRADE_SLOTS; index++) {
            addSlot(new Slot(upgrades, index, FILTER_X, upgradeSlotY(kind, index)) {

                // The same rules as the machine's own container, so the client agrees with the server.
                @Override
                public boolean mayPlace(@NotNull ItemStack stack) {
                    return com.human.common.gameplay.machine.MachineUpgrades.mayPlace(container, getContainerSlot(), stack);
                }

                @Override
                public int getMaxStackSize() {
                    return 1;
                }
            });
        }

        for (var row = 0; row < 3; row++) {
            for (var column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9, 8 + column * 18, 84 + row * 18));
            }
        }

        for (var column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, 8 + column * 18, 142));
        }

        addDataSlots(data);
    }

    public ElectricMachineKind getKind() {
        return kind;
    }

    /** A machine's own synced value (see {@link ElectricMachineKind#extraDataCount()}); 0 when out of range. */
    public int getExtraData(int index) {
        return index >= 0 && index < kind.extraDataCount() ? data.get(ElectricMachineBlockEntity.DATA_COUNT + index) : 0;
    }

    public float getProgress() {
        var total = data.get(ElectricMachineBlockEntity.DATA_TOTAL);
        var progress = data.get(ElectricMachineBlockEntity.DATA_PROGRESS);

        return total <= 0 ? 0.0F : Math.max(0.0F, Math.min(1.0F, progress / (float) total));
    }

    public int getStatus() {
        return data.get(ElectricMachineBlockEntity.DATA_STATUS);
    }

    public MachinePriority getPriority() {
        return MachinePriority.byOrdinal(data.get(ElectricMachineBlockEntity.DATA_PRIORITY));
    }

    /** [stated] Oct 2: the side panel now holds just the four upgrade slots, one above the other. */
    public static int upgradeSlotY(ElectricMachineKind kind, int index) {
        return 12 + index * 18;
    }

    public com.human.common.gameplay.machine.MachinePorts.Mode getItemPort(com.human.common.gameplay.machine.MachinePorts.Face face) {
        return com.human.common.gameplay.machine.MachinePorts.unpack(data.get(ElectricMachineBlockEntity.DATA_PORTS_ITEMS), face);
    }

    public com.human.common.gameplay.machine.MachinePorts.Mode getFluidPort(com.human.common.gameplay.machine.MachinePorts.Face face) {
        return com.human.common.gameplay.machine.MachinePorts.unpack(data.get(ElectricMachineBlockEntity.DATA_PORTS_FLUIDS), face);
    }

    /** The filter entries as last synced - for the Filter pop-up. */
    public ItemStack getFilterEntry(int index) {
        return slots.get(filterFirst + index).getItem();
    }

    public boolean isUpgradeSlot(Slot slot) {
        return slot.index >= upgradeFirst && slot.index < upgradeEnd;
    }

    public com.human.common.gameplay.machine.MachineUpgrades.RedstoneMode getRedstoneMode() {
        return com.human.common.gameplay.machine.MachineUpgrades.RedstoneMode.byOrdinal(data.get(ElectricMachineBlockEntity.DATA_REDSTONE));
    }

    /** Read from the synced slots, so the screen can show a redstone button only when the card is fitted. */
    public boolean hasRedstoneCard() {
        var container = slots.get(upgradeFirst).container;
        return com.human.common.gameplay.machine.MachineUpgrades.hasRedstone(container);
    }

    /** The machine's draw after its cards, worked out from the synced slots - for the screen's tooltip. */
    public long getDrawPerTick() {
        var container = slots.get(upgradeFirst).container;
        return Math.max(1L, Math.round(kind.powerPerTick() * com.human.common.gameplay.machine.MachineUpgrades.drawMultiplier(container)));
    }

    public boolean isFilterSlot(Slot slot) {
        return slot.index >= filterFirst && slot.index < filterEnd;
    }

    public boolean isFillSlot(Slot slot) {
        return kind.layout().hasFillSlot() && slot.index == kind.layout().fillSlot();
    }

    /** The tank as the server last reported it - EMPTY for a machine without one. */
    public FluidContent getFluid() {
        var id = data.get(ElectricMachineBlockEntity.DATA_FLUID);

        if (id < 0) {
            return FluidContent.EMPTY;
        }

        var fluid = BuiltInRegistries.FLUID.byId(id);

        return FluidContent.of(fluid == null ? Fluids.EMPTY : fluid, data.get(ElectricMachineBlockEntity.DATA_FLUID_AMOUNT));
    }

    public boolean keepsItemOutput() {
        return (data.get(ElectricMachineBlockEntity.DATA_FLAGS) & ElectricMachineBlockEntity.FLAG_KEEP_ITEMS) != 0;
    }

    public boolean keepsFluidOutput() {
        return (data.get(ElectricMachineBlockEntity.DATA_FLAGS) & ElectricMachineBlockEntity.FLAG_KEEP_FLUID) != 0;
    }

    @Override
    public boolean clickMenuButton(@NotNull Player player, int id) {
        if (id == BUTTON_CYCLE_PRIORITY) {
            if (blockEntity != null) {
                blockEntity.cyclePriority();
            }

            return true;
        }

        if (id >= BUTTON_SET_PRIORITY && id < BUTTON_SET_PRIORITY + MachinePriority.values().length) {
            if (blockEntity != null) {
                blockEntity.setPriority(MachinePriority.byOrdinal(id - BUTTON_SET_PRIORITY));
            }

            return true;
        }

        if (
            id >= BUTTON_SET_REDSTONE && id < BUTTON_SET_REDSTONE + com.human.common.gameplay.machine.MachineUpgrades.RedstoneMode
                .values().length
        ) {
            if (blockEntity != null) {
                blockEntity.setRedstoneMode(
                    com.human.common.gameplay.machine.MachineUpgrades.RedstoneMode.byOrdinal(id - BUTTON_SET_REDSTONE)
                );
            }

            return true;
        }

        // The filter: the same rule as clicking the old ghost slots - one copy of what is held, the stack untouched.
        if (id >= BUTTON_SET_FILTER && id < BUTTON_SET_FILTER + ElectricMachineBlockEntity.FILTER_SIZE) {
            var carried = getCarried();
            filter.setItem(id - BUTTON_SET_FILTER, carried.isEmpty() ? ItemStack.EMPTY : carried.copyWithCount(1));
            return true;
        }

        if (id >= BUTTON_CLEAR_FILTER && id < BUTTON_CLEAR_FILTER + ElectricMachineBlockEntity.FILTER_SIZE) {
            filter.setItem(id - BUTTON_CLEAR_FILTER, ItemStack.EMPTY);
            return true;
        }

        if (id >= BUTTON_SET_PORT && id < BUTTON_SET_PORT + com.human.common.gameplay.machine.MachinePorts.FACES * 8) {
            var code = id - BUTTON_SET_PORT;

            // The block entity refuses a fluid setting the machine cannot have; nothing else needs checking here.
            if (blockEntity != null) {
                blockEntity.setPort(
                    com.human.common.gameplay.machine.MachinePorts.Face.values()[code / 8],
                    (code & 4) != 0,
                    com.human.common.gameplay.machine.MachinePorts.Mode.byOrdinal(code & 3)
                );
            }

            return true;
        }

        if (id == BUTTON_CYCLE_REDSTONE) {
            if (blockEntity != null) {
                blockEntity.cycleRedstoneMode();
            }

            return true;
        }

        // A machine's own buttons (the distributor's modes). The block entity range-checks its own ids.
        if (blockEntity != null && blockEntity.handleMenuButton(player, id)) {
            return true;
        }

        if (kind.hasDiscardSwitches() && (id == BUTTON_TOGGLE_KEEP_ITEMS || id == BUTTON_TOGGLE_KEEP_FLUID)) {
            if (blockEntity != null) {
                if (id == BUTTON_TOGGLE_KEEP_ITEMS) {
                    blockEntity.toggleKeepItemOutput();
                } else {
                    blockEntity.toggleKeepFluidOutput();
                }
            }

            return true;
        }

        return false;
    }

    @Override
    public void clicked(int slotId, int button, @NotNull ClickType clickType, @NotNull Player player) {
        if (slotId >= filterFirst && slotId < filterEnd) {
            var carried = getCarried();
            var index = slotId - filterFirst;

            // Any click with an item sets the filter entry to one copy of it; any click with an empty cursor, or a
            // shift-click, clears it. The carried stack is never touched.
            if (clickType == ClickType.QUICK_MOVE || carried.isEmpty()) {
                filter.setItem(index, ItemStack.EMPTY);
            } else if (clickType == ClickType.PICKUP || clickType == ClickType.QUICK_CRAFT) {
                filter.setItem(index, carried.copyWithCount(1));
            }

            return;
        }

        super.clicked(slotId, button, clickType, player);
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
        var slot = slots.get(index);

        if (!slot.hasItem() || isFilterSlot(slot)) {
            return ItemStack.EMPTY;
        }

        var stack = slot.getItem();
        var original = stack.copy();

        if (index >= ElectricMachineBlockEntity.FIRST_OUTPUT && index < machineSlots) {
            if (!moveItemStackTo(stack, playerFirst, playerEnd, true)) {
                return ItemStack.EMPTY;
            }

            slot.onQuickCraft(stack, original);
        } else if (index == ElectricMachineBlockEntity.SLOT_INPUT || index >= upgradeFirst && index < upgradeEnd) {
            if (!moveItemStackTo(stack, playerFirst, playerEnd, false)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveIntoMachine(stack)) {
            // Could not go into the machine: shuffle between the main inventory and the hotbar instead.
            var hotbarFirst = playerEnd - 9;

            if (index < hotbarFirst) {
                if (!moveItemStackTo(stack, hotbarFirst, playerEnd, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!moveItemStackTo(stack, playerFirst, hotbarFirst, false)) {
                return ItemStack.EMPTY;
            }
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

    /**
     * Shift-click from the player's inventory: an empty bucket or canister goes to the FILL slot when the machine has
     * one - it has no recipe, so it could only clog the input - and everything else to the input.
     */
    private boolean moveIntoMachine(ItemStack stack) {
        var layout = kind.layout();

        if (com.human.common.gameplay.machine.MachineUpgrades.fits(stack)) {
            return moveItemStackTo(stack, upgradeFirst, upgradeEnd, false);
        }

        if (layout.hasFillSlot() && MachineFluidItems.isFillable(stack)) {
            return moveItemStackTo(stack, layout.fillSlot(), layout.fillSlot() + 1, false);
        }

        return moveItemStackTo(stack, ElectricMachineBlockEntity.SLOT_INPUT, ElectricMachineBlockEntity.SLOT_INPUT + 1, false);
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return machine.stillValid(player);
    }

    @Override
    public void removed(@NotNull Player player) {
        super.removed(player);
        machine.stopOpen(player);
    }

    /** The output: take only, and taking pays out the experience the machine has banked. */
    private static class OutputSlot extends Slot {

        private final Player player;

        OutputSlot(Player player, Container container, int slot, int x, int y) {
            super(container, slot, x, y);
            this.player = player;
        }

        @Override
        public boolean mayPlace(@NotNull ItemStack stack) {
            return false;
        }

        @Override
        public void onTake(@NotNull Player taker, @NotNull ItemStack stack) {
            if (container instanceof ElectricMachineBlockEntity machine && taker instanceof ServerPlayer serverPlayer) {
                machine.awardExperience(serverPlayer);
            }

            super.onTake(taker, stack);
        }

        @Override
        protected void checkTakeAchievements(@NotNull ItemStack stack) {
            stack.onCraftedBy(player.level(), player, stack.getCount());
        }
    }

    /** The fill slot: one empty bucket or canister at a time, filled from the machine's tank. */
    private static class FillSlot extends Slot {

        FillSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(@NotNull ItemStack stack) {
            return MachineFluidItems.isFillable(stack);
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }
    }

    /** The distributor's tank slot: a filled vapor tank, and only one - the empty aluminum tank is refused. */
    private static class VaporTankSlot extends Slot {

        VaporTankSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(@NotNull ItemStack stack) {
            return com.human.common.gameplay.vapor.VaporKind.fromTank(stack) != null;
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }
    }

    /** A filter entry: shows an item, never holds a real one. */
    private static class GhostSlot extends Slot {

        GhostSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(@NotNull ItemStack stack) {
            return false;
        }

        @Override
        public boolean mayPickup(@NotNull Player player) {
            return false;
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }

        /**
         * [stated] Oct 2: the filter lives in the Filter pop-up. The slot stays so its contents keep syncing to the
         * client, but it is never drawn or clicked - a slot's position is fixed, a pop-up's is not.
         */
        @Override
        public boolean isActive() {
            return false;
        }
    }
}
