package com.human.common.gameplay.item.gun;

import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Server-side accuracy state. A state is keyed by shooter and naturally resets after a firing pause. */
public final class GunAccuracyState {

    /**
     * ⚠⚠ THIS MAP LEAKED, AND IT LEAKED PER SHOOTER - NOT PER PLAYER.
     * <p>
     * It is keyed on {@link LivingEntity}, so every MARINE that fires gets an entry too, and marines die constantly. On
     * a busy server the growth came from them, not from the handful of players. Nothing ever removed an entry: no
     * clear, no removeIf, no lifecycle hook.
     * </p>
     * <p>
     * ⭐ DROPPING AN ENTRY IS SAFE AND ALWAYS WAS. {@code nextShot} uses {@code computeIfAbsent}, and {@code snapshot}
     * already handles {@code state == null} by falling back to zero bloom and shot index 0 - which is EXACTLY the state
     * a shooter recovers to after {@value #RECOVERY_WINDOW_TICKS} idle ticks. A missing entry and a fully recovered
     * entry are indistinguishable, so pruning changes no behaviour whatsoever.
     * </p>
     */
    private static final Map<UUID, State> STATES = new HashMap<>();

    /**
     * Past this many idle ticks a shooter's recoil has fully reset - see the {@code shotIndex} resets below, which
     * already used this figure as a literal in two places.
     */
    private static final int RECOVERY_WINDOW_TICKS = 12;

    /**
     * ⚠ SWEEP CADENCE, NOT A TIMER. The sweep runs on every 64th call to {@link #nextShot}, so it costs nothing when
     * nobody is shooting and cannot run at all on an idle server - no tick hook, no scheduler, no lifecycle wiring.
     * <p>
     * ⭐ A power of two so the modulo folds to a mask, and large enough that a full magazine dumped by one shooter
     * triggers at most one sweep.
     * </p>
     */
    private static final int SWEEP_INTERVAL_SHOTS = 64;

    /**
     * ⚠ Entries are kept well past recovery before being dropped. Pruning exactly at the recovery window would churn
     * the map for a shooter firing in bursts - allocating a fresh State every burst - for no memory benefit. Eight
     * times the window is still under a second of game time and removes anything genuinely finished.
     */
    private static final long STALE_AFTER_TICKS = RECOVERY_WINDOW_TICKS * 8L;

    private static int shotsSinceSweep;

    public static Shot nextShot(LivingEntity shooter, FireModeConfig fireModeConfig) {
        var profile = fireModeConfig.recoilProfile();
        var state = STATES.computeIfAbsent(shooter.getUUID(), ignored -> new State());
        var gameTime = shooter.level().getGameTime();

        sweepStaleStates(gameTime);
        var elapsed = Math.max(0L, gameTime - state.lastShotTick);

        state.spread = Math.max(0.0F, state.spread - (elapsed * profile.recoveryPerTick()));
        if (elapsed > RECOVERY_WINDOW_TICKS) {
            state.shotIndex = 0;
        }

        var aimed = shooter.isUsingItem();
        var multiplier = aimed ? profile.aimedMultiplier() : 1.0F;
        var movement = (float) Math.min(shooter.getDeltaMovement().horizontalDistance() * 5.0D, 0.65D);
        var spread = Math.min(profile.maximumSpread(), state.spread + movement) * multiplier;
        var yaw = (shooter.getRandom().nextFloat() - 0.5F) * spread;
        var pitch = (shooter.getRandom().nextFloat() - 0.5F) * spread;
        var direction = applySpread(shooter.getLookAngle(), yaw, pitch);

        var horizontalKick = profile.horizontalOffset(state.shotIndex) * multiplier;
        var verticalKick = profile.verticalKick() * multiplier;
        state.spread = Math.min(profile.maximumSpread(), state.spread + profile.spreadPerShot());
        state.lastShotTick = gameTime;
        state.shotIndex++;

        return new Shot(direction, verticalKick, horizontalKick, spread);
    }

    /**
     * Drops shooters that finished recovering long ago.
     * <p>
     * ⚠ Called from {@link #nextShot} only, and only every {@value #SWEEP_INTERVAL_SHOTS} shots. Sweeping on every shot
     * would walk the whole map inside the firing path, which is the one place that must stay cheap.
     * </p>
     * <p>
     * ⚠ {@code lastShotTick} starts at {@code Long.MIN_VALUE / 2}, so a State created and never fired would subtract to
     * a huge positive elapsed and be swept immediately. That cannot happen - the entry is only ever created one line
     * before its tick is stamped - but the guard below makes it impossible rather than merely unlikely.
     * </p>
     */
    private static void sweepStaleStates(long gameTime) {
        if (++shotsSinceSweep < SWEEP_INTERVAL_SHOTS) {
            return;
        }

        shotsSinceSweep = 0;

        STATES.entrySet().removeIf(entry -> {
            var lastShot = entry.getValue().lastShotTick;

            return lastShot > Long.MIN_VALUE / 2 && gameTime - lastShot > STALE_AFTER_TICKS;
        });
    }

    private static Vec3 applySpread(Vec3 direction, float yawDegrees, float pitchDegrees) {
        var yaw = (float) Math.atan2(-direction.x, direction.z) * Mth.RAD_TO_DEG + yawDegrees;
        var pitch = (float) -Math.asin(direction.y) * Mth.RAD_TO_DEG + pitchDegrees;
        return Vec3.directionFromRotation(pitch, yaw).normalize();
    }

    public static Vec3 scatter(Vec3 direction, float spreadDegrees, RandomSource random) {
        if (spreadDegrees <= 0.0F) {
            return direction;
        }

        return applySpread(
            direction,
            (random.nextFloat() - 0.5F) * spreadDegrees,
            (random.nextFloat() - 0.5F) * spreadDegrees
        );
    }

    public static Snapshot snapshot(LivingEntity shooter, FireModeConfig fireModeConfig) {
        var profile = fireModeConfig.recoilProfile();
        var state = STATES.get(shooter.getUUID());
        var gameTime = shooter.level().getGameTime();
        var elapsed = state == null ? Long.MAX_VALUE : Math.max(0L, gameTime - state.lastShotTick);
        var recoveredBloom = state == null
            ? 0.0F
            : Math.max(0.0F, state.spread - (elapsed * profile.recoveryPerTick()));
        var movementPenalty = (float) Math.min(shooter.getDeltaMovement().horizontalDistance() * 5.0D, 0.65D);
        var aiming = shooter.isUsingItem();
        var multiplier = aiming ? profile.aimedMultiplier() : 1.0F;
        var nextSpread = Math.min(profile.maximumSpread(), recoveredBloom + movementPenalty) * multiplier;
        var shotIndex = state == null || elapsed > RECOVERY_WINDOW_TICKS ? 0 : state.shotIndex;

        return new Snapshot(shotIndex, recoveredBloom, movementPenalty, nextSpread, aiming, multiplier);
    }

    public record Shot(
        Vec3 direction,
        float verticalKick,
        float horizontalKick,
        float spread
    ) {}

    public record Snapshot(
        int shotIndex,
        float bloom,
        float movementPenalty,
        float nextShotSpread,
        boolean aiming,
        float aimingMultiplier
    ) {}

    private static final class State {

        private long lastShotTick = Long.MIN_VALUE / 2;

        private int shotIndex;

        private float spread;
    }

    private GunAccuracyState() {}
}
