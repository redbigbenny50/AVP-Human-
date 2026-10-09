package com.human.common.gameplay.fluid.pipe;

import com.human.common.gameplay.block.fluid.FluidPipeBlock;
import com.human.common.gameplay.block.fluid.FluidPipeBlockEntity;
import com.human.common.gameplay.fluid.FluidEndpoint;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * The fluid pipe networks of one level. [stated] Sep 29/30: pipes are DUMB - they hold nothing; a network moves fluid
 * straight from the blocks feeding it into the blocks accepting it.
 * <h2>THE RULES</h2>
 * <ul>
 * <li>RATE: {@value #FLOW_PER_TICK} mB a tick per network - 20 buckets a second, Mekanism's advanced pipe ([stated]
 * "match the other mods").</li>
 * <li>Faces: GREEN (in) only takes, RED (out) only gives, YELLOW (both) does both - as the block's face allows, or as
 * the player set that end with the flange click (Pull, Push, Both, Off).</li>
 * <li>[stated] "prevent back and forth feeding": yellow GIVES only to green and TAKES only from red. Two yellow blocks
 * never pour into each other, so fluid can never slosh between them forever.</li>
 * <li>A storage is never piped into itself - two faces of one multiblock tank count as one.</li>
 * <li>Sources and destinations take turns from tick to tick, so no tank is starved by its neighbour.</li>
 * </ul>
 * <h2>FINDING THE NETWORKS</h2> Plain pipes have no block entity, so they cannot announce themselves when a world
 * loads. Pipes that touch a PORT do ({@link FluidPipeBlockEntity}); every network worth running has at least one, and
 * is walked from them. Any change - a pipe placed or broken, a neighbour changed - marks the networks stale, and they
 * are rebuilt at most every {@value #REBUILD_INTERVAL} ticks, so a flickering neighbour cannot make them re-walk every
 * tick.
 */
public final class FluidPipeSystem {

    public static final int FLOW_PER_TICK = 1_000;

    private static final int REBUILD_INTERVAL = 10;

    private static final int MODE_REFRESH_INTERVAL = 20;

    /** Weakly held: a level that unloads is not pinned in memory by its pipes. */
    private static final Map<ServerLevel, FluidPipeSystem> SYSTEMS = new WeakHashMap<>();

    private final Set<BlockPos> portPipes = new HashSet<>();

    /**
     * Oct 8 - INCREMENTAL REBUILDS. Every pipe placed or broken, and every port pipe's chunk loading or unloading, used
     * to rebuild EVERY network in the level - on a big base, thousands of pipes re-walked because one chest was placed,
     * and again each time a player walked a chunk border. Now only the networks next to a change are re-walked: the
     * positions that changed are collected here, and each pipe knows its network through {@link #pipeIndex}.
     */
    private final Set<BlockPos> dirtyPositions = new HashSet<>();

    /** Oct 8 - each pipe of a live network, to its network. Rebuilt piece by piece with the networks. */
    private final java.util.Map<BlockPos, Network> pipeIndex = new java.util.HashMap<>();

    /** Oct 8 - the first build (and a flood of changes) walks everything; after that only what changed. */
    private boolean fullRebuild = true;

    /** Oct 8 - more changes than this in one rebuild and a full walk is cheaper than the bookkeeping. */
    private static final int MAX_INCREMENTAL_CHANGES = 2048;

    private final List<Network> networks = new ArrayList<>();

    private boolean dirty = true;

    private long lastRebuild = Long.MIN_VALUE;

    private FluidPipeSystem() {}

    public static FluidPipeSystem get(ServerLevel level) {
        synchronized (SYSTEMS) {
            return SYSTEMS.computeIfAbsent(level, key -> new FluidPipeSystem());
        }
    }

    public void registerPortPipe(BlockPos pos) {
        if (portPipes.add(pos.immutable())) {
            markChanged(pos);
        }
    }

    public void unregisterPortPipe(BlockPos pos) {
        if (portPipes.remove(pos)) {
            markChanged(pos);
        }
    }

    /** Something at or next to this pipe changed. */
    public void invalidate(BlockPos pos) {
        markChanged(pos);
    }

    private void markChanged(BlockPos pos) {
        dirty = true;

        if (!fullRebuild) {
            dirtyPositions.add(pos.immutable());

            if (dirtyPositions.size() > MAX_INCREMENTAL_CHANGES) {
                fullRebuild = true;
                dirtyPositions.clear();
            }
        }
    }

    public void tick(ServerLevel level) {
        var time = level.getGameTime();
        // Oct 8 - /blib perf: pipe networks appear in its BLOCK ENTITIES table ("count" = networks). One boolean read
        // when no session runs.
        var profiling = com.blib.internal.common.perf.BLibPerfProfiler.isActive();

        if (dirty && time - lastRebuild >= REBUILD_INTERVAL) {
            var start = profiling ? System.nanoTime() : 0L;
            rebuild(level);
            lastRebuild = time;
            dirty = false;

            if (profiling) {
                com.blib.internal.common.perf.BLibPerfProfiler.recordBlockEntity(
                    "avp_human:fluid_pipe_rebuild",
                    0L,
                    System.nanoTime() - start
                );
            }
        }

        for (var network : networks) {
            // Oct 8 - the flange refresh is spread over ticks by each network's phase (fluid moves every tick anyway).
            var refreshModes = (time + network.phase) % MODE_REFRESH_INTERVAL == 0;
            var start = profiling ? System.nanoTime() : 0L;
            network.transfer(level);

            if (refreshModes) {
                network.refreshModes(level);
            }

            if (profiling) {
                com.blib.internal.common.perf.BLibPerfProfiler.recordBlockEntity(
                    "avp_human:fluid_pipe_network",
                    network.anchor(),
                    System.nanoTime() - start
                );
            }
        }
    }

    // ------------------------------------------------------------------------------------------------------------
    // Discovery
    // ------------------------------------------------------------------------------------------------------------

    private void rebuild(ServerLevel level) {
        portPipes.removeIf(pos -> level.isLoaded(pos) && !(level.getBlockState(pos).getBlock() instanceof FluidPipeBlock));

        var seeds = new ArrayDeque<BlockPos>();

        if (fullRebuild) {
            networks.clear();
            pipeIndex.clear();
            seeds.addAll(portPipes);
            fullRebuild = false;
        } else {
            // Only the networks next to a change are thrown away and walked again.
            for (var changed : dirtyPositions) {
                discardNetworkAt(changed, seeds);

                for (var direction : Direction.values()) {
                    discardNetworkAt(changed.relative(direction), seeds);
                }

                seeds.add(changed);
            }
        }

        dirtyPositions.clear();

        // One walk per connected piece: every pipe is visited at most once per rebuild, even when a piece with no
        // ports left (nothing to keep) has hundreds of its pipes queued as seeds.
        var walked = new HashSet<BlockPos>();

        while (!seeds.isEmpty()) {
            var start = seeds.poll();

            if (walked.contains(start) || pipeIndex.containsKey(start) || !level.isLoaded(start)) {
                continue;
            }

            walkFrom(level, start, seeds, walked);
        }
    }

    /** Oct 8 - removes the live network a pipe belongs to (if any) and queues its pipes to be walked again. */
    private void discardNetworkAt(BlockPos pos, ArrayDeque<BlockPos> seeds) {
        var network = pipeIndex.get(pos);

        if (network == null) {
            return;
        }

        networks.remove(network);

        for (var pipe : network.pipes) {
            pipeIndex.remove(pipe);
        }

        seeds.addAll(network.pipes);
    }

    /**
     * Walks the pipes connected to {@code start} into one network. A network with no ports moves nothing and is not
     * kept; it is found again from its first port pipe when one appears. Meeting a pipe of a live network that was not
     * known to be affected (two networks joined by a newly loaded chunk, say) merges it in.
     */
    private void walkFrom(ServerLevel level, BlockPos start, ArrayDeque<BlockPos> seeds, Set<BlockPos> visited) {
        var ends = new ArrayList<End>();
        var pipes = new ArrayList<BlockPos>();
        var queue = new ArrayDeque<BlockPos>();

        queue.add(start);
        visited.add(start);

        while (!queue.isEmpty()) {
            var pos = queue.poll();
            var state = level.getBlockState(pos);

            if (!(state.getBlock() instanceof FluidPipeBlock)) {
                continue;
            }

            if (pipeIndex.containsKey(pos)) {
                discardNetworkAt(pos, seeds);
            }

            pipes.add(pos);

            for (var direction : Direction.values()) {
                var side = FluidPipeBlock.side(state, direction);
                var next = pos.relative(direction);

                if (side == FluidPipeBlock.Side.PORT) {
                    ends.add(new End(pos.immutable(), direction, next.immutable()));
                } else if (side == FluidPipeBlock.Side.PIPE && level.isLoaded(next) && visited.add(next.immutable())) {
                    queue.add(next.immutable());
                }
            }
        }

        if (ends.isEmpty()) {
            return;
        }

        var network = new Network(ends, pipes);
        networks.add(network);

        for (var pipe : pipes) {
            pipeIndex.put(pipe, network);
        }
    }

    /** One port: the pipe, its face, and the block it touches. */
    private record End(
        BlockPos pipe,
        Direction face,
        BlockPos target
    ) {

        PipeFluidAccess.Port port(ServerLevel level) {
            if (!level.isLoaded(target)) {
                return PipeFluidAccess.NONE;
            }

            var port = PipeFluidAccess.open(level, target, face.getOpposite());

            // Oct 7 - no container there: an open pipe end facing fluid in the world is an intake (FluidIntakePort).
            if (!port.present()) {
                var intake = FluidIntakePort.open(level, pipe, face, target);

                if (intake != null) {
                    return intake;
                }
            }

            return port;
        }

        /**
         * What the block allows on this face, narrowed by the player's setting for this end ([stated] the flange
         * click).
         */
        byte mode(ServerLevel level, PipeFluidAccess.Port port) {
            // The pipe unloaded since the network was last built: this end does nothing until it is back.
            if (!level.isLoaded(pipe)) {
                return 0;
            }

            if (!port.present()) {
                return 0;
            }

            var setting = level.getBlockEntity(pipe) instanceof FluidPipeBlockEntity entity
                ? entity.getOverride(face)
                : FluidPipeBlockEntity.AUTO;

            // Oct 7 - [stated] an intake works only once it is SET to intake: Pull. On Auto (or anything else) an end
            // dipped in a lake does nothing.
            if (port instanceof FluidIntakePort) {
                return setting == FluidPipeBlockEntity.PULL ? FluidPipeBlockEntity.OUT : 0;
            }

            return FluidPipeBlockEntity.effective(setting, port.canFill(), port.canDrain());
        }

        /** [stated] Sep 30: this end's filter - with no entries, every fluid passes. */
        boolean allows(ServerLevel level, net.minecraft.world.level.material.Fluid fluid) {
            return !level.isLoaded(pipe) || !(level.getBlockEntity(pipe) instanceof FluidPipeBlockEntity entity)
                || entity.allowsFluid(face, fluid);
        }
    }

    private static final class Network {

        private final List<End> ends;

        private int turn;

        /** Oct 8 - every pipe of this network, for incremental rebuilds. */
        private final List<BlockPos> pipes;

        /**
         * Oct 8 - per end: ticks left to skip it as a source, and how long it slept last time (doubles while it has
         * nothing to give or nowhere to give it, up to a second). Fluid networks run every tick, so an empty tank or a
         * full destination on a big network used to be asked again twenty times a second.
         */
        private final int[] sleepTicks;

        private final int[] sleepLength;

        private static final int MAX_SLEEP_TICKS = 20;

        /**
         * Oct 8 - which tick of the refresh interval this network uses; from its first pipe, so it survives rebuilds.
         */
        private final int phase;

        Network(List<End> ends, List<BlockPos> pipes) {
            this.ends = ends;
            this.pipes = pipes;
            this.phase = pipes.isEmpty() ? 0 : Math.floorMod(pipes.get(0).hashCode(), MODE_REFRESH_INTERVAL);
            this.sleepTicks = new int[ends.size()];
            this.sleepLength = new int[ends.size()];
        }

        /** Oct 8 - {@return one pipe of this network, as BlockPos.asLong - names it in /blib perf} */
        long anchor() {
            return ends.isEmpty() ? 0L : ends.get(0).pipe().asLong();
        }

        void transfer(ServerLevel level) {
            if (ends.size() < 2) {
                return;
            }

            var count = ends.size();
            var ports = new PipeFluidAccess.Port[count];
            var modes = new byte[count];
            // Oct 8: each end's filter, kind and priority, read once per round (see the item network for the rules).
            var filters = new com.human.common.gameplay.block.pipe.PipeFilter[count];
            var machines = new boolean[count];
            var priorities = new byte[count];

            for (var index = 0; index < count; index++) {
                var end = ends.get(index);
                ports[index] = end.port(level);
                modes[index] = end.mode(level, ports[index]);
                machines[index] = ports[index].isMachine();

                var entity = level.isLoaded(end.pipe()) && level.getBlockEntity(end.pipe()) instanceof FluidPipeBlockEntity pipe
                    ? pipe
                    : null;
                filters[index] = entity == null ? null : entity.getFilter(end.face());
                priorities[index] = filters[index] == null
                    ? (machines[index]
                        ? com.human.common.gameplay.block.pipe.PipeFilter.PRIORITY_NORMAL
                        : com.human.common.gameplay.block.pipe.PipeFilter.PRIORITY_LOW)
                    : filters[index].resolvePriority(machines[index]);
            }

            var budget = FLOW_PER_TICK;
            var start = Math.floorMod(turn++, count);
            var sinks = sinkOrder(count, start, modes, machines, priorities);

            // Red sources first, then yellow - a yellow block gives only what green blocks want.
            for (var pass = 0; pass < 2 && budget > 0; pass++) {
                for (var offset = 0; offset < count && budget > 0; offset++) {
                    var s = (start + offset) % count;
                    var source = ports[s];
                    var sourceMode = modes[s];

                    // Red ends in the first pass, yellow in the second.
                    if (sourceMode != (pass == 0 ? FluidPipeBlockEntity.OUT : FluidPipeBlockEntity.BOTH)) {
                        continue;
                    }

                    if (sleepTicks[s] > 0) {
                        sleepTicks[s]--;
                        continue;
                    }

                    var sourceYellow = sourceMode == FluidPipeBlockEntity.BOTH;
                    var offer = source.peek(budget);
                    var budgetBefore = budget;

                    // A pulling end only offers what its filter lets through.
                    if (offer.isEmpty() || filters[s] != null && !filters[s].allowsFluid(offer.fluid())) {
                        sleep(s, source);
                        continue;
                    }

                    // Oct 8: best sinks first - priority, then machines before storage, then the round's turn.
                    for (var sinkIndex = 0; sinkIndex < sinks.length && budget > 0 && !offer.isEmpty(); sinkIndex++) {
                        var d = sinks[sinkIndex];

                        if (d == s) {
                            continue;
                        }

                        var sink = ports[d];

                        var sinkMode = modes[d];

                        if (
                            (sinkMode != FluidPipeBlockEntity.IN && sinkMode != FluidPipeBlockEntity.BOTH)
                                || sink.storage().equals(source.storage())
                        ) {
                            continue;
                        }

                        var sinkYellow = sinkMode == FluidPipeBlockEntity.BOTH;

                        // A pushing end only accepts what its filter lets through.
                        if (filters[d] != null && !filters[d].allowsFluid(offer.fluid())) {
                            continue;
                        }

                        // [stated] no back and forth: yellow gives only to green, and takes only from red.
                        if (sourceYellow && sinkYellow) {
                            continue;
                        }

                        // [stated] Oct 7 uphill: Both-set storage only climbs to higher-priority storage.
                        if (sourceYellow && !machines[s] && !machines[d] && priorities[d] <= priorities[s]) {
                            continue;
                        }

                        var wanted = sink.fill(offer.fluid(), Math.min(offer.amount(), budget), true);

                        if (wanted <= 0) {
                            continue;
                        }

                        var taken = source.drain(offer.fluid(), wanted, false);

                        if (taken <= 0) {
                            // Oct 7 - an intake only gives WHOLE buckets: a sink with room for less refuses it, and
                            // the next sink may still take the bucket. Any other source that gives nothing is spent.
                            if (source instanceof FluidIntakePort) {
                                continue;
                            }

                            break;
                        }

                        var put = sink.fill(offer.fluid(), taken, false);

                        // Whatever the sink refused after all goes back where it came from.
                        if (put < taken) {
                            var leftover = taken - put - source.fill(offer.fluid(), taken - put, false);

                            // Oct 8 - NOTHING IS DELETED. A source that cannot take fluid back (an intake has already
                            // lifted the world block; an output-only port refuses fills) used to lose the remainder.
                            // Offer it to the remaining sinks in order; only fluid nobody will hold at all is lost,
                            // and that needs a sink that lied in its own simulation.
                            for (var later = sinkIndex + 1; later < sinks.length && leftover > 0; later++) {
                                var other = sinks[later];

                                if (other == s || other == d || ports[other].storage().equals(source.storage())) {
                                    continue;
                                }

                                if (filters[other] != null && !filters[other].allowsFluid(offer.fluid())) {
                                    continue;
                                }

                                leftover -= ports[other].fill(offer.fluid(), leftover, false);
                            }
                        }

                        budget -= put;
                        offer = offer.withAmount(offer.amount() - put);
                    }

                    if (budget < budgetBefore) {
                        sleepLength[s] = 0;
                    } else {
                        sleep(s, source);
                    }
                }
            }
        }

        /** Oct 8 - an end with nothing moved sleeps, doubling up to a second. */
        private void sleep(int s, PipeFluidAccess.Port source) {
            // An intake has its own one-bucket-per-interval clock; sleeping on top would slow it down.
            if (source instanceof FluidIntakePort) {
                return;
            }

            sleepLength[s] = Math.min(MAX_SLEEP_TICKS, Math.max(1, sleepLength[s] * 2));
            sleepTicks[s] = sleepLength[s];
        }

        /** @return the accepting ends, best first: priority, then machines before storage, then the round's turn */
        private static int[] sinkOrder(int count, int start, byte[] modes, boolean[] machines, byte[] priorities) {
            var order = new ArrayList<Integer>(count);

            for (var offset = 0; offset < count; offset++) {
                var d = (start + offset) % count;

                if (modes[d] == FluidPipeBlockEntity.IN || modes[d] == FluidPipeBlockEntity.BOTH) {
                    order.add(d);
                }
            }

            // Stable: ends of the same priority and kind keep the rotating turn order.
            order.sort((a, b) -> {
                var byPriority = Byte.compare(priorities[b], priorities[a]);
                return byPriority != 0 ? byPriority : Boolean.compare(machines[b], machines[a]);
            });

            var result = new int[order.size()];

            for (var index = 0; index < result.length; index++) {
                result[index] = order.get(index);
            }

            return result;
        }

        /** Keeps each port pipe's flange colour matching what the block's face does now. */
        void refreshModes(ServerLevel level) {
            for (var end : ends) {
                if (!level.isLoaded(end.pipe()) || !(level.getBlockEntity(end.pipe()) instanceof FluidPipeBlockEntity pipe)) {
                    continue;
                }

                pipe.setMode(end.face(), end.mode(level, end.port(level)));
            }
        }
    }

    /** Whether this block entity is one of our fluid blocks - for callers that only care about ours. */
    public static boolean isOurs(Object blockEntity) {
        return blockEntity instanceof FluidEndpoint;
    }
}
