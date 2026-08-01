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

    private static final Map<UUID, State> STATES = new HashMap<>();

    public static Shot nextShot(LivingEntity shooter, FireModeConfig fireModeConfig) {
        var profile = fireModeConfig.recoilProfile();
        var state = STATES.computeIfAbsent(shooter.getUUID(), ignored -> new State());
        var gameTime = shooter.level().getGameTime();
        var elapsed = Math.max(0L, gameTime - state.lastShotTick);

        state.spread = Math.max(0.0F, state.spread - (elapsed * profile.recoveryPerTick()));
        if (elapsed > 12) {
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
        var shotIndex = state == null || elapsed > 12 ? 0 : state.shotIndex;

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
