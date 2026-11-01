package com.human.common.gameplay.gene.machine.storage;

import com.human.common.gameplay.gene.tube.GeneTubeItem;
import com.human.common.registry.init.HumanGeneTubes;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;

/**
 * One Gene Storage's rack ([stated] Oct 27/28): 90 slots - 10 rows of 9 - holding FILLED gene tubes only.
 * <p>
 * Sorting ([stated] Oct 28 screen): A-Z and Z-A by gene name, DOSE groups the same gene and puts the strongest first;
 * AUTO SORT re-sorts on every insert and remove so the rack (and the carousel, which shows the first 32 slots) always
 * stays in order. Filled tubes stack to 16 when gene, value and donor match - the item handles that, so inserting fills
 * matching stacks first, then empty slots.
 * </p>
 */
public class GeneStorageInventory extends SimpleContainer {

    public static final int ROWS = 10;

    public static final int COLUMNS = 9;

    public static final int SIZE = ROWS * COLUMNS;

    /** How many rack slots the carousel shows ([stated] Oct 29: its 32 holders show only what fits). */
    public static final int CAROUSEL = 32;

    public enum SortMode {
        A_Z,
        Z_A,
        DOSE
    }

    private boolean autoSort;

    private SortMode autoSortMode = SortMode.DOSE;

    private boolean sorting;

    public GeneStorageInventory() {
        super(SIZE);
    }

    public static boolean accepts(ItemStack stack) {
        return stack.is(HumanGeneTubes.GENE_TUBE.get()) && GeneTubeItem.read(stack) != null;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return accepts(stack);
    }

    /**
     * Puts as much of {@code stack} in as fits - matching stacks first, then empty slots - and returns what did not
     * fit. The argument is not modified.
     */
    public ItemStack insert(ItemStack stack) {
        if (!accepts(stack)) {
            return stack;
        }
        var remaining = stack.copy();
        for (int i = 0; i < SIZE && !remaining.isEmpty(); i++) {
            var here = getItem(i);
            if (!here.isEmpty() && ItemStack.isSameItemSameComponents(here, remaining)) {
                var move = Math.min(remaining.getCount(), here.getMaxStackSize() - here.getCount());
                if (move > 0) {
                    here.grow(move);
                    remaining.shrink(move);
                }
            }
        }
        for (int i = 0; i < SIZE && !remaining.isEmpty(); i++) {
            if (getItem(i).isEmpty()) {
                var move = Math.min(remaining.getCount(), remaining.getMaxStackSize());
                setItem(i, remaining.split(move));
            }
        }
        setChanged();
        return remaining;
    }

    /** Free room for one more tube of any kind (an empty slot, or room on a matching stack). */
    public boolean hasRoomFor(ItemStack stack) {
        for (int i = 0; i < SIZE; i++) {
            var here = getItem(i);
            if (here.isEmpty()) {
                return true;
            }
            if (ItemStack.isSameItemSameComponents(here, stack) && here.getCount() < here.getMaxStackSize()) {
                return true;
            }
        }
        return false;
    }

    public int tubeCount() {
        var count = 0;
        for (int i = 0; i < SIZE; i++) {
            count += getItem(i).getCount();
        }
        return count;
    }

    /** The last sort chosen - the lit SORT chip, and the order AUTO SORT keeps. */
    public SortMode sortMode() {
        return autoSortMode;
    }

    public void sort(SortMode mode) {
        autoSortMode = mode;
        var tubes = new ArrayList<ItemStack>();
        for (int i = 0; i < SIZE; i++) {
            if (!getItem(i).isEmpty()) {
                tubes.add(getItem(i).copy());
            }
        }
        Comparator<ItemStack> byName = Comparator.comparing(s -> {
            var c = GeneTubeItem.read(s);
            return c == null ? "" : c.gene().getPath();
        });
        Comparator<ItemStack> order = switch (mode) {
            case A_Z -> byName;
            case Z_A -> byName.reversed();
            // Same gene together, the strongest first.
            case DOSE -> byName.thenComparing(
                Comparator.comparingDouble((ItemStack s) -> {
                    var c = GeneTubeItem.read(s);
                    return c == null ? 0.0 : Math.abs(c.value());
                }).reversed()
            );
        };
        tubes.sort(order);
        sorting = true;
        try {
            for (int i = 0; i < SIZE; i++) {
                setItem(i, i < tubes.size() ? tubes.get(i) : ItemStack.EMPTY);
            }
        } finally {
            sorting = false;
        }
        super.setChanged();
    }

    public boolean isAutoSort() {
        return autoSort;
    }

    public void setAutoSort(boolean autoSort, SortMode mode) {
        this.autoSort = autoSort;
        this.autoSortMode = mode;
        if (autoSort) {
            sort(mode);
        }
    }

    @Override
    public void setChanged() {
        // AUTO SORT keeps the rack in order on every change - but never re-enters while it is sorting.
        if (autoSort && !sorting) {
            sort(autoSortMode);
            return;
        }
        super.setChanged();
    }

    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        var items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
        for (int i = 0; i < SIZE; i++) {
            items.set(i, getItem(i));
        }
        ContainerHelper.saveAllItems(tag, items, registries);
        tag.putBoolean("AutoSort", autoSort);
        tag.putString("AutoSortMode", autoSortMode.name());
        return tag;
    }

    public void load(CompoundTag tag, HolderLookup.Provider registries) {
        var items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, items, registries);
        sorting = true;
        try {
            for (int i = 0; i < SIZE; i++) {
                setItem(i, items.get(i));
            }
        } finally {
            sorting = false;
        }
        autoSort = tag.getBoolean("AutoSort");
        try {
            autoSortMode = SortMode.valueOf(tag.getString("AutoSortMode"));
        } catch (IllegalArgumentException ignored) {
            autoSortMode = SortMode.DOSE;
        }
    }
}
