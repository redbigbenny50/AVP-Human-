package com.human.common.gameplay.block.tank;

/**
 * The tank's limits. [stated] Sep 28/29 design review: square footprint 1 x 1 to 5 x 5 (width = depth, like Create), 1
 * to 32 tall, 64 buckets per block - so the biggest tank, 5 x 5 x 32, holds 51,200 buckets.
 * <p>
 * The design asked for max height and buckets-per-block as SERVER CONFIG. avp_human has no config system yet, and
 * vanilla's game-rule registration is private (it would need access wideners on both loaders), so for now they live
 * here, in one place, ready to move into a config when the mod gets one.
 * </p>
 * <h2>WHY THESE NUMBERS ARE SAFE</h2> 51,200 buckets = 51.2 million mB, about 40x under an int's ceiling, so NeoForge's
 * int millibuckets hold it. Fabric's droplets (81 per mB) would overflow an int, but the Fabric bridge converts in
 * long.
 */
public final class FluidTankRules {

    public static final int MAX_WIDTH = 5;

    public static final int MAX_HEIGHT = 32;

    public static final int BUCKETS_PER_BLOCK = 64;

    public static final int MB_PER_BLOCK = BUCKETS_PER_BLOCK * 1000;

    /**
     * A re-form only looks at blocks that could share a tank with the block that changed: within {@code MAX_WIDTH - 1}
     * sideways and {@code MAX_HEIGHT - 1} up or down, plus the whole of any existing tank reaching into that box. So
     * the work per place or break is bounded by the tank size limits, never by the size of a tank farm.
     */
    public static final int REFORM_REACH_XZ = MAX_WIDTH - 1;

    public static final int REFORM_REACH_Y = MAX_HEIGHT - 1;

    private FluidTankRules() {}
}
