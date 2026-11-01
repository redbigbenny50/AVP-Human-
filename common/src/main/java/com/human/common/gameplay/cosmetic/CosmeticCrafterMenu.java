package com.human.common.gameplay.cosmetic;

import com.human.common.gameplay.fabrication.FactionLicenseItem;
import com.human.common.registry.init.HumanCosmetics;
import net.minecraft.server.level.ServerLevel;
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
 * The Cosmetic Crafter's screen ([stated] Oct 31 approved mockup, 320 x 248): SETS, CATALOGUE, the selected design
 * with its STYLE drawer (one license slot per faction row) and materials, then INPUT + NETWORK, the inventory, and
 * CRAFT with its output and power gauge.
 * <p>
 * Everything the screen needs from the server rides in this menu's data slots - no extra packet. The catalogue itself
 * is the same Java table on both sides, so an entry is just its index. Every choice goes through vanilla's menu-button
 * packet and the server range-checks it.
 * </p>
 */
public class CosmeticCrafterMenu extends AbstractContainerMenu {

    // Button ids.
    public static final int COUNT_DOWN = 10;

    public static final int COUNT_UP = 11;

    public static final int COUNT_DOWN_TEN = 12;

    public static final int COUNT_UP_TEN = 13;

    public static final int CRAFT = 14;

    public static final int STOP = 15;

    /** + an index into {@link CosmeticCatalog#entries()}. */
    public static final int SELECT = 1000;

    // Slot positions (the item's top-left; each slot's frame is drawn 1px outside it).
    public static final int INPUT_X = 8;

    public static final int INPUT_Y = 165;

    public static final int INVENTORY_X = 79;

    public static final int INVENTORY_Y = 165;

    public static final int HOTBAR_Y = 223;

    public static final int OUTPUT_X = 263;

    public static final int OUTPUT_Y = 206;

    /** License slots sit at the right of their style's drawer row. */
    public static final int LICENSE_X = 287;

    public static final int DRAWER_TOP = 88;

    public static final int DRAWER_ROW = 19;

    public static int licenseY(CosmeticStyle style) {
        return DRAWER_TOP + 2 + style.ordinal() * DRAWER_ROW + 1;
    }

    public static final int MENU_FIRST_OUTPUT = CosmeticCrafterBlockEntity.INPUT_SLOTS;

    public static final int MENU_FIRST_LICENSE = MENU_FIRST_OUTPUT + CosmeticCrafterBlockEntity.OUTPUT_SLOTS;

    public static final int MENU_FIRST_INVENTORY = MENU_FIRST_LICENSE + CosmeticCrafterBlockEntity.LICENSE_SLOTS;

    private static final int REFRESH_TICKS = 10;

    /**
     * Data slots: 0 progress, 1 status, 2 queued, 3 count, 4 running, 5 draw, 6 selected, 7 linked, 8 licenses,
     * 9 the machine's edition, 10+ have.
     */
    private static final int DATA_HAVE = 10;

    private static final int DATA_SIZE = DATA_HAVE + CosmeticCatalog.MAX_PARTS;

    private final ContainerData data;

    private final @Nullable CosmeticCrafterBlockEntity crafter;

    private final Player player;

    private int count = 1;

    private int selected = -1;

    private int linked;

    private int licenses = 1;

    private final int[] have = new int[CosmeticCatalog.MAX_PARTS];

    /** Client side: the style drawer is open, which is when the license slots exist on screen. */
    public boolean drawerOpen;

    private long lastRefresh = Long.MIN_VALUE / 2;

