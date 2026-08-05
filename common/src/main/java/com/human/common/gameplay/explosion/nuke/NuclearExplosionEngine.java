package com.human.common.gameplay.explosion.nuke;

import com.alien.common.gameplay.entity.living.alien.Alien;
import com.alien.common.model.alien.variant.AlienVariant;
import com.alien.common.util.AlienTransitionUtil;
import com.human.Human;
import com.human.common.gameplay.effect.RadiationStatusEffect;
import com.human.common.gameplay.entity.nuke.MushroomCloudEntity;
import com.human.common.network.packet.S2CNukeEffectPayload;
import com.human.common.property.HumanProperties;
import com.human.common.property.HumanPropertyAccess;
import com.human.common.registry.init.HumanMobEffects;
import com.human.common.registry.init.block.CoreBlocks;
import com.human.common.registry.key.HumanBiomeKeys;
import com.human.compatibility.avp_alien.AVPAlien;
import com.human.util.HumanPredicates;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

public class NuclearExplosionEngine {

    private static final TicketType<UUID> PRIMED_NUKE_TICKET = TicketType.create(
        "avp_human_primed_nuke",
        UUID::compareTo,
        20 * 20
    );

    private static final TicketType<UUID> NUCLEAR_EXPLOSION_TICKET = TicketType.create(
        "avp_human_nuclear_explosion",
        UUID::compareTo,
        20 * 60
    );

    private static final int PRIMED_NUKE_TICKET_DISTANCE = 2;

    private static final int MAX_EXPLOSION_TICKET_DISTANCE = 10;

    private static final Queue<NukeJob> PENDING_JOBS = new ArrayDeque<>();

    private static final List<NukeJob> ACTIVE_JOBS = new ArrayList<>();

    private static final List<NukeJob> DEFERRED_JOBS = new ArrayList<>();

    private static final int BLOCK_UPDATE_FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS;

    private static final int FLUID_DRY_UPDATE_FLAGS =
        Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS;

    private static final int FLUID_SEAL_UPDATE_FLAGS = Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS;

    private static final long STARTUP_WORK_BUDGET_NANOS = 3_000_000L;

    private static final long TICK_WORK_BUDGET_NANOS = 9_000_000L;

    private static final int TIME_CHECK_INTERVAL = 63;

    private static final int SHOCKWAVE_SCAN_INTERVAL_TICKS = 2;

    private static final int BIOME_CONVERSION_RATE_NUMERATOR = 3;

    private static final int BIOME_CONVERSION_RATE_DENOMINATOR = 2;

    private static final int WATER_EVAPORATION_PASSES = 8;

    private static final int WATER_EVAPORATION_RADIUS_PADDING = 32;

    private static final int MIN_FALLOUT_RADIUS_PADDING = 32;

    private static final double FALLOUT_RADIUS_SCALE = 1.35D;

    private static final int DEFERRED_CHUNK_CHECKS_PER_TICK = 12;

    private static final int MAX_DEFERRED_NUKE_JOBS = 16;

    private static final double WATER_RIM_SEAL_INNER_NORMALIZED = 0.88D;

    private static final double WATER_RIM_SEAL_OUTER_NORMALIZED = 1.12D;

    private static final Predicate<BlockState> HAS_FLUID_STATE = state -> !state.getFluidState().isEmpty();

    private NuclearExplosionEngine() {
        throw new UnsupportedOperationException();
    }

    public static void keepPrimedNukeLoaded(ServerLevel level, BlockPos center, UUID nukeId) {
        level.getChunkSource()
            .addRegionTicket(
                PRIMED_NUKE_TICKET,
                new ChunkPos(center),
                PRIMED_NUKE_TICKET_DISTANCE,
                nukeId
            );
    }

    public static void releasePrimedNukeTicket(ServerLevel level, BlockPos center, UUID nukeId) {
        level.getChunkSource()
            .removeRegionTicket(
                PRIMED_NUKE_TICKET,
                new ChunkPos(center),
                PRIMED_NUKE_TICKET_DISTANCE,
                nukeId
            );
    }

    public static void detonate(ServerLevel level, Vec3 center, Entity source) {
        if (!level.getServer().isSameThread()) {
            level.getServer().execute(() -> detonate(level, center, source));
            return;
        }

        var profile = NukeProfile.fromProperties();
        var seed = createDetonationSeed(level, center, source);
        var centerPos = BlockPos.containing(center);

        if (source != null) {
            releasePrimedNukeTicket(level, centerPos, source.getUUID());
        }

        var ticketId = UUID.randomUUID();
        var job = new NukeJob(level, center, source, profile, ticketId);

        spawnVisualCarrier(level, center, profile, seed);
        sendClientEffects(level, centerPos, profile);
        spawnFallbackParticles(level, center, profile);
        applyImmediateEntityEffects(level, center, source, profile);
        level.playSound(null, centerPos, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 24.0F, 0.55F);
        level.playSound(null, centerPos, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER, 12.0F, 0.7F);

        job.process(
            profile.initialTerrainBlocks(),
            profile.initialWaterEvaporationBlocks(),
            System.nanoTime() + STARTUP_WORK_BUDGET_NANOS
        );
        if (!job.isDone()) {
            PENDING_JOBS.add(job);
        }

        Human.LOGGER.info("Started nuclear explosion @ {} with radius {}", center, profile.horizontalRadius());
    }

    private static long createDetonationSeed(ServerLevel level, Vec3 center, Entity source) {
        var centerPos = BlockPos.containing(center);
        var seed = level.getSeed() ^ centerPos.asLong() ^ Double.doubleToLongBits(center.x) ^ Long.rotateLeft(
            Double.doubleToLongBits(center.y),
            21
        ) ^ Long.rotateLeft(Double.doubleToLongBits(center.z), 42) ^ Long.rotateLeft(level.getGameTime(), 11) ^ System.nanoTime();

        if (source != null) {
            var uuid = source.getUUID();
            seed ^= uuid.getMostSignificantBits();
            seed ^= Long.rotateLeft(uuid.getLeastSignificantBits(), 17);
            seed ^= (long) source.getId() << 32;
        }

        return mixSeed(seed);
    }

    private static long mixSeed(long seed) {
        seed = (seed ^ (seed >>> 30)) * 0xBF58476D1CE4E5B9L;
        seed = (seed ^ (seed >>> 27)) * 0x94D049BB133111EBL;
        return seed ^ (seed >>> 31);
    }

    public static void tick(Level level) {
        if (level.isClientSide || !(level instanceof ServerLevel serverLevel)) {
            return;
        }

        var maxActiveJobs = Math.max(
            1,
            HumanPropertyAccess.INSTANCE.get(HumanProperties.Blocks.Nuke.MAX_ACTIVE_NUKE_JOBS)
        );
        while (ACTIVE_JOBS.size() < maxActiveJobs && !PENDING_JOBS.isEmpty()) {
            ACTIVE_JOBS.add(PENDING_JOBS.remove());
        }

        var deadlineNanos = System.nanoTime() + TICK_WORK_BUDGET_NANOS;
        for (var i = ACTIVE_JOBS.size() - 1; i >= 0; i--) {
            var job = ACTIVE_JOBS.get(i);
            if (job.level != serverLevel) {
                continue;
            }

            if (isPastDeadline(deadlineNanos)) {
                break;
            }

            job.process(job.profile.terrainBlocksPerTick(), job.profile.waterEvaporationBlocksPerTick(), deadlineNanos);
            if (job.isPrimaryDone()) {
                if (job.hasDeferredWork()) {
                    job.finishPrimaryPass();
                    deferJob(job);
                } else {
                    job.finish();
                }
                ACTIVE_JOBS.remove(i);
            }
        }

        processDeferredJobs(serverLevel, deadlineNanos);
    }

