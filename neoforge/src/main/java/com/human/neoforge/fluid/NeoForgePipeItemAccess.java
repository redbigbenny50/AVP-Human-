package com.human.neoforge.fluid;

import com.human.common.gameplay.item.pipe.PipeItemAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import org.jetbrains.annotations.Nullable;

/**
 * Other mods' inventories that are not vanilla-style containers, for our item pipes, on NeoForge: the item-handler
 * capability. Mods do not say which way a face works, so the storage default applies ([stated] top and sides in, bottom
 * out) and the player can change any end with the flange click.
 */
public final class NeoForgePipeItemAccess implements PipeItemAccess {

    /** Oct 13 hardening - most slots one look reads of a huge modded inventory. */
    private static final int MAX_SCAN = 256;

    /** Where the next look at a big inventory starts, per position and face. Bounded, so it never grows. */
    private static final java.util.Map<Long, Integer> CURSORS = java.util.Collections.synchronizedMap(
        new java.util.LinkedHashMap<>(64, 0.75F, true) {

            @Override
            protected boolean removeEldestEntry(java.util.Map.Entry<Long, Integer> eldest) {
                return size() > 4096;
            }
        }
    );

    private NeoForgePipeItemAccess() {}

    public static void install() {
        PipeItemAccess.Holder.install(new NeoForgePipeItemAccess());
    }

    @Override
    public @Nullable Port openForeign(Level level, BlockPos pos, Direction side) {
        var handler = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, side);

        if (handler == null || handler.getSlots() <= 0) {
            return null;
        }

        return new Port() {

            /** Oct 13 - every slot, for the fabricator's storage view. Reading moves nothing. */
            @Override
            public void listContents(java.util.function.ObjIntConsumer<ItemStack> sink) {
                for (int slot = 0; slot < handler.getSlots(); slot++) {
                    var stack = handler.getStackInSlot(slot);

                    if (!stack.isEmpty()) {
                        sink.accept(stack, stack.getCount());
                    }
                }
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
            public @Nullable Offer peek(int max, java.util.function.Predicate<ItemStack> allowed) {
                // [stated] Oct 7 MOST WORN FIRST: a damageable item gives the most damaged copy of that item.
                var found = -1;
                ItemStack best = ItemStack.EMPTY;
                // Oct 13 hardening: an empty slot is skipped with a cheap read, before any simulated extract; and an
                // inventory bigger than MAX_SCAN is looked at one window at a time, carrying on where the last look
                // stopped, so every slot is still reached and no single look reads thousands.
                var slots = handler.getSlots();
                var window = Math.min(slots, MAX_SCAN);
                var key = pos.asLong() * 6 + side.ordinal();
                var first = slots > MAX_SCAN ? Math.floorMod(CURSORS.getOrDefault(key, 0), slots) : 0;

                for (var step = 0; step < window; step++) {
                    var slot = (first + step) % slots;

                    if (handler.getStackInSlot(slot).isEmpty()) {
                        continue;
                    }

                    var simulated = handler.extractItem(slot, max, true);

                    if (simulated.isEmpty() || !allowed.test(simulated)) {
                        continue;
                    }

                    if (found < 0) {
                        found = slot;
                        best = simulated;

                        if (!simulated.isDamageableItem()) {
                            break;
                        }
                    } else if (simulated.is(best.getItem()) && simulated.getDamageValue() > best.getDamageValue()) {
                        found = slot;
                        best = simulated;
                    }
                }

                if (slots > MAX_SCAN) {
                    CURSORS.put(key, found >= 0 ? found : first + window);
                }

                if (found < 0) {
                    return null;
                }

                var offered = best;
                var index = found;

                return new Offer() {

                    @Override
                    public ItemStack stack() {
                        return offered;
                    }

                    @Override
                    public int take(int count) {
                        return handler.extractItem(index, count, false).getCount();
                    }
                };
            }

            @Override
            public int insert(ItemStack stack) {
                return stack.getCount() - ItemHandlerHelper.insertItemStacked(handler, stack.copy(), false).getCount();
            }

            /** Oct 13 hardening - takes back what the pipe just put in, when its source gave less than promised. */
            @Override
            public int retract(ItemStack like, int count) {
                var removed = 0;

                for (var slot = 0; slot < handler.getSlots() && removed < count; slot++) {
                    if (ItemStack.isSameItemSameComponents(handler.getStackInSlot(slot), like)) {
                        removed += handler.extractItem(slot, count - removed, false).getCount();
                    }
                }

                return removed;
            }

            @Override
            public Object storage() {
                return pos.immutable();
            }
        };
    }
}
