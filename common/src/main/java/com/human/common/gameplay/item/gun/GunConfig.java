package com.human.common.gameplay.item.gun;

import com.human.common.gameplay.item.gun.animation.dispatcher.GunAnimationDispatcher;
import com.human.common.gameplay.item.gun.targeting.GunTargetingProfile;
import net.minecraft.world.level.ItemLike;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

public record GunConfig(
    @Nullable Supplier<ItemLike> ammunitionItemSupplier,
    GunAnimationDispatcher animationDispatcher,
    int durability,
    List<FireModeConfig> fireModeConfigs,
    @Nullable GunHeatProfile heatProfile,
    int maximumAmmunition,
    int reloadAmount,
    int reloadTimeInTicks,

    /**
     * This gun's NATIVE smart tracker, if it was built with one.
     * <p>
     * ⚠⚠ NEVER READ THIS FIELD DIRECTLY - go through {@code SmartTargeting.resolve(ItemStack)}. GunConfig is
     * per-item-type and static, so a bolt-on smart module living on the STACK could never be seen here. The resolver
     * checks the stack first and falls back to this; anything reading the field alone silently ignores attachments.
     * </p>
     */
    @Nullable GunTargetingProfile targetingProfile
) {

    public @NotNull FireModeConfig getDefaultFireMode() {
        return fireModeConfigs.getFirst();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private final List<FireModeConfig> fireModeConfigs;

        private Supplier<ItemLike> ammunitionItemSupplier;

        private GunAnimationDispatcher animationDispatcher;

        private int durability;

        private GunHeatProfile heatProfile;

        private int maximumAmmunition;

        private int reloadAmount;

        private int reloadTimeInTicks;

        private GunTargetingProfile targetingProfile;

        private Builder() {
            this.animationDispatcher = GunAnimationDispatcher.DEFAULT;
            this.fireModeConfigs = new ArrayList<>();
            this.reloadAmount = 1;
        }

        public Builder withAmmunitionItemSupplier(Supplier<ItemLike> ammunitionItemSupplier) {
            this.ammunitionItemSupplier = ammunitionItemSupplier;
            return this;
        }

        public Builder withAnimationDispatcher(GunAnimationDispatcher animationDispatcher) {
            this.animationDispatcher = animationDispatcher;
            return this;
        }

        public Builder withDurability(int durability) {
            this.durability = durability;
            return this;
        }

        public Builder withFireMode(FireModeConfig fireModeConfig) {
            fireModeConfigs.add(fireModeConfig);
            return this;
        }

        /**
         * Gives this gun an overheat cycle. Left unset - which is every weapon but the two machine guns - the gun has
         * no heat state at all and nothing in the heat path ever runs for it.
         */
        public Builder withHeatProfile(GunHeatProfile heatProfile) {
            this.heatProfile = heatProfile;
            return this;
        }

        /**
         * Gives this gun a native smart tracker. Left unset, the gun has none - and can still be given one later by a
         * bolt-on smart module, which the resolver checks first.
         */
        public Builder withTargetingProfile(GunTargetingProfile targetingProfile) {
            this.targetingProfile = targetingProfile;
            return this;
        }

        public Builder withMaximumAmmunition(int defaultMaxAmmunition) {
            this.maximumAmmunition = defaultMaxAmmunition;
            return this;
        }

        // TODO: Reload data should probably be in an object.
        public Builder withReloadTimeInTicks(int reloadTimeInTicks) {
            this.reloadTimeInTicks = reloadTimeInTicks;
            return this;
        }

        // TODO: Reload data should probably be in an object.
        public Builder withReloadAmount(int reloadAmount) {
            this.reloadAmount = reloadAmount;
            return this;
        }

        public GunConfig build() {
            if (fireModeConfigs.isEmpty()) {
                throw new IllegalStateException("At least 1 fire mode for guns is required.");
            }

            return new GunConfig(
                ammunitionItemSupplier,
                animationDispatcher,
                durability,
                Collections.unmodifiableList(fireModeConfigs),
                heatProfile,
                maximumAmmunition,
                reloadAmount,
                reloadTimeInTicks,
                targetingProfile
            );
        }
    }
}
