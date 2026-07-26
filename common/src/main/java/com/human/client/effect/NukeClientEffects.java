package com.human.client.effect;

import com.human.common.gameplay.entity.nuke.MushroomCloudEntity;
import com.human.common.network.packet.S2CNukeEffectPayload;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.Queue;

public class NukeClientEffects {

    private static final int FLASH_TICKS = 58;

    private static final int IMPACT_TICKS = 10;

    private static final int SHOCK_TICKS = 34;

    private static final int TREMOR_TICKS = 240;

    private static final int PRESSURE_WAVE_TICKS = 26;

    private static final int CLOUD_FALLBACK_DELAY_TICKS = 4;

    private static final int MAX_ACTIVE_SUPPLEMENTAL_EFFECTS = 3;

    private static final int MAX_SUPPLEMENTAL_PARTICLES_PER_TICK = 24;

    private static final int FIREBALL_TICKS = 32;

    private static final int DUST_WAVE_TICKS = 125;

    private static final int AFTERMATH_TICKS = 400;

    private static final Queue<PendingCloudVisual> PENDING_CLOUD_VISUALS = new ArrayDeque<>();

    private static final Queue<SupplementalBlastEffect> ACTIVE_SUPPLEMENTAL_EFFECTS = new ArrayDeque<>();

    private static int flashTicks;

    private static int impactTicks;

    private static int shockDelayTicks;

    private static int shockTicks;

    private static int tremorTicks;

    private static int pressureWaveTicks;

    private static float flashStrength;

    private static float impactStrength;

    private static float pendingShockStrength;

    private static float pendingRumbleStrength;

    private static float shockStrength;

    private static float tremorStrength;

    private static float pressureWaveStrength;

    private static long effectSeed;

    private static int nextLocalCloudId = -1_000_000;

    private NukeClientEffects() {
        throw new UnsupportedOperationException();
    }

    public static void trigger(S2CNukeEffectPayload payload) {
        var minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        if (player == null) {
            return;
        }

        var center = Vec3.atCenterOf(payload.center());
        var distance = player.position().distanceTo(center);
        var effectRange = Math.max(900.0D, payload.radius() * 9.0D);
        var falloff = (float) Mth.clamp(1.0D - distance / effectRange, 0.0D, 1.0D);
        var flashFalloff = 0.2F + falloff * 0.8F;
        var shakeFalloff = 0.08F + falloff * 0.92F;
        var shake = payload.shakeIntensity() * shakeFalloff;

        effectSeed++;
        PENDING_CLOUD_VISUALS.add(
            new PendingCloudVisual(payload.center(), payload.radius(), payload.durationTicks(), effectSeed, CLOUD_FALLBACK_DELAY_TICKS)
        );
        if (ACTIVE_SUPPLEMENTAL_EFFECTS.size() >= MAX_ACTIVE_SUPPLEMENTAL_EFFECTS) {
            ACTIVE_SUPPLEMENTAL_EFFECTS.remove();
        }
        ACTIVE_SUPPLEMENTAL_EFFECTS.add(
            new SupplementalBlastEffect(
                payload.center(),
                payload.radius(),
                payload.durationTicks(),
                effectSeed,
                Mth.clamp((int) (distance / 24.0D), 0, 70)
            )
        );
        spawnImmediateParticleKick(minecraft.level, center);

        flashTicks = Math.max(flashTicks, FLASH_TICKS);
        flashStrength = Math.max(flashStrength, payload.flashIntensity() * flashFalloff);

        if (shake <= 0.0F) {
            return;
        }

        var delay = Mth.clamp((int) (distance / 24.0D), 0, 70);
        impactTicks = Math.max(impactTicks, Math.max(3, (int) (IMPACT_TICKS * Mth.clamp(falloff * 1.4F, 0.35F, 1.0F))));
        impactStrength = Math.max(impactStrength, shake * 0.42F);
        pendingShockStrength = Math.max(pendingShockStrength, shake);
        pendingRumbleStrength = Math.max(
            pendingRumbleStrength,
            payload.shakeIntensity() * (0.08F + falloff * 0.18F)
        );
        tremorStrength = Math.max(tremorStrength, Math.max(shake * 0.34F, pendingRumbleStrength * 0.85F));

        if (delay <= 1) {
            beginShockwave();
        } else if (shockDelayTicks <= 0 || delay < shockDelayTicks) {
            shockDelayTicks = delay;
        }
    }

