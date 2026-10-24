package com.human.common.gameplay.fabrication;

import com.human.Human;
import com.human.common.gameplay.machine.MachinePriority;
import com.human.common.gameplay.machine.MachineUpgrades;
import com.human.common.network.packet.S2CFabricatorPayload;
import com.human.common.registry.init.HumanFabrication;
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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;

/**
 * The fabricator terminal. [stated] Oct 16 layout: FOUR views picked from one row of buttons at the top - PRINT
 * (catalogue, materials and printing together), BLUEPRINTS, STORAGE, SYSTEM. Slots exist only on the view that shows
 * them ({@link #tab}); internal storage is 48 slots shown 32 at a time through a scrolling window.
 * <p>
 * Every choice goes through vanilla's menu-button packet and the server range-checks it, so the client decides nothing.
 * Twice a second the server reads connected storage ONCE and uses that read for the materials and the storage list, and
 * sends only what changed.
 * </p>
 */
public class FabricatorMenu extends AbstractContainerMenu {

    public static final String[] TABS = { "print", "blueprints", "storage", "system" };

    public static final int PRINT_VIEW = 0;

    public static final int BLUEPRINTS = 1;

    public static final int STORAGE = 2;

    public static final int SYSTEM = 3;

    // Button ids. 0-3 pick a view.
    public static final int COUNT_DOWN = 10;

    public static final int COUNT_UP = 11;

    public static final int COUNT_DOWN_TEN = 12;

    public static final int COUNT_UP_TEN = 13;

    public static final int PRINT = 14;

    public static final int STOP = 15;

    /** Quantity +16 (the old -10 / +10 ids stay valid). */
    public static final int COUNT_UP_SIXTEEN = 16;

    /** + the first row of internal storage to show (0 to {@link #MAX_STORAGE_SCROLL}). */
    public static final int STORAGE_SCROLL = 40;

    public static final int STORAGE_COLUMNS = 8;

    public static final int STORAGE_ROWS_SHOWN = 4;

    public static final int STORAGE_SHOWN = STORAGE_COLUMNS * STORAGE_ROWS_SHOWN;

    public static final int MAX_STORAGE_SCROLL = FabricatorBlockEntity.STORAGE_SLOTS / STORAGE_COLUMNS - STORAGE_ROWS_SHOWN;

    // Menu slot order: the 32 storage slots in view, the 16 blueprints, the output, the 4 upgrades, then the inventory.
    public static final int MENU_FIRST_BLUEPRINT = STORAGE_SHOWN;

    public static final int MENU_OUTPUT = MENU_FIRST_BLUEPRINT + FabricatorBlockEntity.BLUEPRINT_SLOTS;

    public static final int MENU_FIRST_UPGRADE = MENU_OUTPUT + 1;

    public static final int MENU_FIRST_INVENTORY = MENU_FIRST_UPGRADE + FabricatorBlockEntity.UPGRADE_SLOTS;

    /** + a {@link MachinePriority} ordinal. */
    public static final int PRIORITY = 20;

    /** + a {@link MachineUpgrades.RedstoneMode} ordinal. */
    public static final int REDSTONE = 30;

    /** + an index into the catalogue this screen was last sent. */
    public static final int SELECT = 1000;

    // Slot positions on the 320 x 236 screen.
    public static final int SLOT_X = 14;

    public static final int STORAGE_Y = 60;

    public static final int BLUEPRINT_Y = 62;

    public static final int UPGRADE_Y = 62;

    public static final int INVENTORY_X = 80;

    public static final int INVENTORY_Y = 152;

    public static final int OUTPUT_X = 291;

    public static final int OUTPUT_Y = 207;

    private static final int REFRESH_TICKS = 10;

    /**
     * Caps on what one packet carries. Vanilla refuses a custom payload over 1 MiB and DISCONNECTS the player, so a
     * huge connected storage must never be able to build one. Catalogue entries are bare item ids (a few bytes each);
     * storage entries are real stacks, components and all, so they get the smaller cap. The screen shows the totals.
     */
    public static final int MAX_CATALOGUE_LINES = 1024;

    public static final int MAX_STORAGE_LINES = 512;

    /**
     * 0 progress, 1 status, 2 queued, 3 count, 4 seconds left, 5 priority, 6 redstone mode, 7 redstone card, 8 draw.
     */
    private final ContainerData data;

