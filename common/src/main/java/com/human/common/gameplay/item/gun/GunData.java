package com.human.common.gameplay.item.gun;

import com.human.common.gameplay.item.gun.animation.dispatcher.impl.M42A3SniperRifleAnimationDispatcher;
import com.human.common.gameplay.item.gun.animation.dispatcher.impl.OldPainlessAnimationDispatcher;
import com.human.common.gameplay.item.gun.attack.projectile.FlamethrowProjectileGunAttackAction;
import com.human.common.gameplay.item.gun.attack.projectile.RocketProjectileGunAttackAction;
import com.human.common.gameplay.item.gun.targeting.GunTargetingProfile;
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
                // ⭐ Oct 6 - one flash per round, not one long flash: see the battle rifle below.
                .withMuzzleFlashDurationInTicks(2)
                .withMuzzleFlashStrobeIntervalInTicks(1)
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

    /**
     * The chem thrower - runs on the vaporizer's five vapor tanks. One tank = 1,000, used up whole, like the
     * flamethrower's fuel tank. ⚠ NO ammunition item here: it takes five, so the reload is its own
     * ({@link com.human.common.gameplay.item.gun.chem.ChemThrowerReloading}) and every shared ammo lookup (marines,
     * resupply, HUD, calibers) already skips a gun with none. Damage 0 here - what the blast does is the vapor's effect
     * ({@link com.human.common.gameplay.vapor.VaporEffects}; each vapor does what its liquid does; the cryomist freezes
     * solid). The spray has its own sound (start / loop / stop); the reload sounds are still the flamethrower's.
     */
    public static final GunConfig CHEM_THROWER = GunConfig.builder()
        .withDurability(4096)
        .withMaximumAmmunition(1000)
        .withReloadAmount(1000)
        .withReloadTimeInTicks(com.human.common.gameplay.item.gun.chem.ChemThrowerReloading.RELOAD_TICKS)
        .withFireMode(
            FireModeConfig.builder()
                .withCooldownInTicks(1)
                .withDamage(0F)
                // [stated] Oct 18: a directed blast about 8 blocks ahead.
                .withRange(8)
                .withReloadFinishSound(HumanSoundEvents.WEAPON_FLAMETHROWER_SEVASTOPOL_RELOAD_FINISH)
                .withReloadStartSound(HumanSoundEvents.WEAPON_FLAMETHROWER_SEVASTOPOL_RELOAD_START)
                // [stated] Oct 19 - no per-shot sound: the spray is heard as one sound that fades in, loops while the
                // trigger is held and fades out on release (client ChemThrowerSound, driven by the blast packets).
                .withPrimaryShootSound(null)
                .withGunAttackAction(com.human.common.gameplay.item.gun.attack.projectile.ChemBlastGunAttackAction.INSTANCE)
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
                // ⭐ FOUR FLASHES PER TRIGGER PULL - ONE PER ROUND THE SHOT ACTUALLY CONSUMES. [stated] "have it do 4
                // flashes one for each round consumed. it will make it feel more real."
                // <p>
                // ⚠ THE BALLISTICS ARE STILL ONE SHOT. consumedAmmunitionPerShot is 4 and the trace, the damage and
                // the ammunition are unchanged - this is the muzzle telling the truth about the four rounds the gun
                // already spends, not four separate bullets. Turning it into four real traces is a damage decision,
                // and a separate one.
                // </p>
                // ⚠ THE ARITHMETIC MATTERS: an 8-tick lit window strobed every tick contains EXACTLY four lit ticks
                // wherever in the tick cycle the shot lands, and 8 fits inside the 10-tick cooldown, so the burst
                // always finishes before the next one can start. Change one of these three numbers and the flash
                // count changes with it.
                .withMuzzleFlashDurationInTicks(8)
                .withMuzzleFlashStrobeIntervalInTicks(1)
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
                // ⭐ Oct 6 - [stated] "rather than it being in spurts like how other guns are so flash, no flash then
                // flash again when continually firing its just a single flash". A round every 2 ticks against the
                // default 5-tick flash kept it lit the whole burst. Same cure as the smartgun: a 2-tick flash chopped
                // one tick on, one tick off, so every round reads as its own flash.
                .withMuzzleFlashDurationInTicks(2)
                .withMuzzleFlashStrobeIntervalInTicks(1)
                .withPrimaryShootSound(HumanSoundEvents.WEAPON_M4RA_BATTLE_RIFLE_SHOOT)
                .build()
        )
        .build();

    /**
     * ⭐ TUNED AGAINST THE MAGAZINE, NOT THE CLOCK. [stated] the smartgun should overheat "the same overheat thing that
     * old painless does", and the option chosen was to match Old Painless's DRUM FRACTION rather than its seconds.
     * <p>
     * The smartgun spends ONE round per trigger tick where Old Painless spends six, so its 800 drum is forty seconds of
     * fire against the minigun's twelve and a half. Copying the minigun's seven seconds would have overheated it nearly
     * six times per drum and fought the "meant to shred" brief. At 2 heat a shot against 1 passive cooling it accrues a
     * net 1 per tick, so it overheats after 420 ticks - TWENTY-ONE SECONDS and 420 rounds, a little over half the drum,
     * which is the same rhythm Old Painless has to its own magazine (840 of 1500).
     * </p>
     * <p>
     * ⚠ ITS SUSTAINABLE DUTY CYCLE IS 50% (passive / per-shot). Marines fire 16-round bursts with a flat 30-tick pause
     * - about 35% - so a marine smartgunner will NEVER overheat. That is a consequence of the burst pause, not of these
     * numbers; the lever for changing it is BURST_PAUSE_IN_TICKS in GunStrategy.
     * </p>
     * <p>
     * ⚠ NO RED-HOT BARREL. Unlike Old Painless it has no heat texture, so the warning is carried entirely by the
     * flashing action-bar message that {@code warningThreshold} switches on - hence the threshold being set here and
     * left at zero on the minigun, which glows instead.
     * </p>
     */
    public static final GunConfig M56_SMARTGUN = GunConfig.builder()
        .withDurability(4096)
        // ⭐ The first smart tracker. Frame, dwell and the locked-shot bonuses all live on the profile so a second
        // smart weapon - or a bolt-on module fitted to a pistol - is a config block rather than a second system.
        .withTargetingProfile(
            GunTargetingProfile.builder()
                .withFrameSize(220, 180)
                // Half a second of holding the target before the lock circle completes.
                .withLockDwellTicks(10)
                .withRange(48.0D)
                .withLockedSpreadMultiplier(0.45F)
                .withArmourPenetration(0.20F)
                .withShowWeaponHeat(true)
                // ⭐ THE OUTLINE IS THE MU/TH/UR GREEN, the same 0x11B852 as the HUD lines - so the marked creature
                // and the frame around it read as one system rather than two effects that happen to be on screen
                // together.
                // ⚠ ONLY THE OUTLINE CAN BE THIS COLOUR. It is drawn in its own pass with its own colour and owes
                // nothing to the creature's texture. A vertex tint MULTIPLIES, so on a near-black xenomorph green is
                // simply unreachable - which is why the body is marked with neutral white instead and the colour is
                // left entirely to the outline.
                // ⚠ STRENGTH STAYS 0: the vertex tint is off. Raising it only darkens the body.
                .withHighlight(0x11B852, 0.0F)
                .build()
        )
        .withHeatProfile(
            GunHeatProfile.builder()
                .withDisplayName("M56 SMARTGUN")
                .withMaxHeat(420)
                .withHeatPerShot(2)
                .withPassiveCoolingPerTick(1)
                .withOverheatedCoolingPerTick(4)
                .withLockoutTicks(100)
                .withSmokeThreshold(0.30F)
                // Roughly four seconds of trigger left when it starts flashing.
                .withWarningThreshold(0.80F)
                .build()
        )
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
                // ⭐ TWENTY ROUNDS A SECOND CANNOT BE SHOWN AS TWENTY FLASHES - the game only ticks twenty times a
                // second, and a flash needs an off-frame to read as a flash at all. One tick lit, one tick dark, is
                // the fastest honest blink there is, and it reads as sustained automatic fire rather than a flame.
                .withMuzzleFlashDurationInTicks(2)
                .withMuzzleFlashStrobeIntervalInTicks(1)
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
                // ⭐ Oct 6 - a shot every 5 ticks against the default 5-tick flash re-lit it the moment it went out, so
                // rapid fire read as one flash. 3 ticks lit leaves 2 dark between shots.
                .withMuzzleFlashDurationInTicks(3)
                .withReloadStartSound(HumanSoundEvents.WEAPON_M88_MOD_4_COMBAT_PISTOL_RELOAD)
                .withPrimaryShootSound(HumanSoundEvents.WEAPON_M88_MOD_4_COMBAT_PISTOL_SHOOT)
                .build()
        )
        .build();

    /**
     * ⚠ THESE HEAT NUMBERS ARE UNCHANGED FROM THE HARDCODED {@code OldPainlessHeat} THEY REPLACED. 4 heat a shot
     * against 1 passive cooling is a net 3 per tick, so continuous fire overheats at 140 ticks - seven seconds, 140
     * shots, 840 of the drum's 1500 rounds. Its sustainable duty cycle is 25%, which is why marines (68% on a 64-round
     * burst) reliably cook their barrels and smartgunners do not.
     * <p>
     * ⚠ {@code warningThreshold} IS DELIBERATELY LEFT AT ZERO. This gun's barrel glows red and smokes from 30% heat via
     * OldPainlessHeatLayer, so it already warns the player; adding a flashing message on top would be noise.
     * </p>
     */
    public static final GunConfig OLD_PAINLESS = GunConfig.builder()
        .withAnimationDispatcher(OldPainlessAnimationDispatcher.INSTANCE)
        .withDurability(4096)
        .withHeatProfile(
            GunHeatProfile.builder()
                .withDisplayName("OLD PAINLESS")
                .withMaxHeat(420)
                .withHeatPerShot(4)
                .withPassiveCoolingPerTick(1)
                .withOverheatedCoolingPerTick(4)
                .withLockoutTicks(100)
                .withSmokeThreshold(0.30F)
                .build()
        )
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
