package com.human.common.gameplay.gene.machine.storage;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

/**
 * The eight blocks of a Gene Storage: it is 2 wide, 2 deep and 2 tall. The CORE (front-left, bottom) holds the rack and
 * the power port; the others only fill the model's footprint.
 * <p>
 * The model is centred on the 2x2. Its block geo is shifted half a block toward geo -X and +Z; BLib mirrors geo X on
 * load (the fabricator's Oct 14 finding), so in the world the model covers the core and the blocks on the facing's
 * CLOCKWISE side and BEHIND it, on every facing.
 * </p>
 */
public enum GeneStoragePart implements StringRepresentable {

    CORE("core", false, false, false),
    SIDE("side", true, false, false),
    BACK("back", false, true, false),
    BACK_SIDE("back_side", true, true, false),
    TOP("top", false, false, true),
    TOP_SIDE("top_side", true, false, true),
    TOP_BACK("top_back", false, true, true),
    TOP_BACK_SIDE("top_back_side", true, true, true);

    private final String name;

    private final boolean side;

    private final boolean back;

    private final boolean top;

    GeneStoragePart(String name, boolean side, boolean back, boolean top) {
        this.name = name;
        this.side = side;
        this.back = back;
        this.top = top;
    }

    @Override
    public @NotNull String getSerializedName() {
        return name;
    }

    /** True for the four upper blocks. */
    public boolean isTop() {
        return top;
    }

    public BlockPos fromCore(BlockPos core, Direction facing) {
        var pos = core;
        if (side) {
            pos = pos.relative(facing.getClockWise());
        }
        if (back) {
            pos = pos.relative(facing.getOpposite());
        }
        if (top) {
            pos = pos.above();
        }
        return pos;
    }

    public BlockPos toCore(BlockPos pos, Direction facing) {
        var core = pos;
        if (side) {
            core = core.relative(facing.getCounterClockWise());
        }
        if (back) {
            core = core.relative(facing);
        }
        if (top) {
            core = core.below();
        }
        return core;
    }
}
