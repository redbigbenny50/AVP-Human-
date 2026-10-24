package com.human.common.gameplay.quarry;

import com.human.common.registry.init.HumanQuarry;
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
 * The Quarry-Com screen's container ([stated] Oct 15: tabs WORK AREA, SETTINGS, PROGRESS, STORAGE; START/STOP in the
 * header on every tab).
 * <p>
 * Tabs are client-side. Slots exist all the time and report themselves inactive off their tab, which is how vanilla
 * hides a slot. The player's inventory sits in a different place on PROGRESS (beside the upgrades) and STORAGE (under
 * the storage grid), so it is added twice - one set per tab, both the same inventory.
 * </p>
 * <h2>THE 128 SLOTS</h2> Only 32 are shown at once (8 wide, 4 rows) and the view scrolls. The visible slots look into
 * the storage through {@link ScrolledView}; the scroll row is a menu button, so the server's view follows the client's.
 */
public class QuarryTerminalMenu extends AbstractContainerMenu {

    public static final int GUI_WIDTH = 296;

    public static final int GUI_HEIGHT = 236;

    public static final int TAB_WORK_AREA = 0;

    public static final int TAB_SETTINGS = 1;

    public static final int TAB_PROGRESS = 2;

    public static final int TAB_STORAGE = 3;

    public static final int VIEW_COLUMNS = 8;

    public static final int VIEW_ROWS = 4;

    public static final int MAX_SCROLL = QuarryTerminalBlockEntity.STORAGE_SLOTS / VIEW_COLUMNS - VIEW_ROWS;

    // Slot positions (vanilla slot coordinates: the item's top-left, one pixel inside the drawn well).
    public static final int STORAGE_X = 17;

    public static final int STORAGE_Y = 69;

    public static final int FRAMES_X = 185;

    public static final int FRAMES_Y = 146;

    public static final int UPGRADES_X = 17;

    public static final int UPGRADES_Y = 153;

    public static final int STORAGE_INVENTORY_X = 17;

    public static final int PROGRESS_INVENTORY_X = 118;

    public static final int INVENTORY_Y = 146;

    public static final int HOTBAR_Y = 204;

    // Menu buttons.
    public static final int BUTTON_START = 1;

    public static final int BUTTON_STOP = 2;

    public static final int BUTTON_SCROLL = 10;

    public static final int BUTTON_SETTING = 100;

    public static final int BUTTON_CONFIRM = 3;

    public static final int BUTTON_EDIT = 4;

    public static final int BUTTON_RETRACT = 5;

    /** Bottom Y: -10, -1, +1, +10. */
    public static final int BUTTON_BOTTOM = 40;

    /** A WORK AREA map cell: base + row * 7 + column. */
    public static final int BUTTON_CELL = 300;

    /** The REFILL panel's own START / STOP, and its MIN Y / MAX Y steppers (-10, -1, +1, +10). */
    public static final int BUTTON_REFILL_START = 6;

    public static final int BUTTON_REFILL_STOP = 7;

    public static final int BUTTON_REFILL_MIN = 50;

    public static final int BUTTON_REFILL_MAX = 60;

    private static final int[] BOTTOM_STEPS = { -10, -1, 1, 10 };

    // Slot index ranges.
    private static final int VIEW_SLOTS = VIEW_COLUMNS * VIEW_ROWS;

    private static final int FIRST_FRAME = VIEW_SLOTS;

    private static final int FIRST_UPGRADE = FIRST_FRAME + QuarryTerminalBlockEntity.FRAME_SLOTS;

    /** The enchantment slot (FORTUNE or SILK TOUCH book) beside the 2 x 2 upgrades. */
    public static final int FIRST_ENCHANT = FIRST_UPGRADE + QuarryTerminalBlockEntity.UPGRADE_SLOTS;

    /** Under the 2 x 2 upgrades, with room for its name at full size beside it. */
    public static final int ENCHANTS_X = UPGRADES_X;

