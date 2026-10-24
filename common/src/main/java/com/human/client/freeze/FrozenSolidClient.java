package com.human.client.freeze;

import com.human.common.network.packet.S2CFrozenSolidPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;

import java.util.HashMap;
import java.util.Map;

/**
 * Which mobs the client should draw frozen solid. Each report holds for {@link #HOLD_TICKS}; the server repeats them
 * every second while a mob stays frozen, so a thawed mob loses its frost within two seconds of thawing.
 */
public final class FrozenSolidClient {

    private static final int HOLD_TICKS = 40;

    private static final Map<Integer, Long> UNTIL = new HashMap<>();

    private FrozenSolidClient() {}

    /**
     * The world the stored timers belong to. Oct 13 audit: they are game times and entity ids of ONE world - joining a
     * world whose clock is behind the last one made any reused id look frozen until the old time came round. A world
     * change now clears them.
     */
    private static java.lang.ref.WeakReference<net.minecraft.world.level.Level> owner = new java.lang.ref.WeakReference<>(null);

    public static void onReport(S2CFrozenSolidPayload payload) {
        var level = Minecraft.getInstance().level;

        if (level != null) {
            claim(level);

            // Prune what has run out, so timers for entities that left view never pile up.
            var now = level.getGameTime();

            UNTIL.values().removeIf(until -> until < now);
            UNTIL.put(payload.entityId(), now + HOLD_TICKS);
        }
    }

    private static void claim(net.minecraft.world.level.Level level) {
        // A WEAK reference: a static strong one would keep a whole old world in memory after disconnecting.
        if (owner.get() != level) {
            owner = new java.lang.ref.WeakReference<>(level);
            UNTIL.clear();
        }
    }

    public static boolean isFrozen(Entity entity) {
        if (UNTIL.isEmpty()) {
            return false;
        }

        if (owner.get() != entity.level()) {
            claim(entity.level());
            return false;
        }

        var until = UNTIL.get(entity.getId());

        if (until == null) {
            return false;
        }

        if (entity.level().getGameTime() > until) {
            UNTIL.remove(entity.getId());
            return false;
        }

        return true;
    }
}
