package com.human.common.gameplay.item;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;

/**
 * Whether an entity is on a titanium capture cord - kept as the one question the rest of the mod asks. [stated] Oct 3:
 * since the clean route the answer comes from {@link CaptureCordTether}'s synced holder, on both sides.
 */
public final class CaptureCordState {

    private CaptureCordState() {}

    public static boolean isOnCord(Entity entity) {
        return entity instanceof Mob mob && CaptureCordTether.isHeld(mob);
    }
}
