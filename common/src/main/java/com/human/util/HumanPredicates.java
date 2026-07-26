package com.human.util;

import com.blib.api.common.entity.v1.BLibEntityPredicates;
import com.human.common.registry.tag.HumanEntityTypeTags;
import com.human.common.registry.tag.HumanItemTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public class HumanPredicates {

    public static boolean canBeIrradiated(Entity entity) {
        if (
            // If this is not a living entity...
            !(entity instanceof LivingEntity livingEntity)
                // Or if the entity is radiation-resistant...
                || livingEntity.getType().is(HumanEntityTypeTags.RADIATION_RESISTANT)
                // Or if the living entity is immortal...
                || BLibEntityPredicates.isInvulnerable(livingEntity)
                // NOTE: deliberately NOT gated on "already has the radiation effect". Radiation is now an
                // accumulating exposure level rather than a one-shot dose, so a source must keep contributing
                // while it is present - the old gate is what produced the endless re-application sawtooth.
                // Or if the entity is no longer alive...
                || !livingEntity.isAlive()
        ) {
            // Then we don't want to or can't reasonably apply the radiation effect. Abort.
            return false;
        }

        var hasFullRadiationResistantArmor = BLibEntityPredicates.hasFullArmorSetMatching(
            livingEntity,
            itemStack -> itemStack.is(HumanItemTags.RADIATION_RESISTANT_ARMORS)
        );

        // Entity should not have a full set of radiation-resistant armor.
        return !hasFullRadiationResistantArmor;
    }
}
