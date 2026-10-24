package com.human.common.gameplay.block.creative;

import com.human.common.gameplay.fluid.FluidContent;
import com.human.common.gameplay.fluid.FluidEndpoint;
import com.human.common.registry.init.HumanCreativeBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * [stated] Oct 20 - the Creative Fluid Tank: set to one fluid (right-click with its bucket), it gives an endless supply
 * of that fluid to pipes and empty buckets, and it accepts and destroys any amount of that SAME fluid piped into it.
 * Any other fluid is refused, so a pipe carrying the wrong fluid never drains into it. Unset, it gives and takes
 * nothing (but pipes still connect, ready for when it is set). Creative only.
 */
public class CreativeFluidTankBlockEntity extends BlockEntity implements FluidEndpoint {

    /** What it reports holding: a deep, half-full tank - room to take and plenty to give, as pipes reckon it. */
    private static final int REPORTED_AMOUNT = 1_000_000;

    private static final int REPORTED_CAPACITY = 2_000_000;

    private static final String NBT_FLUID = "Fluid";

    private Fluid fluid = Fluids.EMPTY;

    public CreativeFluidTankBlockEntity(BlockPos pos, BlockState state) {
        super(HumanCreativeBlocks.CREATIVE_FLUID_TANK_BLOCK_ENTITY.get(), pos, state);
    }

    public Fluid fluid() {
        return fluid;
    }

    /** Sets (or, with {@link Fluids#EMPTY}, clears) the fluid it supplies and voids. Server side. */
    public void setFluid(Fluid newFluid) {
        var source = newFluid == Fluids.EMPTY ? Fluids.EMPTY : FluidContent.sourceOf(newFluid);

        if (source == fluid) {
            return;
        }

        fluid = source;
        setChanged();

        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    private boolean matches(@Nullable Fluid other) {
        return fluid != Fluids.EMPTY && other != null && FluidContent.sourceOf(other) == fluid;
    }

    @Override
    public FluidContent getContent() {
        return fluid == Fluids.EMPTY ? FluidContent.EMPTY : FluidContent.of(fluid, REPORTED_AMOUNT);
    }

    @Override
    public int getCapacity() {
        return REPORTED_CAPACITY;
    }

    /** Its own fluid: all of it, destroyed. Anything else: none. */
    @Override
    public int insert(Fluid incoming, int maxAmount, boolean simulate) {
        return matches(incoming) ? Math.max(0, maxAmount) : 0;
    }

    /** Its own fluid: as much as is asked, forever. */
    @Override
    public int extract(@Nullable Fluid wanted, int maxAmount, boolean simulate) {
        if (fluid == Fluids.EMPTY || (wanted != null && !matches(wanted))) {
            return 0;
        }

        return Math.max(0, maxAmount);
    }

    /** Its content never changes by transfer - only by being set. */
    @Override
    public void setContentSilently(FluidContent content) {}

    @Override
    public void notifyContentChanged() {}

    @Override
    public boolean canFill(@Nullable Direction side) {
        return true;
    }

    @Override
    public boolean canDrain(@Nullable Direction side) {
        return true;
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.saveAdditional(tag, registries);

        if (fluid != Fluids.EMPTY) {
            tag.putString(NBT_FLUID, BuiltInRegistries.FLUID.getKey(fluid).toString());
        }
    }

    @Override
    protected void loadAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.loadAdditional(tag, registries);
        var id = ResourceLocation.tryParse(tag.getString(NBT_FLUID));
        fluid = id == null ? Fluids.EMPTY : BuiltInRegistries.FLUID.getOptional(id).orElse(Fluids.EMPTY);
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
