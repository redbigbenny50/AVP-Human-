package com.human.neoforge.compat.create;

import com.human.common.gameplay.power.PowerSystem;
import com.human.common.gameplay.power.grid.PowerGridExploreUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

/**
 * Joining and leaving avp_human's power grid, for blocks that cannot extend PowerNodeEntityBlock (the converters must
 * extend Create's kinetic blocks). These are the same calls PowerNodeEntityBlock makes, asked the same way: through the
 * ports on BOTH sides of a face, so a gear side never joins a cable.
 */
final class ConverterGrid {

    private ConverterGrid() {}

    static void onPlaced(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        var manager = PowerSystem.get(serverLevel);

        for (var direction : Direction.values()) {
            var neighbour = pos.relative(direction);

            if (PowerGridExploreUtil.portsOpen(level, pos, direction) && PowerGridExploreUtil.isConnectable(level, neighbour)) {
                manager.union(pos, neighbour);
            }
        }
    }

    /** Queued, as for every power block: one explosion can remove hundreds in a tick. */
    static void onRemoved(Level level, BlockPos pos) {
        if (level instanceof ServerLevel serverLevel) {
            PowerSystem.get(serverLevel).queueSplit(pos);
        }
    }
}
