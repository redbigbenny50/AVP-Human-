package com.human.common.gameplay.entity.living.human.soldier;

import com.human.Human;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;

/**
 * Whether a spot is xenomorph hive territory ([stated] Oct 13: "the ape units should appear in xenomorph territory if
 * avp alien is installed even if its not their usual biome").
 * <p>
 * Read through BLib's public territory API, so avp_human needs nothing from avp_alien: without it no such claim ever
 * exists and this is always false. avp_alien claims a hive's territory under the hive's LOCATION faction
 * ({@code HiveLocationClaims} passes the HiveLocation id; verified in the 0.2.7s-fork jar), whose data type is
 * {@code avp_alien:location} - so only a claim of that type counts, never another avp_alien faction or a player claim.
 * </p>
 * <p>
 * Cost: a claim-index lookup, made only for a loaded chunk, once per APE patrol attempt (every few in-game days).
 * </p>
 */
public final class HiveTerritory {

    static final ResourceLocation HIVE_LOCATION_TYPE = ResourceLocation.fromNamespaceAndPath("avp_alien", "location");

    private HiveTerritory() {}

    public static boolean isHiveTerritory(ServerLevel level, BlockPos pos) {
        // Never load a chunk to answer this.
        if (!level.hasChunk(pos.getX() >> 4, pos.getZ() >> 4)) {
            return false;
        }

        var chunk = new ChunkPos(pos);
        var territory = Human.MOD.territory();

        // isClaimed first: it builds BLib's per-level claim index if needed. getClaimants on a level with no index yet
        // falls back to creating a persistent claim store for the chunk - this check must never write chunk data.
        if (!territory.isClaimed(level, chunk)) {
            return false;
        }

        var claimants = territory.getClaimants(level, chunk);

        var factions = Human.MOD.factions();

        for (var id : claimants) {
            var faction = factions.get(id);

            if (faction != null && HIVE_LOCATION_TYPE.equals(faction.typeId())) {
                return true;
            }
        }

        return false;
    }
}
