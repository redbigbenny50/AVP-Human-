package com.human.common.gameplay.gene.machine.analyzer;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

/**
 * The six blocks of a Gene Analyzer: 3 wide, 1 deep, 2 tall. The model is centred on the CORE (bottom middle), so the
 * bench reaches one block to each side; the top row covers the screens and scanner posts.
 */
public enum GeneAnalyzerPart implements StringRepresentable {

    CORE("core", 0, 0),
    LEFT("left", -1, 0),
    RIGHT("right", 1, 0),
    TOP("top", 0, 1),
    TOP_LEFT("top_left", -1, 1),
    TOP_RIGHT("top_right", 1, 1);

    private final String name;

    /** Steps toward the facing's clockwise side. */
    private final int side;

    private final int up;

    GeneAnalyzerPart(String name, int side, int up) {
        this.name = name;
        this.side = side;
        this.up = up;
    }

    @Override
    public @NotNull String getSerializedName() {
        return name;
    }

    public boolean isTop() {
        return up > 0;
    }

    public BlockPos fromCore(BlockPos core, Direction facing) {
        return core.relative(facing.getClockWise(), side).above(up);
    }

    public BlockPos toCore(BlockPos pos, Direction facing) {
        return pos.relative(facing.getClockWise(), -side).below(up);
    }
}
