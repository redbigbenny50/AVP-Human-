package com.human.common.gameplay.item.gun.attack.hitscan;

import com.blib.api.common.dismemberment.v1.hitbox.LimbHitboxDamage;
import com.blib.api.common.dismemberment.v1.hitbox.LimbHitboxRegistry;
import com.blib.api.common.enchantment.v1.EnchantmentUtil;
import com.human.common.gameplay.entity.living.human.marine.MarineAllyUtil;
import com.human.common.gameplay.item.gun.GunCalibers;
import com.human.common.gameplay.item.gun.attack.GunAttackConfig;
import com.human.common.registry.key.HumanDamageTypeKeys;
import com.human.compatibility.avp_alien.HumanAlienArmour;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.Enchantments;
import org.jetbrains.annotations.Nullable;

public class EntityGunHitResultHandler {

    /**
     * Applies one confirmed bullet hit.
     *
     * @param limbHit the limb volume the ray actually struck, or null for a plain body hit. Supplied by the tracer,
     *                which resolves limb geometry server-side; see {@code HitScanGunAttackAction.findNearestEntityHit}.
     */
    public static Result handle(
        GunAttackConfig gunAttackConfig,
        Entity hitEntity,
        int pierceIndex,
        @Nullable LimbHitboxRegistry.Hit limbHit
    ) {
        var shooter = gunAttackConfig.shooter();
        var level = (ServerLevel) shooter.level();

        if (MarineAllyUtil.isMarineAlly(shooter, hitEntity)) {
            return Result.REJECTED;
        }

        if (shooter instanceof Player && hitEntity instanceof Player && !level.getServer().isPvpAllowed()) {
            // Do not hurt entities if shooter was a player, target was a player and if PVP is not allowed.
            return Result.REJECTED;
        }

        // Apply pre-effects.
        if (hitEntity instanceof LivingEntity livingEntity) {
            applyFlameEffects(gunAttackConfig, livingEntity);
        }

        var powerLevel = EnchantmentUtil.getLevel(level, gunAttackConfig.gunItemStack(), Enchantments.POWER);
        var baseDamage = gunAttackConfig.fireModeConfig().damage()
            * gunAttackConfig.damageMultiplier()
            * (1 + (0.25F * powerLevel));
        var multiplier = 1.0F - (0.2F * pierceIndex);

        // Where the bullet landed on the body matters: a head volume carries a 2.0 health multiplier, everything else
        // 1.0. This is the headshot bonus finally being applied — the value has been authored per-limb all along.
        var limbHealthMultiplier = limbHit == null ? 1.0F : limbHit.volume().healthDamageMultiplier();

        // ⭐ ARMOUR THAT CARES WHAT SHOT IT. The per-limb healthDamageMultiplier above is a single flat number and
        // cannot tell a pistol from a sniper; a crusher is meant to shrug off small rounds through its crested
        // head entirely and take a quarter less from medium ones. The caliber is known HERE and the armour rule is
        // known in avp_alien, so this asks across the boundary through the usual guarded proxy - see
        // HumanAlienArmour. Returns 1.0 when avp_alien is absent, so the shot is simply unaffected.
        var caliber = GunCalibers.of(gunAttackConfig.gunConfig());
        var armourMultiplier = hitEntity instanceof LivingEntity armourTarget
            ? HumanAlienArmour.damageMultiplier(
                armourTarget,
                limbHit == null ? null : limbHit.volume().limbId().getPath(),
                caliber
            )
            : 1.0F;

        var damage = baseDamage * multiplier * limbHealthMultiplier * armourMultiplier;

        // ⚠ A FULLY ABSORBED ROUND MUST NOT REACH hurt(). Passing 0 damage would still trip the target's
        // invulnerability window and, worse, the post-effect block below sets invulnerableTime to 0 and re-aims
        // the victim at the shooter - so an "immune" hit would still aggro and still cancel i-frames from a real
        // hit landing the same tick. Report it as a clean miss instead.
        if (damage <= 0.0F) {
            return Result.REJECTED;
        }
        var registry = shooter.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE);
        var damageSource = new DamageSource(registry.getHolderOrThrow(HumanDamageTypeKeys.BULLET), shooter);

        var healthBefore = hitEntity instanceof LivingEntity livingEntity ? livingEntity.getHealth() : 0.0F;
        var wasHurt = hitEntity.hurt(damageSource, damage);
        var actualDamage = hitEntity instanceof LivingEntity livingEntity
            ? Math.max(0.0F, healthBefore - livingEntity.getHealth())
            : wasHurt ? damage : 0.0F;

        // Feed the limb's own damage pool with the health ACTUALLY lost, not the damage rolled - armour and
        // absorption should protect a limb exactly as much as they protect the health bar. LimbHitboxDamage detaches
        // the limb itself once the pool passes that volume's threshold (arms 25, legs 30, tail 35 on a standard xeno).
        if (limbHit != null && actualDamage > 0.0F && hitEntity instanceof LivingEntity limbTarget) {
            LimbHitboxDamage.apply(limbTarget, limbHit, actualDamage);
        }

        // Apply post-effects.
        if (wasHurt && hitEntity instanceof LivingEntity livingEntity) {
            livingEntity.invulnerableTime = 0;
            shooter.setLastHurtMob(livingEntity);
            livingEntity.setLastHurtByMob(shooter);

            applyKnockbackEffects(gunAttackConfig, livingEntity, shooter);
        }

        return new Result(wasHurt, wasHurt && !hitEntity.isAlive(), actualDamage);
    }

    private static void applyFlameEffects(GunAttackConfig gunAttackConfig, LivingEntity livingEntity) {
        var flameLevel = EnchantmentUtil.getLevel(livingEntity.level(), gunAttackConfig.gunItemStack(), Enchantments.FLAME);

        if (flameLevel > 0) {
            livingEntity.igniteForTicks(20 * 5);
        }
    }

    private static void applyKnockbackEffects(GunAttackConfig gunAttackConfig, LivingEntity livingEntity, LivingEntity shooter) {
        var punchLevel = EnchantmentUtil.getLevel(livingEntity.level(), gunAttackConfig.gunItemStack(), Enchantments.PUNCH);
        var baseKnockback = gunAttackConfig.fireModeConfig().knockback();

        if (punchLevel > 0) {
            if (baseKnockback == 0) {
                baseKnockback = 0.2F;
            }

            baseKnockback *= punchLevel;
        }

        livingEntity.knockback(
            baseKnockback,
            Mth.sin(shooter.getYRot() * Mth.DEG_TO_RAD),
            -Mth.cos(shooter.getYRot() * Mth.DEG_TO_RAD)
        );
    }

    public record Result(
        boolean hurt,
        boolean lethal,
        float actualDamage
    ) {

        private static final Result REJECTED = new Result(false, false, 0.0F);
    }
}
