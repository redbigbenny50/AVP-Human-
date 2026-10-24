package com.human.common.gameplay.storage;

import com.human.common.gameplay.item.pipe.PipeItemAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

/**
 * A crate seen from a pipe. Every block of a crate returns a port on the SAME inventory and the SAME identity (the
 * master's position), so the pipe network treats a 3x3 or a container as one storage, not 27 or 45. It goes straight
 * through the crate's slot index, so it never scans all of a big crate's slots.
 */
final class CratePipePort implements PipeItemAccess.Port {

    private final CrateInventory inventory;

    private final Direction side;

    private final BlockPos identity;

    CratePipePort(CrateInventory inventory, Direction side, BlockPos identity) {
        this.inventory = inventory;
        this.side = side;
        this.identity = identity;
    }

    @Override
    public boolean present() {
        return true;
    }

    @Override
    public boolean canInsert() {
        return PipeItemAccess.defaultIn(side);
    }

    @Override
    public boolean canExtract() {
        return PipeItemAccess.defaultOut(side);
    }

    @Override
    public @Nullable PipeItemAccess.Offer peek(int max, Predicate<ItemStack> allowed) {
        var slot = inventory.findForPipe(allowed);

        if (slot < 0) {
            return null;
        }

        var held = inventory.get(slot);
        // Never hand a pipe more than the item's own stack size: a chest at the other end would otherwise end up
        // holding an illegal stack of 10 swords in one slot.
        var offered = held.copyWithCount(Math.min(Math.min(max, held.getCount()), held.getMaxStackSize()));

        return new PipeItemAccess.Offer() {

            @Override
            public ItemStack stack() {
                return offered;
            }

            @Override
            public int take(int count) {
                // The pipe has ALREADY put these items into the destination, so taking fewer than {@code count}
                // here would duplicate them. Take from the peeked slot if it still holds this exact item, and make
                // up any shortfall from other slots of the same item and components.
                var held = inventory.get(slot);
                var taken = 0;

                if (ItemStack.isSameItemSameComponents(held, offered)) {
                    taken = inventory.extractSlot(slot, count, false).getCount();
                }

                if (taken < count) {
                    taken += inventory.extract(offered, count - taken, false).getCount();
                }

                return taken;
            }
        };
    }

    /** Oct 13 hardening - takes back items the pipe just put in, when their source gave less than promised. */
    @Override
    public int retract(ItemStack like, int count) {
        return inventory.extract(like, count, false).getCount();
    }

    @Override
    public int insert(ItemStack stack) {
        return inventory.insert(stack, false);
    }

    @Override
    public Object storage() {
        return identity;
    }

    /** Oct 13 - the crate's condensed list (each item once, with its total), straight from its item index. */
    @Override
    public void listContents(java.util.function.ObjIntConsumer<net.minecraft.world.item.ItemStack> sink) {
        for (var entry : inventory.summarize()) {
            sink.accept(entry.stack(), (int) Math.min(Integer.MAX_VALUE, entry.count()));
        }
    }
}
