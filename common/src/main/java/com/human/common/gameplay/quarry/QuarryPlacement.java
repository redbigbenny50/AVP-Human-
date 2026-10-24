package com.human.common.gameplay.quarry;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.jetbrains.annotations.Nullable;

/**
 * Where a quarry terminal may stand. Shared by the block (which refuses a bad spot) and the placement hologram (which
 * shows the player why), so the two can never disagree.
 * <h2>THE RULE</h2> The terminal goes just outside a chunk, back toward it. The frame ring sits one block outside the
 * chunk, and the terminal's back row sits against the ring:
 *
 * <pre>
 *   core ── back row ── ring ── first block of the chunk being mined
 *    +0        +1        +2       +3   (blocks toward the back)
 * </pre>
 *
 * So the block three behind the core must be the FIRST block of a chunk along that axis. Anywhere else, the player is
 * told how many blocks to move.
 */
public final class QuarryPlacement {

    /** How far behind the core the first block of the work area is. */
    public static final int AREA_OFFSET = 3;

    private QuarryPlacement() {}

    /**
     * @param fits      all six spaces are free and nobody is standing in them
     * @param shift     blocks to move the terminal toward the chunk (positive) or away from it (negative); 0 = at an
     *                  edge
     * @param areaChunk the chunk the quarry would start from (only meaningful when {@code shift == 0})
     */
    public record Result(
        boolean fits,
        int shift,
        ChunkPos areaChunk
    ) {

        public boolean ok() {
            return fits && shift == 0;
        }
    }

    /**
     * @param placing the terminal block's state, used to test whether anyone is standing in each space
     */
    public static Result check(Level level, BlockPos core, Direction facing, BlockState placing, @Nullable BlockPlaceContext context) {
        var fits = true;

        for (var part : QuarryTerminalPart.values()) {
            var pos = part.fromCore(core, facing);
            var state = level.getBlockState(pos);
            var replaceable = context != null ? state.canBeReplaced(context) : state.canBeReplaced();

            if (
                !replaceable || !level.getWorldBorder().isWithinBounds(pos) || pos.getY() >= level.getMaxBuildHeight() || pos.getY() < level
                    .getMinBuildHeight()
            ) {
                fits = false;
                break;
            }

            // Nobody standing in the space - a part placed into a player or mob would trap them inside it.
            if (!level.isUnobstructed(placing, pos, CollisionContext.empty())) {
                fits = false;
                break;
            }
        }

        var back = facing.getOpposite();
        var first = core.relative(back, AREA_OFFSET);

        return new Result(fits, shiftToEdge(first, back), new ChunkPos(first));
    }

    /**
     * Blocks the terminal must move along {@code back} so that {@code first} becomes the first block of a chunk seen
     * from outside: the 0th block when the back points the positive way (south/east), the 15th when it points the
     * negative way (north/west). The smaller of the two moves is returned.
     */
    static int shiftToEdge(BlockPos first, Direction back) {
        var coordinate = back.getAxis() == Direction.Axis.X ? first.getX() : first.getZ();
        var positive = back.getAxisDirection() == Direction.AxisDirection.POSITIVE;
        var inChunk = coordinate & 15;
        // Distance (in the back direction) to the next coordinate that is a chunk's first block from this side.
        var forward = positive ? (16 - inChunk) & 15 : (inChunk - 15 + 16) & 15;

        // forward = 0 means already there. Otherwise moving back 'forward' blocks, or toward the player 16 - forward.
        if (forward == 0) {
            return 0;
        }

        return forward <= 8 ? forward : forward - 16;
    }
}
