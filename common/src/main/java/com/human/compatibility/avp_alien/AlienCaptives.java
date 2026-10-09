package com.human.compatibility.avp_alien;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

/**
 * Whether an entity is a captive being carried off by a xenomorph.
 * <p>
 * Oct 5 - found walking the host-capture path after marines were set to hunt xenos on sight. avp_alien carries a
 * captured host home on the captor's back ({@code HostCaptureTask.capture}: the host rides the xenomorph). Two things
 * in avp_human did not know that state exists:
 * </p>
 * <ul>
 * <li>A carried MARINE kept its brain on. avp_alien switches the captive's GOAP agent off ({@code removeFreedom}), but
 * {@code Marine.tick} re-asserts the agent's enabled state every tick from its own conditions and switched it straight
 * back on - the same trap the predator net hit. The captive's sensors then saw the xeno carrying it as a target and it
 * opened fire from the captor's back.</li>
 * <li>A marine shooting at a carrier could hit the CAPTIVE first - it rides on top of the target's box. Squadmates were
 * already safe (bullets pass through marine allies); a carried player or villager was not.</li>
 * </ul>
 * <p>
 * ⚠ DEPENDENCY-FREE: the captor is recognised by avp_alien's own {@code aliens} tag, named by string. A tag that does
 * not exist is empty, so without avp_alien this is always false. Riding a xenomorph is never voluntary, so the test
 * needs nothing else.
 * </p>
 */
public final class AlienCaptives {

    private static final TagKey<EntityType<?>> ALIENS = TagKey.create(
        Registries.ENTITY_TYPE,
        ResourceLocation.fromNamespaceAndPath("avp_alien", "aliens")
    );

    private AlienCaptives() {
        throw new UnsupportedOperationException();
    }

    /** {@return whether {@code entity} is riding a xenomorph - i.e. being carried off as a host} */
    public static boolean isCarriedOff(Entity entity) {
        var vehicle = entity.getVehicle();

        return vehicle != null && vehicle.getType().is(ALIENS);
    }
}
