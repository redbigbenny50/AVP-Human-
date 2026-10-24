package com.human.common.gameplay.storage;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Where a crate's blocks go. {@code facing} is the direction the placing player looked: the front faces back toward
 * them, the crate runs away from them along {@code facing}, spreads across to their right, and up. The block they
 * clicked (the anchor, which is also the master) is the front-bottom block, centred across the width where the width is
 * odd.
 */
public final class CrateFootprint {

    private CrateFootprint() {}

    private static int anchorColumn(CrateSize size) {
        return (size.width() - 1) / 2;
    }

    /** Every block of the crate, the anchor first. */
    public static List<BlockPos> positions(BlockPos anchor, Direction facing, CrateSize size) {
        var right = facing.getClockWise();
        var column = anchorColumn(size);
        var list = new ArrayList<BlockPos>(size.blocks());

        list.add(anchor.immutable());

        for (int i = 0; i < size.width(); i++) {
            for (int j = 0; j < size.height(); j++) {
                for (int k = 0; k < size.length(); k++) {
                    if (i == column && j == 0 && k == 0) {
                        continue;
                    }

                    list.add(anchor.relative(right, i - column).relative(facing, k).above(j).immutable());
                }
            }
        }

        return list;
    }

    /** The bottom centre of the footprint, relative to the anchor block's corner, in blocks. */
    public static Vec3 centre(Direction facing, CrateSize size) {
        var right = facing.getClockWise();
        var across = (size.width() - 1) / 2.0 - anchorColumn(size);
        var along = (size.length() - 1) / 2.0;

        return new Vec3(
            0.5 + right.getStepX() * across + facing.getStepX() * along,
            0.0,
            0.5 + right.getStepZ() * across + facing.getStepZ() * along
        );
    }
}