    private static void deferJob(NukeJob job) {
        if (DEFERRED_JOBS.size() >= MAX_DEFERRED_NUKE_JOBS) {
            DEFERRED_JOBS.remove(0).finishDeferredExpired();
        }
        DEFERRED_JOBS.add(job);
    }

    private static void processDeferredJobs(ServerLevel level, long deadlineNanos) {
        if (DEFERRED_JOBS.isEmpty() || isPastDeadline(deadlineNanos)) {
            return;
        }

        var chunkChecks = DEFERRED_CHUNK_CHECKS_PER_TICK;
        for (var i = DEFERRED_JOBS.size() - 1; i >= 0 && chunkChecks > 0 && !isPastDeadline(deadlineNanos); i--) {
            var job = DEFERRED_JOBS.get(i);
            if (job.level != level) {
                continue;
            }

            chunkChecks = job.processDeferred(
                Math.max(512, job.profile.terrainBlocksPerTick() / 2),
                Math.max(512, job.profile.waterEvaporationBlocksPerTick() / 3),
                chunkChecks,
                deadlineNanos
            );
            if (job.isDone()) {
                job.finish();
                DEFERRED_JOBS.remove(i);
            }
        }
    }

    private static void spawnVisualCarrier(ServerLevel level, Vec3 center, NukeProfile profile, long seed) {
        var visualBase = findVisualBase(level, center);
        var cloud = new MushroomCloudEntity(level, visualBase.x(), visualBase.y(), visualBase.z());
        cloud.configure(profile.horizontalRadius(), seed, profile.cloudDurationTicks());
        level.addFreshEntity(cloud);
    }

    private static Vec3 findVisualBase(ServerLevel level, Vec3 center) {
        var x = Mth.floor(center.x);
        var z = Mth.floor(center.z);
        var surfaceY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        var y = Math.abs(center.y - surfaceY) > 4.0 ? surfaceY : center.y;
        return new Vec3(center.x, y, center.z);
    }

    private static void spawnFallbackParticles(ServerLevel level, Vec3 center, NukeProfile profile) {
        level.sendParticles(ParticleTypes.FLASH, center.x, center.y, center.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, center.x, center.y, center.z, 2, 1.0D, 1.0D, 1.0D, 0.0D);
    }

