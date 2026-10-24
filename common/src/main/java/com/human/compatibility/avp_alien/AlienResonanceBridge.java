package com.human.compatibility.avp_alien;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/**
 * The resonator's one call into avp_alien, kept in its own class so nothing of avp_alien is loaded unless it is
 * installed (callers check AVPAlien.MOD.isLoaded() first).
 * <p>
 * ⚠ Oct 21 quality pass - an avp_alien OLDER than the resonator redesign has no Resonance class. That call would throw
 * from inside a block entity tick and take the server down, once a second, for anyone mixing an old avp_alien with this
 * avp_human. It is caught once, logged once, and the field simply stays off.
 * </p>
 */
public final class AlienResonanceBridge {

    private static volatile boolean unavailable;

    private AlienResonanceBridge() {}

    public static void pulse(ServerLevel level, BlockPos resonator) {
        if (unavailable) {
            return;
        }

        try {
            com.alien.common.gameplay.resonance.Resonance.pulse(level, resonator);
        } catch (LinkageError error) {
            unavailable = true;
            com.human.Human.LOGGER.error(
                "Resonator: this avp_alien has no resonance field (it is older than this avp_human). Update avp_alien - the resonator stays off until then.",
                error
            );
        }
    }
}