    public static void clientTick(Minecraft minecraft) {
        NukedBiomeFog.tick(minecraft);
        tickPendingCloudVisuals(minecraft);
        tickSupplementalEffects(minecraft);

        if (flashTicks > 0) {
            flashTicks--;
            if (flashTicks <= 0) {
                flashStrength = 0.0F;
            }
        }

        if (minecraft.player == null) {
            tickShakeTimersWithoutPlayer();
            return;
        }

        if (impactTicks > 0 && impactStrength > 0.0F) {
            var life = impactTicks / (float) IMPACT_TICKS;
            applyCameraShake(minecraft, impactStrength * life * life, 4.1F, 2.7F, 1.6F, 0.82F);
            impactTicks--;
            if (impactTicks <= 0) {
                impactStrength = 0.0F;
            }
        }

        if (shockDelayTicks > 0) {
            if (pendingRumbleStrength > 0.0F) {
                var pulse = 0.28F + Mth.sin(shockDelayTicks * 0.31F) * 0.08F;
                applyCameraShake(
                    minecraft,
                    pendingRumbleStrength * pulse,
                    0.18F,
                    0.37F,
                    0.55F,
                    0.32F
                );
            }
            shockDelayTicks--;
            if (shockDelayTicks == 0) {
                beginShockwave();
            }
        }

        if (pressureWaveTicks > 0) {
            pressureWaveTicks--;
            if (pressureWaveTicks <= 0) {
                pressureWaveStrength = 0.0F;
            }
        }

        if (shockTicks > 0 && shockStrength > 0.0F) {
            var life = shockTicks / (float) SHOCK_TICKS;
            var punch = easeOutCubic(life);
            applyCameraShake(minecraft, shockStrength * punch, 1.45F, 2.1F, 2.45F, 1.35F);
            shockTicks--;
            if (shockTicks <= 0) {
                shockStrength = 0.0F;
                tremorTicks = Math.max(tremorTicks, TREMOR_TICKS);
            }
            return;
        }

        if (tremorTicks > 0 && tremorStrength > 0.0F) {
            var life = tremorTicks / (float) TREMOR_TICKS;
            applyCameraShake(minecraft, tremorStrength * life * 0.72F, 0.46F, 0.9F, 0.9F, 0.52F);
            tremorTicks--;
            if (tremorTicks <= 0) {
                tremorStrength = 0.0F;
            }
        }
    }

    private static void tickPendingCloudVisuals(Minecraft minecraft) {
        var level = minecraft.level;
        if (level == null) {
            PENDING_CLOUD_VISUALS.clear();
            return;
        }
        if (PENDING_CLOUD_VISUALS.isEmpty()) {
            return;
        }

        var count = PENDING_CLOUD_VISUALS.size();
        for (var i = 0; i < count; i++) {
            var pending = PENDING_CLOUD_VISUALS.remove();
            pending.delayTicks--;
            if (pending.delayTicks > 0) {
                PENDING_CLOUD_VISUALS.add(pending);
                continue;
            }

            var visualBase = findVisualBase(level, Vec3.atCenterOf(pending.center));
            spawnLocalCloud(level, pending, visualBase);
        }
    }

    private static void spawnLocalCloud(ClientLevel level, PendingCloudVisual pending, Vec3 visualBase) {
        var cloud = new MushroomCloudEntity(level, visualBase.x(), visualBase.y(), visualBase.z());
        cloud.setId(nextLocalCloudId--);
        cloud.configure(pending.radius, pending.seed, pending.durationTicks);
        level.addEntity(cloud);
    }

