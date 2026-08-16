package com.human.common.gameplay.explosion.nuke;

import com.blib.api.common.server.v1.ServerScheduler;
import com.human.common.registry.key.HumanBiomeKeys;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.ChunkAccess;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Converts a disc of chunks around ground zero to the nuked biome, a few chunks per tick.
 * <p>
 * The fallout zone used to be a by-product of the crater: every chunk containing a destroyed block dragged its
 * immediate neighbours in with it, so the contaminated area was a ragged outline of the blast plus a rim, and its size
 * moved whenever the crater's did. Fallout is its own thing, so it now has its own radius measured from the centre, and
 * comes out round.
 * <p>
 * It is spread over several ticks because a chunk conversion is not cheap — the chunk has to be loaded, which for
 * terrain nobody has visited means generating it, then rewritten and resent to every player tracking it. Doing several
 * hundred of those in one tick would stall the server exactly the way the explosion itself used to.
 * <p>
 * Chunks are converted nearest-first, so the contamination visibly creeps outward from the blast rather than appearing
 * in arbitrary order.
 */
public class NuclearFalloutSpreader {

    /**
     * How many chunks to convert per tick. Deliberately modest: the worst case is a chunk that has never been
     * generated, and that cost is paid on the server thread.
     */
    private static final int CHUNKS_PER_CYCLE = 8;

    private final ServerLevel level;

    private final List<Long> chunks;

    private int index = 0;

    private NuclearFalloutSpreader(ServerLevel level, List<Long> chunks) {
        this.level = level;
        this.chunks = chunks;
    }

    /**
     * Begins converting every chunk whose centre lies within {@code radiusInChunks} of the blast.
     */
    public static void spread(ServerLevel level, BlockPos center, int radiusInChunks) {
        var centerChunkX = center.getX() >> 4;
        var centerChunkZ = center.getZ() >> 4;
        var radiusSquared = radiusInChunks * radiusInChunks;
        var chunks = new ArrayList<Long>();

        // Nearest-first, so the biome spreads outward from ground zero instead of in scan order.
        for (var ring = 0; ring <= radiusInChunks; ring++) {
            var ringSquared = ring * ring;

            for (var dx = -ring; dx <= ring; dx++) {
                for (var dz = -ring; dz <= ring; dz++) {
                    var distanceSquared = dx * dx + dz * dz;

                    if (distanceSquared > radiusSquared || distanceSquared > ringSquared) {
                        continue;
                    }

                    if (ring > 0 && distanceSquared <= (ring - 1) * (ring - 1)) {
                        continue;
                    }

                    chunks.add(chunkKey(centerChunkX + dx, centerChunkZ + dz));
                }
            }
        }

        new NuclearFalloutSpreader(level, chunks).scheduleNextCycle();
    }

    private void scheduleNextCycle() {
        ServerScheduler.schedule(this::processCycle, Duration.ofMillis(50));
    }

    private void processCycle() {
        var biome = level.registryAccess()
            .registryOrThrow(Registries.BIOME)
            .getHolderOrThrow(HumanBiomeKeys.NUKED_BIOME);
        var sampler = level.getChunkSource().randomState().sampler();
        var converted = new ArrayList<ChunkAccess>(CHUNKS_PER_CYCLE);
        var limit = Math.min(index + CHUNKS_PER_CYCLE, chunks.size());

        for (; index < limit; index++) {
            var key = chunks.get(index);
            var chunk = level.getChunk(chunkX(key), chunkZ(key));

            chunk.fillBiomesFromNoise((i, j, k, noiseSampler) -> biome, sampler);
            chunk.setUnsaved(true);

            converted.add(chunk);
        }

        if (!converted.isEmpty()) {
            // One resend for the whole batch rather than one per chunk.
            level.getChunkSource().chunkMap.resendBiomesForChunks(converted);
        }

        if (index < chunks.size()) {
            scheduleNextCycle();
        }
    }

    private static long chunkKey(int x, int z) {
        return (long) x & 0xFFFFFFFFL | ((long) z & 0xFFFFFFFFL) << 32;
    }

    private static int chunkX(long key) {
        return (int) (key & 0xFFFFFFFFL);
    }

    private static int chunkZ(long key) {
        return (int) (key >>> 32);
    }
}
