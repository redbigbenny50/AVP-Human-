package com.human.common.gameplay.storage;

import it.unimi.dsi.fastutil.Hash;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.Object2LongLinkedOpenCustomHashMap;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * A crate's slots. Every slot holds up to {@link #SLOT_LIMIT} of anything, including items that normally stack to 1 or
 * 16. A per-item slot index keeps inserts and lookups from scanning every slot, so a 1620-slot container costs a pipe
 * no more than a chest does.
 */
public final class CrateInventory {

    public static final int SLOT_LIMIT = 64;

    /** Item + components equality, the same rule the game uses to stack items. */
    public static final Hash.Strategy<ItemStack> ITEM_AND_COMPONENTS = new Hash.Strategy<>() {

        @Override
        public int hashCode(@Nullable ItemStack stack) {
            return stack == null ? 0 : ItemStack.hashItemAndComponents(stack);
        }

        @Override
        public boolean equals(@Nullable ItemStack a, @Nullable ItemStack b) {
            return a == b || a != null && b != null && ItemStack.isSameItemSameComponents(a, b);
        }
    };

    private final ItemStack[] slots;

    private final BitSet occupied;

    /**
     * The item each slot is indexed under. Kept separately because a stack shrunk to zero in place (Fabric's slot
     * storage does this) reports AIR as its item, and the index must still find and remove the right entry.
     */
    private final Item[] indexed;

    private final Map<Item, IntArrayList> slotsByItem = new HashMap<>();

    private int version;

    private Runnable onChange = () -> {};

    public CrateInventory(int size) {
        slots = new ItemStack[size];
        java.util.Arrays.fill(slots, ItemStack.EMPTY);
        occupied = new BitSet(size);
        indexed = new Item[size];
    }

    public void setOnChange(Runnable onChange) {
        this.onChange = onChange;
    }

    public int size() {
        return slots.length;
    }

    /** Bumped on every change; menus compare it to decide when to resend their list. */
    public int version() {
        return version;
    }

    public ItemStack get(int slot) {
        return slots[slot];
    }

    public boolean isEmpty() {
        return occupied.isEmpty();
    }

    public int usedSlots() {
        return occupied.cardinality();
    }

    public long totalItems() {
        long total = 0;

        for (int slot = occupied.nextSetBit(0); slot >= 0; slot = occupied.nextSetBit(slot + 1)) {
            total += slots[slot].getCount();
        }

        return total;
    }

    /** Full crates never go inside crates, the same rule shulker boxes follow. */
    public static boolean canStore(ItemStack stack) {
        return !stack.isEmpty() && !CrateItem.hasContents(stack);
    }

    /** @return how many of {@code stack} went in (or would, when simulating). The stack is not modified. */
    public int insert(ItemStack stack, boolean simulate) {
        if (!canStore(stack)) {
            return 0;
        }

        int remaining = stack.getCount();
        var existing = slotsByItem.get(stack.getItem());

        if (existing != null) {
            for (int i = 0; i < existing.size() && remaining > 0; i++) {
                var slot = existing.getInt(i);
                var held = slots[slot];

                if (held.getCount() < SLOT_LIMIT && ItemStack.isSameItemSameComponents(held, stack)) {
                    var moved = Math.min(remaining, SLOT_LIMIT - held.getCount());

                    if (!simulate) {
                        held.grow(moved);
                    }

                    remaining -= moved;
                }
            }
        }

        for (int slot = occupied.nextClearBit(0); slot < slots.length && remaining > 0; slot = occupied.nextClearBit(slot + 1)) {
            var moved = Math.min(remaining, SLOT_LIMIT);

            if (!simulate) {
                put(slot, stack.copyWithCount(moved));
            }

            remaining -= moved;

            if (simulate) {
                // A simulated insert cannot mark slots, so count the remaining free slots instead of looping on one.
                var free = slots.length - occupied.cardinality();

                remaining = Math.max(0, remaining - (free - 1) * SLOT_LIMIT);

                break;
            }
        }

        var inserted = stack.getCount() - remaining;

        if (inserted > 0 && !simulate) {
            changed();
        }

        return inserted;
    }