    private static Vec3 findVisualBase(ClientLevel level, Vec3 center) {
        var x = Mth.floor(center.x);
        var z = Mth.floor(center.z);
        var surfaceY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        var y = Math.abs(center.y - surfaceY) > 4.0D ? surfaceY : center.y;
        return new Vec3(center.x, y, center.z);
    }

    private static void spawnImmediateParticleKick(ClientLevel level, Vec3 center) {
        if (level != null) {
            level.addParticle(ParticleTypes.FLASH, center.x, center.y, center.z, 0.0D, 0.0D, 0.0D);
        }
    }

    private static void tickSupplementalEffects(Minecraft minecraft) {
        var level = minecraft.level;
        if (level == null) {
            ACTIVE_SUPPLEMENTAL_EFFECTS.clear();
            return;
        }

        var remainingBudget = MAX_SUPPLEMENTAL_PARTICLES_PER_TICK;
        var effectsRemaining = ACTIVE_SUPPLEMENTAL_EFFECTS.size();
        var count = effectsRemaining;
        for (var i = 0; i < count; i++) {
            var effect = ACTIVE_SUPPLEMENTAL_EFFECTS.remove();
            var effectBudget = remainingBudget <= 0
                ? 0
                : effectsRemaining <= 1 ? remainingBudget : Math.max(1, remainingBudget / effectsRemaining);
            var used = effect.tick(minecraft, effectBudget);
            remainingBudget = Math.max(0, remainingBudget - used);
            effectsRemaining--;
            if (!effect.isComplete()) {
                ACTIVE_SUPPLEMENTAL_EFFECTS.add(effect);
            }
        }
    }

    public static void renderFlash(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        renderPressureWave(guiGraphics, deltaTracker);

        if (flashTicks <= 0 || flashStrength <= 0.0F) {
            return;
        }

        var minecraft = Minecraft.getInstance();
        var life = Mth.clamp((flashTicks + deltaTracker.getGameTimeDeltaPartialTick(false)) / FLASH_TICKS, 0.0F, 1.0F);
        var whiteAlpha = life * life * life * flashStrength;
        var packedWhiteAlpha = Mth.clamp((int) (whiteAlpha * 242.0F), 0, 242);
        guiGraphics.fill(
            0,
            0,
            minecraft.getWindow().getGuiScaledWidth(),
            minecraft.getWindow().getGuiScaledHeight(),
            (packedWhiteAlpha << 24) | 0xFFF8DD
        );

        var afterglow = Mth.sin((1.0F - life) * Mth.PI) * flashStrength * 0.14F;
        var packedAfterglowAlpha = Mth.clamp((int) (afterglow * 255.0F), 0, 48);
        if (packedAfterglowAlpha > 0) {
            guiGraphics.fill(
                0,
                0,
                minecraft.getWindow().getGuiScaledWidth(),
                minecraft.getWindow().getGuiScaledHeight(),
                (packedAfterglowAlpha << 24) | 0x26170E
            );
        }
    }

    private static void renderPressureWave(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        if (pressureWaveTicks <= 0 || pressureWaveStrength <= 0.0F) {
            return;
        }

        var minecraft = Minecraft.getInstance();
        var life = Mth.clamp(
            (pressureWaveTicks + deltaTracker.getGameTimeDeltaPartialTick(false)) / PRESSURE_WAVE_TICKS,
            0.0F,
            1.0F
        );
        var wave = Mth.sin((1.0F - life) * Mth.PI);
        var alpha = Mth.clamp((int) (wave * pressureWaveStrength * 44.0F), 0, 44);
        if (alpha > 0) {
            guiGraphics.fill(
                0,
                0,
                minecraft.getWindow().getGuiScaledWidth(),
                minecraft.getWindow().getGuiScaledHeight(),
                (alpha << 24) | 0x352C22
            );
        }
    }

