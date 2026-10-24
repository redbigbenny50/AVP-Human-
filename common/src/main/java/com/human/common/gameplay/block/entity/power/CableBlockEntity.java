package com.human.common.gameplay.block.entity.power;

import com.human.common.registry.init.HumanBlockEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

/**
 * A cable that meets a block. It carries no power itself - the grid does that - it only remembers each face's flange
 * colour so {@link com.human.client.render.block.CableFlangeRenderer} can draw it: [stated] green = power into our
 * system, red = out of it, yellow = both. Plain cable-to-cable runs have no block entity at all.
 */
public class CableBlockEntity extends BlockEntity implements com.human.common.gameplay.ship.ShipMoveAware {

    public static final byte IN = 1;

    public static final byte OUT = 2;

    public static final byte BOTH = 3;

    private final byte[] modes = new byte[6];

    public CableBlockEntity(BlockPos pos, BlockState state) {
        super(HumanBlockEntityTypes.CABLE.get(), pos, state);
    }

    /** [stated] Oct 21 - landed from a ship rotated: each flange's mode turns to the face it now points at. */
    @Override
    public void onShipMove(com.human.common.gameplay.ship.ShipMove move) {
        if (!move.rotated()) {
            return;
        }

        move.rotateFaces(modes);
        setChanged();

        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public byte getMode(Direction face) {
        return modes[face.get3DDataValue()];
    }

    public void setMode(Direction face, byte mode) {
        if (modes[face.get3DDataValue()] == mode) {
            return;
        }

        modes[face.get3DDataValue()] = mode;
        setChanged();

        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putByteArray("modes", modes);
    }

    @Override
    protected void loadAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.loadAdditional(tag, registries);

        var saved = tag.getByteArray("modes");

        System.arraycopy(saved, 0, modes, 0, Math.min(saved.length, modes.length));
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public @NotNull CompoundTag getUpdateTag(HolderLookup.@NotNull Provider registries) {
        return saveWithoutMetadata(registries);
    }
}