    public static final int ENCHANTS_Y = UPGRADES_Y + 2 * 18 + 4;

    /** The REFILL HOLE FILL WITH grid (SETTINGS tab), 3 x 3, inside the REFILL panel. */
    public static final int FIRST_FILL = FIRST_ENCHANT + QuarryTerminalBlockEntity.ENCHANT_SLOTS;

    public static final int FILL_X = 178;

    public static final int FILL_Y = 107;

    private static final int FIRST_INVENTORY = FIRST_FILL + QuarryTerminalBlockEntity.FILL_SLOTS;

    private static final int INVENTORY_SLOTS = 36;

    // Data slots.
    public static final int DATA_STATUS = 0;

    public static final int DATA_RUNNING = 1;

    public static final int DATA_SCREEN = 2;

    public static final int DATA_USED = 3;

    public static final int DATA_FRAMES = 4;

    public static final int DATA_SECONDS = 5;

    public static final int DATA_EDITION = 6;

    public static final int DATA_SETTINGS = 7;

    public static final int DATA_PROBLEM = DATA_SETTINGS + QuarrySettings.Option.values().length;

    public static final int DATA_CONFIRMED = DATA_PROBLEM + 1;

    public static final int DATA_FRAME_STATE = DATA_PROBLEM + 2;

    public static final int DATA_NEEDED = DATA_PROBLEM + 3;

    public static final int DATA_IN_PLACE = DATA_PROBLEM + 4;

    public static final int DATA_BLOCKED = DATA_PROBLEM + 5;

    public static final int DATA_START_Y = DATA_PROBLEM + 6;

    public static final int DATA_BOTTOM_Y = DATA_PROBLEM + 7;

    public static final int DATA_SELECTION = DATA_PROBLEM + 8;

    public static final int DATA_MAP = DATA_PROBLEM + 9;

    /** Mining progress (phase 4a). All fit the 16 bits a data slot carries: no block counts, only layers. */
    public static final int DATA_MINING = DATA_MAP + QuarryTerminalBlockEntity.MAP_SIZE * QuarryTerminalBlockEntity.MAP_SIZE;

    public static final int DATA_CHUNK = DATA_MINING;

    public static final int DATA_CHUNKS = DATA_MINING + 1;

    public static final int DATA_LAYER_Y = DATA_MINING + 2;

    public static final int DATA_LAYERS_LEFT = DATA_MINING + 3;

    public static final int DATA_LAYERS_TOTAL = DATA_MINING + 4;

    /** BLOCKS MINED in two 16-bit halves (a data slot carries 16 bits; a 3 x 3 area is ~885,000 blocks). */
    public static final int DATA_MINED_LO = DATA_MINING + 5;

    public static final int DATA_MINED_HI = DATA_MINING + 6;

    public static final int DATA_ENERGY = DATA_MINING + 7;

    public static final int DATA_LAYER_OF_CHUNK = DATA_MINING + 8;

    public static final int DATA_LAYERS_PER_CHUNK = DATA_MINING + 9;

    /** The tank: tenths of a bucket (0-1280) and the liquid's registry id (-1 = empty). */
    public static final int DATA_TANK = DATA_MINING + 10;

    public static final int DATA_TANK_FLUID = DATA_MINING + 11;

    /** REFILL (phase 4c). */
    public static final int DATA_FILL_RUNNING = DATA_MINING + 12;

    public static final int DATA_REFILL_MIN = DATA_MINING + 13;

    public static final int DATA_REFILL_MAX = DATA_MINING + 14;

    public static final int DATA_FILL_CHUNK = DATA_MINING + 15;

    public static final int DATA_FILL_LAYER_OF = DATA_MINING + 16;

    public static final int DATA_FILL_LAYERS = DATA_MINING + 17;

    public static final int DATA_FILL_Y = DATA_MINING + 18;

