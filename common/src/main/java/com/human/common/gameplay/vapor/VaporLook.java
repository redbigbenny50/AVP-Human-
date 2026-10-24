package com.human.common.gameplay.vapor;

/**
 * How each vapor LOOKS ([stated] Oct 18, the per-vapor mixes he approved): which puff style it starts in at the nozzle,
 * which it turns into further out, which its lingering cloud and the distributor's fog use, how it drifts, and whether
 * it glows.
 * <ul>
 * <li>CRYO - wisps at the nozzle, heavy cotton that sinks and pools; a pale icy white like cryofluid, not the tank's
 * blue.</li>
 * <li>INFERNIUM - wisps, then billowing smoke that rises; glows.</li>
 * <li>ACIDIUM - bubbly droplets, soft clumps, a dripping cloud of droplets.</li>
 * <li>NETHER ACIDIUM - droplets turning to rising smoke as it burns.</li>
 * <li>IRRADIUM - swirling curls all the way through, hanging in place; glows.</li>
 * </ul>
 *
 * @param near  the style within the first stretch of the blast
 * @param far   the style for the rest of the blast
 * @param cloud the lingering cloud's and the distributor fog's style
 * @param drift vertical drift, blocks a tick (negative sinks)
 * @param glow  drawn at full brightness, so it shows in the dark
 * @param mist  the puffs' colour (0xRRGGBB), or {@link #TANK_COLOUR} for the tank's own colour
 */
public record VaporLook(
    VaporStyle near,
    VaporStyle far,
    VaporStyle cloud,
    float drift,
    boolean glow,
    int mist
) {

    /** Use the vapor's tank colour ({@link VaporKind#colour}). */
    public static final int TANK_COLOUR = -1;

    /**
     * [stated] Oct 18 - "the cryo mist should be a more whiteish color similar to the cryofluid itself ... its
     * particles should reflect the fluid more than the tank color". Cryofluid's own light blue (#94C8FA, the brightest
     * shade of cryofluid_still) taken about a third of the way to white: a pale icy mist. [stated] then "a bit more
     * blue to the cryo just a bit more" - nudged from #CAE4FD.
     */
    public static final int CRYO_MIST = 0xB8D9FC;

    /** The colour this vapor's puffs are drawn in. */
    public int colour(VaporKind kind) {
        return mist == TANK_COLOUR ? kind.colour() : mist;
    }

    private static final VaporLook CRYO = new VaporLook(VaporStyle.WISP, VaporStyle.COTTON, VaporStyle.COTTON, -0.006F, false, CRYO_MIST);

    private static final VaporLook INFERNIUM = new VaporLook(
        VaporStyle.WISP,
        VaporStyle.BILLOW,
        VaporStyle.BILLOW,
        0.010F,
        true,
        TANK_COLOUR
    );

    private static final VaporLook ACIDIUM = new VaporLook(
        VaporStyle.GLOBULE,
        VaporStyle.CLUMP,
        VaporStyle.GLOBULE,
        -0.003F,
        false,
        TANK_COLOUR
    );

    private static final VaporLook NETHER_ACIDIUM = new VaporLook(
        VaporStyle.GLOBULE,
        VaporStyle.BILLOW,
        VaporStyle.BILLOW,
        0.006F,
        false,
        TANK_COLOUR
    );

    private static final VaporLook IRRADIUM = new VaporLook(VaporStyle.CURL, VaporStyle.CURL, VaporStyle.CURL, 0.001F, true, TANK_COLOUR);

    /** How far from the nozzle the blast is all "near" style; then half-and-half to {@link #MIX_END}. */
    public static final double NEAR_END = 2.5;

    public static final double MIX_END = 4.5;

    public static VaporLook of(VaporKind kind) {
        return switch (kind) {
            case CRYO -> CRYO;
            case INFERNIUM -> INFERNIUM;
            case ACIDIUM -> ACIDIUM;
            case NETHER_ACIDIUM -> NETHER_ACIDIUM;
            case IRRADIUM -> IRRADIUM;
        };
    }

    /** The blast's style at {@code along} blocks from the nozzle ({@code roll} 0-1 picks in the mixing stretch). */
    public VaporStyle blastStyle(double along, float roll) {
        if (along < NEAR_END) {
            return near;
        }

        return along < MIX_END && roll < 0.5F ? near : far;
    }
}
