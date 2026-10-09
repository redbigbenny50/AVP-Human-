package com.human.common.gameplay.power;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;

/**
 * Which faces of a power block are BREAKERS - closed, so nothing connects through them.
 * <h2>⭐ WHY A BREAKER AND NOT AN INPUT/OUTPUT PORT</h2> [stated] Sep 25: "if i shut a port it will cut power to
 * anything downstream of it ... i turn off or on sections via disabling the flow from that battery."
 * <p>
 * That is not direction, it is a switch - and a switch fits the grid as it stands. A closed face simply is not
 * connectable, so the walk stops there, the network splits, and the downstream rooms become their own grid with no
 * producers in it. {@code PowerGrid} needs no change at all, which means the conservation proof still covers it, and
 * OPEN faces stay plainly bidirectional - which is also exactly what other mods' energy blocks expect, so cross-mod
 * behaviour costs nothing instead of needing a special case.
 * </p>
 * <p>
 * ⚠ Because open faces are bidirectional, power can route AROUND a closed breaker if another cable path exists. That is
 * correct, not a bug: sectioning a base depends on one feed per room rather than a mesh.
 * </p>
 * <h2>⚠⚠ SAVED AS NAMES, NEVER AS A BITMASK OR ORDINALS</h2> A bitmask silently means something different if the
 * direction order ever changes, and an ordinal read out of range throws inside {@code loadAdditional}, failing the
 * whole chunk. Names are matched against the known directions and anything unrecognised is ignored, so a pasted or
 * edited tag costs an open face, not a crash.
 */
public final class PowerPorts {

    public static final String NBT_KEY = "closedPorts";

    private final boolean[] closed = new boolean[Direction.values().length];

    public boolean isClosed(Direction direction) {
        return direction != null && closed[direction.ordinal()];
    }

    public boolean isOpen(Direction direction) {
        return !isClosed(direction);
    }

    /** @return true when the state actually changed, so the caller only re-links the grid when it must */
    public boolean setClosed(Direction direction, boolean value) {
        if (direction == null || closed[direction.ordinal()] == value) {
            return false;
        }

        closed[direction.ordinal()] = value;

        return true;
    }

    public boolean toggle(Direction direction) {
        return setClosed(direction, !isClosed(direction));
    }

    /** The closed faces as names, for the item component that survives a break. */
    public java.util.List<String> toNames() {
        var names = new java.util.ArrayList<String>();

        for (var direction : Direction.values()) {
            if (closed[direction.ordinal()]) {
                names.add(direction.getSerializedName());
            }
        }

        return names;
    }

    /** ⚠ Cannot throw: an unknown name from a pasted or edited component is skipped, leaving that face open. */
    public void fromNames(java.util.List<String> names) {
        java.util.Arrays.fill(closed, false);

        for (var name : names) {
            var direction = Direction.byName(name);

            if (direction != null) {
                closed[direction.ordinal()] = true;
            }
        }
    }

    public void save(CompoundTag tag) {
        var names = new net.minecraft.nbt.ListTag();

        for (var direction : Direction.values()) {
            if (closed[direction.ordinal()]) {
                names.add(net.minecraft.nbt.StringTag.valueOf(direction.getSerializedName()));
            }
        }

        if (!names.isEmpty()) {
            tag.put(NBT_KEY, names);
        }
    }

    /** ⚠ Cannot throw. Unknown names are skipped; a missing or wrongly-typed tag leaves every face open. */
    public void load(CompoundTag tag) {
        java.util.Arrays.fill(closed, false);

        if (!tag.contains(NBT_KEY, net.minecraft.nbt.Tag.TAG_LIST)) {
            return;
        }

        var names = tag.getList(NBT_KEY, net.minecraft.nbt.Tag.TAG_STRING);

        for (var i = 0; i < names.size(); i++) {
            var direction = Direction.byName(names.getString(i));

            if (direction != null) {
                closed[direction.ordinal()] = true;
            }
        }
    }
}
