package com.human.common.gameplay.distributor;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.BitSet;

/**
 * Which blocks of the 33 x 33 x 33 area the fog can actually reach.
 * <h2>[stated] Oct 17 - THE RULE</h2> "Fog leaves through the fan, then spreads only through open space reachable from
 * the fan - never through blocks (walls, floors, ceilings, closed doors). Flows through doorways, gaps, down past
 * ledges." So this is a flood fill that starts on the block in front of the fan and stops at:
 * <ul>
 * <li>any block whose COLLISION SHAPE IS A FULL CUBE - stone, planks, glass, concrete, a solid machine;</li>
 * <li>a CLOSED door or trapdoor (they are thin, so the full-cube test alone would let fog through them);</li>
 * <li>the edge of the area, and any block in a chunk that is not loaded (⚠ the scan NEVER loads a chunk).</li>
 * </ul>
 * Everything else is open: air, water, slabs, stairs, fences, bars, panes, open doors - air moves through those.
 * <h2>[stated] "spread over ticks, cached ... no TPS spike"</h2>
 * <ul>
 * <li>Each {@link #step} checks at most a budget of blocks (the owner passes it). A fully open area is 35,937 blocks,
 * so at 2,048 a tick a worst-case scan finishes in under a second.</li>
 * <li>The finished result is KEPT ({@link #contains}, {@link #cells}) while the next scan runs into separate arrays,
 * then swapped in - so the fog never blinks out during a rescan.</li>
 * <li>No allocation per block: one reused mutable position, a flat int queue and two bit sets made once.</li>
 * </ul>
 * The same class runs on the server (the sensor and the vapor effects) and on each client that sees the fog (the
 * particles), so no packet carries the room.
 */
public final class DistributorRoom {

    private static final Direction[] DIRECTIONS = Direction.values();

    // The finished room.
    private BitSet done = new BitSet(DistributorFog.CELLS);

    private int[] doneCells = new int[0];

    private int doneCount;

    private boolean hasResult;

    // The scan in progress.
    private BitSet scanning = new BitSet(DistributorFog.CELLS);

    private final int[] queue = new int[DistributorFog.CELLS];

    private int head;

    private int tail;

    private boolean running;

    private final BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

    private BlockPos origin = BlockPos.ZERO;

    /** Game time the last scan finished. */
    private long finishedAt = Long.MIN_VALUE / 2;

    /** Starts a fresh scan from the block in front of the fan. The previous result stays readable until it finishes. */
    public void start(Level level, BlockPos machine, Direction front) {
        origin = machine.immutable();
        scanning.clear();
        head = 0;
        tail = 0;
        running = true;

        var dx = front.getStepX();
        var dy = front.getStepY();
        var dz = front.getStepZ();

        cursor.set(machine.getX() + dx, machine.getY() + dy, machine.getZ() + dz);

        if (isOpen(level, cursor)) {
            var index = DistributorFog.index(dx, dy, dz);

            scanning.set(index);
            queue[tail++] = index;
        }
    }

    public boolean isRunning() {
        return running;
    }

    /**
     * Checks up to {@code budget} more blocks. When the queue empties the scan becomes the room.
     *
     * @return true when this call finished the scan
     */
    public boolean step(Level level, int budget, long gameTime) {
        if (!running) {
            return false;
        }

        var size = DistributorFog.SIZE;
        var radius = DistributorFog.RADIUS;
        var checked = 0;

        while (head < tail && checked < budget) {
            var index = queue[head++];
            var dz = index % size - radius;
            var dy = index / size % size - radius;
            var dx = index / (size * size) - radius;

            for (var direction : DIRECTIONS) {
                var nx = dx + direction.getStepX();
                var ny = dy + direction.getStepY();
                var nz = dz + direction.getStepZ();

                // The machine's own block is never fog, and the area ends at 16.
                if (!DistributorFog.inArea(nx, ny, nz) || nx == 0 && ny == 0 && nz == 0) {
                    continue;
                }

                var next = DistributorFog.index(nx, ny, nz);

                if (scanning.get(next)) {
                    continue;
                }

                checked++;
                cursor.set(origin.getX() + nx, origin.getY() + ny, origin.getZ() + nz);

                if (isOpen(level, cursor)) {
                    scanning.set(next);
                    queue[tail++] = next;
                }
            }
        }

        if (head < tail) {
            return false;
        }

        // Finished: the queue holds exactly the reached blocks, in fill order. Swap it in as the room.
        var cells = new int[tail];
        System.arraycopy(queue, 0, cells, 0, tail);

        var previous = done;
        done = scanning;
        scanning = previous;
        doneCells = cells;
        doneCount = tail;
        hasResult = true;
        running = false;
        finishedAt = gameTime;

        return true;
    }

    /** ⚠ Never loads a chunk: an unloaded block counts as a wall. */
    private static boolean isOpen(Level level, BlockPos pos) {
        if (!level.isLoaded(pos)) {
            return false;
        }

        return isOpen(level, pos, level.getBlockState(pos));
    }

    /** The fog's wall rule, shared with the chem thrower's blast. */
    public static boolean isOpen(Level level, BlockPos pos, BlockState state) {
        var block = state.getBlock();

        if (block instanceof DoorBlock) {
            return state.getValue(DoorBlock.OPEN);
        }

        if (block instanceof TrapDoorBlock) {
            return state.getValue(TrapDoorBlock.OPEN);
        }

        return !state.isCollisionShapeFullBlock(level, pos);
    }

    public boolean hasResult() {
        return hasResult;
    }

    public long finishedAt() {
        return finishedAt;
    }

    /** Where the finished room was scanned from - a moved or rotated machine starts a new scan anyway. */
    public BlockPos origin() {
        return origin;
    }

    /** Whether the fog reaches this world position. False until the first scan has finished. */
    public boolean contains(BlockPos pos) {
        if (!hasResult) {
            return false;
        }

        var dx = pos.getX() - origin.getX();
        var dy = pos.getY() - origin.getY();
        var dz = pos.getZ() - origin.getZ();

        return DistributorFog.inArea(dx, dy, dz) && done.get(DistributorFog.index(dx, dy, dz));
    }

    /** The fog's strength at a world position: the shape's strength where the room reaches, else 0. */
    public float strengthAt(BlockPos pos) {
        if (!contains(pos)) {
            return 0.0F;
        }

        return DistributorFog.strength(pos.getX() - origin.getX(), pos.getY() - origin.getY(), pos.getZ() - origin.getZ());
    }

    /** How many blocks the fog reaches. */
    public int size() {
        return doneCount;
    }

    /** The flat index of the n-th reached block (0 <= n < {@link #size()}). */
    public int cell(int n) {
        return doneCells[n];
    }

    /** Forgets the room entirely - the machine was emptied or switched off long enough not to matter. */
    public void clear() {
        done.clear();
        doneCells = new int[0];
        doneCount = 0;
        hasResult = false;
        running = false;
    }
}
