package com.human.common.gameplay.item.gun.targeting;

import com.human.HumanResources;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Named targeting profiles, so a smart module can carry a small id instead of an inlined copy of every tuning value.
 * <p>
 * ⚠ THE STACK COMPONENT STORES AN ID, NOT A PROFILE. With three or more of these planned, inlining the whole record
 * into a data component would bloat every stack and freeze the tuning at the moment the module was crafted - a balance
 * pass would leave old modules on old numbers forever. An id keeps the component tiny and the numbers in one place.
 * </p>
 */
public final class GunTargetingProfiles {

    private static final Map<ResourceLocation, GunTargetingProfile> PROFILES = new HashMap<>();

    /**
     * The bolt-on module's own profile - deliberately weaker than a purpose-built smart weapon.
     * <p>
     * ⚠ NOT REGISTERED TO ANY ITEM YET. The module item is later work; the profile exists now so the resolver has a
     * real second entry and the design cannot quietly collapse back into "the smartgun's tracker".
     * </p>
     */
    public static final ResourceLocation SMART_MODULE = register(
        HumanResources.location("smart_module"),
        GunTargetingProfile.builder()
            .withFrameSize(150, 120)
            .withLockDwellTicks(18)
            .withRange(32.0D)
            .withLockedSpreadMultiplier(0.70F)
            .withArmourPenetration(0.08F)
            // A gun wearing a bolt-on module usually has no barrel temperature to report.
            .withShowWeaponHeat(false)
            .build()
    );

    public static ResourceLocation register(ResourceLocation id, GunTargetingProfile profile) {
        PROFILES.put(id, profile);
        return id;
    }

    public static @Nullable GunTargetingProfile get(@Nullable ResourceLocation id) {
        return id == null ? null : PROFILES.get(id);
    }

    private GunTargetingProfiles() {
        throw new UnsupportedOperationException();
    }
}
