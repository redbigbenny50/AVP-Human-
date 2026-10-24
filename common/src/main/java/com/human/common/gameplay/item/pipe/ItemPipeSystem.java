package com.human.common.gameplay.item.pipe;

import com.human.common.gameplay.block.item.ItemPipeBlock;
import com.human.common.gameplay.block.item.ItemPipeBlockEntity;
import com.human.common.gameplay.block.pipe.PipeBlock;
import com.human.common.gameplay.block.pipe.PipeBlockEntity;
import com.human.common.gameplay.block.pipe.PipeFilter;
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
 * The item pipe networks of one level. Pipes are dumb ([stated] Sep 29): a network moves items straight from the blocks
 * feeding it into the blocks accepting it.
 * <h2>THE RULES</h2>
 * <ul>
 * <li>RATE: every {@value #INTERVAL} ticks each pulling end moves up to {@value #ITEMS_PER_PULL} items - a full stack,
 * 256 a second. [stated] Oct 16: "if the pipes cant keep up with a quarry at full speed we need to allow the pipes to
 * transfer more" - a quarry with 4 speed cards digs 64 blocks a second (more with Fortune), and the old 8 a pull (32 a
 * second, level with Mekanism's advanced transporter and Oritech's pipe) let its storage fill. Same single move per end
 * per round, just a bigger handful, so the network does no more work.</li>
 * <li>RED ends give, GREEN ends take, YELLOW ends do both - by the block's face (storage: top and sides in, bottom out;
 * our machines: their own ports) or as the player set the end with the flange click.</li>
 * <li>[stated] no back and forth: yellow gives only to green and takes only from red.</li>
 * <li>No inventory feeds itself - both halves of a double chest count as one.</li>
 * <li>Ends take turns from pull to pull, so no chest is starved by its neighbour.</li>
 * </ul>
 * Networks are found from the pipes that touch a port and rebuilt at most every {@value #REBUILD_INTERVAL} ticks after
 * a change, exactly as the fluid networks are.
 */
public final class ItemPipeSystem {

    public static final int INTERVAL = 5;

    public static final int ITEMS_PER_PULL = 64;

    private static final int REBUILD_INTERVAL = 10;

    private static final int MODE_REFRESH_INTERVAL = 20;

    private static final Map<ServerLevel, ItemPipeSystem> SYSTEMS = new WeakHashMap<>();

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
    // This one meant NO item network ever formed: no flanges, nothing moved, anywhere.
    private long lastRebuild = Long.MIN_VALUE / 2;

    private ItemPipeSystem() {}

    public static ItemPipeSystem get(ServerLevel level) {
        synchronized (SYSTEMS) {
            return SYSTEMS.computeIfAbsent(level, key -> new ItemPipeSystem());
        }
    }

    /** Oct 13 - one block attached to a pipe network: where it is, and the pipe face that points at it. */
    public record Attached(
        BlockPos target,
        net.minecraft.core.Direction face
    ) {}

    /**
     * Oct 13 - every block attached to the live network {@code pipe} belongs to, for the fabricator's connected
     * storage. A map lookup and a copy of the network's own end list - no walk. Empty when the pipe is in no network.
     */
    public List<Attached> attachedTo(BlockPos pipe) {
        var network = pipeIndex.get(pipe);

        if (network == null) {
            return List.of();
        }

        var attached = new ArrayList<Attached>(network.ends.size());

        for (var end : network.ends) {
            attached.add(new Attached(end.target(), end.face()));
        }

        return attached;
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
                    "avp_human:item_pipe_rebuild",
                    0L,
                    System.nanoTime() - start
                );
            }
        }

        for (var network : networks) {
            // Oct 8 - SPREAD OVER TICKS: each network has its own phase, so a thousand networks do not all move on the
            // same tick and all refresh their flanges on the same tick. The rates are unchanged (every 5 ticks, every
            // 20 ticks); only which tick differs per network.
            var move = (time + network.phase) % INTERVAL == 0;
            var refreshModes = (time + network.phase) % MODE_REFRESH_INTERVAL == 0;

            if (!move && !refreshModes) {
                continue;
            }

            var start = profiling ? System.nanoTime() : 0L;

            if (move) {
                network.transfer(level);
            }

            if (refreshModes) {
                network.refreshModes(level);
            }

            if (profiling) {
                com.blib.internal.common.perf.BLibPerfProfiler.recordBlockEntity(
                    "avp_human:item_pipe_network",
                    network.anchor(),
                    System.nanoTime() - start
                );
            }
        }
    }

    private void rebuild(ServerLevel level) {
        portPipes.removeIf(pos -> level.isLoaded(pos) && !(level.getBlockState(pos).getBlock() instanceof ItemPipeBlock));

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

            if (!(state.getBlock() instanceof ItemPipeBlock)) {
                continue;
            }

            if (pipeIndex.containsKey(pos)) {
                discardNetworkAt(pos, seeds);
            }

            pipes.add(pos);

            for (var direction : Direction.values()) {
                var side = ItemPipeBlock.side(state, direction);
                var next = pos.relative(direction);

                if (side == PipeBlock.Side.PORT) {
                    ends.add(new End(pos.immutable(), direction, next.immutable()));
                } else if (side == PipeBlock.Side.PIPE && level.isLoaded(next) && visited.add(next.immutable())) {
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

    private record End(
        BlockPos pipe,
        Direction face,
        BlockPos target
    ) {

        PipeItemAccess.Port port(ServerLevel level) {
            return level.isLoaded(target) ? PipeItemAccess.open(level, target, face.getOpposite()) : PipeItemAccess.NONE;
        }

        /** What the block allows on this face, narrowed by the player's setting for this end. */
        byte mode(ServerLevel level, PipeItemAccess.Port port) {
            // The pipe unloaded since the network was last built: this end does nothing until it is back.
            if (!level.isLoaded(pipe)) {
                return 0;
            }

            if (!port.present()) {
                return 0;
            }

            var setting = level.getBlockEntity(pipe) instanceof ItemPipeBlockEntity entity
                ? entity.getOverride(face)
                : PipeBlockEntity.AUTO;

            // A setting can make any end of a two-way inventory pull or push - chest to chest needs exactly that.
            var twoWay = port.canInsert() || port.canExtract();
            var canIn = setting == PipeBlockEntity.AUTO ? port.canInsert() : twoWay;
            var canOut = setting == PipeBlockEntity.AUTO ? port.canExtract() : twoWay;

            return PipeBlockEntity.effective(setting, canIn, canOut);
        }

        /** [stated] Sep 30: this end's filter - with no entries, every item passes. */
        boolean allows(ServerLevel level, net.minecraft.world.item.ItemStack stack) {
            return !level.isLoaded(pipe) || !(level.getBlockEntity(pipe) instanceof ItemPipeBlockEntity entity)
                || entity.allowsItem(face, stack);
        }
    }

    // ------------------------------------------------------------------------------------------------------------
    // Wiring-tool readout
    // ------------------------------------------------------------------------------------------------------------

    /**
     * [stated] Oct 14: "build in that pipe detection with the wiring tool so it tells us whats not working or being
     * seen". Read-only: every end is worked out with the network's OWN port() and mode(), and sources are only peeked
     * at - an item port has no test insert, so a receiving end's free space is not checked.
     *
     * @param level the level
     * @param pipe  the clicked pipe
     * @return the lines to show the player
     */
    public List<net.minecraft.network.chat.Component> diagnose(ServerLevel level, BlockPos pipe) {
        var lines = new ArrayList<net.minecraft.network.chat.Component>();
        var network = pipeIndex.get(pipe);

        if (network == null) {
            lines.add(com.human.common.gameplay.block.pipe.PipeDiagnostics.header("Item pipe: not in a network"));
            lines.add(
                dirty
                    ? com.human.common.gameplay.block.pipe.PipeDiagnostics.note(
                        "The networks are about to be rebuilt - click again in a moment."
                    )
                    : com.human.common.gameplay.block.pipe.PipeDiagnostics.problem(
                        "No pipe in this run touches a block it can move items in or out of, so there is nothing to move between."
                    )
            );
            return lines;
        }

        var count = network.ends.size();
        lines.add(
            com.human.common.gameplay.block.pipe.PipeDiagnostics.header(
                "Item network: " + network.pipes.size() + (network.pipes.size() == 1 ? " pipe, " : " pipes, ") + count
                    + (count == 1 ? " end" : " ends")
            )
        );

        var ports = new PipeItemAccess.Port[count];
        var modes = new byte[count];
        var names = new String[count];

        for (var index = 0; index < count; index++) {
            var end = network.ends.get(index);
            ports[index] = end.port(level);
            modes[index] = end.mode(level, ports[index]);
            var entity = level.isLoaded(end.pipe()) && level.getBlockEntity(end.pipe()) instanceof ItemPipeBlockEntity pipeEntity
                ? pipeEntity
                : null;
            var setting = entity == null ? PipeBlockEntity.AUTO : entity.getOverride(end.face());
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
                if (!ports[index].present()) {
                    detail.add("this face does not take or give items");
                } else if (setting == PipeBlockEntity.OFF) {
                    detail.add("set to Off");
                } else {
                    detail.add("this face can neither take nor give items");
                }
            }

            if (modes[index] == PipeBlockEntity.OUT || modes[index] == PipeBlockEntity.BOTH) {
                var offer = ports[index].peek(64, stack -> true);
                detail.add(offer == null || offer.stack().isEmpty() ? "empty" : "has " + offer.stack().getHoverName().getString());
            }

            detail.add(
                com.human.common.gameplay.block.pipe.PipeDiagnostics.filterNote(entity == null ? null : entity.getFilter(end.face()))
            );
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

        lines.add(itemVerdict(level, network.ends, ports, modes, names));
        return lines;
    }

    /**
     * A dry run of one transfer: the first source with an item its filter lets out, to a sink whose filter lets it in.
     */
    private static net.minecraft.network.chat.Component itemVerdict(
        ServerLevel level,
        List<End> ends,
        PipeItemAccess.Port[] ports,
        byte[] modes,
        String[] names
    ) {
        var anySource = false;
        var anySink = false;

        for (var mode : modes) {
            anySource |= mode == PipeBlockEntity.OUT || mode == PipeBlockEntity.BOTH;
            anySink |= mode == PipeBlockEntity.IN || mode == PipeBlockEntity.BOTH;
        }

        if (!anySource) {
            return com.human.common.gameplay.block.pipe.PipeDiagnostics.problem(
                "Nothing moves: no end is extracting. Set the end at the block holding the items to Extract."
            );
        }

        if (!anySink) {
            return com.human.common.gameplay.block.pipe.PipeDiagnostics.problem(
                "Nothing moves: no end is taking items in. Set the end at the receiving block to Input."
            );
        }

        var anyItems = false;
        String blocked = null;

        for (var s = 0; s < ports.length; s++) {
            if (modes[s] != PipeBlockEntity.OUT && modes[s] != PipeBlockEntity.BOTH) {
                continue;
            }

            var source = ends.get(s);

            if (ports[s].peek(64, stack -> true) == null) {
                continue;
            }

            anyItems = true;
            var offer = ports[s].peek(64, stack -> source.allows(level, stack));

            if (offer == null || offer.stack().isEmpty()) {
                blocked = "the filter at " + names[s] + " does not let any of its items out";
                continue;
            }

            var item = offer.stack().getHoverName().getString();

            for (var d = 0; d < ports.length; d++) {
                if (d == s || modes[d] != PipeBlockEntity.IN && modes[d] != PipeBlockEntity.BOTH) {
                    continue;
                }

                if (!ends.get(d).allows(level, offer.stack())) {
                    blocked = "the filter at " + names[d] + " does not let " + item + " in";
                    continue;
                }

                return com.human.common.gameplay.block.pipe.PipeDiagnostics.ok(
                    "Working: " + item + " can move from " + names[s] + " to " + names[d] + " (if it has room)."
                );
            }
        }

        if (!anyItems) {
            return com.human.common.gameplay.block.pipe.PipeDiagnostics.problem("Nothing moves: every extracting end is empty.");
        }

        return com.human.common.gameplay.block.pipe.PipeDiagnostics.problem(
            "Nothing moves: " + (blocked == null ? "no receiving end besides the source itself." : blocked + ".")
        );
    }

    private static final class Network {

        /** Oct 8 - the longest an idle source sleeps, in transfer rounds (4 rounds = 20 ticks = 1 second). */
        private static final int MAX_SLEEP_ROUNDS = 4;

        private final List<End> ends;

        /**
         * Oct 8 - per end: rounds left to skip it as a source, and how long it slept last time (doubles while idle).
         */
        private final int[] sleepRounds;

        private final int[] sleepLength;

        private int turn;

        /**
         * Oct 8 - each end's mode as last seen (by a round or the flange refresh). Lets a round with every source
         * asleep - the normal state of a big idle base - skip opening a single inventory.
         */
        private byte @org.jetbrains.annotations.Nullable [] lastModes;

        /** Oct 8 - every pipe of this network, for incremental rebuilds. */
        private final List<BlockPos> pipes;

        /** Oct 8 - which tick of each interval this network works on; from its first pipe, so it survives rebuilds. */
        private final int phase;

        private final PipeItemAccess.Port[] portsBuffer;

        private final byte[] modesBuffer;

        private final PipeFilter[] filtersBuffer;

        private final boolean[] machinesBuffer;

        private final byte[] prioritiesBuffer;

        private final int[] sinkBuffer;

        private static final byte IN_MODE = PipeBlockEntity.IN;

        private static final byte BOTH_MODE = PipeBlockEntity.BOTH;

        Network(List<End> ends, List<BlockPos> pipes) {
            this.ends = ends;
            this.pipes = pipes;
            this.phase = pipes.isEmpty() ? 0 : Math.floorMod(pipes.get(0).hashCode(), MODE_REFRESH_INTERVAL);
            this.sleepRounds = new int[ends.size()];
            this.sleepLength = new int[ends.size()];
            // Oct 13 hardening - one set of working arrays per network, reused every round (its ends are fixed until
            // it is rebuilt), instead of six new ones per network per round.
            this.portsBuffer = new PipeItemAccess.Port[ends.size()];
            this.modesBuffer = new byte[ends.size()];
            this.filtersBuffer = new PipeFilter[ends.size()];
            this.machinesBuffer = new boolean[ends.size()];
            this.prioritiesBuffer = new byte[ends.size()];
            this.sinkBuffer = new int[ends.size()];
        }

        /**
         * One round. Oct 8 routing rules:
         * <ul>
         * <li>[stated] Oct 7 PRIORITY: each end routes at High, Normal or Low (Auto = machines Normal, storage Low). An
         * item is offered to the highest-priority accepting ends first;</li>
         * <li>[stated] Oct 7 TIE-BREAK: at equal priority MACHINES are offered it before STORAGE;</li>
         * <li>[stated] Oct 7 UPHILL: storage set to Both gives to other storage only if that storage is a higher
         * priority (it still feeds machines at any priority). Red (Out) storage ends feed anything, as before;</li>
         * <li>equal ends still take turns, so nothing is starved;</li>
         * <li>[stated] Oct 7 MOST WORN FIRST: a filter line matches every durability, and the most damaged one goes
         * first (in the port's peek);</li>
         * <li>PERFORMANCE: each end's filter, priority and kind are read once per round, not once per item and sink; a
         * source with nothing to give (or nowhere to put it) sleeps, doubling up to a second, so idle chests on a huge
         * network cost almost nothing.</li>
         * </ul>
         */
        /** Oct 8 - {@return one pipe of this network, as BlockPos.asLong - names it in /blib perf} */
        long anchor() {
            return ends.isEmpty() ? 0L : ends.get(0).pipe().asLong();
        }

        void transfer(ServerLevel level) {
            if (ends.size() < 2) {
                return;
            }

            var count = ends.size();

            if (allSourcesAsleep()) {
                return;
            }

            var ports = portsBuffer;
            var modes = modesBuffer;
            var filters = filtersBuffer;
            var machines = machinesBuffer;
            var priorities = prioritiesBuffer;

            for (var index = 0; index < count; index++) {
                var end = ends.get(index);
                ports[index] = end.port(level);
                modes[index] = end.mode(level, ports[index]);
                machines[index] = ports[index].isMachine();

                var entity = level.isLoaded(end.pipe()) && level.getBlockEntity(end.pipe()) instanceof ItemPipeBlockEntity pipe
                    ? pipe
                    : null;
                filters[index] = entity == null ? null : entity.getFilter(end.face());
                priorities[index] = filters[index] == null
                    ? (machines[index] ? PipeFilter.PRIORITY_NORMAL : PipeFilter.PRIORITY_LOW)
                    : filters[index].resolvePriority(machines[index]);
            }

            // Oct 14 audit: since the Oct 13 buffers, this makes lastModes the SAME array as modesBuffer, not a copy.
            // That is safe only because allSourcesAsleep() reads it BEFORE the next round overwrites it - keep that
            // order.
            lastModes = modes;

            var start = Math.floorMod(turn++, count);
            var sinks = sinkBuffer;
            var sinkCount = sinkOrder(count, start, modes, machines, priorities);

            // Red ends pull first, then yellow - a yellow block gives only what green blocks want.
            for (var pass = 0; pass < 2; pass++) {
                for (var offset = 0; offset < count; offset++) {
                    var s = (start + offset) % count;

                    if (modes[s] != (pass == 0 ? PipeBlockEntity.OUT : PipeBlockEntity.BOTH)) {
                        continue;
                    }

                    if (sleepRounds[s] > 0) {
                        sleepRounds[s]--;
                        continue;
                    }

                    var moved = offerFrom(s, ports, modes, filters, machines, priorities, sinks, sinkCount);

                    if (moved) {
                        sleepLength[s] = 0;
                    } else {
                        // Nothing to give, or nowhere to put it: sleep, doubling up to a second.
                        sleepLength[s] = Math.min(MAX_SLEEP_ROUNDS, Math.max(1, sleepLength[s] * 2));
                        sleepRounds[s] = sleepLength[s];
                    }
                }
            }
        }

        /**
         * Oct 8 - {@return true (and counts the sleepers down) when every end that gave last time is asleep} Nothing
         * can move this round, so no inventory is opened at all.
         */
        private boolean allSourcesAsleep() {
            var known = lastModes;

            if (known == null || known.length != ends.size()) {
                return false;
            }

            for (var index = 0; index < known.length; index++) {
                var source = known[index] == PipeBlockEntity.OUT || known[index] == PipeBlockEntity.BOTH;

                if (source && sleepRounds[index] <= 0) {
                    return false;
                }
            }

            for (var index = 0; index < known.length; index++) {
                if ((known[index] == PipeBlockEntity.OUT || known[index] == PipeBlockEntity.BOTH) && sleepRounds[index] > 0) {
                    sleepRounds[index]--;
                }
            }

            return true;
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

        /** @return whether anything moved out of source s */
        private static boolean offerFrom(
            int s,
            PipeItemAccess.Port[] ports,
            byte[] modes,
            PipeFilter[] filters,
            boolean[] machines,
            byte[] priorities,
            int[] sinks,
            int sinkCount
        ) {
            var source = ports[s];
            var sourceYellow = modes[s] == PipeBlockEntity.BOTH;
            var sourceFilter = filters[s];

            // A pulling end only offers what its filter lets through - the first allowed stack, not just the first.
            var offer = source.peek(ITEMS_PER_PULL, stack -> sourceFilter == null || sourceFilter.allowsItem(stack));

            if (offer == null) {
                return false;
            }

            var remaining = offer.stack().getCount();
            var moved = false;

            for (var k = 0; k < sinkCount; k++) {
                var d = sinks[k];

                if (remaining <= 0) {
                    break;
                }

                if (d == s) {
                    continue;
                }

                var sinkMode = modes[d];

                // [stated] no back and forth: yellow never feeds yellow. Nothing feeds itself.
                if (sourceYellow && sinkMode == PipeBlockEntity.BOTH || ports[d].storage().equals(source.storage())) {
                    continue;
                }

                // [stated] Oct 7 uphill: Both-set storage only climbs to higher-priority storage.
                if (sourceYellow && !machines[s] && !machines[d] && priorities[d] <= priorities[s]) {
                    continue;
                }

                // A pushing end only accepts what its filter lets through.
                var sinkFilter = filters[d];

                if (sinkFilter != null && !sinkFilter.allowsItem(offer.stack())) {
                    continue;
                }

                // In FIRST, then out: only what the destination really took leaves the source.
                var inserted = ports[d].insert(offer.stack().copyWithCount(remaining));

                if (inserted > 0) {
                    // Oct 13 hardening: the destination already holds these, so the source MUST give as many. If a
                    // misbehaving inventory gives fewer than its own dry run promised, the difference is taken straight
                    // back out of the destination - otherwise it would be duplicated.
                    var taken = offer.take(inserted);

                    if (taken < inserted) {
                        var undone = ports[d].retract(offer.stack(), inserted - taken);

                        if (taken + undone < inserted) {
                            com.human.Human.LOGGER.warn(
                                "Item pipe: a source gave {} of {} {} it promised; {} could not be taken back",
                                taken,
                                inserted,
                                offer.stack().getItem(),
                                inserted - taken - undone
                            );
                        }
                    }

                    remaining -= Math.max(0, taken);
                    moved |= taken > 0;

                    // A source not giving what it says gets no more offers this round.
                    if (taken < inserted) {
                        break;
                    }
                }
            }

            return moved;
        }

        void refreshModes(ServerLevel level) {
            var modes = new byte[ends.size()];

            for (var index = 0; index < ends.size(); index++) {
                var end = ends.get(index);
                modes[index] = end.mode(level, end.port(level));

                if (level.isLoaded(end.pipe()) && level.getBlockEntity(end.pipe()) instanceof ItemPipeBlockEntity pipe) {
                    pipe.setMode(end.face(), modes[index]);
                }
            }

            // A source the player just set (or a chest placed on a pipe) is noticed here even if every other end was
            // asleep, so nothing waits longer than the one-second refresh.
            lastModes = modes;
        }
    }
}
