package com.human.common.gameplay.block.cryo;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
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

    /**
     * [stated] Oct 19 - a queen fitted a tank by her hitbox and then stuck out of its top: her frozen (curled) pose is
     * TALLER than the box she walks around in. These are the avp_alien castes whose frozen pose stands higher than
     * their hitbox, as measured from their models in their cryo poses (blocks). Matched on the end of the id so every
     * strain is covered (queen, nether_queen, aberrant_queen, irradiated_queen ...). Only the height is held to the
     * pose - the renderer shrinks a pose that is merely wide - and a mob not listed fits by its hitbox, as before.
     */
    private static final java.util.Map<String, Float> FROZEN_POSE_HEIGHTS = java.util.Map.of(
        "queen",
        6.65F,
        "empress",
        7.6F,
        "harbinger",
        7.0F,
        "praetorian",
        4.45F,
        "predalien",
        4.45F,
        "ravager",
        4.1F,
        "crusher",
        2.9F
    );

    /**
     * 🚨 Oct 24 - [stated] "Cryotubes seem to allow xenos way too big into small ones for example a praetorian in a 1x1
     * tank." The fit checked a mob's WIDTH by its hitbox, and several castes are given deliberately narrow hitboxes so
     * they can path through hive halls - a praetorian's is 0.98 blocks, so it passed a 1x1 tank, and the renderer then
     * crushed a creature nearly twice that wide into the glass.
     * <p>
     * These are the castes whose BODY is wider than their hitbox: side-to-side across the shoulders and arms, measured
     * from each model (blocks, at its own scale). Length is left out on purpose - tails trail behind and are curled
     * into the frozen pose, which the renderer already fits. Matched on the end of the id like the heights, so every
     * strain is covered; anything not listed fits by its hitbox, as before.
     * </p>
     */
    private static final java.util.Map<String, Float> FROZEN_POSE_WIDTHS = java.util.Map.ofEntries(
        java.util.Map.entry("queen", 5.6F),
        java.util.Map.entry("empress", 3.4F),
        java.util.Map.entry("harbinger", 3.0F),
        java.util.Map.entry("ravager", 2.39F),
        java.util.Map.entry("crusher", 2.3F),
        java.util.Map.entry("praetorian", 1.68F),
        java.util.Map.entry("predalien", 1.51F),
        java.util.Map.entry("boiler", 1.45F),
        java.util.Map.entry("chrysalis", 1.25F),
        java.util.Map.entry("carrier", 1.08F)
    );

    /**
     * The narrowest tank this kind of creature may ever go in, whatever its measured size.
     * <ul>
     * <li>[stated] Oct 24 - "Eggs, chest bursters, and adols should be the only ones in 1x1 tanks", then "facehuggers
     * can go in 1x1": eggs, facehuggers, chestbursters (their cocoons included) and adolescents, in every strain and
     * royal or predalien form, fit a 1x1. Every other xenomorph needs 2x2.</li>
     * <li>[stated] "Villagers and people go in 2x2 preds too": villagers, wandering traders, witches, zombie villagers
     * and illagers; avp_human's marines, W-Y soldiers and colonists; and every avp_predator mob.</li>
     * </ul>
     * Anything else (animals, other mods' mobs) fits by its size alone, as before.
     */
    public static int minimumAcross(net.minecraft.resources.ResourceLocation id) {
        var path = id.getPath();

        switch (id.getNamespace()) {
            case "avp_alien" -> {
                var small = path.contains("ovomorph")
                    || path.contains("facehugger")
                    || path.contains("chestburster")
                    || path.contains("adolescent");
                return small ? 1 : 2;
            }
            case "avp_predator" -> {
                return 2;
            }
            case "avp_human" -> {
                var person = path.equals("marine") || path.equals("tactical_marine") || path.startsWith("wy_")
                    || path.equals("colonist");
                return person ? 2 : 1;
            }
            case "minecraft" -> {
                var type = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.get(id);
                var person = type == net.minecraft.world.entity.EntityType.VILLAGER
                    || type == net.minecraft.world.entity.EntityType.WANDERING_TRADER
                    || type == net.minecraft.world.entity.EntityType.WITCH
                    || type == net.minecraft.world.entity.EntityType.ZOMBIE_VILLAGER
                    || type.is(net.minecraft.tags.EntityTypeTags.ILLAGER);
                return person ? 2 : 1;
            }
            default -> {
                return 1;
            }
        }
    }

    /** The fit, with the 1x1 rule above applied first: 0 when the tank is narrower than this kind may ever go in. */
    public static float fitScale(
        CryotankBlockEntity tank,
        net.minecraft.resources.ResourceLocation id,
        float width,
        float height
    ) {
        if (Math.min(tank.sizeX(), tank.sizeZ()) < minimumAcross(id)) {
            return 0.0F;
        }
        return fitScale(tank, width, height);
    }

    /** How wide this mob is frozen: its hitbox, or its body where that is wider (FROZEN_POSE_WIDTHS). */
    public static float frozenWidth(net.minecraft.world.entity.Entity mob) {
        var id = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType());
        var scale = mob instanceof net.minecraft.world.entity.LivingEntity living ? living.getScale() : 1.0F;
        return poseSize(FROZEN_POSE_WIDTHS, id, scale, mob.getBbWidth());
    }

    /**
     * The frozen width and height of a STORED specimen, from its saved entity id - for a tank that is re-formed around
     * a creature already inside it, which has no live entity to measure. {@code hitboxWidth}/{@code hitboxHeight} are
     * the sizes stored with it at freezing; the stored scale is the creature's own, so the hitbox already carries it.
     */
    public static float[] frozenSize(net.minecraft.nbt.CompoundTag tag, float hitboxWidth, float hitboxHeight) {
        var id = net.minecraft.resources.ResourceLocation.tryParse(tag.getString("id"));
        if (id == null) {
            return new float[] { hitboxWidth, hitboxHeight };
        }
        return new float[] {
            poseSize(FROZEN_POSE_WIDTHS, id, 1.0F, hitboxWidth),
            poseSize(FROZEN_POSE_HEIGHTS, id, 1.0F, hitboxHeight) };
    }

    private static float poseSize(
        java.util.Map<String, Float> table,
        net.minecraft.resources.ResourceLocation id,
        float scale,
        float hitbox
    ) {
        if (!"avp_alien".equals(id.getNamespace())) {
            return hitbox;
        }
        var path = id.getPath();
        for (var entry : table.entrySet()) {
            if (path.equals(entry.getKey()) || path.endsWith("_" + entry.getKey())) {
                return Math.max(hitbox, entry.getValue() * scale);
            }
        }
        return hitbox;
    }

    /** How tall this mob stands frozen: its hitbox, or its frozen pose where that is taller (FROZEN_POSE_HEIGHTS). */
    public static float frozenHeight(net.minecraft.world.entity.Entity mob) {
        var hitbox = mob.getBbHeight();
        var id = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType());

        if (!"avp_alien".equals(id.getNamespace())) {
            return hitbox;
        }

        var path = id.getPath();
        var scale = mob instanceof net.minecraft.world.entity.LivingEntity living ? living.getScale() : 1.0F;

        for (var entry : FROZEN_POSE_HEIGHTS.entrySet()) {
            if (path.equals(entry.getKey()) || path.endsWith("_" + entry.getKey())) {
                return Math.max(hitbox, entry.getValue() * scale);
            }
        }

        return hitbox;
    }

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

        // [stated] Oct 19 - "if the mob is netted it pops the net off when you place them inside". Freed the same way a
        // blade cuts it free, BEFORE it is saved, so the stored specimen carries no net.
        com.human.compatibility.avp_predator.HumanPredatorNet.release(mob);

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
        return releaseFromTankEntity(level, tank, preferred, brokeOut) != null;
    }

    /** As {@link #releaseFromTank}, handing back the specimen that came out (or null). */
    public static @org.jetbrains.annotations.Nullable Entity releaseFromTankEntity(
        ServerLevel level,
        CryotankBlockEntity tank,
        @org.jetbrains.annotations.Nullable net.minecraft.core.BlockPos preferred,
        boolean brokeOut
    ) {
        var tag = tank.getSpecimen();

        if (tag == null) {
            return null;
        }

        if (preferred != null) {
            var at = preferred.getBottomCenter();

            if (fits(level, at, tank.specimenWidth(), tank.specimenHeight())) {
                return spawn(level, tag, at, brokeOut);
            }
        }

        return releaseNearEntity(
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
        return releaseNearEntity(level, tag, min, sx, sy, sz, width, height, brokeOut) != null;
    }

    private static @org.jetbrains.annotations.Nullable Entity releaseNearEntity(
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
            return spawn(level, tag, top, brokeOut);
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
                    return spawn(level, tag, at, brokeOut);
                }
            }
        }

        // Nowhere clear: on top anyway. Better a cramped specimen than one that can never come out.
        return spawn(level, tag, top, brokeOut);
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
        return spawn(level, tag, at, brokeOut) != null;
    }

    private static @org.jetbrains.annotations.Nullable Entity spawn(ServerLevel level, CompoundTag tag, Vec3 at, boolean brokeOut) {
        var entity = EntityType.loadEntityRecursive(
            tag.copy(),
            level,
            loaded -> {
                loaded.moveTo(at.x, at.y, at.z, loaded.getYRot(), loaded.getXRot());
                return loaded;
            }
        );

        if (entity == null || !level.tryAddFreshEntityWithPassengers(entity)) {
            return null;
        }

        // [stated] Oct 19 - "when they come out they should be not netted". New specimens go in net-free already; this
        // also covers anything frozen before that rule, whose saved data may still say netted.
        if (entity instanceof net.minecraft.world.entity.LivingEntity living) {
            com.human.compatibility.avp_predator.HumanPredatorNet.release(living);
        }

        if (brokeOut && entity instanceof Mob mob) {
            Player nearest = level.getNearestPlayer(mob, BREAKOUT_TARGET_RANGE);

            if (nearest != null && !nearest.isCreative() && !nearest.isSpectator()) {
                mob.setTarget(nearest);
            }
        }

        return entity;
    }
}
