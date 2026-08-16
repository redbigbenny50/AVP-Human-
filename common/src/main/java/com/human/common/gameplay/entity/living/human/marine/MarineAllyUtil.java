package com.human.common.gameplay.entity.living.human.marine;

import com.human.common.gameplay.entity.living.dog.MarineDog;
import net.minecraft.world.entity.Entity;

public class MarineAllyUtil {

    /**
     * Whether one marine should hold its fire around another.
     * <p>
     * ⚠ This is not only a targeting question - the hitscan handler, the rocket and the flamethrower all consult it to
     * decide whether a shot connects at all. If it says ally, the round passes straight through. So the moment two
     * sides fall out it has to stop saying ally, or enemy marines would aim at each other and do nothing.
     * <p>
     * Falling out is decided by {@link MarineHostility}, which reads engagement rather than any stored allegiance.
     */
    public static boolean isMarineAlly(Entity source, Entity target) {
        if (!(source instanceof Marine) || !(target instanceof Marine || target instanceof MarineDog)) {
            return false;
        }

        return !MarineHostility.hasBrokenAlliance(source, target);
    }

    private MarineAllyUtil() {
        throw new UnsupportedOperationException();
    }
}
