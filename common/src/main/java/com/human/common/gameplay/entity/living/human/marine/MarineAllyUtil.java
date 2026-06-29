package com.human.common.gameplay.entity.living.human.marine;

import com.human.common.gameplay.entity.living.dog.MarineDog;
import net.minecraft.world.entity.Entity;

public class MarineAllyUtil {

    public static boolean isMarineAlly(Entity source, Entity target) {
        return source instanceof Marine && (target instanceof Marine || target instanceof MarineDog);
    }

    private MarineAllyUtil() {
        throw new UnsupportedOperationException();
    }
}
