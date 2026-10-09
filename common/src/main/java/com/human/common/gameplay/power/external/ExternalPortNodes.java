package com.human.common.gameplay.power.external;

import com.human.common.gameplay.power.PowerNode;
import org.jetbrains.annotations.Nullable;

/**
 * The stand-ins our grid uses for another mod's block on a cable face. The grid only knows its own node kinds, so each
 * external face becomes one of them:
 * <ul>
 * <li>{@link Consumer} (OUT) - a machine our grid powers.</li>
 * <li>{@link Producer} (IN) - a generator feeding our grid.</li>
 * <li>{@link Store} (BOTH) - a battery: the grid charges it from surplus and draws on it when short, exactly as it
 * treats our own batteries. Why a two-way block is a STORE and not a consumer plus a producer: as both, the grid would
 * pour power into an energy cube and pull it straight back out every tick.</li>
 * </ul>
 * 1 of our units = 1 FE. Each face moves at most {@value #MAX_PER_TICK} a tick either way.
 */
public final class ExternalPortNodes {

    public static final long MAX_PER_TICK = 1_000_000L;

    private ExternalPortNodes() {}

    /** The node for this face in this mode, or null when the face connects nothing. */
    public static @Nullable PowerNode create(ExternalEnergy.Port port, PortMode mode) {
        if (!port.present() || mode == PortMode.OFF) {
            return null;
        }

        return switch (mode) {
            case OUT -> port.canReceive() ? new Consumer(port) : null;
            case IN -> port.canExtract() ? new Producer(port) : null;
            case BOTH -> port.canReceive() && port.canExtract() ? new Store(port) : null;
            default -> port.canReceive() && port.canExtract()
                ? new Store(port)
                : port.canReceive() ? new Consumer(port) : port.canExtract() ? new Producer(port) : null;
        };
    }

    /** Our grid powers their block. It may accept less than offered - the grid takes back the rest. */
    public static final class Consumer implements PowerNode.PowerConsumer {

        private final ExternalEnergy.Port port;

        Consumer(ExternalEnergy.Port port) {
            this.port = port;
        }

        @Override
        public long getRequestedPower() {
            return port.present() ? Math.max(0L, port.receive(MAX_PER_TICK, true)) : 0L;
        }

        @Override
        public long receivePower(long maxAmount) {
            return port.present() ? Math.max(0L, port.receive(Math.min(maxAmount, MAX_PER_TICK), false)) : 0L;
        }
    }

    /** Their block feeds our grid. */
    public static final class Producer implements PowerNode.PowerProducer {

        private final ExternalEnergy.Port port;

        Producer(ExternalEnergy.Port port) {
            this.port = port;
        }

        @Override
        public long getAvailablePower() {
            return port.present() ? Math.max(0L, port.extract(MAX_PER_TICK, true)) : 0L;
        }

        @Override
        public long extractPower(long maxAmount) {
            return port.present() ? Math.max(0L, port.extract(Math.min(maxAmount, MAX_PER_TICK), false)) : 0L;
        }
    }

    /** Their battery, used as one of ours. */
    public static final class Store implements PowerNode.PowerStore {

        private final ExternalEnergy.Port port;

        Store(ExternalEnergy.Port port) {
            this.port = port;
        }

        @Override
        public long getStoredPower() {
            return port.present() ? port.stored() : 0L;
        }

        @Override
        public long getCapacity() {
            return port.present() ? port.capacity() : 0L;
        }

        @Override
        public long getRequestedPower() {
            return port.present() ? Math.max(0L, port.receive(MAX_PER_TICK, true)) : 0L;
        }

        @Override
        public long receivePower(long maxAmount) {
            return port.present() ? Math.max(0L, port.receive(Math.min(maxAmount, MAX_PER_TICK), false)) : 0L;
        }

        @Override
        public long getAvailablePower() {
            return port.present() ? Math.max(0L, port.extract(MAX_PER_TICK, true)) : 0L;
        }

        @Override
        public long extractPower(long maxAmount) {
            return port.present() ? Math.max(0L, port.extract(Math.min(maxAmount, MAX_PER_TICK), false)) : 0L;
        }
    }
}
