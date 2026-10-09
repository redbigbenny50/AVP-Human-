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

    public static void onReport(S2CFrozenSolidPayload payload) {
        var level = Minecraft.getInstance().level;

        if (level != null) {
            UNTIL.put(payload.entityId(), level.getGameTime() + HOLD_TICKS);
        }
    }

    public static boolean isFrozen(Entity entity) {
        if (UNTIL.isEmpty()) {
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
