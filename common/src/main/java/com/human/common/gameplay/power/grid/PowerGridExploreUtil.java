package com.human.common.gameplay.power.grid;

import com.human.common.gameplay.block.entity.power.PowerNodeBlockEntity;
import com.human.common.gameplay.power.PowerNode;
import com.human.common.registry.init.HumanBlocks;
import com.just.core.traversal.BFS;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class PowerGridExploreUtil {

    private static final Direction[] VALUES = Direction.values();

    /**
     * Performs a BFS traversal to find all physically connected cable blocks and power nodes.
     *
     * @param start The position to begin traversal.
     * @return A set of all positions in the same network component.
     */
    public static Set<BlockPos> discover(Level level, BlockPos start) {
        var visited = new HashSet<BlockPos>();

        BFS.traverse(
            start,
            current -> Arrays.stream(VALUES)
                // ⭐ THE BREAKER CHECK, AND IT MUST BE ON BOTH SIDES. A face is only traversable when the block we
                // are leaving has that face open AND the block we are entering has ITS facing side open. Checking
                // one end would make a closed port asymmetric - power could not leave the battery but could still be
                // pulled out of it from the far side, which is the kind of one-way leak nobody would ever find.
                .filter(direction -> portsOpen(level, current, direction))
                .map(current::relative)
                .filter(relPos -> isConnectable(level, relPos))
                .toList(),
            visited::add
        );

        return visited;
    }

    /**
     * Determines if a connection exists between two adjacent positions.
     */
    /**
     * Collects the positions the walk REFUSED because their chunk was not loaded.
     * <p>
     * ⚠⚠ Without this the grid silently splits forever. A cable run between an offsite generator and a base crosses
     * chunks that contain nothing but cable - no block entity, nothing that ticks. When those chunks load, NOTHING
     * fires to re-link the two halves, because only a power block entity waking up triggers discovery and there are no
     * power block entities in a corridor of plain cable. The frontier is what lets the manager notice.
     * </p>
     */
    public static Set<BlockPos> frontierOf(Level level, Set<BlockPos> component) {
        var frontier = new HashSet<BlockPos>();

        for (var member : component) {
            for (var direction : Direction.values()) {
                var neighbour = member.relative(direction);

                if (!level.isLoaded(neighbour) && !component.contains(neighbour)) {
                    frontier.add(neighbour.immutable());
                }
            }
        }

        return frontier;
    }

    /** True when neither side has closed the face between these two positions. */
    public static boolean portsOpen(Level level, BlockPos from, Direction direction) {
        // ⚠ The chunk guard applies here too: never read a block entity in an unloaded chunk just to test a port.
        if (!level.isLoaded(from)) {
            return false;
        }

        var to = from.relative(direction);

        // Oct 22 - two power blocks that join each other directly (solar panels side by side) are connected whatever
        // their cable faces say - see PowerNodeBlockEntity.joinsNeighbour. Both must agree.
        if (
            level.isLoaded(to)
                && level.getBlockEntity(from) instanceof PowerNodeBlockEntity fromNode
                && level.getBlockEntity(to) instanceof PowerNodeBlockEntity toNode
                && fromNode.joinsNeighbour(direction, toNode)
                && toNode.joinsNeighbour(direction.getOpposite(), fromNode)
        ) {
            return true;
        }

        if (level.getBlockEntity(from) instanceof PowerNodeBlockEntity node && !node.isFaceConnectable(direction)) {
            return false;
        }

        if (!level.isLoaded(to)) {
            return false;
        }

        return !(level.getBlockEntity(to) instanceof PowerNodeBlockEntity neighbour)
            || neighbour.isFaceConnectable(direction.getOpposite());
    }

    public static boolean isConnectable(Level level, BlockPos blockPos) {
        // ⚠⚠ THE CHUNK GUARD. level.getBlockState() on an unloaded position LOADS THAT CHUNK, so a cable run reaching
        // a chunk border used to drag the neighbouring chunk into memory - and each newly loaded chunk could extend
        // the walk again. One cable laid toward unexplored terrain could chain-load a corridor of chunks and stall
        // the server. An unloaded neighbour is simply not connected; when it loads, its own placement re-links it.
        if (!level.isLoaded(blockPos)) {
            return false;
        }

        var toState = level.getBlockState(blockPos);

        // Check if the destination block is a cable or power node.
        var toBlock = toState.getBlock();

        if (toBlock == HumanBlocks.CABLE.get()) {
            return true;
        }

        return level.getBlockEntity(blockPos) instanceof PowerNode;
    }
}
