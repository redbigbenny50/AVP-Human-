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
                // Service rifle - short controlled bursts.
                .withBurstRounds(5)
                .withCooldownInTicks(2)
                .withDamage(2F * 2)
                .withRange(64)
                .withRecoil(2.5F)
                .withRecoilProfile(
                    new RecoilProfile(0.90F, 0.55F, 0.10F, 1.35F, 0.16F, 0.55F, new float[] { 0.0F, 0.65F, -0.45F, 0.35F, -0.25F })
                )
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
                .withRange(20)
                .withRecoil(8F)
                .withRecoilProfile(new RecoilProfile(3.20F, 1.20F, 0.55F, 4.50F, 0.20F, 0.72F, new float[] { -0.5F, 0.5F }))
                .withPelletCount(8)
                .withPelletSpreadDegrees(12.0F)
                .withDamageFalloff(0.30F, 0.15F)
                .withTracerFrequency(1)
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
                .withRecoilProfile(
                    new RecoilProfile(1.10F, 0.45F, 0.14F, 1.60F, 0.13F, 0.45F, new float[] { -0.15F, 0.45F, -0.50F, 0.25F })
                )
                // M41A pulse rifle - the three-round burst it is known for.
                .withBurstRounds(3)
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
                .withRecoilProfile(new RecoilProfile(2.20F, 0.25F, 0.025F, 0.30F, 0.06F, 0.30F, new float[] { 0.0F }))
                .withTracerFrequency(1)
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
                .withRecoilProfile(
                    new RecoilProfile(0.72F, 0.42F, 0.09F, 1.15F, 0.18F, 0.55F, new float[] { 0.35F, -0.35F, 0.50F, -0.20F })
                )
                // Battle rifle - short controlled bursts.
                .withBurstRounds(5)
                .withPrimaryShootSound(HumanSoundEvents.WEAPON_M4RA_BATTLE_RIFLE_SHOOT)
                .build()
        )
        .build();

    public static final GunConfig M56_SMARTGUN = GunConfig.builder()
        .withDurability(4096)
        .withMaximumAmmunition(800)
        .withReloadAmount(800)
        .withReloadTimeInTicks(20 * 4)
        .withAmmunitionItemSupplier(() -> Objects.requireNonNull(HumanItems.DRUM_CARTRIDGE.get()))
        .withFireMode(
            FireModeConfig.builder()
                .withCooldownInTicks(1)
                .withDamage(2F * 2)
                .withRange(64)
                .withRecoil(1F)
                .withRecoilProfile(
                    new RecoilProfile(0.48F, 0.38F, 0.12F, 2.20F, 0.08F, 0.60F, new float[] { -0.45F, 0.40F, -0.20F, 0.55F, -0.30F })
                )
                // M56 Smartgun - long bursts; it is meant to shred.
                .withBurstRounds(16)
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
                .withRecoilProfile(new RecoilProfile(0.42F, 0.20F, 0.06F, 0.55F, 0.24F, 0.60F, new float[] { 0.0F, 0.35F, -0.25F }))
                .withReloadStartSound(HumanSoundEvents.WEAPON_M88_MOD_4_COMBAT_PISTOL_RELOAD)
                .withPrimaryShootSound(HumanSoundEvents.WEAPON_M88_MOD_4_COMBAT_PISTOL_SHOOT)
                .build()
        )
        .build();

    public static final GunConfig OLD_PAINLESS = GunConfig.builder()
        .withAnimationDispatcher(OldPainlessAnimationDispatcher.INSTANCE)
        .withDurability(4096)
        .withMaximumAmmunition(1500)
        .withReloadAmount(1500)
        .withReloadTimeInTicks(20 * 4)
        .withAmmunitionItemSupplier(() -> Objects.requireNonNull(HumanItems.DRUM_CANNISTER.get()))
        .withFireMode(
            FireModeConfig.builder()
                .withCooldownInTicks(1)
                .withConsumedAmmunitionPerShot(6)
                .withDamage(2F * 2 * 2) // 1 half-heart * 2 hearts * 2 bonus damage multiplier
                .withRange(64)
                .withRecoilProfile(
                    new RecoilProfile(0.60F, 0.48F, 0.15F, 2.75F, 0.06F, 0.65F, new float[] { -0.40F, 0.35F, -0.25F, 0.55F, -0.50F })
                )
                // Old Painless — spins up, then empties. A short burst would waste the spin-up, so it stays long: a
                // minigun that fired in taps would read as a rifle. 64 of its 1500 is a bit over three seconds on the
                // trigger before the pause, which is the steadiest cadence of anything in the mod.
                .withBurstRounds(64)
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
                .withRange(20)
                .withRecoil(8F)
                .withRecoilProfile(new RecoilProfile(2.85F, 1.05F, 0.50F, 4.20F, 0.22F, 0.72F, new float[] { 0.45F, -0.45F }))
                .withPelletCount(8)
                .withPelletSpreadDegrees(11.0F)
                .withDamageFalloff(0.30F, 0.15F)
                .withTracerFrequency(1)
                .withPrimaryShootSound(HumanSoundEvents.WEAPON_ZX_76_SHOTGUN_SHOOT)
                .build()
        )
        .build();
}
