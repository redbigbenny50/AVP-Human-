package com.human.common.gameplay.block.cryo;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Walks cryoconduit runs. [stated] Oct 1: conduits are connections only, so nothing is stored in them - the controller
 * and the tanks find each other by walking the run when they need to.
 * <p>
 * ⚠⚠ THE CHUNK GUARD. Every step asks {@code isLoaded} before reading a block, because reading a block in an unloaded
 * chunk LOADS it, and these walks run from ticks. An unloaded stretch simply ends the walk there; the controller treats
 * that as partly unloaded, never as power loss.
 * </p>
 */
public final class CryoNetwork {

    /** Far more conduit than any real base will lay; it only stops a runaway walk. */
    private static final int MAX_CONDUITS = 4096;

    private CryoNetwork() {}

    /** Every tank master the controller's conduit ports reach, in the order found. */
    public static List<BlockPos> tanksFrom(Level level, BlockPos controllerPos) {
        var masters = new LinkedHashSet<BlockPos>();
        var state = level.getBlockState(controllerPos);

        if (!(state.getBlock() instanceof CryotubeControllerBlock controller)) {
            return new ArrayList<>();
        }

        var starts = new ArrayList<BlockPos>();

        for (var direction : Direction.values()) {
            if (controller.acceptsCryoConduit(state, direction)) {
                starts.add(controllerPos.relative(direction));
            }
        }

        walk(level, starts, (conduit, direction, neighbour) -> {
            var neighbourState = level.getBlockState(neighbour);

            if (
                neighbourState.getBlock() instanceof CryotankBlock tankBlock
                    && tankBlock.acceptsCryoConduit(neighbourState, direction.getOpposite())
                    && level.getBlockEntity(neighbour) instanceof CryotankBlockEntity tank
            ) {
                masters.add(tank.getMasterPos());
            }

            return false;
        });

        return new ArrayList<>(masters);
    }

    /** The controller reached from this tank's top or bottom, or null. */
    public static @Nullable CryotubeControllerBlockEntity controllerFor(Level level, CryotankBlockEntity master) {
        var origin = master.getBlockPos();
        var starts = new ArrayList<BlockPos>();

        for (int x = 0; x < master.sizeX(); x++) {
            for (int z = 0; z < master.sizeZ(); z++) {
                starts.add(origin.offset(x, master.sizeY(), z));
                starts.add(origin.offset(x, -1, z));
            }
        }

        CryotubeControllerBlockEntity[] found = { null };

        walk(level, starts, (conduit, direction, neighbour) -> {
            var neighbourState = level.getBlockState(neighbour);

            if (
                neighbourState.getBlock() instanceof CryotubeControllerBlock controller
                    && controller.acceptsCryoConduit(neighbourState, direction.getOpposite())
                    && level.getBlockEntity(neighbour) instanceof CryotubeControllerBlockEntity entity
            ) {
                found[0] = entity;
                return true;
            }

            return false;
        });

        return found[0];
    }

    @FunctionalInterface
    private interface Visitor {

        /** @return true to stop the walk */
        boolean visit(BlockPos conduit, Direction direction, BlockPos neighbour);
    }

    private static void walk(Level level, List<BlockPos> starts, Visitor visitor) {
        Set<BlockPos> seen = new HashSet<>();
        var queue = new ArrayDeque<BlockPos>();

        for (var start : starts) {
            if (level.isLoaded(start) && level.getBlockState(start).getBlock() instanceof CryoConduitBlock && seen.add(start)) {
                queue.add(start);
            }
        }

        while (!queue.isEmpty() && seen.size() <= MAX_CONDUITS) {
            var conduit = queue.poll();

            for (var direction : Direction.values()) {
                var neighbour = conduit.relative(direction);

                if (!level.isLoaded(neighbour)) {
                    continue;
                }

                if (level.getBlockState(neighbour).getBlock() instanceof CryoConduitBlock) {
                    if (seen.add(neighbour)) {
                        queue.add(neighbour);
                    }

                    continue;
                }

                if (visitor.visit(conduit, direction, neighbour)) {
                    return;
                }
            }
        }
    }
}
