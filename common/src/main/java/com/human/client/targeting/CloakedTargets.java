package com.human.client.targeting;

import net.minecraft.world.entity.LivingEntity;

import java.lang.reflect.Method;

/**
 * Asks avp_predator whether an entity is cloaked, without avp_human depending on avp_predator.
 * <p>
 * ⚠⚠ REFLECTION, AND IT HAS TO BE. avp_predator is a modCompileOnly OPTIONAL dependency: the class compiles against it
 * and it may simply not be present at runtime. A direct call to {@code PredatorCloakClientState.isCloaked} would
 * compile perfectly and then throw NoClassDefFoundError the first time a xenomorph walked into frame on any install
 * without the predator mod - which is most of them. This is the same trap that took down a datagen provider through an
 * innocent-looking import.
 * </p>
 * <p>
 * ⚠ THE LOOKUP HAPPENS ONCE. Resolved on first use and cached, including the failure - a reflective lookup per entity
 * per tick would be a real cost in a scan that already runs twenty times a second.
 * </p>
 */
public final class CloakedTargets {

    private static final String CLASS_NAME = "com.predator.client.cloak.PredatorCloakClientState";

    private static boolean resolved;

    private static Method isCloakedMethod;

    /**
     * ⭐ A CLOAKED TARGET IS NOT A CONTACT. [stated] the tracker "cant track cloaked targets" - which is the whole point
     * of the predator's cloak, and a smart weapon that boxed one anyway would make the cloak worthless against the one
     * thing it most needs to work against.
     * <p>
     * ⚠ VANILLA INVISIBILITY COUNTS TOO. An invisible mob is not something a thermal-and-EM optic should be drawing a
     * box around either, and it costs nothing to check.
     * </p>
     */
    public static boolean isCloaked(LivingEntity entity) {
        if (entity.isInvisible()) {
            return true;
        }

        var method = resolve();

        if (method == null) {
            return false;
        }

        try {
            return (boolean) method.invoke(null, entity);
        } catch (Exception exception) {
            // A predator build that changed the method is not worth a crash - treat it as "not cloaked".
            return false;
        }
    }

    private static synchronized Method resolve() {
        if (resolved) {
            return isCloakedMethod;
        }

        resolved = true;

        try {
            isCloakedMethod = Class.forName(CLASS_NAME).getMethod("isCloaked", LivingEntity.class);
        } catch (Throwable throwable) {
            // avp_predator simply is not installed. Expected, and not an error.
            isCloakedMethod = null;
        }

        return isCloakedMethod;
    }

    private CloakedTargets() {
        throw new UnsupportedOperationException();
    }
}