    /** Inserts into one slot (for loader storage APIs). @return what did not fit. */
    public ItemStack insertSlot(int slot, ItemStack stack, boolean simulate) {
        if (!canStore(stack)) {
            return stack;
        }

        var held = slots[slot];

        if (!held.isEmpty() && !ItemStack.isSameItemSameComponents(held, stack)) {
            return stack;
        }

        var space = SLOT_LIMIT - held.getCount();
        var moved = Math.min(space, stack.getCount());

        if (moved <= 0) {
            return stack;
        }

        if (!simulate) {
            if (held.isEmpty()) {
                put(slot, stack.copyWithCount(moved));
            } else {
                held.grow(moved);
            }

            changed();
        }

        return stack.copyWithCount(stack.getCount() - moved);
    }

    /** Takes from one slot, never more than the item's normal stack size, so other mods get legal stacks. */
    public ItemStack extractSlot(int slot, int amount, boolean simulate) {
        var held = slots[slot];

        if (held.isEmpty() || amount <= 0) {
            return ItemStack.EMPTY;
        }

        var taken = Math.min(Math.min(amount, held.getCount()), held.getMaxStackSize());
        var result = held.copyWithCount(taken);

        if (!simulate) {
            shrink(slot, taken);
            changed();
        }

        return result;
    }

    /** Takes up to {@code amount} of items matching {@code prototype} (item and components) from any slots. */
    public ItemStack extract(ItemStack prototype, int amount, boolean simulate) {
        var list = slotsByItem.get(prototype.getItem());

        if (list == null || amount <= 0) {
            return ItemStack.EMPTY;
        }

        int taken = 0;
        var candidates = list.toIntArray();

        for (var slot : candidates) {
            if (taken >= amount) {
                break;
            }

            var held = slots[slot];

            if (ItemStack.isSameItemSameComponents(held, prototype)) {
                var moved = Math.min(amount - taken, held.getCount());

                if (!simulate) {
                    shrink(slot, moved);
                }

                taken += moved;
            }
        }

        if (taken > 0 && !simulate) {
            changed();
        }

        return taken > 0 ? prototype.copyWithCount(taken) : ItemStack.EMPTY;
    }

    public long count(ItemStack prototype) {
        var list = slotsByItem.get(prototype.getItem());
        long total = 0;

        if (list != null) {
            for (int i = 0; i < list.size(); i++) {
                var held = slots[list.getInt(i)];

                if (ItemStack.isSameItemSameComponents(held, prototype)) {
                    total += held.getCount();
                }
            }
        }

        return total;
    }

    /**
     * The slot a pipe should pull from: the first stack the filter allows, and for worn gear the most worn one,
     * matching how pipes pull from chests.
     */
    public int findForPipe(Predicate<ItemStack> allowed) {
        // The filter runs once per DISTINCT stack, not once per slot: a full container holds up to 1620 slots but
        // usually only a handful of distinct items, and filters (tags, hot buttons) are the expensive part.
        for (var entry : slotsByItem.entrySet()) {
            var list = entry.getValue();
            int found = -1;
            ItemStack tested = ItemStack.EMPTY;
            var testedResult = false;

            for (int i = 0; i < list.size(); i++) {
                var slot = list.getInt(i);
                var stack = slots[slot];

                if (tested.isEmpty() || !ItemStack.isSameItemSameComponents(tested, stack)) {
                    tested = stack;
                    testedResult = allowed.test(stack);
                }

                if (!testedResult) {
                    continue;
                }

                if (found < 0) {
                    found = slot;

                    if (!stack.isDamageableItem()) {
                        break;
                    }
                } else if (stack.getDamageValue() > slots[found].getDamageValue()) {
                    // Worn gear goes first, matching how pipes pull from chests.
                    found = slot;
                }
            }

            if (found >= 0) {
                return found;
            }
        }

        return -1;
    }

    /** A copy of the slots currently holding {@code item} (any components). */
    public int[] slotsHolding(Item item) {
        var list = slotsByItem.get(item);

        return list == null ? new int[0] : list.toIntArray();
    }

