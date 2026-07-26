package com.human.common.gameplay.item.gun.attack.hitscan;

import com.alien.common.gameplay.entity.living.alien.xenomorph.Xenomorph;
import com.blib.api.common.dismemberment.v1.Dismemberable;
import com.blib.api.common.dismemberment.v1.LimbCategories;
import com.blib.api.common.dismemberment.v1.LimbDefinition;
import com.blib.api.common.dismemberment.v1.LimbDismemberer;
import com.human.common.gameplay.item.gun.attack.GunAttackConfig;
import com.human.common.registry.init.item.HumanGunItems;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

final class GunLimbDamage {

    static Result apply(
        GunAttackConfig config,
        LivingEntity target,
        LimbDefinition limb,
        float actualDamage,
        double distance,
        Vec3 bulletDirection
    ) {
        if (!(target instanceof Dismemberable dismemberable) || actualDamage <= 0.0F) {
            return Result.notApplied("target did not take health damage");
        }

        var thresholdFraction = thresholdFraction(limb);
        if (thresholdFraction <= 0.0F) {
            return Result.notApplied("unsupported limb category");
        }
        if (limb.category().equals(LimbCategories.HEAD) && !canDamageHead(config, distance)) {
            return Result.notApplied("head durability rejected: requires M42A3 or shotgun pellet within 8 blocks");
        }
        if (
            limb.category().equals(LimbCategories.LEG)
                && target instanceof Xenomorph xenomorph
                && !xenomorph.getCrawlingManager().canCrawl()
        ) {
            return Result.notApplied("leg durability rejected: this xenomorph cannot crawl");
        }

        var manager = dismemberable.getDismembermentManager();
        var before = manager.getLimbDamage(limb.id());
        var after = manager.addLimbDamage(limb.id(), actualDamage);
        var threshold = target.getMaxHealth() * thresholdFraction;
        var detached = false;
        if (after >= threshold) {
            detached = LimbDismemberer.detach(
                target,
                limb.id(),
                piece -> piece.launch(bulletDirection.normalize().scale(0.48D).add(0.0D, 0.18D, 0.0D))
            ).isPresent();
        }
        return new Result(true, detached, before, after, threshold, "");
    }

    private static float thresholdFraction(LimbDefinition limb) {
        if (limb.category().equals(LimbCategories.HEAD)) {
            return 0.45F;
        }
        if (limb.category().equals(LimbCategories.LEG)) {
            return 0.30F;
        }
        if (limb.category().equals(LimbCategories.ARM) || limb.category().equals(LimbCategories.TAIL)) {
            return 0.25F;
        }
        return 0.0F;
    }

    private static boolean canDamageHead(GunAttackConfig config, double distance) {
        var item = config.gunItemStack().getItem();
        return item == HumanGunItems.M42A3_SNIPER_RIFLE.get()
            || (distance <= 8.0D
                && (item == HumanGunItems.M37_12_SHOTGUN.get() || item == HumanGunItems.ZX_76_SHOTGUN.get()));
    }

    record Result(
        boolean applied,
        boolean detached,
        float damageBefore,
        float damageAfter,
        float threshold,
        String rejectionReason
    ) {

        private static Result notApplied(String reason) {
            return new Result(false, false, 0.0F, 0.0F, 0.0F, reason);
        }
    }

    private GunLimbDamage() {}
}
