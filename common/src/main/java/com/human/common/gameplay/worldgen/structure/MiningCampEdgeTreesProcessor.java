package com.human.common.gameplay.worldgen.structure;

import com.human.common.registry.init.HumanStructureTypes;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Lets trees at the edge of a mining camp keep their branches and canopy over the camp's open ground.
 * <p>
 * [stated] Oct 23 - "if a tree is at the edge and doesnt break or overwrite blocks it can remain at the edge." The
 * camps are placed after all vegetation, and their files save AIR over the whole footprint above the grass, so until
 * now every edge tree was sliced flat at the camp's boundary.
 * </p>
 * <h2>The rule</h2> Only the camp's AIR is ever given up - its real blocks (walls, floors, roofs, lanterns, chests)
 * always overwrite whatever is there. An air cell keeps the world's block when ALL of these hold:
 * <ul>
 * <li>the cell is OPEN SKY in the camp file - above every camp block in its column - so nothing is ever kept inside a
 * room, under a roof, down the mine or in the elevator shaft;</li>
 * <li>the world block there is part of a tree ROOTED OUTSIDE the camp: a log whose trunk base is outside the footprint;
 * leaves that reach, through leaves, a log of such a tree within vanilla's leaf range (6); or vines, cocoa or a hanging
 * propagule touching a kept block (or a tree block outside the camp).</li>
 * </ul>
 * Trees rooted INSIDE the camp - in its yard - are cleared, trunk, canopy and all, so nothing grows in the camp and
 * nothing is left floating.
 * <h2>Safety and cost</h2>
 * <ul>
 * <li>World generation may only read the chunks around the one being placed; every read is checked with the region's
 * own {@code hasChunk}, and anything it cannot see counts as "not a tree", so the worst case is the old behaviour
 * (cleared), never a crash and never a chunk load.</li>
 * <li>Only cells inside the chunk being placed are considered (the template is processed once per chunk it covers).
 * Chunks with no tree blocks in the camp's open air return the list untouched after one pass.</li>
 * <li>Trunk and canopy searches are bounded ({@value #MAX_TRUNK_NODES} logs, {@value #MAX_LEAF_NODES} leaves) and
 * memoised per placement.</li>
 * </ul>
 * <p>
 * ⚠ It cannot see trees that do not exist yet: a neighbouring chunk that generates later can still lean a few leaves
 * into the camp's open air. That is vanilla's chunk-by-chunk generation and applies to every structure.
 * </p>
 */
public class MiningCampEdgeTreesProcessor extends StructureProcessor {

    public static final String NAME = "mining_camp_edge_trees";

    public static final MiningCampEdgeTreesProcessor INSTANCE = new MiningCampEdgeTreesProcessor();

    public static final MapCodec<MiningCampEdgeTreesProcessor> CODEC = MapCodec.unit(() -> INSTANCE);

    /** Logs explored to find a tree's trunk base; enough for a full 2x2 giant jungle tree with its branches. */
    private static final int MAX_TRUNK_NODES = 512;

    /** Vanilla leaves stay alive up to 6 blocks (through leaves) from a log; the same reach decides whose they are. */
    private static final int MAX_LEAF_DISTANCE = 6;

    private static final int MAX_LEAF_NODES = 256;

    private MiningCampEdgeTreesProcessor() {}

    @Override
    public @NotNull List<StructureTemplate.StructureBlockInfo> finalizeProcessing(
        @NotNull ServerLevelAccessor level,
        @NotNull BlockPos offset,
        @NotNull BlockPos pos,
        @NotNull List<StructureTemplate.StructureBlockInfo> originalBlockInfos,
        @NotNull List<StructureTemplate.StructureBlockInfo> processedBlockInfos,
        @NotNull StructurePlaceSettings settings
    ) {
        if (processedBlockInfos.isEmpty()) {
            return processedBlockInfos;
        }

        // The camp's footprint (world space, after rotation) and, per column, the highest camp block that is not air.
        var minX = Integer.MAX_VALUE;
        var maxX = Integer.MIN_VALUE;
        var minZ = Integer.MAX_VALUE;
        var maxZ = Integer.MIN_VALUE;
        var roofs = new HashMap<Long, Integer>();

        for (var info : processedBlockInfos) {
            var p = info.pos();
            minX = Math.min(minX, p.getX());
            maxX = Math.max(maxX, p.getX());
            minZ = Math.min(minZ, p.getZ());
            maxZ = Math.max(maxZ, p.getZ());

            if (!info.state().isAir()) {
                roofs.merge(column(p.getX(), p.getZ()), p.getY(), Math::max);
            }
        }

        var context = new Context(level, minX, maxX, minZ, maxZ);
        BoundingBox box = settings.getBoundingBox();

        // Open-sky air cells in this chunk where the world currently has part of a tree.
        var candidates = new ArrayList<BlockPos>();

        for (var info : processedBlockInfos) {
            var p = info.pos();

            if (!info.state().isAir() || (box != null && !box.isInside(p))) {
                continue;
            }

            var roof = roofs.get(column(p.getX(), p.getZ()));

            if (roof != null && p.getY() <= roof) {
                continue;
            }

            var world = context.read(p);

            if (world != null && (isLog(world) || isLeaves(world) || isAttachment(world))) {
                candidates.add(p);
            }
        }

        if (candidates.isEmpty()) {
            return processedBlockInfos;
        }

        var keep = new HashSet<BlockPos>();

        for (var p : candidates) {
            var world = context.read(p);

            if (world == null) {
                continue;
            }

            if (isLog(world) ? context.isRootedOutside(p) : isLeaves(world) && context.isLeafOfOutsideTree(p)) {
                keep.add(p);
            }
        }

        // Vines, cocoa and hanging propagules stay only where they hang on something that stays.
        for (var p : candidates) {
            var world = context.read(p);

            if (world != null && isAttachment(world) && context.isAttachedToKeptTree(p, keep)) {
                keep.add(p);
            }
        }

        if (keep.isEmpty()) {
            return processedBlockInfos;
        }

        var result = new ArrayList<StructureTemplate.StructureBlockInfo>(processedBlockInfos.size());

        for (var info : processedBlockInfos) {
            if (!keep.contains(info.pos())) {
                result.add(info);
            }
        }

        return result;
    }

    @Override
    protected @NotNull StructureProcessorType<?> getType() {
        return HumanStructureTypes.MINING_CAMP_EDGE_TREES.get();
    }

    private static long column(int x, int z) {
        return ((long) x << 32) | (z & 0xFFFFFFFFL);
    }

    static boolean isLog(BlockState state) {
        return state.is(BlockTags.LOGS) || state.is(Blocks.MANGROVE_ROOTS);
    }

    static boolean isLeaves(BlockState state) {
        return state.is(BlockTags.LEAVES);
    }

    static boolean isAttachment(BlockState state) {
        return state.is(Blocks.VINE) || state.is(Blocks.COCOA) || state.is(Blocks.MANGROVE_PROPAGULE);
    }

    /** One placement's reads and memos. */
    private static final class Context {

        private final ServerLevelAccessor level;

        private final int minX;

        private final int maxX;

        private final int minZ;

        private final int maxZ;

        private final Map<BlockPos, Boolean> logRooting = new HashMap<>();

        private final Map<BlockPos, Boolean> leafOwnership = new HashMap<>();

        private Context(ServerLevelAccessor level, int minX, int maxX, int minZ, int maxZ) {
            this.level = level;
            this.minX = minX;
            this.maxX = maxX;
            this.minZ = minZ;
            this.maxZ = maxZ;
        }

        private boolean insideFootprint(BlockPos p) {
            return p.getX() >= minX && p.getX() <= maxX && p.getZ() >= minZ && p.getZ() <= maxZ;
        }

        /** The world block, or null where world generation cannot see (treated as "not a tree"). */
        private BlockState read(BlockPos p) {
            if (level.isOutsideBuildHeight(p) || !level.hasChunk(p.getX() >> 4, p.getZ() >> 4)) {
                return null;
            }
            return level.getBlockState(p);
        }

        private boolean isLogAt(BlockPos p) {
            var state = read(p);
            return state != null && isLog(state);
        }

        /**
         * Whether the tree this log belongs to stands OUTSIDE the camp: its lowest logs (the trunk base) are outside
         * the footprint. A branch of an edge tree reaching over the camp is kept; a tree growing in the yard is not,
         * even if its branches reach outside. If the tree is too big to explore fully, any log outside the footprint is
         * taken as an outside tree - the safe side for "do not slice the edge trees".
         */
        boolean isRootedOutside(BlockPos start) {
            var cached = logRooting.get(start);
            if (cached != null) {
                return cached;
            }

            var visited = new HashSet<BlockPos>();
            var queue = new ArrayDeque<BlockPos>();
            visited.add(start);
            queue.add(start);
            var lowestY = start.getY();
            var anyOutside = !insideFootprint(start);

            while (!queue.isEmpty() && visited.size() < MAX_TRUNK_NODES) {
                var p = queue.poll();

                for (int dx = -1; dx <= 1; dx++) {
                    for (int dy = -1; dy <= 1; dy++) {
                        for (int dz = -1; dz <= 1; dz++) {
                            if (dx == 0 && dy == 0 && dz == 0) {
                                continue;
                            }
                            var n = p.offset(dx, dy, dz);
                            if (!visited.contains(n) && isLogAt(n)) {
                                visited.add(n);
                                queue.add(n);
                                lowestY = Math.min(lowestY, n.getY());
                                anyOutside |= !insideFootprint(n);
                            }
                        }
                    }
                }
            }

            boolean outside;

            if (!queue.isEmpty()) {
                outside = anyOutside;
            } else {
                outside = false;
                for (var p : visited) {
                    if (p.getY() == lowestY && !insideFootprint(p)) {
                        outside = true;
                        break;
                    }
                }
            }

            for (var p : visited) {
                logRooting.put(p, outside);
            }

            return outside;
        }

        /** Whether these leaves hang from an outside tree: through leaves, within reach, to one of its logs. */
        boolean isLeafOfOutsideTree(BlockPos start) {
            var cached = leafOwnership.get(start);
            if (cached != null) {
                return cached;
            }

            var distance = new HashMap<BlockPos, Integer>();
            var queue = new ArrayDeque<BlockPos>();
            distance.put(start, 0);
            queue.add(start);
            var owned = false;

            search:
            while (!queue.isEmpty() && distance.size() < MAX_LEAF_NODES) {
                var p = queue.poll();
                var d = distance.get(p);

                for (var direction : Direction.values()) {
                    var n = p.relative(direction);

                    if (distance.containsKey(n)) {
                        continue;
                    }

                    var state = read(n);

                    if (state == null) {
                        continue;
                    }

                    if (isLog(state)) {
                        if (isRootedOutside(n)) {
                            owned = true;
                            break search;
                        }
                        distance.put(n, d + 1);
                        continue;
                    }

                    if (isLeaves(state) && d + 1 < MAX_LEAF_DISTANCE) {
                        distance.put(n, d + 1);
                        queue.add(n);
                    }
                }
            }

            leafOwnership.put(start, owned);
            return owned;
        }

        /** Vines and the like stay if they touch a kept block, or a log or leaves standing outside the camp. */
        boolean isAttachedToKeptTree(BlockPos p, Set<BlockPos> keep) {
            for (var direction : Direction.values()) {
                var n = p.relative(direction);

                if (keep.contains(n)) {
                    return true;
                }

                if (!insideFootprint(n)) {
                    var state = read(n);
                    if (state != null && (isLog(state) || isLeaves(state))) {
                        return true;
                    }
                }
            }

            return false;
        }
    }
}
