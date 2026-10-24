package com.human.common.gameplay.quarry;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.ChunkPos;

import java.util.ArrayList;
import java.util.List;

/**
 * The geometry of a quarry's work area, worked out from where the terminal stands.
 * <h2>THE GRID</h2> Seen from the terminal's screen, looking at the chunk: DEPTH counts chunks away (1 = the chunk the
 * terminal's back touches), LATERAL counts chunks to the player's right (+) or left (-). The area is up to 3 x 3 chunks
 * ([stated] Oct 5) and must include the depth-1 chunk straight ahead, so it can only use depths 1-3 and laterals -2..2.
 * <h2>THE FRAME</h2> [stated] Oct 15: a ring one block outside the area at the quarry's own level, the four corners
 * going up four blocks, then a second ring on top - 2 rings + 16 leg blocks.
 */
public final class QuarryArea {

    public static final int MAX_DEPTH = 3;

    public static final int MAX_LATERAL = 2;

    public static final int CANDIDATE_COLUMNS = MAX_LATERAL * 2 + 1;

    public static final int CANDIDATES = MAX_DEPTH * CANDIDATE_COLUMNS;

    /** Blocks between the rings, i.e. the leg height. */
    public static final int LEG_HEIGHT = 4;

    private QuarryArea() {}

    /** The chunk the terminal's back touches (depth 1, lateral 0). */
    public static ChunkPos firstChunk(BlockPos core, Direction facing) {
        return new ChunkPos(core.relative(facing.getOpposite(), QuarryPlacement.AREA_OFFSET));
    }

    /** The chunk at {@code depth} (1 = nearest) and {@code lateral} (+ = the player's right as they face the chunk). */
    public static ChunkPos chunkAt(BlockPos core, Direction facing, int depth, int lateral) {
        var first = firstChunk(core, facing);
        var back = facing.getOpposite();
        // The player looks along 'back'; their right is back's clockwise side.
        var right = back.getClockWise();

        return new ChunkPos(
            first.x + back.getStepX() * (depth - 1) + right.getStepX() * lateral,
            first.z + back.getStepZ() * (depth - 1) + right.getStepZ() * lateral
        );
    }

    /** Bit index of a candidate cell in the selection mask. */
    public static int bit(int depth, int lateral) {
        return (depth - 1) * CANDIDATE_COLUMNS + lateral + MAX_LATERAL;
    }

    public static boolean isCandidate(int depth, int lateral) {
        return depth >= 1 && depth <= MAX_DEPTH && Math.abs(lateral) <= MAX_LATERAL;
    }

    /**
     * Oct 17 - [stated] "you can only choose a 3x3 but a 3x5 area is what can be chosen so im kinda confused". The 3 x
     * 3 may sit centred on the front chunk or one column to either side, so the map holds 5 columns - but only 3 of
     * them can ever be in one area. Whether {@code (depth, lateral)} can still be ADDED to {@code mask} and leave a
     * 3-wide area that includes the front chunk: the columns of the selection, the new chunk and the front chunk
     * (lateral 0) must all fit in 3. Depth always fits - there are only 3 rows.
     */
    public static boolean canAdd(int mask, int depth, int lateral) {
        if (!isCandidate(depth, lateral)) {
            return false;
        }

        int minL = Math.min(0, lateral);
        int maxL = Math.max(0, lateral);

        for (int d = 1; d <= MAX_DEPTH; d++) {
            for (int l = -MAX_LATERAL; l <= MAX_LATERAL; l++) {
                if ((mask & (1 << bit(d, l))) != 0) {
                    minL = Math.min(minL, l);
                    maxL = Math.max(maxL, l);
                }
            }
        }

        return maxL - minL + 1 <= 3;
    }

    /** A selection's shape, or why it is not a valid one. */
    public record Shape(
        boolean valid,
        String problem,
        int width,
        int depth,
        int minLateral,
        int maxLateral,
        int maxDepth
    ) {}

