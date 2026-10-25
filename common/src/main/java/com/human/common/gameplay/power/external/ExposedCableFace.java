package com.human.common.gameplay.power.external;

import com.human.common.gameplay.power.PowerNode;

/**
 * A cable face that OFFERS energy to another mod's block, for the blocks our cable cannot reach into itself.
 * <p>
 * [stated] Oct 24 - "do our machines work with flux pipes i think they should?" Our cables already reach OUT: a face
 * touching another mod's block that offers energy becomes a {@link ExternalPortNodes} stand-in and the grid pushes into
 * it or pulls from it. That misses every block that only works the other way round - one that PUSHES into its
 * neighbours or PULLS from them, and so only connects to a block that itself offers energy: Flux Networks' plugs and
 * points, Pipez energy pipes, many generators. Those saw nothing on our cable and never connected.
 * </p>
 * <h2>The rule</h2> A face gets one of these only where it touches another mod's block entity that our cable could NOT
 * reach into (no stand-in node was made for it), and the face is not OFF. So a block that offers energy is still
 * handled exactly as before, and nothing is ever connected twice - which is what would let power loop between the two
 * systems.
 * <ul>
 * <li>IN (green): it may be pushed INTO - power enters our grid.</li>
 * <li>OUT (red): it may be pulled FROM - power leaves our grid.</li>
 * <li>AUTO and BOTH: either way, whatever the other block does.</li>
 * </ul>
 * <h2>How it joins the grid</h2> Two small buffers and two ordinary grid nodes. What is pushed in waits in the INBOUND
 * buffer and the grid takes it next tick as a producer. The OUTBOUND buffer is filled by the grid as a consumer - at
 * the lowest priority, so our own machines are always served first - and the other block pulls from it. It is kept only
 * about twice what was pulled recently (at least {@value #MIN_OUTBOUND}, at most
 * {@link ExternalPortNodes#MAX_PER_TICK}), so idle faces hold almost nothing. 1 of our units = 1 FE, both ways, with no
 * loss.
 * <p>
 * ⚠ Whatever sits in the two buffers when the face goes away (the cable is broken, the block removed) goes with it - at
 * most about one tick of flow.
 * </p>
 */
public final class ExposedCableFace {

    /** The smallest outbound buffer, so a puller that starts from nothing finds something on its first try. */
    public static final long MIN_OUTBOUND = 1_000L;

    private final boolean receives;

    private final boolean gives;

    private long inbound;

    private long outbound;

    /** Pulled since the grid last filled the outbound buffer. */
    private long pulledSinceFill;

    /** A slowly falling high-water mark of how much is pulled per tick - what the outbound buffer is sized from. */
    private long recentDemand;

    private final Inbound inboundNode = new Inbound();

    private final Outbound outboundNode = new Outbound();

    private ExposedCableFace(boolean receives, boolean gives) {
        this.receives = receives;
        this.gives = gives;
    }

    /** The face for this mode, or null when the face offers nothing (OFF). */
    public static ExposedCableFace forMode(PortMode mode) {
        return switch (mode) {
            case OFF -> null;
            case IN -> new ExposedCableFace(true, false);
            case OUT -> new ExposedCableFace(false, true);
            default -> new ExposedCableFace(true, true);
        };
    }

    public PowerNode.PowerProducer inboundNode() {
        return inboundNode;
    }

    public PowerNode.PowerConsumer outboundNode() {
        return outboundNode;
    }

    public boolean canReceive() {
        return receives;
    }

    public boolean canExtract() {
        return gives;
    }

    /** Another mod pushes {@code amount} in; returns how much was taken. */
    public long receive(long amount, boolean simulate) {
        if (!receives || amount <= 0L) {
            return 0L;
        }
        var taken = Math.min(amount, ExternalPortNodes.MAX_PER_TICK - inbound);
        if (taken <= 0L) {
            return 0L;
        }
        if (!simulate) {
            inbound += taken;
        }
        return taken;
    }

    /** Another mod pulls up to {@code amount} out; returns how much it got. */
    public long extract(long amount, boolean simulate) {
        if (!gives || amount <= 0L) {
            return 0L;
        }
        var given = Math.min(amount, outbound);
        if (given <= 0L) {
            return 0L;
        }
        if (!simulate) {
            outbound -= given;
            pulledSinceFill += given;
        }
        return given;
    }

    /** What a pulling block sees as "stored" on this face. */
    public long stored() {
        return outbound;
    }

    public long capacity() {
        return ExternalPortNodes.MAX_PER_TICK;
    }

    /** For Fabric's transactions: everything an aborted transfer must put back. */
    public long[] snapshot() {
        return new long[] { inbound, outbound, pulledSinceFill };
    }

    public void restore(long[] snapshot) {
        inbound = snapshot[0];
        outbound = snapshot[1];
        pulledSinceFill = snapshot[2];
    }

    /** Power pushed in by another mod, handed to our grid. */
    private final class Inbound implements PowerNode.PowerProducer {

        @Override
        public long getAvailablePower() {
            return inbound;
        }

        @Override
        public long extractPower(long maxAmount) {
            var taken = Math.min(maxAmount, inbound);
            inbound -= Math.max(0L, taken);
            return Math.max(0L, taken);
        }
    }

    /** Our grid fills what another mod pulls - after every machine of ours has been served. */
    private final class Outbound implements PowerNode.PowerConsumer {

        @Override
        public long getRequestedPower() {
            if (!gives) {
                return 0L;
            }
            var target = Math.max(MIN_OUTBOUND, Math.min(ExternalPortNodes.MAX_PER_TICK, Math.max(recentDemand, pulledSinceFill) * 2L));
            return Math.max(0L, target - outbound);
        }

        @Override
        public long receivePower(long maxAmount) {
            // Once per tick that the grid fills us: fold this tick's pulls into the slowly falling demand mark.
            recentDemand = Math.max(pulledSinceFill, recentDemand - recentDemand / 20L);
            pulledSinceFill = 0L;

            var accepted = Math.max(0L, Math.min(maxAmount, getRequestedPower()));
            outbound += accepted;
            return accepted;
        }

        @Override
        public int getPowerPriority() {
            return PowerNode.PRIORITY_DEFERRABLE;
        }
    }
}
