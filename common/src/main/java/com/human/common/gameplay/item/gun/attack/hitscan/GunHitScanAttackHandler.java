package com.human.common.gameplay.item.gun.attack.hitscan;

import com.human.common.gameplay.item.GunItem;
import com.human.common.gameplay.item.gun.attack.GunAttackConfig;
import com.human.common.gameplay.item.gun.attack.GunHitResult;
import com.human.common.network.packet.C2SGunHitResultsPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;

public class GunHitScanAttackHandler {

    public static void handle(C2SGunHitResultsPayload payload, LivingEntity shooter) {
        if (shooter == null || shooter.level().isClientSide) {
            // Shooter is null or level is client-side, nothing we can do beyond this point.
            return;
        }

        var level = (ServerLevel) shooter.level();
        var usedItemHand = shooter.getUsedItemHand();
        var itemStack = shooter.getItemInHand(usedItemHand);
        var item = itemStack.getItem();
        GunItem gunItem;

        if (item instanceof GunItem heldGunItem) {
            gunItem = heldGunItem;
        } else {
            itemStack = shooter.getItemInHand(InteractionHand.MAIN_HAND);
            item = itemStack.getItem();

            if (!(item instanceof GunItem mainHandGunItem)) {
                return;
            }

            gunItem = mainHandGunItem;
        }

        var gunConfig = gunItem.getGunConfig();

        var gunAttackConfig = new GunAttackConfig(gunConfig, gunConfig.getDefaultFireMode(), shooter, itemStack);

        for (var pierceIndex = 0; pierceIndex < payload.gunHitResults().size(); pierceIndex++) {
            var gunHitResult = payload.gunHitResults().get(pierceIndex);

            switch (gunHitResult) {
                case GunHitResult.Block result -> BlockGunHitResultHandler.handle(gunAttackConfig, result, pierceIndex);
                case GunHitResult.Entity result -> {
                    var entityUUID = result.entityUUID();
                    var entity = level.getEntity(entityUUID);

                    if (entity != null) {
                        EntityGunHitResultHandler.handle(gunAttackConfig, entity, pierceIndex);
                    }
                }
            }
        }
    }
}
