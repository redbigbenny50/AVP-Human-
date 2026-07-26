package com.human.common.gameplay.item.gun;

import com.human.common.gameplay.item.gun.animation.dispatcher.impl.M42A3SniperRifleAnimationDispatcher;
import com.human.common.gameplay.item.gun.animation.dispatcher.impl.OldPainlessAnimationDispatcher;
import com.human.common.gameplay.item.gun.attack.projectile.FlamethrowProjectileGunAttackAction;
import com.human.common.gameplay.item.gun.attack.projectile.RocketProjectileGunAttackAction;
import com.human.common.registry.init.HumanSoundEvents;
import com.human.common.registry.init.item.HumanItems;

import java.util.Objects;

public class GunData {

    public static final GunConfig F903WE_RIFLE = GunConfig.builder()
        .withDurability(2048)
        .withMaximumAmmunition(32)
        .withReloadTimeInTicks(20 * 3)
        .withAmmunitionItemSupplier(() -> Objects.requireNonNull(HumanItems.SMALL_BULLET).get())
        .withFireMode(
            FireModeConfig.builder()
                .withCooldownInTicks(2)
                .withDamage(2F * 2)
                .withRange(64)
                .withRecoil(2.5F)
                .build()
        )
        .build();

    public static final GunConfig FLAMETHROWER_SEVASTOPOL = GunConfig.builder()
        .withDurability(4096)
        .withMaximumAmmunition(1000)
        .withReloadAmount(1000)
        .withReloadTimeInTicks(20 * 5)
        .withAmmunitionItemSupplier(() -> Objects.requireNonNull(HumanItems.FUEL_TANK.get()))
        .withFireMode(
            FireModeConfig.builder()
                .withCooldownInTicks(1)
                .withDamage(1F)
                .withPrimaryShootSoundFrequencyInTicks(7)
                .withRange(16)
                .withReloadFinishSound(HumanSoundEvents.WEAPON_FLAMETHROWER_SEVASTOPOL_RELOAD_FINISH)
                .withReloadStartSound(HumanSoundEvents.WEAPON_FLAMETHROWER_SEVASTOPOL_RELOAD_START)
                .withPrimaryShootSound(HumanSoundEvents.WEAPON_FLAMETHROWER_SEVASTOPOL_SHOOT)
                .withGunAttackAction(FlamethrowProjectileGunAttackAction.INSTANCE)
                .build()
        )
        .build();

    public static final GunConfig M37_12_SHOTGUN = GunConfig.builder()
        .withDurability(1024)
        .withMaximumAmmunition(6)
        .withReloadTimeInTicks(20 * 4)
        .withAmmunitionItemSupplier(() -> Objects.requireNonNull(HumanItems.SHOTGUN_SHELL.get()))
        .withFireMode(
            FireModeConfig.builder()
                .withCooldownInTicks(20)
                .withDamage(8F * 2)
                .withKnockback(0.75F)
                .withRange(12)
                .withRecoil(8F)
                .withPrimaryShootSound(HumanSoundEvents.WEAPON_M37_12_SHOTGUN_SHOOT)
                .build()
        )
        .build();

    public static final GunConfig M41A_PULSE_RIFLE = GunConfig.builder()
        .withDurability(2048 + 1024)
        .withMaximumAmmunition(99)
        .withReloadTimeInTicks(20 * 3)
        .withAmmunitionItemSupplier(() -> Objects.requireNonNull(HumanItems.CASELESS_BULLET.get()))
        // Burst
        .withFireMode(
            FireModeConfig.builder()
                .withConsumedAmmunitionPerShot(4)
                .withCooldownInTicks(10)
                .withDamage(6F)
                .withRange(64)
                .withRecoil(2.25F)
                .withPrimaryShootSound(HumanSoundEvents.WEAPON_M41A_PULSE_RIFLE_SHOOT)
                .build()
        )
        .build();

    public static final GunConfig M42A3_SNIPER_RIFLE = GunConfig.builder()
        .withAnimationDispatcher(M42A3SniperRifleAnimationDispatcher.INSTANCE)
        .withDurability(1024)
        .withMaximumAmmunition(6)
        .withReloadTimeInTicks(20 * 7 + 10)
        .withAmmunitionItemSupplier(() -> Objects.requireNonNull(HumanItems.HEAVY_BULLET.get()))
        .withFireMode(
            FireModeConfig.builder()
                .withCooldownInTicks(30)
                .withDamage(15F * 2)
                .withRange(128)
                .withRecoil(2.25F)
                .withPrimaryShootSound(HumanSoundEvents.WEAPON_M42A3_SNIPER_RIFLE_SHOOT)
                .build()
        )
        .build();

    public static final GunConfig M4RA_BATTLE_RIFLE = GunConfig.builder()
        .withDurability(2048)
        .withMaximumAmmunition(90)
        .withReloadTimeInTicks(20 * 4)
        .withAmmunitionItemSupplier(() -> Objects.requireNonNull(HumanItems.MEDIUM_BULLET.get()))
        .withFireMode(
            FireModeConfig.builder()
                .withCooldownInTicks(2)
                .withDamage(2F * 2)
                .withRange(64)
                .withRecoil(1.25F)
                .withPrimaryShootSound(HumanSoundEvents.WEAPON_M4RA_BATTLE_RIFLE_SHOOT)
                .build()
        )
        .build();