    public static Shape shape(int mask) {
        if (mask == 0) {
            return new Shape(false, "nothing", 0, 0, 0, 0, 0);
        }

        int minL = 99, maxL = -99, minD = 99, maxD = -99, count = 0;

        for (int depth = 1; depth <= MAX_DEPTH; depth++) {
            for (int lateral = -MAX_LATERAL; lateral <= MAX_LATERAL; lateral++) {
                if ((mask & (1 << bit(depth, lateral))) != 0) {
                    minL = Math.min(minL, lateral);
                    maxL = Math.max(maxL, lateral);
                    minD = Math.min(minD, depth);
                    maxD = Math.max(maxD, depth);
                    count++;
                }
            }
        }

        var width = maxL - minL + 1;
        var deep = maxD - minD + 1;

        if (count != width * deep) {
            return new Shape(false, "rectangle", width, deep, minL, maxL, maxD);
        }

        if (width > 3 || deep > 3) {
            return new Shape(false, "too_big", width, deep, minL, maxL, maxD);
        }

        if ((mask & (1 << bit(1, 0))) == 0) {
            return new Shape(false, "front", width, deep, minL, maxL, maxD);
        }

        return new Shape(true, "ready", width, deep, minL, maxL, maxD);
    }

    /** Every chunk of a selection. */
    public static List<ChunkPos> chunks(BlockPos core, Direction facing, int mask) {
        var out = new ArrayList<ChunkPos>();

        for (int depth = 1; depth <= MAX_DEPTH; depth++) {
            for (int lateral = -MAX_LATERAL; lateral <= MAX_LATERAL; lateral++) {
                if ((mask & (1 << bit(depth, lateral))) != 0) {
                    out.add(chunkAt(core, facing, depth, lateral));
                }
            }
        }

        return out;
    }

    /** Block bounds of the selected chunks: {minX, minZ, maxX, maxZ}, inclusive. */
    public static int[] bounds(List<ChunkPos> chunks) {
        int minX = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;

        for (var chunk : chunks) {
            minX = Math.min(minX, chunk.getMinBlockX());
            minZ = Math.min(minZ, chunk.getMinBlockZ());
            maxX = Math.max(maxX, chunk.getMaxBlockX());
            maxZ = Math.max(maxZ, chunk.getMaxBlockZ());
        }

        return new int[] { minX, minZ, maxX, maxZ };
    }

    /**
     * Every frame position, in build order: the bottom ring, the four legs, the top ring. Retracting walks it
     * backwards.
     *
     * @param baseY the quarry's level (the terminal's bottom row), where the bottom ring sits
     */
    public static List<BlockPos> framePositions(List<ChunkPos> chunks, int baseY) {
        var out = new ArrayList<BlockPos>();

        if (chunks.isEmpty()) {
            return out;
        }

        var b = bounds(chunks);
        int x0 = b[0] - 1, z0 = b[1] - 1, x1 = b[2] + 1, z1 = b[3] + 1;

        ring(out, x0, z0, x1, z1, baseY);

        for (int up = 1; up <= LEG_HEIGHT; up++) {
            out.add(new BlockPos(x0, baseY + up, z0));
            out.add(new BlockPos(x1, baseY + up, z0));
            out.add(new BlockPos(x1, baseY + up, z1));
            out.add(new BlockPos(x0, baseY + up, z1));
        }

        ring(out, x0, z0, x1, z1, baseY + LEG_HEIGHT + 1);

        return out;
    }

    private static void ring(List<BlockPos> out, int x0, int z0, int x1, int z1, int y) {
        for (int x = x0; x <= x1; x++) {
            out.add(new BlockPos(x, y, z0));
            out.add(new BlockPos(x, y, z1));
        }

        for (int z = z0 + 1; z < z1; z++) {
            out.add(new BlockPos(x0, y, z));
            out.add(new BlockPos(x1, y, z));
        }
    }

    /** Every chunk the frame itself touches (one block outside the area reaches into the neighbours). */
    public static List<ChunkPos> frameChunks(List<ChunkPos> chunks) {
        var out = new ArrayList<ChunkPos>();

        if (chunks.isEmpty()) {
            return out;
        }

        var b = bounds(chunks);

        for (int cx = (b[0] - 1) >> 4; cx <= (b[2] + 1) >> 4; cx++) {
            for (int cz = (b[1] - 1) >> 4; cz <= (b[3] + 1) >> 4; cz++) {
                out.add(new ChunkPos(cx, cz));
            }
        }

        return out;
    }
}
