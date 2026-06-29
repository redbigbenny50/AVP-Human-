package com.human.common.gameplay.item.gun.debug;

import com.human.common.gameplay.item.GunItem;
import com.human.common.gameplay.item.gun.attack.GunAttackConfig;
import com.human.common.gameplay.item.gun.attack.GunHitResult;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public class BulletTrajectoryDebug {

    private static final double PARTICLE_STEP = 2.0;

    private static final int MAX_TRAIL_PARTICLES = 48;

    private static final Set<UUID> ENABLED_PLAYERS = new HashSet<>();

    public static boolean toggle(ServerPlayer player) {
        if (ENABLED_PLAYERS.remove(player.getUUID())) {
            return false;
        }

        ENABLED_PLAYERS.add(player.getUUID());
        return true;
    }

    public static void setEnabled(ServerPlayer player, boolean enabled) {
        if (enabled) {
            ENABLED_PLAYERS.add(player.getUUID());
        } else {
            ENABLED_PLAYERS.remove(player.getUUID());
        }
    }

    public static void renderShot(
        GunAttackConfig gunAttackConfig,
        Vec3 origin,
        Vec3 direction,
        double distanceTraveled,
        int totalPierces,
        int piercingBudget,
        List<GunHitResult> hitResults
    ) {
        if (!(gunAttackConfig.shooter() instanceof ServerPlayer player) || !ENABLED_PLAYERS.contains(player.getUUID())) {
            return;
        }

        var maxDistance = gunAttackConfig.fireModeConfig().range();
        var lineDistance = Math.min(distanceTraveled, maxDistance);
        var particleStep = Math.max(PARTICLE_STEP, lineDistance / MAX_TRAIL_PARTICLES);

        for (double distance = 0.0; distance <= lineDistance; distance += particleStep) {
            var point = origin.add(direction.scale(distance));
            sendParticle(player, ParticleTypes.END_ROD, point, 1, 0.0F, 0.0F, 0.0F, 0.0F);
        }

        for (var hitResult : hitResults) {
            switch (hitResult) {
                case GunHitResult.Block block -> {
                    var center = block.blockPos().getCenter();
                    sendParticle(player, ParticleTypes.FLAME, center, 8, 0.15F, 0.15F, 0.15F, 0.0F);
                }
                case GunHitResult.Entity entityHit -> {
                    var entity = player.serverLevel().getEntity(entityHit.entityUUID());

                    if (entity != null) {
                        sendParticle(
                            player,
                            ParticleTypes.CRIT,
                            new Vec3(entity.getX(), entity.getEyeY(), entity.getZ()),
                            8,
                            0.2F,
                            0.2F,
                            0.2F,
                            0.0F
                        );
                    }
                }
            }
        }

        player.sendSystemMessage(
            Component.literal(
                "Bullet debug: origin=" + format(origin)
                    + " dir=" + format(direction)
                    + " range=" + maxDistance
                    + " traced=" + format(lineDistance)
                    + " recoil=" + format(gunAttackConfig.fireModeConfig().recoil())
                    + " accuracy=100%/no spread"
                    + " pierces=" + totalPierces + "/" + piercingBudget
                    + " hits=" + hitResults.size()
            )
        );
    }

    private static void sendParticle(
        ServerPlayer player,
        net.minecraft.core.particles.ParticleOptions particle,
        Vec3 point,
        int count,
        float xDist,
        float yDist,
        float zDist,
        float maxSpeed
    ) {
        player.connection.send(
            new ClientboundLevelParticlesPacket(
                particle,
                false,
                point.x,
                point.y,
                point.z,
                xDist,
                yDist,
                zDist,
                maxSpeed,
                count
            )
        );
    }

    public static void sendHeldGunInfo(ServerPlayer player, GunItem gunItem) {
        var fireMode = gunItem.getGunConfig().getDefaultFireMode();

        player.sendSystemMessage(
            Component.literal(
                "Held gun debug: range=" + fireMode.range()
                    + " recoil=" + format(fireMode.recoil())
                    + " damage=" + format(fireMode.damage())
                    + " cooldownTicks=" + fireMode.cooldownInTicks()
                    + " accuracy=100%/no spread"
            )
        );
    }

    private static String format(Vec3 vec3) {
        return "(" + format(vec3.x) + ", " + format(vec3.y) + ", " + format(vec3.z) + ")";
    }

    private static String format(double value) {
        return String.format(Locale.ROOT, "%.3f", value);
    }

    private BulletTrajectoryDebug() {
        throw new UnsupportedOperationException();
    }
}
