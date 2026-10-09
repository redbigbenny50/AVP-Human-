package com.human.common.gameplay.item.gun.targeting;

import com.human.common.gameplay.item.GunItem;
import com.human.common.registry.init.HumanDataComponents;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * The ONE place a targeting profile is resolved from a stack. Every caller goes through here.
 * <p>
 * ⚠⚠ ORDER MATTERS: STACK FIRST, THEN THE GUN'S OWN PROFILE. A bolt-on smart module lives on the STACK, so it has to be
 * able to give a tracker to a gun whose {@code GunConfig} has none - and to override a native one if a better module is
 * fitted. A resolver that checked the item type first could never do either.
 * </p>
 * <p>
 * ⚠ NEVER TEST THE ITEM IDENTITY AT A CALL SITE. {@code if (item == SMARTGUN)} is what made the muzzle flash, the
 * ammunition tooltip and the whole heat system need tearing out and rebuilding. If a weapon needs to differ, it needs a
 * field on {@link GunTargetingProfile}.
 * </p>
 */
public final class SmartTargeting {

    /** Returns the tracker this stack should draw, or null if it has none. */
    public static @Nullable GunTargetingProfile resolve(ItemStack itemStack) {
        if (itemStack.isEmpty()) {
            return null;
        }

        var moduleProfileId = itemStack.get(HumanDataComponents.SMART_MODULE.get());

        if (moduleProfileId != null) {
            var moduleProfile = GunTargetingProfiles.get(moduleProfileId);

            if (moduleProfile != null) {
                return moduleProfile;
            }
        }

        return itemStack.getItem() instanceof GunItem gunItem ? gunItem.getGunConfig().targetingProfile() : null;
    }

    public static boolean hasTracker(ItemStack itemStack) {
        return resolve(itemStack) != null;
    }

    private SmartTargeting() {
        throw new UnsupportedOperationException();
    }
}
