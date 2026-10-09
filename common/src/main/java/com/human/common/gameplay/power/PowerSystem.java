package com.human.common.gameplay.power;

import com.human.common.gameplay.power.grid.PowerGridManager;
import net.minecraft.server.level.ServerLevel;

import java.util.HashMap;
import java.util.Map;

public class PowerSystem {

    private static final Map<ServerLevel, PowerGridManager> MANAGERS = new HashMap<>();

    public static PowerGridManager get(ServerLevel level) {
        return MANAGERS.computeIfAbsent(level, l -> new PowerGridManager());
    }

    /**
     * ⚠⚠ CALL THIS WHEN A LEVEL UNLOADS AND WHEN THE SERVER STOPS. The map is static and was never cleared, so every
     * level ever loaded stayed pinned in memory along with its whole grid graph and every block entity in it. On a
     * dedicated server that is a slow leak; in single-player it is worse, because leaving a world and rejoining it
     * leaves the OLD manager in the map and the new session's grids are built on stale node references.
     */
    public static void unload(ServerLevel level) {
        MANAGERS.remove(level);
    }

    /** Server shutdown: drop everything. */
    public static void clear() {
        MANAGERS.clear();
    }
}
