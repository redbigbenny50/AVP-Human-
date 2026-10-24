package com.human.common.gameplay.ship;

import com.human.common.gameplay.power.external.CablePortModes;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Rotation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * [stated] Oct 21 - "I want to make sure it moving with power, fluid, or items systems wont break them." The two halves
 * of a batch move (see {@link ShipMove}), called by the Sable hook around Sable's own block move:
 * <ol>
 * <li>{@link #begin} BEFORE any block moves: remembers which positions are in the batch, and takes the cable port modes
 * (kept per POSITION in a level-wide save file, not in a block entity - so they cannot travel on their own, and the
 * cable's removal clears them).</li>
 * <li>{@link #finish} AFTER every block is in place: hands each moved block entity the move (phase 1, then phase 2) and
 * writes the cable port modes back at the cables' new positions, turned with the batch.</li>
 * </ol>
 * ⚡ One pass over the batch for each half; a map lookup per block. Runs only when a ship is built or lands.
 */
public final class ShipMoves {

    private ShipMoves() {}

    /** What {@link #begin} remembers for {@link #finish}. */
    public static final class Batch {

        private final List<BlockPos> positions;

        private final Set<Long> moved;

        private final Map<Long, Integer> portModes;

        private Batch(List<BlockPos> positions, Set<Long> moved, Map<Long, Integer> portModes) {
            this.positions = positions;
            this.moved = moved;
            this.portModes = portModes;
        }
    }

    public static Batch begin(ServerLevel from, Iterable<BlockPos> blocks) {
        var positions = new ArrayList<BlockPos>();
        var moved = new HashSet<Long>();

        for (var pos : blocks) {
            var immutable = pos.immutable();
            positions.add(immutable);
            moved.add(immutable.asLong());
        }

        var portModes = new HashMap<Long, Integer>();
        var modes = CablePortModes.get(from);

        for (var pos : positions) {
            var packed = modes.packed(pos);

            if (packed != 0) {
                portModes.put(pos.asLong(), packed);
            }
        }

        return new Batch(positions, moved, portModes);
    }

    public static void finish(Batch batch, ServerLevel to, Rotation rotation, Function<BlockPos, BlockPos> map) {
        var originals = new HashMap<Long, BlockPos>();
        var aware = new ArrayList<ShipMoveAware>();

        for (var oldPos : batch.positions) {
            var newPos = map.apply(oldPos);
            originals.put(newPos.asLong(), oldPos);

            if (to.getBlockEntity(newPos) instanceof ShipMoveAware blockEntity) {
                aware.add(blockEntity);
            }
        }

        var move = new ShipMove(to, rotation, batch.moved, map, originals);

        for (var blockEntity : aware) {
            blockEntity.onShipMove(move);
        }

        for (var blockEntity : aware) {
            blockEntity.afterShipMove(move);
        }

        if (!batch.portModes.isEmpty()) {
            var modes = CablePortModes.get(to);

            for (var entry : batch.portModes.entrySet()) {
                var newPos = map.apply(BlockPos.of(entry.getKey()));
                modes.putPacked(newPos, move.rotatePacked(entry.getValue(), CablePortModes.BITS_PER_FACE));
            }
        }
    }
}