    public static final int DATA_PLACED_LO = DATA_MINING + 19;

    public static final int DATA_PLACED_HI = DATA_MINING + 20;

    public static final int DATA_FILL_LEFT = DATA_MINING + 21;

    public static final int DATA_FILL_TOTAL = DATA_MINING + 22;

    public static final int DATA_FILL_SLOT = DATA_MINING + 23;

    public static final int DATA_ON_HAND = DATA_MINING + 24;

    public static final int DATA_REFILL_ON = DATA_MINING + 25;

    private static final int DATA_COUNT = DATA_MINING + 26;

    private final @Nullable QuarryTerminalBlockEntity terminal;

    private final ContainerData data;

    private final ScrolledView view;

    /** Client side: which tab the screen shows. The server has no tabs and keeps every slot live. */
    private int tab = TAB_PROGRESS;

    /** Client. */
    public QuarryTerminalMenu(int containerId, Inventory inventory) {
        this(
            containerId,
            inventory,
            null,
            new SimpleContainer(QuarryTerminalBlockEntity.STORAGE_SLOTS),
            new SimpleContainer(QuarryTerminalBlockEntity.FRAME_SLOTS),
            new SimpleContainer(QuarryTerminalBlockEntity.UPGRADE_SLOTS),
            new SimpleContainer(QuarryTerminalBlockEntity.ENCHANT_SLOTS),
            new SimpleContainer(QuarryTerminalBlockEntity.FILL_SLOTS),
            new SimpleContainerData(DATA_COUNT)
        );
    }

    /** Server. */
    public QuarryTerminalMenu(int containerId, Inventory inventory, QuarryTerminalBlockEntity terminal) {
        this(
            containerId,
            inventory,
            terminal,
            terminal.storage(),
            terminal.frames(),
            terminal.upgrades(),
            terminal.enchants(),
            terminal.fills(),
            null
        );
        terminal.openedBy();
    }

    private QuarryTerminalMenu(
        int containerId,
        Inventory inventory,
        @Nullable QuarryTerminalBlockEntity terminal,
        Container storage,
        Container frames,
        Container upgrades,
        Container enchants,
        Container fills,
        @Nullable ContainerData clientData
    ) {
        super(HumanQuarry.QUARRY_TERMINAL_MENU.get(), containerId);

        this.terminal = terminal;
        this.data = clientData != null ? clientData : serverData(terminal);
        this.view = new ScrolledView(storage);

        for (int row = 0; row < VIEW_ROWS; row++) {
            for (int column = 0; column < VIEW_COLUMNS; column++) {
                addSlot(new TabSlot(view, column + row * VIEW_COLUMNS, STORAGE_X + column * 18, STORAGE_Y + row * 18, TAB_STORAGE) {

                    // Output only: the quarry fills these, the player and pipes take from them.
                    @Override
                    public boolean mayPlace(@NotNull ItemStack stack) {
                        return false;
                    }
                });
            }
        }

        for (int slot = 0; slot < QuarryTerminalBlockEntity.FRAME_SLOTS; slot++) {
            addSlot(new TabSlot(frames, slot, FRAMES_X + (slot % 3) * 18, FRAMES_Y + (slot / 3) * 18, TAB_STORAGE) {

                @Override
                public boolean mayPlace(@NotNull ItemStack stack) {
                    return QuarryTerminalBlockEntity.isFrame(stack);
                }
            });
        }

        for (int slot = 0; slot < QuarryTerminalBlockEntity.UPGRADE_SLOTS; slot++) {
            var index = slot;

            addSlot(new TabSlot(upgrades, slot, UPGRADES_X + (slot % 2) * 18, UPGRADES_Y + (slot / 2) * 18, TAB_PROGRESS) {

                @Override
                public boolean mayPlace(@NotNull ItemStack stack) {
                    return terminal == null || terminal.mayPlaceUpgrade(index, stack);
                }

                @Override
                public int getMaxStackSize() {
                    return 1;
                }
            });
        }

        for (int slot = 0; slot < QuarryTerminalBlockEntity.ENCHANT_SLOTS; slot++) {
            addSlot(new TabSlot(enchants, slot, ENCHANTS_X, ENCHANTS_Y + slot * 18, TAB_PROGRESS) {

                @Override
                public boolean mayPlace(@NotNull ItemStack stack) {
                    return QuarryTerminalBlockEntity.mayPlaceEnchant(stack);
                }

                @Override
                public int getMaxStackSize() {
                    return 1;
                }
            });
        }

        for (int slot = 0; slot < QuarryTerminalBlockEntity.FILL_SLOTS; slot++) {
            addSlot(new TabSlot(fills, slot, FILL_X + (slot % 3) * 18, FILL_Y + (slot / 3) * 18, TAB_SETTINGS) {

                @Override
                public boolean mayPlace(@NotNull ItemStack stack) {
                    return QuarryFiller.isFill(stack);
                }
            });
        }

        addInventory(inventory, STORAGE_INVENTORY_X, TAB_STORAGE);
        addInventory(inventory, PROGRESS_INVENTORY_X, TAB_PROGRESS);
        addDataSlots(data);
    }