    private static void beginShockwave() {
        shockTicks = Math.max(shockTicks, SHOCK_TICKS);
        shockStrength = Math.max(shockStrength, pendingShockStrength);
        tremorStrength = Math.max(tremorStrength, pendingRumbleStrength);
        pressureWaveTicks = Math.max(pressureWaveTicks, PRESSURE_WAVE_TICKS);
        pressureWaveStrength = Math.max(pressureWaveStrength, shockStrength);
        pendingShockStrength = 0.0F;
        pendingRumbleStrength = 0.0F;
    }

    private static void applyCameraShake(
        Minecraft minecraft,
        float strength,
        float lowFrequency,
        float highFrequency,
        float yawScale,
        float pitchScale
    ) {
        if (strength <= 0.0F || minecraft.player == null) {
            return;
        }

        var time = minecraft.level == null ? effectSeed : minecraft.level.getGameTime() + effectSeed * 19L;
        var low = Mth.sin(time * lowFrequency);
        var high = Mth.sin(time * highFrequency + 1.7F) * 0.38F;
        var yaw = (low + high) * strength * yawScale;
        var pitch = (Mth.cos(time * (lowFrequency * 1.17F) + 0.6F) + high * 0.55F) * strength * pitchScale;
        minecraft.player.turn(yaw, pitch);
    }

    private static void tickShakeTimersWithoutPlayer() {
        if (impactTicks > 0) {
            impactTicks--;
        }
        if (shockDelayTicks > 0) {
            shockDelayTicks--;
        }
        if (shockTicks > 0) {
            shockTicks--;
        }
        if (tremorTicks > 0) {
            tremorTicks--;
        }
        if (pressureWaveTicks > 0) {
            pressureWaveTicks--;
        }
    }

    private static float easeOutCubic(float value) {
        var inverse = 1.0F - Mth.clamp(value, 0.0F, 1.0F);
        return 1.0F - inverse * inverse * inverse;
    }

    private static final class PendingCloudVisual {

        private final BlockPos center;

        private final int radius;

        private final int durationTicks;

        private final long seed;

        private int delayTicks;

        private PendingCloudVisual(BlockPos center, int radius, int durationTicks, long seed, int delayTicks) {
            this.center = center;
            this.radius = radius;
            this.durationTicks = durationTicks;
            this.seed = seed;
            this.delayTicks = delayTicks;
        }
    }

    private static final class SupplementalBlastEffect {

        private final BlockPos center;

        private final int radius;

        private final int durationTicks;

        private final RandomSource random;

        private final int soundDelayTicks;

        private Vec3 visualBase;

        private int age;

        private boolean playedShockwaveSound;

        private boolean playedFirstEcho;

        private boolean playedSecondEcho;

        private boolean arrivalBurstPending;

        private SupplementalBlastEffect(BlockPos center, int radius, int durationTicks, long seed, int soundDelayTicks) {
            this.center = center;
            this.radius = Math.max(16, radius);
            this.durationTicks = Math.max(20, durationTicks);
            this.random = RandomSource.create(seed ^ center.asLong());
            this.soundDelayTicks = soundDelayTicks;
        }