    private final @Nullable FabricatorBlockEntity fabricator;

    private final Player player;

    private int count = 1;

    private int serverTab = PRINT_VIEW;

    /** Client side: the view the screen shows, which decides which slots exist. */
    public int tab = PRINT_VIEW;

    /**
     * Client side: the STORAGE view's INTERNAL / CONNECTED switch. CONNECTED shows connected storage instead of the
     * internal slots, so the internal slots hide while it is on.
     */
    public boolean showInternal = true;

    /** The first row of internal storage in view. Set by the screen and mirrored to the server by button. */
    private int storageScroll;

    public int storageScroll() {
        return storageScroll;
    }

    /**
     * Client side: scroll the internal storage. The screen then sends {@link #STORAGE_SCROLL} + the row so the server
     * reads the same slots. (No client classes here: this menu is loaded on the server and in datagen too.)
     */
    public void setStorageScroll(int row) {
        storageScroll = Math.clamp(row, 0, MAX_STORAGE_SCROLL);
    }

    private @Nullable Item selected;

    private List<Item> sentCatalogue = List.of();

    private int sentHash;

    // Oct 14 FIX - was Long.MIN_VALUE: "now - Long.MIN_VALUE" OVERFLOWS to a huge negative number, so the time check
    // never passed. Half of it leaves room to subtract any real game time.
    // Here "now - lastRefresh < REFRESH_TICKS" was always TRUE, so the screen's lists were never refreshed.
    private long lastRefresh = Long.MIN_VALUE / 2;