    /** The first empty slot at or after {@code from}, or -1. */
    public int nextEmpty(int from) {
        var slot = occupied.nextClearBit(Math.max(0, from));

        return slot < slots.length ? slot : -1;
    }

    /** Overwrites a slot (for loader storage APIs that commit a whole stack at once). */
    public void setSlot(int slot, ItemStack stack) {
        if (indexed[slot] != null) {
            unindex(slot);
        }

        slots[slot] = ItemStack.EMPTY;
        occupied.clear(slot);

        if (!stack.isEmpty()) {
            put(slot, stack);
        }

        changed();
    }

    /** One entry per distinct item+components, with the total count, in first-seen order. */
    public List<Entry> summarize() {
        var totals = new Object2LongLinkedOpenCustomHashMap<ItemStack>(ITEM_AND_COMPONENTS);

        for (int slot = occupied.nextSetBit(0); slot >= 0; slot = occupied.nextSetBit(slot + 1)) {
            var stack = slots[slot];

            totals.addTo(stack.copyWithCount(1), stack.getCount());
        }

        var entries = new ArrayList<Entry>(totals.size());

        for (var entry : totals.object2LongEntrySet()) {
            entries.add(new Entry(entry.getKey(), entry.getLongValue()));
        }

        return entries;
    }

    public void clear() {
        java.util.Arrays.fill(slots, ItemStack.EMPTY);
        java.util.Arrays.fill(indexed, null);
        occupied.clear();
        slotsByItem.clear();
        changed();
    }

    public ListTag save(HolderLookup.Provider registries) {
        var list = new ListTag();

        for (int slot = occupied.nextSetBit(0); slot >= 0; slot = occupied.nextSetBit(slot + 1)) {
            var tag = new CompoundTag();

            tag.putInt("Slot", slot);
            list.add(slots[slot].save(registries, tag));
        }

        return list;
    }

    public void load(ListTag list, HolderLookup.Provider registries) {
        java.util.Arrays.fill(slots, ItemStack.EMPTY);
        java.util.Arrays.fill(indexed, null);
        occupied.clear();
        slotsByItem.clear();

        for (int i = 0; i < list.size(); i++) {
            var tag = list.getCompound(i);
            var slot = tag.getInt("Slot");

            if (slot < 0 || slot >= slots.length) {
                continue;
            }

            var stack = ItemStack.parseOptional(registries, tag);

            if (!stack.isEmpty()) {
                put(slot, stack);
            }
        }

        version++;
    }

    /** Copies everything from another inventory, first free slots first. Anything that cannot fit is returned. */
    public List<ItemStack> absorb(CrateInventory other) {
        var leftovers = new ArrayList<ItemStack>();

        for (int slot = other.occupied.nextSetBit(0); slot >= 0; slot = other.occupied.nextSetBit(slot + 1)) {
            var stack = other.slots[slot];
            var moved = insert(stack, false);

            if (moved < stack.getCount()) {
                leftovers.add(stack.copyWithCount(stack.getCount() - moved));
            }
        }

        return leftovers;
    }

    public static ListTag readList(CompoundTag tag, String key) {
        return tag.getList(key, Tag.TAG_COMPOUND);
    }

    private void put(int slot, ItemStack stack) {
        slots[slot] = stack;
        occupied.set(slot);
        indexed[slot] = stack.getItem();
        slotsByItem.computeIfAbsent(stack.getItem(), item -> new IntArrayList()).add(slot);
    }

    private void shrink(int slot, int amount) {
        var held = slots[slot];

        if (amount >= held.getCount()) {
            unindex(slot);
            slots[slot] = ItemStack.EMPTY;
            occupied.clear(slot);
        } else {
            held.shrink(amount);
        }
    }

    private void unindex(int slot) {
        var item = indexed[slot];

        if (item == null) {
            return;
        }

        var list = slotsByItem.get(item);

        if (list != null) {
            list.rem(slot);

            if (list.isEmpty()) {
                slotsByItem.remove(item);
            }
        }

        indexed[slot] = null;
    }

    private void changed() {
        version++;
        onChange.run();
    }

    /** A distinct stored item (count 1) and how many of it the crate holds. */
    public record Entry(
        ItemStack stack,
        long count
    ) {}
}
