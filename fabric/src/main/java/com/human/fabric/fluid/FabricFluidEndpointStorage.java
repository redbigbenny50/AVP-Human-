package com.human.fabric.fluid;

import com.human.common.gameplay.fluid.FluidContent;
import com.human.common.gameplay.fluid.FluidEndpoint;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;
import net.minecraft.core.Direction;
import org.jetbrains.annotations.Nullable;

/**
 * Any {@link FluidEndpoint} as Fabric Transfer API storage for one face.
 * <h2>TRANSACTIONS</h2> A change is made SILENTLY and snapshotted; only when the outermost transaction commits does the
 * endpoint save and sync. A cancelled transfer leaves no trace, not even a flicker of the fill level.
 * <h2>UNITS</h2> 81 droplets per millibucket, computed in {@code long} - a full 5 x 5 x 32 tank is about 4.15 billion
 * droplets, which would overflow an int. Every transfer is rounded DOWN to whole millibuckets so nothing is silently
 * lost.
 * <h2>FACE RULES</h2> Asked at the moment of each transfer, never captured - see {@link FluidEndpoint}.
 */
public final class FabricFluidEndpointStorage extends SnapshotParticipant<FluidContent> implements SingleSlotStorage<FluidVariant> {

    private static final long DROPLETS_PER_MB = FluidConstants.BUCKET / FluidContent.BUCKET;

    private final FluidEndpoint endpoint;

    @Nullable
    private final Direction side;

    private FabricFluidEndpointStorage(FluidEndpoint endpoint, @Nullable Direction side) {
        this.endpoint = endpoint;
        this.side = side;
    }

    /** The storage for one face, or null when that face is not a port at all. */
    public static @Nullable FabricFluidEndpointStorage forSide(FluidEndpoint endpoint, @Nullable Direction side) {
        return endpoint.isPort(side) ? new FabricFluidEndpointStorage(endpoint, side) : null;
    }

    @Override
    public boolean supportsInsertion() {
        return endpoint.canFill(side);
    }

    @Override
    public boolean supportsExtraction() {
        return endpoint.canDrain(side);
    }

    @Override
    public long insert(FluidVariant resource, long maxAmount, TransactionContext transaction) {
        if (!endpoint.canFill(side) || resource.isBlank() || !resource.getComponents().isEmpty()) {
            return 0L;
        }

        var millibuckets = (int) Math.min(Integer.MAX_VALUE, maxAmount / DROPLETS_PER_MB);
        var accepted = endpoint.insert(resource.getFluid(), millibuckets, true);

        if (accepted <= 0) {
            return 0L;
        }

        var content = endpoint.getContent();

        updateSnapshots(transaction);
        endpoint.setContentSilently(FluidContent.of(resource.getFluid(), content.amount() + accepted));

        return accepted * DROPLETS_PER_MB;
    }

    @Override
    public long extract(FluidVariant resource, long maxAmount, TransactionContext transaction) {
        if (!endpoint.canDrain(side) || resource.isBlank()) {
            return 0L;
        }

        var millibuckets = (int) Math.min(Integer.MAX_VALUE, maxAmount / DROPLETS_PER_MB);
        var taken = endpoint.extract(resource.getFluid(), millibuckets, true);

        if (taken <= 0) {
            return 0L;
        }

        var content = endpoint.getContent();

        updateSnapshots(transaction);
        endpoint.setContentSilently(content.withAmount(content.amount() - taken));

        return taken * DROPLETS_PER_MB;
    }

    @Override
    public boolean isResourceBlank() {
        return endpoint.getContent().isEmpty();
    }

    @Override
    public FluidVariant getResource() {
        var content = endpoint.getContent();

        return content.isEmpty() ? FluidVariant.blank() : FluidVariant.of(content.fluid());
    }

    @Override
    public long getAmount() {
        return endpoint.getContent().amount() * DROPLETS_PER_MB;
    }

    @Override
    public long getCapacity() {
        return endpoint.getCapacity() * DROPLETS_PER_MB;
    }

    @Override
    protected FluidContent createSnapshot() {
        return endpoint.getContent();
    }

    @Override
    protected void readSnapshot(FluidContent snapshot) {
        endpoint.setContentSilently(snapshot);
    }

    @Override
    protected void onFinalCommit() {
        endpoint.notifyContentChanged();
    }
}