    private static void sendClientEffects(ServerLevel level, BlockPos center, NukeProfile profile) {
        var payload = new S2CNukeEffectPayload(
            center,
            profile.horizontalRadius(),
            profile.screenFlashIntensity(),
            profile.screenShakeIntensity(),
            profile.cloudDurationTicks()
        );
        var range = profile.clientEffectRange();
        var rangeSqr = range * range;

        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(Vec3.atCenterOf(center)) <= rangeSqr) {
                Human.MOD.networking().sendToClient(player, payload);
            }
        }
    }

    private static void applyImmediateEntityEffects(ServerLevel level, Vec3 center, Entity source, NukeProfile profile) {
        var radius = profile.horizontalRadius();
        var bounds = new AABB(
            center.x - radius,
            center.y - profile.downwardRadius(),
            center.z - radius,
            center.x + radius,
            center.y + profile.upwardRadius(),
            center.z + radius
        );

        for (var entity : level.getEntities(source, bounds)) {
            var distanceSqr = entity.distanceToSqr(center);
            if (distanceSqr > radius * radius) {
                continue;
            }

            var distance = Math.sqrt(distanceSqr);
            var falloff = Mth.clamp(1.0 - (distance / radius), 0.0, 1.0);
            if (falloff <= 0.0) {
                continue;
            }

            var damage = (float) Math.max(5.0, profile.maxDamage() * falloff);
            entity.igniteForSeconds(profile.fireSeconds());
            entity.hurt(level.damageSources().explosion(source, source), damage);
            applyKnockback(center, entity, profile.maxKnockback() * falloff);
            applyRadiation(entity, falloff);
            applyAlienTransition(entity);
        }
    }

    private static void applyShockwaveEntityEffects(
        ServerLevel level,
        Vec3 center,
        Entity source,
        NukeProfile profile,
        Set<UUID> affected,
        double currentRadius,
        double previousRadius
    ) {
        var bounds = new AABB(
            center.x - currentRadius,
            center.y - profile.downwardRadius(),
            center.z - currentRadius,
            center.x + currentRadius,
            center.y + profile.upwardRadius(),
            center.z + currentRadius
        );

        for (var entity : level.getEntities(source, bounds)) {
            var uuid = entity.getUUID();
            if (affected.contains(uuid)) {
                continue;
            }

            var distance = Math.sqrt(entity.distanceToSqr(center));
            if (distance <= previousRadius || distance > currentRadius || distance > profile.horizontalRadius()) {
                continue;
            }

            affected.add(uuid);
            var falloff = Mth.clamp(1.0 - (distance / profile.horizontalRadius()), 0.0, 1.0);
            entity.igniteForSeconds(Math.max(1, (int) (profile.fireSeconds() * 0.5F * falloff)));
            entity.hurt(
                level.damageSources().explosion(source, source),
                (float) Math.max(2.0, profile.maxDamage() * 0.08F * falloff)
            );
            applyKnockback(center, entity, profile.maxKnockback() * 0.65F * falloff);
            applyRadiation(entity, falloff * 0.5F);
        }
    }

    private static void applyKnockback(Vec3 center, Entity entity, double strength) {
        var delta = entity.position().subtract(center);
        if (delta.lengthSqr() < 0.001) {
            delta = new Vec3(0.0, 1.0, 0.0);
        }

        var push = delta.normalize().scale(strength);
        entity.push(push.x, Math.max(0.25, push.y + 0.25), push.z);
        entity.hurtMarked = true;
    }

    private static void applyRadiation(Entity entity, double falloff) {
        if (entity instanceof LivingEntity livingEntity && HumanPredicates.canBeIrradiated(entity)) {
            var duration = (int) Mth.clamp(
                RadiationStatusEffect.LONG_EFFECT_DURATION_IN_TICKS * falloff,
                RadiationStatusEffect.MEDIUM_EFFECT_DURATION_IN_TICKS,
                RadiationStatusEffect.LONG_EFFECT_DURATION_IN_TICKS
            );
            var amplifier = falloff > 0.66 ? 2 : falloff > 0.33 ? 1 : 0;
            livingEntity.addEffect(new MobEffectInstance(HumanMobEffects.getRadiationHolder(), duration, amplifier));
        }
    }

    private static void applyAlienTransition(Entity entity) {
        if (AVPAlien.MOD.isLoaded() && entity instanceof Alien alien) {
            AlienTransitionUtil.transitionIntoVariant(alien, AlienVariant.IRRADIATED);
        }
    }

    public record NukeProfile(
        int horizontalRadius,
        int upwardRadius,
        int downwardRadius,
        int terrainBlocksPerTick,
        int maxActiveJobs,
        int cloudParticleBudget,
        float screenFlashIntensity,
        float screenShakeIntensity,
        int cloudDurationTicks,
        float maxDamage,
        float maxKnockback,
        int fireSeconds
    ) {

        public static NukeProfile fromProperties() {
            return new NukeProfile(
                clampInt(HumanProperties.Blocks.Nuke.HORIZONTAL_RADIUS, 16, 512),
                clampInt(HumanProperties.Blocks.Nuke.UPWARD_RADIUS, 8, 256),
                clampInt(HumanProperties.Blocks.Nuke.DOWNWARD_RADIUS, 8, 256),
                scaleWorkBudget(clampInt(HumanProperties.Blocks.Nuke.TERRAIN_BLOCKS_PER_TICK, 1024, 50000)),
                clampInt(HumanProperties.Blocks.Nuke.MAX_ACTIVE_NUKE_JOBS, 1, 8),
                clampInt(HumanProperties.Blocks.Nuke.CLOUD_PARTICLE_BUDGET, 512, 20000),
                clampFloat(HumanProperties.Blocks.Nuke.SCREEN_FLASH_INTENSITY, 0.0F, 1.0F),
                clampFloat(HumanProperties.Blocks.Nuke.SCREEN_SHAKE_INTENSITY, 0.0F, 1.0F),
                20 * 45,
                1000.0F,
                5.0F,
                15
            );
        }

        private static int clampInt(com.human.common.property.HumanProperty<Integer> property, int min, int max) {
            return Mth.clamp(HumanPropertyAccess.INSTANCE.get(property), min, max);
        }

        private static float clampFloat(com.human.common.property.HumanProperty<Float> property, float min, float max) {
            return Mth.clamp(HumanPropertyAccess.INSTANCE.get(property), min, max);
        }

        private static int scaleWorkBudget(int budget) {
            return Mth.ceil(budget * 1.5F);
        }

        public int initialTerrainBlocks() {
            return Mth.clamp(terrainBlocksPerTick / 2, 1024, 4096);
        }

        public int initialWaterEvaporationBlocks() {
            return Mth.clamp(terrainBlocksPerTick / 4, 512, 2048);
        }

        public int waterEvaporationBlocksPerTick() {
            return Mth.clamp(terrainBlocksPerTick / 2, 1024, 12000);
        }

        public int waterEvaporationRadius() {
            return horizontalRadius + WATER_EVAPORATION_RADIUS_PADDING;
        }

        public int falloutRadius() {
            return Math.max(horizontalRadius + MIN_FALLOUT_RADIUS_PADDING, Mth.ceil(horizontalRadius * FALLOUT_RADIUS_SCALE));
        }

        public double clientEffectRange() {
            return Math.max(900.0, horizontalRadius * 9.0);
        }
    }

    private static final class NukeJob {

        private final ServerLevel level;

        private final Vec3 center;

        private final Entity source;

        private final NukeProfile profile;

        private final UUID ticketId;

        private final ChunkPos ticketCenter;

        private final int ticketDistance;

        private final BlastShape terrainShape;

        private final BlastShape waterShape;

        private final BlastShape falloutShape;

        private final List<ChunkPos> terrainChunks;

        private final List<ChunkPos> waterChunks;

        private final Set<UUID> shockwaveEntities = new HashSet<>();

        private final Queue<ChunkPos> pendingBiomeConversions = new ArrayDeque<>();

        private final Queue<ChunkPos> deferredTerrainChunks = new ArrayDeque<>();

        private final Queue<ChunkPos> deferredWaterChunks = new ArrayDeque<>();

        private final Queue<ChunkPos> deferredBiomeConversions = new ArrayDeque<>();

        private final Set<Long> deferredTerrainChunkKeys = new HashSet<>();

        private final Set<Long> deferredWaterChunkKeys = new HashSet<>();

        private final Set<Long> deferredBiomeChunkKeys = new HashSet<>();

        private final long startedAt;

        private WaterEvaporationCursor waterEvaporationCursor;

        private ChunkCursor currentTerrainCursor;

        private ChunkCursor currentDeferredTerrainCursor;

        private WaterEvaporationCursor currentDeferredWaterCursor;

        private ChunkPos currentDeferredWaterChunk;

        private int chunkIndex;

        private int shockwaveTick;

        private int biomeConversionRemainder;

        private double lastShockwaveRadius;

        private int changedBlocks;

        private int evaporatedWaterBlocks;

        private int skippedUnloadedChunks;

        private int processedTerrainChunks;

        private int deferredTerrainChunksCompleted;

        private int deferredWaterChunksCompleted;

        private int deferredWaterPassesRemaining;

        private boolean primaryPassLogged;

        private boolean ticketReleased;

        private int waterEvaporationPassesRemaining = WATER_EVAPORATION_PASSES;

        private NukeJob(ServerLevel level, Vec3 center, Entity source, NukeProfile profile, UUID ticketId) {
            this.level = level;
            this.center = center;
            this.source = source;
            this.profile = profile;
            this.ticketId = ticketId;
            this.ticketCenter = new ChunkPos(BlockPos.containing(center));
            this.ticketDistance = Math.min(
                MAX_EXPLOSION_TICKET_DISTANCE,
                Mth.ceil(profile.horizontalRadius() / 16.0F) + 1
            );
            level.getChunkSource()
                .addRegionTicket(
                    NUCLEAR_EXPLOSION_TICKET,
                    ticketCenter,
                    ticketDistance,
                    ticketId
                );
            this.terrainShape = BlastShape.terrain(center, profile);
            this.waterShape = BlastShape.water(center, profile);
            this.falloutShape = BlastShape.fallout(center, profile);

            var terrainChunks = collectChunkPositions(level, terrainShape);
            this.terrainChunks = terrainChunks.chunkPositions();
            var waterChunks = collectChunkPositions(level, waterShape);
            this.waterChunks = waterChunks.chunkPositions();
            var falloutChunks = collectChunkPositions(level, falloutShape);
            this.pendingBiomeConversions.addAll(falloutChunks.chunkPositions());

            this.waterEvaporationCursor = new WaterEvaporationCursor(level, this.waterChunks, this::deferWaterChunk);
            this.skippedUnloadedChunks =
                terrainChunks.skippedUnloadedChunks()
                    + waterChunks.skippedUnloadedChunks()
                    + falloutChunks.skippedUnloadedChunks();
            this.startedAt = System.currentTimeMillis();
        }

        private void process(int terrainBudget, int waterBudget, long deadlineNanos) {
            tickShockwave();
            processBiomeConversions(deadlineNanos);
            if (!pendingBiomeConversions.isEmpty() || isPastDeadline(deadlineNanos)) {
                return;
            }

            evaporateWater(waterBudget, sliceDeadline(deadlineNanos, 3));

            while (terrainBudget > 0 && chunkIndex < terrainChunks.size() && !isPastDeadline(deadlineNanos)) {
                if (currentTerrainCursor == null) {
                    var chunkPos = terrainChunks.get(chunkIndex);
                    if (!level.hasChunk(chunkPos.x, chunkPos.z)) {
                        deferTerrainChunk(chunkPos);
                        chunkIndex++;
                        continue;
                    }

                    currentTerrainCursor = new ChunkCursor(level, chunkPos);
                } else if (!level.hasChunk(currentTerrainCursor.chunkPos.x, currentTerrainCursor.chunkPos.z)) {
                    deferTerrainChunk(currentTerrainCursor.chunkPos);
                    currentTerrainCursor = null;
                    continue;
                }

                terrainBudget = currentTerrainCursor.process(level, terrainShape, terrainBudget, deadlineNanos);
                changedBlocks += currentTerrainCursor.changedBlocks;
                currentTerrainCursor.changedBlocks = 0;

                if (currentTerrainCursor.finished) {
                    processedTerrainChunks++;
                    currentTerrainCursor = null;
                    chunkIndex++;
                }
            }

            releaseWorkTicketIfTerrainComplete();
        }

        private boolean isPrimaryDone() {
            return chunkIndex >= terrainChunks.size()
                && currentTerrainCursor == null
                && pendingBiomeConversions.isEmpty()
                && waterEvaporationPassesRemaining <= 0;
        }

        private boolean hasDeferredWork() {
            return currentDeferredTerrainCursor != null
                || !deferredTerrainChunks.isEmpty()
                || currentDeferredWaterCursor != null
                || !deferredWaterChunks.isEmpty()
                || !deferredBiomeConversions.isEmpty()
                || !pendingBiomeConversions.isEmpty();
        }

        private boolean isDone() {
            return isPrimaryDone() && !hasDeferredWork();
        }

        private void finishPrimaryPass() {
            if (primaryPassLogged) {
                return;
            }

            primaryPassLogged = true;
            releaseWorkTicketIfTerrainComplete();
            Human.LOGGER.info(
                "Nuclear explosion @ {} completed loaded chunks; deferred {} terrain chunks, {} water chunks, and {} biome chunks until they load",
                center,
                deferredTerrainChunks.size(),
                deferredWaterChunks.size(),
                deferredBiomeConversions.size()
            );
        }

        private void finish() {
            releaseWorkTicket();
            var elapsed = System.currentTimeMillis() - startedAt;
            Human.LOGGER.info(
                "Nuclear explosion @ {} completed terrain pass in {}ms, changing {} blocks and evaporating {} fluid blocks across {}/{} terrain chunks; initially deferred {} chunk entries",
                center,
                elapsed,
                changedBlocks,
                evaporatedWaterBlocks,
                processedTerrainChunks,
                terrainChunks.size(),
                skippedUnloadedChunks
            );
        }

        private void finishDeferredExpired() {
            releaseWorkTicket();
            Human.LOGGER.warn(
                "Dropping deferred nuclear explosion @ {} with {} terrain chunks, {} water chunks, and {} biome chunks still unloaded",
                center,
                deferredTerrainChunks.size(),
                deferredWaterChunks.size(),
                deferredBiomeConversions.size()
            );
        }

        private void releaseWorkTicket() {
            if (ticketReleased) {
                return;
            }

            ticketReleased = true;
            level.getChunkSource()
                .removeRegionTicket(
                    NUCLEAR_EXPLOSION_TICKET,
                    ticketCenter,
                    ticketDistance,
                    ticketId
                );
        }

        private void releaseWorkTicketIfTerrainComplete() {
            if (
                chunkIndex >= terrainChunks.size()
                    && currentTerrainCursor == null
                    && currentDeferredTerrainCursor == null
                    && deferredTerrainChunks.isEmpty()
            ) {
                releaseWorkTicket();
            }
        }

        private void processBiomeConversions(long deadlineNanos) {
            biomeConversionRemainder += BIOME_CONVERSION_RATE_NUMERATOR;
            var conversionBudget = biomeConversionRemainder / BIOME_CONVERSION_RATE_DENOMINATOR;
            biomeConversionRemainder %= BIOME_CONVERSION_RATE_DENOMINATOR;
            var converted = 0;
            while (
                converted < conversionBudget
                    && !pendingBiomeConversions.isEmpty()
                    && !isPastDeadline(deadlineNanos)
            ) {
                var chunkPos = pendingBiomeConversions.remove();
                if (!convertChunkBiome(level, chunkPos)) {
                    deferBiomeConversion(chunkPos);
                }
                converted++;
            }
        }

        private int processDeferred(int terrainBudget, int waterBudget, int chunkChecks, long deadlineNanos) {
            chunkChecks = processDeferredTerrain(terrainBudget, chunkChecks, deadlineNanos);
            releaseWorkTicketIfTerrainComplete();
            if (chunkChecks > 0 && !isPastDeadline(deadlineNanos)) {
                chunkChecks = processDeferredWater(waterBudget, chunkChecks, deadlineNanos);
            }
            processBiomeConversions(deadlineNanos);
            if (chunkChecks > 0 && !isPastDeadline(deadlineNanos)) {
                chunkChecks = processDeferredBiomeConversions(chunkChecks, deadlineNanos);
            }
            return chunkChecks;
        }

        private int processDeferredTerrain(int budget, int chunkChecks, long deadlineNanos) {
            while (budget > 0 && chunkChecks > 0 && !isPastDeadline(deadlineNanos)) {
                if (currentDeferredTerrainCursor == null) {
                    var chunkPos = nextLoadedDeferredTerrainChunk(chunkChecks);
                    chunkChecks = chunkPos.chunkChecksRemaining();
                    if (chunkPos.chunkPos() == null) {
                        return chunkChecks;
                    }

                    currentDeferredTerrainCursor = new ChunkCursor(level, chunkPos.chunkPos());
                } else if (
                    !level.hasChunk(currentDeferredTerrainCursor.chunkPos.x, currentDeferredTerrainCursor.chunkPos.z)
                ) {
                    deferTerrainChunk(currentDeferredTerrainCursor.chunkPos);
                    currentDeferredTerrainCursor = null;
                    continue;
                }

                budget = currentDeferredTerrainCursor.process(level, terrainShape, budget, deadlineNanos);
                changedBlocks += currentDeferredTerrainCursor.changedBlocks;
                currentDeferredTerrainCursor.changedBlocks = 0;

                if (!currentDeferredTerrainCursor.finished) {
                    return chunkChecks;
                }

                processedTerrainChunks++;
                deferredTerrainChunksCompleted++;
                currentDeferredTerrainCursor = null;
            }

            return chunkChecks;
        }

        private DeferredChunkPoll nextLoadedDeferredTerrainChunk(int chunkChecks) {
            var checksThisPass = Math.min(chunkChecks, deferredTerrainChunks.size());
            while (checksThisPass > 0) {
                var chunkPos = deferredTerrainChunks.remove();
                deferredTerrainChunkKeys.remove(chunkPos.toLong());
                checksThisPass--;
                chunkChecks--;
                if (level.hasChunk(chunkPos.x, chunkPos.z)) {
                    return new DeferredChunkPoll(chunkPos, chunkChecks);
                }

                deferTerrainChunk(chunkPos);
            }

            return new DeferredChunkPoll(null, chunkChecks);
        }

        private int processDeferredWater(int budget, int chunkChecks, long deadlineNanos) {
            while (budget > 0 && chunkChecks > 0 && !isPastDeadline(deadlineNanos)) {
                if (currentDeferredWaterCursor == null) {
                    var chunkPos = nextLoadedDeferredWaterChunk(chunkChecks);
                    chunkChecks = chunkPos.chunkChecksRemaining();
                    if (chunkPos.chunkPos() == null) {
                        return chunkChecks;
                    }

                    currentDeferredWaterChunk = chunkPos.chunkPos();
                    deferredWaterPassesRemaining = WATER_EVAPORATION_PASSES;
                    currentDeferredWaterCursor = new WaterEvaporationCursor(
                        level,
                        List.of(currentDeferredWaterChunk),
                        this::deferWaterChunk
                    );
                } else if (!level.hasChunk(currentDeferredWaterChunk.x, currentDeferredWaterChunk.z)) {
                    deferWaterChunk(currentDeferredWaterChunk);
                    currentDeferredWaterChunk = null;
                    currentDeferredWaterCursor = null;
                    deferredWaterPassesRemaining = 0;
                    continue;
                }

                budget = currentDeferredWaterCursor.process(level, waterShape, terrainShape, budget, deadlineNanos);
                evaporatedWaterBlocks += currentDeferredWaterCursor.evaporatedBlocks;
                currentDeferredWaterCursor.evaporatedBlocks = 0;

                if (!currentDeferredWaterCursor.finished) {
                    return chunkChecks;
                }

                deferredWaterPassesRemaining--;
                if (deferredWaterPassesRemaining > 0) {
                    currentDeferredWaterCursor = new WaterEvaporationCursor(
                        level,
                        List.of(currentDeferredWaterChunk),
                        this::deferWaterChunk
                    );
                    continue;
                }

                deferredWaterChunksCompleted++;
                currentDeferredWaterChunk = null;
                currentDeferredWaterCursor = null;
            }

            return chunkChecks;
        }

        private DeferredChunkPoll nextLoadedDeferredWaterChunk(int chunkChecks) {
            var checksThisPass = Math.min(chunkChecks, deferredWaterChunks.size());
            while (checksThisPass > 0) {
                var chunkPos = deferredWaterChunks.remove();
                deferredWaterChunkKeys.remove(chunkPos.toLong());
                checksThisPass--;
                chunkChecks--;
                if (level.hasChunk(chunkPos.x, chunkPos.z)) {
                    return new DeferredChunkPoll(chunkPos, chunkChecks);
                }

                deferWaterChunk(chunkPos);
            }

            return new DeferredChunkPoll(null, chunkChecks);
        }

        private int processDeferredBiomeConversions(int chunkChecks, long deadlineNanos) {
            var checksThisPass = Math.min(chunkChecks, deferredBiomeConversions.size());
            while (checksThisPass > 0 && !isPastDeadline(deadlineNanos)) {
                var chunkPos = deferredBiomeConversions.remove();
                deferredBiomeChunkKeys.remove(chunkPos.toLong());
                checksThisPass--;
                chunkChecks--;
                if (convertChunkBiome(level, chunkPos)) {
                    continue;
                }

                deferBiomeConversion(chunkPos);
            }

            return chunkChecks;
        }

        private void deferTerrainChunk(ChunkPos chunkPos) {
            if (deferredTerrainChunkKeys.add(chunkPos.toLong())) {
                deferredTerrainChunks.add(chunkPos);
            }
        }

        private void deferWaterChunk(ChunkPos chunkPos) {
            if (deferredWaterChunkKeys.add(chunkPos.toLong())) {
                deferredWaterChunks.add(chunkPos);
            }
        }

        private void deferBiomeConversion(ChunkPos chunkPos) {
            if (deferredBiomeChunkKeys.add(chunkPos.toLong())) {
                deferredBiomeConversions.add(chunkPos);
            }
        }

        private void evaporateWater(int budget, long deadlineNanos) {
            while (budget > 0 && waterEvaporationPassesRemaining > 0 && !isPastDeadline(deadlineNanos)) {
                if (waterEvaporationCursor.finished) {
                    waterEvaporationPassesRemaining--;
                    if (waterEvaporationPassesRemaining > 0) {
                        waterEvaporationCursor = new WaterEvaporationCursor(level, waterChunks, this::deferWaterChunk);
                    }
                    continue;
                }

                budget = waterEvaporationCursor.process(level, waterShape, terrainShape, budget, deadlineNanos);
                evaporatedWaterBlocks += waterEvaporationCursor.evaporatedBlocks;
                waterEvaporationCursor.evaporatedBlocks = 0;
            }
        }

        private void tickShockwave() {
            var shockSpeed = Math.max(6.0, profile.horizontalRadius() / 10.0);
            if (lastShockwaveRadius >= profile.horizontalRadius()) {
                return;
            }

            shockwaveTick++;
            var currentRadius = Math.min(profile.horizontalRadius(), shockwaveTick * shockSpeed);
            if (shockwaveTick % SHOCKWAVE_SCAN_INTERVAL_TICKS != 0 && currentRadius < profile.horizontalRadius()) {
                return;
            }

            applyShockwaveEntityEffects(
                level,
                center,
                source,
                profile,
                shockwaveEntities,
                currentRadius,
                lastShockwaveRadius
            );
            lastShockwaveRadius = currentRadius;
        }
    }

    private record ChunkCollection(
        List<ChunkPos> chunkPositions,
        int skippedUnloadedChunks
    ) {}

    private record DeferredChunkPoll(
        ChunkPos chunkPos,
        int chunkChecksRemaining
    ) {}

    private static ChunkCollection collectChunkPositions(ServerLevel level, BlastShape shape) {
        var chunks = new ArrayList<ChunkPos>();
        var skippedUnloadedChunks = 0;

        for (var chunkX = shape.minChunkX; chunkX <= shape.maxChunkX; chunkX++) {
            for (var chunkZ = shape.minChunkZ; chunkZ <= shape.maxChunkZ; chunkZ++) {
                if (!shape.intersectsChunk(chunkX, chunkZ)) {
                    continue;
                }

                if (!level.hasChunk(chunkX, chunkZ)) {
                    skippedUnloadedChunks++;
                }

                chunks.add(new ChunkPos(chunkX, chunkZ));
            }
        }

        chunks.sort(Comparator.comparingDouble(shape::distanceSquaredToChunkCenter));
        return new ChunkCollection(chunks, skippedUnloadedChunks);
    }

    private static final class BlastShape {

        private final double centerX;

        private final double centerY;

        private final double centerZ;

        private final double horizontalRadius;

        private final double upwardRadius;

        private final double downwardRadius;

        private final double invHorizontalRadiusSquared;

        private final double invUpwardRadiusSquared;

        private final double invDownwardRadiusSquared;

        private final int minBlockX;

        private final int maxBlockX;

        private final int minBlockY;

        private final int maxBlockY;

        private final int minBlockZ;

        private final int maxBlockZ;

        private final int minChunkX;

        private final int maxChunkX;

        private final int minChunkZ;

        private final int maxChunkZ;

        private BlastShape(Vec3 center, double horizontalRadius, double upwardRadius, double downwardRadius) {
            this.centerX = center.x;
            this.centerY = center.y;
            this.centerZ = center.z;
            this.horizontalRadius = horizontalRadius;
            this.upwardRadius = upwardRadius;
            this.downwardRadius = downwardRadius;
            this.invHorizontalRadiusSquared = 1.0D / square(horizontalRadius);
            this.invUpwardRadiusSquared = 1.0D / square(upwardRadius);
            this.invDownwardRadiusSquared = 1.0D / square(downwardRadius);
            this.minBlockX = Mth.floor(center.x - horizontalRadius - 1.0D);
            this.maxBlockX = Mth.floor(center.x + horizontalRadius + 1.0D);
            this.minBlockY = Mth.floor(center.y - downwardRadius - 1.0D);
            this.maxBlockY = Mth.floor(center.y + upwardRadius + 1.0D);
            this.minBlockZ = Mth.floor(center.z - horizontalRadius - 1.0D);
            this.maxBlockZ = Mth.floor(center.z + horizontalRadius + 1.0D);
            this.minChunkX = Math.floorDiv(minBlockX, 16);
            this.maxChunkX = Math.floorDiv(maxBlockX, 16);
            this.minChunkZ = Math.floorDiv(minBlockZ, 16);
            this.maxChunkZ = Math.floorDiv(maxBlockZ, 16);
        }

        private static BlastShape terrain(Vec3 center, NukeProfile profile) {
            return new BlastShape(center, profile.horizontalRadius(), profile.upwardRadius(), profile.downwardRadius());
        }

        private static BlastShape water(Vec3 center, NukeProfile profile) {
            return new BlastShape(
                center,
                profile.waterEvaporationRadius(),
                profile.upwardRadius() + 24.0D,
                profile.downwardRadius() + 24.0D
            );
        }

        private static BlastShape fallout(Vec3 center, NukeProfile profile) {
            return new BlastShape(
                center,
                profile.falloutRadius(),
                profile.upwardRadius(),
                profile.downwardRadius()
            );
        }

        private int minY(ServerLevel level) {
            return Mth.clamp(minBlockY, level.getMinBuildHeight(), level.getMaxBuildHeight() - 1);
        }

        private int maxY(ServerLevel level) {
            return Mth.clamp(maxBlockY, level.getMinBuildHeight(), level.getMaxBuildHeight() - 1);
        }

        private boolean intersectsChunk(int chunkX, int chunkZ) {
            return intersectsHorizontalAabb(
                chunkX << 4,
                (chunkX << 4) + 16.0D,
                chunkZ << 4,
                (chunkZ << 4) + 16.0D
            );
        }

        private boolean intersectsColumn(int x, int z) {
            return intersectsHorizontalAabb(x, x + 1.0D, z, z + 1.0D);
        }

        private boolean intersectsHorizontalAabb(double minX, double maxX, double minZ, double maxZ) {
            var dx = distanceToRange(centerX, minX, maxX);
            var dz = distanceToRange(centerZ, minZ, maxZ);
            return square(dx) + square(dz) <= square(horizontalRadius);
        }

        private boolean intersectsAabb(
            double minX,
            double maxX,
            double minY,
            double maxY,
            double minZ,
            double maxZ
        ) {
            var dx = distanceToRange(centerX, minX, maxX);
            var dy = distanceToRange(centerY, minY, maxY);
            var dz = distanceToRange(centerZ, minZ, maxZ);
            return normalized(dx, dy, dz) <= 1.0D;
        }

        private boolean touchesBlock(int x, int y, int z) {
            return intersectsAabb(x, x + 1.0D, y, y + 1.0D, z, z + 1.0D);
        }

        private double horizontalNormalizedToColumn(int x, int z) {
            var dx = distanceToRange(centerX, x, x + 1.0D);
            var dz = distanceToRange(centerZ, z, z + 1.0D);
            return (square(dx) + square(dz)) * invHorizontalRadiusSquared;
        }

        private double normalizedAtBlockCenter(int x, int y, int z) {
            return normalized(x + 0.5D - centerX, y + 0.5D - centerY, z + 0.5D - centerZ);
        }

        private double normalized(double dx, double dy, double dz) {
            var vertical = dy >= 0.0D ? invUpwardRadiusSquared : invDownwardRadiusSquared;
            return (square(dx) + square(dz)) * invHorizontalRadiusSquared + square(dy) * vertical;
        }

        private double distanceSquaredToChunkCenter(ChunkPos chunkPos) {
            return square(chunkPos.getMiddleBlockX() - centerX) + square(chunkPos.getMiddleBlockZ() - centerZ);
        }

        private static double distanceToRange(double value, double min, double max) {
            if (value < min) {
                return min - value;
            }
            if (value > max) {
                return max - value;
            }
            return 0.0D;
        }
    }

    @FunctionalInterface
    private interface SkippedChunkHandler {

        void accept(ChunkPos chunkPos);
    }

    private static final class WaterEvaporationCursor {

        private final List<ChunkPos> chunks;

        private final SkippedChunkHandler skippedChunkHandler;

        private final BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();

        private final BlockPos.MutableBlockPos neighborPos = new BlockPos.MutableBlockPos();

        private int chunkIndex;

        private ChunkPos chunkPos;

        private LevelChunk chunk;

        private LevelChunkSection[] sections;

        private int x;

        private int z;

        private int y;

        private int minY;

        private int maxY;

        private boolean columnReady;

        private boolean sectionReady;

        private boolean sectionContainsWork;

        private int sectionEndY;

        private boolean finished;

        private int evaporatedBlocks;

        private WaterEvaporationCursor(ServerLevel level, List<ChunkPos> chunks, SkippedChunkHandler skippedChunkHandler) {
            this.chunks = chunks;
            this.skippedChunkHandler = skippedChunkHandler;
            if (chunks.isEmpty()) {
                finished = true;
                return;
            }

            resetChunk(level, chunks.get(0));
        }

        private int process(
            ServerLevel level,
            BlastShape shape,
            BlastShape terrainShape,
            int budget,
            long deadlineNanos
        ) {
            var steps = 0;
            while (budget > 0 && !finished) {
                if (shouldYield(deadlineNanos, steps++)) {
                    break;
                }

                if (chunk == null || !level.hasChunk(chunkPos.x, chunkPos.z)) {
                    if (skippedChunkHandler != null) {
                        skippedChunkHandler.accept(chunkPos);
                    }
                    advanceChunk(level);
                    continue;
                }

                var maxX = chunkPos.getMaxBlockX();
                var maxZ = chunkPos.getMaxBlockZ();
                if (!columnReady) {
                    if (!prepareColumn(level, shape)) {
                        advanceColumn(level, chunkPos, maxX, maxZ);
                        continue;
                    }
                    columnReady = true;
                    sectionReady = false;
                    y = minY;
                }

                if (!prepareSection(shape, false)) {
                    y = sectionEndY;
                    finishColumnIfNeeded(level, maxX, maxZ);
                    continue;
                }

                if (shape.touchesBlock(x, y, z)) {
                    mutablePos.set(x, y, z);
                    if (evaporateFluidAt(level, mutablePos, terrainShape, neighborPos)) {
                        evaporatedBlocks++;
                    }
                    budget--;
                }

                y++;
                finishColumnIfNeeded(level, maxX, maxZ);
            }

            return budget;
        }

        private boolean prepareColumn(ServerLevel level, BlastShape shape) {
            if (!shape.intersectsColumn(x, z)) {
                return false;
            }

            var horizontal = shape.horizontalNormalizedToColumn(x, z);
            if (horizontal > 1.0) {
                return false;
            }

            var verticalAllowance = Math.sqrt(1.0 - horizontal);
            minY = Mth.clamp(
                Mth.floor(shape.centerY - shape.downwardRadius * verticalAllowance),
                shape.minY(level),
                shape.maxY(level)
            );
            maxY = Mth.clamp(
                Mth.ceil(shape.centerY + shape.upwardRadius * verticalAllowance),
                shape.minY(level),
                shape.maxY(level)
            );
            return minY <= maxY;
        }

        private boolean prepareSection(BlastShape shape, boolean terrain) {
            if (sectionReady && y < sectionEndY) {
                return sectionContainsWork;
            }

            sectionEndY = Math.min(maxY + 1, nextSectionStart(y));
            sectionReady = true;
            sectionContainsWork = false;

            if (!shape.intersectsAabb(x, x + 1.0D, y, sectionEndY, z, z + 1.0D)) {
                return false;
            }

            var sectionIndex = chunk.getSectionIndex(y);
            if (sectionIndex < 0 || sectionIndex >= sections.length) {
                return false;
            }

            var section = sections[sectionIndex];
            sectionContainsWork = terrain ? !section.hasOnlyAir() : section.maybeHas(HAS_FLUID_STATE);
            return sectionContainsWork;
        }

        private void advanceColumn(ServerLevel level, ChunkPos chunkPos, int maxX, int maxZ) {
            z++;
            if (z <= maxZ) {
                return;
            }

            z = chunkPos.getMinBlockZ();
            x++;
            if (x > maxX) {
                advanceChunk(level);
            }
        }

        private void advanceChunk(ServerLevel level) {
            chunkIndex++;
            columnReady = false;
            sectionReady = false;
            if (chunkIndex >= chunks.size()) {
                finished = true;
                return;
            }

            resetChunk(level, chunks.get(chunkIndex));
        }

        private void resetChunk(ServerLevel level, ChunkPos nextChunkPos) {
            chunkPos = nextChunkPos;
            if (level != null && level.hasChunk(nextChunkPos.x, nextChunkPos.z)) {
                chunk = level.getChunk(nextChunkPos.x, nextChunkPos.z);
                sections = chunk.getSections();
            } else {
                chunk = null;
                sections = null;
            }

            x = nextChunkPos.getMinBlockX();
            z = nextChunkPos.getMinBlockZ();
            columnReady = false;
            sectionReady = false;
        }

        private void finishColumnIfNeeded(ServerLevel level, int maxX, int maxZ) {
            if (y <= maxY) {
                return;
            }

            columnReady = false;
            sectionReady = false;
            advanceColumn(level, chunkPos, maxX, maxZ);
        }
    }

    private static final class ChunkCursor {

        private final ChunkPos chunkPos;

        private final LevelChunk chunk;

        private final LevelChunkSection[] sections;

        private final RandomSource random;

        private final BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();

        private final BlockPos.MutableBlockPos neighborPos = new BlockPos.MutableBlockPos();

        private int x;

        private int z;

        private int y;

        private int minY;

        private int maxY;

        private boolean columnReady;

        private boolean sectionReady;

        private boolean sectionContainsWork;

        private int sectionEndY;

        private boolean finished;

        private int changedBlocks;

        private ChunkCursor(ServerLevel level, ChunkPos chunkPos) {
            this.chunkPos = chunkPos;
            this.chunk = level.getChunk(chunkPos.x, chunkPos.z);
            this.sections = chunk.getSections();
            this.random = RandomSource.create(level.getSeed() ^ chunkPos.toLong());
            this.x = chunkPos.getMinBlockX();
            this.z = chunkPos.getMinBlockZ();
        }

        private int process(ServerLevel level, BlastShape shape, int budget, long deadlineNanos) {
            var maxX = chunkPos.getMaxBlockX();
            var maxZ = chunkPos.getMaxBlockZ();
            var steps = 0;

            while (budget > 0 && !finished) {
                if (shouldYield(deadlineNanos, steps++)) {
                    break;
                }

                if (!columnReady) {
                    if (!prepareColumn(level, shape)) {
                        advanceColumn(maxX, maxZ);
                        continue;
                    }
                    columnReady = true;
                    sectionReady = false;
                    y = minY;
                }

                if (!prepareSection(shape)) {
                    y = sectionEndY;
                    finishColumnIfNeeded(maxX, maxZ);
                    continue;
                }

                if (shape.touchesBlock(x, y, z)) {
                    mutablePos.set(x, y, z);
                    var normalized = shape.normalizedAtBlockCenter(x, y, z);
                    if (transformBlock(level, mutablePos, normalized, random, neighborPos)) {
                        changedBlocks++;
                    }
                    budget--;
                }

                y++;
                finishColumnIfNeeded(maxX, maxZ);
            }

            return budget;
        }

        private boolean prepareColumn(ServerLevel level, BlastShape shape) {
            if (!shape.intersectsColumn(x, z)) {
                return false;
            }

            var horizontal = shape.horizontalNormalizedToColumn(x, z);
            if (horizontal > 1.0) {
                return false;
            }

            var verticalAllowance = Math.sqrt(1.0 - horizontal);
            minY = Mth.clamp(
                Mth.floor(shape.centerY - shape.downwardRadius * verticalAllowance),
                shape.minY(level),
                shape.maxY(level)
            );
            maxY = Mth.clamp(
                Mth.ceil(shape.centerY + shape.upwardRadius * verticalAllowance),
                shape.minY(level),
                shape.maxY(level)
            );
            return minY <= maxY;
        }

        private boolean prepareSection(BlastShape shape) {
            if (sectionReady && y < sectionEndY) {
                return sectionContainsWork;
            }

            sectionEndY = Math.min(maxY + 1, nextSectionStart(y));
            sectionReady = true;
            sectionContainsWork = false;

            if (!shape.intersectsAabb(x, x + 1.0D, y, sectionEndY, z, z + 1.0D)) {
                return false;
            }

            var sectionIndex = chunk.getSectionIndex(y);
            if (sectionIndex < 0 || sectionIndex >= sections.length) {
                return false;
            }

            sectionContainsWork = !sections[sectionIndex].hasOnlyAir();
            return sectionContainsWork;
        }

        private void advanceColumn(int maxX, int maxZ) {
            z++;
            if (z <= maxZ) {
                return;
            }

            z = chunkPos.getMinBlockZ();
            x++;
            if (x > maxX) {
                finished = true;
            }
        }

        private void finishColumnIfNeeded(int maxX, int maxZ) {
            if (y <= maxY) {
                return;
            }

            columnReady = false;
            sectionReady = false;
            advanceColumn(maxX, maxZ);
        }
    }

    private static int nextSectionStart(int y) {
        return ((y >> 4) + 1) << 4;
    }

    private static boolean shouldYield(long deadlineNanos, int steps) {
        return (steps & TIME_CHECK_INTERVAL) == 0 && isPastDeadline(deadlineNanos);
    }

    private static long sliceDeadline(long deadlineNanos, int divisor) {
        var now = System.nanoTime();
        if (now >= deadlineNanos) {
            return now;
        }

        return Math.min(deadlineNanos, now + Math.max(500_000L, (deadlineNanos - now) / divisor));
    }

    private static boolean isPastDeadline(long deadlineNanos) {
        return System.nanoTime() >= deadlineNanos;
    }

    private static boolean transformBlock(
        ServerLevel level,
        BlockPos pos,
        double normalized,
        RandomSource random,
        BlockPos.MutableBlockPos neighborPos
    ) {
        var state = level.getBlockState(pos);
        if (state.isAir() || state.getDestroySpeed(level, pos) < 0.0F) {
            return false;
        }

        var replacement = chooseReplacement(level, pos, state, normalized, random);
        if (replacement == state) {
            return false;
        }

        if (!state.getFluidState().isEmpty() || !replacement.getFluidState().isEmpty()) {
            setFluidReplacement(level, pos, state, replacement, neighborPos);
        } else {
            level.setBlock(pos, replacement, BLOCK_UPDATE_FLAGS);
        }
        maybePlaceSurfaceAsh(level, pos, normalized, random);
        return true;
    }

    private static BlockState chooseReplacement(ServerLevel level, BlockPos pos, BlockState state, double normalized, RandomSource random) {
        var fluidState = state.getFluidState();
        if (fluidState.is(FluidTags.WATER)) {
            return chooseWaterReplacement(pos, state, normalized, random);
        }

        if (fluidState.is(FluidTags.LAVA)) {
            return normalized <= 0.78 ? Blocks.AIR.defaultBlockState() : Blocks.MAGMA_BLOCK.defaultBlockState();
        }

        if (normalized <= 0.75) {
            return Blocks.AIR.defaultBlockState();
        }

        if (normalized <= 0.98) {
            if (random.nextFloat() > normalized) {
                return Blocks.AIR.defaultBlockState();
            }

            var above = pos.above();
            var fire = Blocks.FIRE.defaultBlockState();
            if (level.isEmptyBlock(above) && fire.canSurvive(level, above)) {
                level.setBlock(above, fire, BLOCK_UPDATE_FLAGS);
            }
            return Blocks.AIR.defaultBlockState();
        }

        return chooseScorchedReplacement(state, random).defaultBlockState();
    }

    private static Block chooseScorchedReplacement(BlockState state, RandomSource random) {
        if (state.is(BlockTags.DIRT)) {
            return Blocks.BASALT;
        }

        if (state.is(BlockTags.SAND) || state.is(Blocks.SANDSTONE) || state.is(Blocks.RED_SANDSTONE)) {
            return random.nextInt(100) < 66 ? CoreBlocks.TRINITITE_BLOCK.get() : Blocks.MAGMA_BLOCK;
        }

        if (state.is(Blocks.ANDESITE)) {
            return Blocks.GRAVEL;
        }
        if (state.is(Blocks.DEEPSLATE)) {
            return Blocks.COBBLED_DEEPSLATE;
        }
        if (state.is(Blocks.DIORITE)) {
            return Blocks.SAND;
        }
        if (state.is(Blocks.GRANITE)) {
            return Blocks.RED_SAND;
        }
        if (state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.PODZOL) || state.is(Blocks.MYCELIUM)) {
            return Blocks.COARSE_DIRT;
        }
        if (state.is(Blocks.LAVA)) {
            return Blocks.MAGMA_BLOCK;
        }
        if (state.is(Blocks.STONE)) {
            return Blocks.BASALT;
        }

        return random.nextFloat() < 0.35F ? Blocks.BLACKSTONE : Blocks.BASALT;
    }

    private static boolean evaporateFluidAt(
        ServerLevel level,
        BlockPos pos,
        BlastShape terrainShape,
        BlockPos.MutableBlockPos neighborPos
    ) {
        var state = level.getBlockState(pos);
        var fluidState = state.getFluidState();
        if (fluidState.isEmpty()) {
            return false;
        }

        BlockState replacement;
        if (fluidState.is(FluidTags.WATER)) {
            replacement = chooseEvaporatedWaterReplacement(pos, state, terrainShape);
        } else if (fluidState.is(FluidTags.LAVA)) {
            replacement = Blocks.MAGMA_BLOCK.defaultBlockState();
        } else {
            return false;
        }

        if (replacement == state) {
            return false;
        }

        setFluidReplacement(level, pos, state, replacement, neighborPos);
        return true;
    }

    private static BlockState chooseWaterReplacement(
        BlockPos pos,
        BlockState state,
        double normalized,
        RandomSource random
    ) {
        if (isWaterloggedBlock(state)) {
            return dryWaterloggedState(state);
        }

        if (normalized >= WATER_RIM_SEAL_INNER_NORMALIZED) {
            return hotWaterSealState(pos, random);
        }

        return Blocks.AIR.defaultBlockState();
    }

    private static BlockState chooseEvaporatedWaterReplacement(BlockPos pos, BlockState state, BlastShape terrainShape) {
        if (isWaterloggedBlock(state)) {
            return dryWaterloggedState(state);
        }

        var normalized = terrainShape.normalizedAtBlockCenter(pos.getX(), pos.getY(), pos.getZ());
        if (normalized >= WATER_RIM_SEAL_INNER_NORMALIZED && normalized <= WATER_RIM_SEAL_OUTER_NORMALIZED) {
            return hotWaterSealState(pos, null);
        }

        return Blocks.AIR.defaultBlockState();
    }

    private static boolean isWaterloggedBlock(BlockState state) {
        return state.hasProperty(BlockStateProperties.WATERLOGGED)
            && Boolean.TRUE.equals(state.getValue(BlockStateProperties.WATERLOGGED));
    }

    private static BlockState hotWaterSealState(BlockPos pos, RandomSource random) {
        var magma = random == null ? (pos.asLong() & 7L) == 0L : random.nextInt(8) == 0;
        return (magma ? Blocks.MAGMA_BLOCK : Blocks.BASALT).defaultBlockState();
    }

    private static BlockState dryWaterloggedState(BlockState state) {
        if (isWaterloggedBlock(state)) {
            return state.setValue(BlockStateProperties.WATERLOGGED, false);
        }

        return Blocks.AIR.defaultBlockState();
    }

    private static void setFluidReplacement(
        ServerLevel level,
        BlockPos pos,
        BlockState oldState,
        BlockState replacement,
        BlockPos.MutableBlockPos neighborPos
    ) {
        if (replacement.isAir()) {
            level.setBlock(pos, replacement, FLUID_DRY_UPDATE_FLAGS);
            return;
        }

        level.setBlock(pos, replacement, FLUID_SEAL_UPDATE_FLAGS);
        level.updateNeighborsAt(pos, replacement.getBlock());
        scheduleFluidTick(level, pos, replacement);

        for (var direction : Direction.values()) {
            neighborPos.setWithOffset(pos, direction);
            var neighborState = level.getBlockState(neighborPos);
            if (neighborState.getFluidState().isEmpty()) {
                continue;
            }

            level.updateNeighborsAt(neighborPos, replacement.getBlock());
            scheduleFluidTick(level, neighborPos, neighborState);
        }
    }

    private static void scheduleFluidTick(ServerLevel level, BlockPos pos, BlockState state) {
        var fluidState = state.getFluidState();
        if (!fluidState.isEmpty()) {
            var fluid = fluidState.getType();
            level.scheduleTick(pos, fluid, fluid.getTickDelay(level));
        }
    }

    private static void maybePlaceSurfaceAsh(ServerLevel level, BlockPos pos, double normalized, RandomSource random) {
        if (normalized <= 0.98 || random.nextInt(10) >= 2) {
            return;
        }

        var above = pos.above();
        if (!level.isEmptyBlock(above)) {
            return;
        }

        var ash = CoreBlocks.ASH_BLOCK.get().defaultBlockState().setValue(SnowLayerBlock.LAYERS, 1);
        if (ash.canSurvive(level, above)) {
            level.setBlock(above, ash, BLOCK_UPDATE_FLAGS);
        }
    }

    private static boolean convertChunkBiome(ServerLevel level, ChunkPos chunkPos) {
        if (!level.hasChunk(chunkPos.x, chunkPos.z)) {
            return false;
        }

        var biome = level.registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(HumanBiomeKeys.NUKED_BIOME);
        var chunk = level.getChunk(chunkPos.x, chunkPos.z);
        setBiome(level, chunk, biome);
        level.getChunkSource().chunkMap.resendBiomesForChunks(List.of(chunk));
        return true;
    }

    private static void setBiome(ServerLevel level, LevelChunk chunk, Holder<Biome> holder) {
        chunk.fillBiomesFromNoise((i, j, k, sampler) -> {
            var blockX = QuartPos.toBlock(i);
            var blockY = QuartPos.toBlock(j);
            var blockZ = QuartPos.toBlock(k);
            return holder;
        }, level.getChunkSource().randomState().sampler());
        chunk.setUnsaved(true);
    }

    private static double square(double value) {
        return value * value;
    }
}
