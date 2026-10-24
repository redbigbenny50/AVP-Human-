package com.human.client.render.cryo;

import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * The client-only copies the cryotank renderer draws its frozen specimens with. Being in this set is what gives an
 * entity the frost overlay and the frozen pose; nothing else ever lands in it, so a live mob is never affected.
 * <p>
 * Weak: a copy is dropped with the tank renderer's cache, and this set lets it go too.
 * </p>
 */
public final class CryoDisplay {

    private static final Set<Entity> DISPLAYS = Collections.newSetFromMap(new WeakHashMap<>());

    /**
     * Where a copy's centre bone sits in its frozen pose, in model space (blocks, before the body's turn and the mob's
     * own scale). Written by {@link CryoPose} each frame the copy is posed, read by the tank renderer to put that point
     * in the middle of the tank. Absent for a specimen whose pose names no centre bone.
     */
    private static final Map<Entity, Vector3f> CENTRES = new WeakHashMap<>();

    private CryoDisplay() {}

    public static void mark(Entity entity) {
        DISPLAYS.add(entity);
    }

    public static boolean isDisplay(Entity entity) {
        return !DISPLAYS.isEmpty() && DISPLAYS.contains(entity);
    }

    /**
     * [stated] Oct 19 (a queen sticking out of the top of her tank, and "floating off the bottom"). The copy's frozen
     * pose as actually drawn: the posed model's box, {minX, minY, minZ, maxX, maxY, maxZ} in model space (blocks,
     * before the body's turn and the mob's own scale). Measured by {@link CryoPose} from the bones every frame the copy
     * is posed; the tank renderer fits and seats the specimen by THIS, not by its hitbox - a curled queen is a block
     * taller than hers, and her pose does not start at her feet.
     */
    private static final Map<Entity, float[]> BOUNDS = new WeakHashMap<>();

    public static void setBounds(Entity entity, float @Nullable [] bounds) {
        if (bounds == null) {
            BOUNDS.remove(entity);
        } else {
            BOUNDS.put(entity, bounds);
        }
    }

    public static float @Nullable [] bounds(Entity entity) {
        return BOUNDS.isEmpty() ? null : BOUNDS.get(entity);
    }

    public static void setCentre(Entity entity, @Nullable Vector3f centre) {
        if (centre == null) {
            CENTRES.remove(entity);
        } else {
            CENTRES.put(entity, centre);
        }
    }

    public static @Nullable Vector3f centre(Entity entity) {
        return CENTRES.isEmpty() ? null : CENTRES.get(entity);
    }
}
