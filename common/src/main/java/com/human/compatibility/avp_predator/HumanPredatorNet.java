package com.human.compatibility.avp_predator;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/**
 * avp_predator's capture net, seen from avp_human, without avp_predator being required at runtime.
 * <p>
 * ⚠⚠ WHY A MARINE NEEDS TO ASK. The net holds a mob by cancelling {@code serverAiStep} and switching its GOAP agent OFF
 * ({@code PredatorNet.setGoapEnabled(mob, false)}). {@code Marine.tick} re-asserts {@code agent.setEnabled(...)} every
 * tick from the inventory-GUI open count - deliberately, so no dropped connection can strand a marine brainless - and
 * that re-assertion switched the agent straight back ON. A netted marine kept planning, walking and shooting through
 * the mesh. [stated] "the net rules we made have to apply to the marines as well." The GUI count and the net are now
 * AND-ed in the same place.
 * <p>
 * {@link #release} is the same call the player's sword or axe makes ({@code MixinMob_NetCutFree}); the marine rescue in
 * {@code ai/cut_free} uses it so a knife and a sword free a mob by exactly one code path.
 * <p>
 * THE GUARD IS THE INNER CLASS, NOT THE IF - same shape as {@code HumanAlienBlood}. Every avp_predator reference lives
 * in {@link Compatibility}, which is not loaded until the mod check has passed.
 */
public final class HumanPredatorNet {

    public static boolean isNetted(Entity entity) {
        if (!AVPPredator.MOD.isLoaded()) {
            return false;
        }

        return Compatibility.isNetted(entity);
    }

    /**
     * The CLIENT's view: whether avp_predator has told this game the entity is netted. ⚠ Client only - the class it
     * reads is never loaded on a dedicated server. Used so a netted mob's net pose wins over the frozen-solid pose
     * hold.
     */
    public static boolean isNettedOnClient(Entity entity) {
        return AVPPredator.MOD.isLoaded() && ClientCompatibility.isNetted(entity);
    }

    private static final class ClientCompatibility {

        private static boolean isNetted(Entity entity) {
            return com.predator.client.net.ClientNetState.isNetted(entity.getId());
        }

        private ClientCompatibility() {
            throw new UnsupportedOperationException();
        }
    }

    /** Frees a netted or captured creature. A no-op without avp_predator, or for a creature that is not netted. */
    public static void release(LivingEntity livingEntity) {
        if (!AVPPredator.MOD.isLoaded()) {
            return;
        }

        Compatibility.release(livingEntity);
    }

    private static final class Compatibility {

        private static boolean isNetted(Entity entity) {
            return com.predator.common.gameplay.net.PredatorNet.isNetted(entity);
        }

        private static void release(LivingEntity livingEntity) {
            if (com.predator.common.gameplay.net.PredatorNet.isNetted(livingEntity)) {
                com.predator.common.gameplay.net.PredatorNet.release(livingEntity);
            }
        }

        private Compatibility() {
            throw new UnsupportedOperationException();
        }
    }

    private HumanPredatorNet() {
        throw new UnsupportedOperationException();
    }
}