        private int tick(Minecraft minecraft, int particleBudget) {
            var level = minecraft.level;
            var player = minecraft.player;
            if (level == null || player == null) {
                age++;
                return 0;
            }

            if (visualBase == null) {
                visualBase = findVisualBase(level, Vec3.atCenterOf(center));
            }

            var distance = player.position().distanceTo(visualBase);
            var effectRange = Math.max(900.0D, radius * 9.0D);
            var falloff = (float) Mth.clamp(1.0D - distance / effectRange, 0.0D, 1.0D);
            var intensity = 0.25F + falloff * 0.75F;

            if (!playedShockwaveSound && age >= soundDelayTicks) {
                playShockwaveSound(level, player, intensity);
                playedShockwaveSound = true;
                arrivalBurstPending = true;
            }
            if (!playedFirstEcho && age >= soundDelayTicks + 20) {
                playThunderEcho(level, player, intensity * 0.58F, 0.52F);
                playedFirstEcho = true;
            }
            if (!playedSecondEcho && age >= soundDelayTicks + 48) {
                playThunderEcho(level, player, intensity * 0.34F, 0.44F);
                playedSecondEcho = true;
            }

            var used = 0;
            if (arrivalBurstPending && particleBudget > used) {
                used += spawnPlayerShockGust(level, player, particleBudget - used, intensity);
                arrivalBurstPending = false;
            }
            if (age < FIREBALL_TICKS && particleBudget > used) {
                used += spawnFireball(level, particleBudget - used, intensity);
            }
            if (age < DUST_WAVE_TICKS && age % 2 == 0 && particleBudget > used) {
                used += spawnDustWave(level, particleBudget - used, intensity);
            }
            if (age >= 125 && age < 190 && age % 4 == 0 && particleBudget > used) {
                used += spawnBackwash(level, particleBudget - used, intensity);
            }
            if (
                age >= 90
                    && age < Math.min(durationTicks, AFTERMATH_TICKS)
                    && age % 3 == 0
                    && particleBudget > used
            ) {
                used += spawnAftermath(level, particleBudget - used, intensity);
            }

            age++;
            return used;
        }

        private int spawnFireball(ClientLevel level, int budget, float intensity) {
            var requested = Mth.clamp(Math.round((10.0F - age * 0.22F) * intensity), 1, 10);
            var count = Math.min(budget, requested);
            var spread = Math.max(3.0D, radius * (0.07D + age * 0.0015D));
            var height = Math.max(4.0D, radius * 0.12D);
            for (var i = 0; i < count; i++) {
                var angle = random.nextDouble() * Mth.TWO_PI;
                var radial = Math.sqrt(random.nextDouble()) * spread;
                var x = visualBase.x + Math.cos(angle) * radial;
                var y = visualBase.y + 1.0D + random.nextDouble() * height;
                var z = visualBase.z + Math.sin(angle) * radial;
                ParticleOptions particle = i % 7 == 0
                    ? ParticleTypes.EXPLOSION
                    : i % 2 == 0 ? ParticleTypes.FLAME : ParticleTypes.LARGE_SMOKE;
                level.addParticle(
                    particle,
                    x,
                    y,
                    z,
                    Math.cos(angle) * 0.05D,
                    0.08D + random.nextDouble() * 0.08D,
                    Math.sin(angle) * 0.05D
                );
            }
            return count;
        }

        private int spawnDustWave(ClientLevel level, int budget, float intensity) {
            var requested = Mth.clamp(Math.round(12.0F * intensity), 3, 12);
            var count = Math.min(budget, requested);
            var progress = easeOutCubic(age / (float) DUST_WAVE_TICKS);
            var waveRadius = radius * Mth.lerp(progress, 0.08D, 1.34D);
            var rotation = random.nextDouble() * Mth.TWO_PI;
            for (var i = 0; i < count; i++) {
                var angle = rotation + Mth.TWO_PI * i / count;
                var distance = waveRadius + random.nextGaussian() * Math.max(1.0D, radius * 0.025D);
                var x = visualBase.x + Math.cos(angle) * distance;
                var z = visualBase.z + Math.sin(angle) * distance;
                var y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Mth.floor(x), Mth.floor(z)) + 0.2D;
                var particle = i % 3 == 0 ? ParticleTypes.POOF : ParticleTypes.CLOUD;
                level.addParticle(
                    particle,
                    x,
                    y,
                    z,
                    Math.cos(angle) * 0.09D,
                    0.025D + random.nextDouble() * 0.035D,
                    Math.sin(angle) * 0.09D
                );
            }
            return count;
        }

