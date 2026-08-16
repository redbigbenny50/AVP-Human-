package com.human.common.gameplay.explosion.nuke;

import com.blib.api.common.server.v1.ServerScheduler;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.Fluids;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Empties the water out of a finished crater.
 * <p>
 * A blast under or beside an ocean leaves a bowl that the sea immediately fills, which hides the crater and undoes the
 * look entirely. This clears every water block in the cylinder above the crater — the full crater diameter, from its
 * floor upward — so what is left is an open hole rather than a lake.
 * <p>
 * The upper bound of each column comes from the {@code MOTION_BLOCKING} heightmap, which counts water, so a column on
 * dry land scans almost nothing and only genuinely flooded columns cost anything. Scanning to the world ceiling instead
 * would be millions of pointless checks for the common case.
 * <p>
 * Like the fallout spread this is budgeted across ticks. It runs once the explosion has finished, so it is draining a
 * crater that has stopped changing shape rather than racing the blast front.
 * <p>
 * Water outside the cleared radius is untouched and will seep back in over time. Holding it out permanently would mean
 * walling the crater, which is a different feature.
 */
public class NuclearCraterDrainer {

    /**
     * Columns drained per tick. Each one is a heightmap lookup plus a short vertical scan, so this is far cheaper per
     * unit than a chunk conversion and can afford a bigger batch.
     */
    private static final int COLUMNS_PER_CYCLE = 256;

    private static final int BLOCK_FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_SUPPRESS_DROPS | Block.UPDATE_KNOWN_SHAPE;

    private final ServerLevel level;

    private final List<BlockPos> columns;

    private final int floorY;

    private int index = 0;

    private NuclearCraterDrainer(ServerLevel level, List<BlockPos> columns, int floorY) {
        this.level = level;
        this.columns = columns;
        this.floorY = floorY;
    }

    /**
     * Begins draining the cylinder of radius {@code radius} centred on {@code center}, from {@code depth} blocks below
     * the centre upward.
     */
    public static void drain(ServerLevel level, BlockPos center, int radius, int depth) {
        var radiusSquared = radius * radius;
        var columns = new ArrayList<BlockPos>();

        for (var dx = -radius; dx <= radius; dx++) {
            for (var dz = -radius; dz <= radius; dz++) {
                if (dx * dx + dz * dz > radiusSquared) {
                    continue;
                }

                columns.add(new BlockPos(center.getX() + dx, 0, center.getZ() + dz));
            }
        }

        var floorY = Math.max(level.getMinBuildHeight(), center.getY() - depth);

        new NuclearCraterDrainer(level, columns, floorY).scheduleNextCycle();
    }

    private void scheduleNextCycle() {
        ServerScheduler.schedule(this::processCycle, Duration.ofMillis(50));
    }

    private void processCycle() {
        var limit = Math.min(index + COLUMNS_PER_CYCLE, columns.size());
        var cursor = new BlockPos.MutableBlockPos();

        for (; index < limit; index++) {
            var column = columns.get(index);
            var topY = level.getHeight(Heightmap.Types.MOTION_BLOCKING, column.getX(), column.getZ());

            for (var y = floorY; y <= topY; y++) {
                cursor.set(column.getX(), y, column.getZ());

                if (level.getFluidState(cursor).isEmpty()) {
                    continue;
                }

                var state = level.getBlockState(cursor);

                if (state.hasProperty(BlockStateProperties.WATERLOGGED)) {
                    // Drain the block rather than destroying it — a waterlogged fence is still a fence.
                    if (state.getValue(BlockStateProperties.WATERLOGGED)) {
                        level.setBlock(cursor, state.setValue(BlockStateProperties.WATERLOGGED, false), BLOCK_FLAGS);
                    }

                    continue;
                }

                if (
                    state.getFluidState().getType() == Fluids.WATER
                        || state.getFluidState().getType() == Fluids.FLOWING_WATER
                ) {
                    level.setBlock(cursor, Blocks.AIR.defaultBlockState(), BLOCK_FLAGS);
                }
            }
        }

        if (index < columns.size()) {
            scheduleNextCycle();
        }
    }
}
