package com.human.client.render.cryo;

import net.minecraft.world.entity.Entity;

import java.util.Collections;
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

    private CryoDisplay() {}

    public static void mark(Entity entity) {
        DISPLAYS.add(entity);
    }

    public static boolean isDisplay(Entity entity) {
        return !DISPLAYS.isEmpty() && DISPLAYS.contains(entity);
    }
}
