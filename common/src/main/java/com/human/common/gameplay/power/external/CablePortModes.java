package com.human.common.gameplay.power.external;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

/**
 * The mode of every cable face someone has changed from AUTO. Cables have no block entity, so the modes live in the
 * level's saved data instead - only the faces that differ from AUTO, three bits each, packed per cable.
 */
public class CablePortModes extends SavedData {

    private static final String NAME = "avp_human_cable_ports";

    private final Map<Long, Integer> modes = new HashMap<>();

    public static CablePortModes get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(new SavedData.Factory<>(CablePortModes::new, CablePortModes::load, null), NAME);
    }

    /** Three bits a face, face i at bit i*3 - see {@link #get}. */
    public static final int BITS_PER_FACE = 3;

    /** [stated] Oct 21 - a cable's modes as stored, so a ship move can carry them (see ShipMoves). 0 = all default. */
    public int packed(BlockPos cable) {
        return modes.getOrDefault(cable.asLong(), 0);
    }

    /** Writes a cable's modes as stored (0 = all default). */
    public void putPacked(BlockPos cable, int packed) {
        if (packed == 0) {
            if (modes.remove(cable.asLong()) != null) {
                setDirty();
            }
            return;
        }

        modes.put(cable.asLong(), packed);
        setDirty();
    }

    public PortMode get(BlockPos cable, Direction face) {
        var packed = modes.getOrDefault(cable.asLong(), 0);

        return PortMode.byOrdinal(packed >> face.get3DDataValue() * 3 & 7);
    }

    public void set(BlockPos cable, Direction face, PortMode mode) {
        var key = cable.asLong();
        var shift = face.get3DDataValue() * 3;
        var packed = modes.getOrDefault(key, 0) & ~(7 << shift) | mode.ordinal() << shift;

        if (packed == 0) {
            modes.remove(key);
        } else {
            modes.put(key, packed);
        }

        setDirty();
    }

    /** A broken cable forgets its modes. */
    public void clear(BlockPos cable) {
        if (modes.remove(cable.asLong()) != null) {
            setDirty();
        }
    }

    @Override
    public @NotNull CompoundTag save(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        var list = new ListTag();

        for (var entry : modes.entrySet()) {
            var element = new CompoundTag();

            element.putLong("pos", entry.getKey());
            element.putInt("modes", entry.getValue());
            list.add(element);
        }

        tag.put("ports", list);

        return tag;
    }

    private static CablePortModes load(CompoundTag tag, HolderLookup.Provider registries) {
        var data = new CablePortModes();

        for (var element : tag.getList("ports", Tag.TAG_COMPOUND)) {
            var compound = (CompoundTag) element;

            data.modes.put(compound.getLong("pos"), compound.getInt("modes"));
        }

        return data;
    }
}
