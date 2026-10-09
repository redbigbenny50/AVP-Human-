package com.human.common.gameplay.block.entity;

import com.human.common.gameplay.fluid.FluidContent;
import com.human.common.gameplay.item.canister.UniversalCanisterItem;
import com.human.common.registry.init.HumanBlockEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

/**
 * A placed universal canister: the same up-to-8-buckets of any fluid, sitting in the world.
 * <p>
 * The fluid is synced to clients with every change so the renderer can show the fill level through the windows; it is
 * the only thing this block entity holds, so the sync is a few bytes.
 * </p>
 */
public class CanisterBlockEntity extends BlockEntity implements com.human.common.gameplay.fluid.FluidEndpoint {

    private static final String NBT_FLUID = "fluid";

    private static final String NBT_AMOUNT = "amount";

    private FluidContent content = FluidContent.EMPTY;

    public CanisterBlockEntity(BlockPos pos, BlockState state) {
        super(HumanBlockEntityTypes.CANISTER.get(), pos, state);
    }

    @Override
    public FluidContent getContent() {
        return content;
    }

    @Override
    public int getCapacity() {
        return UniversalCanisterItem.CAPACITY;
    }

    /** [stated] Sep 29: pipes feed the TOP. */
    @Override
    public boolean canFill(@org.jetbrains.annotations.Nullable net.minecraft.core.Direction side) {
        return side == null || side == net.minecraft.core.Direction.UP;
    }

    /** [stated] Sep 29: pipes drain the BOTTOM. */
    @Override
    public boolean canDrain(@org.jetbrains.annotations.Nullable net.minecraft.core.Direction side) {
        return side == null || side == net.minecraft.core.Direction.DOWN;
    }

    public void setContent(FluidContent newContent) {
        var clamped = newContent.isEmpty()
            ? FluidContent.EMPTY
            : newContent.withAmount(Math.min(newContent.amount(), UniversalCanisterItem.CAPACITY));

        if (clamped.equals(content)) {
            return;
        }

        content = clamped;
        setChanged();

        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
            // Comparators read the fill level.
            level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
        }
    }

    // ------------------------------------------------------------------------------------------------------------
    // Pipes. [stated] Sep 29: "the input port for pipes is the top the export pipe is the bottom". The loader adapters
    // (NeoForge fluid capability, Fabric Transfer API) expose ONLY these two faces: fill through the top, drain through
    // the bottom. The canister never pushes - a pipe on the bottom pulls.
    // ------------------------------------------------------------------------------------------------------------

    /** How much of this fluid would fit / was put in, in millibuckets. */
    @Override
    public int insert(net.minecraft.world.level.material.Fluid fluid, int maxAmount, boolean simulate) {
        if (maxAmount <= 0 || fluid == null || !content.accepts(fluid)) {
            return 0;
        }

        var accepted = Math.min(maxAmount, UniversalCanisterItem.CAPACITY - content.amount());

        if (accepted > 0 && !simulate) {
            setContent(FluidContent.of(fluid, content.amount() + accepted));
        }

        return Math.max(0, accepted);
    }

    /**
     * How much would come out / came out, in millibuckets.
     *
     * @param fluid the fluid wanted, or null for "whatever is inside"
     */
    @Override
    public int extract(
        @org.jetbrains.annotations.Nullable net.minecraft.world.level.material.Fluid fluid,
        int maxAmount,
        boolean simulate
    ) {
        if (maxAmount <= 0 || content.isEmpty() || fluid != null && !content.fluid().isSame(fluid)) {
            return 0;
        }

        var taken = Math.min(maxAmount, content.amount());

        if (!simulate) {
            setContent(content.withAmount(content.amount() - taken));
        }

        return taken;
    }

    /**
     * ⚠ FOR TRANSACTIONAL CALLERS ONLY (Fabric's Transfer API): changes the content WITHOUT saving or syncing, because
     * a transaction may still be rolled back. The caller must call {@link #notifyContentChanged()} once the change is
     * final.
     */
    @Override
    public void setContentSilently(FluidContent newContent) {
        content = newContent.isEmpty() ? FluidContent.EMPTY : newContent;
    }

    /** Saves and syncs after a {@link #setContentSilently} change has been committed. */
    @Override
    public void notifyContentChanged() {
        setChanged();

        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
            level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
        }
    }

    /** Which of the model's eight fill groups shows: one per started bucket, 0 when empty. */
    public int fillLevel() {
        if (content.isEmpty()) {
            return 0;
        }

        return Math.min(UniversalCanisterItem.CAPACITY_BUCKETS, (content.amount() + FluidContent.BUCKET - 1) / FluidContent.BUCKET);
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.saveAdditional(tag, registries);

        if (!content.isEmpty()) {
            tag.putString(NBT_FLUID, BuiltInRegistries.FLUID.getKey(content.fluid()).toString());
            tag.putInt(NBT_AMOUNT, content.amount());
        }
    }

    @Override
    protected void loadAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.loadAdditional(tag, registries);

        var id = ResourceLocation.tryParse(tag.getString(NBT_FLUID));

        // ⚠ An unknown fluid - its mod removed since the canister was filled - loads as empty rather than failing.
        content = id == null
            ? FluidContent.EMPTY
            : FluidContent.of(BuiltInRegistries.FLUID.get(id), Math.min(tag.getInt(NBT_AMOUNT), UniversalCanisterItem.CAPACITY));
    }

    @Override
    public @NotNull CompoundTag getUpdateTag(HolderLookup.@NotNull Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
