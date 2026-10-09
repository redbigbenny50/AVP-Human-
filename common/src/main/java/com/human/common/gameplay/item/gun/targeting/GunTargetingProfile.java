package com.human.common.gameplay.item.gun.targeting;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * Everything the smart targeting system needs to know about ONE weapon's tracker.
 * <p>
 * ⚠⚠ NOTHING IN THIS SYSTEM IS NAMED FOR THE SMARTGUN, AND THAT IS DELIBERATE. A second smart weapon is planned and a
 * bolt-on smart module is being considered that would give ANY gun - down to the pistol - a tracker. The heat system
 * had to be torn out of {@code OldPainlessHeat} and rebuilt as {@code GunHeat} mid-session for exactly this reason;
 * there is no excuse for building the same trap twice. If you find yourself writing {@code if (item == SMARTGUN)}, the
 * answer is a field on this record instead.
 * </p>
 * <p>
 * ⚠ THE PROFILE IS RESOLVED FROM THE STACK, NOT THE ITEM - see {@link SmartTargeting}. A profile hung only on
 * {@code GunConfig} would be per-item-type and static, which an attachment can never be.
 * </p>
 */
public record GunTargetingProfile(
    int frameWidth,
    int frameHeight,
    int lockDwellTicks,
    double range,
    float lockedSpreadMultiplier,
    float armourPenetration,
    boolean showWeaponHeat,
    int highlightColor,
    float highlightStrength,
    ResourceLocation bootSound,
    ResourceLocation acquireSound,
    ResourceLocation lockSound,
    ResourceLocation lockLostSound
) {

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private int frameWidth = 220;

        private int frameHeight = 180;

        private int lockDwellTicks = 10;

        private double range = 48.0D;

        private float lockedSpreadMultiplier = 0.45F;

        private float armourPenetration = 0.20F;

        private boolean showWeaponHeat = true;

        private int highlightColor = 0xFFB347;

        private float highlightStrength = 0.55F;

        private @Nullable ResourceLocation bootSound;

        private @Nullable ResourceLocation acquireSound;

        private @Nullable ResourceLocation lockSound;

        private @Nullable ResourceLocation lockLostSound;

        private Builder() {}

        /**
         * Size of the acquisition frame in GUI pixels.
         * <p>
         * ⚠ THE FRAME IS THE ACQUISITION RULE, NOT A PICTURE OF IT. A target acquires if it projects inside these
         * bounds, so what the player sees and what the weapon takes can never disagree. The alternative - enforcing a
         * fixed angular cone and drawing a frame near it - lets the two drift apart the moment field of view changes,
         * and a xenomorph visibly inside the frame that refuses to acquire is the first bug report you would get.
         * </p>
         */
        public Builder withFrameSize(int frameWidth, int frameHeight) {
            this.frameWidth = frameWidth;
            this.frameHeight = frameHeight;
            return this;
        }

        /**
         * How long a target must stay in the frame before the lock circle completes.
         * <p>
         * ⭐ THE LOCK IS EARNED, AND THAT IS THE WHOLE GAME OF IT. In the films the arm swings and THEN the circle
         * lights. A zero-dwell lock is free, which makes the manual override decoration and lets the circle strobe
         * between two targets at similar angles - taking the armour penetration with it every tick.
         * </p>
         */
        public Builder withLockDwellTicks(int lockDwellTicks) {
            this.lockDwellTicks = lockDwellTicks;
            return this;
        }

        public Builder withRange(double range) {
            this.range = range;
            return this;
        }

        /** Multiplier applied to spread while locked. Lower is tighter; 1.0F is no benefit. */
        public Builder withLockedSpreadMultiplier(float lockedSpreadMultiplier) {
            this.lockedSpreadMultiplier = lockedSpreadMultiplier;
            return this;
        }

        /**
         * Fraction of the target's armour a locked shot ignores.
         * <p>
         * ⚠ NOT WIRED UP YET - this is phase 3. It lives here rather than in the HUD because a bolt-on module on a
         * sniper rifle must NOT inherit the smartgun's penetration; the module carries its own profile so a native
         * smart weapon can stay meaningfully better than an attachment.
         * </p>
         */
        public Builder withArmourPenetration(float armourPenetration) {
            this.armourPenetration = armourPenetration;
            return this;
        }

        /**
         * Whether the weapon block draws a heat gauge.
         * <p>
         * ⚠ A GUN WITH NO HEAT PROFILE MUST NOT RESERVE THE SPACE. A pistol wearing a bolt-on module has no barrel
         * temperature, so the gauge is absent AND the reserve block below it moves up to close the gap. That is why the
         * HUD stacks its blocks from a screen anchor instead of drawing at fixed coordinates.
         * </p>
         */
        public Builder withShowWeaponHeat(boolean showWeaponHeat) {
            this.showWeaponHeat = showWeaponHeat;
            return this;
        }

        /**
         * Colour and strength of the tint painted over the locked target.
         * <p>
         * ⚠ STRENGTH IS A BLEND, NOT AN OPACITY. At 1.0 the creature becomes a flat silhouette with no shading or
         * texture left; at 0 there is no highlight at all. Around half keeps the model readable underneath while still
         * colouring the whole thing. Set the strength to 0 to disable the highlight for a weapon entirely.
         * </p>
         */
        public Builder withHighlight(int highlightColor, float highlightStrength) {
            this.highlightColor = highlightColor;
            this.highlightStrength = highlightStrength;
            return this;
        }

        /**
         * ⭐ SOUNDS ARE FIELDS, NOT CONSTANTS. Every smart weapon defaults to the shared {@code item/smart_hud/} set,
         * but a weapon that wants its own voice names different events here rather than earning a branch in the
         * playback code.
         */
        public Builder withSounds(
            ResourceLocation bootSound,
            ResourceLocation acquireSound,
            ResourceLocation lockSound,
            ResourceLocation lockLostSound
        ) {
            this.bootSound = bootSound;
            this.acquireSound = acquireSound;
            this.lockSound = lockSound;
            this.lockLostSound = lockLostSound;
            return this;
        }

        public GunTargetingProfile build() {
            return new GunTargetingProfile(
                frameWidth,
                frameHeight,
                lockDwellTicks,
                range,
                lockedSpreadMultiplier,
                armourPenetration,
                showWeaponHeat,
                highlightColor,
                Math.max(0.0F, Math.min(1.0F, highlightStrength)),
                bootSound == null ? SmartHudSounds.BOOT : bootSound,
                acquireSound == null ? SmartHudSounds.ACQUIRE : acquireSound,
                lockSound == null ? SmartHudSounds.LOCK : lockSound,
                lockLostSound == null ? SmartHudSounds.LOCK_LOST : lockLostSound
            );
        }
    }
}
