package com.human.common.gameplay.item.gun.attack.projectile;

import com.human.Human;
import com.human.common.gameplay.entity.projectile.Rocket;
import com.human.common.gameplay.item.gun.attack.GunAttackAction;
import com.human.common.gameplay.item.gun.attack.GunAttackConfig;
import com.human.common.gameplay.item.gun.pipeline.GunShootResult;
import com.human.common.network.packet.S2CGunRecoilPayload;
import net.minecraft.server.level.ServerPlayer;

public class RocketProjectileGunAttackAction implements GunAttackAction {

    public static final RocketProjectileGunAttackAction INSTANCE = new RocketProjectileGunAttackAction();

    private RocketProjectileGunAttackAction() {}

    @Override
    public GunShootResult shoot(GunAttackConfig gunAttackConfig) {
        var shooter = gunAttackConfig.shooter();
        var level = shooter.level();

        if (level.isClientSide) {
            return GunShootResult.SHOT;
        }

        var rocket = new Rocket(level, shooter);
        rocket.shootFromRotation(shooter, shooter.getXRot(), shooter.getYRot(), 0.0F, 1.5F, 1.0F);

        if (shooter instanceof ServerPlayer serverPlayer) {
            Human.MOD.networking().sendToClient(serverPlayer, new S2CGunRecoilPayload(gunAttackConfig.fireModeConfig().recoil(), 0.0F));
        }

        level.addFreshEntity(rocket);

        return GunShootResult.SHOT;
    }
}
