package com.human.common.gameplay.item.gun.targeting;

import com.human.HumanResources;
import net.minecraft.resources.ResourceLocation;

/**
 * The shared sound set every smart tracker defaults to.
 * <p>
 * ⚠⚠ THESE ARE BUILT AS PLAIN IDS AND DELIBERATELY DO NOT TOUCH {@code HumanSoundEvents}. Calling {@code .get()} on a
 * registry holder here would run during CLASS INITIALISATION - {@code GunData} builds its targeting profiles as static
 * fields, which loads this class, which would then demand a registry entry that may not exist yet. Registry access
 * during static init is exactly the kind of ordering dependency that boots fine on one loader and dies on the other.
 * The id is enough: the overlay resolves the actual SoundEvent at play time, when the registry is certainly ready.
 * </p>
 * <p>
 * ⚠ ASSET PATH IS {@code item/smart_hud/}, NOT A WEAPON FOLDER. The tracker is shared machinery; a pistol wearing a
 * smart module should not be playing sounds out of the smartgun's folder.
 * </p>
 */
public final class SmartHudSounds {

    public static final ResourceLocation BOOT = HumanResources.location("item.smart_hud.boot");

    public static final ResourceLocation ACQUIRE = HumanResources.location("item.smart_hud.acquire");

    public static final ResourceLocation LOCK = HumanResources.location("item.smart_hud.lock");

    public static final ResourceLocation LOCK_LOST = HumanResources.location("item.smart_hud.lock_lost");

    private SmartHudSounds() {
        throw new UnsupportedOperationException();
    }
}
