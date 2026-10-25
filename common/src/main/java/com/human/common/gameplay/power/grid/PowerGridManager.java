package com.human.common.gameplay.power.grid;

import com.human.common.gameplay.power.PowerNode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class PowerGridManager {

    private static final Direction[] VALUES = Direction.values();

    private final PowerGridDSU dsu;

    // Maps root BlockPos to the actual PowerGrid.
    private final Map<BlockPos, PowerGrid> grids;

    /** Positions removed since the last tick, rebuilt in one pass. */
    private final Set<BlockPos> pendingSplits;

    /**
     * Positions a discovery walk refused because their chunk was not loaded. Re-checked periodically; when one becomes
     * loaded, the network is re-walked from there and the halves merge.
     */
    private final Set<BlockPos> frontier;

    private int frontierTimer;

    /**
     * [stated] Sep 30 - the FE adapter: the stand-in nodes for other mods' blocks on each cable's six faces (see
     * ExternalPortNodes). Replaced whenever the cable is re-examined, so a grid merge or rebuild never leaves two.
     */
    private final Map<BlockPos, PowerNode[]> externals = new HashMap<>();

    /**
     * [stated] Oct 24 - the faces that OFFER energy to another mod's block our cable cannot reach into itself (flux
     * plugs and points, energy pipes) - see ExposedCableFace. Kept beside {@link #externals} and cleared with it.
     */
    private final Map<BlockPos, com.human.common.gameplay.power.external.ExposedCableFace[]> exposed = new HashMap<>();

    /**
     * The face of {@code cable} that offers energy on {@code face}, or null - what the energy capabilities answer with.
     */
    public @org.jetbrains.annotations.Nullable com.human.common.gameplay.power.external.ExposedCableFace exposedFace(
        BlockPos cable,
        Direction face
    ) {
        var faces = exposed.get(cable);
        return faces == null ? null : faces[face.ordinal()];
    }

    /**
     * Cables whose arms and flange colours should be looked at again - every cable a discovery walk passes, so cables
     * already in a world when this loads get their flanges. Handled after the walk, in {@link #tick}: changing a block
     * inside a walk could merge grids while the walk is still going.
     */
    private final Set<BlockPos> pendingVisuals = new HashSet<>();

    /**
     * Members whose node left the grid by UNLOADING - its block entity went with its chunk, but the position is still
     * part of the network. A member that comes back on chunk load leaves this set ({@link #registerNode}); a block that
     * is actually broken leaves it when its split is rebuilt. Read by {@link #isGridPartlyUnloaded}.
     */
    private final Set<BlockPos> unloadedMembers = new HashSet<>();

    /** {@link #isGridPartlyUnloaded} answers, per grid root, for one tick - many machines on one grid ask once. */
    private final Map<BlockPos, Boolean> partlyUnloadedCache = new HashMap<>();

    private long partlyUnloadedTick = Long.MIN_VALUE;

    public PowerGridManager() {
        this.dsu = new PowerGridDSU();
        this.grids = new HashMap<>();
        this.pendingSplits = new HashSet<>();
        this.frontier = new HashSet<>();
    }

    /**
     * Called once per tick from the world or mod tick handler.
     * <p>
     * ⚠ Pending rebuilds are drained FIRST, so a tick never distributes power across a graph that is known to be stale.
     * </p>
     */
    public void tick(Level level) {
        if (!pendingSplits.isEmpty()) {
            rebuildPending(level);
        }

        // ⚠ Cheap by construction: the frontier holds only the handful of positions where a walk hit an unloaded
        // chunk, it is checked once every {@value #FRONTIER_INTERVAL_TICKS} ticks, and it is usually empty. A base
        // entirely inside loaded chunks never pays for this at all.
        if (!frontier.isEmpty() && ++frontierTimer >= FRONTIER_INTERVAL_TICKS) {
            frontierTimer = 0;
            retryFrontier(level);
        }

        // ⚠ Empty grids are dropped here rather than left in the map. A split leaves behind roots that own nothing,
        // and iterating them every tick forever is pure waste on a long-lived server.
        grids.values().removeIf(PowerGrid::isEmpty);

        for (var grid : grids.values()) {
            grid.tick();
        }

        if (!pendingVisuals.isEmpty() && level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            var cables = new ArrayList<>(pendingVisuals);

            pendingVisuals.clear();

            for (var cable : cables) {
                if (serverLevel.isLoaded(cable)) {
                    com.human.common.gameplay.block.power.CableBlock.refreshVisual(serverLevel, cable);
                }
            }
        }
    }

    /**
     * ⭐ THE FAST PATH, AND THE FIX FOR THE O(N^2) LOAD SPIKE.
     * <p>
     * Every power block entity used to run a full BFS of its whole network on its first tick and then call
     * {@link #union} once per discovered position - and union copies node sets between grids. On a base with N power
     * blocks on a network of M positions that is N BFS walks and N*M unions, all firing in the few ticks after a chunk
     * loads. A modest factory could stall the server on entry.
     * </p>
     * <p>
     * Now the FIRST node to wake discovers the network once, links every position into one root and registers every
     * power block entity it finds. Every later node on that network sees its position is already known and does nothing
     * but register itself - O(1). A node whose block entity had not been created yet when the walk ran is covered by
     * exactly that path when it wakes.
     * </p>
     */
    private static final int FRONTIER_INTERVAL_TICKS = 40;

    /**
     * Re-links across chunk borders that have since loaded.
     * <p>
     * ⭐ THIS IS WHAT MAKES AN OFFSITE GENERATOR WORK. A long cable run from a remote solar farm to a base passes
     * through chunks holding nothing but cable. Discovery stops at the first unloaded one, so the farm and the base
     * become two grids. Nothing in a cable-only chunk ticks, so nothing ever notices when that chunk loads - without
     * this, the two halves stay split for the rest of the world's life and the base simply never receives power.
     * </p>
     */
    private void retryFrontier(Level level) {
        var ready = new ArrayList<BlockPos>();

        for (var pos : frontier) {
            if (level.isLoaded(pos)) {
                ready.add(pos);
            }
        }

        if (ready.isEmpty()) {
            return;
        }

        frontier.removeAll(ready);

        for (var pos : ready) {
            if (!PowerGridExploreUtil.isConnectable(level, pos)) {
                continue;
            }

            if (level.getBlockEntity(pos) instanceof PowerNode node) {
                linkOnLoad(level, pos, node);
                continue;
            }

            // A plain cable: link it with no node of its own.
            linkPositions(level, pos, null);
        }
    }

    public void linkOnLoad(Level level, BlockPos pos, PowerNode node) {
        linkPositions(level, pos, node);
    }

    private void linkPositions(Level level, BlockPos pos, PowerNode node) {
        if (dsu.isKnown(pos) && node != null) {
            registerNode(pos, node);
            return;
        }

        if (dsu.isKnown(pos) && node == null) {
            return;
        }

        var component = PowerGridExploreUtil.discover(level, pos);

        if (component.isEmpty()) {
            if (node != null) {
                registerNode(pos, node);
            }

            return;
        }

        // Remember where this walk stopped, so a later chunk load can finish the job.
        frontier.addAll(PowerGridExploreUtil.frontierOf(level, component));

        // ⚠⚠ THE CHUNK-BOUNDARY MERGE, AND IT IS NOT OPTIONAL.
        //
        // The BFS stops at unloaded chunks (it must - see PowerGridExploreUtil). So a cable run crossing a chunk
        // border is discovered as TWO components at two different times, and without this step they would become two
        // separate grids that never merge: one base, two half-powered networks, and no way for a player to tell why.
        //
        // Worse, blindly re-parenting every discovered position would STEAL positions from a grid that already owns
        // them, leaving that grid in the map still holding its node references - the same block entity in two grids,
        // fed twice per tick.
        //
        // So: find every root already present in this component, adopt the first one as THE root, and fold the others
        // into it - nodes and all - instead of overwriting them.
        var root = pos;
        var absorbed = new HashSet<BlockPos>();

        for (var member : component) {
            if (dsu.isKnown(member)) {
                var existing = dsu.find(member);

                if (absorbed.isEmpty()) {
                    root = existing;
                }

                absorbed.add(existing);
            }
        }

        var grid = grids.computeIfAbsent(root, r -> new PowerGrid());

        for (var other : absorbed) {
            if (other.equals(root)) {
                continue;
            }

            var merged = grids.remove(other);

            if (merged != null) {
                merged.getProducers().forEach(grid::add);
                merged.getConsumers().forEach(grid::add);
                merged.getStores().forEach(grid::add);
            }

            dsu.setParent(other, root);
        }

        for (var member : component) {
            dsu.setParent(member, root);

            if (level.getBlockEntity(member) instanceof PowerNode found) {
                grid.add(found);
            }

            attachExternals(level, member, grid);
        }

        // The caller's own block entity may not be reachable via getBlockEntity yet during load.
        dsu.setParent(pos, root);

        if (node != null) {
            grid.add(node);
        }
    }

    /**
     * Queues a removed position for rebuild instead of rebuilding immediately.
     * <p>
     * ⚠ An explosion removes many blocks in one tick, and the old code ran up to six BFS walks PER REMOVED BLOCK. A
     * creeper in a cable room was a few hundred full network walks in a single tick. Batching them means one rebuild
     * pass per tick no matter how many blocks went.
     * </p>
     */
    public void queueSplit(BlockPos removedPos) {
        pendingSplits.add(removedPos.immutable());
    }

    private void rebuildPending(Level level) {
        var seeds = new HashSet<BlockPos>();

        for (var removed : pendingSplits) {
            var oldRoot = dsu.find(removed);

            dsu.remove(removed);
            unloadedMembers.remove(removed);
            grids.remove(oldRoot);
            // A removed cable's stand-ins went with its old grid.
            externals.remove(removed);
            exposed.remove(removed);

            for (var direction : VALUES) {
                seeds.add(removed.relative(direction));
            }
        }

        pendingSplits.clear();

        var visited = new HashSet<BlockPos>();

        for (var seed : seeds) {
            if (visited.contains(seed) || !PowerGridExploreUtil.isConnectable(level, seed)) {
                continue;
            }

            var component = PowerGridExploreUtil.discover(level, seed);

            if (component.isEmpty()) {
                continue;
            }

            var newRoot = component.iterator().next();
            var grid = new PowerGrid();

            grids.put(newRoot, grid);

            for (var pos : component) {
                dsu.setParent(pos, newRoot);
                visited.add(pos);

                if (level.getBlockEntity(pos) instanceof PowerNode node) {
                    grid.add(node);
                }

                attachExternals(level, pos, grid);
            }
        }
    }

    /**
     * Re-examines one cable's faces - a neighbouring block changed, or a face's mode did. A cable not yet on any grid
     * (touching only other mods' blocks) is linked first, which attaches its faces on the way.
     */
    public void refreshCable(Level level, BlockPos pos) {
        if (!(level.getBlockState(pos).getBlock() instanceof com.human.common.gameplay.block.power.CableBlock)) {
            return;
        }

        if (!dsu.isKnown(pos)) {
            linkPositions(level, pos, null);
            return;
        }

        var grid = gridAt(pos);

        if (grid != null) {
            attachExternals(level, pos, grid);
        }
    }

    /**
     * Puts a stand-in node on {@code grid} for each face of this cable that touches ANOTHER MOD's energy block, as that
     * face's mode says. Our own blocks are never external - they join the grid themselves.
     */
    private void attachExternals(Level level, BlockPos pos, PowerGrid grid) {
        var old = externals.remove(pos);

        if (old != null) {
            for (var node : old) {
                if (node != null) {
                    grid.remove(node);
                }
            }
        }

        var oldExposed = exposed.remove(pos);

        if (oldExposed != null) {
            for (var face : oldExposed) {
                if (face != null) {
                    grid.remove(face.inboundNode());
                    grid.remove(face.outboundNode());
                }
            }
        }

        com.human.common.gameplay.power.external.ExposedCableFace[] offered = null;

        if (
            !(level instanceof net.minecraft.server.level.ServerLevel serverLevel)
                || !(level.getBlockState(pos).getBlock() instanceof com.human.common.gameplay.block.power.CableBlock)
        ) {
            return;
        }

        var energy = com.human.common.gameplay.power.external.ExternalEnergy.Holder.get();
        var modes = com.human.common.gameplay.power.external.CablePortModes.get(serverLevel);
        PowerNode[] nodes = null;

        // Only a cable that meets a block - or still shows a flange to clear - needs its look re-checked after the
        // walk.
        var touchesBlock = level.getBlockState(pos).getValue(com.human.common.gameplay.block.power.CableBlock.PORTED);

        for (var direction : VALUES) {
            var target = pos.relative(direction);

            if (!level.isLoaded(target)) {
                continue;
            }

            var targetState = level.getBlockState(target);

            if (!targetState.isAir() && !(targetState.getBlock() instanceof com.human.common.gameplay.block.power.CableBlock)) {
                touchesBlock = true;
            }

            if (
                PowerGridExploreUtil.isConnectable(level, target)
                    || !com.human.common.gameplay.power.external.ExternalEnergy.mayHoldEnergy(level, target)
            ) {
                continue;
            }

            var port = energy.open(serverLevel, target, direction.getOpposite());
            var node = com.human.common.gameplay.power.external.ExternalPortNodes.create(port, modes.get(pos, direction));

            if (node != null) {
                if (nodes == null) {
                    nodes = new PowerNode[VALUES.length];
                }

                nodes[direction.ordinal()] = node;
                grid.add(node);
            } else {
                // Oct 24 - a block our cable cannot reach into (it pushes or pulls by itself): offer this face to it
                // instead, as its mode allows (ExposedCableFace). Never both on one face, so nothing can loop.
                var face = com.human.common.gameplay.power.external.ExposedCableFace.forMode(modes.get(pos, direction));
                if (face != null) {
                    if (offered == null) {
                        offered = new com.human.common.gameplay.power.external.ExposedCableFace[VALUES.length];
                    }
                    offered[direction.ordinal()] = face;
                    grid.add(face.inboundNode());
                    grid.add(face.outboundNode());
                }
            }
        }

        if (nodes != null) {
            externals.put(pos.immutable(), nodes);
        }

        if (offered != null) {
            exposed.put(pos.immutable(), offered);
        }

        if (touchesBlock) {
            pendingVisuals.add(pos.immutable());
        }
    }

    /**
     * Merges the grids of two positions (e.g., when a new cable connects them).
     */
    public void union(BlockPos a, BlockPos b) {
        var rootA = dsu.find(a);
        var rootB = dsu.find(b);

        if (rootA.equals(rootB)) {
            // Roots are the same, nothing to union here.
            return;
        }

        var gridA = grids.computeIfAbsent(rootA, r -> new PowerGrid());
        var gridB = grids.computeIfAbsent(rootB, r -> new PowerGrid());

        gridA.getProducers().forEach(gridB::add);
        gridA.getConsumers().forEach(gridB::add);
        gridA.getStores().forEach(gridB::add);

        // drop old grid.
        grids.remove(rootA);
        // merge.
        dsu.setParent(rootA, rootB);
    }

    /** @deprecated use {@link #queueSplit(BlockPos)}; this rebuilds immediately and does not batch. */
    @Deprecated
    public void splitGrid(Level level, BlockPos removedPos) {
        var oldRoot = dsu.find(removedPos);

        dsu.remove(removedPos);

        // Remove the old PowerGrid (if it exists).
        grids.remove(oldRoot);

        var visited = new HashSet<BlockPos>();

        for (var direction : VALUES) {
            var neighbor = removedPos.relative(direction);

            if (!visited.contains(neighbor) && PowerGridExploreUtil.isConnectable(level, neighbor)) {
                // BFS from this neighbor.
                var component = PowerGridExploreUtil.discover(level, neighbor);

                // Pick new root (e.g., first block).
                var newRoot = component.iterator().next();

                // Create new PowerGrid.
                var grid = new PowerGrid();
                grids.put(newRoot, grid);

                // Union everything into the new root.
                for (var pos : component) {
                    dsu.setParent(pos, newRoot);
                    visited.add(pos);

                    var blockEntity = level.getBlockEntity(pos);

                    if (blockEntity instanceof PowerNode node) {
                        grid.add(node);
                    }
                }
            }
        }
    }

    /**
     * The grid that owns this position, or null when the position has never been linked into one.
     * <p>
     * ⚠ Answers from the union-find only. It never walks the network and never touches the world, so it is cheap enough
     * for a machine to ask every second.
     * </p>
     */
    public @org.jetbrains.annotations.Nullable PowerGrid gridAt(BlockPos pos) {
        if (!dsu.isKnown(pos)) {
            return null;
        }

        return grids.get(dsu.find(pos));
    }

    /**
     * Registers a node (block entity) into the grid at the given position.
     */
    public void registerNode(BlockPos pos, PowerNode node) {
        unloadedMembers.remove(pos);

        var root = dsu.find(pos);
        grids.computeIfAbsent(root, r -> new PowerGrid()).add(node);
    }

    /**
     * Removes a node from its grid (e.g., block broken).
     */
    public void unregisterNode(BlockPos pos, PowerNode node) {
        // Called when a chunk unloads AND when a block is broken. Either way the position is noted; a broken block's
        // chunk is still loaded, so the check below ignores it, and its split rebuild takes it out of the set.
        unloadedMembers.add(pos.immutable());

        var root = dsu.find(pos);
        var grid = grids.get(root);

        if (grid != null) {
            grid.remove(node);
        }
    }

    /**
     * [stated] Oct 1 - agreed with the cryochamber work: whether the grid that owns {@code pos} is PARTLY UNLOADED, so
     * a machine with no power can tell "a battery or generator is in an unloaded chunk" (pause) from "the power is
     * really gone" (the grid ran dry, a breaker opened, a cable was cut).
     * <p>
     * True when the grid has a member whose chunk has unloaded, or touches its FRONTIER - where a discovery walk
     * stopped at an unloaded chunk (after a restart, the far half of a network is never walked until it loads). False
     * for a fully loaded grid, and for a position in no grid.
     * </p>
     * <p>
     * ⚠ Errs toward "partly unloaded": a frontier records where a walk could not look, not what is there, so a grid at
     * the edge of loaded ground can report true with nothing but stone beyond. For a cryochamber that is the safe side
     * - it pauses rather than thaws.
     * </p>
     * <p>
     * Never loads a chunk: it only asks whether chunks are loaded. Answers are cached per grid for the tick.
     * </p>
     */
    public boolean isGridPartlyUnloaded(Level level, BlockPos pos) {
        if (!dsu.isKnown(pos)) {
            return false;
        }

        var tick = level.getGameTime();

        if (tick != partlyUnloadedTick) {
            partlyUnloadedCache.clear();
            partlyUnloadedTick = tick;
        }

        var root = dsu.find(pos);
        var cached = partlyUnloadedCache.get(root);

        if (cached != null) {
            return cached;
        }

        var answer = false;
        var members = unloadedMembers.iterator();

        while (members.hasNext()) {
            var member = members.next();

            // Loaded again but never re-registered (or a broken block awaiting its split): not unloaded - drop it.
            if (level.isLoaded(member)) {
                members.remove();
                continue;
            }

            if (dsu.isKnown(member) && dsu.find(member).equals(root)) {
                answer = true;
                break;
            }
        }

        if (!answer) {
            frontierLoop:
            for (var edge : frontier) {
                if (level.isLoaded(edge)) {
                    continue;
                }

                for (var direction : VALUES) {
                    var inside = edge.relative(direction);

                    if (dsu.isKnown(inside) && dsu.find(inside).equals(root)) {
                        answer = true;
                        break frontierLoop;
                    }
                }
            }
        }

        partlyUnloadedCache.put(root, answer);

        return answer;
    }
}
