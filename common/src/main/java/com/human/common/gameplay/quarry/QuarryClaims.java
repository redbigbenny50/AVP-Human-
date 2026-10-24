package com.human.common.gameplay.quarry;

import com.human.Human;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Whether a quarry may work a chunk ([stated] Oct 5): never in someone else's claim ("Mining not authorized by owner"),
 * never in xenomorph hive territory ("Mining too dangerous, hostile alien lifeforms detected").
 * <p>
 * Read through BLib's public territory API, which holds BLib's player claims and every faction's claims. A player claim
 * is BLib's {@code blib:player_claim/<owner>} faction; the quarry's owner may mine their OWN claim, and a faction claim
 * when they are a member. A hive claim is a faction of type {@code avp_alien:location} (as {@code HiveTerritory} reads
 * it) and is refused for everyone. Claims made only in other claim mods (FTB Chunks and the like) are not visible here.
 * </p>
 * <p>
 * Then the game itself is asked whether the owner may break a block there ({@link QuarryProtection}), which every other
 * protection mod answers - FTB Chunks included.
 * </p>
 * <p>
 * Cost: a claim-index lookup per chunk asked about. isClaimed is asked first: it builds BLib's index if needed, and
 * getClaimants on a level with no index would create claim data for the chunk.
 * </p>
 */
public final class QuarryClaims {

    public enum Verdict {
        FREE,
        CLAIMED,
        XENO
    }

    private static final ResourceLocation HIVE_LOCATION_TYPE = ResourceLocation.fromNamespaceAndPath("avp_alien", "location");

    private QuarryClaims() {}

    /**
     * @param y the height to ask the protection mods about (the quarry's level) - claims are usually whole columns, but
     *          region plugins can be 3D
     */
    public static Verdict check(ServerLevel level, ChunkPos chunk, @Nullable UUID owner, int y) {
        var verdict = blibVerdict(level, chunk, owner);

        // [stated] Oct 15: every other claim system too (FTB Chunks, WorldGuard on hybrid servers, ...) - ask the game
        // whether the owner may break a block here. Only for a loaded chunk; QuarryProtection never loads one.
        if (verdict == Verdict.FREE && !QuarryProtection.check(level, chunk.getMiddleBlockPosition(y), owner)) {
            return Verdict.CLAIMED;
        }

        return verdict;
    }

    private static Verdict blibVerdict(ServerLevel level, ChunkPos chunk, @Nullable UUID owner) {
        var territory = Human.MOD.territory();

        if (!territory.isClaimed(level, chunk)) {
            return Verdict.FREE;
        }

        var factions = Human.MOD.factions();
        var verdict = Verdict.FREE;

        for (var id : territory.getClaimants(level, chunk)) {
            var faction = factions.get(id);

            if (faction != null && HIVE_LOCATION_TYPE.equals(faction.typeId())) {
                return Verdict.XENO;
            }

            if (id.getNamespace().equals("blib") && id.getPath().startsWith("player_claim/")) {
                if (owner == null || !id.getPath().equals("player_claim/" + owner)) {
                    verdict = Verdict.CLAIMED;
                }

                continue;
            }

            if (faction == null || owner == null || !faction.membership().hasEntity(owner)) {
                verdict = Verdict.CLAIMED;
            }
        }

        return verdict;
    }
}
