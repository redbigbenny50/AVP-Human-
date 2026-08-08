package com.human.common.gameplay.explosion.nuke;

import com.blib.api.common.explosion.v1.Explosion;
import com.human.common.registry.init.block.CoreBlocks;
import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowLayerBlock;

import java.util.Map;

/**
 * Applies the nuke's terrain transformation and particles to one sampled block.
 * <p>
 * This runs a million times per detonation, so everything here is written for that scale. None of it changes what the
 * blast looks like — the crater, the block palette and the particle density are all as before. What changed is the
 * amount of work spent producing them:
 * <ul>
 * <li>The explosion's geometry is fixed for its whole lifetime but was being re-derived from the config on every block,
 * including two linear scans through {@code Direction.values()}. It is now resolved once, on the first block.</li>
 * <li>Interior blocks no longer trigger neighbour shape updates. Blocks in the outermost band still do — see
 * {@link #blockFlagsFor}.</li>
 * <li>Particles are batched per cycle instead of two packets per block, and skipped entirely where no player is close
 * enough to receive them — see {@link #queueParticles}.</li>
 * <li>The fallout biome is no longer this class's job. It was a by-product of which chunks happened to contain a
 * destroyed block; it is now its own deliberate region — see {@link NuclearFalloutSpreader}.</li>
 * </ul>
 */
public class NuclearExplosionEffects {

    private static final Map<Block, Block> BLOCK_TRANSFORMER_MAP = Map.ofEntries(
        Map.entry(Blocks.ANDESITE, Blocks.GRAVEL),
        Map.entry(Blocks.DEEPSLATE, Blocks.COBBLED_DEEPSLATE),
        Map.entry(Blocks.DIORITE, Blocks.SAND),
        Map.entry(Blocks.GRANITE, Blocks.RED_SAND),
        Map.entry(Blocks.GRASS_BLOCK, Blocks.COARSE_DIRT),
        Map.entry(Blocks.LAVA, Blocks.MAGMA_BLOCK),
        Map.entry(Blocks.STONE, Blocks.COBBLESTONE)
    );

    /**
     * Everything at or beyond this normalised distance is the transformed outer band — the visible rim of the crater,
     * and the only place where a surviving neighbour can be left needing a shape update.
     */
    private static final double TRANSFORM_BAND_DISTANCE = 0.98;

    private static final double SCORCH_BAND_DISTANCE = 0.75;

    /**
     * Particles are grouped into cubes this many blocks on a side, one packet per cube per cycle. Small enough that the
     * cloud still follows the shape of the blast front, large enough that a cycle produces packets in the hundreds
     * rather than the tens of thousands.
     */
    private static final int PARTICLE_CELL_SIZE = 8;

    private static final int PARTICLE_CELL_SHIFT = 3;

    private static final double PARTICLE_SPREAD = PARTICLE_CELL_SIZE / 2.0;

    /**
     * A player must be within this distance of a particle cell for that cell to be worth building a packet for. The
     * game itself discards level-particle packets beyond 32 blocks, and this is that limit plus enough slack to cover a
     * cell's half-diagonal and the spread applied when it is emitted, so nothing the player would have seen is ever
     * culled here.
     */
    private static final double PARTICLE_CULL_DISTANCE = 48.0;

    private static final double PARTICLE_CULL_DISTANCE_SQUARED = PARTICLE_CULL_DISTANCE * PARTICLE_CULL_DISTANCE;

    /**
     * Safety valve: if a cycle somehow runs long enough to touch this many distinct cells, flush early rather than
     * letting the batch grow without bound.
     */
    private static final int MAX_PENDING_PARTICLE_CELLS = 4096;

    /** Particle cell key to the number of blocks that landed in it this cycle. */
    private final Long2IntMap pendingParticleCells = new Long2IntOpenHashMap();

    private double[] nearbyPlayerPositions = null;

    private boolean geometryResolved = false;

    private int centerX;

    private int centerY;

    private int centerZ;

    private int radiusX;

    private int radiusZ;

    private int radiusYUp;

    private int radiusYDown;

    private double yScaleFactor;

    /**
     * Called at the start of every explosion cycle. Flushes whatever the previous cycle accumulated and re-reads the
     * player positions, which is what keeps the particle culling honest as players move.
     */
    public void beginCycle(ServerLevel level) {
        flushParticles(level);
        nearbyPlayerPositions = null;
    }

    /** Called at the end of every explosion cycle so the last batch is never left unsent. */
    public void flushParticles(ServerLevel level) {
        if (pendingParticleCells.isEmpty()) {
            return;
        }

        for (var entry : pendingParticleCells.long2IntEntrySet()) {
            var key = entry.getLongKey();
            var count = entry.getIntValue();

            var x = BlockPos.getX(key) * PARTICLE_CELL_SIZE + PARTICLE_SPREAD;
            var y = BlockPos.getY(key) * PARTICLE_CELL_SIZE + PARTICLE_SPREAD;
            var z = BlockPos.getZ(key) * PARTICLE_CELL_SIZE + PARTICLE_SPREAD;

            emit(level, ParticleTypes.FLASH, x, y, z, count);
            emit(level, ParticleTypes.SMOKE, x, y, z, count);
        }

        pendingParticleCells.clear();
    }

    private void emit(ServerLevel level, ParticleOptions particle, double x, double y, double z, int count) {
        level.sendParticles(particle, x, y, z, count, PARTICLE_SPREAD, PARTICLE_SPREAD, PARTICLE_SPREAD, 0.0);
    }

