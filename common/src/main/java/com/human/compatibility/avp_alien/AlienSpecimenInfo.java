package com.human.compatibility.avp_alien;

import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

/**
 * Strain and hive of a xenomorph, for the cryotube specimen card. Touches avp_alien only inside {@link Impl}, which is
 * never loaded unless avp_alien is installed and the entity really is one of its aliens.
 */
public final class AlienSpecimenInfo {

    private AlienSpecimenInfo() {}

    /** The strain name (NORMAL, NETHER, ABERRANT, IRRADIATED), or null for anything that is not an avp_alien alien. */
    public static @Nullable String strain(Entity entity) {
        return AVPAlien.MOD.isLoaded() ? Impl.strain(entity) : null;
    }

    /** The hive signature path, "NONE" for a hiveless alien, or null for anything that is not an alien. */
    public static @Nullable String hive(Entity entity) {
        return AVPAlien.MOD.isLoaded() ? Impl.hive(entity) : null;
    }

    /**
     * [stated] Oct 3: the hive's SHORT name, e.g. {@code NO_1_2} - variant code, lineage number, location number, as
     * avp_alien names its hive locations ("Hive NO_1_2"). Null when there is no named hive or avp_alien is absent.
     */
    public static @Nullable String hiveName(Entity entity) {
        return AVPAlien.MOD.isLoaded() ? Impl.hiveName(entity) : null;
    }

    private static final class Impl {

        static @Nullable String strain(Entity entity) {
            return entity instanceof com.alien.common.gameplay.entity.living.alien.Alien alien ? alien.getVariant().name() : null;
        }

        static @Nullable String hive(Entity entity) {
            if (!(entity instanceof com.alien.common.gameplay.entity.living.alien.Alien alien)) {
                return null;
            }

            var signature = alien.getHiveManager().signature();
            return signature.isSome() ? signature.unwrap().getPath() : "NONE";
        }

        static @Nullable String hiveName(Entity entity) {
            if (!(entity instanceof com.alien.common.gameplay.entity.living.alien.Alien alien)) {
                return null;
            }

            // The hive LOCATION faction carries the name avp_alien gave it ("Hive NO_1_2"); the same membership
            // HiveManager.signature() prefers.
            for (var factionId : com.alien.Alien.MOD.factions().getFactionIds(alien.getUUID())) {
                if (com.alien.common.gameplay.hive.id.HiveLocationIds.isHiveLocationId(factionId)) {
                    var faction = com.alien.Alien.MOD.factions().get(factionId);
                    var name = faction == null ? null : faction.name();

                    if (name != null && !name.isBlank()) {
                        return name.startsWith("Hive ") ? name.substring("Hive ".length()) : name;
                    }
                }
            }

            return null;
        }
    }
}
