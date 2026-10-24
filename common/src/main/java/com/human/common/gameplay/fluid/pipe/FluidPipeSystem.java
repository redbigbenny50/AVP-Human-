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

    // Oct 14 FIX - was Long.MIN_VALUE: "now - Long.MIN_VALUE" OVERFLOWS to a huge negative number, so the time check
    // never passed. Half of it leaves room to subtract any real game time.
    // This one meant NO fluid network ever formed: no flanges, nothing moved, anywhere.
    private long lastRebuild = Long.MIN_VALUE / 2;

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

    // ------------------------------------------------------------------------------------------------------------
    // Wiring-tool readout
    // ------------------------------------------------------------------------------------------------------------

    /**
     * [stated] Oct 14: "build in that pipe detection with the wiring tool so it tells us whats not working or being
     * seen". Read-only: every end is worked out with the network's OWN port() and mode(), so the readout cannot
     * disagree with what the network does, and fluid is only ever peeked at or test-filled (simulate), never moved.
     *
     * @param level the level
     * @param pipe  the clicked pipe
     * @return the lines to show the player
     */
    public List<net.minecraft.network.chat.Component> diagnose(ServerLevel level, BlockPos pipe) {
        var lines = new ArrayList<net.minecraft.network.chat.Component>();
        var network = pipeIndex.get(pipe);

        if (network == null) {
            lines.add(com.human.common.gameplay.block.pipe.PipeDiagnostics.header("Fluid pipe: not in a network"));
            lines.add(
                dirty
                    ? com.human.common.gameplay.block.pipe.PipeDiagnostics.note(
                        "The networks are about to be rebuilt - click again in a moment."
                    )
                    : com.human.common.gameplay.block.pipe.PipeDiagnostics.problem(
                        "No pipe in this run touches a block it can move fluid in or out of, so there is nothing to move between."
                    )
            );
            return lines;
        }

        var count = network.ends.size();
        lines.add(
            com.human.common.gameplay.block.pipe.PipeDiagnostics.header(
                "Fluid network: " + network.pipes.size() + (network.pipes.size() == 1 ? " pipe, " : " pipes, ") + count
                    + (count == 1 ? " end" : " ends")
            )
        );

        var ports = new PipeFluidAccess.Port[count];
        var modes = new byte[count];
        var filters = new com.human.common.gameplay.block.pipe.PipeFilter[count];
        var names = new String[count];

        for (var index = 0; index < count; index++) {
            var end = network.ends.get(index);
            ports[index] = end.port(level);
            modes[index] = end.mode(level, ports[index]);
            var entity = level.isLoaded(end.pipe()) && level.getBlockEntity(end.pipe()) instanceof FluidPipeBlockEntity pipeEntity
                ? pipeEntity
                : null;
            filters[index] = entity == null ? null : entity.getFilter(end.face());
            var setting = entity == null ? FluidPipeBlockEntity.AUTO : entity.getOverride(end.face());
            names[index] = level.isLoaded(end.target())
                ? com.human.common.gameplay.block.pipe.PipeDiagnostics.where(level, end.target(), end.face())
                : "unloaded block";

            if (index >= com.human.common.gameplay.block.pipe.PipeDiagnostics.MAX_ENDS_LISTED) {
                continue;
            }

            var detail = new ArrayList<String>();

            if (!level.isLoaded(end.target())) {
                detail.add("that chunk is not loaded");
            } else if (modes[index] == 0) {
                detail.add(whyNotWorking(setting, ports[index]));
            }

            if (modes[index] == FluidPipeBlockEntity.OUT || modes[index] == FluidPipeBlockEntity.BOTH) {
                var held = ports[index].peek(Integer.MAX_VALUE);
                detail.add(
                    held.isEmpty()
                        ? "empty"
                        : "holds " + held.amount() + " mB "
                            + com.human.common.gameplay.item.canister.UniversalCanisterItem.fluidName(held.fluid()).getString()
                );
            }

            detail.add(com.human.common.gameplay.block.pipe.PipeDiagnostics.filterNote(filters[index]));
            lines.add(
                com.human.common.gameplay.block.pipe.PipeDiagnostics.endLine(
                    names[index],
                    setting,
                    modes[index],
                    com.human.common.gameplay.block.pipe.PipeDiagnostics.join(detail)
                )
            );
        }

        if (count > com.human.common.gameplay.block.pipe.PipeDiagnostics.MAX_ENDS_LISTED) {
            lines.add(
                com.human.common.gameplay.block.pipe.PipeDiagnostics.note(
                    " ...and " + (count - com.human.common.gameplay.block.pipe.PipeDiagnostics.MAX_ENDS_LISTED) + " more ends"
                )
            );
        }

        lines.add(fluidVerdict(ports, modes, filters, names));
        return lines;
    }

    private static String whyNotWorking(byte setting, PipeFluidAccess.Port port) {
        if (port instanceof FluidIntakePort) {
            return "open fluid - set this end to Extract to draw from it";
        }

        if (!port.present()) {
            return "this face does not take or give fluid";
        }

        return switch (setting) {
            case FluidPipeBlockEntity.OFF -> "set to Off";
            case FluidPipeBlockEntity.PULL -> "set to Extract, but this face cannot give fluid out";
            case FluidPipeBlockEntity.PUSH -> "set to Input, but this face cannot take fluid in";
            default -> "this face can neither take nor give fluid";
        };
    }

    /** A dry run of one transfer: the first source with fluid, through both filters, into a test-filled sink. */
    private static net.minecraft.network.chat.Component fluidVerdict(
        PipeFluidAccess.Port[] ports,
        byte[] modes,
        com.human.common.gameplay.block.pipe.PipeFilter[] filters,
        String[] names
    ) {
        var anySource = false;
        var anySink = false;

        for (var mode : modes) {
            anySource |= mode == FluidPipeBlockEntity.OUT || mode == FluidPipeBlockEntity.BOTH;
            anySink |= mode == FluidPipeBlockEntity.IN || mode == FluidPipeBlockEntity.BOTH;
        }

        if (!anySource) {
            return com.human.common.gameplay.block.pipe.PipeDiagnostics.problem(
                "Nothing moves: no end is extracting. Set the end at the block holding the fluid to Extract."
            );
        }

        if (!anySink) {
            return com.human.common.gameplay.block.pipe.PipeDiagnostics.problem(
                "Nothing moves: no end is taking fluid in. Set the end at the receiving block to Input."
            );
        }

        var anyFluid = false;
        String blocked = null;

        for (var s = 0; s < ports.length; s++) {
            if (modes[s] != FluidPipeBlockEntity.OUT && modes[s] != FluidPipeBlockEntity.BOTH) {
                continue;
            }

            var offer = ports[s].peek(1000);

            if (offer.isEmpty()) {
                continue;
            }

            anyFluid = true;
            var fluid = offer.fluid();
            var fluidName = com.human.common.gameplay.item.canister.UniversalCanisterItem.fluidName(fluid).getString();

            if (filters[s] != null && !filters[s].allowsFluid(fluid)) {
                blocked = "the filter at " + names[s] + " does not let " + fluidName + " out";
                continue;
            }

            for (var d = 0; d < ports.length; d++) {
                if (d == s || modes[d] != FluidPipeBlockEntity.IN && modes[d] != FluidPipeBlockEntity.BOTH) {
                    continue;
                }

                if (filters[d] != null && !filters[d].allowsFluid(fluid)) {
                    blocked = "the filter at " + names[d] + " does not let " + fluidName + " in";
                    continue;
                }

                if (ports[d].fill(fluid, offer.amount(), true) > 0) {
                    return com.human.common.gameplay.block.pipe.PipeDiagnostics.ok(
                        "Working: " + fluidName + " can move from " + names[s] + " to " + names[d] + "."
                    );
                }

                blocked = names[d] + " will not accept " + fluidName + " (it is full, or does not take that fluid)";
            }
        }

        if (!anyFluid) {
            return com.human.common.gameplay.block.pipe.PipeDiagnostics.problem("Nothing moves: every extracting end is empty.");
        }

        return com.human.common.gameplay.block.pipe.PipeDiagnostics.problem(
            "Nothing moves: " + (blocked == null ? "no receiving end besides the source itself." : blocked + ".")
        );
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

        private final PipeFluidAccess.Port[] portsBuffer;

        private final byte[] modesBuffer;

        private final com.human.common.gameplay.block.pipe.PipeFilter[] filtersBuffer;

        private final boolean[] machinesBuffer;

        private final byte[] prioritiesBuffer;

        private final int[] sinkBuffer;

        private static final byte IN_MODE = FluidPipeBlockEntity.IN;

        private static final byte BOTH_MODE = FluidPipeBlockEntity.BOTH;

        Network(List<End> ends, List<BlockPos> pipes) {
            this.ends = ends;
            this.pipes = pipes;
            this.phase = pipes.isEmpty() ? 0 : Math.floorMod(pipes.get(0).hashCode(), MODE_REFRESH_INTERVAL);
            this.sleepTicks = new int[ends.size()];
            this.sleepLength = new int[ends.size()];
            // Oct 13 hardening - one set of working arrays per network, reused every round (its ends are fixed until
            // it is rebuilt), instead of six new ones per network per round.
            this.portsBuffer = new PipeFluidAccess.Port[ends.size()];
            this.modesBuffer = new byte[ends.size()];
            this.filtersBuffer = new com.human.common.gameplay.block.pipe.PipeFilter[ends.size()];
            this.machinesBuffer = new boolean[ends.size()];
            this.prioritiesBuffer = new byte[ends.size()];
            this.sinkBuffer = new int[ends.size()];
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
            var ports = portsBuffer;
            var modes = modesBuffer;
            // Oct 8: each end's filter, kind and priority, read once per round (see the item network for the rules).
            var filters = filtersBuffer;
            var machines = machinesBuffer;
            var priorities = prioritiesBuffer;

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
            var sinks = sinkBuffer;
            var sinkCount = sinkOrder(count, start, modes, machines, priorities);

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
                    for (var sinkIndex = 0; sinkIndex < sinkCount && budget > 0 && !offer.isEmpty(); sinkIndex++) {
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
                            for (var later = sinkIndex + 1; later < sinkCount && leftover > 0; later++) {
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

        /**
         * Oct 13 hardening - the accepting ends, best first, written into this network's own buffer (no list, no
         * boxing, every round): priority, then machines before storage, then the round's turn. An insertion sort keeps
         * ends of the same priority and kind in turn order, exactly as the stable list sort did.
         *
         * @return how many sinks were written
         */
        private int sinkOrder(int count, int start, byte[] modes, boolean[] machines, byte[] priorities) {
            var written = 0;

            for (var offset = 0; offset < count; offset++) {
                var d = (start + offset) % count;

                if (modes[d] != IN_MODE && modes[d] != BOTH_MODE) {
                    continue;
                }

                var at = written;

                while (at > 0 && ranksBelow(sinkBuffer[at - 1], d, machines, priorities)) {
                    sinkBuffer[at] = sinkBuffer[at - 1];
                    at--;
                }

                sinkBuffer[at] = d;
                written++;
            }

            return written;
        }

        /** Whether end {@code a} belongs after end {@code b}: lower priority, or the same priority but storage. */
        private static boolean ranksBelow(int a, int b, boolean[] machines, byte[] priorities) {
            return priorities[a] < priorities[b] || priorities[a] == priorities[b] && !machines[a] && machines[b];
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