        private int spawnPlayerShockGust(ClientLevel level, Player player, int budget, float intensity) {
            var count = Math.min(budget, Mth.clamp(Math.round(4.0F + intensity * 6.0F), 4, 10));
            var playerPos = player.position();
            var away = playerPos.subtract(visualBase).normalize();
            for (var i = 0; i < count; i++) {
                var angle = random.nextDouble() * Mth.TWO_PI;
                var distance = 1.0D + random.nextDouble() * 4.5D;
                var x = playerPos.x + Math.cos(angle) * distance;
                var y = playerPos.y + random.nextDouble() * 2.2D;
                var z = playerPos.z + Math.sin(angle) * distance;
                var particle = i % 3 == 0 ? ParticleTypes.POOF : ParticleTypes.CLOUD;
                level.addParticle(
                    particle,
                    x,
                    y,
                    z,
                    away.x * (0.16D + intensity * 0.12D),
                    0.035D + random.nextDouble() * 0.045D,
                    away.z * (0.16D + intensity * 0.12D)
                );
            }
            return count;
        }

        private int spawnBackwash(ClientLevel level, int budget, float intensity) {
            var count = Math.min(budget, Mth.clamp(Math.round(5.0F * intensity), 2, 5));
            for (var i = 0; i < count; i++) {
                var angle = random.nextDouble() * Mth.TWO_PI;
                var distance = radius * (0.28D + random.nextDouble() * 0.72D);
                var x = visualBase.x + Math.cos(angle) * distance;
                var z = visualBase.z + Math.sin(angle) * distance;
                var y = level.getHeight(
                    Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    Mth.floor(x),
                    Mth.floor(z)
                ) + 0.4D;
                var inwardSpeed = 0.035D + intensity * 0.035D;
                level.addParticle(
                    i % 2 == 0 ? ParticleTypes.LARGE_SMOKE : ParticleTypes.CLOUD,
                    x,
                    y,
                    z,
                    -Math.cos(angle) * inwardSpeed,
                    0.025D + random.nextDouble() * 0.04D,
                    -Math.sin(angle) * inwardSpeed
                );
            }
            return count;
        }

        private int spawnAftermath(ClientLevel level, int budget, float intensity) {
            var count = Math.min(budget, intensity >= 0.55F ? 3 : 2);
            for (var i = 0; i < count; i++) {
                var angle = random.nextDouble() * Mth.TWO_PI;
                var distance = Math.sqrt(random.nextDouble()) * radius * 0.82D;
                var x = visualBase.x + Math.cos(angle) * distance;
                var z = visualBase.z + Math.sin(angle) * distance;
                var surface = level.getHeight(
                    Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    Mth.floor(x),
                    Mth.floor(z)
                );
                var y = surface + 1.0D + random.nextDouble() * Math.max(5.0D, radius * 0.12D);
                var particle = i % 2 == 0 ? ParticleTypes.ASH : ParticleTypes.SMOKE;
                level.addParticle(particle, x, y, z, 0.0D, 0.015D, 0.0D);
            }
            return count;
        }

        private void playShockwaveSound(ClientLevel level, Player player, float intensity) {
            var volume = Mth.clamp(0.65F + intensity * 2.35F, 0.65F, 3.0F);
            level.playLocalSound(
                player,
                SoundEvents.GENERIC_EXPLODE.value(),
                SoundSource.WEATHER,
                volume,
                0.48F
            );
            level.playLocalSound(
                player,
                SoundEvents.LIGHTNING_BOLT_THUNDER,
                SoundSource.WEATHER,
                volume * 0.7F,
                0.62F
            );
        }

        private void playThunderEcho(ClientLevel level, Player player, float intensity, float pitch) {
            var volume = Mth.clamp(intensity * 1.8F, 0.25F, 1.8F);
            level.playLocalSound(
                player,
                SoundEvents.LIGHTNING_BOLT_THUNDER,
                SoundSource.WEATHER,
                volume,
                pitch
            );
        }

        private boolean isComplete() {
            return age >= Math.min(durationTicks, AFTERMATH_TICKS);
        }
    }
}