    public CosmeticCrafterMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, null);
    }

    public CosmeticCrafterMenu(int containerId, Inventory inventory, @Nullable CosmeticCrafterBlockEntity crafter) {
        super(HumanCosmetics.CRAFTER_MENU.get(), containerId);
        this.crafter = crafter;
        this.player = inventory.player;
        this.data = crafter == null ? new SimpleContainerData(DATA_SIZE) : new ContainerData() {

            @Override
            public int get(int index) {
                return switch (index) {
                    case 0 -> crafter.progressPercent();
                    case 1 -> crafter.status().ordinal();
                    case 2 -> crafter.queued();
                    case 3 -> count;
                    case 4 -> crafter.isRunning() ? 1 : 0;
                    case 5 -> (int) Math.min(Short.MAX_VALUE, CosmeticCrafterBlockEntity.CRAFT_POWER);
                    case 6 -> selected;
                    case 7 -> linked;
                    case 8 -> licenses;
                    case 9 -> crafter.getBlockState().getBlock() instanceof CosmeticCrafterBlock block ? block.edition().ordinal() : 0;
                    // ⚠ Vanilla sends data slots as SHORTS - a count past 32767 would wrap negative, so it is capped.
                    default -> Math.min(Short.MAX_VALUE, have[index - DATA_HAVE]);
                };
            }

            @Override
            public void set(int index, int value) {}

            @Override
            public int getCount() {
                return DATA_SIZE;
            }
        };

        Container container = crafter == null ? new SimpleContainer(CosmeticCrafterBlockEntity.ALL_SLOTS) : new CrafterContainer(crafter);

        for (int slot = 0; slot < CosmeticCrafterBlockEntity.INPUT_SLOTS; slot++) {
            addSlot(new Slot(container, slot, INPUT_X + (slot % 3) * 18, INPUT_Y + (slot / 3) * 18) {

                @Override
                public boolean mayPlace(@NotNull ItemStack stack) {
                    return !(stack.getItem() instanceof FactionLicenseItem);
                }
            });
        }

        for (int slot = 0; slot < CosmeticCrafterBlockEntity.OUTPUT_SLOTS; slot++) {
            addSlot(
                new Slot(container, CosmeticCrafterBlockEntity.FIRST_OUTPUT + slot, OUTPUT_X + (slot % 2) * 18, OUTPUT_Y + (slot / 2) * 18) {

                    @Override
                    public boolean mayPlace(@NotNull ItemStack stack) {
                        return false;
                    }
                }
            );
        }

        for (var style : CosmeticStyle.values()) {
            var slotIndex = CosmeticCrafterBlockEntity.licenseSlot(style);

            if (slotIndex < 0) {
                continue;
            }

            addSlot(new Slot(container, slotIndex, LICENSE_X, licenseY(style)) {

                @Override
                public boolean mayPlace(@NotNull ItemStack stack) {
                    return CosmeticCrafterBlockEntity.mayPlaceLicense(slotIndex, stack);
                }

                @Override
                public int getMaxStackSize() {
                    return 1;
                }

                /** Only while the drawer shows them on the client; always on the server, so shift-click works. */
                @Override
                public boolean isActive() {
                    return !player.level().isClientSide || drawerOpen;
                }
            });
        }

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9, INVENTORY_X + column * 18, INVENTORY_Y + row * 18));
            }
        }

        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, INVENTORY_X + column * 18, HOTBAR_Y));
        }

        addDataSlots(data);
    }

    // ------------------------------------------------------------------------------------------------------------
    // Synced values
    // ------------------------------------------------------------------------------------------------------------

    public int progress() {
        return data.get(0);
    }

    public CosmeticCrafterBlockEntity.Status status() {
        var values = CosmeticCrafterBlockEntity.Status.values();
        var index = data.get(1);

        return index >= 0 && index < values.length ? values[index] : CosmeticCrafterBlockEntity.Status.IDLE;
    }

    public int queued() {
        return data.get(2);
    }

    public int count() {
        return data.get(3);
    }

    public boolean running() {
        return data.get(4) != 0;
    }

    public int powerDraw() {
        return data.get(5);
    }

    public int selected() {
        return data.get(6);
    }

    public int linked() {
        return data.get(7);
    }

    public boolean unlocked(CosmeticStyle style) {
        return (data.get(8) & (1 << style.ordinal())) != 0;
    }

    /** Which edition of the machine this is - its brand at the screen's top right. */
    public CosmeticStyle edition() {
        var values = CosmeticStyle.values();
        var index = data.get(9);

        return index >= 0 && index < values.length ? values[index] : CosmeticStyle.INDUSTRIAL;
    }

    public int have(int part) {
        return part >= 0 && part < CosmeticCatalog.MAX_PARTS ? data.get(DATA_HAVE + part) : 0;
    }

    // ------------------------------------------------------------------------------------------------------------
    // Buttons
    // ------------------------------------------------------------------------------------------------------------

    @Override
    public boolean clickMenuButton(@NotNull Player player, int id) {
        if (crafter == null || !(player.level() instanceof ServerLevel level)) {
            return false;
        }

        if (id == COUNT_DOWN) {
            count = Math.max(1, count - 1);
        } else if (id == COUNT_UP) {
            count = Math.min(CosmeticCrafterBlockEntity.MAX_QUEUE, count + 1);
        } else if (id == COUNT_DOWN_TEN) {
            count = Math.max(1, count - 10);
        } else if (id == COUNT_UP_TEN) {
            count = Math.min(CosmeticCrafterBlockEntity.MAX_QUEUE, count + 10);
        } else if (id == CRAFT) {
            if (selected >= 0 && !crafter.isCrafting()) {
                crafter.start(level, player, selected, count);
            }
        } else if (id == STOP) {
            crafter.stop();
        } else if (id >= SELECT && id - SELECT < CosmeticCatalog.entries().size()) {
            selected = id - SELECT;
        } else {
            return false;
        }

        lastRefresh = Long.MIN_VALUE / 2;

        return true;
    }

    // ------------------------------------------------------------------------------------------------------------
    // Refresh: ONE storage read, used for the materials, the link count and the licenses
    // ------------------------------------------------------------------------------------------------------------

    @Override
    public void broadcastChanges() {
        if (crafter != null && player.level() instanceof ServerLevel level) {
            var now = level.getGameTime();

            if (now - lastRefresh >= REFRESH_TICKS) {
                lastRefresh = now;
                refresh(level);
            }
        }

        super.broadcastChanges();
    }

    private void refresh(ServerLevel level) {
        if (crafter == null) {
            return;
        }

        // ONE scan of connected storage per refresh, shared by the link count, the licenses and the materials.
        var ports = CosmeticCrafterSources.connected(level, crafter);

        linked = ports.size();
        licenses = crafter.licenseBits(ports, player);

        java.util.Arrays.fill(have, 0);

        var entry = CosmeticCatalog.get(selected);

        if (entry != null) {
            var counts = crafter.have(entry, crafter.connectedContents(ports));

            System.arraycopy(counts, 0, have, 0, Math.min(counts.length, have.length));
        }
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

        if (index < MENU_FIRST_INVENTORY) {
            if (!moveItemStackTo(stack, MENU_FIRST_INVENTORY, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (stack.getItem() instanceof FactionLicenseItem) {
            if (!moveItemStackTo(stack, MENU_FIRST_LICENSE, MENU_FIRST_INVENTORY, false)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, 0, MENU_FIRST_OUTPUT, false)) {
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
        return crafter == null || (!crafter.isRemoved() && crafter.getLevel() == player.level() && player.canInteractWithBlock(
            crafter.getBlockPos(),
            4.0D
        ));
    }

    private static final class CrafterContainer implements Container {

        private final CosmeticCrafterBlockEntity crafter;

        private CrafterContainer(CosmeticCrafterBlockEntity crafter) {
            this.crafter = crafter;
        }

        @Override
        public int getContainerSize() {
            return CosmeticCrafterBlockEntity.ALL_SLOTS;
        }

        @Override
        public boolean isEmpty() {
            return crafter.isEmpty();
        }

        @Override
        public @NotNull ItemStack getItem(int slot) {
            return crafter.getSlot(slot);
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
            crafter.setSlot(slot, stack);
        }

        @Override
        public void setChanged() {
            crafter.setChanged();
        }

        @Override
        public boolean stillValid(@NotNull Player player) {
            return !crafter.isRemoved();
        }

        @Override
        public void clearContent() {}
    }
}
