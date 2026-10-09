package com.human.common.gameplay.power;

public sealed interface PowerNode {

    non-sealed interface PowerConsumer extends PowerNode {

        /**
         * Returns how much power this consumer wants to receive this tick.
         */
        long getRequestedPower();

        /**
         * Called by the power grid to offer power to this consumer.
         *
         * @param maxAmount the maximum amount offered
         * @return the amount actually accepted
         */
        long receivePower(long maxAmount);

        /**
         * Serving order when the grid cannot meet full demand: LOWER IS SERVED FIRST. Consumers sharing a priority
         * split what is left proportionally, so identical machines brown out together instead of one running flat out
         * and the next getting nothing by iteration order.
         * <p>
         * ⚠ Anything whose failure costs the player something that cannot be got back belongs ABOVE
         * {@value #PRIORITY_DEFAULT} - a cryotube losing its occupant, for instance. Convenience machines stay at the
         * default, and anything that can simply wait belongs below it.
         * </p>
         */
        default int getPowerPriority() {
            return PRIORITY_DEFAULT;
        }
    }

    /** Life support and anything else whose failure is irreversible. */
    int PRIORITY_CRITICAL = 0;

    int PRIORITY_DEFAULT = 100;

    /** Work that can wait for a better tick: furnaces, fabricators, chargers. */
    int PRIORITY_DEFERRABLE = 200;

    non-sealed interface PowerProducer extends PowerNode {

        /**
         * Returns how much power this producer is willing to provide this tick.
         */
        long getAvailablePower();

        /**
         * Called by the power grid to extract power from this producer.
         *
         * @param maxAmount the maximum amount to extract
         * @return the actual amount extracted
         */
        long extractPower(long maxAmount);
    }

    non-sealed interface PowerStore extends PowerNode, PowerConsumer, PowerProducer {

        long getStoredPower();

        /**
         * The most this store can hold. Read by the wiring tool's grid readout and nothing else - the grid itself never
         * needs it, because a store already caps what it asks for in {@link PowerConsumer#getRequestedPower()}.
         */
        default long getCapacity() {
            return 0L;
        }

        /**
         * Discharge order when the grid is short: LOWER GOES FIRST, and {@link #STORE_NEVER_DISCHARGES} never gives
         * power up at all.
         * <p>
         * ⭐ This is what makes a backup battery room work with NO detector block and NO new machine. A reserve store
         * simply sits in a lower band, so it contributes nothing while any primary still has charge, and takes over the
         * moment they run dry - or the moment a severed cable splits it onto its own grid where it is the only source.
         * The DEFICIT is the trigger, resolved in the same tick, instead of a state machine that has to sense a
         * condition and can be wrong about it.
         * </p>
         */
        default int getStorePriority() {
            return STORE_PRIMARY;
        }

        /**
         * How much of the stored power is held back for CRITICAL machines only - the emergency reserve module. The grid
         * never draws this part for anything else: it is only drawn to cover what Critical machines still lack once
         * generators and every store's unreserved power have been used.
         */
        default long getReservedPower() {
            return 0L;
        }
    }

    /** Discharges whenever the grid is short. */
    int STORE_PRIMARY = 0;

    /** Holds its charge until every primary is empty or gone. */
    int STORE_RESERVE = 100;

    /** ⚠ A pure sink: charges, never discharges. Compared with == , so it must not collide with a real band. */
    int STORE_NEVER_DISCHARGES = Integer.MAX_VALUE;
}
