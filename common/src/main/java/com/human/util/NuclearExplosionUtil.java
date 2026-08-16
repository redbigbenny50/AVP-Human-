package com.human.util;

import com.alien.common.gameplay.entity.living.alien.Alien;
import com.alien.common.model.alien.variant.AlienVariant;
import com.alien.common.util.AlienTransitionUtil;
import com.blib.api.common.explosion.v1.Explosion;
import com.blib.api.common.explosion.v1.ExplosionProgressTracker;
import com.blib.api.common.explosion.v1.ExplosionUtil;
import com.human.Human;
import com.human.common.gameplay.effect.RadiationLevel;
import com.human.common.gameplay.entity.nuke.MushroomCloudEntity;
import com.human.common.gameplay.explosion.nuke.NuclearCraterDrainer;
import com.human.common.gameplay.explosion.nuke.NuclearExplosionEffects;
import com.human.common.gameplay.explosion.nuke.NuclearExplosionSavedData;
import com.human.common.gameplay.explosion.nuke.NuclearFalloutSpreader;
import com.human.common.model.RadiationExposure;
import com.human.common.network.packet.S2CNukeEffectPayload;
import com.human.common.property.HumanProperties;
import com.human.common.property.HumanPropertyAccess;
import com.human.compatibility.avp_alien.AVPAlien;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class NuclearExplosionUtil {

    /** Horizontal crater radius, in chunks. The blast is spec'd as a five-chunk crater. */
    public static final int RADIUS_IN_CHUNKS = 5;

    /** Horizontal crater radius, in blocks. */
    public static final int RADIUS = 16 * RADIUS_IN_CHUNKS;

    /** Vertical crater radius above the centre, in blocks. */
    public static final int RADIUS_UP = RADIUS / 2;

    /** Vertical crater radius below the centre, in blocks. */
    public static final int RADIUS_DOWN = 16 * 2;

    /** Radius of the fallout biome, in chunks. The contaminated zone is far wider than the crater. */
    public static final int FALLOUT_RADIUS_IN_CHUNKS = 16;

    /**
     * How long one carving cycle may hold the server thread. The library's default is 10ms of a 50ms tick; this is 50%
     * more, so the crater finishes proportionally sooner on any machine that can sustain it. The processor still
     * measures each cycle and backs off on hardware that cannot, so this raises the ceiling, not the floor.
     */
    public static final int CYCLE_BUDGET_IN_MILLISECONDS = 15;

    /** Peak knockback applied at ground zero. */
    public static final int MAX_KNOCKBACK = 5;

    /** How long the mushroom cloud lives, client side. */
    public static final int CLOUD_DURATION_IN_TICKS = 20 * 45;

    /** Flash/shake reach as a multiple of the crater radius, with a floor so even a small crater is felt. */
    private static final double CLIENT_EFFECT_RANGE_SCALE = 9.0;

    private static final double MINIMUM_CLIENT_EFFECT_RANGE = 900.0;

    /** Nobody walks away from a detonation clean: the faintest dose still plants level I. */
    private static final int MINIMUM_FALLOUT_EXPOSURE = 1200;

    /** Dimensions whose unfinished explosions have already been picked up, for the server currently running. */
    private static final Set<ResourceKey<Level>> RESUMED_DIMENSIONS = new HashSet<>();

    private static MinecraftServer resumeGuardServer;

    public static Explosion createNuclearExplosion(ServerLevel level, Vec3 center, int radius, int maxKnockback) {
        return createNuclearExplosion(level, center, radius, maxKnockback, UUID.randomUUID());
    }

    /**
     * Builds the explosion under a known id, so its carving progress can be tracked across a world reload.
     * <p>
     * The id is what ties a snapshot in {@link NuclearExplosionSavedData} to the explosion that produced it. A resumed
     * explosion keeps its original id rather than taking a new one, so the record is updated in place instead of
     * accumulating a fresh entry every time the world is reloaded.
     */
    private static Explosion createNuclearExplosion(
        ServerLevel level,
        Vec3 center,
        int radius,
        int maxKnockback,
        UUID explosionId
    ) {
        var progressTracker = new ExplosionProgressTracker();
        var nuclearExplosionEffects = new NuclearExplosionEffects();
        // The callbacks need the Explosion in order to snapshot it, and it does not exist until build() returns.
        // Nothing here runs before then, so a one-slot holder filled straight afterwards is enough.
        var explosionHolder = new Explosion[1];
        var timerStarted = new boolean[1];

        var explosion = Explosion.builder(level, center)
            .withMaxMillisecondsPerCycle(CYCLE_BUDGET_IN_MILLISECONDS)
            .withRadius(Direction.Plane.HORIZONTAL, radius)
            .withRadius(Direction.UP, radius / 2)
            .withRadius(Direction.DOWN, RADIUS_DOWN)
            .onExplosionStart(() -> {
                NuclearFalloutSpreader.spread(level, BlockPos.containing(center), FALLOUT_RADIUS_IN_CHUNKS);

                var entities = ExplosionUtil.getEntitiesInRadius(level, center, radius);

                for (var entity : entities) {
                    var distance = entity.distanceToSqr(center);
                    var damage = ExplosionUtil.computeDamage(radius, 5, 1000, distance);

                    if (AVPAlien.MOD.isLoaded() && entity instanceof Alien alien) {
                        // Aberrants are the one strain radiation can actually hurt, so the blast kills them outright
                        // rather than transmuting them. Routed through hurt() so their loot tables still fire.
                        if (alien.getVariant() == AlienVariant.ABERRANT) {
                            alien.hurt(level.damageSources().explosion(null), Float.MAX_VALUE);

                            continue;
                        }

                        var transition = AlienTransitionUtil.transitionIntoVariant(alien, AlienVariant.IRRADIATED);

                        // A successful transmute DISCARDS this entity and spawns a replacement, so everything below
                        // would be applied to a corpse. Skip it rather than silently burning, damaging and knocking
                        // back something that no longer exists.
                        if (transition instanceof AlienTransitionUtil.AlienTransitionResult.Result) {
                            continue;
                        }
                    }

                    applyFalloutDose(entity, distance, radius);

                    entity.igniteForSeconds(15);
                    entity.hurt(level.damageSources().explosion(null), (float) damage);
                    ExplosionUtil.applyKnockback(center, radius, entity, maxKnockback, distance);
                }
                spawnMushroomCloud(level, center, radius);
                broadcastClientEffects(level, center, radius);
            })
            .onBlockSample(($, pos) -> {
                nuclearExplosionEffects.apply($, pos);
                progressTracker.incrementBlockDestroyCounter();
            })
            .onCycleStart(() -> {
                // Started on the first CYCLE rather than in onExplosionStart, because a resumed explosion never
                // fires the start callback - and an unstarted timer would report the whole Unix epoch.
                if (!timerStarted[0]) {
                    timerStarted[0] = true;

                    progressTracker.startTimer();
                }

                nuclearExplosionEffects.beginCycle(level);
            })
            .onCycleFinish(sampledPositions -> {
                nuclearExplosionEffects.flushParticles(level);
                NuclearExplosionSavedData.get(level)
                    .record(explosionId, center, radius, maxKnockback, explosionHolder[0].saveState());
            })
            .onExplosionFinish(() -> {
                NuclearExplosionSavedData.get(level).forget(explosionId);
                NuclearCraterDrainer.drain(level, BlockPos.containing(center), radius, RADIUS_DOWN);

                progressTracker.stopTimer();

                var timeTakenInMillis = progressTracker.timeTaken();
                var timeTakenInTicks = timeTakenInMillis / 50;

                Human.LOGGER.info(
                    "Explosion @ {} completed in {}ms ({} ticks), destroying {} blocks!",
                    center,
                    timeTakenInMillis,
                    timeTakenInTicks,
                    progressTracker.blocksDestroyed()
                );
            })
            .build();

        explosionHolder[0] = explosion;

        return explosion;
    }

    /**
     * Picks up any explosion that was still carving when this dimension was last unloaded.
     * <p>
     * Hung off the level tick because there is no level-load event to hang it off, and guarded so it runs once per
     * dimension per server. The guard is a set rather than a flag so that leaving one world and loading another in the
     * same session still resumes.
     */
    public static void resumeUnfinishedExplosions(Level level) {
        if (level.isClientSide || !(level instanceof ServerLevel serverLevel)) {
            return;
        }

        var server = serverLevel.getServer();

        if (server != resumeGuardServer) {
            resumeGuardServer = server;

            RESUMED_DIMENSIONS.clear();
        }

        if (!RESUMED_DIMENSIONS.add(serverLevel.dimension())) {
            return;
        }

        var savedData = NuclearExplosionSavedData.get(serverLevel);

        for (var pendingExplosion : savedData.pendingExplosions()) {
            try {
                // The fallout biome is the one piece of the start callback worth repeating. Writing a biome twice
                // is a no-op, and the original spread may itself have been cut short by the same unload that
                // stopped the carve. Everything else in that callback - the entity pass, the dose, the mushroom
                // cloud, the flash - is one-off, and Explosion.resume deliberately never fires it.
                NuclearFalloutSpreader.spread(
                    serverLevel,
                    BlockPos.containing(pendingExplosion.center()),
                    FALLOUT_RADIUS_IN_CHUNKS
                );

                createNuclearExplosion(
                    serverLevel,
                    pendingExplosion.center(),
                    pendingExplosion.radius(),
                    pendingExplosion.maxKnockback(),
                    pendingExplosion.id()
                ).resume(pendingExplosion.cursorState());
            } catch (RuntimeException exception) {
                // A snapshot that no longer fits the explosion it describes - most likely because the crater
                // geometry changed between sessions - is discarded rather than retried, so it cannot throw on
                // every load forever.
                Human.LOGGER.error(
                    "Could not resume nuclear explosion {} in {}; discarding it.",
                    pendingExplosion.id(),
                    serverLevel.dimension().location(),
                    exception
                );

                savedData.forget(pendingExplosion.id());
            }
        }
    }

    /**
     * The procedural mushroom cloud.
     * <p>
     * Replaces the animated MODEL (MushroomCloudAnimator / MushroomCloudAnimDispatcher, both now deleted) with a
     * client-rendered cloudlet field. The entity carries only radius, seed and duration, and the renderer derives every
     * cloudlet from that seed - so the whole visual costs THREE SYNCHED VALUES on the wire rather than an animated
     * entity streaming pose updates to everyone in range.
     * </p>
     * <p>
     * Placed on the surface rather than at the blast centre. A nuke detonating in a hole would otherwise bury its own
     * cloud; the fixed {@code -23} offset the model version used made exactly that mistake.
     * </p>
     */
    private static void spawnMushroomCloud(ServerLevel level, Vec3 center, int radius) {
        var surfaceY = level.getHeight(
            Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
            Mth.floor(center.x()),
            Mth.floor(center.z())
        );
        var y = Math.abs(center.y() - surfaceY) > 4.0 ? surfaceY : center.y();
        var mushroomCloud = new MushroomCloudEntity(level, center.x(), y, center.z());

        mushroomCloud.configure(radius, level.getRandom().nextLong(), CLOUD_DURATION_IN_TICKS);
        level.addFreshEntity(mushroomCloud);
    }

    /**
     * Screen flash and shake, sent ONCE per detonation to players in range.
     * <p>
     * This fires from {@code onExplosionStart}, so it is a single packet per nearby player for the entire event - not
     * per cycle, and emphatically not per block. Intensities are config driven; a player who sets either to zero still
     * receives the packet and simply renders nothing, which keeps the choice client side where it belongs.
     * </p>
     */
    private static void broadcastClientEffects(ServerLevel level, Vec3 center, int radius) {
        var payload = new S2CNukeEffectPayload(
            BlockPos.containing(center),
            radius,
            Mth.clamp(HumanPropertyAccess.INSTANCE.get(HumanProperties.Blocks.Nuke.SCREEN_FLASH_INTENSITY), 0.0F, 1.0F),
            Mth.clamp(HumanPropertyAccess.INSTANCE.get(HumanProperties.Blocks.Nuke.SCREEN_SHAKE_INTENSITY), 0.0F, 1.0F),
            CLOUD_DURATION_IN_TICKS
        );
        var range = Math.max(MINIMUM_CLIENT_EFFECT_RANGE, radius * CLIENT_EFFECT_RANGE_SCALE);
        var rangeSqr = range * range;

        for (var player : level.players()) {
            if (player.distanceToSqr(center) <= rangeSqr) {
                Human.MOD.networking().sendToClient(player, payload);
            }
        }
    }

    /**
     * The blast's radiation dose, written for the exposure-counter system rather than as a fixed effect.
     * <p>
     * A nuke is the one source that contaminates INSTANTLY instead of accumulating: it adds exposure outright, scaled
     * by how close the victim was, so ground zero lands at {@link RadiationLevel#FATAL} and the fringe at a survivable
     * warning. Falloff uses the SQUARED distance the blast loop already has, which is both free and physically apt -
     * intensity drops with the square of range, giving a broad lethal core and a sharp taper rather than a linear
     * gradient.
     * </p>
     * <p>
     * Roughly: ground zero {@literal ->} level V, half the radius {@literal ->} level IV, seven tenths {@literal ->}
     * level III, nine tenths {@literal ->} level I. Anyone caught inside the radius at all is contaminated to at least
     * level I; walking away from a nuclear detonation completely clean is not a thing.
     * </p>
     * <p>
     * Exposure is ADDED, so a second blast compounds on an already-contaminated victim (clamped at the ceiling), and
     * the ordinary 90-seconds-per-level decay is what carries survivors back down. {@code canBeIrradiated} still gates
     * it, which means a sealed hazard suit turns the fallout aside entirely - that is the whole point of owning one -
     * and radiation-immune entities such as xenomorphs shrug it off.
     * </p>
     */
    private static void applyFalloutDose(Entity entity, double distanceSquared, int radius) {
        if (!(entity instanceof LivingEntity livingEntity) || !HumanPredicates.canBeIrradiated(livingEntity)) {
            return;
        }

        var radiusSquared = (double) radius * radius;
        var intensity = Mth.clamp(1.0 - distanceSquared / radiusSquared, 0.0, 1.0);
        var dose = (int) Math.round(RadiationLevel.MAX_EXPOSURE * intensity);

        ((RadiationExposure) livingEntity).avp_human$addRadiationExposure(
            Math.max(MINIMUM_FALLOUT_EXPOSURE, dose)
        );
    }
}
