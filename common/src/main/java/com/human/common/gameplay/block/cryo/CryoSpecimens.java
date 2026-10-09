package com.human.common.gameplay.block.cryo;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Freezing a mob into a tank and getting it back out.
 * <h2>Fitting</h2> [stated] Oct 1: the mob MUST FIT the tank - big mobs need big tanks - and it may be shrunk by up to
 * 15% to squeeze in. The tank's blocks are the volume itself, so a mob fits when its width fits the tank's narrower
 * side and its height fits the tank's height.
 * <h2>Release</h2> The saved entity is loaded back exactly as the game loads one from disk, so it returns with
 * everything it went in with. ⚠ The stored copy is cleared by the CALLER only after this reports success: a release
 * that fails - a full chunk, a UUID clash - leaves the specimen safely in the tank.
 */
public final class CryoSpecimens {

    /** [stated] Oct 1: "you can shrink the mob by 15% to try and fit it". */
    public static final float SQUEEZE_SCALE = 0.85F;

    private static final double BREAKOUT_TARGET_RANGE = 16.0;

    private CryoSpecimens() {}

    /** @return 1 if it fits as it is, {@value #SQUEEZE_SCALE} if only squeezed, or 0 if it does not fit at all */
    public static float fitScale(CryotankBlockEntity tank, float width, float height) {
        var across = Math.min(tank.sizeX(), tank.sizeZ());

        if (width <= across && height <= tank.sizeY()) {
            return 1.0F;
        }

        if (width * SQUEEZE_SCALE <= across && height * SQUEEZE_SCALE <= tank.sizeY()) {
            return SQUEEZE_SCALE;
        }

        return 0.0F;
    }

    /**
     * Saves the mob into the tank and removes it from the world. The caller has already checked it fits.
     *
     * @return false if the mob could not be saved (it is riding something the game will not save it apart from)
     */
    public static boolean freeze(CryotankBlockEntity tank, Mob mob, float scale) {
        mob.ejectPassengers();
        mob.stopRiding();

        var tag = new CompoundTag();

        if (!mob.saveAsPassenger(tag)) {
            return false;
        }

        // The leash is not part of the frozen mob. The holder keeps the lead or cord - see the caller.
        tag.remove("leash");

        // Nothing it was doing at the moment of capture should still be happening when it comes out: no momentum to
        // fling it across the room, no fall damage banked, and not on fire (a burning specimen would also be drawn
        // in flames inside the tank forever).
        var stillness = new net.minecraft.nbt.ListTag();

        for (int axis = 0; axis < 3; axis++) {
            stillness.add(net.minecraft.nbt.DoubleTag.valueOf(0.0));
        }

        tag.put("Motion", stillness);
        tag.putFloat("FallDistance", 0.0F);
        tag.putShort("Fire", (short) -1);

        // The data sheet is read from the LIVE mob, before it is gone.
        var card = SpecimenCard.of(mob);

        tank.setSpecimen(tag, scale, mob.getBbWidth(), mob.getBbHeight());
        tank.setCard(card);
        mob.discard();
        return true;
    }

    /**
     * Spawns a tank's specimen OUTSIDE the tank. ⚠ The tank's blocks are solid: spawning at the tank's own position put
     * the mob inside them, where it suffocated. Looks for room on top of the tank first, then around its base, then a
     * little further out; {@code preferred} (a just-broken block) is tried before all of them.
     */
    public static boolean releaseFromTank(
        ServerLevel level,
        CryotankBlockEntity tank,
        @org.jetbrains.annotations.Nullable net.minecraft.core.BlockPos preferred,
        boolean brokeOut
    ) {
        var tag = tank.getSpecimen();

        if (tag == null) {
            return false;
        }

        if (preferred != null) {
            var at = preferred.getBottomCenter();

            if (fits(level, at, tank.specimenWidth(), tank.specimenHeight())) {
                return release(level, tag, at, brokeOut);
            }
        }

        return releaseNear(
            level,
            tag,
            tank.getBlockPos(),
            tank.sizeX(),
            tank.sizeY(),
            tank.sizeZ(),
            tank.specimenWidth(),
            tank.specimenHeight(),
            brokeOut
        );
    }

    /**
     * Spawns a specimen at the first free spot around a {@code sx x sy x sz} box whose lowest corner is {@code min}.
     */
    public static boolean releaseNear(
        ServerLevel level,
        CompoundTag tag,
        net.minecraft.core.BlockPos min,
        int sx,
        int sy,
        int sz,
        float width,
        float height,
        boolean brokeOut
    ) {
        var centreX = min.getX() + sx / 2.0;
        var centreZ = min.getZ() + sz / 2.0;

        // On top, centred.
        var top = new Vec3(centreX, min.getY() + sy, centreZ);

        if (fits(level, top, width, height)) {
            return release(level, tag, top, brokeOut);
        }

        // Then around the base, one ring at a time, a few blocks out at most.
        var halfWidth = Math.max(0.5, width / 2.0);

        for (int ring = 0; ring <= 3; ring++) {
            var reach = ring + halfWidth + 0.05;

            for (var direction : net.minecraft.core.Direction.Plane.HORIZONTAL) {
                var at = new Vec3(
                    centreX + direction.getStepX() * (sx / 2.0 + reach),
                    min.getY(),
                    centreZ + direction.getStepZ() * (sz / 2.0 + reach)
                );

                if (fits(level, at, width, height)) {
                    return release(level, tag, at, brokeOut);
                }
            }
        }

        // Nowhere clear: on top anyway. Better a cramped specimen than one that can never come out.
        return release(level, tag, top, brokeOut);
    }

    private static boolean fits(ServerLevel level, Vec3 at, float width, float height) {
        var half = Math.max(0.3, width / 2.0);
        var box = new net.minecraft.world.phys.AABB(
            at.x - half,
            at.y,
            at.z - half,
            at.x + half,
            at.y + Math.max(0.5, height),
            at.z + half
        );

        return level.isLoaded(net.minecraft.core.BlockPos.containing(at)) && level.noCollision(box);
    }

    /**
     * Spawns a stored specimen at {@code at}.
     *
     * @param brokeOut true for a thaw breakout or a broken tank: [stated] the specimen comes out awake and turns on the
     *                 nearest player. A deliberate release from the controller is false.
     * @return true once the entity is in the world
     */
    public static boolean release(ServerLevel level, CompoundTag tag, Vec3 at, boolean brokeOut) {
        var entity = EntityType.loadEntityRecursive(
            tag.copy(),
            level,
            loaded -> {
                loaded.moveTo(at.x, at.y, at.z, loaded.getYRot(), loaded.getXRot());
                return loaded;
            }
        );

        if (entity == null || !level.tryAddFreshEntityWithPassengers(entity)) {
            return false;
        }

        if (brokeOut && entity instanceof Mob mob) {
            Player nearest = level.getNearestPlayer(mob, BREAKOUT_TARGET_RANGE);

            if (nearest != null && !nearest.isCreative() && !nearest.isSpectator()) {
                mob.setTarget(nearest);
            }
        }

        return true;
    }
}