    private void addInventory(Inventory inventory, int x, int onTab) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new TabSlot(inventory, column + row * 9 + 9, x + column * 18, INVENTORY_Y + row * 18, onTab));
            }
        }

        for (int column = 0; column < 9; column++) {
            addSlot(new TabSlot(inventory, column, x + column * 18, HOTBAR_Y, onTab));
        }
    }

    /** A slot shown only on one tab (always live on the server, which has no tabs). */
    private class TabSlot extends Slot {

        private final int onTab;

        TabSlot(Container container, int slot, int x, int y, int onTab) {
            super(container, slot, x, y);
            this.onTab = onTab;
        }

        @Override
        public boolean isActive() {
            return terminal != null || tab == onTab;
        }
    }

    /** 32 slots looking at the storage from the scrolled row down. */
    private static final class ScrolledView implements Container {

        private final Container storage;

        private int row;

        ScrolledView(Container storage) {
            this.storage = storage;
        }

        private int index(int slot) {
            return row * VIEW_COLUMNS + slot;
        }

        @Override
        public int getContainerSize() {
            return VIEW_SLOTS;
        }

        @Override
        public boolean isEmpty() {
            return storage.isEmpty();
        }

        @Override
        public @NotNull ItemStack getItem(int slot) {
            return storage.getItem(index(slot));
        }

        @Override
        public @NotNull ItemStack removeItem(int slot, int amount) {
            return storage.removeItem(index(slot), amount);
        }

        @Override
        public @NotNull ItemStack removeItemNoUpdate(int slot) {
            return storage.removeItemNoUpdate(index(slot));
        }

        @Override
        public void setItem(int slot, @NotNull ItemStack stack) {
            storage.setItem(index(slot), stack);
        }

        @Override
        public void setChanged() {
            storage.setChanged();
        }

        @Override
        public boolean stillValid(@NotNull Player player) {
            return true;
        }

        @Override
        public void clearContent() {}
    }

    private static ContainerData serverData(QuarryTerminalBlockEntity terminal) {
        return new ContainerData() {

            @Override
            public int get(int index) {
                if (index >= DATA_MINING) {
                    // Offsets from DATA_MINING: chunk, chunks, layer Y, layers left, layers total.
                    return switch (index - DATA_MINING) {
                        case 0 -> terminal.miningChunk();
                        case 1 -> terminal.areaChunksForRender().size();
                        case 2 -> terminal.miningLayerY();
                        case 3 -> terminal.layersLeft();
                        case 4 -> terminal.layersTotal();
                        case 5 -> terminal.blocksMined() & 0xFFFF;
                        case 6 -> terminal.blocksMined() >>> 16;
                        case 7 -> terminal.energyPercent();
                        case 8 -> terminal.layerOfChunk();
                        case 9 -> terminal.layersPerChunk();
                        case 10 -> terminal.tankTenths();
                        case 11 -> terminal.tankFluidId();
                        case 12 -> terminal.isFillRunning() ? 1 : 0;
                        case 13 -> terminal.refillMin();
                        case 14 -> terminal.refillMax();
                        case 15 -> terminal.fillChunk();
                        case 16 -> terminal.fillLayerOfChunk();
                        case 17 -> terminal.fillLayersPerChunk();
                        case 18 -> terminal.fillLayerY();
                        case 19 -> terminal.blocksPlaced() & 0xFFFF;
                        case 20 -> terminal.blocksPlaced() >>> 16;
                        case 21 -> terminal.fillLayersLeft();
                        case 22 -> terminal.fillLayersTotal();
                        case 23 -> terminal.fillSlot();
                        // 9 stacks of 64 at most: fits 16 bits.
                        case 24 -> terminal.fillOnHand();
                        default -> terminal.refillEnabled() ? 1 : 0;
                    };
                }

                if (index >= DATA_MAP) {
                    return terminal.mapCell(index - DATA_MAP);
                }

                if (index >= DATA_PROBLEM) {
                    return switch (index - DATA_PROBLEM) {
                        case 0 -> terminal.problem();
                        case 1 -> terminal.isConfirmed() ? 1 : 0;
                        case 2 -> terminal.frameState().ordinal();
                        case 3 -> terminal.framesNeeded();
                        case 4 -> terminal.framesInPlace();
                        case 5 -> terminal.blockedCount();
                        case 6 -> terminal.startY();
                        case 7 -> terminal.bottomY();
                        default -> terminal.selection();
                    };
                }

                if (index >= DATA_SETTINGS) {
                    return terminal.settings().get(QuarrySettings.Option.values()[index - DATA_SETTINGS]);
                }

                return switch (index) {
                    case DATA_STATUS -> terminal.status().ordinal();
                    case DATA_RUNNING -> terminal.isRunning() ? 1 : 0;
                    case DATA_SCREEN -> terminal.screen().ordinal();
                    case DATA_USED -> terminal.usedStorageSlots();
                    case DATA_FRAMES -> terminal.frameCount();
                    case DATA_SECONDS -> terminal.secondsPerLayer();
                    case DATA_EDITION -> terminal.edition().ordinal();
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

    // ------------------------------------------------------------------------------------------------------------
    // Client reads
    // ------------------------------------------------------------------------------------------------------------

    public int data(int index) {
        return data.get(index);
    }

    /** Rebuilds a number sent in two 16-bit data slots (the client stores each as a short). */
    public int wide(int lowIndex, int highIndex) {
        return (data.get(highIndex) & 0xFFFF) << 16 | data.get(lowIndex) & 0xFFFF;
    }

    public QuarryTerminalBlockEntity.Status status() {
        var values = QuarryTerminalBlockEntity.Status.values();

        return values[Math.clamp(data.get(DATA_STATUS), 0, values.length - 1)];
    }

    public QuarryEdition edition() {
        var values = QuarryEdition.values();

        return values[Math.clamp(data.get(DATA_EDITION), 0, values.length - 1)];
    }

    public int setting(QuarrySettings.Option option) {
        return data.get(DATA_SETTINGS + option.ordinal());
    }

    public int scrollRow() {
        return view.row;
    }

    public void setTab(int tab) {
        this.tab = tab;
    }

    public int tab() {
        return tab;
    }

    // ------------------------------------------------------------------------------------------------------------
    // Buttons
    // ------------------------------------------------------------------------------------------------------------

    @Override
    public boolean clickMenuButton(@NotNull Player player, int id) {
        if (id >= BUTTON_SCROLL && id <= BUTTON_SCROLL + MAX_SCROLL) {
            view.row = id - BUTTON_SCROLL;
            broadcastChanges();
            return true;
        }

        if (terminal == null) {
            return false;
        }

        if (id == BUTTON_START || id == BUTTON_STOP) {
            terminal.setRunning(id == BUTTON_START);
            return true;
        }

        if (id == BUTTON_REFILL_START || id == BUTTON_REFILL_STOP) {
            terminal.setRefilling(id == BUTTON_REFILL_START);
            return true;
        }

        if (id >= BUTTON_REFILL_MIN && id < BUTTON_REFILL_MIN + BOTTOM_STEPS.length) {
            terminal.moveRefillMin(BOTTOM_STEPS[id - BUTTON_REFILL_MIN]);
            return true;
        }

        if (id >= BUTTON_REFILL_MAX && id < BUTTON_REFILL_MAX + BOTTOM_STEPS.length) {
            terminal.moveRefillMax(BOTTOM_STEPS[id - BUTTON_REFILL_MAX]);
            return true;
        }

        if (id == BUTTON_CONFIRM) {
            terminal.confirm();
            return true;
        }

        if (id == BUTTON_EDIT) {
            terminal.unconfirm();
            return true;
        }

        if (id == BUTTON_RETRACT) {
            terminal.retract();
            return true;
        }

        if (id >= BUTTON_BOTTOM && id < BUTTON_BOTTOM + BOTTOM_STEPS.length) {
            terminal.moveBottom(BOTTOM_STEPS[id - BUTTON_BOTTOM]);
            return true;
        }

        if (id >= BUTTON_CELL && id < BUTTON_CELL + QuarryTerminalBlockEntity.MAP_SIZE * QuarryTerminalBlockEntity.MAP_SIZE) {
            var cell = id - BUTTON_CELL;

            terminal.toggleCell(cell / QuarryTerminalBlockEntity.MAP_SIZE, cell % QuarryTerminalBlockEntity.MAP_SIZE);
            return true;
        }

        if (id >= BUTTON_SETTING && id < BUTTON_CELL) {
            var options = QuarrySettings.Option.values();
            var option = (id - BUTTON_SETTING) / 10;
            var choice = (id - BUTTON_SETTING) % 10;

            if (option < options.length && terminal.settings().set(options[option], choice)) {
                terminal.settingsChanged();
            }

            return true;
        }

        return false;
    }

    /** Client side: scroll the view locally at once (the button tells the server). */
    public void scrollTo(int row) {
        view.row = Math.clamp(row, 0, MAX_SCROLL);
    }

    // ------------------------------------------------------------------------------------------------------------
    // Shift-click
    // ------------------------------------------------------------------------------------------------------------

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
        var slot = slots.get(index);

        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        var stack = slot.getItem();
        var original = stack.copy();

        if (index < FIRST_INVENTORY) {
            // Out of the terminal into the player's inventory.
            if (!moveItemStackTo(stack, FIRST_INVENTORY, FIRST_INVENTORY + INVENTORY_SLOTS, true)) {
                return ItemStack.EMPTY;
            }
        } else if (QuarryTerminalBlockEntity.isFrame(stack)) {
            if (!moveItemStackTo(stack, FIRST_FRAME, FIRST_UPGRADE, false)) {
                return ItemStack.EMPTY;
            }
        } else if (QuarryFiller.isFill(stack)) {
            // Plain blocks go to the FILL WITH grid - the only slots a block can go in.
            if (!moveItemStackTo(stack, FIRST_FILL, FIRST_INVENTORY, false)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, FIRST_UPGRADE, FIRST_FILL, false)) {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }

        return original;
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return terminal == null || !terminal.isRemoved() && player.distanceToSqr(terminal.getBlockPos().getCenter()) <= 64.0;
    }

    @Override
    public void removed(@NotNull Player player) {
        super.removed(player);

        if (terminal != null) {
            terminal.closedBy();
        }
    }
}
