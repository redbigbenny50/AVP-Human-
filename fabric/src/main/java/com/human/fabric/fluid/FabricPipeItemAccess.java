package com.human.fabric.fluid;

import com.human.common.gameplay.item.pipe.PipeItemAccess;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * Other mods' inventories that are not vanilla-style containers, for our item pipes, on Fabric: the Transfer API. Each
 * move runs in its own transaction, committed only when it is real. The storage default applies ([stated] top and sides
 * in, bottom out); the flange click changes any end.
 */
public final class FabricPipeItemAccess implements PipeItemAccess {

    /** Oct 13 hardening - most entries one look reads, so a huge modded storage cannot stall a pipe round. */
    private static final int MAX_VIEWS = 512;

    private FabricPipeItemAccess() {}

    public static void install() {
        PipeItemAccess.Holder.install(new FabricPipeItemAccess());
    }

    @Override
    public @Nullable Port openForeign(Level level, BlockPos pos, Direction side) {
        var storage = ItemStorage.SIDED.find(level, pos, side);

        if (storage == null) {
            return null;
        }

        return new Port() {

            /** Oct 13 - every non-empty view, for the fabricator's storage view. Reading moves nothing. */
            @Override
            public void listContents(java.util.function.ObjIntConsumer<ItemStack> sink) {
                for (var view : storage.nonEmptyViews()) {
                    sink.accept(view.getResource().toStack(), (int) Math.min(Integer.MAX_VALUE, view.getAmount()));
                }
            }

            @Override
            public boolean present() {
                return true;
            }

            @Override
            public boolean canInsert() {
                return storage.supportsInsertion() && PipeItemAccess.defaultIn(side);
            }

            @Override
            public boolean canExtract() {
                return storage.supportsExtraction() && PipeItemAccess.defaultOut(side);
            }

            @Override
            public @Nullable Offer peek(int max, java.util.function.Predicate<ItemStack> allowed) {
                if (Transaction.isOpen()) {
                    return null;
                }

                // [stated] Oct 7 MOST WORN FIRST: a damageable item gives the most damaged copy of that item. Found
                // first, then offered - same measuring as before, just aimed at the right variant.
                net.fabricmc.fabric.api.transfer.v1.item.ItemVariant chosen = null;
                var chosenDamage = -1;

                // Oct 13 hardening: one look reads at most MAX_VIEWS entries of a huge modded storage.
                var looked = 0;

                for (var view : storage.nonEmptyViews()) {
                    if (++looked > MAX_VIEWS) {
                        break;
                    }

                    var candidate = view.getResource();
                    var stack = candidate.toStack();

                    if (!allowed.test(stack)) {
                        continue;
                    }

                    if (chosen == null) {
                        chosen = candidate;
                        chosenDamage = stack.getDamageValue();

                        if (!stack.isDamageableItem()) {
                            break;
                        }
                    } else if (candidate.getItem() == chosen.getItem() && stack.getDamageValue() > chosenDamage) {
                        chosen = candidate;
                        chosenDamage = stack.getDamageValue();
                    }
                }

                if (chosen != null) {
                    var variant = chosen;

                    long available;

                    try (var measure = Transaction.openOuter()) {
                        available = storage.extract(variant, max, measure);
                    }

                    if (available > 0) {
                        var offered = variant.toStack((int) available);

                        return new Offer() {

                            @Override
                            public ItemStack stack() {
                                return offered;
                            }

                            @Override
                            public int take(int count) {
                                try (var move = Transaction.openOuter()) {
                                    var took = storage.extract(variant, count, move);
                                    move.commit();
                                    return (int) Math.min(Integer.MAX_VALUE, took);
                                }
                            }
                        };
                    }
                }

                // The chosen copy could not be taken out (a locked slot, say): offer the first one that can, as before.
                var scanned = 0;

                for (var view : storage.nonEmptyViews()) {
                    if (++scanned > MAX_VIEWS) {
                        break;
                    }

                    var variant = view.getResource();

                    if (!allowed.test(variant.toStack())) {
                        continue;
                    }

                    long available;

                    try (var measure = Transaction.openOuter()) {
                        available = storage.extract(variant, max, measure);
                    }

                    if (available > 0) {
                        var offered = variant.toStack((int) available);

                        return new Offer() {

                            @Override
                            public ItemStack stack() {
                                return offered;
                            }

                            @Override
                            public int take(int count) {
                                try (var move = Transaction.openOuter()) {
                                    var took = storage.extract(variant, count, move);
                                    move.commit();
                                    return (int) Math.min(Integer.MAX_VALUE, took);
                                }
                            }
                        };
                    }
                }

                return null;
            }

            /** Oct 13 hardening - takes back what the pipe just put in, when its source gave less than promised. */
            @Override
            public int retract(ItemStack like, int count) {
                if (like.isEmpty() || count <= 0 || Transaction.isOpen()) {
                    return 0;
                }

                try (var move = Transaction.openOuter()) {
                    var took = storage.extract(ItemVariant.of(like), count, move);
                    move.commit();
                    return (int) Math.min(Integer.MAX_VALUE, took);
                }
            }

            @Override
            public int insert(ItemStack stack) {
                if (stack.isEmpty() || Transaction.isOpen()) {
                    return 0;
                }

                try (var move = Transaction.openOuter()) {
                    var inserted = storage.insert(ItemVariant.of(stack), stack.getCount(), move);

                    move.commit();

                    return (int) inserted;
                }
            }

            @Override
            public Object storage() {
                return pos.immutable();
            }
        };
    }
}
