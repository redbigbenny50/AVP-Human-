package com.human.common.gameplay.distributor;

/**
 * The fog's SHAPE: how strong it is at each block of the 33 x 33 x 33 area, before walls are considered.
 * <h2>[stated] Oct 17 - THE RULINGS THIS IMPLEMENTS</h2>
 * <ul>
 * <li>The area is a 33 x 33 x 33 cube CENTRED on the machine - 16 blocks every way.</li>
 * <li>Full strength in the middle, FADING OVER THE OUTER 6 BLOCKS on all six sides.</li>
 * <li>Corners ROUNDED with a 10-block radius - the fade runs against a rounded box, so the cloud reads as a rounded
 * cloud, not a cube.</li>
 * <li>NO fade against walls - the room scan ({@link DistributorRoom}) cuts the fog off at walls; inside the room it
 * keeps its full strength right up to them.</li>
 * <li>Thickest visibility 45 % - see {@link #MAX_OPACITY}.</li>
 * </ul>
 * <h2>HOW</h2> A rounded box's signed distance (negative inside): the box's half-size is 16.5 (to the outer FACES of
 * the outermost blocks), its corners are spheres of radius 10, and a block's depth inside it is measured from the
 * block's centre. Strength = depth / 6, clamped to 0-1. So the centre plane of a face starts at ~8 % in its outermost
 * block and reaches full 6 blocks in; along a diagonal the corner sphere pulls the cloud in.
 * <p>
 * ⚡ Worked out ONCE into a table of 35,937 floats (140 KB) the first time it is asked - every call after is an array
 * read. The particles read {@link #strength}.
 * </p>
 */
public final class DistributorFog {

    /** [stated] 16 blocks every way. */
    public static final int RADIUS = 16;

    public static final int SIZE = RADIUS * 2 + 1;

    public static final int CELLS = SIZE * SIZE * SIZE;

    /** To the outer faces of the outermost blocks. */
    private static final double HALF_EXTENT = RADIUS + 0.5;

    /** [stated] corners rounded with a 10-block radius. */
    private static final double CORNER_RADIUS = 10.0;

    /** [stated] fading over the outer 6 blocks. */
    private static final double FADE = 6.0;

    /**
     * [stated] "fog at its thickest 45 % opaque" - looking through the WHOLE cloud at full strength (33 blocks).
     */
    public static final float MAX_OPACITY = 0.45F;

    /**
     * The opacity ONE block of full-strength fog adds, so that 33 of them stack to {@link #MAX_OPACITY}:
     * {@code 1 - 0.55^(1/33)} ≈ 0.0180. Over 8 blocks that is ≈ 13.5 % - [stated] "clear within ~8 blocks". The real
     * fog renderer (later) should use this; the placeholder particles only approximate it.
     */
    public static final float OPACITY_PER_BLOCK = (float) (1.0 - Math.pow(1.0 - MAX_OPACITY, 1.0 / SIZE));

    private static float[] table;

    private DistributorFog() {}

    /** The flat index of an offset from the machine, each axis -16..16. */
    public static int index(int dx, int dy, int dz) {
        return ((dx + RADIUS) * SIZE + (dy + RADIUS)) * SIZE + (dz + RADIUS);
    }

    public static boolean inArea(int dx, int dy, int dz) {
        return dx >= -RADIUS && dx <= RADIUS && dy >= -RADIUS && dy <= RADIUS && dz >= -RADIUS && dz <= RADIUS;
    }

    /** 0-1 strength at an offset from the machine; 0 outside the area. Walls are NOT considered here. */
    public static float strength(int dx, int dy, int dz) {
        if (!inArea(dx, dy, dz)) {
            return 0.0F;
        }

        return table()[index(dx, dy, dz)];
    }

    /** Same, by flat index. */
    public static float strength(int index) {
        return index < 0 || index >= CELLS ? 0.0F : table()[index];
    }

    /** How much one block of fog at this strength dims the view - linear in strength. */
    public static float opacity(float strength) {
        return OPACITY_PER_BLOCK * Math.max(0.0F, Math.min(1.0F, strength));
    }

    private static float[] table() {
        var built = table;

        if (built == null) {
            built = new float[CELLS];

            for (var dx = -RADIUS; dx <= RADIUS; dx++) {
                for (var dy = -RADIUS; dy <= RADIUS; dy++) {
                    for (var dz = -RADIUS; dz <= RADIUS; dz++) {
                        built[index(dx, dy, dz)] = compute(dx, dy, dz);
                    }
                }
            }

            // A benign race at worst: two threads build identical tables and one wins.
            table = built;
        }

        return built;
    }

    /** Depth inside the rounded box over the fade length. */
    static float compute(int dx, int dy, int dz) {
        var inner = HALF_EXTENT - CORNER_RADIUS;
        var qx = Math.abs(dx) - inner;
        var qy = Math.abs(dy) - inner;
        var qz = Math.abs(dz) - inner;
        var ox = Math.max(qx, 0.0);
        var oy = Math.max(qy, 0.0);
        var oz = Math.max(qz, 0.0);
        var outside = Math.sqrt(ox * ox + oy * oy + oz * oz);
        var inside = Math.min(Math.max(qx, Math.max(qy, qz)), 0.0);
        var depth = -(outside + inside - CORNER_RADIUS);

        return (float) Math.max(0.0, Math.min(1.0, depth / FADE));
    }
}
