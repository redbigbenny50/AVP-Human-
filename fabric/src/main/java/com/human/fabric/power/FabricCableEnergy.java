package com.human.fabric.power;

import com.human.common.gameplay.block.entity.power.CableBlockEntity;
import com.human.common.gameplay.power.PowerSystem;
import com.human.common.gameplay.power.external.ExposedCableFace;
import com.human.common.registry.init.HumanBlockEntityTypes;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;
import team.reborn.energy.api.EnergyStorage;

/**
 * Our cable's faces as Team Reborn Energy on Fabric - the same as NeoForgeCableEnergy (see {@link ExposedCableFace}).
 * <p>
 * ⚠ Fabric transfers are TRANSACTIONS: a caller may try a transfer and then abort it. So every change first records the
 * face's buffers ({@link SnapshotParticipant}), and an aborted transaction puts them back exactly.
 * </p>
 */
public final class FabricCableEnergy extends SnapshotParticipant<long[]> implements EnergyStorage {

    private final CableBlockEntity cable;

    private final Direction side;

    private @Nullable ExposedCableFace snapshotFace;

    private FabricCableEnergy(CableBlockEntity cable, Direction side) {
        this.cable = cable;
        this.side = side;
    }

    public static void register() {
        EnergyStorage.SIDED.registerForBlockEntity(
            (cable, side) -> side == null ? null : new FabricCableEnergy(cable, side),
            HumanBlockEntityTypes.CABLE.get()
        );
    }

    private @Nullable ExposedCableFace face() {
        if (cable.isRemoved() || !(cable.getLevel() instanceof ServerLevel level)) {
            return null;
        }
        return PowerSystem.get(level).exposedFace(cable.getBlockPos(), side);
    }

    @Override
    public boolean supportsInsertion() {
        var face = face();
        return face != null && face.canReceive();
    }

    @Override
    public long insert(long maxAmount, TransactionContext transaction) {
        var face = face();
        if (face == null || !face.canReceive()) {
            return 0L;
        }
        snapshotFace = face;
        updateSnapshots(transaction);
        return face.receive(maxAmount, false);
    }

    @Override
    public boolean supportsExtraction() {
        var face = face();
        return face != null && face.canExtract();
    }

    @Override
    public long extract(long maxAmount, TransactionContext transaction) {
        var face = face();
        if (face == null || !face.canExtract()) {
            return 0L;
        }
        snapshotFace = face;
        updateSnapshots(transaction);
        return face.extract(maxAmount, false);
    }

    @Override
    public long getAmount() {
        var face = face();
        return face == null ? 0L : face.stored();
    }

    @Override
    public long getCapacity() {
        var face = face();
        return face == null ? 0L : face.capacity();
    }

    @Override
    protected long[] createSnapshot() {
        return snapshotFace == null ? new long[0] : snapshotFace.snapshot();
    }

    @Override
    protected void readSnapshot(long[] snapshot) {
        if (snapshotFace != null && snapshot.length == 3) {
            snapshotFace.restore(snapshot);
        }
    }
}
