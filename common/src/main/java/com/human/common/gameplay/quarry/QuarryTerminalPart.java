package com.human.common.gameplay.quarry;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

/**
 * The six blocks of a quarry terminal ([stated] Oct 13: 3 wide on the bottom against the chunk border, the centre 2
 * deep and 2 tall, the console slope on top of the front block).
 * <p>
 * FACING is the way the FRONT points - toward the player who placed it - and the BACK faces the chunk being mined
 * ([stated] Oct 15: "its front faces you when you place it and the back with the blue thing is what faces inwards
 * towards the chunk and thats where the lasers come out").
 * </p>
 *
 * <pre>
 *            (the chunk)
 *   [BACK_CCW][BACK][BACK_CW]      bottom row, against the frame
 *             [CORE]               + CONSOLE on top of CORE, BACK_TOP on top of BACK
 *            (the player)
 * </pre>
 *
 * The CORE is the model's origin block (front centre, bottom), so the renderer draws from it unmoved. The side blocks
 * are mirror images in the model, so which of them is "clockwise" does not matter for the footprint.
 */
public enum QuarryTerminalPart implements StringRepresentable {

    CORE("core", 0, 0, 0),
    CONSOLE("console", 0, 0, 1),
    BACK("back", 1, 0, 0),
    BACK_TOP("back_top", 1, 0, 1),
    BACK_CW("back_cw", 1, 1, 0),
    BACK_CCW("back_ccw", 1, -1, 0);

    private final String name;

    /** Blocks toward the back (the chunk), toward the facing's clockwise side, and up. */
    private final int back;

    private final int side;

    private final int up;

    QuarryTerminalPart(String name, int back, int side, int up) {
        this.name = name;
        this.back = back;
        this.side = side;
        this.up = up;
    }

    @Override
    public @NotNull String getSerializedName() {
        return name;
    }

    /** Where this part sits, given the core's position and the facing. */
    public BlockPos fromCore(BlockPos core, Direction facing) {
        return core.relative(facing.getOpposite(), back).relative(facing.getClockWise(), side).above(up);
    }

    /** The core's position, given this part's position and the facing. */
    public BlockPos toCore(BlockPos pos, Direction facing) {
        return pos.relative(facing, back).relative(facing.getCounterClockWise(), side).below(up);
    }
}