    public FabricatorMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, null);
    }

    public FabricatorMenu(int containerId, Inventory inventory, @Nullable FabricatorBlockEntity fabricator) {
        super(HumanFabrication.FABRICATOR_MENU.get(), containerId);
        this.fabricator = fabricator;
        this.player = inventory.player;
        this.data = fabricator == null ? new SimpleContainerData(9) : new ContainerData() {

            @Override
            public int get(int index) {
                return switch (index) {
                    case 0 -> fabricator.progressPercent();
                    case 1 -> fabricator.status().ordinal();
                    case 2 -> fabricator.queued();
                    case 3 -> count;
                    case 4 -> Math.min(Short.MAX_VALUE, fabricator.ticksLeft() / 20);
                    case 5 -> fabricator.priority().ordinal();
                    case 6 -> fabricator.redstoneMode().ordinal();
                    case 7 -> fabricator.hasRedstoneCard() ? 1 : 0;
                    default -> (int) Math.min(Short.MAX_VALUE, fabricator.printPower());
                };
            }

            @Override
            public void set(int index, int value) {}

            @Override
            public int getCount() {
                return 9;
            }
        };

        Container container = fabricator == null
            ? new SimpleContainer(FabricatorBlockEntity.ALL_SLOTS)
            : new FabricatorContainer(fabricator);

        // STORAGE: 18 materials.
        // Internal storage: 32 slots on screen, each reading the storage slot the scroll puts under it.
        for (int view = 0; view < STORAGE_SHOWN; view++) {
            var index = view;

            addSlot(new TabSlot(container, 0, SLOT_X + (view % STORAGE_COLUMNS) * 18, STORAGE_Y + (view / STORAGE_COLUMNS) * 18, STORAGE) {

                private int target() {
                    return FabricatorBlockEntity.storageSlot(storageScroll * STORAGE_COLUMNS + index);
                }

                @Override
                public @NotNull ItemStack getItem() {
                    return container.getItem(target());
                }

                @Override
                public void set(@NotNull ItemStack stack) {
                    container.setItem(target(), stack);
                    setChanged();
                }

                @Override
                public void setByPlayer(@NotNull ItemStack stack, @NotNull ItemStack previous) {
                    set(stack);
                }

                @Override
                public @NotNull ItemStack remove(int amount) {
                    return container.removeItem(target(), amount);
                }

                @Override
                public boolean isActive() {
                    return super.isActive() && (!player.level().isClientSide || showInternal);
                }
            });
        }

        // BLUEPRINTS: 16 chips or licenses, one each.
        for (int slot = 0; slot < FabricatorBlockEntity.BLUEPRINT_SLOTS; slot++) {
            addSlot(
                new TabSlot(
                    container,
                    FabricatorBlockEntity.FIRST_BLUEPRINT + slot,
                    SLOT_X + (slot % 8) * 18,
                    BLUEPRINT_Y + (slot / 8) * 18,
                    BLUEPRINTS
                ) {

                    @Override
                    public boolean mayPlace(@NotNull ItemStack stack) {
                        return FabricatorBlockEntity.isBlueprintSlotItem(stack);
                    }

                    @Override
                    public int getMaxStackSize() {
                        return 1;
                    }
                }
            );
        }

        // PROGRESS: the output.
        addSlot(new TabSlot(container, FabricatorBlockEntity.OUTPUT_SLOT, OUTPUT_X, OUTPUT_Y, PRINT_VIEW) {

            @Override
            public boolean mayPlace(@NotNull ItemStack stack) {
                return false;
            }
        });

        // SYSTEM: four upgrade cards, by the machines' own rules.
        for (int slot = 0; slot < FabricatorBlockEntity.UPGRADE_SLOTS; slot++) {
            var index = slot;

            addSlot(new TabSlot(container, FabricatorBlockEntity.FIRST_UPGRADE + slot, SLOT_X + slot * 18, UPGRADE_Y, SYSTEM) {

                @Override
                public boolean mayPlace(@NotNull ItemStack stack) {
                    return fabricator == null || fabricator.mayPlaceUpgrade(index, stack);
                }

                @Override
                public int getMaxStackSize() {
                    return 1;
                }
            });
        }

        // The player's inventory, on the tabs that hold slots.
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new InventorySlot(inventory, column + row * 9 + 9, INVENTORY_X + column * 18, INVENTORY_Y + row * 18));
            }
        }

        for (int column = 0; column < 9; column++) {
            addSlot(new InventorySlot(inventory, column, INVENTORY_X + column * 18, INVENTORY_Y + 58));
        }

        addDataSlots(data);
    }

    /** A slot that exists only while its tab is shown. */
    private class TabSlot extends Slot {

        private final int shownOn;

        TabSlot(Container container, int index, int x, int y, int shownOn) {
            super(container, index, x, y);
            this.shownOn = shownOn;
        }

        @Override
        public boolean isActive() {
            return shown() == shownOn;
        }
    }

    private class InventorySlot extends Slot {

        InventorySlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean isActive() {
            var shown = shown();

            return shown == BLUEPRINTS || shown == STORAGE || shown == SYSTEM;
        }
    }

    /**
     * The tab in view: the screen's on the client, and the one it last reported on the server - shift-click runs there.
     */
    public int shown() {
        return player.level().isClientSide ? tab : serverTab;
    }

    // ------------------------------------------------------------------------------------------------------------
    // Synced values
    // ------------------------------------------------------------------------------------------------------------

    public int progress() {
        return data.get(0);
    }

    public FabricatorBlockEntity.Status status() {
        var values = FabricatorBlockEntity.Status.values();
        var index = data.get(1);

        return index >= 0 && index < values.length ? values[index] : FabricatorBlockEntity.Status.IDLE;
    }

    public int queued() {
        return data.get(2);
    }

    public int count() {
        return data.get(3);
    }

    public int secondsLeft() {
        return data.get(4);
    }

    public MachinePriority priority() {
        var values = MachinePriority.values();
        var index = data.get(5);

        return index >= 0 && index < values.length ? values[index] : MachinePriority.DEFAULT;
    }

    public MachineUpgrades.RedstoneMode redstoneMode() {
        return MachineUpgrades.RedstoneMode.byOrdinal(data.get(6));
    }

    public boolean hasRedstoneCard() {
        return data.get(7) != 0;
    }

    public int powerDraw() {
        return data.get(8);
    }

    // ------------------------------------------------------------------------------------------------------------
    // Buttons
    // ------------------------------------------------------------------------------------------------------------

    @Override
    public boolean clickMenuButton(@NotNull Player player, int id) {
        if (fabricator == null || !(player.level() instanceof ServerLevel level)) {
            return false;
        }

        if (id >= 0 && id < TABS.length) {
            serverTab = id;
        } else if (id == COUNT_DOWN) {
            count = Math.max(1, count - 1);
        } else if (id == COUNT_UP) {
            count = Math.min(FabricatorBlockEntity.MAX_QUEUE, count + 1);
        } else if (id == COUNT_DOWN_TEN) {
            count = Math.max(1, count - 10);
        } else if (id == COUNT_UP_TEN) {
            count = Math.min(FabricatorBlockEntity.MAX_QUEUE, count + 10);
        } else if (id == COUNT_UP_SIXTEEN) {
            count = Math.min(FabricatorBlockEntity.MAX_QUEUE, count + 16);
        } else if (id >= STORAGE_SCROLL && id <= STORAGE_SCROLL + MAX_STORAGE_SCROLL) {
            storageScroll = id - STORAGE_SCROLL;
            return true;
        } else if (id == PRINT) {
            if (selected != null && !fabricator.isPrinting()) {
                fabricator.start(level, selected, count);
            }
        } else if (id == STOP) {
            fabricator.stop();
        } else if (id >= PRIORITY && id < PRIORITY + MachinePriority.values().length) {
            fabricator.setPriority(MachinePriority.values()[id - PRIORITY]);
        } else if (id >= REDSTONE && id < REDSTONE + MachineUpgrades.RedstoneMode.values().length) {
            if (fabricator.hasRedstoneCard()) {
                fabricator.setRedstoneMode(MachineUpgrades.RedstoneMode.values()[id - REDSTONE]);
            }
        } else if (id >= SELECT && id - SELECT < sentCatalogue.size()) {
            selected = sentCatalogue.get(id - SELECT);
        } else {
            return false;
        }

        // Forces the next broadcast to refresh (see the field - never the bare MIN_VALUE).
        lastRefresh = Long.MIN_VALUE / 2;

        return true;
    }

    // ------------------------------------------------------------------------------------------------------------
    // What the screen shows
    // ------------------------------------------------------------------------------------------------------------

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();

        if (fabricator == null || !(player instanceof ServerPlayer serverPlayer) || !(player.level() instanceof ServerLevel level)) {
            return;
        }

        var now = level.getGameTime();

        if (now - lastRefresh < REFRESH_TICKS) {
            return;
        }

        lastRefresh = now;

        var printables = fabricator.printables(level);
        var catalogue = new ArrayList<S2CFabricatorPayload.Entry>();
        var items = new ArrayList<Item>();

        printables.forEach((item, printable) -> {
            if (items.size() < MAX_CATALOGUE_LINES) {
                items.add(item);
                // Oct 21 - the stack shown is one print's batch, so a recipe that makes 4 shows "4".
                catalogue.add(
                    new S2CFabricatorPayload.Entry(
                        new ItemStack(item, FabricatorPlanner.batchSize(level, item)),
                        printable.network() ? 2 : 1
                    )
                );
            }
        });

        if (selected != null && !printables.containsKey(selected)) {
            selected = null;
        }

        // ONE read of connected storage, used for the materials and the storage list alike.
        var connected = fabricator.connectedContents(level);
        var materials = new ArrayList<S2CFabricatorPayload.Material>();

        if (selected != null) {
            var plan = fabricator.plan(level, selected, connected);
            var lines = new LinkedHashMap<Item, int[]>();

            plan.usedInternal().forEach((item, amount) -> lines.computeIfAbsent(item, key -> new int[3])[0] += amount);
            plan.usedNetwork().forEach((item, amount) -> lines.computeIfAbsent(item, key -> new int[3])[1] += amount);
            plan.missing().forEach((item, amount) -> lines.computeIfAbsent(item, key -> new int[3])[2] += amount);
            lines.forEach(
                (item, amounts) -> materials.add(new S2CFabricatorPayload.Material(new ItemStack(item), amounts[0], amounts[1], amounts[2]))
            );
        }

        var storage = new ArrayList<S2CFabricatorPayload.Entry>();

        if (serverTab == STORAGE) {
            var totals = new LinkedHashMap<Item, List<ItemStack>>();

            for (var entry : connected) {
                var same = totals.computeIfAbsent(entry.stack().getItem(), item -> new ArrayList<>());
                var merged = false;

                for (var held : same) {
                    if (ItemStack.isSameItemSameComponents(held, entry.stack())) {
                        held.setCount((int) Math.min(Integer.MAX_VALUE, (long) held.getCount() + entry.stack().getCount()));
                        merged = true;
                        break;
                    }
                }

                if (!merged) {
                    same.add(entry.stack().copy());
                }
            }

            for (var list : totals.values()) {
                for (var stack : list) {
                    if (storage.size() < MAX_STORAGE_LINES) {
                        storage.add(new S2CFabricatorPayload.Entry(stack.copyWithCount(1), stack.getCount()));
                    }
                }
            }
        }

        var printing = fabricator.target() == null ? ItemStack.EMPTY : new ItemStack(fabricator.target());
        var selectedIndex = selected == null ? -1 : items.indexOf(selected);
        var hash = Objects.hash(
            items,
            selectedIndex,
            materialsHash(materials),
            printing.getItem(),
            entriesHash(storage),
            entriesHash(catalogue)
        );

        if (hash != sentHash) {
            sentHash = hash;
            sentCatalogue = List.copyOf(items);
            Human.MOD.networking()
                .sendToClient(serverPlayer, new S2CFabricatorPayload(containerId, catalogue, selectedIndex, materials, printing, storage));
        }
    }

    private static int materialsHash(List<S2CFabricatorPayload.Material> lines) {
        var hash = 1;

        for (var line : lines) {
            hash = 31 * hash + Objects.hash(line.stack().getItem(), line.internal(), line.network(), line.missing());
        }

        return hash;
    }

    private static int entriesHash(List<S2CFabricatorPayload.Entry> lines) {
        var hash = 1;

        for (var line : lines) {
            hash = 31 * hash + Objects.hash(line.stack().getItem(), line.value());
        }

        return hash;
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
        // Menu slot order (not the block's slot numbers): storage in view, blueprints, output, upgrades, inventory.
        if (index < MENU_FIRST_INVENTORY) {
            if (!moveItemStackTo(stack, MENU_FIRST_INVENTORY, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (shown() == BLUEPRINTS && FabricatorBlockEntity.isBlueprintSlotItem(stack)) {
            if (!moveItemStackTo(stack, MENU_FIRST_BLUEPRINT, MENU_OUTPUT, false)) {
                return ItemStack.EMPTY;
            }
        } else if (shown() == SYSTEM) {
            if (!moveItemStackTo(stack, MENU_FIRST_UPGRADE, MENU_FIRST_INVENTORY, false)) {
                return ItemStack.EMPTY;
            }
        } else if (fabricator != null) {
            // Into ALL 48 internal slots, not just the 32 in view - nothing should depend on where it is scrolled.
            var before = stack.getCount();
            var left = fabricator.insertIntoStorage(stack.copy());

            if (left.getCount() == before) {
                return ItemStack.EMPTY;
            }

            stack.setCount(left.getCount());
        } else if (!moveItemStackTo(stack, 0, STORAGE_SHOWN, false)) {
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
    public boolean stillValid(@NotNull Player player) {
        return fabricator == null
            || (!fabricator.isRemoved() && fabricator.getLevel() == player.level() && player.canInteractWithBlock(
                fabricator.getBlockPos(),
                4.0D
            ));
    }

    private static final class FabricatorContainer implements Container {

        private final FabricatorBlockEntity fabricator;

        private FabricatorContainer(FabricatorBlockEntity fabricator) {
            this.fabricator = fabricator;
        }

        @Override
        public int getContainerSize() {
            return FabricatorBlockEntity.ALL_SLOTS;
        }

        @Override
        public boolean isEmpty() {
            return fabricator.isEmpty();
        }

        @Override
        public @NotNull ItemStack getItem(int slot) {
            return fabricator.getSlot(slot);
        }

        @Override
        public @NotNull ItemStack removeItem(int slot, int amount) {
            var stack = getItem(slot);

            if (stack.isEmpty() || amount <= 0) {
                return ItemStack.EMPTY;
            }

            var taken = stack.split(amount);

            setItem(slot, stack.isEmpty() ? ItemStack.EMPTY : stack);

            return taken;
        }

        @Override
        public @NotNull ItemStack removeItemNoUpdate(int slot) {
            var stack = getItem(slot);

            setItem(slot, ItemStack.EMPTY);

            return stack;
        }

        @Override
        public void setItem(int slot, @NotNull ItemStack stack) {
            fabricator.setSlot(slot, stack);
        }

        @Override
        public void setChanged() {
            fabricator.setChanged();
        }

        @Override
        public boolean stillValid(@NotNull Player player) {
            return !fabricator.isRemoved();
        }

        @Override
        public void clearContent() {}
    }
}