    public void apply(Explosion explosion, BlockPos pos) {
        var level = explosion.level();

        resolveGeometry(explosion);

        var x = pos.getX() - centerX;
        var y = pos.getY() - centerY;
        var z = pos.getZ() - centerZ;

        // Same normalisation ExplosionUtil performs, computed from the cached radii so the config is not
        // walked once per block. The vertical term is scaled to balance vertical against horizontal edges.
        var horizontalDistance = (double) (x * x) / (radiusX * radiusX) + (double) (z * z) / (radiusZ * radiusZ);
        var verticalRadius = y < 0 ? radiusYDown : radiusYUp;
        var verticalDistance = (double) (y * y) / (verticalRadius * verticalRadius) * yScaleFactor;
        var distance = horizontalDistance + verticalDistance;

        var flags = blockFlagsFor(distance);
        var rand = level.random.nextFloat();
        var blockState = level.getBlockState(pos);

        if (distance > TRANSFORM_BAND_DISTANCE) {

            Block transformedBlock;

            if (rand > horizontalDistance) {
                transformedBlock = Blocks.BLACKSTONE;
            } else {
                if (blockState.is(BlockTags.DIRT)) {
                    transformedBlock = Blocks.BASALT;
                } else if (blockState.is(BlockTags.SAND) || blockState.is(Blocks.SANDSTONE) || blockState.is(Blocks.RED_SANDSTONE)) {
                    var rand2 = level.random.nextInt(100);

                    if (rand2 < 66) {
                        transformedBlock = CoreBlocks.TRINITITE_BLOCK.get();
                    } else {
                        transformedBlock = Blocks.MAGMA_BLOCK;
                    }
                } else {
                    transformedBlock = BLOCK_TRANSFORMER_MAP.getOrDefault(blockState.getBlock(), Blocks.BASALT);
                }
            }

            if (blockState.isSolidRender(level, pos) && level.getRandom().nextInt(10) < 2) {
                level.setBlock(
                    pos.above(),
                    CoreBlocks.ASH_BLOCK.get().defaultBlockState().setValue(SnowLayerBlock.LAYERS, 1),
                    flags
                );
            }
            level.setBlock(pos, transformedBlock.defaultBlockState(), flags);
        } else if (distance > SCORCH_BAND_DISTANCE) {
            if (rand > horizontalDistance) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), flags);
            } else {
                level.setBlock(pos, Blocks.FIRE.defaultBlockState(), flags);
            }
        } else {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), flags);
        }

        queueParticles(level, pos);
    }

    /**
     * Neighbour shape updates are what let a torch drop off a wall that is no longer there. Inside the blast nothing
     * survives to need one, so the interior sets blocks with {@code UPDATE_KNOWN_SHAPE} and skips the update entirely —
     * for a full-size nuke that is millions of recursive update passes over terrain that is being annihilated anyway.
     * <p>
     * The outermost transformed band keeps the original flags, because that is the boundary where blocks the blast did
     * not reach sit against blocks it did, and it is the only place the difference could ever be seen.
     */
    private int blockFlagsFor(double distance) {
        var flags = Block.UPDATE_CLIENTS | Block.UPDATE_SUPPRESS_DROPS;

        return distance > TRANSFORM_BAND_DISTANCE ? flags : flags | Block.UPDATE_KNOWN_SHAPE;
    }

    /**
     * Records this block's contribution to its particle cell instead of sending two packets for it.
     * <p>
     * A level-particle packet is built before the game checks whether any player is near enough to receive it, so at
     * the old rate almost all of that construction was thrown away. The cell is checked against the players first, and
     * everything that survives is emitted once per cell at the end of the cycle with the block count as the particle
     * count, so the same number of particles reaches the client.
     */
    private void queueParticles(ServerLevel level, BlockPos pos) {
        if (!isAnyPlayerNear(level, pos)) {
            return;
        }

        var key = BlockPos.asLong(
            pos.getX() >> PARTICLE_CELL_SHIFT,
            pos.getY() >> PARTICLE_CELL_SHIFT,
            pos.getZ() >> PARTICLE_CELL_SHIFT
        );

        pendingParticleCells.put(key, pendingParticleCells.get(key) + 1);

        if (pendingParticleCells.size() >= MAX_PENDING_PARTICLE_CELLS) {
            flushParticles(level);
        }
    }

    private boolean isAnyPlayerNear(ServerLevel level, BlockPos pos) {
        var positions = nearbyPlayerPositions;

        if (positions == null) {
            var players = level.players();

            positions = new double[players.size() * 3];

            for (var i = 0; i < players.size(); i++) {
                var player = players.get(i);

                positions[i * 3] = player.getX();
                positions[i * 3 + 1] = player.getY();
                positions[i * 3 + 2] = player.getZ();
            }

            nearbyPlayerPositions = positions;
        }

        for (var i = 0; i < positions.length; i += 3) {
            var dx = positions[i] - pos.getX();
            var dy = positions[i + 1] - pos.getY();
            var dz = positions[i + 2] - pos.getZ();

            if (dx * dx + dy * dy + dz * dz <= PARTICLE_CULL_DISTANCE_SQUARED) {
                return true;
            }
        }

        return false;
    }

    private void resolveGeometry(Explosion explosion) {
        if (geometryResolved) {
            return;
        }

        var config = explosion.config();
        var centerPos = config.centerBlockPosition();

        centerX = centerPos.getX();
        centerY = centerPos.getY();
        centerZ = centerPos.getZ();

        radiusX = config.largestRadius(Direction.Axis.X);
        radiusZ = config.largestRadius(Direction.Axis.Z);
        radiusYDown = config.radius(Direction.DOWN);
        radiusYUp = config.radius(Direction.UP);

        // Scale Y contribution to better balance vertical vs horizontal edge checks
        yScaleFactor = (radiusX + radiusZ) / 2.0 / Math.max(radiusYUp, radiusYDown);

        geometryResolved = true;
    }
}
