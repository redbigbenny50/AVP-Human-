package com.human.common.gameplay.item.gun.debug;

import com.human.common.gameplay.item.GunItem;
import com.human.common.gameplay.item.gun.GunAccuracyState;
import com.human.common.gameplay.item.gun.attack.GunAttackConfig;
import com.human.common.gameplay.item.gun.attack.GunHitResult;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public class BulletTrajectoryDebug {

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

        player.sendSystemMessage(
            Component.literal(
                "Bullet debug: origin=" + format(origin)
                    + " dir=" + format(direction)
                    + " range=" + maxDistance
                    + " traced=" + format(lineDistance)
                    + " pellets=" + gunAttackConfig.fireModeConfig().pelletCount()
                    + " pelletSpread=" + format(gunAttackConfig.fireModeConfig().pelletSpreadDegrees())
                    + " falloffStart=" + format(
                        gunAttackConfig.fireModeConfig().damageFalloffStartFraction() * gunAttackConfig.fireModeConfig().range()
                    )
                    + " recoil=" + format(gunAttackConfig.fireModeConfig().recoilProfile().verticalKick())
                    + " maxSpread=" + format(gunAttackConfig.fireModeConfig().recoilProfile().maximumSpread())
                    + " pierces=" + totalPierces + "/" + piercingBudget
                    + " hits=" + hitResults.size()
            )
        );
    }

    public static void sendHeldGunInfo(ServerPlayer player, GunItem gunItem) {
        var fireMode = gunItem.getGunConfig().getDefaultFireMode();

        player.sendSystemMessage(
            Component.literal(
                "Held gun debug: range=" + fireMode.range()
                    + " recoil=" + format(fireMode.recoil())
                    + " pellets=" + fireMode.pelletCount()
                    + " pelletSpread=" + format(fireMode.pelletSpreadDegrees())
                    + " falloff=" + format(fireMode.damageFalloffStartFraction() * fireMode.range()) + "-" + format(fireMode.range())
                    + " minDamage=" + format(fireMode.minimumDamageMultiplier() * 100.0F) + "%"
                    + " maxSpread=" + format(fireMode.recoilProfile().maximumSpread())
                    + " damage=" + format(fireMode.damage())
                    + " cooldownTicks=" + fireMode.cooldownInTicks()
                    + " spreadPerShot=" + format(fireMode.recoilProfile().spreadPerShot())
                    + " recovery=" + format(fireMode.recoilProfile().recoveryPerTick()) + "/tick"
            )
        );
    }

    public static void sendAccuracyState(ServerPlayer player, GunItem gunItem) {
        var fireMode = gunItem.getGunConfig().getDefaultFireMode();
        var state = GunAccuracyState.snapshot(player, fireMode);
        player.sendSystemMessage(
            Component.literal(
                "Gun state: shot=" + state.shotIndex()
                    + " bloom=" + format(state.bloom())
                    + " movement=" + format(state.movementPenalty())
                    + " nextSpread=" + format(state.nextShotSpread())
                    + " aiming=" + state.aiming()
            )
        );
    }

    public static void sendSpreadInfo(ServerPlayer player, GunItem gunItem) {
        var profile = gunItem.getGunConfig().getDefaultFireMode().recoilProfile();
        player.sendSystemMessage(
            Component.literal(
                "Spread: perShot=" + format(profile.spreadPerShot())
                    + " max=" + format(profile.maximumSpread())
                    + " recovery=" + format(profile.recoveryPerTick()) + "/tick"
                    + " aimedMultiplier=" + format(profile.aimedMultiplier())
            )
        );
    }

    public static void sendRecoilInfo(ServerPlayer player, GunItem gunItem) {
        var profile = gunItem.getGunConfig().getDefaultFireMode().recoilProfile();
        var pattern = new StringBuilder();
        for (var index = 0; index < profile.horizontalPattern().length; index++) {
            if (index > 0) {
                pattern.append(", ");
            }
            pattern.append(format(profile.horizontalPattern()[index] * profile.horizontalKick()));
        }
        player.sendSystemMessage(
            Component.literal(
                "Recoil: vertical=" + format(profile.verticalKick())
                    + " horizontal=" + format(profile.horizontalKick())
                    + " pattern=[" + pattern + "]"
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
