package com.human.common.gameplay.entity.living.human.soldier;

import com.human.common.gameplay.entity.living.human.marine.Marine;
import com.human.common.gameplay.fabrication.Faction;
import net.minecraft.world.entity.Entity;

import java.util.EnumMap;
import java.util.Map;

/**
 * How the human factions stand with each other ([stated] Oct 13): "neutral for now but once factions are properly added
 * they can turn hostile on sight depending on relations".
 * <p>
 * NEUTRAL is exactly today's marine rule - two sides ignore each other until one attacks, then MarineHostility's
 * engagement rules take over. HOSTILE means on sight: soldiers of the two factions are never allies (shots connect) and
 * hunt each other ({@code MarineGOAP.isHunted}). Every pair starts NEUTRAL; the future faction system changes a pair
 * through {@link #set}, and every targeting and friendly-fire check already reads from here.
 * </p>
 */
public final class HumanFactionRelations {

    public enum Relation {
        ALLIED,
        NEUTRAL,
        HOSTILE
    }

    private static final Map<Faction, Map<Faction, Relation>> TABLE = new EnumMap<>(Faction.class);

    private HumanFactionRelations() {}

    public static Relation between(Faction a, Faction b) {
        if (a == b) {
            return Relation.ALLIED;
        }

        var row = TABLE.get(a);

        return row == null ? Relation.NEUTRAL : row.getOrDefault(b, Relation.NEUTRAL);
    }

    /** Sets a pair both ways. Not saved: the faction system that calls this owns persistence. */
    public static synchronized void set(Faction a, Faction b, Relation relation) {
        if (a == b) {
            return;
        }

        TABLE.computeIfAbsent(a, $ -> new EnumMap<>(Faction.class)).put(b, relation);
        TABLE.computeIfAbsent(b, $ -> new EnumMap<>(Faction.class)).put(a, relation);
    }

    /** Whether two soldiers' factions are at war. Anything that is not a soldier is never at war here. */
    public static boolean areHostile(Entity a, Entity b) {
        return a instanceof Marine first && b instanceof Marine second
            && between(first.faction(), second.faction()) == Relation.HOSTILE;
    }
}
