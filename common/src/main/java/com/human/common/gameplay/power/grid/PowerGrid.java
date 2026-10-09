package com.human.common.gameplay.power.grid;

import com.human.common.gameplay.power.PowerNode;
import com.human.common.gameplay.power.diag.PowerDiagnostics;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * One electrically connected network, and the per-tick accounting for it.
 * <h2>⚠⚠ WHY THIS WAS REWRITTEN - the old tick created and destroyed power</h2>
 * <ol>
 * <li><b>Return values were ignored.</b> {@code consumer.receivePower(requested)} was called and the result thrown
 * away, so a consumer that accepted less than it asked for had the difference VANISH, and one that accepted power the
 * grid had not actually extracted got it for free.</li>
 * <li><b>Producers were drained completely.</b> Both branches ended with {@code producer.extractPower(offered)} for
 * every producer regardless of what consumers used, so surplus generation was destroyed rather than banked - and in the
 * surplus path producers were extracted TWICE in one tick.</li>
 * <li><b>Rationing was all-or-nothing.</b> If producers plus stores could not cover the full demand, the code fell
 * through and NOBODY got anything - a grid one FE short ran completely dead instead of running slightly slow.</li>
 * <li><b>Repeated polling.</b> {@code getAvailablePower()} and {@code getRequestedPower()} were called up to four times
 * per node per tick, through streams and again in each loop. Every one of those is real work on a block entity (a solar
 * panel reads the sky, a thermoelectric reads five neighbours), and worse, a node answering differently between two
 * calls in the same tick breaks the arithmetic silently.</li>
 * </ol>
 * <h2>The order of operations, which is now fixed and deliberate</h2>
 * <ol>
 * <li>Poll every node ONCE into a snapshot. Nothing is asked twice in a tick.</li>
 * <li>Serve consumers from PRODUCERS first - generation is use-it-or-lose-it, stored power is not.</li>
 * <li>Cover any shortfall from STORES.</li>
 * <li>Deliver, honouring each consumer's accepted amount, and extract from producers EXACTLY what was delivered.</li>
 * <li>Charge stores from whatever generation is left over.</li>
 * </ol>
 * ⭐ Step 2-before-3 and step 5 together are what stop a battery charging and discharging on the same tick, and stop two
 * batteries trading charge back and forth forever. A single node cannot enforce that about itself; only the grid can,
 * which is why {@code BatteryBlockEntity} documents it as out of its hands.
 * <h2>Rationing when short</h2> Consumers are served by PRIORITY first
 * ({@link PowerNode.PowerConsumer#getPowerPriority()}, lower number served first), and within one priority band the
 * remaining power is split PROPORTIONALLY to what each asked for. So a cryotube keeps its occupant alive while the
 * lights dim, and two identical machines brown out together rather than one running and one dying by map iteration
 * order.
 */
public class PowerGrid {

    private final Set<PowerNode.PowerConsumer> consumers;

    private final Set<PowerNode.PowerProducer> producers;

    private final Set<PowerNode.PowerStore> stores;

    /**
     * ⚠ REUSED ACROSS TICKS, NEVER ESCAPES THIS CLASS. A grid ticks once per server tick on the server thread, so these
     * are safe to keep and refill. Allocating two ArrayLists per grid per tick is 40 short-lived lists a second for a
     * single grid, and a base with a dozen networks turns that into steady GC pressure for no reason - the kind of cost
     * that never shows in a profiler as one big number but raises the floor of every tick.
     */
    private final List<Offer<PowerNode.PowerProducer>> supplyBuffer;

    private final List<Offer<PowerNode.PowerConsumer>> demandBuffer;

    /**
     * ⚠⚠ TWO OUTPUT BUFFERS, AND THAT IS NOT REDUNDANCY. {@code chargeStores} passes the list it received from
     * {@code drawFromProducers} straight back INTO {@code drawFromProducers}. With one shared buffer the nested call
     * clears the very list it is about to iterate, the loop sees nothing, the producers are never billed and the stores
     * keep power that was never generated. A fuzz run caught this on 37,608 of 200,000 random grids. Each call site
     * therefore writes into its own buffer.
     */
    private final List<Offer<PowerNode.PowerProducer>> unusedBuffer;

    private final List<Offer<PowerNode.PowerProducer>> unusedBufferNested;

    private long lastGenerated = -1L;

    private long lastDelivered = -1L;

    private long lastDemand = -1L;

    private long lastStored = -1L;

    private int diagnosticTicks;

    /**
     * Generation offered on the last tick this grid actually ran. Kept so a machine can ask "is there power here?"
     * without polling every producer again - a solar panel reads the sky and a thermoelectric reads five neighbours.
     */
    private long lastSupply;

    private net.minecraft.world.level.Level diagnosticLevel;

    /** Set by the manager so a log line can name the dimension. Diagnostics only; nothing else reads it. */
    public void setDiagnosticLevel(net.minecraft.world.level.Level level) {
        this.diagnosticLevel = level;
    }

    public PowerGrid() {
        this.consumers = new HashSet<>();
        this.producers = new HashSet<>();
        this.stores = new HashSet<>();
        this.supplyBuffer = new ArrayList<>();
        this.demandBuffer = new ArrayList<>();
        this.unusedBuffer = new ArrayList<>();
        this.unusedBufferNested = new ArrayList<>();
    }

    public void add(PowerNode node) {
        switch (node) {
            case PowerNode.PowerStore powerStore -> stores.add(powerStore);
            case PowerNode.PowerConsumer powerConsumer -> consumers.add(powerConsumer);
            case PowerNode.PowerProducer powerProducer -> producers.add(powerProducer);
        }
    }

    public void remove(PowerNode node) {
        switch (node) {
            case PowerNode.PowerStore powerStore -> stores.remove(powerStore);
            case PowerNode.PowerConsumer powerConsumer -> consumers.remove(powerConsumer);
            case PowerNode.PowerProducer powerProducer -> producers.remove(powerProducer);
        }
    }

    /** True when nothing is wired into this grid, so the manager can drop it. */
    public boolean isEmpty() {
        return consumers.isEmpty() && producers.isEmpty() && stores.isEmpty();
    }

    /** One node and the single number it gave us this tick. */
    private record Offer<T>(
        T node,
        long amount
    ) {}

    public void tick() {
        // ⚠ IDLE FAST PATH. A base with no consumers and no storage has nothing to move, and polling every producer
        // to discover that costs real work - a solar panel reads the sky, a thermoelectric reads five neighbours.
        // An unfinished or abandoned wing of a base should cost nothing at all.
        if (consumers.isEmpty() && stores.isEmpty()) {
            return;
        }

        // STEP 1 - poll once. Everything below works from these snapshots, never from a fresh call.
        var supply = supplyBuffer;

        supply.clear();

        var totalSupply = 0L;

        for (var producer : producers) {
            var available = Math.max(0L, producer.getAvailablePower());

            if (available > 0L) {
                supply.add(new Offer<>(producer, available));
                totalSupply += available;
            }
        }

        var demand = demandBuffer;

        demand.clear();

        var totalDemand = 0L;
        var criticalDemand = 0L;

        for (var consumer : consumers) {
            // [stated] Sep 30: a device bound to a fuse panel is powered through the panel only - never by cable.
            if (isOnFusePanel(consumer)) {
                continue;
            }

            var requested = Math.max(0L, consumer.getRequestedPower());

            if (requested > 0L) {
                demand.add(new Offer<>(consumer, requested));
                totalDemand += requested;

                if (consumer.getPowerPriority() <= PowerNode.PRIORITY_CRITICAL) {
                    criticalDemand += requested;
                }
            }
        }

        lastSupply = totalSupply;

        if (totalDemand == 0L) {
            chargeStores(supply);
            return;
        }

        // STEP 2 and 3 - producers first, then top up from stores.
        var budget = Math.min(totalDemand, totalSupply);
        var fromStores = 0L;

        if (budget < totalDemand) {
            fromStores = drawFromStores(totalDemand - budget, Math.max(0L, criticalDemand - totalSupply));
            budget += fromStores;
        }

        if (budget <= 0L) {
            return;
        }

        // STEP 4 - deliver, and count what was really taken.
        var delivered = distribute(demand, budget, totalDemand);

        // STEP 5 - pay for exactly what was delivered.
        //
        // ⚠⚠ PRODUCERS ARE BILLED FIRST, STORES ONLY FOR THE REMAINDER. Store power was withdrawn up front in step 3
        // on the assumption consumers would take it; a consumer that accepted less than offered leaves that withdrawal
        // unspent, and it MUST go back. Letting it evaporate destroys power; billing producers for it creates power.
        var fromProducers = Math.min(delivered, totalSupply);
        var storePowerUsed = Math.max(0L, delivered - fromProducers);
        var unspentStorePower = fromStores - storePowerUsed;

        var unusedSupply = drawFromProducers(supply, fromProducers, unusedBuffer);

        if (unspentStorePower > 0L) {
            returnToStores(unspentStorePower);
        }

        // Generation nobody wanted tops up storage - and is extracted from the producers as the stores accept it.
        var storedBefore = totalStored();

        chargeStores(unusedSupply);

        // ⚠ Guarded so a disabled log costs one boolean read: nothing below is evaluated when it is off.
        if (PowerDiagnostics.isEnabled()) {
            report(totalSupply, delivered, fromStores, totalStored() - storedBefore);
        }
    }

    private long totalStored() {
        var total = 0L;

        for (var store : stores) {
            total += Math.max(0L, store.getStoredPower());
        }

        return total;
    }

    /**
     * Writes one diagnostic line, but only when something moved or the heartbeat is due.
     * <p>
     * ⚠ A grid that logged every tick would bury the interesting lines under twenty a second.
     * </p>
     */
    private void report(long generated, long delivered, long fromStores, long toStores) {
        var stored = totalStored();
        var demand = 0L;

        for (var consumer : consumers) {
            if (!isOnFusePanel(consumer)) {
                demand += Math.max(0L, consumer.getRequestedPower());
            }
        }

        var moved = generated != lastGenerated
            || delivered != lastDelivered
            || demand != lastDemand
            || stored != lastStored;

        diagnosticTicks++;

        if (!moved && diagnosticTicks % PowerDiagnostics.heartbeatTicks() != 0) {
            return;
        }

        lastGenerated = generated;
        lastDelivered = delivered;
        lastDemand = demand;
        lastStored = stored;

        PowerDiagnostics.logTick(
            System.identityHashCode(this) & 0xFFFF,
            diagnosticDimension(),
            // ⚠ No position: PowerNode exposes none, and adding one to the interface for a debug log is not worth
            // the churn. Dimension plus node counts identify a grid well enough to follow it.
            null,
            producers.size(),
            consumers.size(),
            stores.size(),
            generated,
            demand,
            delivered,
            fromStores,
            toStores,
            stored
        );
    }

    private String diagnosticDimension() {
        return diagnosticLevel == null ? "?" : diagnosticLevel.dimension().location().toString();
    }

    /**
     * Hands out {@code budget} across the consumers: priority bands in order, proportional inside a band.
     *
     * @return the total actually accepted, which may be less than the budget if consumers refuse it
     */
    private static long distribute(
        List<Offer<PowerNode.PowerConsumer>> demand,
        long budget,
        long totalDemand
    ) {
        var delivered = 0L;
        var remaining = budget;

        if (budget >= totalDemand) {
            // Everyone gets what they asked for; no ordering needed.
            for (var entry : demand) {
                delivered += Math.max(0L, entry.node().receivePower(entry.amount()));
            }

            return delivered;
        }

        // Short. Lower priority number is served first.
        //
        // ⚠ The sort is SKIPPED when every consumer shares a priority, which is the overwhelmingly common case - a
        // base of ordinary machines with no life support. Sorting there allocates a copy and does n log n comparisons
        // to produce the order it already had.
        var uniformPriority = true;
        var firstPriority = demand.get(0).node().getPowerPriority();

        for (var entry : demand) {
            if (entry.node().getPowerPriority() != firstPriority) {
                uniformPriority = false;
                break;
            }
        }

        List<Offer<PowerNode.PowerConsumer>> ordered;

        if (uniformPriority) {
            ordered = demand;
        } else {
            ordered = new ArrayList<>(demand);
            ordered.sort(Comparator.comparingInt(entry -> entry.node().getPowerPriority()));
        }

        var index = 0;

        while (index < ordered.size() && remaining > 0L) {
            var priority = ordered.get(index).node().getPowerPriority();
            var bandEnd = index;
            var bandDemand = 0L;

            while (bandEnd < ordered.size() && ordered.get(bandEnd).node().getPowerPriority() == priority) {
                bandDemand += ordered.get(bandEnd).amount();
                bandEnd++;
            }

            var bandBudget = Math.min(remaining, bandDemand);

            for (var entry = index; entry < bandEnd && remaining > 0L; entry++) {
                var offer = ordered.get(entry);
                // Proportional share of this band's budget, never more than remaining.
                var share = bandDemand == 0L
                    ? 0L
                    : Math.min(remaining, Math.floorDiv(offer.amount() * bandBudget, bandDemand));

                if (share <= 0L) {
                    continue;
                }

                var accepted = Math.max(0L, Math.min(share, offer.node().receivePower(share)));

                delivered += accepted;
                remaining -= accepted;
            }

            index = bandEnd;
        }

        return delivered;
    }

    /**
     * Takes exactly {@code owed} from producers, in order.
     *
     * @return what each producer still has on offer and has NOT been extracted for
     */
    private List<Offer<PowerNode.PowerProducer>> drawFromProducers(
        List<Offer<PowerNode.PowerProducer>> supply,
        long owed,
        List<Offer<PowerNode.PowerProducer>> out
    ) {
        var remaining = owed;
        var unused = out;

        unused.clear();

        for (var offer : supply) {
            if (remaining <= 0L) {
                // ⚠ NOT extracted. Untaken generation is simply never produced - extracting it here is what
                // destroyed surplus power in the old code.
                unused.add(offer);
                continue;
            }

            var take = Math.min(remaining, offer.amount());
            var extracted = Math.max(0L, Math.min(take, offer.node().extractPower(take)));

            remaining -= extracted;

            if (offer.amount() > extracted) {
                unused.add(new Offer<>(offer.node(), offer.amount() - extracted));
            }
        }

        return unused;
    }

    /** Puts store power back that was withdrawn for a consumer which then declined it. */
    private void returnToStores(long amount) {
        var remaining = amount;

        for (var store : stores) {
            if (remaining <= 0L) {
                break;
            }

            remaining -= Math.max(0L, store.receivePower(remaining));
        }
    }

    /**
     * Takes what the producers could not cover, primaries before reserves.
     * <p>
     * ⚠ The sort is SKIPPED when every store shares a band, which is the ordinary case - a base with no backup room
     * pays nothing for the feature existing.
     * </p>
     */
    private long drawFromStores(long needed, long criticalShortfall) {
        var remaining = needed;
        var drawn = 0L;

        // PASS 1 - unreserved power only: what any machine may use.
        for (var store : orderedStores()) {
            if (remaining <= 0L) {
                break;
            }

            if (store.getStorePriority() == PowerNode.STORE_NEVER_DISCHARGES) {
                continue;
            }

            var usable = Math.max(0L, store.getStoredPower() - Math.max(0L, store.getReservedPower()));

            if (usable <= 0L) {
                continue;
            }

            var extracted = Math.max(0L, store.extractPower(Math.min(remaining, usable)));

            drawn += extracted;
            remaining -= extracted;
        }

        // PASS 2 - emergency reserves, and only what Critical machines still lack after generators and pass 1.
        // ⚠ distribute() serves priority bands in order, Critical first, so everything pass 2 adds to the budget is
        // consumed by Critical machines before any lower band is reached: reserve power cannot reach a Normal or Low
        // machine. [stated] Sep 29: below the line, only Critical machines like cryopods may draw.
        var reserveAllowance = Math.min(remaining, Math.max(0L, criticalShortfall - drawn));

        for (var store : orderedStores()) {
            if (reserveAllowance <= 0L) {
                break;
            }

            if (store.getStorePriority() == PowerNode.STORE_NEVER_DISCHARGES || store.getReservedPower() <= 0L) {
                continue;
            }

            var extracted = Math.max(0L, store.extractPower(reserveAllowance));

            drawn += extracted;
            reserveAllowance -= extracted;
        }

        return drawn;
    }

    /** Stores in discharge order. Returns the live set untouched when they all share a band. */
    private Iterable<PowerNode.PowerStore> orderedStores() {
        if (stores.size() < 2) {
            return stores;
        }

        var first = Integer.MIN_VALUE;
        var mixed = false;

        for (var store : stores) {
            var priority = store.getStorePriority();

            if (first == Integer.MIN_VALUE) {
                first = priority;
            } else if (priority != first) {
                mixed = true;
                break;
            }
        }

        if (!mixed) {
            return stores;
        }

        var ordered = new ArrayList<>(stores);

        ordered.sort(Comparator.comparingInt(PowerNode.PowerStore::getStorePriority));

        return ordered;
    }

    /**
     * Banks leftover generation.
     * <p>
     * ⚠⚠ THE STORES ARE CHARGED AND THE PRODUCERS ARE BILLED FOR EXACTLY THAT AMOUNT. An earlier version charged stores
     * from a loose surplus figure and then billed an EMPTY producer list, which minted power out of nothing - the
     * precise failure this whole ordering exists to prevent.
     * </p>
     */
    private void chargeStores(List<Offer<PowerNode.PowerProducer>> supply) {
        var surplus = 0L;

        for (var offer : supply) {
            surplus += offer.amount();
        }

        if (surplus <= 0L) {
            return;
        }

        var remaining = surplus;

        // ⚠ SAME ORDER AS DISCHARGE, deliberately: primaries fill first, so a reserve tops up only from what is
        // genuinely spare and never competes with the batteries actually carrying the base.
        for (var store : orderedStores()) {
            if (remaining <= 0L) {
                break;
            }

            remaining -= Math.max(0L, store.receivePower(remaining));
        }

        // Writes to the NESTED buffer: supply here IS unusedBuffer, and clearing it mid-iteration was the bug above.
        drawFromProducers(supply, surplus - remaining, unusedBufferNested);
    }

    public void addAll(Collection<? extends PowerNode> nodes) {
        nodes.forEach(this::add);
    }

    public void removeAll(Collection<? extends PowerNode> nodes) {
        nodes.forEach(this::remove);
    }

    /**
     * Whether this grid could power a machine right now: something generated on the last tick, or a store that is
     * allowed to discharge still holds charge.
     * <p>
     * ⚠ It reads what the last tick already measured and never polls a producer, so it is safe to call from every
     * machine on the grid. It drives the machine's ON light only - actual power delivery is still decided by the tick.
     * </p>
     */
    public boolean hasSupply() {
        if (lastSupply > 0L) {
            return true;
        }

        for (var store : stores) {
            if (store.getStorePriority() != PowerNode.STORE_NEVER_DISCHARGES && store.getStoredPower() > 0L) {
                return true;
            }
        }

        return false;
    }

    /**
     * One reading of the whole grid, for the wiring tool.
     * <p>
     * ⚠ This DOES poll every producer and consumer, so it must only ever be called from a player action, never from a
     * tick. One right-click, one walk.
     * </p>
     */
    public Snapshot snapshot() {
        var generation = 0L;
        var demand = 0L;
        var stored = 0L;
        var capacity = 0L;

        for (var producer : producers) {
            generation += Math.max(0L, producer.getAvailablePower());
        }

        for (var consumer : consumers) {
            if (!isOnFusePanel(consumer)) {
                demand += Math.max(0L, consumer.getRequestedPower());
            }
        }

        for (var store : stores) {
            stored += Math.max(0L, store.getStoredPower());
            capacity += Math.max(0L, store.getCapacity());
        }

        return new Snapshot(producers.size(), consumers.size(), stores.size(), generation, demand, stored, capacity);
    }

    public record Snapshot(
        int producers,
        int consumers,
        int stores,
        long generation,
        long demand,
        long stored,
        long capacity
    ) {}

    public Set<PowerNode.PowerConsumer> getConsumers() {
        return Collections.unmodifiableSet(consumers);
    }

    public Set<PowerNode.PowerProducer> getProducers() {
        return Collections.unmodifiableSet(producers);
    }

    public Set<PowerNode.PowerStore> getStores() {
        return Collections.unmodifiableSet(stores);
    }

    /** A consumer bound to a fuse panel draws through the panel, so every grid it touches by cable leaves it out. */
    private static boolean isOnFusePanel(PowerNode.PowerConsumer consumer) {
        return consumer instanceof com.human.common.gameplay.block.entity.power.PowerConsumerBlockEntity device
            && device.getFusePanel() != null;
    }
}
