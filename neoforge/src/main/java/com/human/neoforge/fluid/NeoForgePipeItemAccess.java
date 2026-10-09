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

                for (var slot = 0; slot < handler.getSlots(); slot++) {
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
                    public void take(int count) {
                        handler.extractItem(index, count, false);
                    }
                };
            }

            @Override
            public int insert(ItemStack stack) {
                return stack.getCount() - ItemHandlerHelper.insertItemStacked(handler, stack.copy(), false).getCount();
            }

            @Override
            public Object storage() {
                return pos.immutable();
            }
        };
    }
}
