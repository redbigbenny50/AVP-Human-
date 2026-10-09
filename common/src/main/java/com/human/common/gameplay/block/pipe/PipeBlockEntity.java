package com.human.common.gameplay.block.pipe;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

/**
 * A pipe that touches a port - fluid or item. It holds nothing ([stated] pipes are dumb). Per face it keeps:
 * <ul>
 * <li>the MODE its flange shows - {@link #IN} green (the pipe puts things into the block), {@link #OUT} red (it takes
 * them out), {@link #BOTH} yellow - worked out by its network;</li>
 * <li>the player's SETTING for that end ([stated] Sep 30, the flange click): {@link #AUTO}, {@link #PULL},
 * {@link #PUSH}, {@link #SET_BOTH} or {@link #OFF}.</li>
 * </ul>
 * {@link #effective} turns what the block allows plus the setting into the end's working mode, and both networks use
 * it.
 * <h2>FILTERS</h2> Each end has a {@link PipeFilter} (Oct 8): any number of entries - exact items or fluids, tags,
 * whole mods - an allow / block switch and a priority. With no entries everything passes, as with the machine filter.
 */
public abstract class PipeBlockEntity extends BlockEntity {

    public static final byte IN = 1;

    public static final byte OUT = 2;

    public static final byte BOTH = 3;

    public static final byte AUTO = 0;

    public static final byte PULL = 1;

    public static final byte PUSH = 2;

    public static final byte SET_BOTH = 3;

    public static final byte OFF = 4;

    private final byte[] modes = new byte[6];

    private final byte[] overrides = new byte[6];

    /**
     * Oct 8 - each end's filter: any number of entries (items, tags, mods, each with a "Not enchanted" toggle), allow
     * or block, and a priority. See {@link PipeFilter}. Replaces the nine fixed slots; old saves load into it
     * unchanged.
     */
    private final PipeFilter[] filters = new PipeFilter[6];

    protected PipeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public byte getMode(Direction face) {
        return modes[face.get3DDataValue()];
    }

    /** Set by the network as it looks at its ports; only a real change is saved and sent. */
    public void setMode(Direction face, byte mode) {
        if (modes[face.get3DDataValue()] != mode) {
            modes[face.get3DDataValue()] = mode;
            changed();
        }
    }

    /** @return this end's live filter (never null) */
    public PipeFilter getFilter(Direction face) {
        var filter = filters[face.get3DDataValue()];

        if (filter == null) {
            filter = new PipeFilter();
            filters[face.get3DDataValue()] = filter;
        }

        return filter;
    }

    /** Replaces this end's filter (a copy is stored) and tells clients. */
    public void setFilter(Direction face, PipeFilter filter) {
        filters[face.get3DDataValue()] = filter.copy();
        changed();
    }

    /** Adds one line to this end's filter and tells clients. */
    public void addFilterEntry(Direction face, PipeFilter.Entry entry) {
        if (getFilter(face).add(entry)) {
            changed();
        }
    }

    /** @return whether this end lets the item through */
    public boolean allowsItem(Direction face, net.minecraft.world.item.ItemStack stack) {
        var filter = filters[face.get3DDataValue()];
        return filter == null || filter.allowsItem(stack);
    }

    /** @return whether this end lets the fluid through */
    public boolean allowsFluid(Direction face, net.minecraft.world.level.material.Fluid fluid) {
        var filter = filters[face.get3DDataValue()];
        return filter == null || filter.allowsFluid(fluid);
    }

    /** @return this end's routing priority, resolving Auto by what it touches */
    public byte priority(Direction face, boolean machine) {
        var filter = filters[face.get3DDataValue()];
        return filter == null
            ? (machine ? PipeFilter.PRIORITY_NORMAL : PipeFilter.PRIORITY_LOW)
            : filter.resolvePriority(machine);
    }

    public byte getOverride(Direction face) {
        return overrides[face.get3DDataValue()];
    }

    /** The flange click: Auto, Pull, Push, Both, Off, and round again. @return the new setting */
    /** The pipe-end screen's mode buttons. */
    public void setOverride(Direction face, byte setting) {
        if (setting >= AUTO && setting <= OFF && overrides[face.get3DDataValue()] != setting) {
            overrides[face.get3DDataValue()] = setting;
            changed();
        }
    }

    public byte cycleOverride(Direction face) {
        var next = (byte) ((overrides[face.get3DDataValue()] + 1) % 5);

        overrides[face.get3DDataValue()] = next;
        changed();

        return next;
    }

    public static String overrideName(byte setting) {
        return switch (setting) {
            case PULL -> "pull";
            case PUSH -> "push";
            case SET_BOTH -> "both";
            case OFF -> "off";
            default -> "auto";
        };
    }

    /**
     * An end's working mode: what the block allows on that face ({@code canIn} - the pipe may put things in;
     * {@code canOut} - it may take them out), narrowed by the player's setting. 0 = this end does nothing.
     */
    public static byte effective(byte setting, boolean canIn, boolean canOut) {
        return switch (setting) {
            case OFF -> 0;
            case PULL -> canOut ? OUT : 0;
            case PUSH -> canIn ? IN : 0;
            default -> canIn && canOut ? BOTH : canIn ? IN : canOut ? OUT : 0;
        };
    }

    private void changed() {
        setChanged();

        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putByteArray("modes", modes);
        tag.putByteArray("ends", overrides);

        var list = new net.minecraft.nbt.ListTag();

        for (var face = 0; face < 6; face++) {
            var entry = new CompoundTag();

            if (filters[face] != null) {
                filters[face].save(entry);
            }

            list.add(entry);
        }

        tag.put("filters", list);
    }

    @Override
    protected void loadAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.loadAdditional(tag, registries);

        var saved = tag.getByteArray("modes");
        var ends = tag.getByteArray("ends");

        System.arraycopy(saved, 0, modes, 0, Math.min(saved.length, modes.length));
        System.arraycopy(ends, 0, overrides, 0, Math.min(ends.length, overrides.length));

        var list = tag.getList("filters", net.minecraft.nbt.Tag.TAG_COMPOUND);

        for (var face = 0; face < 6; face++) {
            // PipeFilter.load also reads the old nine-slot format, so existing filters survive the update.
            filters[face] = face < list.size() ? PipeFilter.load(list.getCompound(face)) : null;
        }
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
