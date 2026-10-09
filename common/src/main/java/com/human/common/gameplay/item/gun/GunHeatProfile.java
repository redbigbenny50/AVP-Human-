package com.human.common.gameplay.item.gun;

import org.jetbrains.annotations.Nullable;

/**
 * Per-gun overheat tuning. A gun with no profile on its {@link GunConfig} has no heat at all, which is every weapon
 * except the two machine guns.
 * <p>
 * ⭐⭐ THE ONE NUMBER THAT DECIDES EVERYTHING IS {@code heatPerShot} MINUS {@code passiveCoolingPerTick}. Cooling runs
 * every tick from {@code inventoryTick} whether the gun is firing or not, so holding the trigger accrues that
 * difference per shot interval. Two consequences worth keeping in mind before touching any of these:
 * </p>
 * <ul>
 * <li>TIME TO OVERHEAT (continuous fire, one shot per tick) = {@code maxHeat / (heatPerShot - passiveCoolingPerTick)}
 * ticks.</li>
 * <li>THE SUSTAINABLE DUTY CYCLE is {@code passiveCoolingPerTick / heatPerShot}. Below it the gun NEVER overheats no
 * matter how long the engagement runs - which is how marines behave, since they fire in bursts with a flat 30-tick
 * pause between them.</li>
 * </ul>
 * <p>
 * ⚠ A GUN'S ROUNDS-PER-SHOT IS NOT ITS HEAT-PER-SHOT. Old Painless spends six rounds per trigger tick and the smartgun
 * spends one, so matching them on seconds and matching them on magazine fraction are very different settings. See
 * {@link GunData} for which of the two each gun was tuned against.
 * </p>
 */
public record GunHeatProfile(
    String displayName,
    int lockoutTicks,
    int maxHeat,
    int heatPerShot,
    int overheatedCoolingPerTick,
    int passiveCoolingPerTick,
    float smokeThreshold,
    float warningThreshold
) {

    /**
     * ⚠ BUILT, NOT CONSTRUCTED POSITIONALLY, ON PURPOSE. Six of these eight fields are ints in the same rough range, so
     * a positional constructor is exactly the shape that silently swapped two config values once before and compiled
     * clean. Name every value at the call site.
     */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private String displayName = "";

        private int lockoutTicks = 100;

        private int maxHeat = 420;

        private int heatPerShot = 4;

        private int overheatedCoolingPerTick = 4;

        private int passiveCoolingPerTick = 1;

        private float smokeThreshold = 0.30F;

        private float warningThreshold;

        private Builder() {}

        /** Shown in the action bar, upper-cased by the caller's own wording: "OLD PAINLESS", "M56 SMARTGUN". */
        public Builder withDisplayName(String displayName) {
            this.displayName = displayName;
            return this;
        }

        /** How long the item cooldown holds after an overheat. */
        public Builder withLockoutTicks(int lockoutTicks) {
            this.lockoutTicks = lockoutTicks;
            return this;
        }

        public Builder withMaxHeat(int maxHeat) {
            this.maxHeat = maxHeat;
            return this;
        }

        public Builder withHeatPerShot(int heatPerShot) {
            this.heatPerShot = heatPerShot;
            return this;
        }

        public Builder withOverheatedCoolingPerTick(int overheatedCoolingPerTick) {
            this.overheatedCoolingPerTick = overheatedCoolingPerTick;
            return this;
        }

        public Builder withPassiveCoolingPerTick(int passiveCoolingPerTick) {
            this.passiveCoolingPerTick = passiveCoolingPerTick;
            return this;
        }

        /** Fraction of maxHeat above which the barrels smoke. */
        public Builder withSmokeThreshold(float smokeThreshold) {
            this.smokeThreshold = smokeThreshold;
            return this;
        }

        /**
         * Fraction of maxHeat above which a flashing action-bar warning appears. Zero disables the warning entirely,
         * which is what a gun with a visible red-hot barrel wants - it already tells the player.
         */
        public Builder withWarningThreshold(float warningThreshold) {
            this.warningThreshold = warningThreshold;
            return this;
        }

        public GunHeatProfile build() {
            if (heatPerShot <= passiveCoolingPerTick) {
                throw new IllegalStateException(
                    "A gun whose heat per shot does not exceed its passive cooling can never overheat."
                );
            }

            return new GunHeatProfile(
                displayName,
                lockoutTicks,
                maxHeat,
                heatPerShot,
                overheatedCoolingPerTick,
                passiveCoolingPerTick,
                smokeThreshold,
                warningThreshold
            );
        }
    }

    public @Nullable String overheatedMessage() {
        return displayName.isEmpty() ? null : displayName + " OVERHEATED - COOLING";
    }

    public @Nullable String overheatingMessage() {
        return displayName.isEmpty() ? null : displayName + " OVERHEATING";
    }
}