    public static final GunConfig M56_SMARTGUN = GunConfig.builder()
        .withDurability(4096)
        .withMaximumAmmunition(500)
        .withReloadTimeInTicks(20 * 7)
        .withAmmunitionItemSupplier(() -> Objects.requireNonNull(HumanItems.CASELESS_BULLET.get()))
        .withFireMode(
            FireModeConfig.builder()
                .withCooldownInTicks(1)
                .withDamage(2F * 2)
                .withRange(64)
                .withRecoil(1F)
                .withPrimaryShootSound(HumanSoundEvents.WEAPON_M56_SMARTGUN_SHOOT)
                .build()
        )
        .build();

    public static final GunConfig M6B_ROCKET_LAUNCHER = GunConfig.builder()
        .withDurability(512)
        .withMaximumAmmunition(4)
        .withReloadTimeInTicks(20 * 4)
        .withAmmunitionItemSupplier(() -> Objects.requireNonNull(HumanItems.ROCKET.get()))
        .withFireMode(
            FireModeConfig.builder()
                .withCooldownInTicks(60)
                .withDamage(8F * 2)
                .withRange(100)
                .withRecoil(12F)
                .withReloadFinishSound(HumanSoundEvents.WEAPON_M6B_ROCKET_LAUNCHER_RELOAD_FINISH)
                .withReloadStartSound(HumanSoundEvents.WEAPON_M6B_ROCKET_LAUNCHER_RELOAD_START)
                .withPrimaryShootSound(HumanSoundEvents.WEAPON_M6B_ROCKET_LAUNCHER_SHOOT)
                .withGunAttackAction(RocketProjectileGunAttackAction.INSTANCE)
                .build()
        )
        .build();

    public static final GunConfig M88_MOD_4_COMBAT_PISTOL = GunConfig.builder()
        .withDurability(1024)
        .withMaximumAmmunition(18)
        .withReloadTimeInTicks(10 * 2 + 10)
        .withAmmunitionItemSupplier(() -> Objects.requireNonNull(HumanItems.SMALL_BULLET.get()))
        .withFireMode(
            FireModeConfig.builder()
                .withCooldownInTicks(5)
                .withDamage(3F * 1)
                .withRange(32)
                .withRecoil(0.35F)
                .withReloadStartSound(HumanSoundEvents.WEAPON_M88_MOD_4_COMBAT_PISTOL_RELOAD)
                .withPrimaryShootSound(HumanSoundEvents.WEAPON_M88_MOD_4_COMBAT_PISTOL_SHOOT)
                .build()
        )
        .build();

    public static final GunConfig OLD_PAINLESS = GunConfig.builder()
        .withAnimationDispatcher(OldPainlessAnimationDispatcher.INSTANCE)
        .withDurability(4096)
        .withMaximumAmmunition(Integer.MAX_VALUE)
        .withReloadTimeInTicks(0)
        .withAmmunitionItemSupplier(() -> Objects.requireNonNull(HumanItems.HEAVY_BULLET.get()))
        .withFireMode(
            FireModeConfig.builder()
                .withCooldownInTicks(1)
                .withConsumedAmmunitionPerShot(6)
                .withDamage(2F * 2 * 2) // 1 half-heart * 2 hearts * 2 bonus damage multiplier
                .withRange(64)
                .withPrimaryShootSound(HumanSoundEvents.WEAPON_OLD_PAINLESS_SHOOT)
                .withPrimaryShootSoundFrequencyInTicks(10)
                .withSecondaryShootSound(HumanSoundEvents.WEAPON_OLD_PAINLESS_SHOOT_SPINNING)
                .withSecondaryShootSoundFrequencyInTicks(30)
                .withShootDelayInTicks(20)
                .withShootFinishSound(HumanSoundEvents.WEAPON_OLD_PAINLESS_SHOOT_FINISH)
                .withShootStartSound(HumanSoundEvents.WEAPON_OLD_PAINLESS_SHOOT_START)
                .build()
        )
        .build();

    public static final GunConfig ZX_76_SHOTGUN = GunConfig.builder()
        .withDurability(1024)
        .withMaximumAmmunition(12)
        .withReloadTimeInTicks(20 * 3)
        .withAmmunitionItemSupplier(() -> Objects.requireNonNull(HumanItems.SHOTGUN_SHELL.get()))
        .withFireMode(
            FireModeConfig.builder()
                .withCooldownInTicks(20)
                .withDamage(8F * 2)
                .withKnockback(0.75F)
                .withRange(12)
                .withRecoil(8F)
                .withPrimaryShootSound(HumanSoundEvents.WEAPON_ZX_76_SHOTGUN_SHOOT)
                .build()
        )
        .build();
}
