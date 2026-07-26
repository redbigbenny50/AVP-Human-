package com.human.common.gameplay.item.gun;

import com.human.common.gameplay.item.gun.attack.GunAttackAction;
import com.human.common.gameplay.item.gun.attack.hitscan.HitScanGunAttackAction;
import com.human.common.registry.init.HumanSoundEvents;
import net.minecraft.sounds.SoundEvent;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

public record FireModeConfig(
    int consumedAmmunitionPerShot,
    int cooldownInTicks,
    float damage,
    float knockback,
    Supplier<SoundEvent> primaryShootSoundEvent,
    int primaryShootSoundFrequencyInTicks,
    int range,
    float recoil,
    Supplier<SoundEvent> reloadFinishSoundEvent,
    Supplier<SoundEvent> reloadStartSoundEvent,
    @Nullable Supplier<SoundEvent> secondaryShootSoundEvent,
    int secondaryShootSoundFrequencyInTicks,
    int shootDelayInTicks,
    @Nullable Supplier<SoundEvent> shootFinishSoundEvent,
    @Nullable Supplier<SoundEvent> shootStartSoundEvent,
    GunAttackAction gunAttackAction,
    RecoilProfile recoilProfile,
    int pelletCount,
    float pelletSpreadDegrees,
    float damageFalloffStartFraction,
    float minimumDamageMultiplier,
    int tracerFrequency
) {

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private int consumedAmmunitionPerShot;

        private int cooldownInTicks;

        private float damage;

        private float knockback;

        private Supplier<SoundEvent> primaryShootSoundEvent;

        private int primaryShootSoundFrequencyInTicks;

        private int range;

        private float recoil;

        private Supplier<SoundEvent> reloadFinishSoundEvent;

        private Supplier<SoundEvent> reloadStartSoundEvent;

        private Supplier<SoundEvent> secondaryShootSoundEvent;

        private int secondaryShootSoundFrequencyInTicks;

        private int shootDelayInTicks;

        private Supplier<SoundEvent> shootFinishSoundEvent;

        private Supplier<SoundEvent> shootStartSoundEvent;

        private GunAttackAction gunAttackAction;

        private RecoilProfile recoilProfile;

        private int pelletCount;

        private float pelletSpreadDegrees;

        private float damageFalloffStartFraction;

        private float minimumDamageMultiplier;

        private int tracerFrequency;

        private Builder() {
            this.consumedAmmunitionPerShot = 1;
            this.cooldownInTicks = 0;
            this.damage = 1F;
            this.knockback = 0F;
            this.primaryShootSoundEvent = HumanSoundEvents.WEAPON_GENERIC_SHOOT;
            this.primaryShootSoundFrequencyInTicks = 0;
            this.range = 16;
            this.recoil = 0F;
            this.reloadFinishSoundEvent = HumanSoundEvents.WEAPON_GENERIC_RELOAD;
            this.reloadStartSoundEvent = HumanSoundEvents.WEAPON_GENERIC_RELOAD;
            this.secondaryShootSoundFrequencyInTicks = 0;
            this.shootDelayInTicks = 0;
            this.gunAttackAction = HitScanGunAttackAction.INSTANCE;
            this.pelletCount = 1;
            this.damageFalloffStartFraction = 0.55F;
            this.minimumDamageMultiplier = 0.55F;
            this.tracerFrequency = 4;
        }

        public Builder withConsumedAmmunitionPerShot(int consumedAmmunitionPerShot) {
            this.consumedAmmunitionPerShot = consumedAmmunitionPerShot;
            return this;
        }

        public Builder withCooldownInTicks(int cooldownInTicks) {
            this.cooldownInTicks = cooldownInTicks;
            return this;
        }

        public Builder withDamage(float damage) {
            this.damage = damage;
            return this;
        }

        public Builder withKnockback(float knockback) {
            this.knockback = knockback;
            return this;
        }

        public Builder withPrimaryShootSound(Supplier<SoundEvent> primaryShootSoundEvent) {
            this.primaryShootSoundEvent = primaryShootSoundEvent;
            return this;
        }

        public Builder withPrimaryShootSoundFrequencyInTicks(int primaryShootSoundFrequencyInTicks) {
            this.primaryShootSoundFrequencyInTicks = primaryShootSoundFrequencyInTicks;
            return this;
        }

        public Builder withRange(int range) {
            this.range = range;
            return this;
        }

        public Builder withRecoil(float recoil) {
            this.recoil = recoil;
            return this;
        }

        public Builder withReloadFinishSound(Supplier<SoundEvent> reloadFinishSoundEvent) {
            this.reloadFinishSoundEvent = reloadFinishSoundEvent;
            return this;
        }

        public Builder withReloadStartSound(Supplier<SoundEvent> reloadStartSoundEvent) {
            this.reloadStartSoundEvent = reloadStartSoundEvent;
            return this;
        }

        public Builder withSecondaryShootSoundFrequencyInTicks(int secondaryShootSoundFrequencyInTicks) {
            this.secondaryShootSoundFrequencyInTicks = secondaryShootSoundFrequencyInTicks;
            return this;
        }

        public Builder withSecondaryShootSound(Supplier<SoundEvent> secondaryShootSoundEvent) {
            this.secondaryShootSoundEvent = secondaryShootSoundEvent;
            return this;
        }

        public Builder withShootDelayInTicks(int shootDelayInTicks) {
            this.shootDelayInTicks = shootDelayInTicks;
            return this;
        }

        public Builder withShootFinishSound(Supplier<SoundEvent> shootFinishSoundEvent) {
            this.shootFinishSoundEvent = shootFinishSoundEvent;
            return this;
        }

        public Builder withShootStartSound(Supplier<SoundEvent> shootStartSoundEvent) {
            this.shootStartSoundEvent = shootStartSoundEvent;
            return this;
        }

        public Builder withGunAttackAction(GunAttackAction gunAttackAction) {
            this.gunAttackAction = gunAttackAction;
            return this;
        }

        public Builder withRecoilProfile(RecoilProfile recoilProfile) {
            this.recoilProfile = recoilProfile;
            return this;
        }

        public Builder withPelletCount(int pelletCount) {
            this.pelletCount = pelletCount;
            return this;
        }

        public Builder withPelletSpreadDegrees(float pelletSpreadDegrees) {
            this.pelletSpreadDegrees = pelletSpreadDegrees;
            return this;
        }

        public Builder withDamageFalloff(float startFraction, float minimumMultiplier) {
            this.damageFalloffStartFraction = startFraction;
            this.minimumDamageMultiplier = minimumMultiplier;
            return this;
        }

        public Builder withTracerFrequency(int tracerFrequency) {
            this.tracerFrequency = tracerFrequency;
            return this;
        }

        public FireModeConfig build() {
            return new FireModeConfig(
                consumedAmmunitionPerShot,
                cooldownInTicks,
                damage,
                knockback,
                primaryShootSoundEvent,
                primaryShootSoundFrequencyInTicks,
                range,
                recoil,
                reloadFinishSoundEvent,
                reloadStartSoundEvent,
                secondaryShootSoundEvent,
                secondaryShootSoundFrequencyInTicks,
                shootDelayInTicks,
                shootFinishSoundEvent,
                shootStartSoundEvent,
                gunAttackAction,
                recoilProfile == null ? RecoilProfile.fromLegacy(recoil) : recoilProfile,
                Math.max(1, pelletCount),
                Math.max(0.0F, pelletSpreadDegrees),
                Math.clamp(damageFalloffStartFraction, 0.0F, 1.0F),
                Math.clamp(minimumDamageMultiplier, 0.0F, 1.0F),
                Math.max(1, tracerFrequency)
            );
        }
    }
}
