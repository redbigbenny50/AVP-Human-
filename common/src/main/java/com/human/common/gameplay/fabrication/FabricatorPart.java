package com.human.common.gameplay.fabrication;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

/**
 * The four blocks of a fabricator: it is 2 wide, 2 tall and 1 deep. The CORE holds everything; the other three only
 * fill the model's footprint.
 * <p>
 * Oct 14 FIX ([stated] "fabricator bounding box is bigger than the model"): the extra column is on the core's CLOCKWISE
 * side. The fabricator_v2 geometry extends toward model -X, but BLib mirrors cube X when it loads a model (verified in
 * AzBuiltinBakedModelFactory, as GeckoLib does), so in the world it extends toward +X - and BLib's block rotation
 * (north 0, west 90, south 180, east 270 about +Y) carries +X to the facing's clockwise side every time. The column was
 * on the counter-clockwise side, so the parts stood one block off the model on every facing.
 * </p>
 */
public enum FabricatorPart implements StringRepresentable {

    CORE("core", false, false),
    SIDE("side", true, false),
    TOP("top", false, true),
    TOP_SIDE("top_side", true, true);

    private final String name;

    private final boolean side;

    private final boolean top;

    FabricatorPart(String name, boolean side, boolean top) {
        this.name = name;
        this.side = side;
        this.top = top;
    }

    @Override
    public @NotNull String getSerializedName() {
        return name;
    }

    /** Where this part sits, given the core's position and facing. */
    public BlockPos fromCore(BlockPos core, Direction facing) {
        var pos = core;

        if (side) {
            pos = pos.relative(facing.getClockWise());
        }

        if (top) {
            pos = pos.above();
        }

        return pos;
    }

    /** The core's position, given this part's position and facing. */
    public BlockPos toCore(BlockPos pos, Direction facing) {
        var core = pos;

        if (side) {
            core = core.relative(facing.getCounterClockWise());
        }

        if (top) {
            core = core.below();
        }

        return core;
    }
}
