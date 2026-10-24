package com.human.common.gameplay.item.gun.chem;

import com.human.common.gameplay.distributor.DistributorRoom;
import com.human.common.gameplay.vapor.VaporShape;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * The chem thrower's blast - [stated] Oct 18: "a directed blast of vapor about 8 blocks ahead of the player ... it will
 * expand into a cone from the nozzle so it would go from 1 block directly in front of the gun to a 3x3 area at the end
 * of the cone with the same kind of fading at the edges the distributor cube has. its more concentrated so it would
 * have a 80% opacity".
 * <h2>THE SHAPE</h2> Along the aim, from the nozzle out to {@value #LENGTH} blocks. Each slice across it is a rounded
 * square ({@link VaporShape#square}, the distributor's proportions) whose width grows straight from 1 block at the
 * nozzle to 3 at the end. The last block fades out lengthways, so the tip softens into the lingering cloud
 * ({@code ChemCloud}) rather than ending in a flat wall.
 * <h2>WALLS</h2> Like the distributor's fog, the blast does not pass through blocks: it is cut short at the first block
 * on the aim line that the fog would treat as a wall (full blocks, closed doors and trapdoors). The cloud then forms in
 * front of that wall.
 */
public final class ChemCone {

    /** [stated] about 8 blocks. */
    public static final double LENGTH = 8.0;

    /** [stated] 1 block wide at the nozzle... */
    public static final double NEAR_HALF = 0.5;

    /** ...3 x 3 at the end. */
    public static final double FAR_HALF = 1.5;

    /** [stated] Oct 18 - FLAME: "increasing the area from 3x3 to 5x5 ... stronger flame = wider area, stronger fog". */
    public static final double FLAME_FAR_HALF = 2.5;

    /** The tip softens over this last stretch. */
    private static final double TIP_FADE = 1.0;

    /**
     * [stated] 80 % at its thickest - looking down the whole 8-block blast. One block of full-strength blast adds
     * {@code 1 - 0.2^(1/8)} ≈ 18 %. The distributor's fog, for comparison, adds about 1.8 % a block (45 % over 33).
     */
    public static final float MAX_OPACITY = 0.80F;

    public static final float OPACITY_PER_BLOCK = (float) (1.0 - Math.pow(1.0 - MAX_OPACITY, 1.0 / LENGTH));

    private static final double WALL_STEP = 0.25;

    private ChemCone() {}

    /** The cross-section's half-width at this distance from the nozzle, for a cone ending {@code farHalf} wide. */
    public static double halfAt(double distance, double farHalf) {
        var t = Math.max(0.0, Math.min(1.0, distance / LENGTH));
        return NEAR_HALF + (farHalf - NEAR_HALF) * t;
    }

    /** [stated] Oct 18 - POWER: a stronger vapor, 25 % more of everything it does per level. */
    public static final float POWER_PER_LEVEL = 0.25F;

    /**
     * [stated] Oct 18 - PUNCH: the blast pushes what it catches away along the cone - this much speed a tick, a level.
     */
    public static final double PUNCH_PUSH_PER_LEVEL = 0.06;

    public static float potency(net.minecraft.world.level.Level level, net.minecraft.world.item.ItemStack gun) {
        return 1.0F + POWER_PER_LEVEL * Math.max(0, enchantment(level, gun, net.minecraft.world.item.enchantment.Enchantments.POWER));
    }

    public static int punch(net.minecraft.world.level.Level level, net.minecraft.world.item.ItemStack gun) {
        return Math.max(0, enchantment(level, gun, net.minecraft.world.item.enchantment.Enchantments.PUNCH));
    }

    private static int enchantment(
        net.minecraft.world.level.Level level,
        net.minecraft.world.item.ItemStack gun,
        net.minecraft.resources.ResourceKey<net.minecraft.world.item.enchantment.Enchantment> key
    ) {
        return com.blib.api.common.enchantment.v1.EnchantmentUtil.getLevel(level, gun, key);
    }

    /** The end's half-width for this gun: 1.5 (3 x 3), or 2.5 (5 x 5) with FLAME. */
    public static double farHalf(net.minecraft.world.level.Level level, net.minecraft.world.item.ItemStack gun) {
        return com.blib.api.common.enchantment.v1.EnchantmentUtil.getLevel(
            level,
            gun,
            net.minecraft.world.item.enchantment.Enchantments.FLAME
        ) > 0 ? FLAME_FAR_HALF : FAR_HALF;
    }

    /**
     * How far the blast reaches before a wall: up to {@link #LENGTH}. ⚠ Never loads a chunk - an unloaded block is a
     * wall.
     */
    public static double reach(Level level, Vec3 nozzle, Vec3 direction) {
        var cursor = new BlockPos.MutableBlockPos();

        for (var distance = WALL_STEP; distance <= LENGTH; distance += WALL_STEP) {
            cursor.set(nozzle.x + direction.x * distance, nozzle.y + direction.y * distance, nozzle.z + direction.z * distance);

            if (!level.isLoaded(cursor) || !DistributorRoom.isOpen(level, cursor, level.getBlockState(cursor))) {
                return Math.max(0.0, distance - WALL_STEP);
            }
        }

        return LENGTH;
    }

    /**
     * 0-1 strength at a point, for a blast from {@code nozzle} along the unit vector {@code direction} reaching
     * {@code reach} blocks. {@code margin} widens the test by half a body, so a mob is caught when the blast touches
     * it, not only when it covers its centre.
     */
    public static float strengthAt(Vec3 point, Vec3 nozzle, Vec3 direction, double reach, double farHalf, double margin) {
        var offset = point.subtract(nozzle);
        var along = offset.dot(direction);

        if (along < 0.0 || along > reach) {
            return 0.0F;
        }

        // Two directions across the aim, for the square slice.
        var side = direction.cross(new Vec3(0.0, 1.0, 0.0));

        if (side.lengthSqr() < 1.0E-6) {
            side = new Vec3(1.0, 0.0, 0.0);
        }

        side = side.normalize();

        var up = side.cross(direction).normalize();
        var across = offset.subtract(direction.scale(along));
        var du = Math.max(0.0, Math.abs(across.dot(side)) - margin);
        var dv = Math.max(0.0, Math.abs(across.dot(up)) - margin);
        var strength = VaporShape.square(du, dv, halfAt(along, farHalf));
        var tip = (LENGTH - along) / TIP_FADE;

        return (float) (strength * Math.max(0.0, Math.min(1.0, tip)));
    }

    /** A random point inside the cone's box (not weighted - pair it with {@link #strengthAt}). */
    public static Vec3 randomPoint(
        Vec3 nozzle,
        Vec3 direction,
        double reach,
        double farHalf,
        net.minecraft.util.RandomSource random
    ) {
        var side = direction.cross(new Vec3(0.0, 1.0, 0.0));

        if (side.lengthSqr() < 1.0E-6) {
            side = new Vec3(1.0, 0.0, 0.0);
        }

        side = side.normalize();

        var up = side.cross(direction).normalize();
        var along = random.nextDouble() * reach;
        var half = halfAt(along, farHalf);
        var u = (random.nextDouble() * 2.0 - 1.0) * half;
        var v = (random.nextDouble() * 2.0 - 1.0) * half;

        return nozzle.add(direction.scale(along)).add(side.scale(u)).add(up.scale(v));
    }

    /** Where the gun's nozzle is, near enough, for a shooter: in front of the eyes, a little right and down. */
    public static Vec3 nozzle(LivingEntity shooter) {
        var look = shooter.getLookAngle();
        var yaw = Math.toRadians(shooter.getYRot());
        var right = new Vec3(-Math.cos(yaw), 0.0D, -Math.sin(yaw));

        return shooter.getEyePosition().add(look.scale(0.8D)).add(right.scale(0.2D)).add(0.0D, -0.25D, 0.0D);
    }
}
