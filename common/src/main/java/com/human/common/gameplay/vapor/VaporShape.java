package com.human.common.gameplay.vapor;

/**
 * The soft-edged shapes vapor takes. [stated] Oct 17/18: the distributor's cube fades over its outer blocks with
 * rounded corners, and the chem thrower's cone has "the same kind of fading at the edges". Both are a ROUNDED BOX
 * fading inward from its surface; the proportions here are the distributor's, so a small cloud looks like a scaled-down
 * distributor cloud: corners rounded at 10/16.5 of the half-size, a fade over 6/16.5 of it.
 */
public final class VaporShape {

    /** The distributor's corner radius over its half-size. */
    public static final double CORNER = 10.0 / 16.5;

    /** The distributor's fade length over its half-size. */
    public static final double FADE = 6.0 / 16.5;

    private VaporShape() {}

    /** 0-1 strength at an offset from the centre of a rounded CUBE of this half-size (3D: a cloud). */
    public static float box(double dx, double dy, double dz, double half) {
        var corner = half * CORNER;
        var inner = half - corner;
        var qx = Math.abs(dx) - inner;
        var qy = Math.abs(dy) - inner;
        var qz = Math.abs(dz) - inner;
        var ox = Math.max(qx, 0.0);
        var oy = Math.max(qy, 0.0);
        var oz = Math.max(qz, 0.0);
        var depth = -(Math.sqrt(ox * ox + oy * oy + oz * oz) + Math.min(Math.max(qx, Math.max(qy, qz)), 0.0) - corner);

        return clamp(depth / (half * FADE));
    }

    /** 0-1 strength at an offset from the centre of a rounded SQUARE of this half-size (2D: a slice of the cone). */
    public static float square(double du, double dv, double half) {
        var corner = half * CORNER;
        var inner = half - corner;
        var qu = Math.abs(du) - inner;
        var qv = Math.abs(dv) - inner;
        var ou = Math.max(qu, 0.0);
        var ov = Math.max(qv, 0.0);
        var depth = -(Math.sqrt(ou * ou + ov * ov) + Math.min(Math.max(qu, qv), 0.0) - corner);

        return clamp(depth / (half * FADE));
    }

    private static float clamp(double value) {
        return (float) Math.max(0.0, Math.min(1.0, value));
    }
}
