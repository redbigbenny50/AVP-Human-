package com.human.fabric.fluid;

import com.human.common.gameplay.fluid.FluidContent;
import com.human.common.gameplay.fluid.pipe.PipeFluidAccess;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.Nullable;

/**
 * Other mods' fluid blocks for our pipes, on Fabric: the Transfer API. Fabric counts fluid in droplets -
 * {@link FluidConstants#BUCKET} to a bucket, 81 to a millibucket - so every amount is converted, and only whole
 * millibuckets are ever moved. Each move runs in its own transaction, committed only when it is real.
 * <p>
 * A foreign face's colour comes from what it can do NOW: only give (red), only take (green), both (yellow).
 * </p>
 */
public final class FabricPipeFluidAccess implements PipeFluidAccess {

    private static final long DROPLETS_PER_MB = FluidConstants.BUCKET / 1000;

    private FabricPipeFluidAccess() {}

    public static void install() {
        PipeFluidAccess.Holder.install(new FabricPipeFluidAccess());
    }

    @Override
    public @Nullable Port openForeign(ServerLevel level, BlockPos pos, Direction side) {
        var storage = FluidStorage.SIDED.find(level, pos, side);

        if (storage == null) {
            return null;
        }

        return new Port() {

            @Override
            public boolean present() {
                return true;
            }

            @Override
            public boolean canFill() {
                return storage.supportsInsertion() && insert(storage, probeFluid(storage), 1, true) > 0;
            }

            @Override
            public boolean canDrain() {
                return storage.supportsExtraction() && !peek(1).isEmpty();
            }

            @Override
            public FluidContent peek(int max) {
                for (var view : storage.nonEmptyViews()) {
                    var variant = view.getResource();
                    var available = extract(storage, variant.getFluid(), max, true);

                    if (available > 0) {
                        return FluidContent.of(variant.getFluid(), available);
                    }
                }

                return FluidContent.EMPTY;
            }

            @Override
            public int fill(Fluid fluid, int amount, boolean simulate) {
                return insert(storage, fluid, amount, simulate);
            }

            @Override
            public int drain(Fluid fluid, int amount, boolean simulate) {
                return extract(storage, fluid, amount, simulate);
            }

            @Override
            public Object storage() {
                return pos.immutable();
            }
        };
    }

    private static Fluid probeFluid(Storage<FluidVariant> storage) {
        for (var view : storage.nonEmptyViews()) {
            return view.getResource().getFluid();
        }

        return Fluids.WATER;
    }

    /** Measure first (the transaction is closed uncommitted, which aborts it), then move exactly whole millibuckets. */
    private static int insert(Storage<FluidVariant> storage, Fluid fluid, int mb, boolean simulate) {
        if (mb <= 0 || Transaction.isOpen()) {
            return 0;
        }

        long allowed;

        try (var measure = Transaction.openOuter()) {
            allowed = storage.insert(FluidVariant.of(fluid), mb * DROPLETS_PER_MB, measure) / DROPLETS_PER_MB;
        }

        if (simulate || allowed <= 0) {
            return (int) allowed;
        }

        try (var move = Transaction.openOuter()) {
            var inserted = storage.insert(FluidVariant.of(fluid), allowed * DROPLETS_PER_MB, move);

            move.commit();

            return (int) (inserted / DROPLETS_PER_MB);
        }
    }

    private static int extract(Storage<FluidVariant> storage, Fluid fluid, int mb, boolean simulate) {
        if (mb <= 0 || Transaction.isOpen()) {
            return 0;
        }

        long allowed;

        try (var measure = Transaction.openOuter()) {
            allowed = storage.extract(FluidVariant.of(fluid), mb * DROPLETS_PER_MB, measure) / DROPLETS_PER_MB;
        }

        if (simulate || allowed <= 0) {
            return (int) allowed;
        }

        try (var move = Transaction.openOuter()) {
            var extracted = storage.extract(FluidVariant.of(fluid), allowed * DROPLETS_PER_MB, move);

            move.commit();

            return (int) (extracted / DROPLETS_PER_MB);
        }
    }
}
