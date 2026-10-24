package com.human.common.gameplay.ship;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Rotation;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.function.Function;

/**
 * [stated] Oct 21 - one batch of blocks moved together as a whole: Sable building a Create Aeronautics ship out of the
 * world, or landing one back into it. Every block in the batch moved by the same mapping, and the batch may have been
 * turned by a quarter-turn {@link Rotation} (Simulated lands a ship rotated to the nearest 90 degrees; building one
 * never rotates).
 * <p>
 * Our block entities arrive at their new positions carrying their saved data unchanged - and some of that data names
 * OTHER positions (a fuse panel's devices, a consumer's panel, a cryotank's master) or is kept per FACE by compass
 * direction (breakers, flanges, port modes). Both go stale after a move. A {@link ShipMoveAware} block entity is handed
 * this object to put them right.
 * </p>
 * Sable-free on purpose: the Sable hook (neoforge module) builds one of these; nothing in common touches Sable.
 */
public final class ShipMove {

    private final ServerLevel level;

    private final Rotation rotation;

    private final Set<Long> moved;

    private final Function<BlockPos, BlockPos> map;

    /** New position -> old, for {@link #original}. */
    private final java.util.Map<Long, BlockPos> originals;

    public ShipMove(
        ServerLevel level,
        Rotation rotation,
        Set<Long> moved,
        Function<BlockPos, BlockPos> map,
        java.util.Map<Long, BlockPos> originals
    ) {
        this.level = level;
        this.rotation = rotation == null ? Rotation.NONE : rotation;
        this.moved = moved;
        this.map = map;
        this.originals = originals;
    }

    /** Where a block now at {@code newPos} was before this move, or null if it was not part of it. */
    public @Nullable BlockPos original(BlockPos newPos) {
        return originals.get(newPos.asLong());
    }

    /** The level the blocks are in NOW. */
    public ServerLevel level() {
        return level;
    }

    public Rotation rotation() {
        return rotation;
    }

    public boolean rotated() {
        return rotation != Rotation.NONE;
    }

    /** Whether the block that was at {@code oldPos} was part of this batch. */
    public boolean moved(BlockPos oldPos) {
        return oldPos != null && moved.contains(oldPos.asLong());
    }

    /**
     * Where a block that was at {@code oldPos} is now - or null if it was NOT part of this batch (it stayed behind, so
     * any link to it from a block that moved is broken).
     */
    public @Nullable BlockPos remap(@Nullable BlockPos oldPos) {
        if (!moved(oldPos)) {
            return null;
        }

        return map.apply(oldPos).immutable();
    }

    /** A compass face turned with the batch. Up and down never change. */
    public Direction rotate(Direction face) {
        return rotation.rotate(face);
    }

    /** A relative offset turned with the batch. */
    public BlockPos rotateOffset(BlockPos offset) {
        return offset.rotate(rotation);
    }

    /** Per-face data indexed by {@link Direction#get3DDataValue()}, turned with the batch. In place. */
    public void rotateFaces(byte[] faces) {
        if (!rotated()) {
            return;
        }

        var copy = faces.clone();

        for (var face : Direction.values()) {
            faces[rotate(face).get3DDataValue()] = copy[face.get3DDataValue()];
        }
    }

    /** As {@link #rotateFaces(byte[])}, for objects. In place. */
    public <T> void rotateFaces(T[] faces) {
        if (!rotated()) {
            return;
        }

        var copy = faces.clone();

        for (var face : Direction.values()) {
            faces[rotate(face).get3DDataValue()] = copy[face.get3DDataValue()];
        }
    }

    /** Per-face data packed {@code bitsPerFace} bits a face, face {@code i} at bit {@code i * bitsPerFace}. */
    public int rotatePacked(int packed, int bitsPerFace) {
        if (!rotated()) {
            return packed;
        }

        var mask = (1 << bitsPerFace) - 1;
        var out = 0;

        for (var face : Direction.values()) {
            var value = packed >> face.get3DDataValue() * bitsPerFace & mask;
            out |= value << rotate(face).get3DDataValue() * bitsPerFace;
        }

        return out;
    }
}
