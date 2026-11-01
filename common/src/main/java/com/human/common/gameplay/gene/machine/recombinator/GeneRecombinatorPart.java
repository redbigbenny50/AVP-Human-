package com.human.common.gameplay.gene.machine.recombinator;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

/**
 * The four blocks of a Gene Recombinator: 2 wide, 1 deep, 2 tall ([stated] Oct 27: 2x1x2). The CORE is the bottom block
 * the player clicked; the model reaches one block toward the facing's clockwise side (its block geo is shifted half a
 * block toward geo -X, which BLib's X mirror puts on the clockwise side, as for the Gene Storage).
 */
public enum GeneRecombinatorPart implements StringRepresentable {

    CORE("core", false, false),
    SIDE("side", true, false),
    TOP("top", false, true),
    TOP_SIDE("top_side", true, true);

    private final String name;

    private final boolean side;

    private final boolean top;

    GeneRecombinatorPart(String name, boolean side, boolean top) {
        this.name = name;
        this.side = side;
        this.top = top;
    }

    @Override
    public @NotNull String getSerializedName() {
        return name;
    }

    public boolean isTop() {
        return top;
    }

    public BlockPos fromCore(BlockPos core, Direction facing) {
        var pos = side ? core.relative(facing.getClockWise()) : core;
        return top ? pos.above() : pos;
    }

    public BlockPos toCore(BlockPos pos, Direction facing) {
        var core = side ? pos.relative(facing.getCounterClockWise()) : pos;
        return top ? core.below() : core;
    }
}
