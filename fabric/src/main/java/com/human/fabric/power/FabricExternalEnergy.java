package com.human.fabric.power;

import com.human.common.gameplay.power.external.ExternalEnergy;
import net.fabricmc.fabric.api.lookup.v1.block.BlockApiCache;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;
import team.reborn.energy.api.EnergyStorage;

/**
 * Other mods' energy blocks on Fabric: Team Reborn Energy, through a {@link BlockApiCache} per port. Each transfer runs
 * in its own transaction and is committed only when it is real, so a simulated one leaves the block untouched.
 * <p>
 * ⚠ A transaction cannot be opened inside another on the same thread. The grid ticks outside any, but if something ever
 * calls in from within one, the port simply moves nothing that tick rather than throwing.
 * </p>
 */
public final class FabricExternalEnergy implements ExternalEnergy {

    private FabricExternalEnergy() {}

    public static void install() {
        ExternalEnergy.Holder.install(new FabricExternalEnergy());
    }

    @Override
    public Port open(ServerLevel level, BlockPos pos, Direction side) {
        var cache = BlockApiCache.create(EnergyStorage.SIDED, level, pos);

        return new Port() {

            /** Never reaches into an unloaded chunk - the port lives as long as its grid does. */
            private @Nullable EnergyStorage storage() {
                return level.isLoaded(pos) ? cache.find(side) : null;
            }

            @Override
            public boolean present() {
                return storage() != null;
            }

            @Override
            public boolean canReceive() {
                var storage = storage();

                return storage != null && storage.supportsInsertion();
            }

            @Override
            public boolean canExtract() {
                var storage = storage();

                return storage != null && storage.supportsExtraction();
            }

            @Override
            public long receive(long amount, boolean simulate) {
                var storage = storage();

                if (storage == null || amount <= 0L || Transaction.isOpen()) {
                    return 0L;
                }

                try (var transaction = Transaction.openOuter()) {
                    var moved = storage.insert(amount, transaction);

                    if (!simulate) {
                        transaction.commit();
                    }

                    return moved;
                }
            }

            @Override
            public long extract(long amount, boolean simulate) {
                var storage = storage();

                if (storage == null || amount <= 0L || Transaction.isOpen()) {
                    return 0L;
                }

                try (var transaction = Transaction.openOuter()) {
                    var moved = storage.extract(amount, transaction);

                    if (!simulate) {
                        transaction.commit();
                    }

                    return moved;
                }
            }

            @Override
            public long stored() {
                var storage = storage();

                return storage == null ? 0L : storage.getAmount();
            }

            @Override
            public long capacity() {
                var storage = storage();

                return storage == null ? 0L : storage.getCapacity();
            }
        };
    }
}
